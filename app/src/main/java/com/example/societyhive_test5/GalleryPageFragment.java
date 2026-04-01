package com.example.societyhive_test5;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GalleryPageFragment extends Fragment {

    private static final String ARG_SOCIETY_ID  = "societyId";
    private static final String ARG_CURRENT_UID = "currentUid";
    private static final String ARG_IS_ADMIN    = "isAdmin";

    private String societyId;
    private String currentUid;
    private boolean isAdmin;

    private GalleryAdapter adapter;
    private final List<GalleryPhoto> photos = new ArrayList<>();
    private ListenerRegistration snapshotListener;

    public static GalleryPageFragment newInstance(String societyId, String currentUid, boolean isAdmin) {
        GalleryPageFragment f = new GalleryPageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SOCIETY_ID, societyId);
        args.putString(ARG_CURRENT_UID, currentUid);
        args.putBoolean(ARG_IS_ADMIN, isAdmin);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            societyId  = getArguments().getString(ARG_SOCIETY_ID, "");
            currentUid = getArguments().getString(ARG_CURRENT_UID, "");
            isAdmin    = getArguments().getBoolean(ARG_IS_ADMIN, false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rvGallery);
        TextView tvEmpty = view.findViewById(R.id.tvEmptyGallery);

        adapter = new GalleryAdapter(requireContext(), currentUid, isAdmin);
        adapter.setDeleteListener(photo ->
                FirebaseFirestore.getInstance()
                        .collection("gallery")
                        .document(photo.getId())
                        .delete());

        rv.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        rv.setAdapter(adapter);

        listenPhotos(tvEmpty);
    }

    private void listenPhotos(TextView tvEmpty) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query query = societyId.isEmpty()
                ? db.collection("gallery")
                : db.collection("gallery").whereEqualTo("societyId", societyId);

        snapshotListener = query.addSnapshotListener((snap, e) -> {
            if (e != null || snap == null || !isAdded()) return;
            photos.clear();
            for (QueryDocumentSnapshot doc : snap) {
                GalleryPhoto photo = doc.toObject(GalleryPhoto.class);
                photo.setId(doc.getId());
                photos.add(photo);
            }
            photos.sort((a, b) -> {
                if (a.getCreatedAt() == null) return 1;
                if (b.getCreatedAt() == null) return -1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });
            adapter.updatePhotos(photos);
            tvEmpty.setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (snapshotListener != null) snapshotListener.remove();
    }
}
