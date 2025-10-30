package com.spam_blocker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BlockedNumbersAdapter extends RecyclerView.Adapter<BlockedNumbersAdapter.BlockedNumberViewHolder> {

    private BlockedNumbersManager blockedNumbersManager;
    private OnBlockedNumberDeleteListener deleteListener;

    public interface OnBlockedNumberDeleteListener {
        void onDelete(BlockedNumber blockedNumber);
    }

    public BlockedNumbersAdapter(BlockedNumbersManager blockedNumbersManager,
            OnBlockedNumberDeleteListener deleteListener) {
        this.blockedNumbersManager = blockedNumbersManager;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public BlockedNumberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_blocked_number, parent, false);
        return new BlockedNumberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BlockedNumberViewHolder holder, int position) {
        List<BlockedNumber> blockedNumbers = blockedNumbersManager.getBlockedNumbers();
        BlockedNumber blockedNumber = blockedNumbers.get(position);

        // Set phone number
        String phoneNumber = blockedNumber.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            phoneNumber = "Unknown Number";
        }
        holder.tvPhoneNumber.setText(phoneNumber);

        // Set timestamp
        String timeStr = formatTimestamp(blockedNumber.getTimestamp());
        holder.tvTimestamp.setText(timeStr);

        // Set reason
        String reason = blockedNumber.getReason();
        if (reason == null || reason.isEmpty()) {
            reason = "Spam detected";
        }
        holder.tvReason.setText(reason);

        // Set caller info (optional)
        String callerInfo = blockedNumber.getCallerInfo();
        if (callerInfo != null && !callerInfo.isEmpty() && !callerInfo.equals(reason)) {
            holder.layoutCallerInfo.setVisibility(View.VISIBLE);
            holder.tvCallerInfo.setText(callerInfo);
        } else {
            holder.layoutCallerInfo.setVisibility(View.GONE);
        }

        // Set delete button listener
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deleteListener != null) {
                    deleteListener.onDelete(blockedNumber);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return blockedNumbersManager.getBlockedNumbers().size();
    }

    private String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);

        // Check if it's today
        long now = System.currentTimeMillis();
        long dayInMillis = 24 * 60 * 60 * 1000;

        if (now - timestamp < dayInMillis) {
            // Today - show time only
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return "Today " + timeFormat.format(date);
        } else if (now - timestamp < 2 * dayInMillis) {
            // Yesterday
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return "Yesterday " + timeFormat.format(date);
        } else {
            // Older - show date and time
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            return dateTimeFormat.format(date);
        }
    }

    public static class BlockedNumberViewHolder extends RecyclerView.ViewHolder {
        TextView tvPhoneNumber;
        TextView tvTimestamp;
        TextView tvReason;
        TextView tvCallerInfo;
        LinearLayout layoutCallerInfo;
        ImageButton btnDelete;

        public BlockedNumberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPhoneNumber = itemView.findViewById(R.id.tv_phone_number);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvReason = itemView.findViewById(R.id.tv_reason);
            tvCallerInfo = itemView.findViewById(R.id.tv_caller_info);
            layoutCallerInfo = itemView.findViewById(R.id.layout_caller_info);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}