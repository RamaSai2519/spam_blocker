package com.spam_blocker;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class TruecallerAccessibilityService extends AccessibilityService {
    private static final String TAG = "TruecallerAccessibility";
    private static final String TRUECALLER_PACKAGE = "com.truecaller";
    private static final long MAX_WAIT_TIME = 6000; // 6 seconds (slightly more than 5 to be safe)
    
    private KeywordManager keywordManager;
    private Handler handler;
    private Runnable timeoutRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        keywordManager = new KeywordManager(this);
        handler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "Accessibility service created");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) {
            return;
        }

        String packageName = event.getPackageName().toString();
        
        // Only process Truecaller events when a call is incoming
        if (!packageName.equals(TRUECALLER_PACKAGE)) {
            return;
        }

        CallStateManager callState = CallStateManager.getInstance();
        
        if (!callState.isCallIncoming() || callState.hasCheckedCaller()) {
            return;
        }

        // Check if we're still within the waiting period
        if (callState.getCallDuration() > MAX_WAIT_TIME) {
            if (!callState.hasCheckedCaller()) {
                Log.d(TAG, "Timeout reached without finding caller name - allowing call");
                callState.setCheckedCaller(true);
                CallReceiver.unmuteRinger(this);
            }
            return;
        }

        int eventType = event.getEventType();
        
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            Log.d(TAG, "Truecaller window event detected");
            
            // Try to extract caller name from Truecaller
            String callerName = extractCallerName(event);
            
            if (callerName != null && !callerName.isEmpty()) {
                processCallerName(callerName);
            } else {
                // Schedule a timeout check if we haven't found the name yet
                scheduleTimeoutCheck();
            }
        }
    }

    private String extractCallerName(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source == null) {
            return null;
        }

        String callerName = null;
        
        // Try to find text nodes that might contain the caller name
        callerName = findCallerNameInNode(source);
        
        source.recycle();
        return callerName;
    }

    private String findCallerNameInNode(AccessibilityNodeInfo node) {
        if (node == null) {
            return null;
        }

        // Check current node for text
        CharSequence text = node.getText();
        if (text != null && text.length() > 0) {
            String textStr = text.toString().trim();
            // Filter out common UI elements and keep potential names
            if (textStr.length() > 2 && 
                !textStr.equalsIgnoreCase("truecaller") &&
                !textStr.equalsIgnoreCase("calling") &&
                !textStr.equalsIgnoreCase("incoming call") &&
                !textStr.matches("\\d+") && // Not just digits
                !textStr.matches("[+\\-\\d\\s()]+")) { // Not phone number format
                Log.d(TAG, "Found potential caller name: " + textStr);
                return textStr;
            }
        }

        // Check content description
        CharSequence contentDesc = node.getContentDescription();
        if (contentDesc != null && contentDesc.length() > 2) {
            String descStr = contentDesc.toString().trim();
            if (!descStr.equalsIgnoreCase("truecaller") &&
                !descStr.matches("\\d+") &&
                !descStr.matches("[+\\-\\d\\s()]+")) {
                Log.d(TAG, "Found potential caller name in description: " + descStr);
                return descStr;
            }
        }

        // Recursively check child nodes
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                String result = findCallerNameInNode(child);
                child.recycle();
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private void processCallerName(String callerName) {
        CallStateManager callState = CallStateManager.getInstance();
        
        if (callState.hasCheckedCaller()) {
            return; // Already processed
        }

        Log.d(TAG, "Processing caller name: " + callerName);
        callState.setCheckedCaller(true);
        
        // Cancel any pending timeout
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
        }

        // Check if the name contains any blocked keywords
        boolean isSpam = keywordManager.containsKeyword(callerName);
        
        if (isSpam) {
            Log.d(TAG, "SPAM DETECTED: " + callerName);
            // Block the call - keep it muted and end it
            CallReceiver.endCall(this);
        } else {
            Log.d(TAG, "Caller allowed: " + callerName);
            // Allow the call - unmute ringer
            CallReceiver.unmuteRinger(this);
        }
    }

    private void scheduleTimeoutCheck() {
        // Remove any existing timeout callback
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
        }

        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                CallStateManager callState = CallStateManager.getInstance();
                if (callState.isCallIncoming() && !callState.hasCheckedCaller()) {
                    Log.d(TAG, "Timeout reached - no caller name found, allowing call");
                    callState.setCheckedCaller(true);
                    CallReceiver.unmuteRinger(TruecallerAccessibilityService.this);
                }
            }
        };

        // Schedule timeout
        long remainingTime = MAX_WAIT_TIME - CallStateManager.getInstance().getCallDuration();
        if (remainingTime > 0) {
            handler.postDelayed(timeoutRunnable, remainingTime);
            Log.d(TAG, "Scheduled timeout check in " + remainingTime + "ms");
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
        }
        Log.d(TAG, "Accessibility service destroyed");
    }
}
