package com.spam_blocker;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeywordManager {
    private static final String PREFS_NAME = "SpamBlockerPrefs";
    private static final String KEY_KEYWORDS = "blocked_keywords";
    
    private SharedPreferences prefs;
    private Set<String> keywords;

    public KeywordManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadKeywords();
    }

    private void loadKeywords() {
        keywords = new HashSet<>(prefs.getStringSet(KEY_KEYWORDS, new HashSet<>()));
    }

    public void addKeyword(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            keywords.add(keyword.trim().toLowerCase());
            save();
        }
    }

    public void removeKeyword(String keyword) {
        if (keyword != null) {
            keywords.remove(keyword.trim().toLowerCase());
            save();
        }
    }

    public List<String> getKeywords() {
        return new ArrayList<>(keywords);
    }

    public boolean containsKeyword(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        String lowerText = text.toLowerCase();
        for (String keyword : keywords) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void save() {
        prefs.edit().putStringSet(KEY_KEYWORDS, keywords).apply();
    }
}
