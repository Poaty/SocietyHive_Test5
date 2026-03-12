package com.example.societyhive_test5;

import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Firestore-backed profile fragment.
 *
 * Reads:
 * users/{uid}
 *   - fullName
 *   - email
 *   - role
 *   - societyIds (array of society document IDs)
 *
 * Then resolves:
 * societies/{societyId}
 *   - name
 *   - hexColor
 *   - description
 */
public class ProfileFragment extends Fragment {

    private final List<Society> societies = new ArrayList<>();
    private JoinedSocietyAdapter adapter;

    private TextView tvName;
    private TextView tvEmail;
    private TextView tvRole;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvName = view.findViewById(R.id.tvProfileName);
        tvEmail = view.findViewById(R.id.tvProfileEmail);
        tvRole = view.findViewById(R.id.tvProfileRole);

        RecyclerView rv = view.findViewById(R.id.rvJoinedSocieties);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setHasFixedSize(true);

        adapter = new JoinedSocietyAdapter(
                new ArrayList<>(),
                society -> Toast.makeText(
                        requireContext(),
                        "Later: manage " + society.getName(),
                        Toast.LENGTH_SHORT
                ).show()
        );

        rv.setAdapter(adapter);

        loadProfileFromFirestore();
    }

    private void loadProfileFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            tvName.setText("No signed-in user");
            tvEmail.setText("Please log in");
            tvRole.setText("Guest");
            societies.clear();
            adapter.updateList(societies);
            return;
        }

        // Fallback values first
        tvName.setText("SocietyHive Member");
        tvEmail.setText(user.getEmail() != null ? user.getEmail() : "No email available");
        tvRole.setText("Member");

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Toast.makeText(requireContext(),
                                "User profile not found in Firestore.",
                                Toast.LENGTH_SHORT).show();
                        societies.clear();
                        adapter.updateList(societies);
                        return;
                    }

                    String fullName = document.getString("fullName");
                    String email = document.getString("email");
                    String role = document.getString("role");

                    if (fullName != null && !fullName.trim().isEmpty()) {
                        tvName.setText(fullName);
                    }
                    if (email != null && !email.trim().isEmpty()) {
                        tvEmail.setText(email);
                    }
                    if (role != null && !role.trim().isEmpty()) {
                        tvRole.setText(capitalize(role));
                    }

                    List<String> societyIds = (List<String>) document.get("societyIds");
                    loadSocieties(societyIds);
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Failed to load profile: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }

    private void loadSocieties(@Nullable List<String> societyIds) {
        societies.clear();

        if (societyIds == null || societyIds.isEmpty()) {
            adapter.updateList(societies);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (String societyId : societyIds) {
            if (societyId == null || societyId.trim().isEmpty()) continue;

            db.collection("societies")
                    .document(societyId)
                    .get()
                    .addOnSuccessListener(doc -> addSocietyIfValid(doc))
                    .addOnFailureListener(e -> Toast.makeText(requireContext(),
                            "Failed to load a society: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show());
        }
    }

    private void addSocietyIfValid(@NonNull DocumentSnapshot doc) {
        if (!doc.exists()) return;

        String name = doc.getString("name");
        String colorHex = doc.getString("hexColor");
        String description = doc.getString("description");

        if (name == null || name.trim().isEmpty()) {
            name = "Unnamed Society";
        }

        if (colorHex == null || colorHex.trim().isEmpty()) {
            colorHex = "#8D2E3A";
        }

        if (description == null || description.trim().isEmpty()) {
            description = "Joined society";
        }

        societies.add(new Society(
                doc.getId(),
                name,
                description,
                colorHex
        ));

        adapter.updateList(societies);
    }

    private String capitalize(@NonNull String input) {
        if (input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
