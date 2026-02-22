package com.marianz.bing_search_automation

import android.R.attr.delay
import android.accessibilityservice.AccessibilityService
import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import kotlin.random.Random

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
class SearchExecutor(private val service: AccessibilityService) {

    private val handler = Handler(Looper.getMainLooper())
    private var triggerSearchAttempt = 0
    private val MAX_TRIGGER_ATTEMPTS = 10
    private val RETRY_DELAY_MS = 300L
    private var currentSearchAttempts = 0
    private val MAX_SEARCH_ATTEMPTS = 3

    private val TAG = "SearchExecutor"

    fun clickHomeSearchBox() {
        Log.d(TAG, "clickHomeSearchBox: Attempting to click home search box")
        val root = service.rootInActiveWindow
        if (root == null) {
            Log.d(TAG, "clickHomeSearchBox: Root is null, retrying (attempt ${triggerSearchAttempt + 1})")
            retryTriggerSearch()
            return
        }

        root.refresh()
        Log.d(TAG, "clickHomeSearchBox: Root refreshed")

        val searchNode = NodeFinder.findByText(root, "Search")

        if (searchNode != null) {
            Log.d(TAG, "clickHomeSearchBox: Found search node with text: ${searchNode.text}")
            if (searchNode.isClickable) {
                Log.d(TAG, "clickHomeSearchBox: Search node is clickable, performing click")
                val success = searchNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "clickHomeSearchBox: Click performed, success: $success")
                triggerSearchAttempt = 0
            } else {
                Log.d(TAG, "clickHomeSearchBox: Search node found but not clickable")
                retryTriggerSearch()
            }
        } else {
            Log.d(TAG, "clickHomeSearchBox: No search node found")
            retryTriggerSearch()
        }
    }

    private fun retryTriggerSearch() {
        triggerSearchAttempt++
        Log.d(TAG, "retryTriggerSearch: Attempt $triggerSearchAttempt of $MAX_TRIGGER_ATTEMPTS")

        if (triggerSearchAttempt <= MAX_TRIGGER_ATTEMPTS) {
            handler.postDelayed({
                Log.d(TAG, "retryTriggerSearch: Executing retry")
                clickHomeSearchBox()
            }, RETRY_DELAY_MS)
        } else {
            Log.d(TAG, "retryTriggerSearch: Max attempts reached, resetting counter")
            triggerSearchAttempt = 0
        }
    }

    fun performSearch(query: String, onComplete: () -> Unit) {
        Log.d(TAG, "performSearch: Starting search with query: '$query'")
        currentSearchAttempts = 0
        executeSearchFlow(query, onComplete)
    }

    private fun executeSearchFlow(query: String, onComplete: () -> Unit) {
        currentSearchAttempts++
        Log.d(TAG, "executeSearchFlow: Attempt $currentSearchAttempts of $MAX_SEARCH_ATTEMPTS for query: '$query'")

        val root = service.rootInActiveWindow
        if (root == null) {
            Log.d(TAG, "executeSearchFlow: Root is null, retrying flow")
            retryFlow(query, onComplete)
            return
        }

        root.refresh()
        Log.d(TAG, "executeSearchFlow: Root refreshed")

        val editText = NodeFinder.findFirstEditText(root)
        if (editText == null) {
            Log.d(TAG, "executeSearchFlow: No EditText found, clicking home search box and retrying")
            clickHomeSearchBox()
            retryFlow(query, onComplete)
            return
        }

        Log.d(TAG, "executeSearchFlow: Found EditText: ${editText.text}")

        enterSearchQuery(query) { success ->
            if (success) {
                Log.d(TAG, "executeSearchFlow: Query entered successfully, attempting to trigger search")
                attemptToTriggerSearch(query, onComplete)
            } else {
                Log.d(TAG, "executeSearchFlow: Failed to enter query, retrying flow")
                retryFlow(query, onComplete)
            }
        }
    }

    private fun retryFlow(query: String, onComplete: () -> Unit) {
        if (currentSearchAttempts < MAX_SEARCH_ATTEMPTS) {
            Log.d(TAG, "retryFlow: Scheduling retry in 1500ms")
            handler.postDelayed({
                Log.d(TAG, "retryFlow: Executing retry")
                executeSearchFlow(query, onComplete)
            }, 1500)
        } else {
            Log.d(TAG, "retryFlow: Max attempts reached, completing with random delay")
            waitRandomDelayAndComplete(onComplete)
        }
    }

    private fun enterSearchQuery(
        query: String,
        onComplete: (Boolean) -> Unit
    ) {
        Log.d(TAG, "enterSearchQuery: Attempting to enter query: '$query'")

        val root = service.rootInActiveWindow
        if (root == null) {
            Log.d(TAG, "enterSearchQuery: Root is null")
            onComplete(false)
            return
        }

        val editText = NodeFinder.findFirstEditText(root)
        if (editText == null) {
            Log.d(TAG, "enterSearchQuery: No EditText found")
            onComplete(false)
            return
        }

        Log.d(TAG, "enterSearchQuery: Found EditText, current text: ${editText.text}")

        // Focus and click
        Log.d(TAG, "enterSearchQuery: Setting focus")
        val focusSuccess = editText.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        Log.d(TAG, "enterSearchQuery: Focus result: $focusSuccess")

        Log.d(TAG, "enterSearchQuery: Performing click")
        val clickSuccess = editText.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Log.d(TAG, "enterSearchQuery: Click result: $clickSuccess")

        handler.postDelayed({
            Log.d(TAG, "enterSearchQuery: Clearing existing text")
            val clearArgs = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    ""
                )
            }

            val clearSuccess = editText.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                clearArgs
            )
            Log.d(TAG, "enterSearchQuery: Clear result: $clearSuccess")

            handler.postDelayed({
                Log.d(TAG, "enterSearchQuery: Setting new text: '$query'")
                val args = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        query
                    )
                }

                val success = editText.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    args
                )

                Log.d(TAG, "enterSearchQuery: Set text result: $success")

                handler.postDelayed({
                    Log.d(TAG, "enterSearchQuery: Completing with result: $success")
                    onComplete(success)
                }, 800)

            }, 300)

        }, 300)
    }

    private fun attemptToTriggerSearch(query: String, onComplete: () -> Unit) {
        Log.d(TAG, "attemptToTriggerSearch: Attempting to trigger search for: '$query'")
        tryClickFirstSuggestion(query, onComplete)
    }

    private fun tryClickFirstSuggestion(query: String, onComplete: () -> Unit) {
        Log.d(TAG, "tryClickFirstSuggestion: Looking for suggestions for: '$query'")

        handler.postDelayed({
            Log.d(TAG, "tryClickFirstSuggestion: Executing suggestion check")

            val root = service.rootInActiveWindow
            if (root == null) {
                Log.d(TAG, "tryClickFirstSuggestion: Root is null")
                tryClickSearchButton(onComplete)
                return@postDelayed
            }

            root.refresh()
            Log.d(TAG, "tryClickFirstSuggestion: Root refreshed")

            val clickableItems = findClickableItemsWithText(root)
            Log.d(TAG, "tryClickFirstSuggestion: Found ${clickableItems.size} clickable items with text")

            // First attempt: Find suggestions matching the query
            val suggestions = clickableItems.filter {
                it.text?.toString()?.contains(query, true) == true &&
                        it.className != "android.widget.EditText"
            }

            Log.d(TAG, "tryClickFirstSuggestion: Found ${suggestions.size} suggestions matching query")

            if (suggestions.isNotEmpty()) {
                val first = suggestions.first()
                Log.d(TAG, "tryClickFirstSuggestion: First suggestion text: '${first.text}'")

                val clickableParent = NodeFinder.findClickableParent(first)
                val targetNode = clickableParent ?: first

                Log.d(TAG, "tryClickFirstSuggestion: Clicking node")
                val clickSuccess = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "tryClickFirstSuggestion: Click result: $clickSuccess")

                Log.d(TAG, "tryClickFirstSuggestion: Suggestion clicked, waiting for results")
                waitForResults(onComplete)
            } else {
                // Second attempt: Try the RecyclerView approach that worked before
                Log.d(TAG, "tryClickFirstSuggestion: No suggestions found, trying RecyclerView approach")
                if (clickFirstTextViewUnderRecyclerView()) {
                    Log.d(TAG, "tryClickFirstSuggestion: RecyclerView item clicked successfully")
                    waitForResults(onComplete)
                } else {
                    // Final fallback: Try search button
                    Log.d(TAG, "tryClickFirstSuggestion: RecyclerView approach failed, falling back to search button")
                    tryClickSearchButton(onComplete)
                }
            }

        }, 1200)
    }

    private fun clickFirstTextViewUnderRecyclerView(): Boolean {
        Log.d(TAG, "clickFirstTextViewUnderRecyclerView: Attempting to find and click RecyclerView item")

        val rootNode = service.rootInActiveWindow
        if (rootNode == null) {
            Log.d(TAG, "clickFirstTextViewUnderRecyclerView: Root is null")
            return false
        }

        val recyclerViewNode = NodeFinder.findNodeByClass(rootNode, "androidx.recyclerview.widget.RecyclerView")

        if (recyclerViewNode == null) {
            Log.d(TAG, "clickFirstTextViewUnderRecyclerView: No RecyclerView found")
            return false
        }

        Log.d(TAG, "clickFirstTextViewUnderRecyclerView: Found RecyclerView")

        val textView = NodeFinder.findFirstNonEmptyTextView(recyclerViewNode)
        if (textView == null) {
            Log.d(TAG, "clickFirstTextViewUnderRecyclerView: No non-empty TextView found in RecyclerView")
            return false
        }

        Log.d(TAG, "clickFirstTextViewUnderRecyclerView: Found TextView with text: '${textView.text}'")

        val clickableParent = NodeFinder.findClickableParent(textView)
        if (clickableParent != null) {
            Log.d(TAG, "clickFirstTextViewUnderRecyclerView: Clicking parent")
            val success = clickableParent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "clickFirstTextViewUnderRecyclerView: Click result: $success")
            return success
        } else {
            Log.d(TAG, "clickFirstTextViewUnderRecyclerView: Clicking TextView directly")
            val success = textView.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "clickFirstTextViewUnderRecyclerView: Click result: $success")
            return success
        }
    }

    private fun tryClickSearchButton(onComplete: () -> Unit) {
        Log.d(TAG, "tryClickSearchButton: Attempting to click search button")

        val root = service.rootInActiveWindow
        if (root == null) {
            Log.d(TAG, "tryClickSearchButton: Root is null")
            // Still need to complete somehow
            waitRandomDelayAndComplete(onComplete)
            return
        }

        root.refresh()
        Log.d(TAG, "tryClickSearchButton: Root refreshed")

        val button = NodeFinder.findByText(root, "Search")

        if (button != null) {
            Log.d(TAG, "tryClickSearchButton: Found search button")
            if (button.isClickable) {
                Log.d(TAG, "tryClickSearchButton: Search button is clickable, performing click")
                val clickSuccess = button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "tryClickSearchButton: Click result: $clickSuccess")
            } else {
                Log.d(TAG, "tryClickSearchButton: Search button not clickable")
            }
        } else {
            Log.d(TAG, "tryClickSearchButton: No search button found")
        }

        waitForResults(onComplete)
    }

    private fun waitForResultsX(onComplete: () -> Unit) {
        Log.d(TAG, "waitForResults: Starting to wait for search results")

        var attempts = 0
        val maxAttempts = 15

        fun check() {
            val root = service.rootInActiveWindow
            if (root == null) {
                Log.d(TAG, "waitForResults check: Root is null")
                attempts++
                if (attempts < maxAttempts) {
                    handler.postDelayed(::check, 700)
                } else {
                    Log.d(TAG, "waitForResults check: Max attempts reached with null root, proceeding")
                    waitRandomDelayAndComplete(onComplete)
                }
                return
            }

            root.refresh()
            Log.d(TAG, "waitForResults check: Root refreshed, attempt $attempts")

            if (isSearchResultsPageLoaded()) {
                Log.d(TAG, "waitForResults check: Results page detected")
                waitRandomDelayAndComplete(onComplete)
                return
            }

            attempts++
            Log.d(TAG, "waitForResults check: Results not ready, attempt $attempts of $maxAttempts")

            if (attempts < maxAttempts) {
                handler.postDelayed(::check, 700)
            } else {
                Log.d(TAG, "waitForResults check: Max attempts reached, proceeding anyway")
                waitRandomDelayAndComplete(onComplete)
            }
        }

        Log.d(TAG, "waitForResults: Scheduling first check in 1200ms")
        handler.postDelayed(::check, 1200)
    }

    private fun waitForResults(onComplete: () -> Unit) {
        Log.d(TAG, "waitForResults: Starting to wait for search results")

        var attempts = 0
        val maxAttempts = 15

        fun check() {
            val root = service.rootInActiveWindow
            if (root == null) {
                Log.d(TAG, "waitForResults check: Root is null")
                attempts++
                if (attempts < maxAttempts) {
                    handler.postDelayed(::check, 1200)
                } else {
                    Log.d(TAG, "waitForResults check: Max attempts reached with null root, proceeding")
                    waitRandomDelayAndComplete(onComplete)
                }
                return
            }

            root.refresh()
            Log.d(TAG, "waitForResults check: Root refreshed, attempt $attempts")

            // Check both conditions: results page loaded AND search tabs present
            val resultsPageLoaded = isSearchResultsPageLoaded()
            val searchTabsPresent = areSearchTabsPresent()

            Log.d(TAG, "waitForResults check: Results page loaded: $resultsPageLoaded, Search tabs present: $searchTabsPresent")

            if (resultsPageLoaded && searchTabsPresent) {
                Log.d(TAG, "waitForResults check: Results page fully loaded with all tabs")
                waitRandomDelayAndComplete(onComplete)
                return
            }

            attempts++
            Log.d(TAG, "waitForResults check: Page not fully loaded, attempt $attempts of $maxAttempts")

            if (attempts < maxAttempts) {
                handler.postDelayed(::check, 1200)
            } else {
                Log.d(TAG, "waitForResults check: Max attempts reached, proceeding anyway")
                waitRandomDelayAndComplete(onComplete)
            }
        }

        Log.d(TAG, "waitForResults: Scheduling first check in 1200ms")
        handler.postDelayed(::check, 1200)
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun areSearchTabsPresent(): Boolean {
        val rootNode = service.rootInActiveWindow ?: return false
        Log.d(TAG, "areSearchTabsPresent: Checking for search tabs")

        val requiredTabs = listOf("ALL", "SEARCH", "IMAGES", "VIDEOS", "NEWS")
        val foundTabs = mutableSetOf<String>()

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return

            val text = node.text?.toString()?.trim()?.uppercase()
            val contentDesc = node.contentDescription?.toString()?.trim()?.uppercase()

            for (tab in requiredTabs) {
                if (text == tab || contentDesc == tab) {
                    if (!foundTabs.contains(tab)) {
                        Log.d(TAG, "areSearchTabsPresent: Found tab: $tab")
                        foundTabs.add(tab)
                    }
                }
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        traverse(rootNode)

        val allTabsFound = foundTabs.containsAll(requiredTabs)
        Log.d(TAG, "areSearchTabsPresent: Found ${foundTabs.size}/5 tabs, all found: $allTabsFound")
        return allTabsFound
    }

    private fun isSearchResultsPageLoaded(): Boolean {
        val root = service.rootInActiveWindow
        if (root == null) {
            Log.d(TAG, "isSearchResultsPageLoaded: Root is null")
            return false
        }

        val hasRecyclerView = NodeFinder.findNodeByClass(
            root,
            "androidx.recyclerview.widget.RecyclerView"
        ) != null

        val hasWebView = NodeFinder.findNodeByClass(root, "android.webkit.WebView") != null
        val hasAllText = NodeFinder.findByText(root, "ALL") != null

        Log.d(TAG, "isSearchResultsPageLoaded - RecyclerView: $hasRecyclerView, WebView: $hasWebView, ALL text: $hasAllText")

        return hasRecyclerView || hasWebView || hasAllText
    }

    private fun findClickableItemsWithText(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        Log.d(TAG, "findClickableItemsWithText: Traversing nodes")

        val items = mutableListOf<AccessibilityNodeInfo>()

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return

            if (node.isClickable && !node.text.isNullOrBlank()) {
                Log.d(TAG, "findClickableItemsWithText: Found clickable item with text: '${node.text}'")
                items.add(node)
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        traverse(root)
        Log.d(TAG, "findClickableItemsWithText: Total clickable items found: ${items.size}")
        return items
    }

    private fun waitRandomDelayAndComplete(onComplete: () -> Unit) {
        val delay = (10_000..20_000).random()
        Log.d(TAG, "waitRandomDelayAndComplete: Completing after random delay of ${delay}ms")
        handler.postDelayed({
            Log.d(TAG, "waitRandomDelayAndComplete: Executing onComplete")
            onComplete()
        }, delay.toLong())
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    fun waitUntilSearchEditTextFound(
        maxAttempts: Int = 10,
        delayMillis: Long = 2000L,
        onSuccess: (AccessibilityNodeInfo) -> Unit,
        onFailure: () -> Unit
    ) {
        var attempt = 0
        Log.d(TAG, "waitUntilSearchEditTextFound: Starting search for EditText")

        val runnable = object : Runnable {
            override fun run() {
                val rootNode = service.rootInActiveWindow
                Log.d(TAG, "waitUntilSearchEditTextFound run: Attempt ${attempt + 1}")

                val editText = NodeFinder.findFirstEditText(rootNode)

                if (editText != null) {
                    Log.d(TAG, "waitUntilSearchEditTextFound: EditText found")
                    onSuccess(editText)
                } else {
                    attempt++
                    Log.d(TAG, "waitUntilSearchEditTextFound: EditText not found")
                    if (attempt < maxAttempts) {
                        Log.d(TAG, "waitUntilSearchEditTextFound: Scheduling retry")
                        handler.postDelayed(this, delayMillis)
                    } else {
                        Log.d(TAG, "waitUntilSearchEditTextFound: Max attempts reached")
                        onFailure()
                    }
                }
            }
        }

        handler.post(runnable)
    }
}