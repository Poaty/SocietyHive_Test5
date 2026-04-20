package com.example.societyhive_test5;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private final List<Society> societies = new ArrayList<>();
    private JoinedSocietyAdapter adapter;

    private TextView tvName, tvEmail, tvRole;
    private ImageView ivProfilePicture;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadProfilePicture(uri);
            });

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvName          = view.findViewById(R.id.tvProfileName);
        tvEmail         = view.findViewById(R.id.tvProfileEmail);
        tvRole          = view.findViewById(R.id.tvProfileRole);
        ivProfilePicture = view.findViewById(R.id.ivProfilePicture);

        RecyclerView rv = view.findViewById(R.id.rvJoinedSocieties);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setHasFixedSize(false);
        adapter = new JoinedSocietyAdapter(new ArrayList<>(), this::showManageSheet);
        rv.setAdapter(adapter);

        // tap the picture to change it
        ivProfilePicture.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        view.findViewById(R.id.btnBrowseSocieties).setOnClickListener(v ->
                NavHelpers.navigate(this, R.id.browseSocietiesFragment));

        view.findViewById(R.id.btnLogOut).setOnClickListener(v -> {
            AuthHelpers.signOut();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        fetchProfileData();
    }

    // pulls user data and joined societies from firestore
    private void fetchProfileData() {
        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null) return;

        // show the auth email immediately — the firestore one might be slightly different if they changed it
        tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usersCollection = db.collection("users");
        DocumentReference userDocument = usersCollection.document(user.getUid());
        userDocument.get()
                .addOnSuccessListener(document -> {
                    if (!isAdded() || !document.exists()) return;

                    String fullName = document.getString("fullName");
                    String email    = document.getString("email");
                    String role     = document.getString("role");
                    String photoUrl = document.getString("profileImageUrl");

                    if (fullName != null && !fullName.isEmpty()) tvName.setText(fullName);
                    if (email    != null && !email.isEmpty())    tvEmail.setText(email);
                    if (role     != null && !role.isEmpty())     tvRole.setText(capitalize(role));

                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        loadAvatar(photoUrl);
                    }

                    List<String> societyIds = (List<String>) document.get("societyIds");
                    renderSocietyBadges(societyIds);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireActivity(),
                            "couldn't load profile", Toast.LENGTH_SHORT).show();
                });
    }

    // upload to cloudinary then save the url in firestore
    private void uploadProfilePicture(Uri uri) {
        Toast.makeText(requireContext(), "Uploading…", Toast.LENGTH_SHORT).show();
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dybgordqu");
            MediaManager.init(requireContext().getApplicationContext(), config);
        } catch (IllegalStateException ignored) {}  // already initialised, fine

        MediaManager.get().upload(uri)
                .unsigned("societyhive_gallery")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        if (getContext() == null) return;
                        String imageUrl = (String) resultData.get("secure_url");
                        FirebaseUser user = AuthHelpers.currentUser();
                        if (user == null || imageUrl == null) return;

                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        DocumentReference userRef = db.collection("users").document(user.getUid());
                        userRef.update("profileImageUrl", imageUrl)
                                .addOnSuccessListener(unused -> {
                                    if (!isAdded()) return;
                                    loadAvatar(imageUrl);
                                    Toast.makeText(requireContext(),
                                            "Photo saved!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    if (!isAdded()) return;
                                    Toast.makeText(requireContext(),
                                            "Uploaded but failed to save: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (getContext() == null) return;
                        Toast.makeText(requireContext(),
                                "Upload failed: " + error.getDescription(),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    // glide handles the circle crop, no need to do it manually
    private void loadAvatar(String url) {
        ivProfilePicture.setPadding(0, 0, 0, 0);
        ivProfilePicture.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        ivProfilePicture.setBackground(null);
        Glide.with(this).load(url).circleCrop().into(ivProfilePicture);
    }

    private void renderSocietyBadges(@Nullable List<String> societyIds) {
        societies.clear();
        adapter.updateList(societies);
        if (societyIds == null || societyIds.isEmpty()) return;
        for (String id : societyIds) {

            if (id == null || TextHelpers.isBlank(id)) continue;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference socRef = db.collection("societies").document(id);
            socRef.get().addOnSuccessListener(this::addSocietyIfValid);
        }
    }

    // called once per society doc, adds to list and refreshes adapter each time
    private void addSocietyIfValid(@NonNull DocumentSnapshot doc) {
        if (!isAdded() || !doc.exists()) return;
        String name     = doc.getString("name");
        String colorHex = doc.getString("hexColor");
        String desc     = doc.getString("description");
        String iconUrl  = doc.getString("iconUrl");

        if (name     == null || TextHelpers.isBlank(name))     name     = "Unnamed Society";
        if (colorHex == null || TextHelpers.isBlank(colorHex)) colorHex = "#8D2E3A";
        if (desc     == null || TextHelpers.isBlank(desc))     desc     = "";

        if (iconUrl  == null) iconUrl = "";
        societies.add(new Society(doc.getId(), name, desc, colorHex, iconUrl));
        adapter.updateList(societies);
    }

    // bottom sheet with chat/events/leave options for a society
    private void showManageSheet(@NonNull Society society) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_manage_society, null);
        sheet.setContentView(sheetView);

        TextView tvSheetName = sheetView.findViewById(R.id.tvSheetSocietyName);
        TextView tvSheetDesc = sheetView.findViewById(R.id.tvSheetSocietyDesc);
        View accent          = sheetView.findViewById(R.id.viewSheetAccent);

        if (tvSheetName != null) tvSheetName.setText(society.getName());
        if (tvSheetDesc != null) tvSheetDesc.setText(society.getSubtitle());
        if (accent != null) {
            try { accent.setBackgroundColor(Color.parseColor(society.getColorHex())); }
            catch (IllegalArgumentException ignored) {}
        }

        View rowChat = sheetView.findViewById(R.id.rowOpenChat);
        if (rowChat != null) rowChat.setOnClickListener(v -> {
            sheet.dismiss();
            Bundle b = new Bundle();
            b.putString("societyId", society.getId());
            b.putString("chatTitle", society.getName());
            b.putString("chatColor", society.getColorHex());
            NavHelpers.navigate(this, R.id.chatConversationFragment, b);
        });

        View rowEvents = sheetView.findViewById(R.id.rowViewEvents);
        if (rowEvents != null) rowEvents.setOnClickListener(v -> {
            sheet.dismiss();
            NavHelpers.navigate(this, R.id.eventsFragment);
        });

        View rowLeave = sheetView.findViewById(R.id.rowLeaveSociety);
        if (rowLeave != null) rowLeave.setOnClickListener(v -> {
            sheet.dismiss();
            confirmLeaveSociety(society);
        });

        sheet.show();
    }

    private void confirmLeaveSociety(@NonNull Society society) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Leave " + society.getName() + "?")
                .setMessage("You will no longer see this society's chat or events.")
                .setPositiveButton("Leave", (d, w) -> leaveSociety(society))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void leaveSociety(@NonNull Society society) {
        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null) return;
        // remove society id from the user's list
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference col = db.collection("users");
        DocumentReference ref = col.document(user.getUid());
        ref.update("societyIds", FieldValue.arrayRemove(society.getId()))
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Left " + society.getName(), Toast.LENGTH_SHORT).show();
                    societies.removeIf(s -> s.getId().equals(society.getId()));
                    adapter.updateList(societies);
                });
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
