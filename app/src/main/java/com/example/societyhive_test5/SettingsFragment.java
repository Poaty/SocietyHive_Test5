package com.example.societyhive_test5;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Settings screen.
 *
 * Sections:
 *   Account    — display name (editable), email (read-only)
 *   Security   — change password (requires re-auth), send reset email
 *   Notifications — toggle switches (UI-only until FCM is wired up)
 *   Account Actions — sign out
 */
public class SettingsFragment extends Fragment {

    public SettingsFragment() {
        super(R.layout.fragment_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadAccountInfo(view);

        view.findViewById(R.id.rowEditName).setOnClickListener(v -> showEditNameDialog());
        view.findViewById(R.id.rowChangePassword).setOnClickListener(v -> showChangePasswordDialog());
        view.findViewById(R.id.rowResetPassword).setOnClickListener(v -> sendPasswordResetEmail());
        view.findViewById(R.id.rowSignOut).setOnClickListener(v -> confirmSignOut());
    }

    // -------------------------------------------------------------------------
    // Load account info
    // -------------------------------------------------------------------------

    private void loadAccountInfo(@NonNull View view) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        TextView tvEmail = view.findViewById(R.id.tvCurrentEmail);
        if (tvEmail != null && user.getEmail() != null) {
            tvEmail.setText(user.getEmail());
        }

        TextView tvName = view.findViewById(R.id.tvCurrentName);
        if (tvName == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    String name = doc.getString("fullName");
                    tvName.setText(name != null && !name.isEmpty() ? name : "Not set");
                });
    }

    // -------------------------------------------------------------------------
    // Edit display name
    // -------------------------------------------------------------------------

    private void showEditNameDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Build dialog with a TextInputLayout for a polished look
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_input_field, null);
        TextInputLayout til = dialogView.findViewById(R.id.tilDialogInput);
        TextInputEditText et = dialogView.findViewById(R.id.etDialogInput);

        if (til != null) til.setHint("Full name");

        // Pre-fill with current name
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    String current = doc.getString("fullName");
                    if (current != null && et != null) et.setText(current);
                });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Update Display Name")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    if (et == null) return;
                    String newName = et.getText() != null
                            ? et.getText().toString().trim() : "";

                    if (newName.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveDisplayName(user.getUid(), newName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveDisplayName(@NonNull String uid, @NonNull String name) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("fullName", name)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Name updated", Toast.LENGTH_SHORT).show();

                    // Refresh the subtitle in the row
                    View v = getView();
                    if (v != null) {
                        TextView tv = v.findViewById(R.id.tvCurrentName);
                        if (tv != null) tv.setText(name);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to update name: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // -------------------------------------------------------------------------
    // Change password (requires re-authentication)
    // -------------------------------------------------------------------------

    private void showChangePasswordDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        TextInputEditText etCurrent = dialogView.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNew = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = dialogView.findViewById(R.id.etConfirmNewPassword);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Update", null) // set manually to prevent auto-dismiss
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String current = etCurrent != null && etCurrent.getText() != null
                        ? etCurrent.getText().toString().trim() : "";
                String newPass = etNew != null && etNew.getText() != null
                        ? etNew.getText().toString().trim() : "";
                String confirm = etConfirm != null && etConfirm.getText() != null
                        ? etConfirm.getText().toString().trim() : "";

                if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                    Toast.makeText(requireContext(),
                            "Fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!newPass.equals(confirm)) {
                    Toast.makeText(requireContext(),
                            "New passwords don't match", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newPass.length() < 6) {
                    Toast.makeText(requireContext(),
                            "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                reauthAndChangePassword(user, current, newPass, dialog);
            });
        });

        dialog.show();
    }

    private void reauthAndChangePassword(@NonNull FirebaseUser user,
                                         @NonNull String currentPassword,
                                         @NonNull String newPassword,
                                         @NonNull AlertDialog dialog) {
        if (user.getEmail() == null) return;

        AuthCredential credential = EmailAuthProvider
                .getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> user.updatePassword(newPassword)
                        .addOnSuccessListener(v -> {
                            if (!isAdded()) return;
                            dialog.dismiss();
                            Toast.makeText(requireContext(),
                                    "Password updated successfully",
                                    Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(),
                                    "Failed to update password: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }))
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Current password is incorrect",
                            Toast.LENGTH_SHORT).show();
                });
    }

    // -------------------------------------------------------------------------
    // Password reset email
    // -------------------------------------------------------------------------

    private void sendPasswordResetEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        FirebaseAuth.getInstance()
                .sendPasswordResetEmail(user.getEmail())
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Reset email sent to " + user.getEmail(),
                            Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to send reset email: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // -------------------------------------------------------------------------
    // Sign out
    // -------------------------------------------------------------------------

    private void confirmSignOut() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
