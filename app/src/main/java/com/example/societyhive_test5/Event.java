package com.example.societyhive_test5;

import androidx.annotation.NonNull;

/**
 * Simple Event model for UI + future Firestore mapping.
 * Keep fields primitive/String-friendly for easy serialization.
 */
public class Event {
    private final String id;
    private final String name;
    private final String info; // e.g. "17:00 • Nov 11, 2026 • NTU City Campus"
    private boolean going;

    public Event(@NonNull String id, @NonNull String name, @NonNull String info, boolean going) {
        this.id = id;
        this.name = name;
        this.info = info;
        this.going = going;
    }

    @NonNull public String getId() { return id; }
    @NonNull public String getName() { return name; }
    @NonNull public String getInfo() { return info; }

    public boolean isGoing() { return going; }
    public void setGoing(boolean going) { this.going = going; }
}
