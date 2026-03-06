package com.example.societyhive_test5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

/**
 * Adapter using ListAdapter + DiffUtil (ready for Firebase updates).
 */
public class EventsAdapter extends ListAdapter<Event, EventsAdapter.EventVH> {

    public interface OnGoingChangedListener {
        void onGoingChanged(@NonNull Event event, boolean going);
    }

    private final OnGoingChangedListener listener;

    public EventsAdapter(OnGoingChangedListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public interface OnEventClickListener {
        void onEventClick(@NonNull Event event);
    }

    private static final DiffUtil.ItemCallback<Event> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Event>() {
                @Override
                public boolean areItemsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
                    return oldItem.getName().equals(newItem.getName())
                            && oldItem.getInfo().equals(newItem.getInfo())
                            && oldItem.isGoing() == newItem.isGoing();
                }
            };

    @NonNull
    @Override
    public EventVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EventVH holder, int position) {
        Event event = getItem(position);
        holder.bind(event, listener);
    }

    static class EventVH extends RecyclerView.ViewHolder {
        private final MaterialCheckBox cbGoing;
        private final TextView tvName;
        private final TextView tvInfo;

        EventVH(@NonNull View itemView) {
            super(itemView);
            cbGoing = itemView.findViewById(R.id.cbGoing);
            tvName = itemView.findViewById(R.id.tvEventName);
            tvInfo = itemView.findViewById(R.id.tvEventInfo);
        }

        void bind(@NonNull Event event, OnGoingChangedListener listener) {
            tvName.setText(event.getName());
            tvInfo.setText(event.getInfo());

            // Prevent recycler reuse triggering listener unexpectedly
            cbGoing.setOnCheckedChangeListener(null);
            cbGoing.setChecked(event.isGoing());

            cbGoing.setOnCheckedChangeListener((buttonView, isChecked) -> {
                event.setGoing(isChecked);
                if (listener != null) {
                    listener.onGoingChanged(event, isChecked);
                }
            });
        }
    }
}
