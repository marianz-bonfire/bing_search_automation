import 'dart:io';

import 'package:bing_search_automation_example/local_webserver.dart';
import 'package:bing_search_automation_example/screens/auto_search_android.dart';
import 'package:bing_search_automation_example/screens/auto_search_windows.dart';
import 'package:flutter/material.dart';

Future<void> main() async {
  String serverUrl = '';
  if (Platform.isWindows) {
    serverUrl = await LocalWebServer.init();
  }
  runApp(BingAutoSearchApp(url: serverUrl));
}

class BingAutoSearchApp extends StatelessWidget {
  final String url;
  const BingAutoSearchApp({super.key, required this.url});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Bing Auto Search',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        fontFamily: 'Roboto',
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
      ),
      home: Platform.isAndroid ? AutoSearchAndroidPage() : AutoSearchWindowsPage(url: url),
    );
  }
}
