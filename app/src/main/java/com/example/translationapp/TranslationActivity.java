package com.example.translationapp;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Locale;

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
  MaterialButton btnPlayAudio;
  TextView tvResult;
  ProgressBar progressBar;
  ImageButton btnLogout, btnSettings;

  private AudioPlayer audioPlayer;
  private TextToSpeech textToSpeech;
  private String currentTranslation;
  private String currentAudioBase64;
  private boolean isTtsReady = false;
  private String lastTargetLang = "";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_translation);

    // Setup Toolbar
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    inputText = findViewById(R.id.et_input_text);
    toLang = findViewById(R.id.et_to_lang);
    btnTranslate = findViewById(R.id.btn_translate);
    btnHistory = findViewById(R.id.btn_history);
    btnPlayAudio = findViewById(R.id.btn_play_audio);
    tvResult = findViewById(R.id.tv_result);
    progressBar = findViewById(R.id.progress_bar);
    btnLogout = findViewById(R.id.btn_logout);
    btnSettings = findViewById(R.id.btn_settings);

    // Initialize audio player for existing network-based audio
    audioPlayer = new AudioPlayer(this);

    // Initialize Native TextToSpeech for Arabic
    textToSpeech = new TextToSpeech(this, status -> {
      if (status == TextToSpeech.SUCCESS) {
        int result = textToSpeech.setLanguage(new Locale("ar"));
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
          isTtsReady = false;
        } else {
          isTtsReady = true;
        }
      } else {
        isTtsReady = false;
      }
    });

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

      // Store target language for audio playback logic
      lastTargetLang = to;

      // Reset audio state
      currentAudioBase64 = null;
      btnPlayAudio.setEnabled(false);
      btnPlayAudio.setAlpha(0.5f);

      // Start translation
      translateWithAudio(text, to, Config.getUsername(this));
    });

    // Add history button click listener
    btnHistory.setOnClickListener(v -> {
      Intent intent = new Intent(TranslationActivity.this, HistoryActivity.class);
      startActivity(intent);
    });

    // Play button logic
    btnPlayAudio.setOnClickListener(v -> {
      String textToSpeak = tvResult.getText().toString().trim();
      
      if (textToSpeak.isEmpty() || textToSpeak.equals("Translating...") || textToSpeak.contains("Your translation will appear here")) return;

      String langLower = lastTargetLang.toLowerCase();
      // Treat Darija as Arabic for TTS
      if (langLower.contains("arabic") || langLower.contains("darija")) {
        if (isTtsReady && textToSpeech != null) {
          textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "ArabicTTS");
        } else {
          Toast.makeText(this, "Arabic TTS is not ready", Toast.LENGTH_SHORT).show();
        }
      } else if (currentAudioBase64 != null && !currentAudioBase64.isEmpty()) {
        audioPlayer.playAudio(currentAudioBase64);
      } else {
        Toast.makeText(this, "No audio available for " + lastTargetLang, Toast.LENGTH_SHORT).show();
      }
    });

    // Logout Button
    btnLogout.setOnClickListener(v -> showLogoutConfirmation());

    // Settings Button
    btnSettings.setOnClickListener(v -> {
        Intent intent = new Intent(TranslationActivity.this, AccountSettingsActivity.class);
        startActivity(intent);
    });
  }

  private void showLogoutConfirmation() {
    new MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Logout", (dialog, which) -> {
                Config.clearUserData(this);
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("Cancel", null)
            .show();
  }

  private void translateWithAudio(String text, String to, String username) {
    progressBar.setVisibility(View.VISIBLE);
    tvResult.setText("Translating...");

    HttpUrl.Builder urlBuilder = HttpUrl.parse(Config.BASE_URL + "translate").newBuilder();
    urlBuilder.addQueryParameter("text", text);
    urlBuilder.addQueryParameter("to", to);
    
    String toLower = to.toLowerCase();
    // Don't request server audio for languages we handle locally with TTS
    if (!toLower.contains("arabic") && !toLower.contains("darija")) {
        urlBuilder.addQueryParameter("includeAudio", "true");
    }
    if (username != null) urlBuilder.addQueryParameter("username", username);

    new OkHttpClient().newCall(new Request.Builder().url(urlBuilder.build()).get().build()).enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        runOnUiThread(() -> {
          progressBar.setVisibility(View.GONE);
          tvResult.setText("Error: " + e.getMessage());
        });
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        if (response.isSuccessful() && response.body() != null) {
          try {
            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            String translation = json.get("translation").getAsString();
            if (json.has("audio")) currentAudioBase64 = json.get("audio").getAsString();

            runOnUiThread(() -> {
              progressBar.setVisibility(View.GONE);
              tvResult.setText(translation);
              
              // FORCE ENABLE BUTTON
              btnPlayAudio.setEnabled(true);
              btnPlayAudio.setAlpha(1.0f);
            });
          } catch (Exception e) {
            runOnUiThread(() -> progressBar.setVisibility(View.GONE));
          }
        } else {
            runOnUiThread(() -> progressBar.setVisibility(View.GONE));
        }
      }
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (audioPlayer != null) audioPlayer.cleanup();
    if (textToSpeech != null) {
      textToSpeech.stop();
      textToSpeech.shutdown();
    }
  }
}
