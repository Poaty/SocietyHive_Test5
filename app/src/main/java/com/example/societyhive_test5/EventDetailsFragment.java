package com.example.societyhive_test5;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.transition.MaterialFadeThrough;

public class EventDetailsFragment extends Fragment {

    public EventDetailsFragment() {
        super(R.layout.fragment_event_details);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialFadeThrough());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String eventId = null;
        Bundle args = getArguments();
        if (args != null) {
            eventId = args.getString("eventId");
        }

        TextView tvDebug = view.findViewById(R.id.tvEventId);
        tvDebug.setText("Event ID: " + (eventId == null ? "null" : eventId));
    }
}