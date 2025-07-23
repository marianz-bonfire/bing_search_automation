
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

class AutoSearchWindowsPage extends StatefulWidget {
  final String url;

  const AutoSearchWindowsPage({super.key, required this.url});

  @override
  State<AutoSearchWindowsPage> createState() => _AutoSearchWindowsPageState();
}

class _AutoSearchWindowsPageState extends State<AutoSearchWindowsPage> {
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
