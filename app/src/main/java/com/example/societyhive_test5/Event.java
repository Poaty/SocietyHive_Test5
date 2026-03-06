package com.example.societyhive_test5;

import androidx.annotation.NonNull;

/**
 * Event model for expandable hybrid cards.
 * Later this can map cleanly to Firestore documents.
 */
public class Event {
    private final String id;
    private final String name;
    private final String dateTime;
    private final String location;
    private final String organiser;
    private final String description;

    private boolean attending;
    private boolean expanded;

    public Event(@NonNull String id,
                 @NonNull String name,
                 @NonNull String dateTime,
                 @NonNull String location,
                 @NonNull String organiser,
                 @NonNull String description,
                 boolean attending,
                 boolean expanded) {
        this.id = id;
        this.name = name;
        this.dateTime = dateTime;
        this.location = location;
        this.organiser = organiser;
        this.description = description;
        this.attending = attending;
        this.expanded = expanded;
    }

    @NonNull public String getId() { return id; }
    @NonNull public String getName() { return name; }
    @NonNull public String getDateTime() { return dateTime; }
    @NonNull public String getLocation() { return location; }
    @NonNull public String getOrganiser() { return organiser; }
    @NonNull public String getDescription() { return description; }

    public boolean isAttending() { return attending; }
    public void setAttending(boolean attending) { this.attending = attending; }

    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }
}
