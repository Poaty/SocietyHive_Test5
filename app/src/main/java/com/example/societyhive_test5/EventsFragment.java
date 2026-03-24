package com.example.societyhive_test5;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Events screen.
 *
 * Loads events from Firestore: events/{eventId}
 * Filters by visibility:
 *   - isPublic == true  → visible to everyone
 *   - isPublic == false → visible only if user is in the event's society
 *
 * Attendance persisted to: userAttendance/{userId}/attendingEvents/{eventId}
 *
 * Loading sequence:
 *   1. loadEventsFromFirestore()    — fetch all event documents
 *   2. loadAttendanceAndMerge()     — mark which events user is attending
 *   3. loadUserSocietiesAndFilter() — fetch user's societyIds, filter list, render
 */
public class EventsFragment extends Fragment {

    private static final String ATTENDANCE_COLLECTION = "userAttendance";
    private static final String ATTENDING_SUB = "attendingEvents";

    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> filteredEvents = new ArrayList<>();
    private EventsAdapter adapter;
    private View rootView;

    // The societies this user belongs to — used for visibility filtering
    private final Set<String> userSocietyIds = new HashSet<>();

    public EventsFragment() {
        super(R.layout.fragment_events);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;

        RecyclerView rv = view.findViewById(R.id.rvEvents);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setHasFixedSize(true);

        adapter = new EventsAdapter(
                new ArrayList<>(),
                this::toggleAttendance,
                event -> {
                    android.os.Bundle b = new android.os.Bundle();
                    b.putString("eventId", event.getId());
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.eventDetailsFragment, b);
                }
        );

        rv.setAdapter(adapter);
        hookSearch(view);
        hookChips(view);

        loadEventsFromFirestore();
    }

    // -------------------------------------------------------------------------
    // Step 1 — Load events
    // -------------------------------------------------------------------------

    private void loadEventsFromFirestore() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;

                    allEvents.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        allEvents.add(new Event(
                                doc.getId(),
                                safeString(doc.getString("name"), "Unnamed Event"),
                                safeString(doc.getString("dateTime"), "TBC"),
                                safeString(doc.getString("location"), "TBC"),
                                safeString(doc.getString("organiser"), "Unknown"),
                                safeString(doc.getString("description"), ""),
                                safeString(doc.getString("societyId"), ""),
                                Boolean.TRUE.equals(doc.getBoolean("isPublic")),
                                false,
                                false
                        ));
                    }

                    if (allEvents.isEmpty()) seedDummyEvents();

                    loadAttendanceAndMerge();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    if (allEvents.isEmpty()) seedDummyEvents();
                    Toast.makeText(requireContext(),
                            "Could not load events: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    loadAttendanceAndMerge();
                });
    }

    // -------------------------------------------------------------------------
    // Step 2 — Merge attendance state
    // -------------------------------------------------------------------------

    private void loadAttendanceAndMerge() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            loadUserSocietiesAndFilter();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(ATTENDANCE_COLLECTION)
                .document(user.getUid())
                .collection(ATTENDING_SUB)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    Set<String> attendingIds = new HashSet<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        attendingIds.add(doc.getId());
                    }
                    for (Event event : allEvents) {
                        event.setAttending(attendingIds.contains(event.getId()));
                    }
                    loadUserSocietiesAndFilter();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    loadUserSocietiesAndFilter();
                });
    }

    // -------------------------------------------------------------------------
    // Step 3 — Load user's societies, then filter and render
    // -------------------------------------------------------------------------

    /**
     * Fetches the current user's societyIds from users/{uid}, then calls
     * applyFilters() so the list only shows events the user is allowed to see.
     *
     * Visibility rule:
     *   show event if event.isPublic == true
     *               OR event.societyId is in the user's societyIds
     */
    private void loadUserSocietiesAndFilter() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            applyFilters();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener((DocumentSnapshot doc) -> {
                    if (!isAdded()) return;
                    userSocietyIds.clear();
                    List<?> ids = (List<?>) doc.get("societyIds");
                    if (ids != null) {
                        for (Object id : ids) {
                            if (id instanceof String) userSocietyIds.add((String) id);
                        }
                    }
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    applyFilters();
                });
    }

    // -------------------------------------------------------------------------
    // Attendance toggle — writes/deletes in Firestore
    // -------------------------------------------------------------------------

    private void toggleAttendance(@NonNull Event event, boolean attending) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(),
                    "Please log in to attend events.", Toast.LENGTH_SHORT).show();
            event.setAttending(!attending);
            adapter.notifyDataSetChanged();
            return;
        }

        DocumentReference ref = FirebaseFirestore.getInstance()
                .collection(ATTENDANCE_COLLECTION)
                .document(user.getUid())
                .collection(ATTENDING_SUB)
                .document(event.getId());

        if (attending) {
            Map<String, Object> data = new HashMap<>();
            data.put("eventId", event.getId());
            data.put("eventName", event.getName());
            data.put("attendedAt", Timestamp.now());
            ref.set(data).addOnFailureListener(e -> {
                if (!isAdded()) return;
                event.setAttending(false);
                adapter.notifyDataSetChanged();
                Toast.makeText(requireContext(),
                        "Failed to save attendance.", Toast.LENGTH_SHORT).show();
            });
        } else {
            ref.delete().addOnFailureListener(e -> {
                if (!isAdded()) return;
                event.setAttending(true);
                adapter.notifyDataSetChanged();
                Toast.makeText(requireContext(),
                        "Failed to remove attendance.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    // -------------------------------------------------------------------------
    // Dummy data — used until Firestore events collection is populated.
    // societyId values match whatever document IDs you have in your
    // Firestore societies collection. Update them to match yours.
    // -------------------------------------------------------------------------

    private void seedDummyEvents() {
        allEvents.add(new Event(
                "e1",
                "Nottingham Car Show",
                "19-Nov-2025 • 17:00",
                "Royal Concert Hall",
                "Motorsport Society",
                "A showcase of classic and modern cars around Nottingham city centre.",
                "motorsport-society-id",
                false, // members only
                false, false
        ));
        allEvents.add(new Event(
                "e2",
                "Society Mixer",
                "20-Nov-2025 • 18:00",
                "Student Union",
                "Business Society",
                "Meet new members, socialise, and hear about upcoming society activities.",
                "business-society-id",
                true, // public — anyone can see this
                false, false
        ));
        allEvents.add(new Event(
                "e3",
                "Hack Night",
                "22-Nov-2025 • 18:30",
                "Makerspace",
                "Computing Society",
                "Bring your laptop and work on projects in a relaxed, collaborative session.",
                "computing-society-id",
                false, // members only
                false, false
        ));
        allEvents.add(new Event(
                "e4",
                "Career Talk: Grad Roles",
                "25-Nov-2025 • 16:00",
                "Newton LT",
                "Careers Hub",
                "A speaker session covering graduate roles, interview expectations, and application tips.",
                "careers-hub-id",
                true, // public — open taster
                false, false
        ));
        allEvents.add(new Event(
                "e5",
                "Freshers Meetup",
                "Next Week • 12:00",
                "Atrium",
                "Student Union",
                "A welcome event for new students to connect with societies and student reps.",
                "student-union-id",
                true, // public
                false, false
        ));
    }

    // -------------------------------------------------------------------------
    // Search, chip filters, and rendering
    // -------------------------------------------------------------------------

    private void hookSearch(@NonNull View view) {
        View et = view.findViewById(R.id.etSearchEvents);
        if (!(et instanceof android.widget.EditText)) return;
        ((android.widget.EditText) et).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void hookChips(@NonNull View view) {
        ChipGroup group = view.findViewById(R.id.chipGroupFilters);
        if (group == null) return;
        group.setOnCheckedChangeListener((chipGroup, checkedId) -> applyFilters());
    }

    private void applyFilters() {
        if (rootView == null) return;

        String query = "";
        View et = rootView.findViewById(R.id.etSearchEvents);
        if (et instanceof android.widget.EditText) {
            query = ((android.widget.EditText) et).getText().toString().trim().toLowerCase(Locale.UK);
        }

        int checkedId = View.NO_ID;
        ChipGroup group = rootView.findViewById(R.id.chipGroupFilters);
        if (group != null) checkedId = group.getCheckedChipId();

        filteredEvents.clear();

        for (Event e : allEvents) {
            // Visibility: show if user is in the society OR is already attending (e.g. joined via QR)
            if (!userSocietyIds.contains(e.getSocietyId()) && !e.isAttending()) continue;

            // Search filter
            if (!query.isEmpty() && !e.getName().toLowerCase(Locale.UK).contains(query)) continue;

            // Chip filter
            if (checkedId == R.id.chipThisWeek) {
                if (e.getDateTime().toLowerCase(Locale.UK).contains("next week")) continue;
            } else if (checkedId == R.id.chipNextWeek) {
                if (!e.getDateTime().toLowerCase(Locale.UK).contains("next week")) continue;
            }

            filteredEvents.add(e);
        }

        adapter.updateList(filteredEvents);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @NonNull
    private String safeString(@Nullable String value, @NonNull String fallback) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : fallback;
    }
}
