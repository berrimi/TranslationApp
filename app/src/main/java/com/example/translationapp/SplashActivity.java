package com.example.translationapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Optional: If you want to show the layout with your logo
        setContentView(R.layout.activity_splash);

        // Use the main looper for the handler
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Always go to SignupActivity
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);

            // Finish Splash so the user can't go back to it
            finish();
        }, SPLASH_DURATION);
    }
}