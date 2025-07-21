import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:bing_search_automation/bing_search_automation_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelBingSearchAutomation platform = MethodChannelBingSearchAutomation();
  const MethodChannel channel = MethodChannel('bing_search_automation');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return '42';
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
