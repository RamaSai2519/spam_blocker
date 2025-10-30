package com.spam_blocker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class KeywordAdapter extends RecyclerView.Adapter<KeywordAdapter.KeywordViewHolder> {
    
    private KeywordManager keywordManager;
    private OnKeywordDeleteListener deleteListener;

    public interface OnKeywordDeleteListener {
        void onDelete(String keyword);
    }

    public KeywordAdapter(KeywordManager keywordManager, OnKeywordDeleteListener deleteListener) {
        this.keywordManager = keywordManager;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public KeywordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_keyword, parent, false);
        return new KeywordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KeywordViewHolder holder, int position) {
        List<String> keywords = keywordManager.getKeywords();
        String keyword = keywords.get(position);
        holder.tvKeyword.setText(keyword);
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deleteListener != null) {
                    deleteListener.onDelete(keyword);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return keywordManager.getKeywords().size();
    }

    static class KeywordViewHolder extends RecyclerView.ViewHolder {
        TextView tvKeyword;
        ImageButton btnDelete;

        public KeywordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKeyword = itemView.findViewById(R.id.tv_keyword);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
