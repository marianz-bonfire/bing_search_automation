package com.marianz.bing_search_automation

import android.os.Bundle
import android.accessibilityservice.AccessibilityService
import android.annotation.TargetApi
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import kotlin.random.Random


@TargetApi(Build.VERSION_CODES.DONUT)
class SearchAccessibilityService : AccessibilityService() {

    companion object {
        val searchQueue: MutableList<String> = mutableListOf()

        private var currentQuery: String? = null

        fun getCurrentQuery(): String? = currentQuery
        fun setCurrentQuery(query: String?) {
            currentQuery = query
        }

        private var launchedFromApp: Boolean = false

        fun getLaunchedFromApp(): Boolean = launchedFromApp
        fun setLaunchedFromApp(launched: Boolean) {
            launchedFromApp = launched
        }

    }

    private var isSearching = false
    private var lastSearchTime = 0L
    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        val eventType = event.eventType

       // Log.d("SearchAccessibility", "Event from package: $packageName, type: $eventType")

        if (!launchedFromApp) {
            Log.d("SearchAccessibility", "Manual launch detected, skipping automation")
            return
        }

        if (isSearching) {
            return
        }
        // Handle only Bing-related packages
        if (packageName.contains("bing", ignoreCase = true) && !isSearching) {
            Log.d("SearchAccessibility", "Bing app launched with package: $packageName, type: $eventType")

            // Log current node tree
            //logNodeTree(rootInActiveWindow)

            // Try to trigger search box immediately
            if (!isSearching) {
                Handler(Looper.getMainLooper()).postDelayed({
                    triggerSearchBox()
                }, 5000) // wait for keyboard to appear
            }


            //logNodeTree(rootInActiveWindow)

            // Wait a short delay and then attempt search if EditText is visible
            Handler(Looper.getMainLooper()).postDelayed({
                val rootNode = rootInActiveWindow
                val editText = findFirstEditText(rootNode)
                if (editText != null && searchQueue.isNotEmpty()) {
                    isSearching = true
                    val nextQuery = searchQueue.removeAt(0)
                    //performSearch(nextQuery)
                    maybePerformSearchWithDelay(nextQuery)

                } else {
                   // Log.w("SearchAccessibility", "EditText not found after delay.")
                }
            }, 1000) // Wait 1 second before searching
        }

        when (eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val text = event.text?.firstOrNull()?.toString()
                if (!text.isNullOrBlank()) {
                    currentQuery = text
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


    override fun onInterrupt() {
        launchedFromApp = false
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun performSearch(query: String) {
        val rootNode = rootInActiveWindow ?: return
        val editText = findFirstEditText(rootNode)

        if (editText != null) {
            val args = Bundle()
            Log.d("SearchAccessibility", "Inputting valye: $query")

            args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                query
            )
            val success = editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            Log.d("SearchAccessibility", "Set text result: $success")

            if (success) {
                //logNodeTree(rootNode)
                // Wait a moment before pressing Enter or clicking Search
                Handler(Looper.getMainLooper()).postDelayed({
                    clickSearchButton()  // Try softkey button first
                    pressEnterKey() //if no UI button found
                    if (clickSearchOnKeyboard(rootInActiveWindow)) {


                    }
                    //logNodeTree(rootNode)
                    //pressKeyboardSearchKey(); //It works if rooted
                }, 500)

                Handler(Looper.getMainLooper()).postDelayed({
                    clickFirstTextViewUnderRecyclerView()
                }, 1000)



                Handler(Looper.getMainLooper()).postDelayed({
                    waitForSearchTabs(
                        maxRetries = 100,
                        delayMillis = 5000,
                        onFound = {
                            Log.d("SearchAccessibility", "Ready to click tab now.")

                            // Add random delay (10–20 seconds)
                            val randomDelay = (10_000..20_000).random()
                            Log.d("SearchAccessibility", "Delaying performBackAction() by ${randomDelay}ms")

                            Handler(Looper.getMainLooper()).postDelayed({

                                isSearching = false
                                performBackAction()
                            }, randomDelay.toLong())
                        },
                        onTimeout = {
                            Log.e("SearchAccessibility", "Tabs not found after waiting.")
                        }
                    )
                }, 1000)


            }
        } else {
            Log.w("SearchAccessibility", "No EditText found for search")
        }
    }

    private fun maybePerformSearchWithDelay(query: String, delayMs: Long = 1000L) {
        if (!launchedFromApp) {
            Log.d("SearchAccessibility", "Skipped search: Bing not launched from app")
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastSearchTime < 3000) {
            //Log.d("SearchAccessibility", "Skipped search: last one was too recent")
            return
        }

        lastSearchTime = now
        Handler(Looper.getMainLooper()).postDelayed({
            performSearch(query)
        }, delayMs)
    }



    private fun findEditText(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.className == "android.widget.EditText") {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findEditText(child)
            if (result != null) return result
        }

        return null
    }

    private fun findAndClickButton(node: AccessibilityNodeInfo?, buttonText: String): Boolean {
        if (node == null) return false

        if (node.className?.contains("Button") == true && node.text?.toString()?.contains(buttonText, true) == true) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        for (i in 0 until node.childCount) {
            if (findAndClickButton(node.getChild(i), buttonText)) return true
        }

        return false
    }

    private var triggerSearchAttempt = 0
    private val MAX_TRIGGER_ATTEMPTS = 10
    private val RETRY_DELAY_MS = 300L

    private fun triggerSearchBox() {
        val rootNode = rootInActiveWindow

        if (isSearching) {
            return
        }
        if (rootNode == null) {
            Log.w("SearchAccessibility", "Root window is null")
            retryTriggerSearch()
            return
        }

        val searchNode = findByText(rootNode, "Search")

        if (searchNode != null && searchNode.isClickable) {
            //Log.d("SearchAccessibility", "Clicking on 'Search' TextView (attempt $triggerSearchAttempt)")
            searchNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            triggerSearchAttempt = 0 // reset attempts after success
        } else {
            //Log.w("SearchAccessibility", "'Search' not found or not clickable (attempt $triggerSearchAttempt)")
            retryTriggerSearch()
        }
    }

    private fun retryTriggerSearch() {
        triggerSearchAttempt++
        if (triggerSearchAttempt <= MAX_TRIGGER_ATTEMPTS) {
            Handler(Looper.getMainLooper()).postDelayed({
                triggerSearchBox()
            }, RETRY_DELAY_MS)
        } else {
            Log.e("SearchAccessibility", "Max triggerSearchBox attempts reached. Giving up.")
            triggerSearchAttempt = 0 // reset
        }
    }


    private fun areSearchTabsPresent(): Boolean {
        val rootNode = rootInActiveWindow ?: return false

        val requiredTabs = listOf("ALL", "SEARCH", "IMAGES", "VIDEOS", "NEWS")
        val foundTabs = mutableSetOf<String>()

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return

            val text = node.text?.toString()?.trim()?.uppercase()
            val contentDesc = node.contentDescription?.toString()?.trim()?.uppercase()

            for (tab in requiredTabs) {
                if (text == tab || contentDesc == tab) {
                    foundTabs.add(tab)
                }
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        traverse(rootNode)

        return foundTabs.containsAll(requiredTabs)
    }

    private fun waitForSearchTabs(
        maxRetries: Int = 20,
        delayMillis: Long = 300L,
        onFound: () -> Unit = {},
        onTimeout: () -> Unit = {}
    ) {
        var retries = 0

        val handler = Handler(Looper.getMainLooper())
        fun checkCondition() {
            if (areSearchTabsPresent()) {
                Log.d("SearchAccessibility", "All tabs found.")
                onFound()
            } else if (retries < maxRetries) {
                retries++
                handler.postDelayed({ checkCondition() }, delayMillis)
            } else {
                Log.w("SearchAccessibility", "Timeout waiting for search tabs.")
                onTimeout()
            }
        }

        checkCondition()
    }


    fun clickSearchButton() {
        val rootNode = rootInActiveWindow ?: return
        val searchButton = findByText(rootNode, "Search")  // or use "Go", "Enter", "Submit" if needed

        if (searchButton != null && searchButton.isClickable) {
            val clicked = searchButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d("SearchAccessibility", "Search button clicked: $clicked")
        } else {
            Log.w("SearchAccessibility", "No clickable search button found")
        }
    }

    fun clickSearchOnKeyboard(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false

        if (root.className == "android.widget.TextView" &&
            root.text?.toString()?.equals("Search", ignoreCase = true) == true) {
            return root.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        for (i in 0 until root.childCount) {
            if (clickSearchOnKeyboard(root.getChild(i))) return true
        }
        return false
    }


    fun pressEnterKey() {
        val root = rootInActiveWindow ?: return
        val focusedNode = findFocusedEditText(root)
        if (focusedNode != null) {
            val result = focusedNode.performAction(EditorInfo.IME_ACTION_SEARCH)
            Log.d("SearchAccessibility", "IME_ACTION result: $result")
        }

    }

    fun findFocusedEditText(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isFocused && node.className == "android.widget.EditText") return node

        for (i in 0 until node.childCount) {
            val result = findFocusedEditText(node.getChild(i))
            if (result != null) return result
        }
        return null
    }

    fun pressKeyboardSearchKey() {
        try {
            // Simulates keyboard Search or Enter key
            Runtime.getRuntime().exec(arrayOf("input", "keyevent", "66")) // Try 84 if needed
            Log.d("SearchAccessibility", "Sent ENTER key event to keyboard")
        } catch (e: Exception) {
            Log.e("SearchAccessibility", "Failed to send ENTER key", e)
        }
    }


    private fun findFirstEditText(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.className == "android.widget.EditText") {
            return node
        }

        for (i in 0 until node.childCount) {
            val result = findFirstEditText(node.getChild(i))
            if (result != null) return result
        }

        return null
    }

    fun clickFirstTextViewUnderRecyclerView() {
        val rootNode = rootInActiveWindow ?: return
        val recyclerViewNode = findNodeByClassName(rootNode, "androidx.recyclerview.widget.RecyclerView")
        if (recyclerViewNode != null) {
            val firstTextView = findFirstNonEmptyTextView(recyclerViewNode)
            if (firstTextView != null) {
                val clickableParent = findClickableParent(firstTextView)
                if (clickableParent != null) {
                    clickableParent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.d("Accessibility", "Clicked parent of: ${firstTextView.text}")
                } else {
                    Log.w("Accessibility", "No clickable parent found for: ${firstTextView.text}")
                }
            } else {
                Log.d("Accessibility", "No non-empty TextView found under RecyclerView")
            }
        } else {
            Log.d("Accessibility", "RecyclerView not found")
        }
    }


    fun findNodeByClassName(node: AccessibilityNodeInfo?, className: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.className == className) return node

        for (i in 0 until node.childCount) {
            val result = findNodeByClassName(node.getChild(i), className)
            if (result != null) return result
        }
        return null
    }

    fun findFirstNonEmptyTextView(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.className == "android.widget.TextView" && !node.text.isNullOrBlank()) {
            return node
        }

        for (i in 0 until node.childCount) {
            val result = findFirstNonEmptyTextView(node.getChild(i))
            if (result != null) return result
        }

        return null
    }


    fun findClickableParent(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var current = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    private fun findByText(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (node == null) return null

        if (
            node.text?.toString()?.contains(text, true) == true ||
            node.contentDescription?.toString()?.contains(text, true) == true
        ) {
            return node
        }

        for (i in 0 until node.childCount) {
            val result = findByText(node.getChild(i), text)
            if (result != null) return result
        }

        return null
    }


    private fun findSearchBox(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        // Try different combinations
        if ((node.className?.contains("EditText") == true || node.className?.contains("Text") == true)
            && (node.contentDescription?.contains("Search", true) == true
                    || node.hintText?.contains("Search", true) == true
                    || node.text?.contains("Search", true) == true)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findSearchBox(child)
            if (result != null) return result
        }

        return null
    }


    private fun logNodeTree(node: AccessibilityNodeInfo?, indent: String = "") {
        if (node == null) return

        val className = node.className
        val text = node.text
        val contentDesc = node.contentDescription
        val viewId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) node.viewIdResourceName else "N/A"

        Log.d("NodeTree", "$indent- Class: $className, Text: $text, ContentDesc: $contentDesc, ID: $viewId")

        for (i in 0 until node.childCount) {
            logNodeTree(node.getChild(i), "$indent  ")
        }
    }

    fun performBackAction() {
        val success = performGlobalAction(GLOBAL_ACTION_BACK)
        Log.d("Accessibility", "Back action result: $success")
    }


}
