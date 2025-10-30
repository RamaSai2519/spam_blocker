package com.spam_blocker;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.List;

public class BlockedNumbersActivity extends AppCompatActivity {
    private RecyclerView rvBlockedNumbers;
    private TextView tvEmptyBlocked;
    private TextView tvTotalBlocked;
    private TextView tvTodayBlocked;
    private Button btnClearAll;

    private BlockedNumbersAdapter adapter;
    private BlockedNumbersManager blockedNumbersManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_numbers);

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Blocked Numbers");
        }

        initViews();
        setupRecyclerView();
        setupListeners();
        loadBlockedNumbers();
    }

    private void initViews() {
        rvBlockedNumbers = findViewById(R.id.rv_blocked_numbers);
        tvEmptyBlocked = findViewById(R.id.tv_empty_blocked);
        tvTotalBlocked = findViewById(R.id.tv_total_blocked);
        tvTodayBlocked = findViewById(R.id.tv_today_blocked);
        btnClearAll = findViewById(R.id.btn_clear_all);

        blockedNumbersManager = new BlockedNumbersManager(this);
    }

    private void setupRecyclerView() {
        adapter = new BlockedNumbersAdapter(blockedNumbersManager,
                new BlockedNumbersAdapter.OnBlockedNumberDeleteListener() {
                    @Override
                    public void onDelete(BlockedNumber blockedNumber) {
                        showDeleteConfirmDialog(blockedNumber);
                    }
                });

        rvBlockedNumbers.setLayoutManager(new LinearLayoutManager(this));
        rvBlockedNumbers.setAdapter(adapter);
    }

    private void setupListeners() {
        btnClearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearAllConfirmDialog();
            }
        });
    }

    private void loadBlockedNumbers() {
        List<BlockedNumber> blockedNumbers = blockedNumbersManager.getBlockedNumbers();
        updateUI();
        updateStatistics(blockedNumbers);
    }

    private void updateUI() {
        adapter.notifyDataSetChanged();

        List<BlockedNumber> blockedNumbers = blockedNumbersManager.getBlockedNumbers();
        if (blockedNumbers.isEmpty()) {
            tvEmptyBlocked.setVisibility(View.VISIBLE);
            rvBlockedNumbers.setVisibility(View.GONE);
            btnClearAll.setEnabled(false);
        } else {
            tvEmptyBlocked.setVisibility(View.GONE);
            rvBlockedNumbers.setVisibility(View.VISIBLE);
            btnClearAll.setEnabled(true);
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
        new AlertDialog.Builder(this)
                .setTitle("Delete Entry")
                .setMessage("Remove this blocked number entry?\n\nNumber: " + blockedNumber.getPhoneNumber())
                .setPositiveButton("Delete", (dialog, which) -> {
                    blockedNumbersManager.removeBlockedNumber(blockedNumber);
                    loadBlockedNumbers();
                    Toast.makeText(BlockedNumbersActivity.this, "Entry deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearAllConfirmDialog() {
        int count = blockedNumbersManager.getBlockedCount();
        new AlertDialog.Builder(this)
                .setTitle("Clear All History")
                .setMessage("This will permanently delete all " + count
                        + " blocked number entries.\n\nThis action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    blockedNumbersManager.clearAllBlockedNumbers();
                    loadBlockedNumbers();
                    Toast.makeText(BlockedNumbersActivity.this, "All entries cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBlockedNumbers(); // Refresh data when returning to this activity
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}