import 'package:flutter/material.dart';
import 'dart:async';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

class SplashScreen extends StatefulWidget {
  final VoidCallback onInitializationComplete;

  const SplashScreen({
    super.key,
    required this.onInitializationComplete,
  });

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  @override
  void initState() {
    super.initState();
    _initializeApp();
  }

  Future<void> _initializeApp() async {
    await Future.delayed(const Duration(milliseconds: 3500));

    // Check notification permissions
    await _checkAndRequestPermissions();

    // Notify parent that initialization is complete
    widget.onInitializationComplete();
  }

  Future<void> _checkAndRequestPermissions() async {
    try {
      // Check notification permission status
      final status = await Permission.notification.status;

      if (!status.isGranted && !status.isPermanentlyDenied) {
        // Request notification permission
        await Permission.notification.request();
      }

      // You can add other permissions here if needed
      // For example: await Permission.storage.request();

    } catch (e) {
      debugPrint('Error checking permissions: $e');
      // Continue even if permission check fails
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              Colors.blue.shade50,
              Colors.white,
            ],
          ),
        ),
        child: Stack(
          children: [
            // Animated background elements
            Positioned(
              top: -50,
              right: -50,
              child: Container(
                width: 200,
                height: 200,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: RadialGradient(
                    colors: [
                      Colors.blue.shade100.withValues(alpha:0.3),
                      Colors.transparent,
                    ],
                  ),
                ),
              ),
            ),
            Positioned(
              bottom: -50,
              left: -50,
              child: Container(
                width: 200,
                height: 200,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: RadialGradient(
                    colors: [
                      Colors.blue.shade100.withValues(alpha:0.2),
                      Colors.transparent,
                    ],
                  ),
                ),
              ),
            ),

            // Main content
            Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  // Logo with animation
                  _buildAnimatedLogo(),
                  const SizedBox(height: 40),

                  // App title
                  Text(
                    'Tarsier Bing Search',
                    style: TextStyle(
                      fontSize: 32,
                      fontWeight: FontWeight.w700,
                      color: Colors.blue.shade800,
                      letterSpacing: -0.5,
                    ),
                  ),
                  const SizedBox(height: 8),

                  // Subtitle
                  Text(
                    'Bing Auto Search for Microsoft Rewards',
                    style: TextStyle(
                      fontSize: 16,
                      color: Colors.grey.shade600,
                    ),
                  ),
                  const SizedBox(height: 60),

                  // Loading indicator
                  _buildLoadingIndicator(),
                  const SizedBox(height: 30),

                  // Permission status
                  _buildPermissionStatus(),
                ],
              ),
            ),

            // Version info at bottom
            Positioned(
              bottom: 30,
              left: 0,
              right: 0,
              child: Column(
                children: [
                  Text(
                    'Version 1.0.0',
                    style: TextStyle(
                      fontSize: 14,
                      color: Colors.grey.shade500,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '© 2024 Local Web Server. All rights reserved.',
                    style: TextStyle(
                      fontSize: 12,
                      color: Colors.grey.shade400,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAnimatedLogo() {
    return Container(
      width: 150,
      height: 150,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            Colors.blue.shade100,
            Colors.blue.shade50,
          ],
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.blue.withValues(alpha:0.2),
            blurRadius: 20,
            spreadRadius: 5,
          ),
        ],
      ),
      child: Center(
        child: Container(
          width: 120,
          height: 120,

          child: ClipRRect(
            borderRadius: BorderRadius.circular(60),
            child: FutureBuilder<Uint8List?>(
              future: _loadLogoImage(),
              builder: (context, snapshot) {
                if (snapshot.hasData && snapshot.data != null) {
                  return Image.memory(
                    snapshot.data!,
                    fit: BoxFit.fitHeight,
                    width: 80,
                    height: 80,
                  );
                } else if (snapshot.hasError) {
                  // Fallback icon if image fails to load
                  return Icon(
                    Icons.public_sharp,
                    size: 60,
                    color: Colors.blue.shade600,
                  );
                } else {
                  // Loading placeholder
                  return Container(
                    width: 80,
                    height: 80,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: Colors.grey.shade200,
                    ),
                  );
                }
              },
            ),
          ),
        ),
      ),
    );
  }

  Future<Uint8List?> _loadLogoImage() async {
    try {
      final data = await rootBundle.load('assets/icon.png');
      return data.buffer.asUint8List();
    } catch (e) {
      print('Error loading logo: $e');
      return null;
    }
  }

  Widget _buildLoadingIndicator() {
    return SizedBox(
      width: 200,
      child: Column(
        children: [
          // Progress bar
          Container(
            height: 4,
            width: 200,
            decoration: BoxDecoration(
              color: Colors.grey.shade200,
              borderRadius: BorderRadius.circular(2),
            ),
            child: LayoutBuilder(
              builder: (context, constraints) {
                return AnimatedContainer(
                  duration: const Duration(milliseconds: 1000),
                  curve: Curves.easeInOut,
                  width: constraints.maxWidth * 0.7, // 70% progress
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: [
                        Colors.blue.shade600,
                        Colors.blue.shade400,
                      ],
                    ),
                    borderRadius: BorderRadius.circular(2),
                  ),
                );
              },
            ),
          ),
          const SizedBox(height: 12),

          // Loading text
          Text(
            'Initializing application...',
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey.shade600,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPermissionStatus() {
    return FutureBuilder<PermissionStatus>(
      future: Permission.notification.status,
      builder: (context, snapshot) {
        Color statusColor = Colors.grey;
        String statusText = 'Checking permissions...';
        IconData statusIcon = Icons.info;

        if (snapshot.hasData) {
          final status = snapshot.data!;

          switch (status) {
            case PermissionStatus.granted:
            case PermissionStatus.limited:
              statusColor = Colors.green;
              statusText = 'Notifications enabled';
              statusIcon = Icons.check_circle;
              break;
            case PermissionStatus.denied:
              statusColor = Colors.orange;
              statusText = 'Notifications pending';
              statusIcon = Icons.info;
              break;
            case PermissionStatus.permanentlyDenied:
              statusColor = Colors.red;
              statusText = 'Notifications disabled';
              statusIcon = Icons.cancel;
              break;
            default:
              statusColor = Colors.grey;
              statusText = 'Permission unknown';
          }
        }

        return Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          decoration: BoxDecoration(
            color: statusColor.withValues(alpha:0.1),
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: statusColor.withValues(alpha:0.3)),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                statusIcon,
                size: 16,
                color: statusColor,
              ),
              const SizedBox(width: 8),
              Text(
                statusText,
                style: TextStyle(
                  fontSize: 12,
                  color: statusColor,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}