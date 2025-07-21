package com.marianz.bing_search_automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** BingSearchAutomationPlugin */
class BingSearchAutomationPlugin: FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private lateinit var context: Context

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    context = flutterPluginBinding.applicationContext
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "bing_search_automation")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    if (call.method == "getPlatformVersion") {
      result.success("Android ${android.os.Build.VERSION.RELEASE}")
    } else if (call.method == "setSearchQuery") {
      val query = call.argument<String>("query")
      if (query != null) {
        SearchAccessibilityService.searchQueue.clear()
        SearchAccessibilityService.searchQueue.add(query)
        result.success("1 query added")
      } else result.error("NULL_QUERY", "Query is null", null)
    } else if (call.method == "setSearchQueue") {
      val list = call.argument<List<String>>("queries")
      if (list != null) {
        SearchAccessibilityService.searchQueue.clear()
        SearchAccessibilityService.searchQueue.addAll(list)
        result.success("${list.size} queries queued")
      } else result.error("NULL_LIST", "No queries provided", null)

    } else if (call.method == "checkServiceEnabled") {
      result.success(isServiceEnabled(context))
    } else if (call.method == "getCurrentQuery") {
      result.success(SearchAccessibilityService.getCurrentQuery())
    } else if (call.method == "launchBing") {
      launchBingApp(context)
      result.success(null)
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  private fun isServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
    val enabledServices = android.provider.Settings.Secure.getString(
      context.contentResolver,
      android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    return enabledServices.contains(context.packageName + "/" + SearchAccessibilityService::class.java.name)
  }

  private fun launchBingApp(context: Context) {
    try {
      val launchIntent = context.packageManager.getLaunchIntentForPackage("com.microsoft.bing")
      if (launchIntent != null) {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)

        // Optional: mark that it was launched from your app
        SearchAccessibilityService.setLaunchedFromApp(true)

        Log.d("BingLauncher", "Launching Bing app.")
      } else {
        Log.e("BingLauncher", "Bing app not installed. Redirecting to Play Store.")

        // Fallback: redirect to Play Store
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("market://details?id=com.microsoft.bing")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
      }
    } catch (e: Exception) {
      Log.e("BingLauncher", "Error launching Bing: ${e.message}", e)
    }
  }

}
