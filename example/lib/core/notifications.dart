import 'package:flutter_local_notifications/flutter_local_notifications.dart';

final FlutterLocalNotificationsPlugin notifications = FlutterLocalNotificationsPlugin();

Future<void> initNotifications() async {
  const android = AndroidInitializationSettings('@mipmap/ic_launcher');
  const ios = DarwinInitializationSettings();
  const initSettings = InitializationSettings(android: android, iOS: ios);

  await notifications.initialize(initSettings);
}

Future<void> showProgressNotification(int current, int total) async {
  final androidDetails = AndroidNotificationDetails(
    'search_progress_channel',
    'Search Progress',
    channelDescription: 'Notifications for search automation progress',
    icon: '@mipmap/ic_launcher',
    importance: Importance.low, // progress notifications are usually low priority
    priority: Priority.low,
    //onlyAlertOnce: true,
    showProgress: true,
    maxProgress: total,
    progress: current,
  );

  final details = NotificationDetails(android: androidDetails);

  await notifications.show(
    0, // use same ID so it updates instead of stacking
    'Search Progress',
    'Completed $current of $total searches',
    details,
    payload: 'progress',
  );
}

Future<void> showCompletedNotification() async {
  // Final "complete" notification
  const androidDetails = AndroidNotificationDetails(
    'search_complete_channel',
    'Search Complete',
    channelDescription: 'Completion notification for search automation',
    importance: Importance.high,
    priority: Priority.high,
    icon: '@mipmap/ic_launcher',
  );
  const details = NotificationDetails(android: androidDetails);

  await notifications.show(1, 'Search Completed 🎉', 'All searches finished successfully!', details);
}
