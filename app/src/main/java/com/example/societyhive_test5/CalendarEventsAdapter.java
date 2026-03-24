package com.example.societyhive_test5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarEventsAdapter extends RecyclerView.Adapter<CalendarEventsAdapter.ViewHolder> {

    private final List<Event> events = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_event, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);
        holder.tvName.setText(event.getName());

        // Format subtitle: "17:00 - Nov 19 - 2025"
        String subtitle = formatSubtitle(event.getDateTime());
        holder.tvDateTime.setText(subtitle);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public void updateList(@NonNull List<Event> newList) {
        events.clear();
        events.addAll(newList);
        notifyDataSetChanged();
    }

    private String formatSubtitle(String dateTime) {
        if (dateTime == null || !dateTime.contains(" • ")) return dateTime != null ? dateTime : "";
        try {
            String[] parts = dateTime.split(" • ");
            String datePart = parts[0].trim();   // "19-Nov-2025"
            String timePart = parts[1].trim();   // "17:00"

            Date d = new SimpleDateFormat("dd-MMM-yyyy", Locale.UK).parse(datePart);
            if (d == null) return dateTime;

            String formatted = new SimpleDateFormat("MMM d - yyyy", Locale.UK).format(d);
            return timePart + " - " + formatted;
        } catch (Exception e) {
            return dateTime;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvDateTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCalEventName);
            tvDateTime = itemView.findViewById(R.id.tvCalEventDateTime);
        }
    }
}
