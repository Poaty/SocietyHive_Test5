package com.example.societyhive_test5;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Profile screen.
 *
 * Shows name, email, role and joined societies.
 * The "Manage" button on each society opens a bottom sheet with:
 *   - Open Society Chat
 *   - View Society Events
 *   - Leave Society (with confirmation + Firestore write)
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvName  = view.findViewById(R.id.tvProfileName);
        tvEmail = view.findViewById(R.id.tvProfileEmail);
        tvRole  = view.findViewById(R.id.tvProfileRole);

        RecyclerView rv = view.findViewById(R.id.rvJoinedSocieties);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setHasFixedSize(false);

        adapter = new JoinedSocietyAdapter(
                new ArrayList<>(),
                society -> showManageSheet(society)
        );

        rv.setAdapter(adapter);
        loadProfileFromFirestore();
    }

    // -------------------------------------------------------------------------
    // Firestore loading (unchanged from before)
    // -------------------------------------------------------------------------

    private void loadProfileFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            tvName.setText("No signed-in user");
            tvEmail.setText("Please log in");
            tvRole.setText("Guest");
            return;
        }

        tvName.setText("SocietyHive Member");
        tvEmail.setText(user.getEmail() != null ? user.getEmail() : "No email available");
        tvRole.setText("Member");

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (!isAdded() || !document.exists()) return;

                    String fullName = document.getString("fullName");
                    String email    = document.getString("email");
                    String role     = document.getString("role");

                    if (fullName != null && !fullName.isEmpty()) tvName.setText(fullName);
                    if (email    != null && !email.isEmpty())    tvEmail.setText(email);
                    if (role     != null && !role.isEmpty())     tvRole.setText(capitalize(role));

                    List<String> societyIds = (List<String>) document.get("societyIds");
                    loadSocieties(societyIds);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to load profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void loadSocieties(@Nullable List<String> societyIds) {
        societies.clear();
        adapter.updateList(societies);

        if (societyIds == null || societyIds.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (String societyId : societyIds) {
            if (societyId == null || societyId.trim().isEmpty()) continue;

            db.collection("societies")
                    .document(societyId)
                    .get()
                    .addOnSuccessListener(this::addSocietyIfValid)
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                "Failed to load a society: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void addSocietyIfValid(@NonNull DocumentSnapshot doc) {
        if (!isAdded() || !doc.exists()) return;

        String name        = doc.getString("name");
        String colorHex    = doc.getString("hexColor");
        String description = doc.getString("description");

        if (name        == null || name.trim().isEmpty())        name        = "Unnamed Society";
        if (colorHex    == null || colorHex.trim().isEmpty())    colorHex    = "#8D2E3A";
        if (description == null || description.trim().isEmpty()) description = "Joined society";

        societies.add(new Society(doc.getId(), name, description, colorHex));
        adapter.updateList(societies);
    }

    // -------------------------------------------------------------------------
    // Manage bottom sheet
    // -------------------------------------------------------------------------

    private void showManageSheet(@NonNull Society society) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_manage_society, null);
        sheet.setContentView(sheetView);

        // Populate header
        TextView tvName = sheetView.findViewById(R.id.tvSheetSocietyName);
        TextView tvDesc = sheetView.findViewById(R.id.tvSheetSocietyDesc);
        View accent     = sheetView.findViewById(R.id.viewSheetAccent);

        if (tvName != null) tvName.setText(society.getName());
        if (tvDesc != null) tvDesc.setText(society.getSubtitle());
        if (accent != null) {
            try { accent.setBackgroundColor(Color.parseColor(society.getColorHex())); }
            catch (IllegalArgumentException ignored) {}
        }

        // Open chat
        View rowChat = sheetView.findViewById(R.id.rowOpenChat);
        if (rowChat != null) {
            rowChat.setOnClickListener(v -> {
                sheet.dismiss();
                Bundle b = new Bundle();
                b.putString("societyId",  society.getId());
                b.putString("chatTitle",  society.getName());
                b.putString("chatColor",  society.getColorHex());
                NavHostFragment.findNavController(this)
                        .navigate(R.id.chatConversationFragment, b);
            });
        }

        // View events (navigate to events tab — filter can be added later)
        View rowEvents = sheetView.findViewById(R.id.rowViewEvents);
        if (rowEvents != null) {
            rowEvents.setOnClickListener(v -> {
                sheet.dismiss();
                NavHostFragment.findNavController(this)
                        .navigate(R.id.eventsFragment);
            });
        }

        // Leave society
        View rowLeave = sheetView.findViewById(R.id.rowLeaveSociety);
        if (rowLeave != null) {
            rowLeave.setOnClickListener(v -> {
                sheet.dismiss();
                confirmLeaveSociety(society);
            });
        }

        sheet.show();
    }

    // -------------------------------------------------------------------------
    // Leave society
    // -------------------------------------------------------------------------

    private void confirmLeaveSociety(@NonNull Society society) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Leave " + society.getName() + "?")
                .setMessage("You will no longer see this society's chat or events. You can re-join by scanning the society's QR code.")
                .setPositiveButton("Leave", (dialog, which) -> leaveSociety(society))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void leaveSociety(@NonNull Society society) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .update("societyIds", FieldValue.arrayRemove(society.getId()))
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Left " + society.getName(), Toast.LENGTH_SHORT).show();

                    // Remove from local list and refresh
                    societies.removeIf(s -> s.getId().equals(society.getId()));
                    adapter.updateList(societies);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to leave society: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // -------------------------------------------------------------------------

    private String capitalize(@NonNull String input) {
        if (input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
