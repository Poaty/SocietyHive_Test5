package com.example.societyhive_test5;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class SignUpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        MaterialButton btnCreateAccount = findViewById(R.id.btnCreateAccount);
        TextView tvLogin = findViewById(R.id.tvSignUp);

// "Create Account" button → back to Login
        btnCreateAccount.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish(); // prevent stacking multiple SignUp pages
        });

// "Log In" text → back to Login
        tvLogin.setOnClickListener(v -> {
            finish(); // just go back (cleaner than starting new LoginActivity)
        });
    }
}