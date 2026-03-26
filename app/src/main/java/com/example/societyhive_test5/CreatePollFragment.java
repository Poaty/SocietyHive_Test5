package com.example.societyhive_test5;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin screen for creating a new poll.
 *
 * Loads all societies from Firestore so the admin can choose which
 * society the poll belongs to. Options can be added dynamically.
 *
 * Firestore structure written:
 *   polls/{auto}
 *     title       (String)
 *     question    (String)
 *     options     (Array<String>)
 *     societyId   (String)
 *     isActive    (boolean) = true
 *     createdBy   (String)
 *     createdAt   (Timestamp)
 */
public class CreatePollFragment extends Fragment {

    private TextInputEditText etTitle;
    private TextInputEditText etQuestion;
    private LinearLayout optionsContainer;
    private Spinner spinnerSociety;
    private TextView tvSocietyLabel;

    private final List<TextInputEditText> optionFields = new ArrayList<>();
    private final List<String> societyNames = new ArrayList<>();
    private final List<String> societyIds   = new ArrayList<>();

    public CreatePollFragment() {
        super(R.layout.fragment_create_poll);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etTitle        = view.findViewById(R.id.etTitle);
        etQuestion     = view.findViewById(R.id.etQuestion);
        optionsContainer = view.findViewById(R.id.optionsContainer);
        spinnerSociety = view.findViewById(R.id.spinnerSociety);
        tvSocietyLabel = view.findViewById(R.id.tvSocietyLabel);

        MaterialButton btnAddOption  = view.findViewById(R.id.btnAddOption);
        MaterialButton btnCreatePoll = view.findViewById(R.id.btnCreatePoll);

        // Start with two blank option fields
        addOptionField();
        addOptionField();

        btnAddOption.setOnClickListener(v -> addOptionField());
        btnCreatePoll.setOnClickListener(v -> attemptCreate());

        loadSocieties();
    }

    // -------------------------------------------------------------------------

    private void addOptionField() {
        View optionView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_poll_option, optionsContainer, false);
        TextInputLayout til = (TextInputLayout) optionView;
        til.setHint("Option " + (optionFields.size() + 1));

        TextInputEditText et = optionView.findViewById(R.id.etOption);
        optionsContainer.addView(optionView);
        optionFields.add(et);
    }

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

    private void attemptCreate() {
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

        List<String> options = new ArrayList<>();
        for (TextInputEditText et : optionFields) {
            String t = text(et);
            if (!t.isEmpty()) options.add(t);
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

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String societyId = societyIds.get(spinnerSociety.getSelectedItemPosition());

        Map<String, Object> data = new HashMap<>();
        data.put("title",     title);
        data.put("question",  question);
        data.put("options",   options);
        data.put("societyId", societyId);
        data.put("isActive",  true);
        data.put("createdBy", user.getUid());
        data.put("createdAt", Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection("polls")
                .add(data)
                .addOnSuccessListener(ref -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Poll created!", Toast.LENGTH_SHORT).show();
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
