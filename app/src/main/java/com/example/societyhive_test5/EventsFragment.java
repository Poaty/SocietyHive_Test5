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
 * Loads events from Firestore collection: events/{eventId}
 * Expected fields per document:
 *   name        (String)
 *   dateTime    (String)
 *   location    (String)
 *   organiser   (String)
 *   description (String)
 *
 * Attendance is persisted to Firestore under:
 *   userAttendance/{userId}/attendingEvents/{eventId}
 *     eventId    (String)
 *     eventName  (String)
 *     attendedAt (Timestamp)
 *
 * Falls back to hard-coded dummy data if Firestore returns nothing,
 * so the screen is never blank during development.
 */
public class EventsFragment extends Fragment {

    private static final String ATTENDANCE_COLLECTION = "userAttendance";
    private static final String ATTENDING_SUB = "attendingEvents";

    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> filteredEvents = new ArrayList<>();
    private EventsAdapter adapter;

    // Keeps a reference to the root view for filter reapplication
    private View rootView;

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

        // Step 1: load events, then Step 2: merge attendance state
        loadEventsFromFirestore();
    }

    // -------------------------------------------------------------------------
    // Firestore loading
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
                                false,
                                false
                        ));
                    }

                    if (allEvents.isEmpty()) {
                        seedDummyEvents();
                    }

                    // Step 2: load which events this user is attending, then render
                    loadAttendanceAndMerge();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;

                    if (allEvents.isEmpty()) {
                        seedDummyEvents();
                    }

                    Toast.makeText(requireContext(),
                            "Could not load events: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    // Still attempt to load attendance even if events partially failed
                    loadAttendanceAndMerge();
                });
    }

    /**
     * Fetches the set of event IDs the current user is attending from Firestore
     * (userAttendance/{userId}/attendingEvents), marks those events in allEvents,
     * then calls applyFilters() to render the final list.
     */
    private void loadAttendanceAndMerge() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Not logged in — show events without any attendance state
            applyFilters();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(ATTENDANCE_COLLECTION)
                .document(user.getUid())
                .collection(ATTENDING_SUB)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;

                    // Collect all event IDs the user is attending into a Set
                    Set<String> attendingIds = new HashSet<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        attendingIds.add(doc.getId());
                    }

                    // Mark each event with the user's actual attendance state
                    for (Event event : allEvents) {
                        event.setAttending(attendingIds.contains(event.getId()));
                    }

                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    // Couldn't load attendance — render events without marking any
                    applyFilters();
                });
    }

    // -------------------------------------------------------------------------
    // Attendance toggle — writes/deletes in Firestore
    // -------------------------------------------------------------------------

    /**
     * Called when the user taps "Attend Event" or "Attending".
     *
     * Firestore path: userAttendance/{userId}/attendingEvents/{eventId}
     *   - Attending:     set document with eventId, eventName, attendedAt
     *   - Not attending: delete document
     *
     * The adapter has already updated local state optimistically. If the Firestore
     * write fails we revert the local state and show a toast.
     */
    private void toggleAttendance(@NonNull Event event, boolean attending) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(),
                    "Please log in to attend events.", Toast.LENGTH_SHORT).show();
            // Revert the optimistic UI update
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
    // Dummy data (used until Firestore events collection is populated)
    // -------------------------------------------------------------------------

    private void seedDummyEvents() {
        allEvents.add(new Event(
                "e1",
                "Nottingham Car Show",
                "19-Nov-2025 • 17:00",
                "Royal Concert Hall",
                "Motorsport Society",
                "This event doesn't actually exist, but it shows how the expanded card can preview more information before opening a full details page.",
                false,
                false
        ));
        allEvents.add(new Event(
                "e2",
                "Society Mixer",
                "20-Nov-2025 • 18:00",
                "Student Union",
                "Business Society",
                "Meet new members, socialise, and hear about upcoming society activities.",
                false,
                false
        ));
        allEvents.add(new Event(
                "e3",
                "Hack Night",
                "22-Nov-2025 • 18:30",
                "Makerspace",
                "Computing Society",
                "Bring your laptop and work on projects in a relaxed, collaborative session.",
                false,
                false
        ));
        allEvents.add(new Event(
                "e4",
                "Career Talk: Grad Roles",
                "25-Nov-2025 • 16:00",
                "Newton LT",
                "Careers Hub",
                "A speaker session covering graduate roles, interview expectations, and application tips.",
                false,
                false
        ));
        allEvents.add(new Event(
                "e5",
                "Freshers Meetup",
                "Next Week • 12:00",
                "Atrium",
                "Student Union",
                "A welcome event for new students to connect with societies and student reps.",
                false,
                false
        ));
    }

    // -------------------------------------------------------------------------
    // Search and chip filters
    // -------------------------------------------------------------------------

    private void hookSearch(@NonNull View view) {
        View et = view.findViewById(R.id.etSearchEvents);
        if (!(et instanceof android.widget.EditText)) return;

        android.widget.EditText etSearch = (android.widget.EditText) et;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
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
            if (!query.isEmpty() && !e.getName().toLowerCase(Locale.UK).contains(query)) {
                continue;
            }

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
