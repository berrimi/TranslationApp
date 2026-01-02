package com.example.translationapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    EditText username_input, password_input;
    Button login;
    TextView signupTextView;
    String username, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        username_input = findViewById(R.id.username_input);
        password_input = findViewById(R.id.password_input);
        login = findViewById(R.id.login_btn);
        signupTextView = findViewById(R.id.tv_signup_link);

        String text = "Don't have an account? Sign up";

        // Create a SpannableString so we can make only "Sign up" clickable
        SpannableString spannableString = new SpannableString(text);

        // Find start and end of "Sign up"
        int start = text.indexOf("Sign up");
        int end = start + "Sign up".length();

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // Open SignupActivity
                Intent intent = new Intent(MainActivity.this, SignupActivity.class);
                startActivity(intent);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                // Use the context from the Activity
                ds.setColor(MainActivity.this.getResources().getColor(R.color.primary_600, MainActivity.this.getTheme()));
                ds.setUnderlineText(false); // Remove underline for cleaner look
            }
        };

        // Apply the clickable span
        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the text and make links clickable
        signupTextView.setText(spannableString);
        signupTextView.setMovementMethod(LinkMovementMethod.getInstance());
        signupTextView.setHighlightColor(Color.TRANSPARENT);

        login.setOnClickListener(view -> {
            username = username_input.getText().toString().trim();
            password = password_input.getText().toString().trim();

            if(username.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Call backend API on a background thread
            new Thread(() -> {
                try {
                    OkHttpClient client = new OkHttpClient();

                    // JSON body
                    String json = "{ \"username\": \"" + username + "\", \"password\": \"" + password + "\" }";

                    RequestBody body = RequestBody.create(json, okhttp3.MediaType.parse("application/json"));

                    Request request = new Request.Builder()
                            .url(Config.BASE_URL + "auth/login")
                            .post(body)
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseBody = response.body().string();

                    runOnUiThread(() -> {
                        if(response.isSuccessful()) {
                            // Login successful - save username
                            Config.saveUsername(MainActivity.this, username);

                            Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MainActivity.this, TranslationActivity.class);
                            startActivity(intent);
                            finish(); // Prevent going back to login
                        } else {
                            // Login failed
                            Toast.makeText(MainActivity.this, "Login failed: " + responseBody, Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            }).start();
        });

    }
}