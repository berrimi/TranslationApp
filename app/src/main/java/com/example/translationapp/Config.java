package com.example.translationapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.util.Base64;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Config {
    public static final String BASE_URL = "http://192.168.1.7:8080/translation-service/api/";
    private static final String PREF_NAME = "TranslationAppPrefs";
    private static final String KEY_USERNAME = "username";

    public static void saveUsername(Context context, String username) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_USERNAME, username)
                .apply();
    }

    public static String getUsername(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USERNAME, null);
    }

    public static void clearUserData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_USERNAME)
                .apply();
    }

    /**
     * Play audio from base64 string
     */
    public static void playAudioFromBase64(Context context, String base64Audio) {
        if (base64Audio == null || base64Audio.isEmpty()) {
            Toast.makeText(context, "No audio available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Decode base64 to byte array
            byte[] audioBytes = Base64.decode(base64Audio, Base64.DEFAULT);

            // Create temporary file
            File tempFile = File.createTempFile("audio", ".mp3", context.getCacheDir());
            tempFile.deleteOnExit();

            // Write bytes to file
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(audioBytes);
            fos.close();

            // Play audio using MediaPlayer
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(tempFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            // Release media player when done
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                tempFile.delete();
            });

        } catch (IOException e) {
            Toast.makeText(context, "Error playing audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}