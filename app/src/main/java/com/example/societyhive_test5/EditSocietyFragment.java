package com.example.societyhive_test5;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditSocietyFragment extends Fragment {

    private AutoCompleteTextView actvSociety;
    private TextInputEditText    etName;
    private TextInputEditText    etDescription;
    private TextInputEditText    etColor;
    private View                 colorSwatch;
    private ImageView            ivSocietyIcon;

    private final List<String> societyNames  = new ArrayList<>();
    private final List<String> societyIds    = new ArrayList<>();
    private final List<String> storedNames   = new ArrayList<>();
    private final List<String> storedDescs   = new ArrayList<>();
    private final List<String> storedColors  = new ArrayList<>();
    private final List<String> storedIcons   = new ArrayList<>();

    private int    selectedIndex  = 0;
    private String pendingIconUrl = null;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadIcon(uri);
            });

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
        ivSocietyIcon = view.findViewById(R.id.ivSocietyIcon);

        ivSocietyIcon.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        MaterialButton btnSave = view.findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> attemptSave());

        etColor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                updateSwatch(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadSocieties();
    }

    private void loadSocieties() {
        FirebaseFirestore.getInstance().collection("societies").get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    societyIds.clear(); societyNames.clear();
                    storedNames.clear(); storedDescs.clear();
                    storedColors.clear(); storedIcons.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name  = doc.getString("name");
                        String desc  = doc.getString("description");
                        String color = doc.getString("hexColor");
                        String icon  = doc.getString("iconUrl");
                        societyIds.add(doc.getId());
                        societyNames.add(name  != null ? name  : doc.getId());
                        storedNames .add(name  != null ? name  : "");
                        storedDescs .add(desc  != null ? desc  : "");
                        storedColors.add(color != null ? color : "#8D2E3A");
                        storedIcons .add(icon  != null ? icon  : "");
                    }
                    setupDropdown();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to load societies.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupDropdown() {
        if (societyIds.isEmpty()) {
            Toast.makeText(requireContext(), "No societies found.", Toast.LENGTH_LONG).show();
            return;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, societyNames);
        actvSociety.setAdapter(adapter);
        actvSociety.setText(societyNames.get(0), false);
        fillFields(0);

        actvSociety.setOnItemClickListener((parent, v, position, id) -> {
            selectedIndex = position;
            pendingIconUrl = null; // reset pending icon when switching society
            fillFields(position);
        });
    }

    private void fillFields(int index) {
        etName.setText(storedNames.get(index));
        etDescription.setText(storedDescs.get(index));
        String color = storedColors.get(index);
        etColor.setText(color);
        updateSwatch(color);

        String iconUrl = storedIcons.get(index);
        if (iconUrl != null && !iconUrl.isEmpty()) {
            ivSocietyIcon.setPadding(0, 0, 0, 0);
            ivSocietyIcon.setBackground(null);
            ivSocietyIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            Glide.with(this).load(iconUrl).circleCrop().into(ivSocietyIcon);
        } else {
            ivSocietyIcon.setPadding(14, 14, 14, 14);
            ivSocietyIcon.setBackgroundResource(R.drawable.bg_circle_neutral);
            ivSocietyIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
            ivSocietyIcon.setImageResource(R.drawable.ic_gallery);
        }
    }

    private void updateSwatch(@NonNull String hex) {
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
                        Glide.with(EditSocietyFragment.this)
                                .load(pendingIconUrl).circleCrop().into(ivSocietyIcon);
                        Toast.makeText(requireContext(),
                                "Icon ready \u2014 tap Save to apply", Toast.LENGTH_SHORT).show();
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

    private void attemptSave() {
        if (societyIds.isEmpty()) {
            Toast.makeText(requireContext(), "No society selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        String name  = text(etName);
        String desc  = text(etDescription);
        String color = text(etColor);

        if (name.isEmpty()) { etName.setError("Please enter a society name"); return; }
        if (!color.startsWith("#") || color.length() < 4) {
            etColor.setError("Enter a valid hex colour (e.g. #8D2E3A)"); return;
        }
        try { Color.parseColor(color); }
        catch (IllegalArgumentException e) { etColor.setError("Invalid colour \u2014 use #RRGGBB"); return; }

        String societyId = societyIds.get(selectedIndex);
        Map<String, Object> updates = new HashMap<>();
        updates.put("name",        name);
        updates.put("description", desc);
        updates.put("hexColor",    color);
        if (pendingIconUrl != null) updates.put("iconUrl", pendingIconUrl);

        FirebaseFirestore.getInstance().collection("societies").document(societyId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Society updated!", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).navigateUp();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @NonNull
    private String text(@Nullable TextInputEditText et) {
        return (et != null && et.getText() != null) ? et.getText().toString().trim() : "";
    }
}
