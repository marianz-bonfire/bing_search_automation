package com.marianz.bing_search_automation


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


class ReadExecutor(private val service: AccessibilityService) {
    private val handler = Handler(Looper.getMainLooper())
    private var triggerSearchAttempt = 0
    private val MAX_TRIGGER_ATTEMPTS = 10
    private val RETRY_DELAY_MS = 300L

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    fun performRead(query: String, onComplete: () -> Unit) {
        val rootNode = service.rootInActiveWindow ?: return
        val rewards = waitForRewardsProgressBlocking(
            rootProvider = { rootNode },
            timeoutMs = 5000
        )

        if (rewards != null) {
            Log.d("RewardsInfo", "Found Rewards Progress: $rewards")
            Logger.logNodeTree(service.rootInActiveWindow)
        } else {
            Log.d("RewardsInfo", "Timeout: Rewards not found.")
        }

    }

    fun waitForRewardsProgressBlocking(
        rootProvider: () -> AccessibilityNodeInfo?,
        timeoutMs: Long = 5000,
        intervalMs: Long = 250
    ): String? {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val root = rootProvider()
            val result = NodeFinder.findRewardsProgress(root)
            if (result != null) return result

            try {
                Thread.sleep(intervalMs)
            } catch (e: InterruptedException) {
                e.printStackTrace()
                break
            }
        }

        return null
    }

}