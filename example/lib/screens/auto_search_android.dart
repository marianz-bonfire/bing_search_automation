
import 'dart:math';

import 'package:bing_search_automation/bing_search_automation.dart';
import 'package:bing_search_automation/query_type.dart';
import 'package:bing_search_automation_example/constants.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class AutoSearchAndroidPage extends StatefulWidget {
  const AutoSearchAndroidPage({super.key});

  @override
  State<AutoSearchAndroidPage> createState() => _AutoSearchAndroidPageState();
}

class _AutoSearchAndroidPageState extends State<AutoSearchAndroidPage> {
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
    _bingSearchAutomationPlugin.listen(onProgress: (current, total) {
      print('Progress $current of $total');
    });
    await _bingSearchAutomationPlugin.enableLog(true);
    await _bingSearchAutomationPlugin.setQueryType(QueryType.SEARCH);
    await _bingSearchAutomationPlugin.setSearchQueue(keywords);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFEFF5FB),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 24.0),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const SizedBox(height: 30),
                const Icon(Icons.emoji_events, size: 80, color: Colors.amber),
                const SizedBox(height: 20),
                const Text(
                  "Bing Auto Search for Microsoft Rewards",
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.w600,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(height: 30),
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [Color(0xFFB3D4FC), Color(0xFFCEE5FF)],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Column(
                    children: [
                      ElevatedButton.icon(
                        onPressed: () async {
                          await _bingSearchAutomationPlugin.launchBing();
                        },
                        icon: const Icon(Icons.play_arrow),
                        label: const Text("Click to Start Auto Search"),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.blue.shade700,
                          foregroundColor: Colors.white,
                          textStyle: const TextStyle(fontSize: 16),
                          padding: const EdgeInsets.symmetric(
                              vertical: 12, horizontal: 20),
                        ),
                      ),
                      const SizedBox(height: 10),
                      const Text(
                        "Auto Search Settings: 35 searches (default), "
                            "10~60 seconds (random) interval and Multi-tab Mode Enabled.",
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 14),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 30),
                GestureDetector(
                  onTap: () {
                    // Handle help link
                  },
                  child: const Text(
                    "Need help? (Tutorial and Settings)",
                    style: TextStyle(
                      color: Colors.blue,
                      fontWeight: FontWeight.bold,
                      decoration: TextDecoration.underline,
                    ),
                  ),
                ),
                const SizedBox(height: 20),
                const Text(
                  "This page is not affiliated with Microsoft, Rewards, Bing or Edge.",
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 13),
                ),
                const SizedBox(height: 10),
                const Text(
                  "Microsoft, Rewards, Bing and Edge are registered trademarks of "
                      "Microsoft Corporation in the United States of America and elsewhere.",
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 12),
                ),
                const SizedBox(height: 30),
                RichText(
                  textAlign: TextAlign.center,
                  text: TextSpan(
                    style: const TextStyle(fontSize: 12, color: Colors.black87),
                    children: [
                      const TextSpan(text: "Made by "),
                      const TextSpan(
                        text: "Tarsier Marianz\n",
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      const TextSpan(text: "This is a non-profit project and is licensed under open source terms on "),
                      TextSpan(
                        text: "GitHub.",
                        style: const TextStyle(
                          color: Colors.blue,
                          decoration: TextDecoration.underline,
                        ),
                        // Add actual GitHub link logic here
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 40),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

