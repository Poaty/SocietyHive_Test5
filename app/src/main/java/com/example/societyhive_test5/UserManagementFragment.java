package com.example.societyhive_test5;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen for managing users.
 *
 * - Lists all users from the `users` Firestore collection.
 * - Toggle switch sets `role` to "admin" or "member".
 * - Shows a count badge for pending join requests (from `joinRequests` collection).
 * - Live search filters the displayed list by name.
 */
public class UserManagementFragment extends Fragment {

    private UserManagementAdapter adapter;
    private TextView tvRequestCount;

    public UserManagementFragment() {
        super(R.layout.fragment_user_management);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvRequestCount = view.findViewById(R.id.tvRequestCount);

        // RecyclerView
        RecyclerView rvUsers = view.findViewById(R.id.rvUsers);
        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUsers.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        adapter = new UserManagementAdapter(this::onRoleToggled);
        rvUsers.setAdapter(adapter);

        // Live search
        EditText etSearch = view.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadUsers();
        loadJoinRequestCount();
    }

    // -------------------------------------------------------------------------
    // Load users
    // -------------------------------------------------------------------------

    private void loadUsers() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        FirebaseFirestore.getInstance()
                .collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    List<UserManagementAdapter.UserItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        // Skip the current admin to prevent self-demotion
                        if (doc.getId().equals(currentUid)) continue;

                        String fullName = doc.getString("fullName");
                        String role     = doc.getString("role");
                        boolean isAdmin = "admin".equalsIgnoreCase(role);

                        items.add(new UserManagementAdapter.UserItem(
                                doc.getId(),
                                fullName != null && !fullName.isEmpty() ? fullName : "Unknown User",
                                isAdmin));
                    }
                    adapter.setUsers(items);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to load users: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // -------------------------------------------------------------------------
    // Join requests badge
    // -------------------------------------------------------------------------

    private void loadJoinRequestCount() {
        FirebaseFirestore.getInstance()
                .collection("joinRequests")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    int count = querySnapshot.size();
                    if (count > 0) {
                        tvRequestCount.setText(String.valueOf(count));
                        tvRequestCount.setVisibility(View.VISIBLE);
                    }
                });
        // If collection doesn't exist yet, badge stays hidden — no error shown.
    }

    // -------------------------------------------------------------------------
    // Role toggle
    // -------------------------------------------------------------------------

    private void onRoleToggled(String uid, boolean makeAdmin) {
        String newRole = makeAdmin ? "admin" : "member";
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("role", newRole)
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to update role: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}
