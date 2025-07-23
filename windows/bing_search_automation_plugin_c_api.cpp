#include "include/bing_search_automation/bing_search_automation_plugin_c_api.h"

#include <flutter/plugin_registrar_windows.h>

#include "bing_search_automation_plugin.h"

void BingSearchAutomationPluginCApiRegisterWithRegistrar(
    FlutterDesktopPluginRegistrarRef registrar) {
  bing_search_automation::BingSearchAutomationPlugin::RegisterWithRegistrar(
      flutter::PluginRegistrarManager::GetInstance()
          ->GetRegistrar<flutter::PluginRegistrarWindows>(registrar));
}
