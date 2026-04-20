package com.example.societyhive_test5;

import androidx.annotation.NonNull;

public class Event {
    private String id;
    private String name;
    private String dateTime;
    private String location;
    private String organiser;
    private String description;
    private String societyId;
    private boolean isPublic;


    private boolean attending;
    private boolean expanded; // ui-only state, not persisted — just tracks whether the card is expanded in the list


    public Event() {}

    public Event(@NonNull String id,
                 @NonNull String name,
                 @NonNull String dateTime,
                 @NonNull String location,
                 @NonNull String organiser,
                 @NonNull String description,
                 @NonNull String societyId,
                 boolean isPublic,
                 boolean attending,
                 boolean expanded) {
        this.id = id;
        this.name = name;
        this.dateTime = dateTime;
        this.location = location;
        this.organiser = organiser;
        this.description = description;
        this.societyId = societyId;
        this.isPublic = isPublic;
        this.attending = attending;
        this.expanded = expanded;
    }

    @NonNull public String getId() { return id != null ? id : ""; }
    @NonNull public String getName() { return name != null ? name : ""; }
    @NonNull public String getDateTime() { return dateTime != null ? dateTime : ""; }
    @NonNull public String getLocation() { return location != null ? location : ""; }
    @NonNull public String getOrganiser() { return organiser != null ? organiser : ""; }
    @NonNull public String getDescription() { return description != null ? description : ""; }
    @NonNull public String getSocietyId() { return societyId != null ? societyId : ""; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }
    public void setLocation(String location) { this.location = location; }
    public void setOrganiser(String organiser) { this.organiser = organiser; }
    public void setDescription(String description) { this.description = description; }
    public void setSocietyId(String societyId) { this.societyId = societyId; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public boolean isAttending() { return attending; }
    public void setAttending(boolean attending) { this.attending = attending; }

    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }
}
