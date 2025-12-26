package com.spam_blocker;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AllowListFragment extends Fragment {

    private RecyclerView recyclerView;
    private AllowListAdapter adapter;
    private TextView tvEmptyState;
    private EditText etSearchAllowList;
    private Button btnAddNumber;
    private AllowListManager allowListManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_allow_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        allowListManager = new AllowListManager(requireContext());

        initViews(view);
        setupRecyclerView();
        setupSearchBar();
        loadAllowList();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_allow_list);
        tvEmptyState = view.findViewById(R.id.tv_empty_allow_list);
        etSearchAllowList = view.findViewById(R.id.et_search_allow_list);
        btnAddNumber = view.findViewById(R.id.btn_add_to_allow_list);

        btnAddNumber.setOnClickListener(v -> showAddNumberDialog());
    }

    private void setupRecyclerView() {
        adapter = new AllowListAdapter(requireContext(), phoneNumber -> {
            // Handle remove from allow list
            new AlertDialog.Builder(requireContext())
                    .setTitle("Remove from Allow List")
                    .setMessage("Remove " + phoneNumber + " from the allow list?")
                    .setPositiveButton("Remove", (dialog, which) -> {
                        allowListManager.removeFromAllowList(phoneNumber);
                        loadAllowList();
                        Toast.makeText(requireContext(), "Removed from allow list", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchBar() {
        etSearchAllowList.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadAllowList() {
        List<String> allowList = allowListManager.getAllowList();
        adapter.setData(allowList);

        // Show/hide empty state
        if (allowList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void showAddNumberDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_phone_number, null);
        EditText etPhoneNumber = dialogView.findViewById(R.id.et_phone_number);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add to Allow List")
                .setMessage("Enter a phone number to always allow:")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String phoneNumber = etPhoneNumber.getText().toString().trim();
                    if (!phoneNumber.isEmpty()) {
                        allowListManager.addToAllowList(phoneNumber);
                        loadAllowList();
                        Toast.makeText(requireContext(), "Added to allow list", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Please enter a phone number", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllowList();
    }
}
