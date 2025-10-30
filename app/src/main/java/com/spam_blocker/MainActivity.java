package com.spam_blocker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;

    private EditText etKeyword;
    private Button btnAddKeyword;
    private Button btnEnableAccessibility;
    private Button btnBlockedNumbers;
    private Button btnDndAccess;
    private Button btnTestBlocking;
    private RecyclerView rvKeywords;
    private TextView tvServiceStatus;
    private TextView tvDndStatus;
    private TextView tvEmptyKeywords;

    private KeywordAdapter keywordAdapter;
    private KeywordManager keywordManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        keywordManager = new KeywordManager(this);

        // Add some test blocked numbers for demonstration (only add once)
        addTestBlockedNumbers();

        initViews();
        setupRecyclerView();
        setupListeners();
        checkPermissions();
        updateUI();
    }

    private void initViews() {
        etKeyword = findViewById(R.id.et_keyword);
        btnAddKeyword = findViewById(R.id.btn_add_keyword);
        btnEnableAccessibility = findViewById(R.id.btn_enable_accessibility);
        btnBlockedNumbers = findViewById(R.id.btn_blocked_numbers);
        btnDndAccess = findViewById(R.id.btn_dnd_access);
        btnTestBlocking = findViewById(R.id.btn_test_blocking);
        rvKeywords = findViewById(R.id.rv_keywords);
        tvServiceStatus = findViewById(R.id.tv_service_status);
        tvDndStatus = findViewById(R.id.tv_dnd_status);
        tvEmptyKeywords = findViewById(R.id.tv_empty_keywords);
    }

    private void setupRecyclerView() {
        keywordAdapter = new KeywordAdapter(keywordManager, new KeywordAdapter.OnKeywordDeleteListener() {
            @Override
            public void onDelete(String keyword) {
                keywordManager.removeKeyword(keyword);
                updateUI();
                Toast.makeText(MainActivity.this, "Keyword removed", Toast.LENGTH_SHORT).show();
            }
        });
        rvKeywords.setLayoutManager(new LinearLayoutManager(this));
        rvKeywords.setAdapter(keywordAdapter);
    }

    private void setupListeners() {
        btnAddKeyword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyword = etKeyword.getText().toString().trim();
                if (!keyword.isEmpty()) {
                    keywordManager.addKeyword(keyword);
                    etKeyword.setText("");
                    updateUI();
                    Toast.makeText(MainActivity.this, "Keyword added", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a keyword", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnEnableAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                Toast.makeText(MainActivity.this, "Please enable Spam Blocker accessibility service", Toast.LENGTH_LONG)
                        .show();
            }
        });

        btnBlockedNumbers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BlockedNumbersActivity.class);
                startActivity(intent);
            }
        });

        btnDndAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!PermissionManager.hasNotificationPolicyAccess(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, PermissionManager.getDndAccessExplanation(),
                            Toast.LENGTH_LONG).show();
                    PermissionManager.requestNotificationPolicyAccess(MainActivity.this);
                } else {
                    Toast.makeText(MainActivity.this, "Do Not Disturb access is already granted",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnTestBlocking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testBlockingFunctionality();
            }
        });
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
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        keywordAdapter.notifyDataSetChanged();

        if (keywordManager.getKeywords().isEmpty()) {
            tvEmptyKeywords.setVisibility(View.VISIBLE);
            rvKeywords.setVisibility(View.GONE);
        } else {
            tvEmptyKeywords.setVisibility(View.GONE);
            rvKeywords.setVisibility(View.VISIBLE);
        }

        // Check if accessibility service is enabled
        boolean isAccessibilityEnabled = isAccessibilityServiceEnabled();
        if (isAccessibilityEnabled) {
            tvServiceStatus.setText(R.string.status_service_enabled);
            tvServiceStatus.setBackgroundColor(0xFFE8F5E9);
            tvServiceStatus.setTextColor(0xFF2E7D32);
        } else {
            tvServiceStatus.setText(R.string.status_service_disabled);
            tvServiceStatus.setBackgroundColor(0xFFFFEBEE);
            tvServiceStatus.setTextColor(0xFFC62828);
        }

        // Check Do Not Disturb access
        boolean hasDndAccess = PermissionManager.hasNotificationPolicyAccess(this);
        if (hasDndAccess) {
            tvDndStatus.setText("Do Not Disturb: Access Granted");
            tvDndStatus.setBackgroundColor(0xFFE8F5E9);
            tvDndStatus.setTextColor(0xFF2E7D32);
            btnDndAccess.setText("DND Access Granted ✓");
            btnDndAccess.setEnabled(false);
        } else {
            tvDndStatus.setText("Do Not Disturb: Access Required");
            tvDndStatus.setBackgroundColor(0xFFFFEBEE);
            tvDndStatus.setTextColor(0xFFC62828);
            btnDndAccess.setText("Grant DND Access");
            btnDndAccess.setEnabled(true);
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String serviceName = getPackageName() + "/" + ScreenScanAccessibilityService.class.getName();
        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabledServices != null && enabledServices.contains(serviceName);
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

    private void testBlockingFunctionality() {
        StringBuilder result = new StringBuilder();
        result.append("=== Spam Blocker Test Results ===\n\n");

        // Check permissions
        result.append("Permissions:\n");
        result.append("- Accessibility Service: ").append(isAccessibilityServiceEnabled() ? "✓ Enabled" : "✗ Disabled")
                .append("\n");
        result.append("- Do Not Disturb Access: ")
                .append(PermissionManager.hasNotificationPolicyAccess(this) ? "✓ Granted" : "✗ Missing").append("\n");
        result.append("- Phone Permissions: ").append(checkBasicPermissions() ? "✓ Granted" : "✗ Missing")
                .append("\n\n");

        // Check keywords
        KeywordManager keywordManager = new KeywordManager(this);
        result.append("Keywords (").append(keywordManager.getKeywords().size()).append("):\n");
        for (String keyword : keywordManager.getKeywords()) {
            result.append("- ").append(keyword).append("\n");
        }
        result.append("\n");

        // Check blocked numbers
        BlockedNumbersManager blockedManager = new BlockedNumbersManager(this);
        result.append("Blocked Numbers History (").append(blockedManager.getBlockedCount()).append("):\n");

        // Test specific numbers
        String[] testNumbers = { "+1234567890", "+9876543210", "+5555555555" };
        for (String number : testNumbers) {
            boolean isBlocked = blockedManager.isNumberBlocked(number);
            result.append("- ").append(number).append(": ").append(isBlocked ? "✓ Blocked" : "○ Not blocked")
                    .append("\n");
        }

        result.append("\n=== To test call blocking ===\n");
        result.append("1. Ensure accessibility service is enabled\n");
        result.append("2. Run simulate_call.ps1 from the project root\n");
        result.append("3. Check if +1234567890 gets blocked\n");
        result.append("4. View results in 'View Blocked Numbers'");

        // Show result in a dialog or toast
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Test Results")
                .setMessage(result.toString())
                .setPositiveButton("OK", null)
                .setNeutralButton("View Blocked Numbers", (dialog, which) -> {
                    Intent intent = new Intent(MainActivity.this, BlockedNumbersActivity.class);
                    startActivity(intent);
                })
                .show();
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
