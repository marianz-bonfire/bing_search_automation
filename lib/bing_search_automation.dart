
import 'package:bing_search_automation/query_type.dart';

import 'bing_search_automation_platform_interface.dart';

class BingSearchAutomation {
  Future<String?> getPlatformVersion() {
    return BingSearchAutomationPlatform.instance.getPlatformVersion();
  }

  Future<bool> isServiceEnabled() {
    return BingSearchAutomationPlatform.instance.isServiceEnabled();
  }

  Future<void> openAccessibilitySettings() {
    return BingSearchAutomationPlatform.instance.openAccessibilitySettings();
  }

  Future<void> setSearchQuery(String query) {
    return BingSearchAutomationPlatform.instance.setSearchQuery(query);
  }

  Future<void> setSearchQueue(List<String> queries) {
    return BingSearchAutomationPlatform.instance.setSearchQueue(queries);
  }

  Future<void> setQueryType(QueryType type) {
    return BingSearchAutomationPlatform.instance.setQueryType(type);
  }

  Future<void> enableLog(bool enable) {
    return BingSearchAutomationPlatform.instance.enableLog(enable);
  }

  Future<void> launchBing() {
    return BingSearchAutomationPlatform.instance.launchBing();
  }

  void listen({
    required void Function(int current, int total) onProgress,
  }) {
    BingSearchAutomationPlatform.instance.listen(onProgress: onProgress);
  }
}
