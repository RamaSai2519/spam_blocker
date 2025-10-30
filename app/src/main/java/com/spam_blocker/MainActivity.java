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
    private RecyclerView rvKeywords;
    private TextView tvServiceStatus;
    private TextView tvEmptyKeywords;

    private KeywordAdapter keywordAdapter;
    private KeywordManager keywordManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        keywordManager = new KeywordManager(this);

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
        rvKeywords = findViewById(R.id.rv_keywords);
        tvServiceStatus = findViewById(R.id.tv_service_status);
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
}
