package com.example.translationapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class TranslationActivity extends AppCompatActivity {

  EditText inputText;
  AutoCompleteTextView toLang;
  Button btnTranslate;
  TextView tvResult;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_translation);

    inputText = findViewById(R.id.et_input_text);
    toLang = findViewById(R.id.et_to_lang);
    btnTranslate = findViewById(R.id.btn_translate);
    tvResult = findViewById(R.id.tv_result);

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
        Toast.makeText(TranslationActivity.this, "Please specify both languages", Toast.LENGTH_SHORT).show();
        return;
      }

      // Call backend translation API
      tvResult.setText(R.string.translating);

      new Thread(() -> {
        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(Config.BASE_URL + "translate").newBuilder();
        urlBuilder.addQueryParameter("text", text);
        urlBuilder.addQueryParameter("to", to);
        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();

        try (Response response = client.newCall(request).execute()) {
          if (response.isSuccessful()) {
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
              String responseBodyString = responseBody.string();
              Gson gson = new Gson();
              TranslationResponse translationResponse = gson.fromJson(responseBodyString, TranslationResponse.class);
              String translatedText = translationResponse.getTranslation();
              runOnUiThread(() -> tvResult.setText(translatedText));
            }
          } else {
            runOnUiThread(() -> tvResult.setText("Error: " + response.message()));
          }
        } catch (IOException e) {
          runOnUiThread(() -> tvResult.setText("Error: " + e.getMessage()));
        }
      }).start();
    });
  }

  public static class TranslationResponse {
    private String translation;

    public String getTranslation() {
      return translation;
    }
  }
}
