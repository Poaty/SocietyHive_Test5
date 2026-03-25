package com.example.societyhive_test5;

import com.google.firebase.Timestamp;

public class Announcement {

    private String id;
    private String title;
    private String content;
    private String societyId;
    private String societyName; // UI-only, not stored in Firestore
    private String createdBy;
    private Timestamp createdAt;

    public Announcement() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSocietyId() { return societyId; }
    public void setSocietyId(String societyId) { this.societyId = societyId; }

    public String getSocietyName() { return societyName; }
    public void setSocietyName(String societyName) { this.societyName = societyName; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
