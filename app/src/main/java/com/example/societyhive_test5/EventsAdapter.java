package com.example.societyhive_test5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Expandable hybrid event cards:
 * - collapsed: quick scan + attending indicator
 * - expanded: description + organiser + attend button + view details button
 */
public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventVH> {

    public interface OnAttendClickListener {
        void onAttendClick(@NonNull Event event, boolean attending);
    }

    public interface OnViewDetailsClickListener {
        void onViewDetailsClick(@NonNull Event event);
    }

    private final List<Event> events;
    private final OnAttendClickListener attendListener;
    private final OnViewDetailsClickListener detailsListener;

    public EventsAdapter(@NonNull List<Event> events,
                         @NonNull OnAttendClickListener attendListener,
                         @NonNull OnViewDetailsClickListener detailsListener) {
        this.events = events;
        this.attendListener = attendListener;
        this.detailsListener = detailsListener;
    }

    @NonNull
    @Override
    public EventVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EventVH holder, int position) {
        holder.bind(events.get(position));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public void updateList(@NonNull List<Event> newEvents) {
        List<Event> copy = new java.util.ArrayList<>(newEvents);
        events.clear();
        events.addAll(copy);
        notifyDataSetChanged();
    }

    class EventVH extends RecyclerView.ViewHolder {
        private final TextView tvEventName;
        private final TextView tvEventDateTime;
        private final TextView tvEventLocation;
        private final TextView tvAttendingIndicator;

        private final LinearLayout layoutExpanded;
        private final TextView tvExpandedDescription;
        private final TextView tvExpandedOrganiser;
        private final MaterialButton btnAttend;
        private final MaterialButton btnViewDetails;

        EventVH(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvEventDateTime = itemView.findViewById(R.id.tvEventDateTime);
            tvEventLocation = itemView.findViewById(R.id.tvEventLocation);
            tvAttendingIndicator = itemView.findViewById(R.id.tvAttendingIndicator);

            layoutExpanded = itemView.findViewById(R.id.layoutExpanded);
            tvExpandedDescription = itemView.findViewById(R.id.tvExpandedDescription);
            tvExpandedOrganiser = itemView.findViewById(R.id.tvExpandedOrganiser);
            btnAttend = itemView.findViewById(R.id.btnAttendEvent);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }

        void bind(@NonNull Event event) {
            tvEventName.setText(event.getName());
            tvEventDateTime.setText(event.getDateTime());
            tvEventLocation.setText(event.getLocation());

            if (event.isAttending()) {
                tvAttendingIndicator.setVisibility(View.VISIBLE);
                tvAttendingIndicator.setText("Attending");
                btnAttend.setText("Attending");
            } else {
                tvAttendingIndicator.setVisibility(View.GONE);
                btnAttend.setText("Attend Event");
            }

            layoutExpanded.setVisibility(event.isExpanded() ? View.VISIBLE : View.GONE);
            tvExpandedDescription.setText(event.getDescription());
            tvExpandedOrganiser.setText(event.getOrganiser());

            itemView.setOnClickListener(v -> {
                event.setExpanded(!event.isExpanded());
                notifyItemChanged(getBindingAdapterPosition());
            });

            btnAttend.setOnClickListener(v -> {
                boolean newState = !event.isAttending();
                event.setAttending(newState);
                notifyItemChanged(getBindingAdapterPosition());
                attendListener.onAttendClick(event, newState);
            });

            btnViewDetails.setOnClickListener(v -> detailsListener.onViewDetailsClick(event));
        }
    }
}
