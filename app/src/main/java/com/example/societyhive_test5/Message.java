package com.example.societyhive_test5;

import androidx.annotation.NonNull;

public class Message {
    private final String id;
    private final String text;
    private final boolean sentByMe;

    public Message(@NonNull String id, @NonNull String text, boolean sentByMe) {
        this.id = id;
        this.text = text;
        this.sentByMe = sentByMe;
    }

    @NonNull public String getId() { return id; }
    @NonNull public String getText() { return text; }
    public boolean isSentByMe() { return sentByMe; }
}
