package com.spam_blocker;

/**
 * Data model representing a blocked phone number entry
 */
public class BlockedNumber {
    private String phoneNumber;
    private long timestamp;
    private String reason;
    private String callerInfo;

    public BlockedNumber(String phoneNumber, long timestamp, String reason, String callerInfo) {
        this.phoneNumber = phoneNumber;
        this.timestamp = timestamp;
        this.reason = reason;
        this.callerInfo = callerInfo;
    }

    // Default constructor for SharedPreferences serialization
    public BlockedNumber() {
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCallerInfo() {
        return callerInfo;
    }

    public void setCallerInfo(String callerInfo) {
        this.callerInfo = callerInfo;
    }

    /**
     * Convert to string for storage in SharedPreferences
     * Format: phoneNumber|timestamp|reason|callerInfo
     */
    public String toStorageString() {
        return phoneNumber + "|" + timestamp + "|" + (reason != null ? reason : "") + "|"
                + (callerInfo != null ? callerInfo : "");
    }

    /**
     * Create from storage string
     */
    public static BlockedNumber fromStorageString(String storageString) {
        if (storageString == null || storageString.isEmpty()) {
            return null;
        }

        String[] parts = storageString.split("\\|", 4);
        if (parts.length >= 3) {
            BlockedNumber blockedNumber = new BlockedNumber();
            blockedNumber.phoneNumber = parts[0];
            try {
                blockedNumber.timestamp = Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                blockedNumber.timestamp = System.currentTimeMillis();
            }
            blockedNumber.reason = parts.length > 2 ? parts[2] : "";
            blockedNumber.callerInfo = parts.length > 3 ? parts[3] : "";
            return blockedNumber;
        }

        return null;
    }
}