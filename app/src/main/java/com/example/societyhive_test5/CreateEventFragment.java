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

        btnCreate.setOnClickListener(v -> attemptCreate());

        if (getArguments() != null) {
            preSelectedSocietyId = getArguments().getString("preSelectedSocietyId", "");
        }

        loadSocieties();
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





    private void loadSocieties() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference societiesCollection = db.collection("societies");
        societiesCollection.get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    societyIds.clear();
                    societyNames.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        societyIds.add(doc.getId());
                        societyNames.add(name != null ? name : doc.getId());
                    }
                    setupSocietyDropdown();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to load societies.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupSocietyDropdown() {
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

        if (!preSelectedSocietyId.isEmpty()) {
            int idx = societyIds.indexOf(preSelectedSocietyId);
            if (idx >= 0) {
                selectedSocietyIndex = idx;
                actvSociety.setText(societyNames.get(idx), false);
            }
            actvSociety.setEnabled(false);
        }
    }





    private void attemptCreate() {
        String name        = text(etEventName);
        String description = text(etDescription);
        String location    = text(etLocation);
        String dateTime    = text(etDateTime);

        if (name.isEmpty()) {
            etEventName.setError("Please enter an event name");
            return;
        }
        if (description.isEmpty()) {
            etDescription.setError("Please enter a description");
            return;
        }
        if (!dateTimePicked) {
            Toast.makeText(requireContext(),
                    "Please pick a date and time.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (societyIds.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No society selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null) return;

        String societyId = societyIds.get(selectedSocietyIndex);
        boolean isPublic = switchPublic.isChecked();


        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usersCollection = db.collection("users");
        DocumentReference userDocument = usersCollection.document(user.getUid());
        userDocument.get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    String organiser = doc.getString("fullName");
                    if (organiser == null || TextHelpers.isBlank(organiser)) {
                        organiser = user.getEmail() != null ? user.getEmail() : "Admin";
                    }
                    writeEvent(name, description, location, dateTime,
                               organiser, societyId, isPublic, user.getUid());
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;

                    writeEvent(name, description, location, dateTime,
                               "Admin", societyId, isPublic, user.getUid());
                });
    }

    private void writeEvent(String name, String description, String location,
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
        CollectionReference eventsCollection = db.collection("events");
        eventsCollection.add(data)
                .addOnSuccessListener(ref -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Event created!", Toast.LENGTH_SHORT).show();
                    NavHelpers.navigateUp(this);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @NonNull
    private String text(@Nullable TextInputEditText et) {
        return (et != null && et.getText() != null) ? TextHelpers.trimmed(et) : "";
    }
}
