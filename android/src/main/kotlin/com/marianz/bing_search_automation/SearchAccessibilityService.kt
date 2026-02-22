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
        }

        fun getQueryType(): QueryType = queryType

        fun setLoggingEnabled(enabled: Boolean) {
            loggingEnabled = enabled
            Logger.enable(enabled)
        }


        private var currentQuery: String? = null
        private var isSearching = false
        private var lastSearchTime = 0L

        fun getCurrentQuery(): String? = currentQuery
        fun setCurrentQuery(query: String?) {
            currentQuery = query
        }
    }

    override fun onCreate() {
        super.onCreate()

        instance = this
        searchExecutor = SearchExecutor(this)
        readExecutor = ReadExecutor(this)
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return

        if (!getLaunchedFromApp()) {
            Logger.d("Manual launch detected, skipping automation")
            return
        }

        if (isSearching) {
            return
        }

        if (packageName.contains("bing", ignoreCase = true)) {
            handleBingEvent(event)


            //Logger.logNodeTree(rootInActiveWindow)

            val eventType = event.eventType
            when (eventType) {
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                    val text = event.text?.firstOrNull()?.toString()
                    if (!text.isNullOrBlank()) {
                        Log.d("SearchAccessibility", "Text changed: $text")
                    }
                }

                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    val node = event.source
                    val className = node?.className?.toString()
                    if (className?.contains("Button", ignoreCase = true) == true) {
                        Log.d("SearchAccessibility", "Button clicked: $className")
                        // Optional: Mark query as submitted
                        // currentQuery = null
                    }
                }

                AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                    Log.d("SearchAccessibility", "View focused")
                }

                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> {
                    Log.d("SearchAccessibility", "Accessibility focus")
                }

                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    Log.d("SearchAccessibility", "Window state changed: ${event.className}")
                }

                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    Log.d("SearchAccessibility", "Window content changed")
                }

                AccessibilityEvent.TYPE_ANNOUNCEMENT -> {
                    Log.d("SearchAccessibility", "Announcement event")
                }

                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                    Log.d("SearchAccessibility", "Notification state changed")
                }

                AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START,
                AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END -> {
                    Log.d("SearchAccessibility", "Touch exploration gesture")
                }

                else -> {
                    Log.d("SearchAccessibility", "Unhandled event type: $eventType")
                }


            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun handleBingEvent(event: AccessibilityEvent) {
        //Logger.d("Bing app launched, type: ${event.eventType}")

        if (queryType == QueryType.SEARCH) {
            if (!isSearching) {
                Handler(Looper.getMainLooper()).postDelayed({
                    searchExecutor.clickHomeSearchBox()
                }, 5000) // wait for keyboard to appear
            }

            searchExecutor.waitUntilSearchEditTextFound(
                onSuccess = { editText ->
                    if (searchQueue.isNotEmpty() && shouldPerformSearch()) {
                        isSearching = true
                        updateLastSearchTime()

                        val nextQuery = searchQueue.removeAt(0)
                        setCurrentQuery(nextQuery)

                        BingSearchAutomationPlugin.sendProgress(totalQueries - searchQueue.size, totalQueries)

                        searchExecutor.performSearch(nextQuery) {
                            isSearching = false
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                    } else {
                        BingSearchAutomationPlugin.sendCompleted()
                    }
                },
                onFailure = {
                    //Logger.e("EditText not found after 10 tries.")
                }
            )
        } else if (queryType == QueryType.READ) {
            Logger.d("READ query type");

            readExecutor.performRead("Rewards") {
                isSearching = false
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            Logger.logNodeTree(rootInActiveWindow);
        } else if (queryType == QueryType.ANSWER) {
            Logger.d("ANSWER query type");
        }


    }

    override fun onInterrupt() {
        setLaunchedFromApp(false)
    }

    fun shouldPerformSearch(): Boolean {
        val now = System.currentTimeMillis()
        return !isSearching && (now - lastSearchTime >= 3000)
    }

    fun updateLastSearchTime() {
        lastSearchTime = System.currentTimeMillis()
    }

}