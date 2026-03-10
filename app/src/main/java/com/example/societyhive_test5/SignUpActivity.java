package com.example.societyhive_test5;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Firebase-backed sign-up screen.
 *
 * Expected XML ids in activity_sign_up.xml:
 * - etFullName
 * - etEmail
 * - etPassword
 * - etConfirmPassword
 * - btnCreateAccount
 * - tvSignUp   (the "Log In" text link on your current screen)
 *
 * If your XML ids differ, rename the findViewById references to match.
 */
public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        EditText etFullName = findViewById(R.id.etFullName);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);

        MaterialButton btnCreateAccount = findViewById(R.id.btnCreateAccount);
        TextView tvLogin = findViewById(R.id.tvSignUp);

        btnCreateAccount.setOnClickListener(v -> {
            String fullName = etFullName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            String msg = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Unknown sign up error";
                            Toast.makeText(this,
                                    "Sign up failed: " + msg,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(this,
                                    "Account created but user session unavailable.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("fullName", fullName);
                        userData.put("email", user.getEmail());
                        userData.put("role", "member");
                        userData.put("societyIds", new ArrayList<String>());
                        userData.put("createdAt", FieldValue.serverTimestamp());

                        db.collection("users")
                                .document(user.getUid())
                                .set(userData)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();

                                    // Sign out so the user returns through the login flow cleanly
                                    mAuth.signOut();

                                    startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this,
                                            "Auth created, but profile save failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    });
        });

        tvLogin.setOnClickListener(v -> finish());
    }
}
