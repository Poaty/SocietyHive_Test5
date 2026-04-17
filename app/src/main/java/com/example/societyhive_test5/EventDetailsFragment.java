package com.example.societyhive_test5;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.HashMap;
import java.util.Map;

public class EventDetailsFragment extends Fragment {

    private static final String ATTENDANCE_COLLECTION = "userAttendance";
    private static final String ATTENDING_SUB = "attendingEvents";


    private TextView tvTitle;
    private TextView tvMeta;
    private TextView tvOrganiser;
    private TextView tvDescription;
    private MaterialButton btnAttend;
    private ImageView ivQrCode;
    private MaterialCardView cardQr;


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
        ivQrCode      = view.findViewById(R.id.ivQrCode);
        cardQr        = view.findViewById(R.id.cardQr);

        Bundle args = getArguments();
        eventId = args != null ? args.getString("eventId") : null;

        if (eventId == null) {
            tvTitle.setText("Event not found");
            return;
        }

        loadEvent();
    }





    private void loadEvent() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference eventsCollection = db.collection("events");
        DocumentReference eventDocument = eventsCollection.document(eventId);
        eventDocument.get()
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
        generateQrCode();
    }

    // generate a qr with the event id encoded so it can be scanned for check-in
    private void generateQrCode() {
        if (eventId == null || ivQrCode == null || cardQr == null) return;
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(eventId, BarcodeFormat.QR_CODE, 400, 400);
            ivQrCode.setImageBitmap(bitmap);
            cardQr.setVisibility(View.VISIBLE);
        } catch (Exception ignored) {}
    }





    private void loadAttendanceState() {
        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null) {

            updateAttendButton(false);
            wireAttendButton();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference attendanceCollection = db.collection(ATTENDANCE_COLLECTION);
        DocumentReference userAttendanceDocument = attendanceCollection.document(user.getUid());
        CollectionReference attendingEventsCollection = userAttendanceDocument.collection(ATTENDING_SUB);
        DocumentReference eventAttendanceDocument = attendingEventsCollection.document(eventId);
        eventAttendanceDocument.get()
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
        // toggle and save - optimistic update so it feels instant
        btnAttend.setOnClickListener(v -> {
            isAttending = !isAttending;
            updateAttendButton(isAttending);
            saveAttendance(isAttending);
        });
    }





    private void saveAttendance(boolean attending) {
        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null) {
            Toast.makeText(requireContext(),
                    "Please log in to attend events.", Toast.LENGTH_SHORT).show();
            isAttending = !attending;
            updateAttendButton(isAttending);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference attendanceCollection = db.collection(ATTENDANCE_COLLECTION);
        DocumentReference userAttendanceDocument = attendanceCollection.document(user.getUid());
        CollectionReference attendingEventsCollection = userAttendanceDocument.collection(ATTENDING_SUB);
        DocumentReference ref = attendingEventsCollection.document(eventId);

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





    private void updateAttendButton(boolean attending) {
        btnAttend.setText(attending ? "Attending ✓" : "Attend Event");
    }

    @NonNull
    private String safeString(@Nullable String value, @NonNull String fallback) {

        return (value != null && !TextHelpers.isBlank(value)) ? value.trim() : fallback;

    }
}
