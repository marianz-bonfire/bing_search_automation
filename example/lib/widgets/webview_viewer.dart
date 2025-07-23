import 'dart:io';

import 'package:flutter/material.dart';
import 'package:path/path.dart' as p;

import 'package:webview_windows/webview_windows.dart';

class WebViewViewer extends StatefulWidget {
  final String url;

  const WebViewViewer({super.key, required this.url});

  @override
  State<WebViewViewer> createState() => _WebViewViewerState();
}

class _WebViewViewerState extends State<WebViewViewer> {
  final WebviewController _controller = WebviewController();

  bool _initialized = false;
  bool _hasError = false;
  String _errorMessage = '';
  late final bool _isWindows;

  @override
  void initState() {
    super.initState();
    _isWindows = Platform.isWindows;
    WidgetsBinding.instance.addPostFrameCallback((_) => loadUrl());
  }

  Future<void> loadUrl() async {
    try {
      if (_isWindows) {
        await _controller.initialize();
        await _controller.loadUrl(widget.url);
      }

      if (mounted) setState(() => _initialized = true);
    } catch (error, stack) {
      if (mounted) {
        setState(() {
          _hasError = true;
          _initialized = true;
          _errorMessage = error.toString();
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!_initialized) {
      return Center(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [CircularProgressIndicator(), SizedBox(height: 10), Text('Initializing served PDF file...')],
        ),
      );
    }

    if (_hasError) {
      return Center(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [Text('Failed to load PDF'), SizedBox(height: 10), Text(_errorMessage)],
        ),
      );
    }

    return Webview(_controller);
  }

  @override
  void dispose() {
    if (_isWindows) {
      _controller.dispose();
    }
    super.dispose();
  }
}
