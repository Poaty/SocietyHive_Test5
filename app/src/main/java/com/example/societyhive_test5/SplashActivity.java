package com.example.societyhive_test5;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // If user is already logged in, go straight to MainActivity
            // Otherwise go to LoginActivity
            Class<?> destination = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? MainActivity.class
                    : LoginActivity.class;

            startActivity(new Intent(this, destination));
            finish();
        }, 1500);
    }
}
