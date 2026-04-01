package com.example.societyhive_test5;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.PhotoViewHolder> {

    public interface OnDeleteListener {
        void onDelete(GalleryPhoto photo);
    }

    private final Context context;
    private final String currentUid;
    private final boolean isAdmin;
    private final List<GalleryPhoto> photos = new ArrayList<>();
    private OnDeleteListener deleteListener;

    public GalleryAdapter(Context context, String currentUid, boolean isAdmin) {
        this.context = context;
        this.currentUid = currentUid;
        this.isAdmin = isAdmin;
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
        View v = LayoutInflater.from(context).inflate(R.layout.item_gallery_photo, parent, false);
        return new PhotoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        GalleryPhoto photo = photos.get(position);

        Glide.with(context)
                .load(photo.getImageUrl())
                .centerCrop()
                .into(holder.ivPhoto);

        boolean canDelete = isAdmin || currentUid.equals(photo.getUploadedBy());
        holder.btnDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);

        if (canDelete) {
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) deleteListener.onDelete(photo);
            });
        }
    }

    @Override
    public int getItemCount() { return photos.size(); }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivPhoto;
        final ImageButton btnDelete;

        PhotoViewHolder(@NonNull View v) {
            super(v);
            ivPhoto = v.findViewById(R.id.ivPhoto);
            btnDelete = v.findViewById(R.id.btnDeletePhoto);
        }
    }
}
