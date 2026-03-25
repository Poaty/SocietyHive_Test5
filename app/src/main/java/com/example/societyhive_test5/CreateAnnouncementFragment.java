package com.example.societyhive_test5;

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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin screen for posting a new announcement.
 *
 * If the admin belongs to a single society the societyId is picked automatically.
 * If they belong to multiple, a Spinner lets them choose which society to post to.
 */
public class CreateAnnouncementFragment extends Fragment {

    private TextInputEditText etTitle;
    private TextInputEditText etContent;
    private Spinner spinnerSociety;
    private TextView tvSocietyLabel;

    // Parallel lists: display names and their Firestore IDs
    private final List<String> societyNames = new ArrayList<>();
    private final List<String> societyIds   = new ArrayList<>();

    public CreateAnnouncementFragment() {
        super(R.layout.fragment_create_announcement);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etTitle         = view.findViewById(R.id.etTitle);
        etContent       = view.findViewById(R.id.etContent);
        spinnerSociety  = view.findViewById(R.id.spinnerSociety);
        tvSocietyLabel  = view.findViewById(R.id.tvSocietyLabel);

        MaterialButton btnPost = view.findViewById(R.id.btnPostAnnouncement);
        btnPost.setOnClickListener(v -> attemptPost());

        loadSocieties();
    }

    // -------------------------------------------------------------------------
    // Load the admin's societies to populate the spinner
    // -------------------------------------------------------------------------

    private void loadSocieties() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;

                    List<?> ids = (List<?>) doc.get("societyIds");
                    if (ids == null || ids.isEmpty()) return;

                    final int[] remaining = {ids.size()};

                    for (Object idObj : ids) {
                        if (!(idObj instanceof String)) { remaining[0]--; continue; }
                        String sid = (String) idObj;

                        FirebaseFirestore.getInstance()
                                .collection("societies").document(sid).get()
                                .addOnCompleteListener(task -> {
                                    if (!isAdded()) return;
                                    if (task.isSuccessful() && task.getResult().exists()) {
                                        String name = task.getResult().getString("name");
                                        societyIds.add(sid);
                                        societyNames.add(name != null ? name : sid);
                                    }
                                    remaining[0]--;
                                    if (remaining[0] == 0) setupSpinner();
                                });
                    }
                });
    }

    private void setupSpinner() {
        if (societyIds.size() > 1) {
            // Multiple societies — show the picker
            tvSocietyLabel.setVisibility(View.VISIBLE);
            spinnerSociety.setVisibility(View.VISIBLE);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    societyNames
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSociety.setAdapter(adapter);
        }
        // If only one society, it's auto-selected silently (societyIds.get(0))
    }

    // -------------------------------------------------------------------------
    // Post
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
            Toast.makeText(requireContext(), "No society found to post to.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String selectedSocietyId = societyIds.size() == 1
                ? societyIds.get(0)
                : societyIds.get(spinnerSociety.getSelectedItemPosition());

        Map<String, Object> data = new HashMap<>();
        data.put("title",     title);
        data.put("content",   content);
        data.put("societyId", selectedSocietyId);
        data.put("createdBy", user.getUid());
        data.put("createdAt", Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection("announcements")
                .add(data)
                .addOnSuccessListener(ref -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Announcement posted!", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).navigateUp();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to post: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
