package com.marianz.bing_search_automation

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result as MethodResult


class BingSearchAutomationPlugin: FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private lateinit var context: Context

    companion object {
        var eventSink: EventChannel.EventSink? = null

        fun sendProgress(current: Int, total: Int) {
            Handler(Looper.getMainLooper()).post {
                eventSink?.success(mapOf("current" to current, "total" to total))
            }
        }

        fun sendCompleted() {
            Handler(Looper.getMainLooper()).post {
                eventSink?.success(mapOf("completed" to true))
            }
        }

    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "bing_search_automation")
        channel.setMethodCallHandler(this)

        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "bing_search_progress")
        eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                eventSink = events
            }

            override fun onCancel(arguments: Any?) {
                eventSink = null
            }
        })
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodResult) {
        when (call.method) {
            "getPlatformVersion" -> result.success("Android ${android.os.Build.VERSION.RELEASE}")
            "setSearchQuery" -> handleSetSearchQuery(call, result)
            "setSearchQueue" -> handleSetSearchQueue(call, result)
            "checkServiceEnabled" -> result.success(isServiceEnabled(context))
            "getCurrentQuery" -> result.success(SearchAccessibilityService.getCurrentQuery())
            "launchBing" -> handleLaunchBing(result)
            "setQueryType" -> handleSetQueryType(call, result)
            "enableLog" -> handleEnableLog(call, result)
            else -> result.notImplemented()
        }
    }

    private fun handleSetSearchQuery(call: MethodCall, result: MethodResult) {
        val query = call.argument<String>("query") ?: run {
            result.error("NULL_QUERY", "Query is null", null)
            return
        }
        SearchAccessibilityService.searchQueue.clear()
        SearchAccessibilityService.searchQueue.add(query)
        result.success("1 query added")
    }

    private fun handleSetSearchQueue(call: MethodCall, result: MethodResult) {
        val list = call.argument<List<String>>("queries")
        if (list != null) {
            SearchAccessibilityService.searchQueue.clear()
            SearchAccessibilityService.searchQueue.addAll(list as Collection<String>)
            SearchAccessibilityService.totalQueries = list.size

            result.success("${list.size} queries queued")
        } else result.error("NULL_LIST", "No queries provided", null)
    }

    private fun handleSetQueryType(call: MethodCall, result: MethodResult) {
        try {
            val typeStr = call.argument<String>("type") ?: run {
                result.error("NULL_TYPE", "Query type is null", null)
                return
            }

            val type = when (typeStr.uppercase()) {
                "SEARCH" -> QueryType.SEARCH
                "READ" -> QueryType.READ
                "ANSWER" -> QueryType.ANSWER
                else -> throw IllegalArgumentException("Unknown query type: $typeStr")
            }

            SearchAccessibilityService.setQueryType(type)
            //SearchAccessibilityService.getInstance()?.setQueryType(type)

            result.success(true)
        } catch (e: Exception) {
            result.error("INVALID_TYPE", "Invalid query type: ${e.message}", null)
        }
    }

    private fun handleEnableLog(call: MethodCall, result: MethodResult) {
        val enable = call.argument<Boolean>("enable") ?: run {
            result.error("NULL_VALUE", "Enable parameter is null", null)
            return
        }

        SearchAccessibilityService.setLoggingEnabled(enable)
        //SearchAccessibilityService.getInstance()?.enableLogging(enable)
        result.success(true)
    }

    private fun handleLaunchBing(result: MethodResult) {
        launchBingApp()
        result.success(null)
    }

    @TargetApi(Build.VERSION_CODES.DONUT)
    private fun isServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(context.packageName + "/" + SearchAccessibilityService::class.java.name)
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private fun launchBingApp() {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.microsoft.bing")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)

                SearchAccessibilityService.setLaunchedFromApp(true)
                //SearchAccessibilityService.getInstance()?.setLaunchedFromApp(true)


                Logger.d("Launching Bing app.")
            } else {
                Logger.e("Bing app not installed. Redirecting to Play Store.")
                redirectToPlayStore()
            }
        } catch (e: Exception) {
            Logger.e("Error launching Bing: ${e.message}", e)
            redirectToPlayStore()
        }
    }

    private fun redirectToPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=com.microsoft.bing")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Logger.e("Error redirecting to Play Store: ${e.message}", e)
        }
    }
}