import 'dart:io';

import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'package:shelf/shelf.dart';
import 'package:shelf/shelf_io.dart' as io;
import 'package:shelf_static/shelf_static.dart';

class LocalWebServer {

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

  // Initialize server
  static Future<String> init({port = 9080}) async {
    String location;
    if (Platform.isAndroid || Platform.isIOS) {
      location = await extractAssetsToTempDir(); // mobile: use temp dir
    } else {
      location = '${Directory.current.path}/assets/web'; // desktop: use file system path
    }
    // Start server if not already running
    final staticHandler = createStaticHandler(location, defaultDocument: 'index.html');

    final handler = const Pipeline().addMiddleware(_noCacheMiddleware).addHandler(staticHandler);

    final server = await io.serve(handler, InternetAddress.loopbackIPv4, port);

    return 'http://${server.address.address}:${server.port}';
  }

  static Future<String> extractAssetsToTempDir() async {
    final tempDir = await getTemporaryDirectory();
    final outputDir = Directory('${tempDir.path}/web');
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
      final data = await rootBundle.load(assetPath);
      final file = File('${outputDir.path}/${assetPath.split('/').last}');
      await file.writeAsBytes(data.buffer.asUint8List());
    }

    return outputDir.path;
  }
}
