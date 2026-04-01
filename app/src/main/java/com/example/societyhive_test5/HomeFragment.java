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

public class HomeFragment extends Fragment {

    private AnnouncementsAdapter announcementsAdapter;
    private final List<Announcement> announcements = new ArrayList<>();
    private final Set<String> userSocietyIds = new HashSet<>();
    private boolean isAdmin = false;
    private String adminOfSocietyId = null; // first society this user is admin of

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

                    // Role — super admin
                    String role = doc.getString("role");
                    isAdmin = "admin".equalsIgnoreCase(role);
                    showAdminSection(view, isAdmin);

                    // Society admin — adminOf array
                    List<?> adminOf = (List<?>) doc.get("adminOf");
                    if (!isAdmin && adminOf != null && !adminOf.isEmpty()) {
                        adminOfSocietyId = (String) adminOf.get(0);
                        showSocietyAdminSection(view, true);
                        wireSocietyAdminTiles(view, adminOfSocietyId);
                    } else {
                        showSocietyAdminSection(view, false);
                    }

                    // Society IDs
                    userSocietyIds.clear();
                    List<?> ids = (List<?>) doc.get("societyIds");
                    if (ids != null) {
                        for (Object id : ids) {
                            if (id instanceof String) userSocietyIds.add((String) id);
                        }
                    }

                    loadAnnouncements(view);
                });
    }

    private void showAdminSection(@NonNull View view, boolean visible) {
        int v = visible ? View.VISIBLE : View.GONE;
        view.findViewById(R.id.tvAdminItems).setVisibility(v);
        view.findViewById(R.id.cardAdminDashboard).setVisibility(v);
    }

    private void showSocietyAdminSection(@NonNull View view, boolean visible) {
        int v = visible ? View.VISIBLE : View.GONE;
        view.findViewById(R.id.tvSocietyAdminItems).setVisibility(v);
        view.findViewById(R.id.cardSocietyAdminDashboard).setVisibility(v);
    }

    private void wireSocietyAdminTiles(@NonNull View view, @NonNull String societyId) {
        // Post Pin — pre-fill societyId
        View tilePin = view.findViewById(R.id.tileSAPostPin);
        if (tilePin != null) tilePin.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("preSelectedSocietyId", societyId);
            NavHostFragment.findNavController(this).navigate(R.id.createPinFragment, args);
        });

        // Create Event
        View tileEvent = view.findViewById(R.id.tileSACreateEvent);
        if (tileEvent != null) tileEvent.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("preSelectedSocietyId", societyId);
            NavHostFragment.findNavController(this).navigate(R.id.createEventFragment, args);
        });

        // Create Poll
        View tilePoll = view.findViewById(R.id.tileSACreatePoll);
        if (tilePoll != null) tilePoll.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("preSelectedSocietyId", societyId);
            NavHostFragment.findNavController(this).navigate(R.id.createPollFragment, args);
        });

        // Manage Members — pass societyFilter so UserManagementFragment filters to this society
        View tileMembers = view.findViewById(R.id.tileSAManageMembers);
        if (tileMembers != null) tileMembers.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("societyFilter", societyId);
            NavHostFragment.findNavController(this).navigate(R.id.userManagementFragment, args);
        });
    }

    // -------------------------------------------------------------------------
    // Announcements (Pins)
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
                        if (!isAdmin && societyId != null && !societyId.isEmpty()
                                && !userSocietyIds.contains(societyId)) continue;

                        Announcement a = new Announcement();
                        a.setId(doc.getId());
                        a.setTitle(safeString(doc.getString("content"), ""));
                        a.setContent("");
                        a.setSocietyId(societyId != null ? societyId : "");
                        a.setCreatedBy(safeString(doc.getString("createdBy"), ""));
                        a.setCreatedAt(doc.getTimestamp("createdAt"));
                        announcements.add(a);
                    }

                    fetchAnnouncementSocietyNames(view);
                });
    }

    private void fetchAnnouncementSocietyNames(@NonNull View view) {
        Set<String> ids = new HashSet<>();
        for (Announcement a : announcements) {
            if (!a.getSocietyId().isEmpty()) ids.add(a.getSocietyId());
        }

        if (ids.isEmpty()) { publishAnnouncements(view); return; }

        final int[] remaining = {ids.size()};
        final Map<String, String> nameMap = new HashMap<>();

        for (String sid : ids) {
            FirebaseFirestore.getInstance().collection("societies").document(sid).get()
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
        if (isAdmin) {
            announcementsAdapter.setDeleteListener(pinId ->
                    FirebaseFirestore.getInstance()
                            .collection("pins").document(pinId).delete()
                            .addOnSuccessListener(unused -> {
                                if (!isAdded()) return;
                                announcements.removeIf(a -> a.getId().equals(pinId));
                                announcementsAdapter.updateList(announcements);
                                int vis = announcements.isEmpty() ? View.GONE : View.VISIBLE;
                                view.findViewById(R.id.tvAnnouncementsLabel).setVisibility(vis);
                                view.findViewById(R.id.rvAnnouncements).setVisibility(vis);
                            }));
        }

        announcementsAdapter.updateList(announcements);
        int visibility = announcements.isEmpty() ? View.GONE : View.VISIBLE;
        view.findViewById(R.id.tvAnnouncementsLabel).setVisibility(visibility);
        view.findViewById(R.id.rvAnnouncements).setVisibility(visibility);
    }

    // -------------------------------------------------------------------------
    // Tile navigation
    // -------------------------------------------------------------------------

    private void wireTiles(@NonNull View view) {
        wire(view, R.id.tileEvents,         R.id.eventsFragment);
        wire(view, R.id.tileChats,          R.id.chatsFragment);
        wire(view, R.id.tileQr,             R.id.qrFragment);
        wire(view, R.id.tileCalendar,       R.id.calendarFragment);
        wire(view, R.id.tilePolls,          R.id.pollsFragment);
        wire(view, R.id.tileGallery,        R.id.galleryFragment);
        wire(view, R.id.tileUserManagement, R.id.userManagementFragment);
        wire(view, R.id.tileCreatePoll,     R.id.createPollFragment);
        wire(view, R.id.tileCreateEvent,    R.id.createEventFragment);
        wire(view, R.id.tileCreatePin,      R.id.createPinFragment);
        wire(view, R.id.tileEditSociety,    R.id.editSocietyFragment);
    }

    private void wire(@NonNull View root, int tileId, int destId) {
        View tile = root.findViewById(tileId);
        if (tile != null) tile.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(destId));
    }

    @NonNull
    private String safeString(@Nullable String value, @NonNull String fallback) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : fallback;
    }
}
