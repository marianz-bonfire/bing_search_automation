import 'dart:ffi';
import 'dart:io';
import 'package:win32/win32.dart';
import 'package:ffi/ffi.dart';

class AutoStartManager {
  static const String _appName = "MyFlutterApp";

  static String getExePath() {
    //return Platform.resolvedExecutable;
    return File(Platform.resolvedExecutable).path;
  }

  static Future<void> enableAutoStart() async {
    await _enableAutoStart(getExePath());
  }

  static Future<void> _enableAutoStart(String exePath) async {
    final hKey = calloc<HKEY>();
    final pathPtr = TEXT(r"Software\Microsoft\Windows\CurrentVersion\Run");

    final result = RegOpenKeyEx(HKEY_CURRENT_USER, pathPtr, 0, KEY_WRITE, hKey);

    if (result == ERROR_SUCCESS) {
      final valueName = TEXT(_appName);
      final data = TEXT('"$exePath"');

      RegSetValueEx(hKey.value, valueName, 0, REG_SZ, data.cast(), (data.length + 1) * 2);

      RegCloseKey(hKey.value);
      free(valueName);
      free(data);
    }

    free(pathPtr);
    calloc.free(hKey);
  }

  static Future<void> disableAutoStart() async {
    final hKey = calloc<HKEY>();
    final pathPtr = TEXT(r"Software\Microsoft\Windows\CurrentVersion\Run");

    if (RegOpenKeyEx(HKEY_CURRENT_USER, pathPtr, 0, KEY_WRITE, hKey) == ERROR_SUCCESS) {
      final valueName = TEXT(_appName);
      RegDeleteValue(hKey.value, valueName);
      RegCloseKey(hKey.value);
      free(valueName);
    }

    free(pathPtr);
    calloc.free(hKey);
  }

  static bool isAutoStartEnabled() {
    final hKey = calloc<HKEY>();
    final pathPtr = TEXT(r"Software\Microsoft\Windows\CurrentVersion\Run");

    if (RegOpenKeyEx(HKEY_CURRENT_USER, pathPtr, 0, KEY_READ, hKey) == ERROR_SUCCESS) {
      final valueName = TEXT(_appName);
      final buffer = calloc<Uint16>(260);
      final size = calloc<Uint32>()..value = 520;

      final result = RegQueryValueEx(hKey.value, valueName, nullptr, nullptr, buffer.cast(), size);

      RegCloseKey(hKey.value);
      free(valueName);
      free(buffer);
      calloc.free(size);

      return result == ERROR_SUCCESS;
    }

    return false;
  }
}
