package com.marianz.bing_search_automation

import android.annotation.TargetApi
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

object Logger {
    private var enabled = false
    private var tag  = "BING_SEARCH_ACCESSIBILITY_SERVICE"

    fun enable(enabled: Boolean) {
        this.enabled = enabled
    }

    fun d(message: String) {
        if (enabled) Log.d(tag, message)
    }

    fun e(message: String, e: Exception? = null) {
        if (enabled) Log.e(tag, message, e)
    }

    // Add other log levels as needed (i, w, v, etc.)

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun logNodeTree(node: AccessibilityNodeInfo?, indent: String = "") {
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
}