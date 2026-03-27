package com.example.societyhive_test5;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Home screen.
 *
 * - Loads the signed-in user's name, role and society IDs from Firestore in one read.
 * - Shows the Admin Items section only when role == "admin".
 * - Loads and displays announcements for the user's societies inline (above Quick Access).
 * - Wires all dashboard tiles to their nav destinations.
 */
public class HomeFragment extends Fragment {

    private AnnouncementsAdapter announcementsAdapter;
    private final List<Announcement> announcements = new ArrayList<>();
    private final Set<String> userSocietyIds = new HashSet<>();
    private boolean isAdmin = false;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvAnnouncements = view.findViewById(R.id.rvAnnouncements);
        rvAnnouncements.setLayoutManager(new LinearLayoutManager(requireContext()));
        announcementsAdapter = new AnnouncementsAdapter();
        rvAnnouncements.setAdapter(announcementsAdapter);

        wireTiles(view);
        loadUserData(view);
    }

    // -------------------------------------------------------------------------
    // Single Firestore read: name + role + societyIds
    // -------------------------------------------------------------------------

    private void loadUserData(@NonNull View view) {
        TextView tvWelcome = view.findViewById(R.id.tvWelcome);
        if (tvWelcome != null) tvWelcome.setText("Welcome back");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;

                    // Name
                    String fullName = doc.getString("fullName");
                    if (tvWelcome != null && fullName != null && !fullName.trim().isEmpty()) {
                        String firstName = fullName.trim().split("\\s+")[0];
                        tvWelcome.setText("Welcome, " + firstName);
                    } else if (tvWelcome != null) {
                        tvWelcome.setText("Welcome");
                    }

                    // Role
                    String role = doc.getString("role");
                    isAdmin = "admin".equalsIgnoreCase(role);
                    showAdminSection(view, isAdmin);

                    // Society IDs
                    userSocietyIds.clear();
                    List<?> ids = (List<?>) doc.get("societyIds");
                    if (ids != null) {
                        for (Object id : ids) {
                            if (id instanceof String) userSocietyIds.add((String) id);
                        }
                    }

                    loadAnnouncements(view);
                })
                .addOnFailureListener(e -> {
                    // Silent — placeholder text stays
                });
    }

    private void showAdminSection(@NonNull View view, boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        view.findViewById(R.id.tvAdminItems).setVisibility(visibility);
        view.findViewById(R.id.cardAdminDashboard).setVisibility(visibility);
    }

    // -------------------------------------------------------------------------
    // Announcements
    // -------------------------------------------------------------------------

    private void loadAnnouncements(@NonNull View view) {
        FirebaseFirestore.getInstance()
                .collection("pins")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    announcements.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String societyId = doc.getString("societyId");
                        // Admins see all pins; regular users see pins for their societies only
                        if (!isAdmin && societyId != null && !societyId.isEmpty()
                                && !userSocietyIds.contains(societyId)) continue;

                        Announcement a = new Announcement();
                        a.setId(doc.getId());
                        // Pins have no separate title — show content as the headline
                        a.setTitle(safeString(doc.getString("content"), ""));
                        a.setContent("");
                        a.setSocietyId(societyId != null ? societyId : "");
                        a.setCreatedBy(safeString(doc.getString("createdBy"), ""));
                        a.setCreatedAt(doc.getTimestamp("createdAt"));
                        announcements.add(a);
                    }

                    fetchAnnouncementSocietyNames(view);
                })
                .addOnFailureListener(e -> {
                    // Silent — no pins shown
                });
    }

    private void fetchAnnouncementSocietyNames(@NonNull View view) {
        Set<String> ids = new HashSet<>();
        for (Announcement a : announcements) {
            if (!a.getSocietyId().isEmpty()) ids.add(a.getSocietyId());
        }

        if (ids.isEmpty()) {
            publishAnnouncements(view);
            return;
        }

        final int[] remaining = {ids.size()};
        final Map<String, String> nameMap = new HashMap<>();

        for (String sid : ids) {
            FirebaseFirestore.getInstance()
                    .collection("societies").document(sid).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            String name = task.getResult().getString("name");
                            if (name != null) nameMap.put(sid, name);
                        }
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            for (Announcement a : announcements) {
                                String name = nameMap.get(a.getSocietyId());
                                if (name != null) a.setSocietyName(name);
                            }
                            if (isAdded()) publishAnnouncements(view);
                        }
                    });
        }
    }

    private void publishAnnouncements(@NonNull View view) {
        announcementsAdapter.updateList(announcements);

        int visibility = announcements.isEmpty() ? View.GONE : View.VISIBLE;
        view.findViewById(R.id.tvAnnouncementsLabel).setVisibility(visibility);
        view.findViewById(R.id.rvAnnouncements).setVisibility(visibility);
    }

    // -------------------------------------------------------------------------
    // Tile navigation
    // -------------------------------------------------------------------------

    private void wireTiles(@NonNull View view) {
        wire(view, R.id.tileEvents,            R.id.eventsFragment);
        wire(view, R.id.tileChats,             R.id.chatsFragment);
        wire(view, R.id.tileQr,                R.id.qrFragment);
        wire(view, R.id.tileCalendar,          R.id.calendarFragment);
        wire(view, R.id.tilePolls,             R.id.pollsFragment);
        wire(view, R.id.tilePostAnnouncement,  R.id.createAnnouncementFragment);
        wire(view, R.id.tileUserManagement,    R.id.userManagementFragment);
        wire(view, R.id.tileCreatePoll,        R.id.createPollFragment);
        wire(view, R.id.tileCreateEvent,       R.id.createEventFragment);
        wire(view, R.id.tileCreatePin,         R.id.createPinFragment);
        wire(view, R.id.tileEditSociety,       R.id.editSocietyFragment);
        // tileGallery — destination not yet implemented
    }

    private void wire(@NonNull View root, int tileId, int destId) {
        View tile = root.findViewById(tileId);
        if (tile != null) {
            tile.setOnClickListener(v ->
                    NavHostFragment.findNavController(this).navigate(destId));
        }
    }

    // -------------------------------------------------------------------------

    @NonNull
    private String safeString(@Nullable String value, @NonNull String fallback) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : fallback;
    }
}
