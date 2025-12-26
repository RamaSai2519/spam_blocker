package com.spam_blocker;

import java.util.ArrayList;
import java.util.List;

/**
 * Data model representing a group of blocked entries from the same phone number
 */
public class GroupedBlockedNumber {
    private String phoneNumber;
    private String contactName;
    private List<BlockedNumber> blockedEntries;
    private boolean isExpanded;
    private boolean isContactNameLoaded;

    public GroupedBlockedNumber(String phoneNumber, String contactName) {
        this.phoneNumber = phoneNumber;
        this.contactName = contactName;
        this.blockedEntries = new ArrayList<>();
        this.isExpanded = false;
        this.isContactNameLoaded = (contactName != null);
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
        this.isContactNameLoaded = true;
    }

    public boolean isContactNameLoaded() {
        return isContactNameLoaded;
    }

    public boolean hasContactName() {
        return contactName != null && !contactName.isEmpty();
    }

    public List<BlockedNumber> getBlockedEntries() {
        return blockedEntries;
    }

    public void addBlockedEntry(BlockedNumber blockedNumber) {
        this.blockedEntries.add(blockedNumber);
    }

    public int getBlockedCount() {
        return blockedEntries.size();
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    /**
     * Get the most recent blocked entry
     */
    public BlockedNumber getLatestEntry() {
        if (blockedEntries.isEmpty()) {
            return null;
        }
        return blockedEntries.get(0);
    }

    /**
     * Get display name - contact name if available, otherwise phone number
     */
    public String getDisplayName() {
        if (contactName != null && !contactName.isEmpty()) {
            return contactName;
        }
        return phoneNumber != null && !phoneNumber.isEmpty() ? phoneNumber : "Unknown Number";
    }
}
