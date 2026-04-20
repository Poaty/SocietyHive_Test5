package com.example.societyhive_test5;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateEventFragment extends Fragment {

    private static final String[] MONTH_NAMES = {
            "Jan","Feb","Mar","Apr","May","Jun",
            "Jul","Aug","Sep","Oct","Nov","Dec"
    };

    private TextInputEditText etEventName;
    private TextInputEditText etDescription;
    private TextInputEditText etLocation;
    private TextInputEditText etDateTime;
    private SwitchMaterial       switchPublic;
    private AutoCompleteTextView actvSociety;

    private int pickedYear, pickedMonth, pickedDay, pickedHour, pickedMinute;
    private boolean dateTimePicked = false;

    private final List<String> societyNames = new ArrayList<>();
    private final List<String> societyIds   = new ArrayList<>();
    private int selectedSocietyIndex = 0;
    private String preSelectedSocietyId = "";

    public CreateEventFragment() {
        super(R.layout.fragment_create_event);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etEventName    = view.findViewById(R.id.etEventName);
        etDescription  = view.findViewById(R.id.etDescription);
        etLocation     = view.findViewById(R.id.etLocation);
        etDateTime     = view.findViewById(R.id.etDateTime);
        switchPublic = view.findViewById(R.id.switchPublic);
        actvSociety  = view.findViewById(R.id.actvSociety);

        MaterialButton btnCreate = view.findViewById(R.id.btnCreateEvent);

        etDateTime.setOnClickListener(v -> showDatePicker());
        btnCreate.setOnClickListener(v -> tryCreateEvent());

        if (getArguments() != null) {
            preSelectedSocietyId = getArguments().getString("preSelectedSocietyId", "");
        }

        fetchSocieties();
    }


    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (datePicker, year, month, day) -> {
            pickedYear  = year;
            pickedMonth = month;
            pickedDay   = day;
            showTimePicker();
        }, cal.get(Calendar.YEAR),
           cal.get(Calendar.MONTH),
           cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(requireContext(), (timePicker, hour, minute) -> {
            pickedHour   = hour;
            pickedMinute = minute;
            dateTimePicked = true;

            // reject past times so the event list doesn't immediately look broken
            java.util.Calendar selected = java.util.Calendar.getInstance();
            selected.set(pickedYear, pickedMonth, pickedDay, hour, minute, 0);
            if (selected.before(java.util.Calendar.getInstance())) {
                dateTimePicked = false;
                etDateTime.setText("");
                Toast.makeText(requireContext(),
                        "Please select a future date and time.", Toast.LENGTH_SHORT).show();
                return;
            }

            String formatted = String.format(Locale.getDefault(),
                    "%d-%s-%d • %02d:%02d",
                    pickedDay, MONTH_NAMES[pickedMonth], pickedYear, hour, minute);
            etDateTime.setText(formatted);
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
    }


    private void fetchSocieties() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference societies = db.collection("societies");
        societies.get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    societyIds.clear();
                    societyNames.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        societyIds.add(doc.getId());
                        societyNames.add(name != null ? name : doc.getId());
                    }
                    buildSocietyDropdown();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "couldn't load societies", Toast.LENGTH_SHORT).show();
                });
    }

    private void buildSocietyDropdown() {
        if (societyIds.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No societies found. Add societies to Firestore first.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, societyNames);
        actvSociety.setAdapter(adapter);
        actvSociety.setText(societyNames.get(0), false);
        actvSociety.setOnItemClickListener(
                (parent, v, position, id) -> selectedSocietyIndex = position);

        // if opened from a society's own admin panel, lock the dropdown so you can't accidentally
        // create the event under a different society
        if (!preSelectedSocietyId.isEmpty()) {
            int idx = societyIds.indexOf(preSelectedSocietyId);
            if (idx >= 0) {
                selectedSocietyIndex = idx;
                actvSociety.setText(societyNames.get(idx), false);
            }
            actvSociety.setEnabled(false);
        }
    }


    private void tryCreateEvent() {
        String name = etEventName.getText() != null ? etEventName.getText().toString().trim() : "";
        String description = text(etDescription);
        String location    = text(etLocation);
        String dateTime    = text(etDateTime);

        if (name.isEmpty()) {
            etEventName.setError("Please enter an event name");
            return;
        }
        if (name.length() > 100) {
            etEventName.setError("Name is too long (max 100)");
            return;
        }
        if (description.isEmpty()) {
            etDescription.setError("Please enter a description");
            return;
        }
        if (description.length() > 1000) {
            etDescription.setError("Description is too long (max 1000)");
            return;
        }
        if (location.length() > 120) {
            etLocation.setError("Location is too long (max 120)");
            return;
        }
        if (!dateTimePicked) {
            Toast.makeText(requireContext(),
                    "Please pick a date and time.", Toast.LENGTH_SHORT).show();
            return;
        }
        // TODO: past-date check would be nice here. at the moment you can create an event last week
        //  which turns up as "upcoming" and confuses people
        if (societyIds.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No society selected.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedSocietyIndex < 0 || selectedSocietyIndex >= societyIds.size()) {
            selectedSocietyIndex = 0;
        }

        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null) return;

        String societyId = societyIds.get(selectedSocietyIndex);
        boolean isPublic = switchPublic.isChecked();

        // pull the organiser name from their profile — fall back to email if somehow blank
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDoc = db.collection("users").document(user.getUid());
        userDoc.get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    String organiser = doc.getString("fullName");

                    if (organiser == null || TextHelpers.isBlank(organiser)) {
                        organiser = user.getEmail() != null ? user.getEmail() : "Admin";
                    }
                    persistEvent(name, description, location, dateTime,
                               organiser, societyId, isPublic, user.getUid());
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;

                    persistEvent(name, description, location, dateTime,
                               "Admin", societyId, isPublic, user.getUid());
                });
    }

    private void persistEvent(String name, String description, String location,
                            String dateTime, String organiser,
                            String societyId, boolean isPublic, String uid) {
        Map<String, Object> data = new HashMap<>();
        data.put("name",        name);
        data.put("description", description);
        data.put("location",    location.isEmpty() ? "TBC" : location);
        data.put("dateTime",    dateTime);
        data.put("organiser",   organiser);
        data.put("societyId",   societyId);
        data.put("isPublic",    isPublic);
        data.put("createdBy",   uid);
        data.put("createdAt",   Timestamp.now());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference events = db.collection("events");
        events.add(data)
                .addOnSuccessListener(ref -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireActivity(), "Event created!", Toast.LENGTH_SHORT).show();
                    NavHelpers.navigateUp(this);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @NonNull
    private String text(TextInputEditText et) {
        return (et != null && et.getText() != null) ? TextHelpers.trimmed(et) : "";
    }
}
