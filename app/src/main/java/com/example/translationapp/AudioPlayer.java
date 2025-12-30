package com.example.translationapp;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Base64;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioPlayer {

    private MediaPlayer mediaPlayer;
    private final Context context;

    public AudioPlayer(Context context) {
        this.context = context;
    }

    /**
     * Play audio from base64 string
     */
    public void playAudio(String base64Audio) {
        if (base64Audio == null || base64Audio.isEmpty()) {
            Toast.makeText(context, "No audio available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Stop any currently playing audio
        stop();

        new PlayAudioTask().execute(base64Audio);
    }

    /**
     * Stop currently playing audio
     */
    public void stop() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * Check if audio is playing
     */
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        stop();
    }

    private class PlayAudioTask extends AsyncTask<String, Void, File> {

        @Override
        protected File doInBackground(String... params) {
            String base64Audio = params[0];

            try {
                // Decode base64 to byte array
                byte[] audioBytes = Base64.decode(base64Audio, Base64.DEFAULT);

                // Create temporary file
                File tempFile = File.createTempFile("audio_" + System.currentTimeMillis(), ".mp3", context.getCacheDir());
                tempFile.deleteOnExit();

                // Write bytes to file
                FileOutputStream fos = new FileOutputStream(tempFile);
                fos.write(audioBytes);
                fos.close();

                return tempFile;

            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(File tempFile) {
            if (tempFile == null) {
                Toast.makeText(context, "Error preparing audio", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(tempFile.getAbsolutePath());
                mediaPlayer.prepare();
                mediaPlayer.start();

                // Release media player when done
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    mediaPlayer = null;
                    tempFile.delete();
                });

            } catch (IOException e) {
                Toast.makeText(context, "Error playing audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }
}