package com.example.societyhive_test5;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;

public class Message {
    private String id;
    private String text;
    private String senderId;
    private String senderName;
    private com.google.firebase.Timestamp timestamp;


    private boolean sentByMe;
    private String senderPhotoUrl;


    public Message() {}


    public Message(@NonNull String text,
                   @NonNull String senderId,
                   @NonNull String senderName) {
        this.text = text;
        this.senderId = senderId;
        this.senderName = senderName;
        this.timestamp = com.google.firebase.Timestamp.now();
    }


    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getText() { return text != null ? text : ""; }
    public void setText(String text) { this.text = text; }

    public String getSenderId() { return senderId != null ? senderId : ""; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName != null ? senderName : ""; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public com.google.firebase.Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(com.google.firebase.Timestamp timestamp) { this.timestamp = timestamp; }

    public boolean isSentByMe() { return sentByMe; }
    public void setSentByMe(boolean sentByMe) { this.sentByMe = sentByMe; }

    public String getSenderPhotoUrl() { return senderPhotoUrl != null ? senderPhotoUrl : ""; }
    public void setSenderPhotoUrl(String url) { this.senderPhotoUrl = url; }
}
