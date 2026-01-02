package com.example.translationapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private TextView tvHistoryCount;
    private Button btnClearHistory;
    private EditText etSearch;
    private View emptyState;
    private List<HistoryItem> fullHistoryList = new ArrayList<>();
    private List<HistoryItem> filteredList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish()); // Back button

        // Initialize views
        recyclerView = findViewById(R.id.history_recycler_view);
        tvHistoryCount = findViewById(R.id.tv_history_count);
        btnClearHistory = findViewById(R.id.btn_clear_history);
        etSearch = findViewById(R.id.et_search);
        emptyState = findViewById(R.id.empty_state);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(filteredList);
        recyclerView.setAdapter(adapter);

        // Search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadTranslationHistory();

        btnClearHistory.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Clear History")
                    .setMessage("Are you sure you want to delete all your translation history?")
                    .setPositiveButton("Clear All", (dialog, which) -> clearHistory())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void filter(String query) {
        if (query.isEmpty()) {
            filteredList.clear();
            filteredList.addAll(fullHistoryList);
        } else {
            String lowerQuery = query.toLowerCase();
            filteredList.clear();
            for (HistoryItem item : fullHistoryList) {
                if (item.getOriginalText().toLowerCase().contains(lowerQuery) ||
                    item.getTranslatedText().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        tvHistoryCount.setText("Showing " + filteredList.size() + " items");
    }

    private void loadTranslationHistory() {
        new Thread(() -> {
            try {
                String username = Config.getUsername(HistoryActivity.this);
                if (username == null) return;

                OkHttpClient client = new OkHttpClient();
                HttpUrl url = HttpUrl.parse(Config.BASE_URL + "translate/history")
                        .newBuilder()
                        .addQueryParameter("username", username)
                        .build();

                Request request = new Request.Builder().url(url).get().build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                        JsonArray historyArray = json.getAsJsonArray("history");

                        fullHistoryList.clear();
                        for (int i = 0; i < historyArray.size(); i++) {
                            JsonObject item = historyArray.get(i).getAsJsonObject();
                            fullHistoryList.add(new HistoryItem(
                                    item.get("id").getAsString(),
                                    item.get("originalText").getAsString(),
                                    item.get("translatedText").getAsString(),
                                    item.get("targetLang").getAsString(),
                                    item.get("timestamp").getAsString()
                            ));
                        }

                        runOnUiThread(() -> {
                            filteredList.clear();
                            filteredList.addAll(fullHistoryList);
                            adapter.notifyDataSetChanged();
                            updateEmptyState();
                        });
                    }
                }
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Error loading history", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void clearHistory() {
        new Thread(() -> {
            try {
                String username = Config.getUsername(this);
                OkHttpClient client = new OkHttpClient();
                HttpUrl url = HttpUrl.parse(Config.BASE_URL + "translate/clear-history")
                        .newBuilder()
                        .addQueryParameter("username", username)
                        .build();

                Request request = new Request.Builder().url(url).get().build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            fullHistoryList.clear();
                            filteredList.clear();
                            adapter.notifyDataSetChanged();
                            updateEmptyState();
                            Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Connection error", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // RecyclerView Adapter
    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<HistoryItem> items;

        HistoryAdapter(List<HistoryItem> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistoryItem item = items.get(position);
            holder.tvOriginal.setText(item.getOriginalText());
            holder.tvTranslated.setText(item.getTranslatedText());
            holder.tvLangTag.setText("EN → " + item.getTargetLang().toUpperCase());
            
            // Format timestamp (assuming "yyyy-MM-dd HH:mm:ss.SSS" or similar)
            String date = item.getTimestamp();
            if (date.length() > 10) date = date.substring(0, 10);
            holder.tvTimestamp.setText(date);

            holder.itemView.setOnClickListener(v -> showDetailDialog(item));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvOriginal, tvTranslated, tvLangTag, tvTimestamp;
            ViewHolder(View v) {
                super(v);
                tvOriginal = v.findViewById(R.id.tv_original);
                tvTranslated = v.findViewById(R.id.tv_translated);
                tvLangTag = v.findViewById(R.id.tv_lang_tag);
                tvTimestamp = v.findViewById(R.id.tv_timestamp);
            }
        }
    }

    private void showDetailDialog(HistoryItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Translation Details")
                .setMessage("Original:\n" + item.getOriginalText() + 
                           "\n\nTranslated (" + item.getTargetLang() + "):\n" + item.getTranslatedText() + 
                           "\n\nDate: " + item.getTimestamp())
                .setPositiveButton("Close", null)
                .setNeutralButton("Copy Result", (dialog, which) -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("translation", item.getTranslatedText());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    public static class HistoryItem {
        private String id, originalText, translatedText, targetLang, timestamp;
        public HistoryItem(String id, String originalText, String translatedText, String targetLang, String timestamp) {
            this.id = id; this.originalText = originalText; this.translatedText = translatedText;
            this.targetLang = targetLang; this.timestamp = timestamp;
        }
        public String getOriginalText() { return originalText; }
        public String getTranslatedText() { return translatedText; }
        public String getTargetLang() { return targetLang; }
        public String getTimestamp() { return timestamp; }
    }
}