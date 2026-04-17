package com.example.societyhive_test5;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarFragment extends Fragment {

    private static final String ATTENDANCE_COLLECTION = "userAttendance";
    private static final String ATTENDING_SUB = "attendingEvents";
    private static final SimpleDateFormat SDF_PARSE  = new SimpleDateFormat("dd-MMM-yyyy", Locale.UK);
    private static final SimpleDateFormat SDF_HEADER = new SimpleDateFormat("MMMM d", Locale.UK);
    private static final SimpleDateFormat SDF_MONTH  = new SimpleDateFormat("MMMM", Locale.UK);


    private static final int COLOR_PRIMARY      = 0xFF8D2E3A;
    private static final int COLOR_PRIMARY_LIGHT = 0xFFF3D7DC;
    private static final int COLOR_TEXT_DARK    = 0xFF1E1E1E;
    private static final int COLOR_TEXT_GREY    = 0xFF9E9E9E;

    private final List<Event> allVisibleEvents = new ArrayList<>();
    private CalendarEventsAdapter adapter;
    private TextView tvEventsOnDate;
    private TextView tvNoEvents;
    private final Set<String> userSocietyIds = new HashSet<>();


    private int currentYear;
    private int currentMonth;


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


        Calendar today = Calendar.getInstance();
        currentYear  = today.get(Calendar.YEAR);
        currentMonth = today.get(Calendar.MONTH);
        selectedYear  = currentYear;
        selectedMonth = currentMonth;
        selectedDay   = today.get(Calendar.DAY_OF_MONTH);


        view.findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 0) { currentMonth = 11; currentYear--; }
            updateCalendar();
        });
        view.findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 11) { currentMonth = 0; currentYear++; }
            updateCalendar();
        });
        view.findViewById(R.id.btnPrevYear).setOnClickListener(v -> { currentYear--; updateCalendar(); });
        view.findViewById(R.id.btnNextYear).setOnClickListener(v -> { currentYear++; updateCalendar(); });


        updateMonthYearHeader();
        updateEventsHeader(selectedYear, selectedMonth, selectedDay);

        loadEventsFromFirestore();
    }





    private void updateCalendar() {
        updateMonthYearHeader();
        buildCalendarGrid(currentYear, currentMonth);
    }

    private void updateMonthYearHeader() {
        View root = getView();
        if (root == null) return;
        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);
        ((TextView) root.findViewById(R.id.tvMonth)).setText(SDF_MONTH.format(cal.getTime()));
        ((TextView) root.findViewById(R.id.tvYear)).setText(String.valueOf(currentYear));
    }

    private void buildCalendarGrid(int year, int month) {
        View root = getView();
        if (root == null) return;

        TableLayout table = root.findViewById(R.id.calendarTable);
        table.removeAllViews();

        Set<Integer> eventDays = getEventDaysForMonth(year, month);

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysInMonth    = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int offset = firstDayOfWeek - 1;
        int totalCells = offset + daysInMonth;
        int rows = (int) Math.ceil(totalCells / 7.0);

        int cellH = dpToPx(42);

        for (int r = 0; r < rows; r++) {
            TableRow row = new TableRow(requireContext());
            row.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));

            for (int c = 0; c < 7; c++) {
                int cellIndex = r * 7 + c;
                int day = cellIndex - offset + 1;
                boolean valid      = day >= 1 && day <= daysInMonth;
                boolean hasEvent   = valid && eventDays.contains(day);
                boolean isSelected = valid && day == selectedDay
                        && year == selectedYear && month == selectedMonth;

                TextView cell = makeDayCell(valid ? day : -1, hasEvent, isSelected, cellH);
                row.addView(cell);
            }

            table.addView(row);
        }
    }

    private TextView makeDayCell(int day, boolean hasEvent, boolean isSelected, int heightPx) {
        TextView tv = new TextView(requireContext());
        TableRow.LayoutParams lp = new TableRow.LayoutParams(0, heightPx, 1f);
        lp.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        if (day < 1) return tv;

        tv.setText(String.valueOf(day));

        if (isSelected) {
            tv.setBackground(makeCircle(COLOR_PRIMARY, 0, 0));
            tv.setTextColor(Color.WHITE);
        } else if (hasEvent) {
            tv.setBackground(makeCircle(COLOR_PRIMARY_LIGHT, COLOR_PRIMARY, 2));
            tv.setTextColor(COLOR_PRIMARY);
        } else {
            tv.setTextColor(COLOR_TEXT_DARK);
        }

        final int d = day;
        tv.setOnClickListener(v -> onDayClicked(d));
        return tv;
    }

    private GradientDrawable makeCircle(int fillArgb, int strokeArgb, int strokeDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(fillArgb);
        if (strokeDp > 0) gd.setStroke(dpToPx(strokeDp), strokeArgb);
        return gd;
    }

    private void onDayClicked(int day) {
        selectedYear  = currentYear;
        selectedMonth = currentMonth;
        selectedDay   = day;
        buildCalendarGrid(currentYear, currentMonth);
        showEventsForSelectedDate();
    }





    private void loadEventsFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference eventsCollection = db.collection("events");
        eventsCollection.get()
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





    private void loadAttendanceAndMerge() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { loadUserSocietiesAndFilter(); return; }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference attendanceCollection = db.collection(ATTENDANCE_COLLECTION);
        DocumentReference userAttendanceDocument = attendanceCollection.document(user.getUid());
        CollectionReference attendingEventsCollection = userAttendanceDocument.collection(ATTENDING_SUB);
        attendingEventsCollection.get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    Set<String> attendingIds = new HashSet<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) attendingIds.add(doc.getId());
                    for (Event event : allVisibleEvents) event.setAttending(attendingIds.contains(event.getId()));
                    loadUserSocietiesAndFilter();
                })
                .addOnFailureListener(e -> { if (isAdded()) loadUserSocietiesAndFilter(); });
    }





    private void loadUserSocietiesAndFilter() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { applyVisibilityFilter(); return; }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usersCollection = db.collection("users");
        DocumentReference userDocument = usersCollection.document(user.getUid());
        userDocument.get()
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
                .addOnFailureListener(e -> { if (isAdded()) applyVisibilityFilter(); });
    }

    private void applyVisibilityFilter() {
        List<Event> visible = new ArrayList<>();
        for (Event e : allVisibleEvents) {
            if (userSocietyIds.contains(e.getSocietyId()) || e.isAttending()) visible.add(e);
        }
        allVisibleEvents.clear();
        allVisibleEvents.addAll(visible);


        buildCalendarGrid(currentYear, currentMonth);
        showEventsForSelectedDate();
    }





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
        updateEventsHeader(selectedYear, selectedMonth, selectedDay);
        adapter.updateList(dayEvents);
        tvNoEvents.setVisibility(dayEvents.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private Set<Integer> getEventDaysForMonth(int year, int month) {
        Set<Integer> days = new HashSet<>();
        for (Event e : allVisibleEvents) {
            Date d = parseEventDate(e.getDateTime());
            if (d == null) continue;
            Calendar ec = Calendar.getInstance();
            ec.setTime(d);
            if (ec.get(Calendar.YEAR) == year && ec.get(Calendar.MONTH) == month) {
                days.add(ec.get(Calendar.DAY_OF_MONTH));
            }
        }
        return days;
    }

    private void updateEventsHeader(int year, int month, int day) {
        if (tvEventsOnDate == null) return;
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        tvEventsOnDate.setText("Events on " + SDF_HEADER.format(cal.getTime()));
    }






    @Nullable
    static Date parseEventDate(@Nullable String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) return null;
        try {
            String datePart = dateTime.contains(" • ")
                    ? dateTime.split(" • ")[0].trim()
                    : dateTime.trim();
            return SDF_PARSE.parse(datePart);
        } catch (ParseException e) {
            return null;
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * requireContext().getResources().getDisplayMetrics().density);
    }

    @NonNull
    private static String safeString(@Nullable String value, @NonNull String fallback) {
        return (value != null && !(value == null || value.trim().isEmpty())) ? value.trim() : fallback;
    }
}
