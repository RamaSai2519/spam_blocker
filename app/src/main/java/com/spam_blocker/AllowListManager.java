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
 * Manages the allow list of phone numbers that should never be blocked
 */
public class AllowListManager {
    private static final String TAG = "AllowListManager";
    private static final String PREFS_NAME = "AllowListPrefs";
    private static final String KEY_ALLOW_LIST = "allow_list";

    private SharedPreferences prefs;
    private Context context;

    public AllowListManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Add a phone number to the allow list
     */
    public void addToAllowList(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            Log.w(TAG, "Cannot add to allow list: phone number is empty");
            return;
        }

        Set<String> allowList = new HashSet<>(prefs.getStringSet(KEY_ALLOW_LIST, new HashSet<>()));
        allowList.add(phoneNumber.trim());

        prefs.edit()
                .putStringSet(KEY_ALLOW_LIST, allowList)
                .apply();

        Log.d(TAG, "Added to allow list: " + phoneNumber);
    }

    /**
     * Remove a phone number from the allow list
     */
    public void removeFromAllowList(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return;
        }

        Set<String> allowList = new HashSet<>(prefs.getStringSet(KEY_ALLOW_LIST, new HashSet<>()));
        allowList.remove(phoneNumber.trim());

        prefs.edit()
                .putStringSet(KEY_ALLOW_LIST, allowList)
                .apply();

        Log.d(TAG, "Removed from allow list: " + phoneNumber);
    }

    /**
     * Check if a phone number is in the allow list
     */
    public boolean isInAllowList(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        Set<String> allowList = prefs.getStringSet(KEY_ALLOW_LIST, new HashSet<>());
        return allowList.contains(phoneNumber.trim());
    }

    /**
     * Get all numbers in the allow list
     */
    public List<String> getAllowList() {
        Set<String> allowListSet = prefs.getStringSet(KEY_ALLOW_LIST, new HashSet<>());
        List<String> allowList = new ArrayList<>(allowListSet);

        // Sort alphabetically
        Collections.sort(allowList, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });

        return allowList;
    }

    /**
     * Clear the entire allow list
     */
    public void clearAllowList() {
        prefs.edit()
                .remove(KEY_ALLOW_LIST)
                .apply();

        Log.d(TAG, "Cleared allow list");
    }

    /**
     * Get count of numbers in allow list
     */
    public int getAllowListCount() {
        return prefs.getStringSet(KEY_ALLOW_LIST, new HashSet<>()).size();
    }
}
