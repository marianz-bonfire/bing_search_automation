import 'dart:io';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'package:shelf/shelf.dart';
import 'package:shelf/shelf_io.dart' as io;
import 'package:shelf_static/shelf_static.dart';

class LocalWebServer {
  static HttpServer? _server;
  static int _port = 9080;
  static bool _isRunning = false;
  static String _serverUrl = '';

  // tells WebView2 not to cache responses
  static Middleware get _noCacheMiddleware {
    return (Handler innerHandler) {
      return (Request request) async {
        final response = await innerHandler(request);
        return response.change(
          headers: {
            ...response.headers,
            'Cache-Control': 'no-store, no-cache, must-revalidate, max-age=0',
            'Pragma': 'no-cache',
            'Expires': '0',
          },
        );
      };
    };
  }

  // Initialize server with error handling
  static Future<String> init({int port = 9080}) async {
    try {
      if (_isRunning && _server != null) {
        return _serverUrl;
      }

      _port = port;
      String location;

      if (Platform.isAndroid || Platform.isIOS) {
        location = await _extractAssetsToTempDir(); // mobile: use temp dir
      } else {
        location = '${Directory.current.path}/assets/web'; // desktop: use file system path
      }

      // Start server if not already running
      final staticHandler = createStaticHandler(
        location,
        defaultDocument: 'index.html',
      );

      final handler = const Pipeline()
          .addMiddleware(_noCacheMiddleware)
          .addHandler(staticHandler);

      _server = await io.serve(
        handler,
        InternetAddress.loopbackIPv4,
        _port,
        shared: true,
      );

      _isRunning = true;
      _serverUrl = 'http://${_server!.address.address}:${_server!.port}';

      return _serverUrl;
    } catch (e) {
      // Try different port if default is busy
      if (e.toString().contains('Address already in use') && port < 9100) {
        return init(port: port + 1);
      }
      rethrow;
    }
  }

  static Future<void> stop() async {
    if (_server != null) {
      await _server!.close();
      _server = null;
      _isRunning = false;
    }
  }

  static bool get isRunning => _isRunning;
  static String get serverUrl => _serverUrl;
  static int get port => _port;

  static Future<String> _extractAssetsToTempDir() async {
    final tempDir = await getTemporaryDirectory();
    final outputDir = Directory('${tempDir.path}/web_assets');

    if (!outputDir.existsSync()) {
      outputDir.createSync(recursive: true);
    }

    // List of asset files you want to extract
    final files = [
      'assets/web/index.html',
      'assets/web/LICENSE',
      'assets/web/js/script.js',
      'assets/web/css/style.css',
      'assets/web/img/favicon.png',
      'assets/web/img/rewards.png',
    ];

    for (final assetPath in files) {
      try {
        final data = await rootBundle.load(assetPath);
        final fileName = assetPath.split('/').last;
        final file = File('${outputDir.path}/$fileName');
        await file.writeAsBytes(data.buffer.asUint8List());
      } catch (e) {
        print('Error extracting asset $assetPath: $e');
      }
    }

    return outputDir.path;
  }
}