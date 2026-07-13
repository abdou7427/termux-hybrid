package com.termux.app;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class HybridNotificationService extends NotificationListenerService {
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {}

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}
}
