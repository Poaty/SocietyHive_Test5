package com.example.societyhive_test5;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;

/**
 * A single chat message stored in Firestore under:
 *   societies/{societyId}/messages/{messageId}
 *
 * Fields:
 *   text       (String)
 *   senderId   (String)  — Firebase Auth UID
 *   senderName (String)  — display name stored at send time
 *   timestamp  (Timestamp)
 */
public class Message {
    private String id;          // Firestore document ID (not stored as a field)
    private String text;
    private String senderId;
    private String senderName;
    private com.google.firebase.Timestamp timestamp;

    // Local-only flag: is this message from the currently signed-in user?
    private boolean sentByMe;

    /** Required for Firestore toObject(). */
    public Message() {}

    /** Constructor for sending a new message. */
    public Message(@NonNull String text,
                   @NonNull String senderId,
                   @NonNull String senderName) {
        this.text = text;
        this.senderId = senderId;
        this.senderName = senderName;
        this.timestamp = com.google.firebase.Timestamp.now();
    }

    // Getters / setters used by Firestore serialisation
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

    // Local UI flag — not stored in Firestore
    public boolean isSentByMe() { return sentByMe; }
    public void setSentByMe(boolean sentByMe) { this.sentByMe = sentByMe; }
}
