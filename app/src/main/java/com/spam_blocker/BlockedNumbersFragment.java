package com.spam_blocker;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.List;

public class BlockedNumbersFragment extends Fragment {
    private RecyclerView rvBlockedNumbers;
    private TextView tvEmptyBlocked;
    private TextView tvTotalBlocked;
    private TextView tvTodayBlocked;
    private Button btnClearAll;
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private ImageButton btnSort;

    private BlockedNumbersGroupAdapter adapter;
    private BlockedNumbersManager blockedNumbersManager;
    private AllowListManager allowListManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blocked_numbers, container, false);

        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadBlockedNumbers();

        return view;
    }

    private void initViews(View view) {
        rvBlockedNumbers = view.findViewById(R.id.rv_blocked_numbers);
        tvEmptyBlocked = view.findViewById(R.id.tv_empty_blocked);
        tvTotalBlocked = view.findViewById(R.id.tv_total_blocked);
        tvTodayBlocked = view.findViewById(R.id.tv_today_blocked);
        btnClearAll = view.findViewById(R.id.btn_clear_all);
        etSearch = view.findViewById(R.id.et_search);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);
        btnSort = view.findViewById(R.id.btn_sort);

        blockedNumbersManager = new BlockedNumbersManager(requireContext());
        allowListManager = new AllowListManager(requireContext());
    }

    private void setupRecyclerView() {
        adapter = new BlockedNumbersGroupAdapter(requireContext(),
                new BlockedNumbersGroupAdapter.OnBlockedNumberDeleteListener() {
                    @Override
                    public void onDelete(BlockedNumber blockedNumber) {
                        showDeleteConfirmDialog(blockedNumber);
                    }
                },
                new BlockedNumbersGroupAdapter.OnBlockedNumberAllowListener() {
                    @Override
                    public void onAddToAllowList(String phoneNumber) {
                        showAddToAllowListDialog(phoneNumber);
                    }
                });

        rvBlockedNumbers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBlockedNumbers.setAdapter(adapter);
    }

    private void setupListeners() {
        btnClearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearAllConfirmDialog();
            }
        });

        // Search text change listener
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                adapter.filter(query);

                // Show/hide clear search button
                if (query.isEmpty()) {
                    btnClearSearch.setVisibility(View.GONE);
                } else {
                    btnClearSearch.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Clear search button
        btnClearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearch.setText("");
            }
        });

        // Sort button
        btnSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSortMenu(v);
            }
        });
    }

    private void loadBlockedNumbers() {
        List<BlockedNumber> blockedNumbers = blockedNumbersManager.getBlockedNumbers();
        adapter.setData(blockedNumbers);
        updateUI(blockedNumbers);
        updateStatistics(blockedNumbers);

        // Load contact names asynchronously after the initial display
        adapter.loadContactNamesAsync(null);
    }

    private void showSortMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_sort_blocked, popup.getMenu());

        // Mark current sort option
        BlockedNumbersGroupAdapter.SortOrder currentSort = adapter.getSortOrder();
        if (currentSort == BlockedNumbersGroupAdapter.SortOrder.RECENT_FIRST) {
            popup.getMenu().findItem(R.id.sort_recent).setChecked(true);
        } else {
            popup.getMenu().findItem(R.id.sort_contacts_first).setChecked(true);
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.sort_recent) {
                adapter.setSortOrder(BlockedNumbersGroupAdapter.SortOrder.RECENT_FIRST);
                return true;
            } else if (id == R.id.sort_contacts_first) {
                adapter.setSortOrder(BlockedNumbersGroupAdapter.SortOrder.CONTACTS_FIRST);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void updateUI(List<BlockedNumber> blockedNumbers) {
        if (blockedNumbers.isEmpty()) {
            tvEmptyBlocked.setVisibility(View.VISIBLE);
            rvBlockedNumbers.setVisibility(View.GONE);
            btnClearAll.setEnabled(false);
            etSearch.setEnabled(false);
        } else {
            tvEmptyBlocked.setVisibility(View.GONE);
            rvBlockedNumbers.setVisibility(View.VISIBLE);
            btnClearAll.setEnabled(true);
            etSearch.setEnabled(true);
        }
    }

    private void updateStatistics(List<BlockedNumber> blockedNumbers) {
        int totalBlocked = blockedNumbers.size();
        int todayBlocked = 0;

        // Calculate today's date range
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long todayStart = today.getTimeInMillis();

        today.add(Calendar.DAY_OF_MONTH, 1);
        long todayEnd = today.getTimeInMillis();

        // Count today's blocked numbers
        for (BlockedNumber blockedNumber : blockedNumbers) {
            if (blockedNumber.getTimestamp() >= todayStart && blockedNumber.getTimestamp() < todayEnd) {
                todayBlocked++;
            }
        }

        tvTotalBlocked.setText(String.valueOf(totalBlocked));
        tvTodayBlocked.setText(String.valueOf(todayBlocked));
    }

    private void showDeleteConfirmDialog(final BlockedNumber blockedNumber) {
        // Get all entries for this phone number
        String phoneNumber = blockedNumber.getPhoneNumber();
        List<BlockedNumber> allEntries = blockedNumbersManager.getBlockedNumbersForPhone(phoneNumber);

        String contactName = ContactsHelper.getContactName(requireContext(), phoneNumber);
        String displayName = contactName != null ? contactName : phoneNumber;

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete All Entries")
                .setMessage("Remove all " + allEntries.size() + " blocked entries for " + displayName + "?")
                .setPositiveButton("Delete All", (dialog, which) -> {
                    // Delete all entries for this number
                    for (BlockedNumber entry : allEntries) {
                        blockedNumbersManager.removeBlockedNumber(entry);
                    }
                    loadBlockedNumbers();
                    Toast.makeText(requireContext(), "All entries deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearAllConfirmDialog() {
        int count = blockedNumbersManager.getBlockedCount();
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear All History")
                .setMessage("This will permanently delete all " + count
                        + " blocked number entries.\n\nThis action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    blockedNumbersManager.clearAllBlockedNumbers();
                    loadBlockedNumbers();
                    Toast.makeText(requireContext(), "All entries cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddToAllowListDialog(final String phoneNumber) {
        // Check if already in allow list
        if (allowListManager.isInAllowList(phoneNumber)) {
            Toast.makeText(requireContext(),
                    "This number is already in the allow list",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String contactName = ContactsHelper.getContactName(requireContext(), phoneNumber);
        String displayName = contactName != null ? contactName : phoneNumber;

        new AlertDialog.Builder(requireContext())
                .setTitle("Add to Allow List")
                .setMessage("Add " + displayName + " to the allow list?\n\n" +
                        phoneNumber + "\n\n" +
                        "This number will never be blocked until removed from the allow list.")
                .setPositiveButton("Add to Allow List", (dialog, which) -> {
                    allowListManager.addToAllowList(phoneNumber);
                    Toast.makeText(requireContext(),
                            "Added to allow list",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBlockedNumbers(); // Refresh data when returning to this fragment
    }
}