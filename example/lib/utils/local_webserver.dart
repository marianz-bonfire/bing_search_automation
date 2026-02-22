import 'dart:convert';
import 'dart:io';
import 'dart:async';

import 'package:flutter/foundation.dart';
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
  static String _currentAssetPath = '';

  // Custom static handler with proper MIME types
  static Future<Response> _staticHandler(Request request) async {
    final String path = request.url.path;

    // Determine the file path
    String filePath;
    if (path.isEmpty || path == '/' || path.endsWith('/')) {
      filePath = '$_currentAssetPath/index.html';
    } else {
      filePath = '$_currentAssetPath/$path';
    }

    final file = File(filePath);

    try {
      if (await file.exists()) {
        final bytes = await file.readAsBytes();
        String contentType = _getMimeType(filePath);

        return Response.ok(
          bytes,
          headers: {
            'Content-Type': contentType,
            'Cache-Control': 'no-store, no-cache, must-revalidate, max-age=0',
            'Pragma': 'no-cache',
            'Expires': '0',
            'Access-Control-Allow-Origin': '*',
          },
        );
      } else {
        return Response.notFound('File not found: $path');
      }
    } catch (e) {
      return Response.internalServerError(body: 'Error reading file: $e');
    }
  }

  static String _getMimeType(String filePath) {
    final extension = filePath.split('.').last.toLowerCase();

    switch (extension) {
      case 'css':
        return 'text/css';
      case 'js':
        return 'application/javascript';
      case 'png':
        return 'image/png';
      case 'jpg':
      case 'jpeg':
        return 'image/jpeg';
      case 'gif':
        return 'image/gif';
      case 'svg':
        return 'image/svg+xml';
      case 'ico':
      case 'icon':
        return 'image/x-icon';
      case 'webp':
        return 'image/webp';
      case 'woff':
        return 'font/woff';
      case 'woff2':
        return 'font/woff2';
      case 'ttf':
        return 'font/ttf';
      case 'eot':
        return 'application/vnd.ms-fontobject';
      case 'html':
      case 'htm':
        return 'text/html';
      case 'json':
        return 'application/json';
      case 'txt':
        return 'text/plain';
      default:
        return 'application/octet-stream';
    }
  }

  // Initialize server with error handling
  static Future<String> init({int port = 9080}) async {
    try {
      if (_isRunning && _server != null) {
        return _serverUrl;
      }

      _port = port;

      // Clear any existing server
      if (_server != null) {
        await stop();
      }

      String location;

      if (Platform.isAndroid || Platform.isIOS) {
        location = await _extractAssetsToTempDir();
      } else if (kDebugMode) {
        // In debug mode, use the actual assets directory
        location = '${Directory.current.path}/assets/web';
        if (!await Directory(location).exists()) {
          location = await _extractAssetsToTempDir();
        }
      } else {
        // For production desktop, extract to temp directory with proper structure
        location = await _extractAssetsToTempDir();
      }

      _currentAssetPath = location;
      debugPrint('Serving web assets from: $_currentAssetPath');

      // Verify the assets were extracted correctly
      await _verifyAssets();

      // Create and start the server
      final handler = _staticHandler;
      _server = await io.serve(handler, InternetAddress.loopbackIPv4, _port, shared: true);

      _isRunning = true;
      _serverUrl = 'http://${_server!.address.address}:${_server!.port}';

      debugPrint('Server running at $_serverUrl');

      return _serverUrl;
    } catch (e) {
      debugPrint('Error starting server: $e');
      if (e.toString().contains('Address already in use') && port < 9100) {
        return init(port: port + 1);
      }
      rethrow;
    }
  }

  static Future<void> _verifyAssets() async {
    debugPrint('\n=== Verifying Extracted Assets ===');

    // Check if directory exists
    final dir = Directory(_currentAssetPath);
    if (!await dir.exists()) {
      debugPrint('ERROR: Asset directory does not exist!');
      return;
    }

    // List all extracted files
    int fileCount = 0;
    await for (var entity in dir.list(recursive: true)) {
      if (entity is File) {
        fileCount++;
        final relativePath = entity.path.replaceFirst(_currentAssetPath, '');
        debugPrint('  Extracted: $relativePath');
      }
    }
  }

  static Future<String> _extractAssetsToTempDir() async {
    try {
      // Get temp directory
      final tempDir = await getTemporaryDirectory();

      final appName = 'bing_search_automation_example';
      final outputDir = Directory('${tempDir.path}/$appName/web_assets');

      debugPrint('Extracting assets to: ${outputDir.path}');

      // Check if assets already exist and are valid
      if (await outputDir.exists()) {
        final indexPath = File('${outputDir.path}/index.html');
        if (await indexPath.exists()) {
          debugPrint('Assets already exist, reusing existing extraction');

          // Verify CSS and JS directories have content
          final cssDir = Directory('${outputDir.path}/css');
          final jsDir = Directory('${outputDir.path}/js');

          if (await cssDir.exists() && await jsDir.exists()) {
            final cssFiles = await cssDir.list().toList();
            final jsFiles = await jsDir.list().toList();

            if (cssFiles.isNotEmpty && jsFiles.isNotEmpty) {
              debugPrint('CSS files: ${cssFiles.length}, JS files: ${jsFiles.length}');
              return outputDir.path;
            }
          }
        }

        // If validation fails, clean and re-extract
        debugPrint('Existing assets are incomplete, re-extracting...');
        await outputDir.delete(recursive: true);
      }

      await outputDir.create(recursive: true);

      // Load asset manifest
      final manifestContent = await rootBundle.loadString('AssetManifest.json');
      debugPrint(manifestContent);
      final Map<String, dynamic> manifestMap = jsonDecode(manifestContent);

      // Find all web assets
      final webAssets = manifestMap.keys.where((key) => key.startsWith('assets/web/')).toList();

      if (webAssets.isEmpty) {
        manifestMap.keys.take(20).forEach((key) => debugPrint('  $key'));
        throw Exception('No web assets found in assets/web/');
      }

      debugPrint('Found ${webAssets.length} assets in assets/web/');

      // Extract each asset maintaining directory structure
      int extractedCount = 0;
      for (final assetPath in webAssets) {
        try {
          final data = await rootBundle.load(assetPath);

          // Remove 'assets/web/' prefix to get the relative path
          final relativePath = assetPath.replaceFirst('assets/web/', '');

          // Create the file in the output directory
          final file = File('${outputDir.path}/$relativePath');

          // Ensure the directory exists
          await file.parent.create(recursive: true);

          // Write the file
          await file.writeAsBytes(data.buffer.asUint8List());
          extractedCount++;

          // Debug print first few files
          if (extractedCount <= 10) {
            debugPrint('  Extracted: $relativePath');
          }
        } catch (e) {
          debugPrint('Error extracting $assetPath: $e');
        }
      }

      debugPrint('Successfully extracted $extractedCount files');

      // Verify critical files exist
      final indexPath = File('${outputDir.path}/index.html');
      if (!await indexPath.exists()) {
        throw Exception('index.html was not extracted!');
      }

      // Double-check CSS directory
      final cssDir = Directory('${outputDir.path}/css');
      if (!await cssDir.exists()) {
        debugPrint('Creating css directory');
        await cssDir.create();
      }

      // List CSS files
      if (await cssDir.exists()) {
        final cssFiles = await cssDir.list().toList();
        debugPrint('CSS directory contains ${cssFiles.length} files');
        for (var file in cssFiles) {
          if (file is File) {
            debugPrint('  CSS File: ${file.path}');
          }
        }
      }

      // Double-check JS directory
      final jsDir = Directory('${outputDir.path}/js');
      if (!await jsDir.exists()) {
        debugPrint('Creating js directory');
        await jsDir.create();
      }

      // List JS files
      if (await jsDir.exists()) {
        final jsFiles = await jsDir.list().toList();
        debugPrint('JS directory contains ${jsFiles.length} files');
        for (var file in jsFiles) {
          if (file is File) {
            debugPrint('  JS File: ${file.path}');
          }
        }
      }

      // Double-check img directory
      final imgDir = Directory('${outputDir.path}/img');
      if (!await imgDir.exists()) {
        debugPrint('Creating img directory');
        await imgDir.create();
      }

      return outputDir.path;
    } catch (e) {
      debugPrint('Error extracting assets: $e');
      rethrow;
    }
  }

  static Future<void> stop() async {
    if (_server != null) {
      await _server!.close(force: true);
      _server = null;
      _isRunning = false;
    }

    debugPrint('Server stopped, assets preserved at: $_currentAssetPath');
  }

  static bool get isRunning => _isRunning;

  static String get serverUrl => _serverUrl;

  static int get port => _port;

  static String get assetPath => _currentAssetPath;
}
