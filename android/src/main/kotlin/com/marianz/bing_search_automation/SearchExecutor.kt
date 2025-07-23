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

class SearchExecutor(private val service: AccessibilityService) {
    private val handler = Handler(Looper.getMainLooper())
    private var triggerSearchAttempt = 0
    private val MAX_TRIGGER_ATTEMPTS = 10
    private val RETRY_DELAY_MS = 300L

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    fun clickHomeSearchBox() {
        val rootNode = service.rootInActiveWindow ?: return

        if (rootNode == null) {
            Logger.d("Root window is null")
            retryTriggerSearch()
            return
        }

        val searchNode = NodeFinder.findByText(rootNode, "Search")

        if (searchNode != null && searchNode.isClickable) {
            searchNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            triggerSearchAttempt = 0 // reset attempts after success
        } else {
            retryTriggerSearch()
        }
    }

    private fun retryTriggerSearch() {
        triggerSearchAttempt++
        if (triggerSearchAttempt <= MAX_TRIGGER_ATTEMPTS) {
            handler.postDelayed({
                clickHomeSearchBox()
            }, RETRY_DELAY_MS)
        } else {
            Logger.e("Max triggerSearchBox attempts reached. Giving up.")
            triggerSearchAttempt = 0 // reset
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    fun waitUntilSearchEditTextFound(
        maxAttempts: Int = 10,
        delayMillis: Long = 2000L,
        onSuccess: (AccessibilityNodeInfo) -> Unit,
        onFailure: () -> Unit
    ) {
        var attempt = 0

        val runnable = object : Runnable {
            override fun run() {
                val rootNode = service.rootInActiveWindow ?: return
                val editText = NodeFinder.findFirstEditText(rootNode)

                if (editText != null) {
                    onSuccess(editText)
                } else {
                    attempt++
                    if (attempt < maxAttempts) {
                        handler.postDelayed(this, delayMillis)
                    } else {
                        onFailure()
                    }
                }
            }
        }

        handler.post(runnable)
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    fun performSearch(query: String, onComplete: () -> Unit) {
        val rootNode = service.rootInActiveWindow ?: return
        val editText = NodeFinder.findFirstEditText(rootNode)

        if (editText != null) {
            val args = Bundle()
            Logger.d("Inputting valye: $query")

            args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                query
            )

            val success = editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            Logger.d("Set text result: $success")

            if (success) {
                handler.postDelayed({
                    // Lets try all possible way know on how to search after entering keyword
                    clickSearchButton()
                    pressEnterKey()
                    clickSearchOnKeyboard(rootNode)
                }, 1000)

                handler.postDelayed({
                    clickFirstTextViewUnderRecyclerView()
                }, 1000)

                handler.postDelayed({
                    waitForSearchTabs(
                        onFound = {
                            val randomDelay = (10_000..20_000).random()
                            handler.postDelayed({
                                onComplete()
                            }, randomDelay.toLong())
                        },
                        onTimeout = {
                            Logger.e("Tabs not found after waiting.")
                            onComplete()
                        }
                    )
                }, 1000)
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun clickSearchButton() {
        val rootNode = service.rootInActiveWindow ?: return
        val searchButton = NodeFinder.findByText(rootNode, "Search")

        if (searchButton != null && searchButton.isClickable) {
            val clicked = searchButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Logger.d("Search button clicked: $clicked")
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun pressEnterKey() {
        val root = service.rootInActiveWindow ?: return
        val focusedNode = NodeFinder.findFocusedEditText(root)
        focusedNode?.performAction(EditorInfo.IME_ACTION_SEARCH)
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun clickSearchOnKeyboard(root: AccessibilityNodeInfo?): Boolean {
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

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun clickFirstTextViewUnderRecyclerView() {
        val rootNode = service.rootInActiveWindow ?: return
        val recyclerViewNode = NodeFinder.findNodeByClass(rootNode, "androidx.recyclerview.widget.RecyclerView")

        recyclerViewNode?.let {
            NodeFinder.findFirstNonEmptyTextView(it)?.let { textView ->
                NodeFinder.findClickableParent(textView)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun areSearchTabsPresent(): Boolean {
        val rootNode = service.rootInActiveWindow ?: return false

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
        maxRetries: Int = 100,
        delayMillis: Long = 2000L,
        onFound: () -> Unit,
        onTimeout: () -> Unit
    ) {
        var retries = 0

        fun checkCondition() {
            if (areSearchTabsPresent()) {
                onFound()
            } else if (retries < maxRetries) {
                retries++
                handler.postDelayed(::checkCondition, delayMillis)
            } else {
                onTimeout()
            }
        }

        checkCondition()
    }
}