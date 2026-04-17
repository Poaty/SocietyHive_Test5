package com.example.societyhive_test5;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class GalleryFragment extends Fragment {

    private static final String CLOUD_NAME     = "dybgordqu";
    private static final String UPLOAD_PRESET  = "societyhive_gallery";

    private boolean isAdmin = false;
    private final List<String> tabSocietyIds     = new ArrayList<>();
    private final List<String> tabSocietyNames   = new ArrayList<>();
    private final List<String> tabSocietyColors  = new ArrayList<>();
    private String pendingSocietyId;
    private ActivityResultLauncher<String> imagePickerLauncher;

    public GalleryFragment() {
        super(R.layout.fragment_gallery);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init cloudinary - wrapped in try because it crashes if called twice
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", CLOUD_NAME);
            MediaManager.init(requireContext().getApplicationContext(), config);
        } catch (IllegalStateException ignored) {}

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && pendingSocietyId != null) {
                        uploadImage(uri, pendingSocietyId);
                    }
                });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TabLayout tabLayout  = view.findViewById(R.id.tabLayoutGallery);
        ViewPager2 viewPager = view.findViewById(R.id.viewPagerGallery);
        FloatingActionButton fabUpload = view.findViewById(R.id.fabUpload);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // admin sees all societies, normal users only see what theyre in
        CollectionReference usersCollection = db.collection("users");
        DocumentReference userDocument = usersCollection.document(uid);
        userDocument.get().addOnSuccessListener(userDoc -> {
            if (!isAdded()) return;
            isAdmin = "admin".equals(userDoc.getString("role"));

            if (isAdmin) {
                CollectionReference societiesCollection = db.collection("societies");
                societiesCollection.get().addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    tabSocietyIds.add("");
                    tabSocietyNames.add("All");
                    tabSocietyColors.add("");
                    for (QueryDocumentSnapshot doc : snap) {
                        tabSocietyIds.add(doc.getId());
                        String name = doc.getString("name");
                        tabSocietyNames.add(name != null ? name : doc.getId());
                        String color = doc.getString("hexColor");
                        tabSocietyColors.add(color != null ? color : "#8D2E3A");
                    }
                    setupTabs(tabLayout, viewPager, fabUpload);
                });
            } else {
                List<?> rawIds = (List<?>) userDoc.get("societyIds");
                List<String> ids = new ArrayList<>();
                if (rawIds != null) {
                    for (Object o : rawIds) {
                        if (o instanceof String) ids.add((String) o);
                    }
                }

                if (ids.isEmpty()) {
                    view.findViewById(R.id.tvEmptyGallery).setVisibility(View.VISIBLE);
                    tabLayout.setVisibility(View.GONE);
                    viewPager.setVisibility(View.GONE);
                    return;
                }

                if (ids.size() > 1) {
                    tabSocietyIds.add("");
                    tabSocietyNames.add("All");
                    tabSocietyColors.add("");
                }

                AtomicInteger remaining = new AtomicInteger(ids.size());
                Map<String, String> nameMap = new HashMap<>();
                Map<String, String> colorMap = new HashMap<>();

                for (String id : ids) {
                    CollectionReference societiesCollection = db.collection("societies");
                    DocumentReference societyDocument = societiesCollection.document(id);
                    societyDocument.get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            String n = task.getResult().getString("name");
                            if (n != null) nameMap.put(id, n);
                            String c = task.getResult().getString("hexColor");
                            colorMap.put(id, c != null ? c : "#8D2E3A");
                        }
                        if (remaining.decrementAndGet() == 0) {
                            for (String sid : ids) {
                                tabSocietyIds.add(sid);
                                tabSocietyNames.add(nameMap.getOrDefault(sid, sid));
                                tabSocietyColors.add(colorMap.getOrDefault(sid, "#8D2E3A"));
                            }
                            if (isAdded()) setupTabs(tabLayout, viewPager, fabUpload);
                        }
                    });
                }
            }
        });
    }

    private void setupTabs(TabLayout tabLayout, ViewPager2 viewPager, FloatingActionButton fabUpload) {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        GalleryPagerAdapter pagerAdapter =
                new GalleryPagerAdapter(this, tabSocietyIds, tabSocietyColors, currentUid, isAdmin);
        viewPager.setAdapter(pagerAdapter);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, pos) -> tab.setText(tabSocietyNames.get(pos))).attach();

        boolean firstIsAll = tabSocietyIds.get(0).isEmpty();
        fabUpload.setVisibility(firstIsAll ? View.GONE : View.VISIBLE);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String sid = tabSocietyIds.get(tab.getPosition());
                fabUpload.setVisibility(sid.isEmpty() ? View.GONE : View.VISIBLE);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        fabUpload.setOnClickListener(v -> {
            int pos = viewPager.getCurrentItem();
            String sid = tabSocietyIds.get(pos);
            if (!sid.isEmpty()) {
                pendingSocietyId = sid;
                imagePickerLauncher.launch("image/*");
            }
        });
    }

    // upload to cloudinary then store the url + metadata in firestore
    private void uploadImage(Uri uri, String societyId) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), "Uploading…", Toast.LENGTH_SHORT).show();

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        MediaManager.get().upload(uri)
                .unsigned(UPLOAD_PRESET)
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        if (!isAdded()) return;
                        String imageUrl = (String) resultData.get("secure_url");

                        Map<String, Object> photo = new HashMap<>();
                        photo.put("societyId", societyId);
                        photo.put("imageUrl", imageUrl);
                        photo.put("uploadedBy", uid);
                        photo.put("createdAt", Timestamp.now());

                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        CollectionReference galleryCollection = db.collection("gallery");
                        galleryCollection.add(photo)
                                .addOnSuccessListener(ref ->
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(requireContext(),
                                                        "Photo uploaded!", Toast.LENGTH_SHORT).show()));
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        "Upload failed: " + error.getDescription(),
                                        Toast.LENGTH_LONG).show());
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }
}
