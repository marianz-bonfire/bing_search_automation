import 'package:shared_preferences/shared_preferences.dart';

class Preferences {
  Preferences._internal();
  static final Preferences instance = Preferences._internal();

  SharedPreferences? _prefs;

  // Keys
  static const String _keyAutoStart = 'auto_start';
  static const String _keyAutoSearch = 'auto_search';

  /// Initialize once at app start
  Future<void> init() async {
    _prefs ??= await SharedPreferences.getInstance();
  }

  bool get isAutoStartEnabled => _prefs?.getBool(_keyAutoStart) ?? false;

  Future<void> setAutoStart(bool value) async {
    await _prefs?.setBool(_keyAutoStart, value);
  }

  bool get isAutoSearchEnabled => _prefs?.getBool(_keyAutoSearch) ?? true;

  Future<void> setAutoSearch(bool value) async {
    await _prefs?.setBool(_keyAutoSearch, value);
  }
}
