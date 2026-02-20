import 'dart:io';

import 'package:bing_search_automation_example/core/preferences.dart';
import 'package:bing_search_automation_example/screens/auto_search_android.dart';
import 'package:bing_search_automation_example/screens/auto_search_windows.dart';
import 'package:bing_search_automation_example/screens/splash_screen.dart';
import 'package:bing_search_automation_example/utils/desktop_manager.dart';
import 'package:flutter/material.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Preferences.instance.init();
  await DesktopManager.init();

  runApp(BingAutoSearchApp());
}

class BingAutoSearchApp extends StatelessWidget {
  const BingAutoSearchApp({super.key});

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
      home: InitializationScreen(),
    );
  }
}

class InitializationScreen extends StatefulWidget {
  const InitializationScreen({super.key});

  @override
  State<InitializationScreen> createState() => _InitializationScreenState();
}

class _InitializationScreenState extends State<InitializationScreen> {
  bool _isInitialized = false;

  @override
  Widget build(BuildContext context) {
    return _isInitialized
        ? Platform.isAndroid
            ? AutoSearchAndroidPage()
            : AutoSearchWindowsPage()
        : SplashScreen(
          onInitializationComplete: () {
            setState(() {
              _isInitialized = true;
            });
          },
        );
  }
}
