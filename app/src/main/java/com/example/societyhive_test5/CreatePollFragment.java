package com.example.societyhive_test5;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatePollFragment extends Fragment {

    private TextInputEditText etTitle;
    private TextInputEditText etQuestion;
    private TextInputEditText etCloseDate;
    private LinearLayout optionsContainer;
    private AutoCompleteTextView actvSociety;

    private final List<TextInputEditText> optionFields = new ArrayList<>();
    private final List<String> societyNames = new ArrayList<>();
    private final List<String> societyIds   = new ArrayList<>();
    private int selectedSocietyIndex = 0;
    private String preSelectedSocietyId = "";
    private Calendar closeDateCal = null;

    public CreatePollFragment() {
        super(R.layout.fragment_create_poll);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etTitle          = view.findViewById(R.id.etTitle);
        etQuestion       = view.findViewById(R.id.etQuestion);
        etCloseDate      = view.findViewById(R.id.etCloseDate);
        optionsContainer = view.findViewById(R.id.optionsContainer);
        actvSociety      = view.findViewById(R.id.actvSociety);

        etCloseDate.setOnClickListener(v -> showDatePicker());

        MaterialButton btnAddOption  = view.findViewById(R.id.btnAddOption);
        MaterialButton btnCreatePoll = view.findViewById(R.id.btnCreatePoll);

        // start with 2 blank options so the form doesn't look empty
        addOptionField();
        addOptionField();

        btnAddOption.setOnClickListener(v -> addOptionField());
        btnCreatePoll.setOnClickListener(v -> submitPoll());

        if (getArguments() != null) {
            preSelectedSocietyId = getArguments().getString("preSelectedSocietyId", "");
        }

        fetchSocieties();
    }


    private void showDatePicker() {
        Calendar start = closeDateCal != null ? closeDateCal : Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (datePicker, year, month, day) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, day, 23, 59, 59);
                    picked.set(Calendar.MILLISECOND, 0);

                    // closing date must be in the future, otherwise votes could be cast after it closes
                    if (!picked.after(Calendar.getInstance())) {
                        Toast.makeText(requireContext(),
                                "Closing date must be in the future", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    closeDateCal = picked;
                    etCloseDate.setText(
                            new SimpleDateFormat("d MMM yyyy", Locale.UK)
                                    .format(closeDateCal.getTime()));
                },
                start.get(Calendar.YEAR),
                start.get(Calendar.MONTH),
                start.get(Calendar.DAY_OF_MONTH));

        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }


    private void addOptionField() {
        View optionView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_poll_option_input, optionsContainer, false);
        TextInputLayout til = (TextInputLayout) optionView;
        til.setHint("Option " + (optionFields.size() + 1));

        TextInputEditText et = optionView.findViewById(R.id.etOption);
        optionsContainer.addView(optionView);
        optionFields.add(et);
    }

    private void fetchSocieties() {
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
                    buildDropdown();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "couldn't load societies", Toast.LENGTH_SHORT).show();
                });
    }

    private void buildDropdown() {
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


    private void submitPoll() {
        String title    = text(etTitle);
        String question = text(etQuestion);

        if (title.isEmpty()) {
            etTitle.setError("Please enter a poll title");
            return;
        }
        if (question.isEmpty()) {
            etQuestion.setError("Please enter a poll question");
            return;
        }

        if (title.length() > 120) {
            etTitle.setError("Title is too long (max 120)");
            return;
        }
        if (question.length() > 500) {
            etQuestion.setError("Question is too long (max 500)");
            return;
        }

        List<String> options = new ArrayList<>();
        for (TextInputEditText et : optionFields) {
            String t = text(et);
            if (t.isEmpty()) continue;
            if (t.length() > 100) {
                et.setError("Option is too long (max 100)");
                return;
            }
            // case-insensitive duplicate check so "Yes" and "yes" don't both count as real options
            boolean dup = false;
            for (String existing : options) {
                if (existing.equalsIgnoreCase(t)) { dup = true; break; }
            }
            if (dup) {
                et.setError("Duplicate option");
                return;
            }
            options.add(t);
        }
        if (options.size() < 2) {
            Toast.makeText(requireContext(),
                    "Please enter at least 2 options.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (societyIds.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No society selected.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedSocietyIndex < 0 || selectedSocietyIndex >= societyIds.size()) {
            selectedSocietyIndex = 0;
        }
        if (closeDateCal != null && closeDateCal.getTimeInMillis() <= System.currentTimeMillis()) {
            etCloseDate.setError("Close date must be in the future");
            return;
        }

        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null) return;

        String societyId = societyIds.get(selectedSocietyIndex);

        Map<String, Object> data = new HashMap<>();
        data.put("title",     title);
        data.put("question",  question);
        data.put("options",   options);
        data.put("societyId", societyId);
        data.put("isActive",  true);
        data.put("createdBy", user.getUid());
        data.put("createdAt", Timestamp.now());
        if (closeDateCal != null) {
            data.put("endsAt", new Timestamp(closeDateCal.getTime()));
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference pollsCollection = db.collection("polls");
        pollsCollection.add(data)
                .addOnSuccessListener(ref -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireActivity(), "Poll created!", Toast.LENGTH_SHORT).show();
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
