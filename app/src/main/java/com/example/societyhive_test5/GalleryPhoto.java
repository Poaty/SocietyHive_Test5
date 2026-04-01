package com.example.societyhive_test5;

import com.google.firebase.Timestamp;

public class GalleryPhoto {
    private String id;
    private String societyId;
    private String imageUrl;
    private String uploadedBy;
    private Timestamp createdAt;

    public GalleryPhoto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSocietyId() { return societyId; }
    public void setSocietyId(String societyId) { this.societyId = societyId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
