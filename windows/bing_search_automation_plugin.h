#ifndef FLUTTER_PLUGIN_BING_SEARCH_AUTOMATION_PLUGIN_H_
#define FLUTTER_PLUGIN_BING_SEARCH_AUTOMATION_PLUGIN_H_

#include <flutter/method_channel.h>
#include <flutter/plugin_registrar_windows.h>

#include <memory>

namespace bing_search_automation {

class BingSearchAutomationPlugin : public flutter::Plugin {
 public:
  static void RegisterWithRegistrar(flutter::PluginRegistrarWindows *registrar);

  BingSearchAutomationPlugin();

  virtual ~BingSearchAutomationPlugin();

  // Disallow copy and assign.
  BingSearchAutomationPlugin(const BingSearchAutomationPlugin&) = delete;
  BingSearchAutomationPlugin& operator=(const BingSearchAutomationPlugin&) = delete;

  // Called when a method is called on this plugin's channel from Dart.
  void HandleMethodCall(
      const flutter::MethodCall<flutter::EncodableValue> &method_call,
      std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>> result);
};

}  // namespace bing_search_automation

#endif  // FLUTTER_PLUGIN_BING_SEARCH_AUTOMATION_PLUGIN_H_
