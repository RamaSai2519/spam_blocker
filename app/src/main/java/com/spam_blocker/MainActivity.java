package com.spam_blocker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Add some test blocked numbers for demonstration (only add once)
        addTestBlockedNumbers();

        initViews();
        setupBottomNavigation();
        checkPermissions();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

    private void initViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_blocked_numbers) {
                selectedFragment = new BlockedNumbersFragment();
            } else if (item.getItemId() == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }

            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }

        // Request ANSWER_PHONE_CALLS permission separately for API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.ANSWER_PHONE_CALLS },
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, R.string.grant_permissions, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void addTestBlockedNumbers() {
        BlockedNumbersManager blockedManager = new BlockedNumbersManager(this);

        // Only add test data if no blocked numbers exist yet
        if (blockedManager.getBlockedCount() == 0) {
            // Add some test blocked numbers with different timestamps
            long now = System.currentTimeMillis();

            blockedManager.addBlockedNumber("+1234567890", "Keyword match: spam", "SPAM LIKELY");

            // Add one from yesterday
            long yesterday = now - (24 * 60 * 60 * 1000);
            BlockedNumber yesterday_call = new BlockedNumber("+9876543210", yesterday, "Keyword match: telemarketer",
                    "Unknown Caller - Telemarketer Service");

            // Add one from last week
            long lastWeek = now - (7 * 24 * 60 * 60 * 1000);
            BlockedNumber old_call = new BlockedNumber("+5555555555", lastWeek, "Keyword match: promo",
                    "Promotional Offer - Limited Time");

            // Manually add these to storage (simulating older calls)
            blockedManager.addBlockedNumber("+9876543210", "Keyword match: telemarketer",
                    "Unknown Caller - Telemarketer Service");
            blockedManager.addBlockedNumber("+5555555555", "Keyword match: promo", "Promotional Offer - Limited Time");
        }
    }

    private boolean checkBasicPermissions() {
        String[] permissions = {
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
        };

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
