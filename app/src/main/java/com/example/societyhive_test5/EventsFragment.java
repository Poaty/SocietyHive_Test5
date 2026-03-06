package com.example.societyhive_test5;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * UI-first Events screen.
 * Later: replace dummy data with Firestore query + submitList(results).
 */
public class EventsFragment extends Fragment {

    private final List<Event> allEvents = new ArrayList<>();
    private EventsAdapter adapter;

    public EventsFragment() {
        super(R.layout.fragment_events);
    }
    @Override
    public void onCreate(@androidx.annotation.Nullable android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new com.google.android.material.transition.MaterialFadeThrough());
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rvEvents);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setHasFixedSize(true);
        rv.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());
        rv.addItemDecoration(new androidx.recyclerview.widget.DividerItemDecoration(
                requireContext(),
                androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
        ));

        adapter = new EventsAdapter((event, going) -> {
            // TODO (later): write attendance to Firestore:
            // events/{eventId}/attendees/{uid}
        });

        rv.setAdapter(adapter);

        seedDummyEvents();
        applyFilters(view);
        hookSearch(view);
        hookChips(view);
    }

    private void seedDummyEvents() {
        if (!allEvents.isEmpty()) return;

        allEvents.add(new Event("e1", "Society Mixer", "18:00 • Wed • Student Union", false));
        allEvents.add(new Event("e2", "Karaoke Night", "19:30 • Fri • The Loft", true));
        allEvents.add(new Event("e3", "Career Talk: Grad Roles", "16:00 • Tue • Newton LT", false));
        allEvents.add(new Event("e4", "Board Games Social", "17:00 • Thu • Library Lounge", false));
        allEvents.add(new Event("e5", "Five-a-side Football", "20:00 • Mon • Sports Centre", true));
        allEvents.add(new Event("e6", "Hack Night", "18:30 • Sat • Makerspace", false));
        allEvents.add(new Event("e7", "Study Group", "14:00 • Sun • City Campus", false));
        allEvents.add(new Event("e8", "Freshers Meetup", "12:00 • Next Week • Atrium", false));
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

        List<Event> filtered = new ArrayList<>();
        for (Event e : allEvents) {
            if (!query.isEmpty()) {
                String n = e.getName().toLowerCase(Locale.UK);
                if (!n.contains(query)) continue;
            }

            // Placeholder filter behavior until you have real dates:
            if (checkedId == R.id.chipThisWeek) {
                if (e.getInfo().toLowerCase(Locale.UK).contains("next week")) continue;
            } else if (checkedId == R.id.chipNextWeek) {
                if (!e.getInfo().toLowerCase(Locale.UK).contains("next week")) continue;
            }

            filtered.add(e);
        }

        adapter.submitList(filtered);
    }
}
