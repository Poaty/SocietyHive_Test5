package com.example.societyhive_test5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnnouncementsAdapter extends RecyclerView.Adapter<AnnouncementsAdapter.ViewHolder> {

    private final List<Announcement> announcements = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_announcement, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Announcement a = announcements.get(position);

        String societyName = a.getSocietyName();
        if (societyName != null && !societyName.isEmpty()) {
            holder.tvSocietyName.setVisibility(View.VISIBLE);
            holder.tvSocietyName.setText(societyName);
        } else {
            holder.tvSocietyName.setVisibility(View.GONE);
        }

        holder.tvTitle.setText(a.getTitle());
        holder.tvContent.setText(a.getContent());

        if (a.getCreatedAt() != null) {
            String date = new SimpleDateFormat("d MMM yyyy", Locale.UK)
                    .format(a.getCreatedAt().toDate());
            holder.tvTimestamp.setText(date);
        } else {
            holder.tvTimestamp.setText("");
        }
    }

    @Override
    public int getItemCount() { return announcements.size(); }

    public void updateList(@NonNull List<Announcement> newList) {
        announcements.clear();
        announcements.addAll(newList);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvSocietyName;
        final TextView tvTitle;
        final TextView tvContent;
        final TextView tvTimestamp;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSocietyName = itemView.findViewById(R.id.tvSocietyName);
            tvTitle       = itemView.findViewById(R.id.tvAnnouncementTitle);
            tvContent     = itemView.findViewById(R.id.tvAnnouncementContent);
            tvTimestamp   = itemView.findViewById(R.id.tvAnnouncementTimestamp);
        }
    }
}
