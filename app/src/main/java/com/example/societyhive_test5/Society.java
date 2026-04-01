package com.example.societyhive_test5;

import androidx.annotation.NonNull;

public class Society {
    private final String id;
    private final String name;
    private final String subtitle;
    private final String colorHex;
    private String iconUrl;

    public Society(@NonNull String id,
                   @NonNull String name,
                   @NonNull String subtitle,
                   @NonNull String colorHex) {
        this.id = id;
        this.name = name;
        this.subtitle = subtitle;
        this.colorHex = colorHex;
        this.iconUrl = "";
    }

    public Society(@NonNull String id,
                   @NonNull String name,
                   @NonNull String subtitle,
                   @NonNull String colorHex,
                   @NonNull String iconUrl) {
        this.id = id;
        this.name = name;
        this.subtitle = subtitle;
        this.colorHex = colorHex;
        this.iconUrl = iconUrl;
    }

    @NonNull public String getId() { return id; }
    @NonNull public String getName() { return name; }
    @NonNull public String getSubtitle() { return subtitle; }
    @NonNull public String getColorHex() { return colorHex; }
    @NonNull public String getIconUrl() { return iconUrl; }
}
