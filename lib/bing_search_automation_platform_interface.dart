import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'bing_search_automation_method_channel.dart';

abstract class BingSearchAutomationPlatform extends PlatformInterface {
  /// Constructs a BingSearchAutomationPlatform.
  BingSearchAutomationPlatform() : super(token: _token);

  static final Object _token = Object();

  static BingSearchAutomationPlatform _instance = MethodChannelBingSearchAutomation();

  /// The default instance of [BingSearchAutomationPlatform] to use.
  ///
  /// Defaults to [MethodChannelBingSearchAutomation].
  static BingSearchAutomationPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [BingSearchAutomationPlatform] when
  /// they register themselves.
  static set instance(BingSearchAutomationPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<String?> getCurrentQuery() {
    throw UnimplementedError('getCurrentQuery() has not been implemented.');
  }

  Future<void> setSearchQuery(String query) async {
    throw UnimplementedError('setSearchQuery() has not been implemented.');
  }

  Future<void> setSearchQueue(List<String> queries) async {
    throw UnimplementedError('setSearchQueue() has not been implemented.');
  }

  Future<bool> isServiceEnabled() async {
    throw UnimplementedError('isServiceEnabled() has not been implemented.');
  }

  Future<void> openAccessibilitySettings() {
    throw UnimplementedError('openAccessibilitySettings() has not been implemented.');
  }

  Future<void> launchBing() {
    throw UnimplementedError('launchBing() has not been implemented.');
  }
}
