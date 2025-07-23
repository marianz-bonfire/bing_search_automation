package com.marianz.bing_search_automation


import android.accessibilityservice.AccessibilityService
import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import kotlin.random.Random


class ReadExecutor(private val service: AccessibilityService) {
    private val handler = Handler(Looper.getMainLooper())
    private var triggerSearchAttempt = 0
    private val MAX_TRIGGER_ATTEMPTS = 10
    private val RETRY_DELAY_MS = 300L

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    fun performRead(query: String, onComplete: () -> Unit) {
        val rootNode = service.rootInActiveWindow ?: return

    }

  }