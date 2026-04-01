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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class GalleryFragment extends Fragment {

    private boolean isAdmin = false;
    private final List<String> tabSocietyIds = new ArrayList<>();
    private final List<String> tabSocietyNames = new ArrayList<>();
    private String pendingSocietyId;
    private ActivityResultLauncher<String> imagePickerLauncher;

    public GalleryFragment() {
        super(R.layout.fragment_gallery);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        TabLayout tabLayout = view.findViewById(R.id.tabLayoutGallery);
        ViewPager2 viewPager = view.findViewById(R.id.viewPagerGallery);
        FloatingActionButton fabUpload = view.findViewById(R.id.fabUpload);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            if (!isAdded()) return;
            isAdmin = "admin".equals(userDoc.getString("role"));

            if (isAdmin) {
                // Admin sees all societies
                db.collection("societies").get().addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    tabSocietyIds.add("");
                    tabSocietyNames.add("All");
                    for (QueryDocumentSnapshot doc : snap) {
                        tabSocietyIds.add(doc.getId());
                        String name = doc.getString("name");
                        tabSocietyNames.add(name != null ? name : doc.getId());
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

                // Only show "All" tab when in more than one society
                if (ids.size() > 1) {
                    tabSocietyIds.add("");
                    tabSocietyNames.add("All");
                }

                AtomicInteger remaining = new AtomicInteger(ids.size());
                Map<String, String> nameMap = new HashMap<>();

                for (String id : ids) {
                    db.collection("societies").document(id).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            String n = task.getResult().getString("name");
                            if (n != null) nameMap.put(id, n);
                        }
                        if (remaining.decrementAndGet() == 0) {
                            for (String sid : ids) {
                                tabSocietyIds.add(sid);
                                tabSocietyNames.add(nameMap.getOrDefault(sid, sid));
                            }
                            if (isAdded()) setupTabs(tabLayout, viewPager, fabUpload);
                        }
                    });
                }
            }
        });
    }

    private void setupTabs(TabLayout tabLayout, ViewPager2 viewPager, FloatingActionButton fabUpload) {
        GalleryPagerAdapter pagerAdapter = new GalleryPagerAdapter(this, tabSocietyIds);
        viewPager.setAdapter(pagerAdapter);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, pos) -> tab.setText(tabSocietyNames.get(pos))).attach();

        if (isAdmin) {
            // FAB hidden on "All" tab, visible on specific society tabs
            boolean firstTabIsAll = tabSocietyIds.get(0).isEmpty();
            fabUpload.setVisibility(firstTabIsAll ? View.GONE : View.VISIBLE);

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
    }

    private void uploadImage(Uri uri, String societyId) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), "Uploading…", Toast.LENGTH_SHORT).show();

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String filename = System.currentTimeMillis() + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("gallery/" + societyId + "/" + filename);

        ref.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    if (!isAdded()) return;
                    Map<String, Object> photo = new HashMap<>();
                    photo.put("societyId", societyId);
                    photo.put("imageUrl", downloadUri.toString());
                    photo.put("uploadedBy", uid);
                    photo.put("createdAt", Timestamp.now());

                    FirebaseFirestore.getInstance().collection("gallery").add(photo)
                            .addOnSuccessListener(docRef ->
                                    Toast.makeText(requireContext(),
                                            "Photo uploaded!", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
