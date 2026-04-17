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

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EventsFragment extends Fragment {

    private static final String ATTENDANCE_COLLECTION = "userAttendance";
    private static final String ATTENDING_SUB = "attendingEvents";

    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> filteredEvents = new ArrayList<>();
    private EventsAdapter adapter;
    private View rootView;
    private android.widget.ProgressBar progressEvents;


    private final Set<String> userSocietyIds = new HashSet<>();
    private boolean isAdmin = false;

    public EventsFragment() {
        super(R.layout.fragment_events);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        progressEvents = view.findViewById(R.id.progressEvents);

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

        view.findViewById(R.id.btnCalendar).setOnClickListener(v ->
                NavHelpers.navigate(this, R.id.calendarFragment));

        loadEventsFromFirestore();
    }





    private void loadEventsFromFirestore() {
        if (progressEvents != null) progressEvents.setVisibility(View.VISIBLE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference eventsCollection = db.collection("events");
        eventsCollection.get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    if (progressEvents != null) progressEvents.setVisibility(View.GONE);

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
                    if (progressEvents != null) progressEvents.setVisibility(View.GONE);
                    if (allEvents.isEmpty()) seedDummyEvents();
                    Toast.makeText(requireContext(),
                            "Could not load events: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    loadAttendanceAndMerge();
                });
    }





    private void loadAttendanceAndMerge() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            loadUserSocietiesAndFilter();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference attendanceCollection = db.collection(ATTENDANCE_COLLECTION);
        DocumentReference userAttendanceDocument = attendanceCollection.document(user.getUid());
        CollectionReference attendingEventsCollection = userAttendanceDocument.collection(ATTENDING_SUB);
        attendingEventsCollection.get()
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






    private void loadUserSocietiesAndFilter() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            applyFilters();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usersCollection = db.collection("users");
        DocumentReference userDocument = usersCollection.document(user.getUid());
        userDocument.get()
                .addOnSuccessListener((DocumentSnapshot doc) -> {
                    if (!isAdded()) return;
                    isAdmin = "admin".equalsIgnoreCase(doc.getString("role"));
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





    private void toggleAttendance(@NonNull Event event, boolean attending) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(),
                    "Please log in to attend events.", Toast.LENGTH_SHORT).show();
            event.setAttending(!attending);
            adapter.notifyDataSetChanged();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference attendanceCollection = db.collection(ATTENDANCE_COLLECTION);
        DocumentReference userAttendanceDocument = attendanceCollection.document(user.getUid());
        CollectionReference attendingEventsCollection = userAttendanceDocument.collection(ATTENDING_SUB);
        DocumentReference ref = attendingEventsCollection.document(event.getId());

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







    private void seedDummyEvents() {
        allEvents.add(new Event(
                "e1",
                "Nottingham Car Show",
                "19-Nov-2025 • 17:00",
                "Royal Concert Hall",
                "Motorsport Society",
                "A showcase of classic and modern cars around Nottingham city centre.",
                "motorsport-society-id",
                false,
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
                true,
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
                false,
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
                true,
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
                true,
                false, false
        ));
    }





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



        View.OnClickListener filterListener = v -> applyFilters();
        Chip chipAll      = view.findViewById(R.id.chipAll);
        Chip chipThisWeek = view.findViewById(R.id.chipThisWeek);
        Chip chipNextWeek = view.findViewById(R.id.chipNextWeek);
        if (chipAll      != null) chipAll.setOnClickListener(filterListener);
        if (chipThisWeek != null) chipThisWeek.setOnClickListener(filterListener);
        if (chipNextWeek != null) chipNextWeek.setOnClickListener(filterListener);
    }

    private void applyFilters() {
        if (rootView == null) return;

        String query = "";
        View et = rootView.findViewById(R.id.etSearchEvents);
        if (et instanceof android.widget.EditText) {
            query = ((android.widget.EditText) et).getText().toString().trim().toLowerCase(Locale.UK);
        }


        Chip chipThisWeekView = rootView.findViewById(R.id.chipThisWeek);
        Chip chipNextWeekView = rootView.findViewById(R.id.chipNextWeek);
        boolean filterThisWeek = chipThisWeekView != null && chipThisWeekView.isChecked();
        boolean filterNextWeek = chipNextWeekView != null && chipNextWeekView.isChecked();

        filteredEvents.clear();


        Calendar thisWeekStart = getWeekStart(0);
        Calendar thisWeekEnd   = getWeekStart(1);
        Calendar nextWeekEnd   = getWeekStart(2);

        for (Event e : allEvents) {


            if (!isAdmin && !e.isPublic()
                    && !userSocietyIds.contains(e.getSocietyId())
                    && !e.isAttending()) continue;


            if (!query.isEmpty() && !e.getName().toLowerCase(Locale.UK).contains(query)) continue;


            if (filterThisWeek || filterNextWeek) {
                Date eventDate = parseEventDate(e.getDateTime());
                if (eventDate == null) continue;

                if (filterThisWeek) {
                    if (eventDate.before(thisWeekStart.getTime())
                            || !eventDate.before(thisWeekEnd.getTime())) continue;
                } else {
                    if (eventDate.before(thisWeekEnd.getTime())
                            || !eventDate.before(nextWeekEnd.getTime())) continue;
                }
            }

            filteredEvents.add(e);
        }

        adapter.updateList(filteredEvents);
    }






    @NonNull
    private static Calendar getWeekStart(int weeksFromNow) {
        Calendar cal = Calendar.getInstance(Locale.UK);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.WEEK_OF_YEAR, weeksFromNow);
        return cal;
    }


    @Nullable
    private static Date parseEventDate(@Nullable String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) return null;
        try {
            String datePart = dateTime.contains(" • ")
                    ? dateTime.split(" • ")[0].trim()
                    : dateTime.trim();
            return new SimpleDateFormat("dd-MMM-yyyy", Locale.UK).parse(datePart);
        } catch (ParseException e) {
            return null;
        }
    }

    @NonNull
    private String safeString(@Nullable String value, @NonNull String fallback) {
        return (value != null && !(value == null || value.trim().isEmpty())) ? value.trim() : fallback;
    }
}
