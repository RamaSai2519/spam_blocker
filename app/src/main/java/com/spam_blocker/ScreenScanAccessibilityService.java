package com.spam_blocker;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScreenScanAccessibilityService extends AccessibilityService {
    private static final String TAG = "ScreenAccessibility";
    private static final long SCREEN_SCAN_DELAY = 5000; // Wait 5 seconds before scanning
    private static final long MAX_WAIT_TIME = 10000; // 10 seconds total (5 + 5 buffer)

    // Common call app packages to prioritize
    private static final String[] CALL_APP_PACKAGES = {
            "com.android.dialer", // Default Android dialer
            "com.google.android.dialer", // Google Phone app
            "com.samsung.android.dialer", // Samsung dialer
            "com.truecaller", // Truecaller
            "com.android.incallui", // In-call UI
            "com.sec.android.app.callsetting", // Samsung call settings
            "com.oneplus.dialer", // OnePlus dialer
            "com.miui.securitycenter", // MIUI security (may show caller info)
            "com.xiaomi.xmsf" // Xiaomi services
    };

    private KeywordManager keywordManager;
    private Handler handler;
    private Runnable screenScanRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        keywordManager = new KeywordManager(this);
        handler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "Accessibility service created");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }

        CallStateManager callState = CallStateManager.getInstance();

        // Only process events when a call is incoming and we haven't checked yet
        if (!callState.isCallIncoming() || callState.hasCheckedCaller()) {
            return;
        }

        // Check if we're still within the maximum wait time
        if (callState.getCallDuration() > MAX_WAIT_TIME) {
            if (!callState.hasCheckedCaller()) {
                Log.d(TAG, "Maximum timeout reached - allowing call");
                callState.setCheckedCaller(true);
                CallReceiver.unmuteRinger(this);
            }
            return;
        }

        int eventType = event.getEventType();
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";

        // Listen to window changes from any app during incoming calls
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            Log.d(TAG, "Window event detected from: " + packageName +
                    ", call duration: " + callState.getCallDuration() + "ms");

            // Schedule screen scan after 5 seconds if not already scheduled
            scheduleScreenScan();
        }
    }

    private void scheduleScreenScan() {
        // Remove any existing screen scan callback
        if (screenScanRunnable != null) {
            handler.removeCallbacks(screenScanRunnable);
        }

        CallStateManager callState = CallStateManager.getInstance();
        long currentDuration = callState.getCallDuration();

        // Only schedule if we haven't reached the scan time yet
        if (currentDuration < SCREEN_SCAN_DELAY) {
            screenScanRunnable = new Runnable() {
                @Override
                public void run() {
                    performScreenScan();
                }
            };

            long delayTime = SCREEN_SCAN_DELAY - currentDuration;
            handler.postDelayed(screenScanRunnable, delayTime);
            Log.d(TAG, "Scheduled screen scan in " + delayTime + "ms");
        } else if (currentDuration < MAX_WAIT_TIME) {
            // We're past 5 seconds but before 10 seconds, scan immediately
            performScreenScan();
        }
    }

    private void performScreenScan() {
        CallStateManager callState = CallStateManager.getInstance();

        if (!callState.isCallIncoming() || callState.hasCheckedCaller()) {
            Log.d(TAG, "Screen scan cancelled - call state changed");
            return;
        }

        Log.d(TAG, "Starting comprehensive screen scan for caller information");

        Set<String> allScreenText = new HashSet<>();

        try {
            // Get all accessible windows
            List<AccessibilityWindowInfo> windows = getWindows();

            for (AccessibilityWindowInfo window : windows) {
                if (window != null) {
                    AccessibilityNodeInfo rootNode = window.getRoot();
                    if (rootNode != null) {
                        collectAllTextFromNode(rootNode, allScreenText);
                        rootNode.recycle();
                    }
                }
            }

            // If no windows available, try getting root node directly
            if (allScreenText.isEmpty()) {
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                if (rootNode != null) {
                    collectAllTextFromNode(rootNode, allScreenText);
                    rootNode.recycle();
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during screen scan", e);
        }

        // Process all collected text
        processScreenText(allScreenText);
    }

    private void collectAllTextFromNode(AccessibilityNodeInfo node, Set<String> textCollection) {
        if (node == null) {
            return;
        }

        // Skip nodes from our own package to prevent false positives
        String packageName = node.getPackageName() != null ? node.getPackageName().toString() : "";
        if ("com.spam_blocker".equals(packageName)) {
            return; // Don't scan our own app's content
        }

        // Collect text from current node
        CharSequence text = node.getText();
        if (text != null && text.length() > 0) {
            String textStr = text.toString().trim();
            if (textStr.length() > 1) { // Include all non-empty text
                textCollection.add(textStr);
            }
        }

        // Collect content description
        CharSequence contentDesc = node.getContentDescription();
        if (contentDesc != null && contentDesc.length() > 0) {
            String descStr = contentDesc.toString().trim();
            if (descStr.length() > 1) {
                textCollection.add(descStr);
            }
        }

        // Recursively collect from child nodes
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                collectAllTextFromNode(child, textCollection);
                child.recycle();
            }
        }
    }

    private void processScreenText(Set<String> allText) {
        CallStateManager callState = CallStateManager.getInstance();

        if (callState.hasCheckedCaller()) {
            return; // Already processed
        }

        Log.d(TAG, "Processing screen text: found " + allText.size() + " text elements");

        boolean spamDetected = false;
        String matchedKeyword = null;
        String matchedText = null;

        // Check all screen text for spam keywords
        for (String text : allText) {
            if (text == null || text.trim().isEmpty()) {
                continue;
            }

            // Skip common UI elements and system text
            if (isSystemUIElement(text)) {
                continue;
            }

            Log.d(TAG, "Checking text: '" + text + "'");

            if (keywordManager.containsKeyword(text)) {
                spamDetected = true;
                matchedText = text;
                // Find which keyword matched
                for (String keyword : keywordManager.getKeywords()) {
                    if (text.toLowerCase().contains(keyword.toLowerCase())) {
                        matchedKeyword = keyword;
                        break;
                    }
                }
                break; // Stop on first match
            }
        }

        // Mark as checked to prevent duplicate processing
        callState.setCheckedCaller(true);

        // Cancel any pending timeout
        if (screenScanRunnable != null) {
            handler.removeCallbacks(screenScanRunnable);
        }

        if (spamDetected) {
            Log.d(TAG, "SPAM DETECTED in text: '" + matchedText + "' (keyword: '" + matchedKeyword + "')");
            String reason = "Keyword match: " + matchedKeyword;
            CallReceiver.endCall(this, reason, matchedText);
        } else {
            Log.d(TAG, "No spam keywords found in screen text - allowing call");
            CallReceiver.unmuteRinger(this);
        }
    }

    private boolean isSystemUIElement(String text) {
        if (text == null)
            return true;

        String lowerText = text.toLowerCase().trim();

        // Filter out our own app's UI elements to prevent false positives
        if (lowerText.contains("spam blocker") ||
                lowerText.equals("spam blocker") ||
                lowerText.equals("test blocking functionality") ||
                lowerText.equals("grant dnd access") ||
                lowerText.equals("view blocked numbers") ||
                lowerText.contains("dnd access") ||
                lowerText.contains("do not disturb") ||
                lowerText.contains("accessibility service") ||
                lowerText.contains("blocked keywords") ||
                lowerText.contains("add keyword")) {
            return true;
        }

        // Filter out common system UI elements
        return lowerText.isEmpty() ||
                lowerText.matches("\\d+") || // Just numbers
                lowerText.matches("[+\\-\\d\\s()\\-]+") || // Phone number format
                lowerText.equals("calling") ||
                lowerText.equals("incoming call") ||
                lowerText.equals("answer") ||
                lowerText.equals("decline") ||
                lowerText.equals("reject") ||
                lowerText.equals("accept") ||
                lowerText.equals("end call") ||
                lowerText.equals("mute") ||
                lowerText.equals("speaker") ||
                lowerText.equals("add call") ||
                lowerText.equals("hold") ||
                lowerText.equals("keypad") ||
                lowerText.equals("contacts") ||
                lowerText.equals("message") ||
                lowerText.equals("call") ||
                lowerText.length() < 2; // Very short text
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && screenScanRunnable != null) {
            handler.removeCallbacks(screenScanRunnable);
        }
        Log.d(TAG, "Accessibility service destroyed");
    }
}
