package com.example.translationapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AccountSettingsActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPhone, etOldPassword, etNewPassword, etDeleteConfirmPassword;
    private MaterialButton btnUpdateProfile, btnChangePassword, btnDeleteAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etOldPassword = findViewById(R.id.et_old_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etDeleteConfirmPassword = findViewById(R.id.et_delete_confirm_password);
        
        btnUpdateProfile = findViewById(R.id.btn_update_profile);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnDeleteAccount = findViewById(R.id.btn_delete_account);

        String username = Config.getUsername(this);
        etUsername.setText(username);

        // Fetch current user data from API
        loadUserData(username);

        btnUpdateProfile.setOnClickListener(v -> updateProfile(username));
        btnChangePassword.setOnClickListener(v -> changePassword(username));
        btnDeleteAccount.setOnClickListener(v -> showDeleteConfirmation(username));
    }

    private void loadUserData(String username) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(Config.BASE_URL + "auth/user/" + username)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                        
                        String email = json.has("email") && !json.get("email").isJsonNull() ? json.get("email").getAsString() : "";
                        String phone = json.has("phone") && !json.get("phone").isJsonNull() ? json.get("phone").getAsString() : "";

                        runOnUiThread(() -> {
                            etEmail.setText(email);
                            etPhone.setText(phone);
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateProfile(String username) {
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (email.isEmpty() && phone.isEmpty()) {
            Toast.makeText(this, "Please enter an email or phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                JsonObject jsonObject = new JsonObject();
                if (!email.isEmpty()) jsonObject.addProperty("email", email);
                if (!phone.isEmpty()) jsonObject.addProperty("phone", phone);

                RequestBody body = RequestBody.create(jsonObject.toString(), MediaType.parse("application/json"));
                
                Request request = new Request.Builder()
                        .url(Config.BASE_URL + "auth/user/" + username)
                        .put(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void changePassword(String username) {
        String oldPass = etOldPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (oldPass.isEmpty() || newPass.isEmpty()) {
            Toast.makeText(this, "Please fill both current and new password fields", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("oldPassword", oldPass);
                jsonObject.addProperty("newPassword", newPass);

                RequestBody body = RequestBody.create(jsonObject.toString(), MediaType.parse("application/json"));
                
                Request request = new Request.Builder()
                        .url(Config.BASE_URL + "auth/user/" + username + "/password")
                        .put(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                            etOldPassword.setText("");
                            etNewPassword.setText("");
                        } else {
                            Toast.makeText(this, "Password change failed: Check current password", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showDeleteConfirmation(String username) {
        String password = etDeleteConfirmPassword.getText().toString().trim();
        if (password.isEmpty()) {
            Toast.makeText(this, "Enter password to confirm deletion", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Account?")
                .setMessage("Are you sure you want to permanently delete your account?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount(username, password))
                .show();
    }

    private void deleteAccount(String username, String password) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(Config.BASE_URL + "auth/user/" + username + "?password=" + password)
                        .delete()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                            Config.clearUserData(this);
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Delete failed: Check password", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
