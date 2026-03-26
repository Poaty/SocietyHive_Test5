package com.example.societyhive_test5;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Admin screen for creating a new event.
 *
 * Writes to Firestore structure:
 *   events/{auto}
 *     name        (String)
 *     description (String)
 *     location    (String)
 *     dateTime    (String, e.g. "19-Nov-2025 • 17:00")
 *     organiser   (String) — admin's display name
 *     societyId   (String)
 *     isPublic    (boolean)
 *     createdBy   (String) — admin uid
 *     createdAt   (Timestamp)
 */
public class CreateEventFragment extends Fragment {

    private static final String[] MONTH_NAMES = {
            "Jan","Feb","Mar","Apr","May","Jun",
            "Jul","Aug","Sep","Oct","Nov","Dec"
    };

    private TextInputEditText etEventName;
    private TextInputEditText etDescription;
    private TextInputEditText etLocation;
    private TextInputEditText etDateTime;
    private SwitchMaterial    switchPublic;
    private Spinner           spinnerSociety;
    private TextView          tvSocietyLabel;

    // Picked date/time parts
    private int pickedYear, pickedMonth, pickedDay, pickedHour, pickedMinute;
    private boolean dateTimePicked = false;

    private final List<String> societyNames = new ArrayList<>();
    private final List<String> societyIds   = new ArrayList<>();

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
        switchPublic   = view.findViewById(R.id.switchPublic);
        spinnerSociety = view.findViewById(R.id.spinnerSociety);
        tvSocietyLabel = view.findViewById(R.id.tvSocietyLabel);

        MaterialButton btnCreate = view.findViewById(R.id.btnCreateEvent);

        // Tapping the date/time field opens a DatePicker, then a TimePicker
        etDateTime.setOnClickListener(v -> showDatePicker());

        btnCreate.setOnClickListener(v -> attemptCreate());

        loadSocieties();
    }

    // -------------------------------------------------------------------------
    // Date / Time pickers
    // -------------------------------------------------------------------------

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
            String formatted = String.format(Locale.getDefault(),
                    "%d-%s-%d • %02d:%02d",
                    pickedDay, MONTH_NAMES[pickedMonth], pickedYear, hour, minute);
            etDateTime.setText(formatted);
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
    }

    // -------------------------------------------------------------------------
    // Societies
    // -------------------------------------------------------------------------

    private void loadSocieties() {
        FirebaseFirestore.getInstance()
                .collection("societies")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    societyIds.clear();
                    societyNames.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        societyIds.add(doc.getId());
                        societyNames.add(name != null ? name : doc.getId());
                    }
                    setupSpinner();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to load societies.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupSpinner() {
        if (societyIds.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No societies found. Add societies to Firestore first.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        tvSocietyLabel.setVisibility(View.VISIBLE);
        spinnerSociety.setVisibility(View.VISIBLE);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, societyNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSociety.setAdapter(adapter);
    }

    // -------------------------------------------------------------------------
    // Submit
    // -------------------------------------------------------------------------

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

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String societyId = societyIds.get(spinnerSociety.getSelectedItemPosition());
        boolean isPublic = switchPublic.isChecked();

        // Fetch the admin's display name to use as organiser
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    String organiser = doc.getString("fullName");
                    if (organiser == null || organiser.trim().isEmpty()) {
                        organiser = user.getEmail() != null ? user.getEmail() : "Admin";
                    }
                    writeEvent(name, description, location, dateTime,
                               organiser, societyId, isPublic, user.getUid());
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    // Fall back gracefully
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

        FirebaseFirestore.getInstance()
                .collection("events")
                .add(data)
                .addOnSuccessListener(ref -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Event created!", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).navigateUp();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @NonNull
    private String text(@Nullable TextInputEditText et) {
        return (et != null && et.getText() != null) ? et.getText().toString().trim() : "";
    }
}
