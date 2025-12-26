package com.spam_blocker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AllowListAdapter extends RecyclerView.Adapter<AllowListAdapter.ViewHolder> {

    private Context context;
    private List<String> phoneNumbers;
    private List<String> filteredPhoneNumbers;
    private OnRemoveListener removeListener;
    private String searchQuery = "";

    public interface OnRemoveListener {
        void onRemove(String phoneNumber);
    }

    public AllowListAdapter(Context context, OnRemoveListener removeListener) {
        this.context = context;
        this.removeListener = removeListener;
        this.phoneNumbers = new ArrayList<>();
        this.filteredPhoneNumbers = new ArrayList<>();
    }

    public void setData(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers != null ? new ArrayList<>(phoneNumbers) : new ArrayList<>();
        applyFilter();
    }

    public void filter(String query) {
        this.searchQuery = query == null ? "" : query.toLowerCase().trim();
        applyFilter();
    }

    private void applyFilter() {
        filteredPhoneNumbers.clear();

        if (searchQuery.isEmpty()) {
            filteredPhoneNumbers.addAll(phoneNumbers);
        } else {
            for (String phoneNumber : phoneNumbers) {
                // Check if phone number contains search query
                if (phoneNumber.toLowerCase().contains(searchQuery)) {
                    filteredPhoneNumbers.add(phoneNumber);
                    continue;
                }

                // Check contact name if available
                String contactName = ContactsHelper.getContactName(context, phoneNumber);
                if (contactName != null && contactName.toLowerCase().contains(searchQuery)) {
                    filteredPhoneNumbers.add(phoneNumber);
                }
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_allow_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String phoneNumber = filteredPhoneNumbers.get(position);

        // Get contact name if available
        String contactName = ContactsHelper.getContactName(context, phoneNumber);

        if (contactName != null && !contactName.isEmpty()) {
            holder.tvContactName.setText(contactName);
            holder.tvContactName.setVisibility(View.VISIBLE);
            holder.tvPhoneNumber.setText(phoneNumber);
        } else {
            holder.tvContactName.setVisibility(View.GONE);
            holder.tvPhoneNumber.setText(phoneNumber);
        }

        holder.btnRemove.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemove(phoneNumber);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredPhoneNumbers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvContactName;
        TextView tvPhoneNumber;
        ImageButton btnRemove;

        ViewHolder(View itemView) {
            super(itemView);
            tvContactName = itemView.findViewById(R.id.tv_contact_name);
            tvPhoneNumber = itemView.findViewById(R.id.tv_phone_number);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }
    }
}
