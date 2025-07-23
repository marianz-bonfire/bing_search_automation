package com.marianz.bing_search_automation

import android.util.Log

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
}