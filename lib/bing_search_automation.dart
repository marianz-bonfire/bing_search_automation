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

  Future<void> launchBing() {
    return BingSearchAutomationPlatform.instance.launchBing();
  }
}
