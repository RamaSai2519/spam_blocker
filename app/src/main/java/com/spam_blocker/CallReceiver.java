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

    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        
        if (state == null) {
            return;
        }

        Log.d(TAG, "Phone state changed: " + state);

        if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            // Incoming call - mute immediately
            onIncomingCall(context);
        } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            // Call ended - restore ringer
            onCallEnded(context);
        }
    }

    private void onIncomingCall(Context context) {
        Log.d(TAG, "Incoming call detected - muting ringer");
        isCallActive = true;
        
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            previousRingerMode = audioManager.getRingerMode();
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            Log.d(TAG, "Ringer muted, previous mode: " + previousRingerMode);
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
        
        // Restore previous ringer mode
        if (audioManager != null) {
            audioManager.setRingerMode(previousRingerMode);
            Log.d(TAG, "Ringer restored to mode: " + previousRingerMode);
        }

        // Reset call state
        CallStateManager.getInstance().reset();
    }

    public static void unmuteRinger(Context context) {
        Log.d(TAG, "Unmuting ringer - call allowed");
        if (audioManager != null) {
            audioManager.setRingerMode(previousRingerMode);
        }
    }

    public static void endCall(Context context) {
        Log.d(TAG, "Ending call - spam detected");
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
                if (telecomManager != null) {
                    boolean result = telecomManager.endCall();
                    Log.d(TAG, "End call result: " + result);
                }
            } else {
                // Use reflection for older APIs
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
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
}
