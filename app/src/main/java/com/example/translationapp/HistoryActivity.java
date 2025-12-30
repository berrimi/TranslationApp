package com.example.translationapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HistoryActivity extends AppCompatActivity {

    private ListView historyListView;
    private TextView tvHistoryCount;
    private Button btnClearHistory;
    private List<HistoryItem> historyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyListView = findViewById(R.id.history_list_view);
        tvHistoryCount = findViewById(R.id.tv_history_count);
        btnClearHistory = findViewById(R.id.btn_clear_history);

        // Load translation history
        loadTranslationHistory();

        // Set up clear history button
        btnClearHistory.setOnClickListener(v -> clearHistory());
    }

    private void loadTranslationHistory() {
        new Thread(() -> {
            try {
                String username = Config.getUsername(HistoryActivity.this);
                if (username == null || username.isEmpty()) {
                    runOnUiThread(() -> {
                        Toast.makeText(HistoryActivity.this, "No user logged in", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                OkHttpClient client = new OkHttpClient();

                // Build URL with username parameter
                okhttp3.HttpUrl url = okhttp3.HttpUrl.parse(Config.BASE_URL + "translate/history")
                        .newBuilder()
                        .addQueryParameter("username", username)
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        ResponseBody responseBody = response.body();
                        if (responseBody != null) {
                            String responseBodyString = responseBody.string();

                            // Parse JSON response
                            JsonObject jsonResponse = JsonParser.parseString(responseBodyString).getAsJsonObject();
                            int count = jsonResponse.get("count").getAsInt();
                            JsonArray historyArray = jsonResponse.getAsJsonArray("history");

                            historyList.clear();

                            for (int i = 0; i < historyArray.size(); i++) {
                                JsonObject item = historyArray.get(i).getAsJsonObject();
                                HistoryItem historyItem = new HistoryItem(
                                        item.get("id").getAsString(),
                                        item.get("originalText").getAsString(),
                                        item.get("translatedText").getAsString(),
                                        item.get("targetLang").getAsString(),
                                        item.get("timestamp").getAsString()
                                );
                                historyList.add(historyItem);
                            }

                            runOnUiThread(() -> {
                                updateHistoryList();
                                tvHistoryCount.setText("Total: " + count + " translations");
                            });
                        }
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(HistoryActivity.this, "Failed to load history", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(HistoryActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void updateHistoryList() {
        List<String> displayItems = new ArrayList<>();
        for (HistoryItem item : historyList) {
            // Truncate long texts for display
            String original = item.getOriginalText();
            String translated = item.getTranslatedText();

            if (original.length() > 30) {
                original = original.substring(0, 27) + "...";
            }
            if (translated.length() > 30) {
                translated = translated.substring(0, 27) + "...";
            }

            String displayText = original + "\n→ " + translated +
                    "\n[" + item.getTargetLang() + "]";
            displayItems.add(displayText);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                displayItems
        );

        historyListView.setAdapter(adapter);

        // Set item click listener to show full translation
        historyListView.setOnItemClickListener((parent, view, position, id) -> {
            HistoryItem selected = historyList.get(position);
            String fullText = "Original: " + selected.getOriginalText() +
                    "\n\nTranslation: " + selected.getTranslatedText() +
                    "\n\nLanguage: " + selected.getTargetLang() +
                    "\nTime: " + selected.getTimestamp();

            Toast.makeText(HistoryActivity.this, fullText, Toast.LENGTH_LONG).show();
        });
    }

    private void clearHistory() {
        new Thread(() -> {
            try {
                String username = Config.getUsername(HistoryActivity.this);
                if (username == null || username.isEmpty()) {
                    return;
                }

                OkHttpClient client = new OkHttpClient();

                // Build URL for clearing history
                okhttp3.HttpUrl url = okhttp3.HttpUrl.parse(Config.BASE_URL + "translate/clear-history")
                        .newBuilder()
                        .addQueryParameter("username", username)
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(HistoryActivity.this, "History cleared", Toast.LENGTH_SHORT).show();
                            historyList.clear();
                            updateHistoryList();
                            tvHistoryCount.setText("Total: 0 translations");
                        });
                    }
                }
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(HistoryActivity.this, "Error clearing history", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    // Inner class for history items
    public static class HistoryItem {
        private String id;
        private String originalText;
        private String translatedText;
        private String targetLang;
        private String timestamp;

        public HistoryItem(String id, String originalText, String translatedText,
                           String targetLang, String timestamp) {
            this.id = id;
            this.originalText = originalText;
            this.translatedText = translatedText;
            this.targetLang = targetLang;
            this.timestamp = timestamp;
        }

        public String getId() { return id; }
        public String getOriginalText() { return originalText; }
        public String getTranslatedText() { return translatedText; }
        public String getTargetLang() { return targetLang; }
        public String getTimestamp() { return timestamp; }
    }
}