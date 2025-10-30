package com.spam_blocker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Method;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "CallReceiver";
    private static AudioManager audioManager;
    private static int previousRingerMode;
    private static boolean isCallActive = false;
    private static String currentPhoneNumber = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if (state == null) {
            return;
        }

        Log.d(TAG, "Phone state changed: " + state);

        if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            // Extract phone number from intent
            currentPhoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            Log.d(TAG, "Incoming call from: " + (currentPhoneNumber != null ? currentPhoneNumber : "Unknown"));
            // Incoming call - mute immediately
            onIncomingCall(context);
        } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            // Call ended - restore ringer
            onCallEnded(context);
            currentPhoneNumber = null; // Clear the phone number
        }
    }

    private void onIncomingCall(Context context) {
        Log.d(TAG, "Incoming call detected - muting ringer");
        isCallActive = true;

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            previousRingerMode = audioManager.getRingerMode();

            if (PermissionManager.canChangeRingerMode(context)) {
                try {
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    Log.d(TAG, "Ringer muted, previous mode: " + previousRingerMode);
                } catch (SecurityException se) {
                    Log.w(TAG, "Unexpected SecurityException despite permission check", se);
                }
            } else {
                Log.i(TAG, "Cannot mute ringer - Do Not Disturb access not granted");
            }
        }

        // Notify the accessibility service that a call is incoming
        CallStateManager.getInstance().setCallIncoming(true);
    }

    private void onCallEnded(Context context) {
        if (!isCallActive) {
            return;
        }

        Log.d(TAG, "Call ended - restoring ringer");
        isCallActive = false;

        // Restore previous ringer mode intelligently
        if (audioManager != null && PermissionManager.canChangeRingerMode(context)) {
            try {
                // Only restore to silent if the user originally had silent mode
                // Otherwise, keep normal/vibrate mode for future calls
                int currentMode = audioManager.getRingerMode();
                if (previousRingerMode == AudioManager.RINGER_MODE_SILENT) {
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    Log.d(TAG, "Ringer restored to silent mode (user's original preference)");
                } else if (previousRingerMode == AudioManager.RINGER_MODE_VIBRATE) {
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    Log.d(TAG, "Ringer restored to vibrate mode");
                } else {
                    // Keep current mode if it's already normal, or set to normal
                    if (currentMode != AudioManager.RINGER_MODE_NORMAL) {
                        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    }
                    Log.d(TAG, "Ringer restored to normal mode");
                }
            } catch (SecurityException se) {
                Log.w(TAG, "Unexpected SecurityException during restore", se);
            }
        } else if (audioManager != null) {
            Log.i(TAG, "Cannot restore ringer - Do Not Disturb access not granted");
        }

        // Reset call state
        CallStateManager.getInstance().reset();
    }

    public static void unmuteRinger(Context context) {
        Log.d(TAG, "Unmuting ringer - call allowed");
        if (audioManager != null && PermissionManager.canChangeRingerMode(context)) {
            try {
                // For legitimate calls, always restore to normal ringing mode
                // Don't use previousRingerMode as it might be silent (0)
                int targetMode = (previousRingerMode == AudioManager.RINGER_MODE_VIBRATE)
                        ? AudioManager.RINGER_MODE_VIBRATE
                        : AudioManager.RINGER_MODE_NORMAL;

                audioManager.setRingerMode(targetMode);
                Log.d(TAG, "Ringer unmuted successfully to mode: " + targetMode + " (was: " + previousRingerMode + ")");
            } catch (SecurityException se) {
                Log.w(TAG, "Unexpected SecurityException in unmuteRinger", se);
            }
        } else if (audioManager != null) {
            Log.i(TAG, "Cannot unmute ringer - Do Not Disturb access not granted");
        }
    }

    public static void endCall(Context context) {
        endCall(context, null, null);
    }

    public static void endCall(Context context, String reason, String callerInfo) {
        Log.d(TAG, "Ending call - spam detected. Reason: " + reason);

        // Store the blocked number
        if (currentPhoneNumber != null) {
            BlockedNumbersManager blockedManager = new BlockedNumbersManager(context);
            blockedManager.addBlockedNumber(currentPhoneNumber, reason, callerInfo);
            Log.d(TAG, "Stored blocked number: " + currentPhoneNumber);
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
                if (telecomManager != null) {
                    boolean result = telecomManager.endCall();
                    Log.d(TAG, "End call result: " + result);
                }
            } else {
                // Use reflection for older APIs
                TelephonyManager telephonyManager = (TelephonyManager) context
                        .getSystemService(Context.TELEPHONY_SERVICE);
                if (telephonyManager != null) {
                    Method method = telephonyManager.getClass().getDeclaredMethod("endCall");
                    method.setAccessible(true);
                    method.invoke(telephonyManager);
                    Log.d(TAG, "Call ended using reflection");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to end call", e);
        }
    }

    public static String getCurrentPhoneNumber() {
        return currentPhoneNumber;
    }
}
