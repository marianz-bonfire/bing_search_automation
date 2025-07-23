import 'package:bing_search_automation/QueryType.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:bing_search_automation/bing_search_automation.dart';
import 'package:bing_search_automation/bing_search_automation_platform_interface.dart';
import 'package:bing_search_automation/bing_search_automation_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockBingSearchAutomationPlatform
    with MockPlatformInterfaceMixin
    implements BingSearchAutomationPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<String?> getCurrentQuery() {
    // TODO: implement getCurrentQuery
    throw UnimplementedError();
  }

  @override
  Future<bool> isServiceEnabled() {
    // TODO: implement isServiceEnabled
    throw UnimplementedError();
  }

  @override
  Future<void> openAccessibilitySettings() {
    // TODO: implement openAccessibilitySettings
    throw UnimplementedError();
  }

  @override
  Future<void> setSearchQuery(String query) {
    // TODO: implement setSearchQuery
    throw UnimplementedError();
  }

  @override
  Future<void> setSearchQueue(List<String> queries) {
    // TODO: implement setSearchQueue
    throw UnimplementedError();
  }

  @override
  Future<void> launchBing() {
    // TODO: implement launchBing
    throw UnimplementedError();
  }

  @override
  Future<void> enableLog(bool enable) {
    // TODO: implement enableLog
    throw UnimplementedError();
  }

  @override
  Future<void> setQueryType(QueryType type) {
    // TODO: implement setQueryType
    throw UnimplementedError();
  }

  @override
  void listen({required void Function(int current, int total) onProgress}) {
    // TODO: implement listen
  }
}

void main() {
  final BingSearchAutomationPlatform initialPlatform = BingSearchAutomationPlatform.instance;

  test('$MethodChannelBingSearchAutomation is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelBingSearchAutomation>());
  });

  test('getPlatformVersion', () async {
    BingSearchAutomation bingSearchAutomationPlugin = BingSearchAutomation();
    MockBingSearchAutomationPlatform fakePlatform = MockBingSearchAutomationPlatform();
    BingSearchAutomationPlatform.instance = fakePlatform;

    expect(await bingSearchAutomationPlugin.getPlatformVersion(), '42');
  });
}
