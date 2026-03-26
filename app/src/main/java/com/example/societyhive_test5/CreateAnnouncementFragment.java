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
 * Admin screen for posting a new announcement.
 *
 * Loads all societies from Firestore so the admin can post to any society
 * regardless of their own membership. Society is chosen via an exposed
 * dropdown menu that clearly labels itself "Select Society".
 */
public class CreateAnnouncementFragment extends Fragment {

    private TextInputEditText    etTitle;
    private TextInputEditText    etContent;
    private AutoCompleteTextView actvSociety;

    private final List<String> societyNames = new ArrayList<>();
    private final List<String> societyIds   = new ArrayList<>();
    private int selectedSocietyIndex = 0;

    public CreateAnnouncementFragment() {
        super(R.layout.fragment_create_announcement);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etTitle     = view.findViewById(R.id.etTitle);
        etContent   = view.findViewById(R.id.etContent);
        actvSociety = view.findViewById(R.id.actvSociety);

        MaterialButton btnPost = view.findViewById(R.id.btnPostAnnouncement);
        btnPost.setOnClickListener(v -> attemptPost());

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

    private void attemptPost() {
        String title   = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";

        if (title.isEmpty()) {
            etTitle.setError("Please enter a title");
            return;
        }
        if (content.isEmpty()) {
            etContent.setError("Please enter some content");
            return;
        }
        if (societyIds.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No society found to post to.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("title",     title);
        data.put("content",   content);
        data.put("societyId", societyIds.get(selectedSocietyIndex));
        data.put("createdBy", user.getUid());
        data.put("createdAt", Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection("announcements")
                .add(data)
                .addOnSuccessListener(ref -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Announcement posted!", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).navigateUp();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to post: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
