package com.example.societyhive_test5;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shows a full-month calendar and lists the user's visible events for the selected date.
 *
 * Uses the same three-step loading sequence as EventsFragment:
 *   1. Load all events from Firestore
 *   2. Merge attendance state
 *   3. Load user's societyIds, filter for visibility, then render for selected date
 */
public class CalendarFragment extends Fragment {

    private static final String ATTENDANCE_COLLECTION = "userAttendance";
    private static final String ATTENDING_SUB = "attendingEvents";
    private static final SimpleDateFormat SDF_PARSE = new SimpleDateFormat("dd-MMM-yyyy", Locale.UK);
    private static final SimpleDateFormat SDF_HEADER = new SimpleDateFormat("MMMM d", Locale.UK);

    private final List<Event> allVisibleEvents = new ArrayList<>();
    private CalendarEventsAdapter adapter;
    private TextView tvEventsOnDate;
    private TextView tvNoEvents;
    private final Set<String> userSocietyIds = new HashSet<>();

    // Tracks the currently selected date so events can be re-filtered after load
    private int selectedYear;
    private int selectedMonth;
    private int selectedDay;

    public CalendarFragment() {
        super(R.layout.fragment_calendar);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvEventsOnDate = view.findViewById(R.id.tvEventsOnDate);
        tvNoEvents = view.findViewById(R.id.tvNoEvents);

        RecyclerView rv = view.findViewById(R.id.rvCalendarEvents);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CalendarEventsAdapter();
        rv.setAdapter(adapter);

        // Default to today
        Calendar today = Calendar.getInstance();
        selectedYear = today.get(Calendar.YEAR);
        selectedMonth = today.get(Calendar.MONTH);
        selectedDay = today.get(Calendar.DAY_OF_MONTH);

        updateHeader(selectedYear, selectedMonth, selectedDay);

        CalendarView calendarView = view.findViewById(R.id.calendarView);
        calendarView.setOnDateChangeListener((calView, year, month, dayOfMonth) -> {
            selectedYear = year;
            selectedMonth = month;
            selectedDay = dayOfMonth;
            showEventsForSelectedDate();
        });

        loadEventsFromFirestore();
    }

    // -------------------------------------------------------------------------
    // Step 1 — Load all events
    // -------------------------------------------------------------------------

    private void loadEventsFromFirestore() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    allVisibleEvents.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        allVisibleEvents.add(new Event(
                                doc.getId(),
                                safeString(doc.getString("name"), "Unnamed Event"),
                                safeString(doc.getString("dateTime"), ""),
                                safeString(doc.getString("location"), "TBC"),
                                safeString(doc.getString("organiser"), "Unknown"),
                                safeString(doc.getString("description"), ""),
                                safeString(doc.getString("societyId"), ""),
                                Boolean.TRUE.equals(doc.getBoolean("isPublic")),
                                false, false
                        ));
                    }
                    loadAttendanceAndMerge();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
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
                    for (QueryDocumentSnapshot doc : querySnapshot) attendingIds.add(doc.getId());
                    for (Event event : allVisibleEvents) {
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
    // Step 3 — Filter by visibility, then show for selected date
    // -------------------------------------------------------------------------

    private void loadUserSocietiesAndFilter() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            applyVisibilityFilter();
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
                    applyVisibilityFilter();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    applyVisibilityFilter();
                });
    }

    private void applyVisibilityFilter() {
        List<Event> visible = new ArrayList<>();
        for (Event e : allVisibleEvents) {
            if (userSocietyIds.contains(e.getSocietyId()) || e.isAttending()) {
                visible.add(e);
            }
        }
        allVisibleEvents.clear();
        allVisibleEvents.addAll(visible);
        showEventsForSelectedDate();
    }

    // -------------------------------------------------------------------------
    // Date filtering and display
    // -------------------------------------------------------------------------

    private void showEventsForSelectedDate() {
        List<Event> dayEvents = new ArrayList<>();
        for (Event e : allVisibleEvents) {
            Date eventDate = parseEventDate(e.getDateTime());
            if (eventDate == null) continue;

            Calendar ec = Calendar.getInstance();
            ec.setTime(eventDate);

            if (ec.get(Calendar.YEAR) == selectedYear
                    && ec.get(Calendar.MONTH) == selectedMonth
                    && ec.get(Calendar.DAY_OF_MONTH) == selectedDay) {
                dayEvents.add(e);
            }
        }

        updateHeader(selectedYear, selectedMonth, selectedDay);
        adapter.updateList(dayEvents);
        tvNoEvents.setVisibility(dayEvents.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateHeader(int year, int month, int day) {
        if (tvEventsOnDate == null) return;
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        tvEventsOnDate.setText("Events on " + SDF_HEADER.format(cal.getTime()));
    }

    @Nullable
    private static Date parseEventDate(String dateTime) {
        if (dateTime == null || !dateTime.contains(" • ")) return null;
        try {
            return SDF_PARSE.parse(dateTime.split(" • ")[0].trim());
        } catch (ParseException e) {
            return null;
        }
    }

    @NonNull
    private static String safeString(@Nullable String value, @NonNull String fallback) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : fallback;
    }
}
