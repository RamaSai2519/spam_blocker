package com.spam_blocker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BlockedNumbersGroupAdapter extends RecyclerView.Adapter<BlockedNumbersGroupAdapter.GroupViewHolder> {

    private Context context;
    private List<GroupedBlockedNumber> groupedNumbers;
    private List<GroupedBlockedNumber> filteredNumbers;
    private OnBlockedNumberDeleteListener deleteListener;
    private OnBlockedNumberAllowListener allowListener;
    private String searchQuery = "";

    public enum SortOrder {
        RECENT_FIRST,
        CONTACTS_FIRST
    }

    private SortOrder currentSortOrder = SortOrder.RECENT_FIRST;

    public interface OnBlockedNumberDeleteListener {
        void onDelete(BlockedNumber blockedNumber);
    }

    public interface OnBlockedNumberAllowListener {
        void onAddToAllowList(String phoneNumber);
    }

    public BlockedNumbersGroupAdapter(Context context,
            OnBlockedNumberDeleteListener deleteListener,
            OnBlockedNumberAllowListener allowListener) {
        this.context = context;
        this.deleteListener = deleteListener;
        this.allowListener = allowListener;
        this.groupedNumbers = new ArrayList<>();
        this.filteredNumbers = new ArrayList<>();
    }

    /**
     * Set the data from BlockedNumbersManager and group by phone number
     */
    public void setData(List<BlockedNumber> blockedNumbers) {
        // Group blocked numbers by phone number
        Map<String, GroupedBlockedNumber> groups = new HashMap<>();

        for (BlockedNumber blockedNumber : blockedNumbers) {
            String phoneNumber = blockedNumber.getPhoneNumber();
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                phoneNumber = "Unknown";
            }

            GroupedBlockedNumber group = groups.get(phoneNumber);
            if (group == null) {
                // Don't fetch contact name here - will be loaded asynchronously
                group = new GroupedBlockedNumber(phoneNumber, null);
                groups.put(phoneNumber, group);
            }
            group.addBlockedEntry(blockedNumber);
        }

        // Convert to list
        this.groupedNumbers = new ArrayList<>(groups.values());

        // Apply current search filter and sort
        applyFilter();
    }

    /**
     * Filter the list by search query
     */
    public void filter(String query) {
        this.searchQuery = query == null ? "" : query.toLowerCase().trim();
        applyFilter();
    }

    private void applyFilter() {
        filteredNumbers.clear();

        if (searchQuery.isEmpty()) {
            filteredNumbers.addAll(groupedNumbers);
        } else {
            for (GroupedBlockedNumber group : groupedNumbers) {
                // Search by contact name
                if (group.getContactName() != null &&
                        group.getContactName().toLowerCase().contains(searchQuery)) {
                    filteredNumbers.add(group);
                    continue;
                }

                // Search by phone number
                if (group.getPhoneNumber() != null &&
                        group.getPhoneNumber().contains(searchQuery)) {
                    filteredNumbers.add(group);
                }
            }
        }

        // Apply sorting
        applySorting();

        notifyDataSetChanged();
    }

    private void applySorting() {
        if (currentSortOrder == SortOrder.CONTACTS_FIRST) {
            // Sort: contacts first, then by most recent
            java.util.Collections.sort(filteredNumbers, (g1, g2) -> {
                boolean g1HasContact = g1.hasContactName();
                boolean g2HasContact = g2.hasContactName();

                // Contacts first
                if (g1HasContact && !g2HasContact) {
                    return -1;
                } else if (!g1HasContact && g2HasContact) {
                    return 1;
                }

                // Within same category, sort by most recent
                BlockedNumber e1 = g1.getLatestEntry();
                BlockedNumber e2 = g2.getLatestEntry();
                if (e1 != null && e2 != null) {
                    return Long.compare(e2.getTimestamp(), e1.getTimestamp());
                }
                return 0;
            });
        }
        // RECENT_FIRST is the default - already sorted by timestamp from manager
    }

    /**
     * Set the sort order
     */
    public void setSortOrder(SortOrder sortOrder) {
        this.currentSortOrder = sortOrder;
        applyFilter();
    }

    /**
     * Get current sort order
     */
    public SortOrder getSortOrder() {
        return currentSortOrder;
    }

    /**
     * Update contact name for a specific phone number and refresh display
     */
    public void updateContactName(String phoneNumber, String contactName) {
        for (GroupedBlockedNumber group : groupedNumbers) {
            if (group.getPhoneNumber().equals(phoneNumber)) {
                group.setContactName(contactName);
                break;
            }
        }
        applyFilter(); // Refresh to apply changes and resort
    }

    /**
     * Load contact names asynchronously for all groups
     */
    public void loadContactNamesAsync(Runnable onComplete) {
        new Thread(() -> {
            for (GroupedBlockedNumber group : groupedNumbers) {
                if (!group.isContactNameLoaded()) {
                    String contactName = ContactsHelper.getContactName(context, group.getPhoneNumber());
                    group.setContactName(contactName);
                }
            }

            // Notify on main thread
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    applyFilter(); // Refresh to show contact names
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
            }
        }).start();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_blocked_number_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        GroupedBlockedNumber group = filteredNumbers.get(position);

        // Set display name (contact name or phone number)
        holder.tvDisplayName.setText(group.getDisplayName());

        // Show phone number as subtitle if contact name exists
        if (group.getContactName() != null && !group.getContactName().isEmpty()) {
            holder.tvPhoneNumberSubtitle.setVisibility(View.VISIBLE);
            holder.tvPhoneNumberSubtitle.setText(group.getPhoneNumber());
        } else {
            holder.tvPhoneNumberSubtitle.setVisibility(View.GONE);
        }

        // Set blocked count
        int count = group.getBlockedCount();
        holder.tvBlockedCount.setText(String.valueOf(count));

        // Set latest entry info
        BlockedNumber latestEntry = group.getLatestEntry();
        if (latestEntry != null) {
            holder.tvLatestTime.setText(formatTimestamp(latestEntry.getTimestamp()));

            String reason = latestEntry.getReason();
            if (reason == null || reason.isEmpty()) {
                reason = "Spam detected";
            }
            holder.tvLatestReason.setText(reason);
        }

        // Set expand/collapse icon
        if (group.isExpanded()) {
            holder.ivExpandIcon.setRotation(180f);
            holder.layoutExpandedContent.setVisibility(View.VISIBLE);
        } else {
            holder.ivExpandIcon.setRotation(0f);
            holder.layoutExpandedContent.setVisibility(View.GONE);
        }

        // Setup nested RecyclerView for expanded entries
        if (group.isExpanded()) {
            BlockedEntriesAdapter entriesAdapter = new BlockedEntriesAdapter(
                    group.getBlockedEntries());
            holder.rvBlockedEntries.setLayoutManager(new LinearLayoutManager(context));
            holder.rvBlockedEntries.setAdapter(entriesAdapter);
        }

        // Handle expand/collapse icon click
        holder.ivExpandIcon.setOnClickListener(v -> {
            group.setExpanded(!group.isExpanded());
            notifyItemChanged(position);
        });

        // Handle allow button for the entire group
        holder.btnAllowGroup.setOnClickListener(v -> {
            if (allowListener != null) {
                allowListener.onAddToAllowList(group.getPhoneNumber());
            }
        });

        // Handle delete button for the entire group
        holder.btnDeleteGroup.setOnClickListener(v -> {
            // Delete all entries in this group
            if (deleteListener != null && latestEntry != null) {
                // Delete the first entry to trigger the group deletion dialog
                deleteListener.onDelete(latestEntry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredNumbers.size();
    }

    private String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        long now = System.currentTimeMillis();
        long dayInMillis = 24 * 60 * 60 * 1000;

        if (now - timestamp < dayInMillis) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return "Today " + timeFormat.format(date);
        } else if (now - timestamp < 2 * dayInMillis) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return "Yesterday " + timeFormat.format(date);
        } else {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            return dateTimeFormat.format(date);
        }
    }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutHeader;
        TextView tvDisplayName;
        TextView tvPhoneNumberSubtitle;
        TextView tvBlockedCount;
        ImageView ivExpandIcon;
        ImageButton btnAllowGroup;
        ImageButton btnDeleteGroup;
        TextView tvLatestTime;
        TextView tvLatestReason;
        LinearLayout layoutExpandedContent;
        RecyclerView rvBlockedEntries;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutHeader = itemView.findViewById(R.id.layout_header);
            tvDisplayName = itemView.findViewById(R.id.tv_display_name);
            tvPhoneNumberSubtitle = itemView.findViewById(R.id.tv_phone_number_subtitle);
            tvBlockedCount = itemView.findViewById(R.id.tv_blocked_count);
            ivExpandIcon = itemView.findViewById(R.id.iv_expand_icon);
            btnAllowGroup = itemView.findViewById(R.id.btn_allow_group);
            btnDeleteGroup = itemView.findViewById(R.id.btn_delete_group);
            tvLatestTime = itemView.findViewById(R.id.tv_latest_time);
            tvLatestReason = itemView.findViewById(R.id.tv_latest_reason);
            layoutExpandedContent = itemView.findViewById(R.id.layout_expanded_content);
            rvBlockedEntries = itemView.findViewById(R.id.rv_blocked_entries);
        }
    }

    /**
     * Inner adapter for displaying individual blocked entries within a group
     */
    private class BlockedEntriesAdapter extends RecyclerView.Adapter<BlockedEntriesAdapter.EntryViewHolder> {

        private List<BlockedNumber> entries;

        public BlockedEntriesAdapter(List<BlockedNumber> entries) {
            this.entries = entries;
        }

        @NonNull
        @Override
        public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_blocked_entry, parent, false);
            return new EntryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
            BlockedNumber entry = entries.get(position);

            // Set timestamp
            holder.tvEntryTimestamp.setText(formatTimestamp(entry.getTimestamp()));

            // Set reason
            String reason = entry.getReason();
            if (reason == null || reason.isEmpty()) {
                reason = "Spam detected";
            }
            holder.tvEntryReason.setText(reason);

            // Set caller info (optional)
            String callerInfo = entry.getCallerInfo();
            if (callerInfo != null && !callerInfo.isEmpty() && !callerInfo.equals(reason)) {
                holder.tvEntryCallerInfo.setVisibility(View.VISIBLE);
                holder.tvEntryCallerInfo.setText(callerInfo);
            } else {
                holder.tvEntryCallerInfo.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        private String formatTimestamp(long timestamp) {
            Date date = new Date(timestamp);
            long now = System.currentTimeMillis();
            long dayInMillis = 24 * 60 * 60 * 1000;

            if (now - timestamp < dayInMillis) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return "Today " + timeFormat.format(date);
            } else if (now - timestamp < 2 * dayInMillis) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return "Yesterday " + timeFormat.format(date);
            } else {
                SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                return dateTimeFormat.format(date);
            }
        }

        class EntryViewHolder extends RecyclerView.ViewHolder {
            TextView tvEntryTimestamp;
            TextView tvEntryReason;
            TextView tvEntryCallerInfo;

            public EntryViewHolder(@NonNull View itemView) {
                super(itemView);
                tvEntryTimestamp = itemView.findViewById(R.id.tv_entry_timestamp);
                tvEntryReason = itemView.findViewById(R.id.tv_entry_reason);
                tvEntryCallerInfo = itemView.findViewById(R.id.tv_entry_caller_info);
            }
        }
    }
}
