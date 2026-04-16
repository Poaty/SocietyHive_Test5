package com.example.societyhive_test5;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.PhotoViewHolder> {

    public interface OnDeleteListener {
        void onDelete(GalleryPhoto photo);
    }

    private final String currentUid;
    private final boolean isAdmin;
    private final Map<String, String> societyColorMap;
    private final List<GalleryPhoto> photos = new ArrayList<>();
    private final Map<String, String> nameCache = new HashMap<>();
    private OnDeleteListener deleteListener;

    public GalleryAdapter(String currentUid, boolean isAdmin, Map<String, String> societyColorMap) {
        this.currentUid = currentUid;
        this.isAdmin = isAdmin;
        this.societyColorMap = societyColorMap;
    }

    public void setDeleteListener(OnDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void updatePhotos(List<GalleryPhoto> newPhotos) {
        photos.clear();
        photos.addAll(newPhotos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery_photo, parent, false);
        return new PhotoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        GalleryPhoto photo = photos.get(position);

        Glide.with(holder.ivPhoto.getContext())
                .load(photo.getImageUrl())
                .centerCrop()
                .into(holder.ivPhoto);

        String colorHex = societyColorMap.containsKey(photo.getSocietyId())
                ? societyColorMap.get(photo.getSocietyId())
                : "#8D2E3A";
        try {
            holder.accentStrip.setBackgroundColor(Color.parseColor(colorHex));
        } catch (IllegalArgumentException ignored) {
            holder.accentStrip.setBackgroundColor(Color.parseColor("#8D2E3A"));
        }

        if (photo.getCreatedAt() != null) {
            holder.tvDate.setText(formatTimestamp(photo.getCreatedAt().toDate()));
        } else {
            holder.tvDate.setText("");
        }

        boolean canDelete = isAdmin || currentUid.equals(photo.getUploadedBy());
        holder.btnDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);
        if (canDelete) {
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) deleteListener.onDelete(photo);
            });
        }

        String uid = photo.getUploadedBy();
        if (uid == null || uid.isEmpty()) {
            holder.tvName.setText("Unknown");
            return;
        }

        if (nameCache.containsKey(uid)) {
            holder.tvName.setText(nameCache.get(uid));
        } else {
            holder.tvName.setText("");
            FirebaseFirestore.getInstance()
                    .collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        String name = doc.getString("fullName");
                        if (name == null || name.isEmpty()) name = "Unknown";
                        nameCache.put(uid, name);
                        int idx = photos.indexOf(photo);
                        if (idx >= 0) notifyItemChanged(idx);
                    });
        }
    }

    @Override
    public int getItemCount() { return photos.size(); }

    private String formatTimestamp(Date date) {
        long now = System.currentTimeMillis();
        long diff = now - date.getTime();
        long minutes = diff / 60000;
        long hours   = diff / 3600000;
        long days    = diff / 86400000;

        if (minutes < 1)  return "Just now";
        if (minutes < 60) return minutes + " min ago";
        if (hours < 24)   return hours + " hr" + (hours == 1 ? "" : "s") + " ago";
        if (days < 7) {
            return new SimpleDateFormat("EEE", Locale.getDefault()).format(date);
        }
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        cal.setTime(date);
        int photoYear = cal.get(Calendar.YEAR);
        if (photoYear == currentYear) {
            return new SimpleDateFormat("MMM d", Locale.getDefault()).format(date);
        }
        return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date);
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final View accentStrip;
        final ImageView ivPhoto;
        final ImageView ivAvatar;
        final TextView tvName;
        final TextView tvDate;
        final ImageButton btnDelete;

        PhotoViewHolder(@NonNull View v) {
            super(v);
            card        = v.findViewById(R.id.cardGalleryPhoto);
            accentStrip = v.findViewById(R.id.viewSocietyAccentStrip);
            ivPhoto     = v.findViewById(R.id.ivPhoto);
            ivAvatar    = v.findViewById(R.id.ivUploaderAvatar);
            tvName      = v.findViewById(R.id.tvUploaderName);
            tvDate      = v.findViewById(R.id.tvUploadDate);
            btnDelete   = v.findViewById(R.id.btnDeletePhoto);
        }
    }
}
