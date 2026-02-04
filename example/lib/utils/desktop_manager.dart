import 'dart:io';
import 'dart:ui';

import 'package:window_manager/window_manager.dart';

class DesktopManager {
  static Future<void> init() async {
    if (Platform.isWindows) {
      await WindowManager.instance.ensureInitialized();
      windowManager.waitUntilReadyToShow().then((_) async {
        await windowManager.setTitle('Bing Search Local Server');
        await windowManager.setSize(const Size(400, 800));
        await windowManager.setMinimumSize(const Size(400, 800));

        await windowManager.center();
        await windowManager.show();
        await windowManager.setPreventClose(false);
        await windowManager.setSkipTaskbar(false);
      });
    }
  }
}