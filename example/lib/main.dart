import 'dart:math';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:bing_search_automation/bing_search_automation.dart';

import 'constants.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  final _bingSearchAutomationPlugin = BingSearchAutomation();

  @override
  void initState() {
    super.initState();
    initPlatformState();
    initAccessibilityService();
    setQueries();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      platformVersion = await _bingSearchAutomationPlugin.getPlatformVersion() ?? 'Unknown platform version';
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  Future<void> initAccessibilityService() async {
    final enabled = await _bingSearchAutomationPlugin.isServiceEnabled();
    if (!enabled) {
      await _bingSearchAutomationPlugin.openAccessibilitySettings();
    }
  }

  Future<void> setQueries() async {
    keywords.shuffle(Random());
    await _bingSearchAutomationPlugin.setSearchQueue(keywords);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Plugin example app')),
        body: Center(
          child: Column(
            children: [
              Text('Running on: $_platformVersion\n'),
              SizedBox(height: 20),
              ElevatedButton(
                onPressed: () async {
                  await _bingSearchAutomationPlugin.launchBing();
                },
                child: Text('Launch Bing'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
