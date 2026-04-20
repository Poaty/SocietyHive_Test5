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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsFragment extends Fragment {

    public SettingsFragment() {
        super(R.layout.fragment_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fetchAccountDetails(view);

        view.findViewById(R.id.rowEditName).setOnClickListener(v -> showEditNameDialog());
        view.findViewById(R.id.rowChangePassword).setOnClickListener(v -> showChangePasswordDialog());
        view.findViewById(R.id.rowSignOut).setOnClickListener(v -> confirmSignOut());
    }


    private void fetchAccountDetails(@NonNull View view) {
        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null) return;

        TextView tvName = view.findViewById(R.id.tvCurrentName);
        if (tvName == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDoc = db.collection("users").document(user.getUid());
        userDoc.get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    String name = doc.getString("fullName");
                    tvName.setText(name != null && !name.isEmpty() ? name : "Not set");
                });
    }





    private void showEditNameDialog() {
        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null) return;


        View dialogView = getLayoutInflater().inflate(R.layout.dialog_input_field, null);
        TextInputLayout til = dialogView.findViewById(R.id.tilDialogInput);
        TextInputEditText et = dialogView.findViewById(R.id.etDialogInput);

        if (til != null) til.setHint("Full name");


        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference col = db.collection("users");
        DocumentReference userRef = col.document(user.getUid());
        userRef.get()
                .addOnSuccessListener(doc -> {
                    if (getContext() == null) return;
                    String current = doc.getString("fullName");
                    if (current != null && et != null) et.setText(current);
                });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Update Display Name")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    if (et == null) return;
                    String newName = et.getText() != null
                            ? TextHelpers.trimmed(et) : "";

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

    private void saveDisplayName(@NonNull String uid, String name) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDocument = db.collection("users").document(uid);
        userDocument.update("fullName", name)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireActivity(),
                            "Name updated", Toast.LENGTH_SHORT).show();

                    // update the label in place so it doesn't need a full reload
                    View v = getView();
                    if (v != null) {
                        TextView tv = v.findViewById(R.id.tvCurrentName);
                        if (tv != null) tv.setText(name);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "update failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }





    private void showChangePasswordDialog() {
        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        TextInputEditText etCurrent = dialogView.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNew = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = dialogView.findViewById(R.id.etConfirmNewPassword);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String current = etCurrent != null && etCurrent.getText() != null
                        ? TextHelpers.trimmed(etCurrent) : "";
                String newPass = etNew != null && etNew.getText() != null
                        ? TextHelpers.trimmed(etNew) : "";
                String confirm = etConfirm != null && etConfirm.getText() != null
                        ? TextHelpers.trimmed(etConfirm) : "";

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
                            if (getContext() == null) return;
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





    private void confirmSignOut() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    AuthHelpers.signOut();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
