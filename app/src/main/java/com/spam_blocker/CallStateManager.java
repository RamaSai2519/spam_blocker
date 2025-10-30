package com.spam_blocker;

import android.util.Log;

public class CallStateManager {
    private static final String TAG = "CallStateManager";
    private static CallStateManager instance;
    
    private boolean isCallIncoming = false;
    private boolean hasCheckedCaller = false;
    private long callStartTime = 0;
    
    private CallStateManager() {}
    
    public static synchronized CallStateManager getInstance() {
        if (instance == null) {
            instance = new CallStateManager();
        }
        return instance;
    }
    
    public synchronized void setCallIncoming(boolean incoming) {
        isCallIncoming = incoming;
        if (incoming) {
            callStartTime = System.currentTimeMillis();
            hasCheckedCaller = false;
            Log.d(TAG, "Call incoming, timer started");
        }
    }
    
    public synchronized boolean isCallIncoming() {
        return isCallIncoming;
    }
    
    public synchronized boolean hasCheckedCaller() {
        return hasCheckedCaller;
    }
    
    public synchronized void setCheckedCaller(boolean checked) {
        hasCheckedCaller = checked;
        Log.d(TAG, "Caller check status: " + checked);
    }
    
    public synchronized long getCallDuration() {
        if (callStartTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - callStartTime;
    }
    
    public synchronized void reset() {
        isCallIncoming = false;
        hasCheckedCaller = false;
        callStartTime = 0;
        Log.d(TAG, "Call state reset");
    }
}
