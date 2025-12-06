package com.example.translationapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignupActivity extends AppCompatActivity {

    EditText usernameInput, emailInput, numberInput, passwordInput, confirmPasswordInput;
    Button signupBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        usernameInput = findViewById(R.id.username_input);
        emailInput = findViewById(R.id.email_input);
        numberInput = findViewById(R.id.number_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        signupBtn = findViewById(R.id.signup_btn);

        signupBtn.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String number = numberInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            if(username.isEmpty() || email.isEmpty() || number.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(SignupActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if(!password.equals(confirmPassword)) {
                Toast.makeText(SignupActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Call backend signup API
            new Thread(() -> {
                try {
                    OkHttpClient client = new OkHttpClient();

                    String json = "{"
                            + "\"username\":\"" + username + "\","
                            + "\"email\":\"" + email + "\","
                            + "\"phone\":\"" + number + "\","
                            + "\"password\":\"" + password + "\""
                            + "}";

                    RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                    // Use your local IP if testing on device, or 10.0.2.2 for emulator
                    Request request = new Request.Builder()
                            .url( Config.BASE_URL + "auth/signup")
                            .post(body)
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseBody = response.body().string();

                    runOnUiThread(() -> {
                        if(response.isSuccessful()) {
                            Toast.makeText(SignupActivity.this, "Signup successful", Toast.LENGTH_SHORT).show();
                            finish(); // Close activity and go back to login
                        } else {
                            Toast.makeText(SignupActivity.this, "Signup failed: " + responseBody, Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(SignupActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            }).start();
        });
    }
}
