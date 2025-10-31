package com.spam_blocker;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private Button btnEnableAccessibility;
    private Button btnDndAccess;
    private TextView tvServiceStatus;
    private TextView tvDndStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        initViews(view);
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        btnEnableAccessibility = view.findViewById(R.id.btn_enable_accessibility);
        btnDndAccess = view.findViewById(R.id.btn_dnd_access);
        tvServiceStatus = view.findViewById(R.id.tv_service_status);
        tvDndStatus = view.findViewById(R.id.tv_dnd_status);
    }

    private void setupListeners() {
        btnEnableAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                Toast.makeText(requireContext(), "Please enable Spam Blocker accessibility service", Toast.LENGTH_LONG)
                        .show();
            }
        });

        btnDndAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!PermissionManager.hasNotificationPolicyAccess(requireContext())) {
                    Toast.makeText(requireContext(), PermissionManager.getDndAccessExplanation(),
                            Toast.LENGTH_LONG).show();
                    PermissionManager.requestNotificationPolicyAccess(requireContext());
                } else {
                    Toast.makeText(requireContext(), "Do Not Disturb access is already granted",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        // Check if accessibility service is enabled
        boolean isAccessibilityEnabled = isAccessibilityServiceEnabled();
        if (isAccessibilityEnabled) {
            tvServiceStatus.setText(R.string.status_service_enabled);
            tvServiceStatus.setBackgroundColor(getResources().getColor(R.color.status_enabled_bg, null));
            tvServiceStatus.setTextColor(getResources().getColor(R.color.status_enabled, null));
        } else {
            tvServiceStatus.setText(R.string.status_service_disabled);
            tvServiceStatus.setBackgroundColor(getResources().getColor(R.color.status_disabled_bg, null));
            tvServiceStatus.setTextColor(getResources().getColor(R.color.status_disabled, null));
        }

        // Check Do Not Disturb access
        boolean hasDndAccess = PermissionManager.hasNotificationPolicyAccess(requireContext());
        if (hasDndAccess) {
            tvDndStatus.setText("✓ Do Not Disturb: Access Granted");
            tvDndStatus.setBackgroundColor(getResources().getColor(R.color.status_enabled_bg, null));
            tvDndStatus.setTextColor(getResources().getColor(R.color.status_enabled, null));
            btnDndAccess.setText("DND Access Granted ✓");
            btnDndAccess.setEnabled(false);
        } else {
            tvDndStatus.setText("⚠ Do Not Disturb: Access Required");
            tvDndStatus.setBackgroundColor(getResources().getColor(R.color.status_disabled_bg, null));
            tvDndStatus.setTextColor(getResources().getColor(R.color.status_disabled, null));
            btnDndAccess.setText("Grant DND Access");
            btnDndAccess.setEnabled(true);
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String serviceName = requireContext().getPackageName() + "/" + ScreenScanAccessibilityService.class.getName();
        String enabledServices = Settings.Secure.getString(
                requireContext().getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabledServices != null && enabledServices.contains(serviceName);
    }
}