package com.example.translationapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class TranslationActivity extends AppCompatActivity {

  EditText inputText;
  AutoCompleteTextView toLang;
  Button btnTranslate, btnHistory;
  ImageButton btnPlayAudio;
  TextView tvResult;
  ProgressBar progressBar;

  private AudioPlayer audioPlayer;
  private String currentTranslation;
  private String currentAudioBase64;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_translation);

    inputText = findViewById(R.id.et_input_text);
    toLang = findViewById(R.id.et_to_lang);
    btnTranslate = findViewById(R.id.btn_translate);
    btnHistory = findViewById(R.id.btn_history);
    btnPlayAudio = findViewById(R.id.btn_play_audio);
    tvResult = findViewById(R.id.tv_result);
    progressBar = findViewById(R.id.progress_bar);

    // Initialize audio player
    audioPlayer = new AudioPlayer(this);

    String[] languages = new String[] { "Darija", "English", "French", "Spanish", "Arabic" };
    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_dropdown_item_1line, languages);
    toLang.setAdapter(adapter);

    toLang.setOnClickListener(v -> toLang.showDropDown());
    toLang.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus)
        toLang.showDropDown();
    });

    btnTranslate.setOnClickListener(v -> {
      String text = inputText.getText().toString().trim();
      String to = toLang.getText().toString().trim();

      if (text.isEmpty()) {
        Toast.makeText(TranslationActivity.this, "Please enter text", Toast.LENGTH_SHORT).show();
        return;
      }

      if (to.isEmpty()) {
        Toast.makeText(TranslationActivity.this, "Please specify target language", Toast.LENGTH_SHORT).show();
        return;
      }

      // Get username from shared preferences
      String username = Config.getUsername(TranslationActivity.this);

      // Reset audio
      currentAudioBase64 = null;
      btnPlayAudio.setEnabled(false);

      // Start translation with audio
      translateWithAudio(text, to, username);
    });

    // Add history button click listener
    btnHistory.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String username = Config.getUsername(TranslationActivity.this);
        if (username == null || username.isEmpty()) {
          Toast.makeText(TranslationActivity.this, "Please login first", Toast.LENGTH_SHORT).show();
          return;
        }

        // Open History Activity
        Intent intent = new Intent(TranslationActivity.this, HistoryActivity.class);
        startActivity(intent);
      }
    });

    // Add audio play button listener
    btnPlayAudio.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (currentAudioBase64 != null && !currentAudioBase64.isEmpty()) {
          audioPlayer.playAudio(currentAudioBase64);
        } else {
          Toast.makeText(TranslationActivity.this, "No audio available", Toast.LENGTH_SHORT).show();
        }
      }
    });
  }

  private void translateWithAudio(String text, String to, String username) {
    // Show progress
    progressBar.setVisibility(View.VISIBLE);
    tvResult.setText("Translating...");

    OkHttpClient client = new OkHttpClient();

    // Build translation URL with audio parameter
    HttpUrl.Builder urlBuilder = HttpUrl.parse(Config.BASE_URL + "translate").newBuilder();
    urlBuilder.addQueryParameter("text", text);
    urlBuilder.addQueryParameter("to", to);
    urlBuilder.addQueryParameter("includeAudio", "true");  // Request audio

    // Add username if available
    if (username != null && !username.isEmpty()) {
      urlBuilder.addQueryParameter("username", username);
    }

    String url = urlBuilder.build().toString();

    Request request = new Request.Builder()
            .url(url)
            .get()
            .build();

    client.newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        runOnUiThread(() -> {
          progressBar.setVisibility(View.GONE);
          tvResult.setText("Error: " + e.getMessage());
          Toast.makeText(TranslationActivity.this, "Translation failed", Toast.LENGTH_SHORT).show();
        });
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        runOnUiThread(() -> progressBar.setVisibility(View.GONE));

        if (response.isSuccessful()) {
          ResponseBody responseBody = response.body();
          if (responseBody != null) {
            String responseBodyString = responseBody.string();

            try {
              // Parse JSON response
              JsonObject jsonResponse = JsonParser.parseString(responseBodyString).getAsJsonObject();

              // Get translation
              String translatedText = jsonResponse.get("translation").getAsString();
              currentTranslation = translatedText;

              // Check if audio is included in response
              if (jsonResponse.has("audio")) {
                currentAudioBase64 = jsonResponse.get("audio").getAsString();
              }

              runOnUiThread(() -> {
                tvResult.setText(translatedText);

                // Enable audio button if audio is available
                if (currentAudioBase64 != null && !currentAudioBase64.isEmpty()) {
                  btnPlayAudio.setEnabled(true);
                  btnPlayAudio.setAlpha(1.0f);
                } else {
                  btnPlayAudio.setEnabled(false);
                  btnPlayAudio.setAlpha(0.5f);
                }
              });

            } catch (Exception e) {
              runOnUiThread(() -> {
                tvResult.setText("Error parsing response");
                Toast.makeText(TranslationActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
              });
            }
          }
        } else {
          runOnUiThread(() -> {
            tvResult.setText("Error: " + response.message());
            Toast.makeText(TranslationActivity.this, "Translation failed", Toast.LENGTH_SHORT).show();
          });
        }
      }
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    // Clean up audio player
    if (audioPlayer != null) {
      audioPlayer.cleanup();
    }
  }

  public static class TranslationResponse {
    private String translation;
    private String historyId;
    private String audio;
    private String audioFormat;

    public String getTranslation() {
      return translation;
    }

    public String getHistoryId() {
      return historyId;
    }

    public String getAudio() {
      return audio;
    }

    public String getAudioFormat() {
      return audioFormat;
    }
  }
}