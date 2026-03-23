package com.example.societyhive_test5;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Full event details screen.
 *
 * Receives an eventId via navigation arguments, fetches the event document
 * from Firestore, and displays all fields. The Attend button reads and writes
 * to the same userAttendance path used by EventsFragment so attendance state
 * is consistent across both screens.
 */
public class EventDetailsFragment extends Fragment {

    private static final String ATTENDANCE_COLLECTION = "userAttendance";
    private static final String ATTENDING_SUB = "attendingEvents";

    // Views
    private TextView tvTitle;
    private TextView tvMeta;
    private TextView tvOrganiser;
    private TextView tvDescription;
    private MaterialButton btnAttend;

    // The event loaded from Firestore
    private String eventId;
    private String eventName = "";
    private boolean isAttending = false;

    public EventDetailsFragment() {
        super(R.layout.fragment_event_details);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialFadeThrough());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTitle       = view.findViewById(R.id.tvEventTitle);
        tvMeta        = view.findViewById(R.id.tvEventMeta);
        tvOrganiser   = view.findViewById(R.id.tvEventOrganiser);
        tvDescription = view.findViewById(R.id.tvEventDescription);
        btnAttend     = view.findViewById(R.id.btnAttendDetails);

        Bundle args = getArguments();
        eventId = args != null ? args.getString("eventId") : null;

        if (eventId == null) {
            tvTitle.setText("Event not found");
            return;
        }

        loadEvent();
    }

    // -------------------------------------------------------------------------
    // Load event from Firestore
    // -------------------------------------------------------------------------

    private void loadEvent() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || !doc.exists()) return;
                    bindEvent(doc);
                    loadAttendanceState();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Could not load event.", Toast.LENGTH_SHORT).show();
                });
    }

    private void bindEvent(@NonNull DocumentSnapshot doc) {
        eventName = safeString(doc.getString("name"), "Unnamed Event");
        String dateTime  = safeString(doc.getString("dateTime"), "TBC");
        String location  = safeString(doc.getString("location"), "TBC");
        String organiser = safeString(doc.getString("organiser"), "Unknown");
        String desc      = safeString(doc.getString("description"), "No description provided.");

        tvTitle.setText(eventName);
        tvMeta.setText(dateTime + "  •  " + location);
        tvOrganiser.setText("Organised by " + organiser);
        tvDescription.setText(desc);
    }

    // -------------------------------------------------------------------------
    // Load attendance state, then wire up the button
    // -------------------------------------------------------------------------

    private void loadAttendanceState() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Not logged in — show button but it won't save
            updateAttendButton(false);
            wireAttendButton();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(ATTENDANCE_COLLECTION)
                .document(user.getUid())
                .collection(ATTENDING_SUB)
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    isAttending = doc.exists();
                    updateAttendButton(isAttending);
                    wireAttendButton();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    updateAttendButton(false);
                    wireAttendButton();
                });
    }

    private void wireAttendButton() {
        btnAttend.setOnClickListener(v -> {
            isAttending = !isAttending;
            updateAttendButton(isAttending);
            saveAttendance(isAttending);
        });
    }

    // -------------------------------------------------------------------------
    // Save attendance to Firestore
    // -------------------------------------------------------------------------

    private void saveAttendance(boolean attending) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(),
                    "Please log in to attend events.", Toast.LENGTH_SHORT).show();
            isAttending = !attending;
            updateAttendButton(isAttending);
            return;
        }

        DocumentReference ref = FirebaseFirestore.getInstance()
                .collection(ATTENDANCE_COLLECTION)
                .document(user.getUid())
                .collection(ATTENDING_SUB)
                .document(eventId);

        if (attending) {
            Map<String, Object> data = new HashMap<>();
            data.put("eventId", eventId);
            data.put("eventName", eventName);
            data.put("attendedAt", Timestamp.now());
            ref.set(data).addOnFailureListener(e -> {
                if (!isAdded()) return;
                isAttending = false;
                updateAttendButton(false);
                Toast.makeText(requireContext(),
                        "Failed to save attendance.", Toast.LENGTH_SHORT).show();
            });
        } else {
            ref.delete().addOnFailureListener(e -> {
                if (!isAdded()) return;
                isAttending = true;
                updateAttendButton(true);
                Toast.makeText(requireContext(),
                        "Failed to remove attendance.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void updateAttendButton(boolean attending) {
        btnAttend.setText(attending ? "Attending ✓" : "Attend Event");
    }

    @NonNull
    private String safeString(@Nullable String value, @NonNull String fallback) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : fallback;
    }
}
