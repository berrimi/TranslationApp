package com.example.translationapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignupActivity extends AppCompatActivity {

    private TextInputLayout usernameLayout, emailLayout, numberLayout, passwordLayout, confirmPasswordLayout;
    private TextInputEditText usernameInput, emailInput, numberInput, passwordInput, confirmPasswordInput;
    private Button signupBtn;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize views
        initializeViews();

        // Setup text change listeners for real-time validation
        setupTextChangeListeners();

        // Setup signup button click listener
        setupSignupButton();
    }

    private void initializeViews() {
        usernameLayout = findViewById(R.id.username_layout);
        emailLayout = findViewById(R.id.email_layout);
        numberLayout = findViewById(R.id.number_layout);
        passwordLayout = findViewById(R.id.password_layout);
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout);

        usernameInput = findViewById(R.id.username_input);
        emailInput = findViewById(R.id.email_input);
        numberInput = findViewById(R.id.number_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);

        signupBtn = findViewById(R.id.signup_btn);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupTextChangeListeners() {
        // Username validation
        usernameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String username = s.toString().trim();
                if (username.isEmpty()) {
                    usernameLayout.setError("Username is required");
                } else if (username.length() < 3) {
                    usernameLayout.setError("Username must be at least 3 characters");
                } else {
                    usernameLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Email validation
        emailInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String email = s.toString().trim();
                if (email.isEmpty()) {
                    emailLayout.setError("Email is required");
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailLayout.setError("Please enter a valid email");
                } else {
                    emailLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Phone number validation
        numberInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String number = s.toString().trim();
                if (number.isEmpty()) {
                    numberLayout.setError("Phone number is required");
                } else if (number.length() < 8) {
                    numberLayout.setError("Please enter a valid phone number");
                } else {
                    numberLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Password validation
        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString().trim();
                if (password.isEmpty()) {
                    passwordLayout.setError("Password is required");
                } else if (password.length() < 6) {
                    passwordLayout.setError("Password must be at least 6 characters");
                } else {
                    passwordLayout.setError(null);
                }

                // Also validate confirm password
                validatePasswordMatch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Confirm password validation
        confirmPasswordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePasswordMatch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void validatePasswordMatch() {
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (!confirmPassword.isEmpty() && !password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords do not match");
        } else {
            confirmPasswordLayout.setError(null);
        }
    }

    private void setupSignupButton() {
        signupBtn.setOnClickListener(v -> {
            // Clear previous errors
            clearErrors();

            // Get input values
            String username = usernameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String number = numberInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            // Validate inputs
            if (!validateInputs(username, email, number, password, confirmPassword)) {
                return;
            }

            // Start signup process
            signupUser(username, email, number, password);
        });
    }

    private void clearErrors() {
        usernameLayout.setError(null);
        emailLayout.setError(null);
        numberLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);
    }

    private boolean validateInputs(String username, String email, String number,
                                   String password, String confirmPassword) {
        boolean isValid = true;

        if (username.isEmpty()) {
            usernameLayout.setError("Username is required");
            isValid = false;
        } else if (username.length() < 3) {
            usernameLayout.setError("Username must be at least 3 characters");
            isValid = false;
        }

        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Please enter a valid email");
            isValid = false;
        }

        if (number.isEmpty()) {
            numberLayout.setError("Phone number is required");
            isValid = false;
        } else if (number.length() < 8) {
            numberLayout.setError("Please enter a valid phone number");
            isValid = false;
        }

        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.setError("Please confirm your password");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords do not match");
            isValid = false;
        }

        return isValid;
    }

    private void signupUser(String username, String email, String number, String password) {
        // Show progress bar and disable button
        progressBar.setVisibility(View.VISIBLE);
        signupBtn.setEnabled(false);

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // Create JSON object using Gson for proper escaping
                SignupRequest signupRequest = new SignupRequest(username, password, email, number);
                String json = new Gson().toJson(signupRequest);

                RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));

                Request request = new Request.Builder()
                        .url(Config.BASE_URL + "auth/signup")
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    signupBtn.setEnabled(true);

                    if (response.isSuccessful()) {
                        // Parse response to check for success message
                        try {
                            Gson gson = new Gson();
                            SignupResponse signupResponse = gson.fromJson(responseBody, SignupResponse.class);

                            if (signupResponse != null && signupResponse.getMessage() != null) {
                                Toast.makeText(SignupActivity.this,
                                        signupResponse.getMessage(),
                                        Toast.LENGTH_SHORT).show();

                                // Clear form and go back to login
                                clearForm();
                                finish();
                            } else {
                                Toast.makeText(SignupActivity.this,
                                        "Signup successful",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } catch (Exception e) {
                            Toast.makeText(SignupActivity.this,
                                    "Signup successful",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        // Try to parse error message from response
                        try {
                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(responseBody, ErrorResponse.class);

                            if (errorResponse != null && errorResponse.getError() != null) {
                                showSignupError(errorResponse.getError());
                            } else {
                                showSignupError("Signup failed: " + responseBody);
                            }
                        } catch (Exception e) {
                            showSignupError("Signup failed: " + responseBody);
                        }
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    signupBtn.setEnabled(true);
                    showSignupError("Error: " + e.getMessage());
                });
            }
        }).start();
    }

    private void showSignupError(String message) {
        // Check if it's a username conflict error
        if (message.toLowerCase().contains("username") && message.toLowerCase().contains("exists")) {
            usernameLayout.setError("Username already exists");
        } else if (message.toLowerCase().contains("email") && message.toLowerCase().contains("exists")) {
            emailLayout.setError("Email already registered");
        } else {
            Toast.makeText(SignupActivity.this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void clearForm() {
        usernameInput.setText("");
        emailInput.setText("");
        numberInput.setText("");
        passwordInput.setText("");
        confirmPasswordInput.setText("");
        clearErrors();
    }

    // Handle login link click
    public void onLoginClick(View view) {
        finish(); // Close activity and go back to login
    }

    // Model classes for JSON serialization/deserialization

    private static class SignupRequest {
        private String username;
        private String password;
        private String email;
        private String phone;

        public SignupRequest(String username, String password, String email, String phone) {
            this.username = username;
            this.password = password;
            this.email = email;
            this.phone = phone;
        }
    }

    private static class SignupResponse {
        private String message;

        public String getMessage() {
            return message;
        }
    }

    private static class ErrorResponse {
        private String error;

        public String getError() {
            return error;
        }
    }
}