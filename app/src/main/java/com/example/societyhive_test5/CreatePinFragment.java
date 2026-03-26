package com.example.societyhive_test5;

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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin screen for creating a pinned message for a society.
 *
 * Writes to Firestore:
 *   pins/{auto}
 *     content    (String)
 *     societyId  (String)
 *     createdBy  (String)
 *     createdAt  (Timestamp)
 */
public class CreatePinFragment extends Fragment {

    private TextInputEditText    etPinContent;
    private AutoCompleteTextView actvSociety;

    private final List<String> societyNames = new ArrayList<>();
    private final List<String> societyIds   = new ArrayList<>();
    private int selectedSocietyIndex = 0;

    public CreatePinFragment() {
        super(R.layout.fragment_create_pin);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etPinContent = view.findViewById(R.id.etPinContent);
        actvSociety  = view.findViewById(R.id.actvSociety);

        MaterialButton btnCreatePin = view.findViewById(R.id.btnCreatePin);
        btnCreatePin.setOnClickListener(v -> attemptCreate());

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
    }

    // -------------------------------------------------------------------------

    private void attemptCreate() {
        String content = etPinContent.getText() != null
                ? etPinContent.getText().toString().trim() : "";

        if (content.isEmpty()) {
            etPinContent.setError("Please write a pin message");
            return;
        }
        if (societyIds.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No society selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String societyId = societyIds.get(selectedSocietyIndex);

        Map<String, Object> data = new HashMap<>();
        data.put("content",   content);
        data.put("societyId", societyId);
        data.put("createdBy", user.getUid());
        data.put("createdAt", Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection("pins")
                .add(data)
                .addOnSuccessListener(ref -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Pin created!", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).navigateUp();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
