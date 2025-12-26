package com.spam_blocker;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for looking up contact information from phone numbers
 */
public class ContactsHelper {
    private static final String TAG = "ContactsHelper";
    private static final Map<String, String> contactNameCache = new HashMap<>();

    /**
     * Get the contact name for a given phone number
     * Returns null if no contact is found
     */
    public static String getContactName(Context context, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return null;
        }

        // Check cache first
        synchronized (contactNameCache) {
            if (contactNameCache.containsKey(phoneNumber)) {
                return contactNameCache.get(phoneNumber);
            }
        }

        try {
            // Use PhoneLookup API which properly handles phone number matching
            android.net.Uri uri = android.net.Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    android.net.Uri.encode(phoneNumber));

            Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME },
                    null,
                    null,
                    null);

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                        if (nameIndex >= 0) {
                            String contactName = cursor.getString(nameIndex);
                            Log.d(TAG, "Found contact name for " + phoneNumber + ": " + contactName);
                            // Cache the result
                            synchronized (contactNameCache) {
                                contactNameCache.put(phoneNumber, contactName);
                            }
                            return contactName;
                        }
                    }
                } finally {
                    cursor.close();
                }
            }

            Log.d(TAG, "No contact found for " + phoneNumber);
            // Cache null result to avoid repeated lookups
            synchronized (contactNameCache) {
                contactNameCache.put(phoneNumber, null);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "No permission to read contacts", e);
        } catch (Exception e) {
            Log.e(TAG, "Error looking up contact name for " + phoneNumber, e);
        }

        return null;
    }

    /**
     * Normalize phone number by removing non-digit characters
     */
    private static String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        return phoneNumber.replaceAll("[^0-9+]", "");
    }

    /**
     * Format phone number for display
     */
    public static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "Unknown Number";
        }

        // Remove any existing formatting
        String digits = normalizePhoneNumber(phoneNumber);

        // Format based on length (assuming US format, adjust as needed)
        if (digits.length() == 10) {
            return String.format("(%s) %s-%s",
                    digits.substring(0, 3),
                    digits.substring(3, 6),
                    digits.substring(6, 10));
        } else if (digits.length() == 11 && digits.startsWith("1")) {
            return String.format("+1 (%s) %s-%s",
                    digits.substring(1, 4),
                    digits.substring(4, 7),
                    digits.substring(7, 11));
        }

        // Return as-is if doesn't match expected format
        return phoneNumber;
    }

    /**
     * Clear the contact name cache
     */
    public static void clearCache() {
        synchronized (contactNameCache) {
            contactNameCache.clear();
        }
    }

    /**
     * Prefetch contact names for a list of phone numbers in a background thread
     * 
     * @param context      The context
     * @param phoneNumbers The list of phone numbers
     * @param callback     Callback to notify when loading is complete
     */
    public static void prefetchContactNames(Context context, java.util.List<String> phoneNumbers, Runnable callback) {
        new Thread(() -> {
            for (String phoneNumber : phoneNumbers) {
                getContactName(context, phoneNumber);
            }
            if (callback != null) {
                callback.run();
            }
        }).start();
    }
}
