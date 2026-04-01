package com.example.societyhive_test5;

import androidx.annotation.NonNull;

public class Chat {
    private final String id;
    private final String title;
    private final String lastMessage;
    private final String time;
    private final String societyColor;
    private String iconUrl;

    public Chat(@NonNull String id,
                @NonNull String title,
                @NonNull String lastMessage,
                @NonNull String time,
                @NonNull String societyColor) {
        this.id = id;
        this.title = title;
        this.lastMessage = lastMessage;
        this.time = time;
        this.societyColor = societyColor;
        this.iconUrl = "";
    }

    public Chat(@NonNull String id,
                @NonNull String title,
                @NonNull String lastMessage,
                @NonNull String time,
                @NonNull String societyColor,
                @NonNull String iconUrl) {
        this.id = id;
        this.title = title;
        this.lastMessage = lastMessage;
        this.time = time;
        this.societyColor = societyColor;
        this.iconUrl = iconUrl;
    }

    @NonNull public String getId() { return id; }
    @NonNull public String getTitle() { return title; }
    @NonNull public String getLastMessage() { return lastMessage; }
    @NonNull public String getTime() { return time; }
    @NonNull public String getSocietyColor() { return societyColor; }
    @NonNull public String getIconUrl() { return iconUrl; }
}
