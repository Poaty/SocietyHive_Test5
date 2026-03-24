package com.example.societyhive_test5;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Firebase-backed login screen.
 *
 * Expected XML ids in activity_login.xml:
 * - etEmail
 * - etPassword
 * - btnLogin
 * - tvSignUp
 */
public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // Auto-skip login if already signed in
        //if (mAuth.getCurrentUser() != null) {
        //    startActivity(new Intent(LoginActivity.this, MainActivity.class));
        //    finish();
        //    return;
        //}

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        TextView tvSignUp = findViewById(R.id.tvSignUp);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            String msg = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Unknown login error";
                            Toast.makeText(LoginActivity.this,
                                    "Login failed: " + msg,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Fetch the user's saved theme before launching MainActivity so
                        // ThemeHelper.apply() in MainActivity reads the correct key immediately
                        // — no colour flash on the home screen.
                        String uid = mAuth.getCurrentUser().getUid();
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .get()
                                .addOnCompleteListener(docTask -> {
                                    if (docTask.isSuccessful()
                                            && docTask.getResult() != null
                                            && docTask.getResult().exists()) {
                                        String key = docTask.getResult().getString("themeKey");
                                        if (key != null && !key.isEmpty()) {
                                            ThemeHelper.save(LoginActivity.this, key);
                                        }
                                    }
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                });
                    });
        });

        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });
    }
}
