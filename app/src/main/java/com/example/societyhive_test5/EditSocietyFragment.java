package com.example.societyhive_test5;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin screen for editing a society's profile.
 *
 * Loads all societies from Firestore. When the admin selects one, the
 * name, description and hexColor fields are pre-filled. Saving writes
 * only those three fields back to the document.
 *
 * Firestore path:  societies/{societyId}
 *   name        (String)
 *   description (String)
 *   hexColor    (String)
 */
public class EditSocietyFragment extends Fragment {

    private AutoCompleteTextView actvSociety;
    private TextInputEditText    etName;
    private TextInputEditText    etDescription;
    private TextInputEditText    etColor;
    private View                 colorSwatch;

    private final List<String> societyNames = new ArrayList<>();
    private final List<String> societyIds   = new ArrayList<>();
    // Parallel lists for the current field values loaded from Firestore
    private final List<String> storedNames  = new ArrayList<>();
    private final List<String> storedDescs  = new ArrayList<>();
    private final List<String> storedColors = new ArrayList<>();

    private int selectedIndex = 0;

    public EditSocietyFragment() {
        super(R.layout.fragment_edit_society);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        actvSociety   = view.findViewById(R.id.actvSociety);
        etName        = view.findViewById(R.id.etName);
        etDescription = view.findViewById(R.id.etDescription);
        etColor       = view.findViewById(R.id.etColor);
        colorSwatch   = view.findViewById(R.id.colorSwatch);

        MaterialButton btnSave = view.findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> attemptSave());

        // Live-preview the colour swatch as the admin types
        etColor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                updateSwatch(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadSocieties();
    }

    // -------------------------------------------------------------------------

    private void loadSocieties() {
        FirebaseFirestore.getInstance()
                .collection("societies")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    societyIds.clear();
                    societyNames.clear();
                    storedNames.clear();
                    storedDescs.clear();
                    storedColors.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name  = doc.getString("name");
                        String desc  = doc.getString("description");
                        String color = doc.getString("hexColor");
                        societyIds.add(doc.getId());
                        societyNames.add(name  != null ? name  : doc.getId());
                        storedNames .add(name  != null ? name  : "");
                        storedDescs .add(desc  != null ? desc  : "");
                        storedColors.add(color != null ? color : "#8D2E3A");
                    }

                    setupDropdown();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to load societies.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupDropdown() {
        if (societyIds.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No societies found in Firestore.", Toast.LENGTH_LONG).show();
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, societyNames);
        actvSociety.setAdapter(adapter);
        actvSociety.setText(societyNames.get(0), false);
        fillFields(0);

        actvSociety.setOnItemClickListener((parent, v, position, id) -> {
            selectedIndex = position;
            fillFields(position);
        });
    }

    /** Pre-fills the form fields with the stored values for the selected society. */
    private void fillFields(int index) {
        etName.setText(storedNames.get(index));
        etDescription.setText(storedDescs.get(index));
        String color = storedColors.get(index);
        etColor.setText(color);
        updateSwatch(color);
    }

    private void updateSwatch(@NonNull String hex) {
        try {
            colorSwatch.setBackgroundColor(Color.parseColor(hex));
        } catch (IllegalArgumentException ignored) {
            // Don't crash while user is mid-typing
        }
    }

    // -------------------------------------------------------------------------

    private void attemptSave() {
        if (societyIds.isEmpty()) {
            Toast.makeText(requireContext(), "No society selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        String name  = text(etName);
        String desc  = text(etDescription);
        String color = text(etColor);

        if (name.isEmpty()) {
            etName.setError("Please enter a society name");
            return;
        }
        if (!color.startsWith("#") || color.length() < 4) {
            etColor.setError("Enter a valid hex colour (e.g. #8D2E3A)");
            return;
        }
        // Validate the hex is parseable
        try {
            Color.parseColor(color);
        } catch (IllegalArgumentException e) {
            etColor.setError("Invalid colour — use format #RRGGBB");
            return;
        }

        String societyId = societyIds.get(selectedIndex);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name",        name);
        updates.put("description", desc);
        updates.put("hexColor",    color);

        FirebaseFirestore.getInstance()
                .collection("societies")
                .document(societyId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Society updated!", Toast.LENGTH_SHORT).show();
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
