import 'package:android_intent_plus/android_intent.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'bing_search_automation_platform_interface.dart';

/// An implementation of [BingSearchAutomationPlatform] that uses method channels.
class MethodChannelBingSearchAutomation extends BingSearchAutomationPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('bing_search_automation');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<String?> getCurrentQuery() async {
    return await methodChannel.invokeMethod<String>('getCurrentQuery');
  }

  @override
  Future<void> setSearchQuery(String query) async {
    await methodChannel.invokeMethod('setSearchQuery', {'query': query});
  }

  @override
  Future<void> setSearchQueue(List<String> queries) async {
    await methodChannel.invokeMethod('setSearchQueue', {'queries': queries});
  }

  @override
  Future<bool> isServiceEnabled() async {
    final result = await methodChannel.invokeMethod('checkServiceEnabled');
    return result == true;
  }


  @override
  Future<void> launchBing() async {
    await methodChannel.invokeMethod('launchBing');
  }

  @override
  Future<void> openAccessibilitySettings() async {
    const intent = AndroidIntent(action: 'android.settings.ACCESSIBILITY_SETTINGS');
    await intent.launch();
  }
}
