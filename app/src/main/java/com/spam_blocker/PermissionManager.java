package com.spam_blocker;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

/**
 * Manages various permissions required by the spam blocker app
 */
public class PermissionManager {
    private static final String TAG = "PermissionManager";

    /**
     * Check if the app has Do Not Disturb (Notification Policy) access
     */
    public static boolean hasNotificationPolicyAccess(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            return notificationManager != null && notificationManager.isNotificationPolicyAccessGranted();
        }
        // On older versions, permission is not required
        return true;
    }

    /**
     * Open the Do Not Disturb access settings page
     */
    public static void requestNotificationPolicyAccess(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(intent);
                Log.d(TAG, "Opened notification policy access settings");
            } catch (Exception e) {
                Log.e(TAG, "Failed to open notification policy access settings", e);
            }
        }
    }

    /**
     * Check if the app can change ringer mode (combines version check and
     * permission)
     */
    public static boolean canChangeRingerMode(Context context) {
        return hasNotificationPolicyAccess(context);
    }

    /**
     * Get a user-friendly explanation for why DND access is needed
     */
    public static String getDndAccessExplanation() {
        return "To automatically mute incoming spam calls, this app needs access to Do Not Disturb settings. " +
                "This allows the app to temporarily silence your phone when a spam call is detected, " +
                "then restore the normal ringer mode after blocking the call.\n\n" +
                "Look for 'Spam Blocker' in the list of apps that can access Do Not Disturb settings.";
    }
}