package com.example.societyhive_test5;

import androidx.annotation.NonNull;

public class Society {
    private final String id;
    private final String name;
    private final String subtitle;
    private final String colorHex;

    public Society(@NonNull String id,
                   @NonNull String name,
                   @NonNull String subtitle,
                   @NonNull String colorHex) {
        this.id = id;
        this.name = name;
        this.subtitle = subtitle;
        this.colorHex = colorHex;
    }

    @NonNull public String getId() { return id; }
    @NonNull public String getName() { return name; }
    @NonNull public String getSubtitle() { return subtitle; }
    @NonNull public String getColorHex() { return colorHex; }
}
