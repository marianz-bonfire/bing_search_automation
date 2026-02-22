package com.marianz.bing_search_automation

import android.accessibilityservice.AccessibilityService
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

@TargetApi(Build.VERSION_CODES.DONUT)
class SearchAccessibilityService : AccessibilityService() {
    private lateinit var searchExecutor: SearchExecutor
    private lateinit var readExecutor: ReadExecutor
    private val handler = Handler(Looper.getMainLooper())
    private var readSessionActive = false
    private var readSessionCompleted = false
    private var readSessionStartTime: Long = 0
    private val READ_SESSION_TIMEOUT = 60000L // 60 seconds timeout

    companion object {
        val searchQueue: MutableList<String> = mutableListOf()
        var totalQueries = 0

        private var instance: SearchAccessibilityService? = null
        fun getInstance(): SearchAccessibilityService? = instance

        private var launchedFromApp: Boolean = false
        fun getLaunchedFromApp(): Boolean = launchedFromApp
        fun setLaunchedFromApp(launched: Boolean) {
            launchedFromApp = launched
        }

        private var queryType: QueryType = QueryType.SEARCH
        private var loggingEnabled: Boolean = false

        fun setQueryType(type: QueryType) {
            queryType = type
            Logger.d("QueryType set to: $type")
        }

        fun getQueryType(): QueryType = queryType

        fun setLoggingEnabled(enabled: Boolean) {
            loggingEnabled = enabled
            Logger.enable(enabled)
        }

        private var currentQuery: String? = null
        private var lastSearchTime: Long = System.currentTimeMillis()

        @Volatile
        var isSearching: Boolean = false

        fun getCurrentQuery(): String? = currentQuery
        fun setCurrentQuery(query: String?) {
            currentQuery = query
        }

        private var currentKeyword: String? = null
        fun getCurrentKeyword(): String? = currentKeyword
        fun setCurrentKeyword(keyword: String?) {
            currentKeyword = keyword
        }

        private var minSearchInterval: Long = 10_000 // 10 seconds
        private var maxSearchInterval: Long = 60_000 // 60 seconds

        fun setSearchInterval(minSeconds: Int, maxSeconds: Int) {
            minSearchInterval = (minSeconds * 1000).toLong()
            maxSearchInterval = (maxSeconds * 1000).toLong()
        }

        fun getRandomDelay(): Long {
            return (minSearchInterval..maxSearchInterval).random()
        }

        @Volatile
        var shouldProcessEvents: Boolean = true
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        searchExecutor = SearchExecutor(this)
        readExecutor = ReadExecutor(this)

        //Initialize lastSearchTime when service is created
        Companion.lastSearchTime = System.currentTimeMillis()
        Logger.d("SearchAccessibilityService created, lastSearchTime initialized to ${Companion.lastSearchTime}")
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return

        Logger.d("onAccessibilityEvent - Type: ${event.eventType}, Package: $packageName")
        Logger.d("launchedFromApp: ${getLaunchedFromApp()}, shouldProcessEvents: ${Companion.shouldProcessEvents}")
        Logger.d("queryType: ${getQueryType()}, isSearching: $isSearching")
        Logger.d("searchQueue size: ${searchQueue.size}, totalQueries: $totalQueries")

        // Only process if launched from our app AND we should process events
        if (!getLaunchedFromApp() || !Companion.shouldProcessEvents) {
            Logger.d("Skipping event - launchedFromApp: ${getLaunchedFromApp()}, shouldProcessEvents: ${Companion.shouldProcessEvents}")
            return
        }

        // Only process Bing app events
        if (!packageName.contains("bing", ignoreCase = true)) {
            Logger.d("Skipping non-Bing app: $packageName")
            return
        }

        // Check timeout for read sessions
        if (readSessionActive) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - readSessionStartTime > READ_SESSION_TIMEOUT) {
                Logger.e("Read session timeout reached, resetting")
                readSessionActive = false
                readSessionCompleted = true
                BingSearchAutomationPlugin.sendCompleted()
                return
            }
        }

        // Don't process if read session already completed
        if (readSessionCompleted && getQueryType() == QueryType.READ) {
            Logger.d("Read session already completed, skipping")
            return
        }

        // Only process window state/content changes to prevent multiple triggers
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                Logger.d("Window state changed event received")
                processEventBasedOnQueryType(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                Logger.d("Window content changed event received")
                processEventBasedOnQueryType(event)
            }
            else -> {
                Logger.d("Ignoring event type: ${event.eventType}")
            }
        }
    }

    private fun processEventBasedOnQueryType(event: AccessibilityEvent) {
        val currentQueryType = getQueryType()
        Logger.d("processEventBasedOnQueryType - QueryType: $currentQueryType")

        when (currentQueryType) {
            QueryType.SEARCH -> {
                Logger.d("Processing SEARCH query type")
                Logger.d("isSearching: $isSearching, searchQueue.isEmpty(): ${searchQueue.isEmpty()}")

                // Only handle search if not already searching
                if (!isSearching && searchQueue.isNotEmpty()) {
                    Logger.d("Starting search handling")
                    handleSearch(event)
                } else if (searchQueue.isEmpty() && isSearching) {
                    Logger.d("Queue is empty while searching, sending completion")
                    // Queue is empty, send completion
                    isSearching = false
                    BingSearchAutomationPlugin.sendCompleted()
                } else {
                    Logger.d("Not handling search - isSearching: $isSearching, queueEmpty: ${searchQueue.isEmpty()}")
                }
            }
            QueryType.READ -> {
                Logger.d("Processing READ query type")
                Logger.d("readSessionActive: $readSessionActive, readSessionCompleted: $readSessionCompleted")

                // Only start read session if not active and not completed
                if (!readSessionActive && !readSessionCompleted) {
                    Logger.d("Starting read handling")
                    handleRead(event)
                } else {
                    Logger.d("Not handling read - active: $readSessionActive, completed: $readSessionCompleted")
                }
            }
            QueryType.ANSWER -> {
                Logger.d("Processing ANSWER query type")
                handleAnswer()
            }
        }
    }

    private fun handleSearch(event: AccessibilityEvent) {
        Logger.d("handleSearch: Starting search session")
        Logger.d("handleSearch: Event package: ${event.packageName}, type: ${event.eventType}")

        // This prevents multiple overlapping search attempts
        if (isSearching) {
            Logger.d("handleSearch: Already searching, returning")
            return
        }

        isSearching = true
        Logger.d("handleSearch: isSearching set to true")

        // Wait for Bing to load
        Logger.d("handleSearch: Scheduling clickHomeSearchBox in 3000ms")
        handler.postDelayed({
            Logger.d("handleSearch: Executing clickHomeSearchBox")
            searchExecutor.clickHomeSearchBox()

            // Wait for search box to appear
            Logger.d("handleSearch: Scheduling waitUntilSearchEditTextFound in 3000ms")
            handler.postDelayed({
                Logger.d("handleSearch: Executing waitUntilSearchEditTextFound")
                searchExecutor.waitUntilSearchEditTextFound(
                    onSuccess = { editText ->
                        Logger.d("handleSearch: EditText found successfully")

                        // Double-check that we're still supposed to be searching
                        if (!isSearching) {
                            Logger.d("handleSearch: Search was cancelled, returning")
                            return@waitUntilSearchEditTextFound
                        }

                        if (searchQueue.isNotEmpty()) {  // Removed shouldPerformSearch check here
                            Logger.d("handleSearch: Queue has ${searchQueue.size} items, performing search")
                            updateLastSearchTime()  // Update time when we actually start the search

                            val nextQuery = searchQueue.removeAt(0)
                            Logger.d("handleSearch: Next query: '$nextQuery'")

                            setCurrentQuery(nextQuery)
                            setCurrentKeyword(nextQuery)

                            val currentProgress = totalQueries - searchQueue.size
                            Logger.d("handleSearch: Progress $currentProgress/$totalQueries")

                            BingSearchAutomationPlugin.sendProgress(
                                currentProgress,
                                totalQueries,
                                nextQuery
                            )

                            Logger.d("handleSearch: Executing search for '$nextQuery'")
                            searchExecutor.performSearch(nextQuery) {
                                Logger.d("handleSearch: Search completed for '$nextQuery'")
                                isSearching = false
                                Logger.d("handleSearch: isSearching set to false")

                                Logger.d("handleSearch: Performing back action")
                                performGlobalAction(GLOBAL_ACTION_BACK)

                                // Check if queue is empty
                                if (searchQueue.isEmpty()) {
                                    Logger.d("handleSearch: Queue is empty, sending completion")
                                    BingSearchAutomationPlugin.sendCompleted()
                                    Companion.shouldProcessEvents = false
                                    Logger.d("handleSearch: shouldProcessEvents set to false")
                                } else {
                                    Logger.d("handleSearch: Queue still has ${searchQueue.size} items remaining")
                                    // Trigger next search after back action
                                    handler.postDelayed({
                                        if (getLaunchedFromApp() && Companion.shouldProcessEvents) {
                                            Logger.d("handleSearch: Triggering next search")
                                            processEventBasedOnQueryType(event)
                                        }
                                    }, 2000)
                                }
                            }
                        } else {
                            Logger.d("handleSearch: Queue is empty, completing")
                            isSearching = false
                            BingSearchAutomationPlugin.sendCompleted()
                            Companion.shouldProcessEvents = false
                        }
                    },
                    onFailure = {
                        Logger.e("handleSearch: EditText not found after retries")
                        isSearching = false
                        if (searchQueue.isEmpty()) {
                            Logger.d("handleSearch: Queue empty, sending completion after failure")
                            BingSearchAutomationPlugin.sendCompleted()
                            Companion.shouldProcessEvents = false
                        }
                    }
                )
            }, 3000)
        }, 3000)
    }

    private fun handleRead(event: AccessibilityEvent) {
        Logger.d("handleRead: Starting read session")
        Logger.d("handleRead: Event package: ${event.packageName}, type: ${event.eventType}")

        if (readSessionActive || readSessionCompleted) {
            Logger.d("handleRead: Read session already active or completed, skipping")
            return
        }

        Logger.d("handleRead: Setting read session active")
        readSessionActive = true
        readSessionStartTime = System.currentTimeMillis()
        Companion.shouldProcessEvents = false
        Logger.d("handleRead: shouldProcessEvents set to false")

        // Wait for Bing to fully load
        Logger.d("handleRead: Scheduling performRead in 5000ms")
        handler.postDelayed({
            Logger.d("handleRead: Executing performRead")
            readExecutor.performRead("latest news") {
                Logger.d("handleRead: Read session completed")

                // Read session completed
                readSessionActive = false
                readSessionCompleted = true
                Logger.d("handleRead: readSessionActive=false, readSessionCompleted=true")

                Logger.d("handleRead: Sending completion")
                BingSearchAutomationPlugin.sendCompleted()

                // Go back to home (optional)
                Logger.d("handleRead: Scheduling back action in 1000ms")
                handler.postDelayed({
                    Logger.d("handleRead: Performing back action")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }, 1000)

                // Reset after delay to allow app to settle
                Logger.d("handleRead: Scheduling re-enable events in 5000ms")
                handler.postDelayed({
                    Logger.d("handleRead: Re-enabling event processing")
                    Companion.shouldProcessEvents = true
                }, 5000)
            }
        }, 5000)
    }

    private fun handleAnswer() {
        Logger.d("handleAnswer: ANSWER query type - Not implemented")
        // For now, just send completion
        BingSearchAutomationPlugin.sendCompleted()
    }

    override fun onInterrupt() {
        Logger.d("onInterrupt: Service interrupted")
        cleanup()
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d("onDestroy: Service being destroyed")
        cleanup()
    }

    private fun cleanup() {
        Logger.d("cleanup: Cleaning up service")
        Companion.shouldProcessEvents = true
        setLaunchedFromApp(false)
        readSessionActive = false
        readSessionCompleted = false
        isSearching = false
        handler.removeCallbacksAndMessages(null)
        readExecutor.cleanup()
        Logger.d("cleanup: Cleanup completed")
    }

    fun shouldPerformSearch(): Boolean {
        val now = System.currentTimeMillis()
        // Add check for first search
        val timeSinceLastSearch = now - lastSearchTime

        // If this is the first search (lastSearchTime was just initialized),
        // we should allow it regardless of the time difference
        val shouldPerform = !isSearching && (timeSinceLastSearch >= 3000 || timeSinceLastSearch < 0)

        Logger.d("shouldPerformSearch: $shouldPerform (isSearching: $isSearching, time since last: ${timeSinceLastSearch}ms)")
        return shouldPerform
    }

    fun updateLastSearchTime() {
        lastSearchTime = System.currentTimeMillis()
        Logger.d("updateLastSearchTime: Updated to $lastSearchTime")
    }
}