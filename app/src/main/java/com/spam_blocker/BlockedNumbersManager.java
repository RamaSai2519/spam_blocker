package com.spam_blocker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages storage and retrieval of blocked phone numbers
 */
public class BlockedNumbersManager {
    private static final String TAG = "BlockedNumbersManager";
    private static final String PREFS_NAME = "BlockedNumbersPrefs";
    private static final String KEY_BLOCKED_NUMBERS = "blocked_numbers";

    private SharedPreferences prefs;
    private Context context;

    public BlockedNumbersManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Add a blocked number to the history
     */
    public void addBlockedNumber(String phoneNumber, String reason, String callerInfo) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            Log.w(TAG, "Cannot add blocked number: phone number is empty");
            return;
        }

        BlockedNumber blockedNumber = new BlockedNumber(
                phoneNumber.trim(),
                System.currentTimeMillis(),
                reason,
                callerInfo);

        Set<String> blockedNumbers = new HashSet<>(prefs.getStringSet(KEY_BLOCKED_NUMBERS, new HashSet<>()));
        blockedNumbers.add(blockedNumber.toStorageString());

        prefs.edit()
                .putStringSet(KEY_BLOCKED_NUMBERS, blockedNumbers)
                .apply();

        Log.d(TAG, "Added blocked number: " + phoneNumber + " (reason: " + reason + ")");
    }

    /**
     * Get all blocked numbers, sorted by most recent first
     */
    public List<BlockedNumber> getBlockedNumbers() {
        Set<String> blockedNumbersSet = prefs.getStringSet(KEY_BLOCKED_NUMBERS, new HashSet<>());
        List<BlockedNumber> blockedNumbers = new ArrayList<>();

        for (String storageString : blockedNumbersSet) {
            BlockedNumber blockedNumber = BlockedNumber.fromStorageString(storageString);
            if (blockedNumber != null) {
                blockedNumbers.add(blockedNumber);
            }
        }

        // Sort by timestamp, most recent first
        Collections.sort(blockedNumbers, new Comparator<BlockedNumber>() {
            @Override
            public int compare(BlockedNumber b1, BlockedNumber b2) {
                return Long.compare(b2.getTimestamp(), b1.getTimestamp());
            }
        });

        return blockedNumbers;
    }

    /**
     * Get count of blocked numbers
     */
    public int getBlockedCount() {
        return prefs.getStringSet(KEY_BLOCKED_NUMBERS, new HashSet<>()).size();
    }

    /**
     * Remove a specific blocked number entry
     */
    public void removeBlockedNumber(BlockedNumber blockedNumber) {
        if (blockedNumber == null)
            return;

        Set<String> blockedNumbers = new HashSet<>(prefs.getStringSet(KEY_BLOCKED_NUMBERS, new HashSet<>()));
        blockedNumbers.remove(blockedNumber.toStorageString());

        prefs.edit()
                .putStringSet(KEY_BLOCKED_NUMBERS, blockedNumbers)
                .apply();

        Log.d(TAG, "Removed blocked number: " + blockedNumber.getPhoneNumber());
    }

    /**
     * Clear all blocked numbers history
     */
    public void clearAllBlockedNumbers() {
        prefs.edit()
                .remove(KEY_BLOCKED_NUMBERS)
                .apply();

        Log.d(TAG, "Cleared all blocked numbers history");
    }

    /**
     * Check if a phone number has been blocked before
     */
    public boolean isNumberBlocked(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        List<BlockedNumber> blockedNumbers = getBlockedNumbers();
        for (BlockedNumber blockedNumber : blockedNumbers) {
            if (phoneNumber.equals(blockedNumber.getPhoneNumber())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get blocked numbers for a specific phone number
     */
    public List<BlockedNumber> getBlockedNumbersForPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<BlockedNumber> allBlocked = getBlockedNumbers();
        List<BlockedNumber> phoneBlocked = new ArrayList<>();

        for (BlockedNumber blockedNumber : allBlocked) {
            if (phoneNumber.equals(blockedNumber.getPhoneNumber())) {
                phoneBlocked.add(blockedNumber);
            }
        }

        return phoneBlocked;
    }
}