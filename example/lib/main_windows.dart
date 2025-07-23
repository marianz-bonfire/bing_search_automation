import 'package:bing_search_automation_example/local_webserver.dart';
import 'package:bing_search_automation_example/widgets/webview_viewer.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  String serverUrl = await LocalWebServer.init();

  runApp(MyApp(serverUrl: serverUrl));
}

class MyApp extends StatelessWidget {
  final String serverUrl;

  const MyApp({super.key, required this.serverUrl});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(home: WebViewScreen(url: serverUrl));
  }
}

class WebViewScreen extends StatefulWidget {
  final String url;

  const WebViewScreen({super.key, required this.url});

  @override
  State<WebViewScreen> createState() => _WebViewScreenState();
}

class _WebViewScreenState extends State<WebViewScreen> {
  @override
  void initState() {
    super.initState();
    launch();
  }

  Future<void> launch() async {
    print(widget.url);
    if (!await launchUrl(Uri.parse(widget.url), mode: LaunchMode.externalApplication)) {
      throw 'Could not launch ${widget.url}';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Local HTML Viewer')),
      body: Center(child: Column(
        children: [
          ElevatedButton(onPressed: launch, child: Text('Launch')),
          //Expanded(child: WebViewViewer(url: widget.url)),
        ],
      )),
    );
  }
}
