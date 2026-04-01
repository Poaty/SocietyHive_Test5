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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User Management screen.
 *
 * Super admin (no args):   shows all users, can promote to society admin.
 * Society admin (societyFilter arg):  shows only members of that society,
 *   can remove members and handle join requests for that society.
 */
public class UserManagementFragment extends Fragment {

    private static final String ARG_SOCIETY_FILTER = "societyFilter";

    private String societyFilter = null;

    private final List<UserItem> allUsers    = new ArrayList<>();
    private final List<UserItem> filtered    = new ArrayList<>();
    private UserManagementAdapter adapter;

    public UserManagementFragment() {
        super(R.layout.fragment_user_management);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            societyFilter = getArguments().getString(ARG_SOCIETY_FILTER, null);
            if (societyFilter != null && societyFilter.isEmpty()) societyFilter = null;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rvUsers);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new UserManagementAdapter();
        rv.setAdapter(adapter);

        adapter.setClickListener(user -> showUserOptions(user));

        EditText etSearch = view.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }
        });

        // Join requests card
        view.findViewById(R.id.cardJoinRequests).setOnClickListener(v -> showJoinRequests());

        loadJoinRequestCount(view);
        loadUsers();
    }

    private void loadUsers() {
        FirebaseFirestore.getInstance().collection("users").get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    allUsers.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        List<String> societyIds = (List<String>) doc.get("societyIds");
                        List<String> adminOf    = (List<String>) doc.get("adminOf");

                        // Filter by society if we're in society admin mode
                        if (societyFilter != null) {
                            if (societyIds == null || !societyIds.contains(societyFilter)) continue;
                        }

                        allUsers.add(new UserItem(
                                doc.getId(),
                                doc.getString("fullName"),
                                doc.getString("email"),
                                doc.getString("role"),
                                adminOf
                        ));
                    }
                    filtered.clear();
                    filtered.addAll(allUsers);
                    adapter.updateList(filtered);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to load users", Toast.LENGTH_SHORT).show();
                });
    }

    private void filterUsers(String query) {
        String q = query.toLowerCase().trim();
        filtered.clear();
        for (UserItem u : allUsers) {
            if (u.getFullName().toLowerCase().contains(q) || u.getEmail().toLowerCase().contains(q)) {
                filtered.add(u);
            }
        }
        adapter.updateList(filtered);
    }

    private void loadJoinRequestCount(@NonNull View view) {
        TextView tvCount = view.findViewById(R.id.tvRequestCount);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        com.google.firebase.firestore.Query query = (societyFilter != null)
                ? db.collection("joinRequests")
                        .whereEqualTo("societyId", societyFilter)
                        .whereEqualTo("status", "pending")
                : db.collection("joinRequests")
                        .whereEqualTo("status", "pending");

        query.get().addOnSuccessListener(snap -> {
            if (!isAdded()) return;
            int count = snap.size();
            if (count > 0) {
                tvCount.setText(String.valueOf(count));
                tvCount.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showJoinRequests() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        com.google.firebase.firestore.Query query = (societyFilter != null)
                ? db.collection("joinRequests")
                        .whereEqualTo("societyId", societyFilter)
                        .whereEqualTo("status", "pending")
                : db.collection("joinRequests")
                        .whereEqualTo("status", "pending");

        query.get().addOnSuccessListener(snap -> {
            if (!isAdded() || snap.isEmpty()) {
                Toast.makeText(requireContext(), "No pending requests", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> labels = new ArrayList<>();
            List<String> docIds = new ArrayList<>();
            List<String> uids   = new ArrayList<>();
            List<String> sids   = new ArrayList<>();

            for (QueryDocumentSnapshot doc : snap) {
                String uid = doc.getString("userId");
                String sid = doc.getString("societyId");
                String name = doc.getString("userName");
                if (name == null) name = uid;
                labels.add(name + " → " + sid);
                docIds.add(doc.getId());
                uids.add(uid != null ? uid : "");
                sids.add(sid != null ? sid : "");
            }

            String[] items = labels.toArray(new String[0]);
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Pending Join Requests")
                    .setItems(items, (dialog, which) ->
                            showApproveRejectDialog(docIds.get(which), uids.get(which), sids.get(which), labels.get(which)))
                    .setNegativeButton("Close", null)
                    .show();
        });
    }

    private void showApproveRejectDialog(String reqId, String uid, String sid, String label) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(label)
                .setPositiveButton("Approve", (d, w) -> approveRequest(reqId, uid, sid))
                .setNegativeButton("Reject", (d, w)  -> rejectRequest(reqId))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void approveRequest(String reqId, String uid, String sid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Add user to society
        db.collection("users").document(uid)
                .update("societyIds", com.google.firebase.firestore.FieldValue.arrayUnion(sid))
                .addOnSuccessListener(unused ->
                        db.collection("joinRequests").document(reqId)
                                .update("status", "approved")
                                .addOnSuccessListener(u2 -> {
                                    if (!isAdded()) return;
                                    Toast.makeText(requireContext(), "Request approved!", Toast.LENGTH_SHORT).show();
                                    loadUsers();
                                }));
    }

    private void rejectRequest(String reqId) {
        FirebaseFirestore.getInstance().collection("joinRequests").document(reqId)
                .update("status", "rejected")
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Request rejected", Toast.LENGTH_SHORT).show();
                });
    }

    private void showUserOptions(@NonNull UserItem user) {
        List<String> options = new ArrayList<>();
        options.add("Make Society Admin");
        if (societyFilter != null) options.add("Remove from Society");

        String[] items = options.toArray(new String[0]);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(user.getFullName().isEmpty() ? "User" : user.getFullName())
                .setItems(items, (dialog, which) -> {
                    if (items[which].equals("Make Society Admin")) {
                        showPickSocietyForAdmin(user);
                    } else if (items[which].equals("Remove from Society") && societyFilter != null) {
                        removeFromSociety(user);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeFromSociety(@NonNull UserItem user) {
        FirebaseFirestore.getInstance().collection("users").document(user.getId())
                .update("societyIds", com.google.firebase.firestore.FieldValue.arrayRemove(societyFilter))
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Removed from society", Toast.LENGTH_SHORT).show();
                    allUsers.remove(user);
                    filtered.remove(user);
                    adapter.updateList(filtered);
                });
    }

    private void showPickSocietyForAdmin(@NonNull UserItem user) {
        FirebaseFirestore.getInstance().collection("societies").get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    List<String> names = new ArrayList<>();
                    List<String> ids   = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        String n = doc.getString("name");
                        names.add(n != null ? n : doc.getId());
                        ids.add(doc.getId());
                    }
                    String[] items = names.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Admin of which society?")
                            .setItems(items, (d, which) -> {
                                Map<String, Object> update = new HashMap<>();
                                update.put("adminOf", com.google.firebase.firestore.FieldValue.arrayUnion(ids.get(which)));
                                FirebaseFirestore.getInstance()
                                        .collection("users").document(user.getId())
                                        .update(update)
                                        .addOnSuccessListener(u -> {
                                            if (!isAdded()) return;
                                            Toast.makeText(requireContext(),
                                                    user.getFullName() + " is now admin of " + names.get(which),
                                                    Toast.LENGTH_SHORT).show();
                                            loadUsers();
                                        });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }
}
