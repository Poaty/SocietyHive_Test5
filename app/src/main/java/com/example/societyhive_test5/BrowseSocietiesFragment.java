package com.example.societyhive_test5;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BrowseSocietiesFragment extends Fragment {


    private static class SocietyRow {
        String id, name, iconUrl, colorHex;
        int memberCount;
        boolean requested;

        SocietyRow(String id, String name, String iconUrl, String colorHex, int memberCount) {
            this.id = id;
            this.name = name;
            this.iconUrl = iconUrl != null ? iconUrl : "";
            this.colorHex = colorHex != null ? colorHex : "#8D2E3A";
            this.memberCount = memberCount;
        }
    }

    private final List<SocietyRow> rows = new ArrayList<>();
    private BrowseAdapter adapter;
    private TextView tvEmpty;

    public BrowseSocietiesFragment() {
        super(R.layout.fragment_browse_societies);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvEmpty = view.findViewById(R.id.tvEmpty);

        RecyclerView rv = view.findViewById(R.id.rvBrowseSocieties);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BrowseAdapter();
        rv.setAdapter(adapter);

        loadSocieties();
    }



    // shows all societies the user hasnt joined yet, with pending state if they already requested
    private void loadSocieties() {
        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();


        CollectionReference usersCollection = db.collection("users");
        DocumentReference userDocument = usersCollection.document(user.getUid());
        userDocument.get()
                .addOnSuccessListener(userDoc -> {
                    if (!isAdded()) return;

                    List<String> joined = (List<String>) userDoc.get("societyIds");
                    Set<String> joinedSet = joined != null ? new HashSet<>(joined) : new HashSet<>();


                    CollectionReference joinRequestsCollection = db.collection("joinRequests");
                    joinRequestsCollection
                            .whereEqualTo("userId", user.getUid())
                            .whereEqualTo("status", "pending")
                            .get()
                            .addOnSuccessListener(reqSnap -> {
                                if (!isAdded()) return;

                                Set<String> pendingIds = new HashSet<>();
                                for (QueryDocumentSnapshot req : reqSnap) {
                                    String sid = req.getString("societyId");
                                    if (sid != null) pendingIds.add(sid);
                                }


                                CollectionReference societiesCollection = db.collection("societies");
                                societiesCollection.get()
                                        .addOnSuccessListener(societiesSnap -> {
                                            if (!isAdded()) return;
                                            rows.clear();
                                            for (QueryDocumentSnapshot doc : societiesSnap) {
                                                if (joinedSet.contains(doc.getId())) continue;

                                                String name = doc.getString("name");
                                                String iconUrl = doc.getString("iconUrl");
                                                String colorHex = doc.getString("hexColor");
                                                Long memberCountLong = doc.getLong("memberCount");
                                                int memberCount = memberCountLong != null
                                                        ? memberCountLong.intValue() : 0;

                                                SocietyRow row = new SocietyRow(
                                                        doc.getId(),
                                                        name != null ? name : "Unnamed Society",
                                                        iconUrl,
                                                        colorHex,
                                                        memberCount
                                                );
                                                row.requested = pendingIds.contains(doc.getId());
                                                rows.add(row);
                                            }

                                            adapter.notifyDataSetChanged();
                                            tvEmpty.setVisibility(rows.isEmpty()
                                                    ? View.VISIBLE : View.GONE);
                                        })
                                        .addOnFailureListener(e -> {
                                            if (!isAdded()) return;
                                            Toast.makeText(requireContext(),
                                                    "Failed to load societies",
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            });
                });
    }

    private void submitRequest(SocietyRow row, MaterialButton btn) {
        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null) return;

        // write the join request, admin will see it in user management
        Map<String, Object> data = new HashMap<>();
        data.put("userId",    user.getUid());
        data.put("societyId", row.id);
        data.put("status",    "pending");
        data.put("createdAt", Timestamp.now());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference joinRequestsCollection = db.collection("joinRequests");
        joinRequestsCollection.add(data)
                .addOnSuccessListener(ref -> {
                    if (!isAdded()) return;
                    row.requested = true;
                    btn.setText("Requested");
                    btn.setEnabled(false);
                    Toast.makeText(requireContext(),
                            "Request sent!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to send request", Toast.LENGTH_SHORT).show();
                });
    }





    private class BrowseAdapter extends RecyclerView.Adapter<BrowseAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_browse_society, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.bind(rows.get(position));
        }

        @Override
        public int getItemCount() { return rows.size(); }

        class VH extends RecyclerView.ViewHolder {
            final View accent;
            final ImageView ivIcon;
            final TextView tvName, tvMemberCount;
            final MaterialButton btnRequest;

            VH(@NonNull View v) {
                super(v);
                accent        = v.findViewById(R.id.viewAccent);
                ivIcon        = v.findViewById(R.id.ivSocietyIcon);
                tvName        = v.findViewById(R.id.tvSocietyName);
                tvMemberCount = v.findViewById(R.id.tvMemberCount);
                btnRequest    = v.findViewById(R.id.btnRequestJoin);
            }

            void bind(SocietyRow row) {
                tvName.setText(row.name);
                tvMemberCount.setText(row.memberCount > 0
                        ? row.memberCount + " member" + (row.memberCount == 1 ? "" : "s")
                        : "No members yet");

                try {
                    accent.setBackgroundColor(Color.parseColor(row.colorHex));
                } catch (IllegalArgumentException e) {
                    accent.setBackgroundColor(Color.parseColor("#8D2E3A"));
                }

                if (!row.iconUrl.isEmpty()) {
                    ivIcon.setPadding(0, 0, 0, 0);
                    ivIcon.setBackground(null);
                    Glide.with(ivIcon.getContext())
                            .load(row.iconUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_profile)
                            .into(ivIcon);
                } else {
                    ivIcon.setPadding(4, 4, 4, 4);
                    ivIcon.setBackgroundResource(R.drawable.bg_circle_neutral);
                    ivIcon.setImageResource(R.drawable.ic_profile);
                }

                // disable the button if they already sent a request
                if (row.requested) {
                    btnRequest.setText("Requested");
                    btnRequest.setEnabled(false);
                } else {
                    btnRequest.setText("Request");
                    btnRequest.setEnabled(true);
                    btnRequest.setOnClickListener(v -> submitRequest(row, btnRequest));
                }
            }
        }
    }
}
