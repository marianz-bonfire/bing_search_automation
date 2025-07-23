package com.marianz.bing_search_automation

import android.annotation.TargetApi
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo


object NodeFinder {



    fun findFirstEditText(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        return findNodeByClass(node, "android.widget.EditText")
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun findFocusedEditText(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isFocused && node.className == "android.widget.EditText") return node

        for (i in 0 until node.childCount) {
            val result = findFocusedEditText(node.getChild(i))
            if (result != null) return result
        }
        return null
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun findByText(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.text?.toString().containsText(text) ||
            node.contentDescription?.toString().containsText(text)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val result = findByText(node.getChild(i), text)
            if (result != null) return result
        }

        return null
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun findNodeByClass(node: AccessibilityNodeInfo?, className: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.className == className) return node

        for (i in 0 until node.childCount) {
            val result = findNodeByClass(node.getChild(i), className)
            if (result != null) return result
        }
        return null
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun findFirstNonEmptyTextView(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        return findNodeWithCondition(node) {
            it.className == "android.widget.TextView" && !it.text.isNullOrBlank()
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun findClickableParent(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var current = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun findNodeWithCondition(
        node: AccessibilityNodeInfo?,
        condition: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (node == null) return null
        if (condition(node)) return node

        for (i in 0 until node.childCount) {
            val result = findNodeWithCondition(node.getChild(i), condition)
            if (result != null) return result
        }
        return null
    }

    fun String?.containsText(text: String): Boolean {
        return this?.contains(text, true) == true
    }
}