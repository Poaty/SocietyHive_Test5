package com.example.societyhive_test5;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Events screen with hybrid expandable cards.
 * Later swap dummy data + local filters with Firestore queries.
 */
public class EventsFragment extends Fragment {

    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> filteredEvents = new ArrayList<>();
    private EventsAdapter adapter;

    public EventsFragment() {
        super(R.layout.fragment_events);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rvEvents);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setHasFixedSize(true);

        adapter = new EventsAdapter(
                new ArrayList<>(),
                (event, attending) -> {
                    // TODO later: write attendance state to Firestore
                },
                event -> {
                    android.os.Bundle b = new android.os.Bundle();
                    b.putString("eventId", event.getId());
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.eventDetailsFragment, b);
                }
        );

        rv.setAdapter(adapter);

        seedDummyEvents();
        applyFilters(view);
        hookSearch(view);
        hookChips(view);
    }

    private void seedDummyEvents() {
        if (!allEvents.isEmpty()) return;

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

    private void hookSearch(@NonNull View view) {
        View et = view.findViewById(R.id.etSearchEvents);
        if (!(et instanceof android.widget.EditText)) return;

        android.widget.EditText etSearch = (android.widget.EditText) et;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters(view);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void hookChips(@NonNull View view) {
        ChipGroup group = view.findViewById(R.id.chipGroupFilters);
        if (group == null) return;
        group.setOnCheckedChangeListener((chipGroup, checkedId) -> applyFilters(view));
    }

    private void applyFilters(@NonNull View view) {
        String query = "";
        View et = view.findViewById(R.id.etSearchEvents);
        if (et instanceof android.widget.EditText) {
            query = ((android.widget.EditText) et).getText().toString().trim().toLowerCase(Locale.UK);
        }

        int checkedId = View.NO_ID;
        ChipGroup group = view.findViewById(R.id.chipGroupFilters);
        if (group != null) checkedId = group.getCheckedChipId();

        filteredEvents.clear();

        for (Event e : allEvents) {
            if (!query.isEmpty() && !e.getName().toLowerCase(Locale.UK).contains(query)) {
                continue;
            }

            if (checkedId == R.id.chipThisWeek) {
                if (e.getDateTime().toLowerCase(Locale.UK).contains("next week")) {
                    continue;
                }
            } else if (checkedId == R.id.chipNextWeek) {
                if (!e.getDateTime().toLowerCase(Locale.UK).contains("next week")) {
                    continue;
                }
            }

            filteredEvents.add(e);
        }

        adapter.updateList(filteredEvents);
    }
}
