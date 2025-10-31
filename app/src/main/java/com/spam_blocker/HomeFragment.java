package com.spam_blocker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class HomeFragment extends Fragment {

    private EditText etKeyword;
    private Button btnAddKeyword;
    private RecyclerView rvKeywords;
    private TextView tvEmptyKeywords;

    private KeywordAdapter keywordAdapter;
    private KeywordManager keywordManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        keywordManager = new KeywordManager(requireContext());

        initViews(view);
        setupRecyclerView();
        setupListeners();
        updateUI();

        return view;
    }

    private void initViews(View view) {
        etKeyword = view.findViewById(R.id.et_keyword);
        btnAddKeyword = view.findViewById(R.id.btn_add_keyword);
        rvKeywords = view.findViewById(R.id.rv_keywords);
        tvEmptyKeywords = view.findViewById(R.id.tv_empty_keywords);
    }

    private void setupRecyclerView() {
        keywordAdapter = new KeywordAdapter(keywordManager, new KeywordAdapter.OnKeywordDeleteListener() {
            @Override
            public void onDelete(String keyword) {
                keywordManager.removeKeyword(keyword);
                updateUI();
                Toast.makeText(requireContext(), R.string.keyword_removed, Toast.LENGTH_SHORT).show();
            }
        });
        rvKeywords.setLayoutManager(new LinearLayoutManager(requireContext()));
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
                    Toast.makeText(requireContext(), R.string.keyword_added, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), R.string.keyword_empty_error, Toast.LENGTH_SHORT).show();
                }
            }
        });
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
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }
}