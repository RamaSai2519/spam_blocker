package com.spam_blocker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class CallMonitorService extends Service {
    private static final String TAG = "CallMonitorService";
    private static final String CHANNEL_ID = "CallMonitorChannel";
    private static final int NOTIFICATION_ID = 1;

    private CallReceiver callReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "CallMonitorService created");

        // Create notification channel for Android O and above
        createNotificationChannel();

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification());

        // Register the CallReceiver dynamically
        registerCallReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "CallMonitorService started");
        return START_STICKY; // Service will be restarted if killed
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // This is not a bound service
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Call Monitor Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Monitors incoming calls to block spam");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Spam Blocker")
                .setContentText("Monitoring calls for spam")
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void registerCallReceiver() {
        if (callReceiver == null) {
            callReceiver = new CallReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
            filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);

            // For Android 8.0+ we need to register the receiver dynamically
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(callReceiver, filter, RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(callReceiver, filter);
            }

            Log.d(TAG, "CallReceiver registered successfully");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister the receiver when service is destroyed
        if (callReceiver != null) {
            try {
                unregisterReceiver(callReceiver);
                Log.d(TAG, "CallReceiver unregistered");
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "CallReceiver was not registered", e);
            }
            callReceiver = null;
        }

        Log.d(TAG, "CallMonitorService destroyed");
    }
}
