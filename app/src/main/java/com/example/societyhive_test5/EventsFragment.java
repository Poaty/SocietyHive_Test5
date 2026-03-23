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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
 * Falls back to hard-coded dummy data if Firestore returns nothing,
 * so the screen is never blank during development.
 *
 * Attend button: toggles local UI state now; Firestore write is
 * a clearly marked TODO so you can add it when ready.
 */
public class EventsFragment extends Fragment {

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
                (event, attending) -> {
                    // TODO: write attendance state to Firestore
                    // Example:
                    // FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    // if (user == null) return;
                    // DocumentReference ref = FirebaseFirestore.getInstance()
                    //     .collection("events").document(event.getId())
                    //     .collection("attendees").document(user.getUid());
                    // if (attending) ref.set(...); else ref.delete();
                },
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

        // Load from Firestore; fall back to dummy data if collection is empty
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
                        Event event = new Event(
                                doc.getId(),
                                safeString(doc.getString("name"), "Unnamed Event"),
                                safeString(doc.getString("dateTime"), "TBC"),
                                safeString(doc.getString("location"), "TBC"),
                                safeString(doc.getString("organiser"), "Unknown"),
                                safeString(doc.getString("description"), ""),
                                false,
                                false
                        );
                        allEvents.add(event);
                    }

                    if (allEvents.isEmpty()) {
                        // Firestore collection is empty — use dummy data for now
                        seedDummyEvents();
                    }

                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;

                    // Could not reach Firestore — fall back to dummy data silently
                    if (allEvents.isEmpty()) {
                        seedDummyEvents();
                        applyFilters();
                    }

                    Toast.makeText(requireContext(),
                            "Could not load events: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
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
                true,
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
