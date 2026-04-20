package com.example.societyhive_test5;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateSocietyFragment extends Fragment {

    private TextInputEditText etName;
    private TextInputEditText etDescription;
    private TextInputEditText etColor;
    private View              colorSwatch;
    private ImageView         ivSocietyIcon;

    private String pendingIconUrl = null;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadIcon(uri);
            });

    public CreateSocietyFragment() {
        super(R.layout.fragment_create_society);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etName        = view.findViewById(R.id.etName);
        etDescription = view.findViewById(R.id.etDescription);
        etColor       = view.findViewById(R.id.etColor);
        colorSwatch   = view.findViewById(R.id.colorSwatch);
        ivSocietyIcon = view.findViewById(R.id.ivSocietyIcon);


        etColor.setText("#8D2E3A");

        ivSocietyIcon.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        MaterialButton btnCreate = view.findViewById(R.id.btnCreate);
        btnCreate.setOnClickListener(v -> submitNewSociety());

        etColor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                updateSwatch(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void updateSwatch(String hex) {
        try { colorSwatch.setBackgroundColor(Color.parseColor(hex)); }
        catch (IllegalArgumentException ignored) {}
    }

    private void uploadIcon(Uri uri) {
        Toast.makeText(requireContext(), "Uploading icon\u2026", Toast.LENGTH_SHORT).show();
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dybgordqu");
            MediaManager.init(requireContext().getApplicationContext(), config);
        } catch (IllegalStateException ignored) {}

        MediaManager.get().upload(uri)
                .unsigned("societyhive_gallery")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        if (!isAdded()) return;
                        pendingIconUrl = (String) resultData.get("secure_url");
                        ivSocietyIcon.setPadding(0, 0, 0, 0);
                        ivSocietyIcon.setBackground(null);
                        ivSocietyIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                        Glide.with(CreateSocietyFragment.this)
                                .load(pendingIconUrl).circleCrop().into(ivSocietyIcon);
                        Toast.makeText(requireActivity(),
                                "Icon ready \u2014 tap Create to apply", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                "Icon upload failed: " + error.getDescription(),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void submitNewSociety() {
        String name  = text(etName);
        String desc  = text(etDescription);
        // hex validation before hitting firestore — bad colours would cause crash in adapters later
        String color = text(etColor);

        if (name.isEmpty()) {
            etName.setError("Please enter a society name");
            return;
        }
        if (!color.startsWith("#") || color.length() < 4) {
            etColor.setError("Enter a valid hex colour (e.g. #8D2E3A)");
            return;
        }
        try { Color.parseColor(color); }
        catch (IllegalArgumentException e) {
            etColor.setError("Invalid colour \u2014 use #RRGGBB");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name",        name);
        data.put("description", desc);
        data.put("hexColor",    color);
        data.put("createdAt",   Timestamp.now());
        if (pendingIconUrl != null) data.put("iconUrl", pendingIconUrl);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference societiesCollection = db.collection("societies");
        societiesCollection.add(data)
                .addOnSuccessListener(docRef -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Society created \u2014 remember to assign a society admin via User Management",
                            Toast.LENGTH_LONG).show();
                    NavHelpers.navigateUp(this);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "couldn't create society: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    @NonNull
    private String text(@Nullable TextInputEditText et) {
        return (et != null && et.getText() != null) ? TextHelpers.trimmed(et) : "";
    }
}
