package com.example.societyhive_test5;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // grab views




        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        TextView tvSignUp = findViewById(R.id.tvSignUp);

        btnLogin.setOnClickListener(v -> {
            String email = TextHelpers.trimmed(etEmail);
            String password = TextHelpers.trimmed(etPassword);

            // dont bother if fields are empty
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // actually try to sign in
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




                        // pull their theme preference before launching MainActivity so it doesn't flicker
                        String uid = mAuth.getCurrentUser().getUid();
                        com.google.firebase.firestore.FirebaseFirestore db =
                                com.google.firebase.firestore.FirebaseFirestore.getInstance();
                        CollectionReference usersCollection = db.collection("users");
                        DocumentReference userDocument = usersCollection.document(uid);
                        userDocument.get()
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
