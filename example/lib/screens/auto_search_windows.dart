import 'dart:io';
import 'package:bing_search_automation_example/core/auto_start_manager.dart';
import 'package:bing_search_automation_example/core/preferences.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:flutter/services.dart';
import 'package:bing_search_automation_example/utils/local_webserver.dart';

enum ContentView { main, settings }

class AutoSearchWindowsPage extends StatefulWidget {
  const AutoSearchWindowsPage({super.key});

  @override
  State<AutoSearchWindowsPage> createState() => _AutoSearchWindowsPageState();
}

class _AutoSearchWindowsPageState extends State<AutoSearchWindowsPage> {
  String serverUrl = '';
  bool isLoading = false;
  bool serverRunning = false;
  String statusMessage = 'Ready to launch';
  Color statusColor = Colors.blue;
  Uint8List? logoImage;
  Uint8List? backgroundImage;
  ContentView view = ContentView.settings;

  bool isAutoStartEnabled = false;
  bool isAutoSearchEnabled = false;
  @override
  void initState() {
    super.initState();
    _checkServerStatus();
    _loadImages();
  }

  Future<void> _loadImages() async {
    try {
      // Load logo image
      final logoData = await rootBundle.load('assets/web/img/rewards.png');
      setState(() {
        logoImage = logoData.buffer.asUint8List();
      });

      // Load background image
      final bgData = await rootBundle.load('assets/web/img/bg.png');
      setState(() {
        backgroundImage = bgData.buffer.asUint8List();
      });
    } catch (e) {
      print('Error loading images: $e');
    }
  }

  Future<void> _checkServerStatus() async {
    isAutoSearchEnabled = Preferences.instance.isAutoSearchEnabled;
    isAutoStartEnabled = Preferences.instance.isAutoStartEnabled;

    if (LocalWebServer.isRunning) {
      setState(() {
        serverRunning = true;
        serverUrl = LocalWebServer.serverUrl;
        statusMessage = 'Server running on port ${LocalWebServer.port}';
        statusColor = Colors.green;
      });
    }
    if (isAutoSearchEnabled) {
      _startAndOpen();
    }
  }

  Future<void> _launchServer() async {
    if (Platform.isWindows) {
      setState(() {
        isLoading = true;
        statusMessage = 'Starting local server...';
        statusColor = Colors.orange;
      });

      try {
        final url = await LocalWebServer.init();

        setState(() {
          serverUrl = url;
          serverRunning = true;
          isLoading = false;
          statusMessage = 'Server running on ${_getHostFromUrl(url)}';
          statusColor = Colors.green;
        });

        // Show success snackbar
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Local server started successfully!'),
              backgroundColor: Colors.green,
              duration: Duration(seconds: 2),
            ),
          );
        }
      } catch (e) {
        setState(() {
          isLoading = false;
          statusMessage = 'Failed to start server: ${e.toString()}';
          statusColor = Colors.red;
        });

        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Error: $e'), backgroundColor: Colors.red, duration: Duration(seconds: 3)),
          );
        }
      }
    }
  }

  Future<void> _stopServer() async {
    setState(() {
      isLoading = true;
      statusMessage = 'Stopping server...';
      statusColor = Colors.orange;
    });

    _openInBrowser(queryParams: '?stop=true');

    await Future.delayed(Duration(seconds: 1));
    await LocalWebServer.stop();

    setState(() {
      serverRunning = false;
      serverUrl = '';
      isLoading = false;
      statusMessage = 'Server stopped';
      statusColor = Colors.blue;
    });
  }

  Future<void> _startAndOpen() async {
    await _launchServer();

    const timeout = Duration(seconds: 10);
    const checkInterval = Duration(milliseconds: 300);

    final startTime = DateTime.now();

    while (!serverRunning) {
      await Future.delayed(checkInterval);

      if (DateTime.now().difference(startTime) > timeout) {
        print("Server failed to start within timeout.");
        return;
      }
    }

    _openInBrowser(queryParams: '?auto=true&limit=50&interval=5000&multitab=true');
  }

  Future<void> _copyToClipboard() async {
    if (serverUrl.isNotEmpty) {
      await Clipboard.setData(ClipboardData(text: serverUrl));
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('URL copied to clipboard'),
            backgroundColor: Colors.green,
            duration: Duration(seconds: 1),
          ),
        );
      }
    }
  }

  Future<void> _openInBrowser({String queryParams = ''}) async {
    if (serverUrl.isNotEmpty) {
      try {
        await launchUrl(Uri.parse('$serverUrl$queryParams'), mode: LaunchMode.externalApplication);
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text('Failed to open browser: $e'), backgroundColor: Colors.red));
        }
      }
    }
  }

  String _getHostFromUrl(String url) {
    try {
      final uri = Uri.parse(url);
      return '${uri.host}:${uri.port}';
    } catch (e) {
      return url;
    }
  }

  int _selectedIndex = 0;

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          // Background Image
          if (backgroundImage != null)
            Positioned.fill(child: Opacity(opacity: 0.05, child: Image.memory(backgroundImage!, fit: BoxFit.cover))),

          _selectedIndex == 0 ? _buildContent() : _buildSettings(),
        ],
      ),
      bottomNavigationBar: BottomNavigationBar(
        items: const <BottomNavigationBarItem>[
          BottomNavigationBarItem(icon: Icon(Icons.home), label: 'Home'),
          BottomNavigationBarItem(icon: Icon(Icons.settings), label: 'Settings'),
        ],
        currentIndex: _selectedIndex,
        onTap: _onItemTapped,
      ),
    );
  }

  Widget _buildContent() {
    return Padding(
      padding: const EdgeInsets.all(20.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            padding: const EdgeInsets.symmetric(vertical: 16),
            child: Column(
              children: [
                Text(
                  'Bing Search Local Server',
                  style: TextStyle(
                    fontWeight: FontWeight.w600,
                    fontSize: 28,
                    color: Theme.of(context).colorScheme.primary,
                  ),
                ),
                const SizedBox(height: 8),
                // Logo Image below title
                if (logoImage != null)
                  Container(
                    height: 80,
                    width: 80,

                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(12),
                      child: Image.memory(logoImage!, fit: BoxFit.contain),
                    ),
                  ),
              ],
            ),
          ),
          // Status Card
          Card(
            elevation: 4,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                children: [
                  Row(
                    children: [
                      Icon(
                        serverRunning ? Icons.cloud_done_outlined : Icons.cloud_off_outlined,
                        color: statusColor,
                        size: 32,
                      ),
                      SizedBox(width: 16),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              serverRunning ? 'Server Active' : 'Server Inactive',
                              style: TextStyle(fontWeight: FontWeight.w600, fontSize: 18),
                            ),
                            SizedBox(height: 4),
                            Text(statusMessage, style: TextStyle(color: Colors.grey[600], fontSize: 14)),
                          ],
                        ),
                      ),
                      Container(
                        padding: EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                        decoration: BoxDecoration(
                          color: statusColor.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Container(
                              width: 8,
                              height: 8,
                              decoration: BoxDecoration(color: statusColor, shape: BoxShape.circle),
                            ),
                            SizedBox(width: 6),
                            Text(
                              serverRunning ? 'ONLINE' : 'OFFLINE',
                              style: TextStyle(color: statusColor, fontWeight: FontWeight.w600, fontSize: 12),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  if (serverUrl.isNotEmpty) ...[
                    SizedBox(height: 20),
                    Container(
                      padding: EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: Colors.grey[50],
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: Colors.grey[200]!),
                      ),
                      child: Row(
                        children: [
                          Icon(Icons.link, color: Colors.grey[600], size: 20),
                          SizedBox(width: 12),
                          Expanded(
                            child: SelectableText(
                              serverUrl,
                              style: TextStyle(fontFamily: 'Monospace', fontSize: 14, color: Colors.grey[800]),
                            ),
                          ),
                          IconButton(
                            onPressed: _copyToClipboard,
                            icon: Icon(Icons.copy, color: Theme.of(context).colorScheme.primary, size: 20),
                            tooltip: 'Copy URL',
                          ),
                        ],
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),

          SizedBox(height: 24),

          // Control Buttons
          Row(
            children: [
              Expanded(
                child: ElevatedButton(
                  onPressed: isLoading ? null : (serverRunning ? _stopServer : _launchServer),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: serverRunning ? Colors.red : Color(0x99238828),
                    //foregroundColor: serverRunning ? Colors.red : null,
                    padding: EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    elevation: 0,
                    minimumSize: Size(double.infinity, 60),
                  ),
                  child:
                      isLoading
                          ? SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                          : Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(
                                serverRunning ? Icons.stop_circle : Icons.play_circle,
                                size: 20,
                                color: Colors.white,
                              ),
                              SizedBox(width: 8),
                              Text(
                                serverRunning ? 'Stop Server' : 'Start Server',
                                style: TextStyle(fontWeight: FontWeight.w600, color: Colors.white),
                              ),
                            ],
                          ),
                ),
              ),
              if (serverRunning) ...[
                SizedBox(width: 12),
                Expanded(
                  child: OutlinedButton(
                    onPressed: _openInBrowser,
                    style: OutlinedButton.styleFrom(
                      padding: EdgeInsets.symmetric(vertical: 16),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      side: BorderSide(color: Theme.of(context).colorScheme.primary),
                      minimumSize: Size(double.infinity, 60),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.public_sharp, size: 20),
                        SizedBox(width: 8),
                        Text('Open in Browser', style: TextStyle(fontWeight: FontWeight.w600)),
                      ],
                    ),
                  ),
                ),
              ],
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSettings() {
    return Padding(
      padding: const EdgeInsets.all(20.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // AppBar replacement with logo
          Container(
            padding: const EdgeInsets.symmetric(vertical: 16),
            child: Column(
              children: [
                Text(
                  'Bing Search Local Server',
                  style: TextStyle(
                    fontWeight: FontWeight.w600,
                    fontSize: 28,
                    color: Theme.of(context).colorScheme.primary,
                  ),
                ),
                const SizedBox(height: 8),
                // Logo Image below title
                if (logoImage != null)
                  Container(
                    height: 80,
                    width: 80,

                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(12),
                      child: Image.memory(logoImage!, fit: BoxFit.contain),
                    ),
                  ),
              ],
            ),
          ),

          // Information Section
          Expanded(
            child: ListView(
              physics: BouncingScrollPhysics(),
              children: [
                _buildInfoCard(
                  icon: Icons.power_settings_new,
                  title: 'Auto Start',
                  description: 'Automatically launch the application when Windows starts.',
                  child: Switch(
                    value: isAutoStartEnabled,
                    onChanged: (value) async {
                      await Preferences.instance.setAutoStart(value);
                      if (value) {
                        await AutoStartManager.enableAutoStart();
                      } else {
                        await AutoStartManager.disableAutoStart();
                      }

                      setState(() {
                        isAutoStartEnabled = value;
                      });
                    },
                  ),
                ),
                SizedBox(height: 12),
                _buildInfoCard(
                  icon: Icons.search,
                  title: 'Auto Search',
                  description: 'Automatically perform search with predefined random search keywords.',
                  child: Switch(
                    value: isAutoSearchEnabled,
                    onChanged: (value) async {
                      await Preferences.instance.setAutoSearch(value);

                      setState(() {
                        isAutoSearchEnabled = value;
                      });
                    },
                  ),
                ),
                SizedBox(height: 12),
                _buildInfoCard(
                  icon: Icons.public_sharp,
                  title: 'Web Assets',
                  description:
                      '${LocalWebServer.assetPath.substring(0, 10)}...${LocalWebServer.assetPath.substring(LocalWebServer.assetPath.length - 20, LocalWebServer.assetPath.length)}',
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoCard({required IconData icon, required String title, required String description, Widget? child}) {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Container(
              padding: EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.primary.withOpacity(0.1),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Icon(icon, color: Theme.of(context).colorScheme.primary, size: 24),
            ),
            SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (child != null) ...[
                    Row(
                      children: [
                        Text(title, style: TextStyle(fontWeight: FontWeight.w600, fontSize: 16)),
                        Spacer(),
                        child,
                      ],
                    ),
                  ] else ...[
                    Text(title, style: TextStyle(fontWeight: FontWeight.w600, fontSize: 16)),
                  ],
                  SizedBox(height: 4),
                  Text(description, style: TextStyle(color: Colors.grey[600], fontSize: 14)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
