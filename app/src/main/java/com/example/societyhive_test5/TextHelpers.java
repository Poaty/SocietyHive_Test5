package com.example.societyhive_test5;

import android.widget.TextView;

public final class TextHelpers {

    private TextHelpers() {}

    public static String text(TextView view) {
        CharSequence value = view.getText();
        return value != null ? value.toString() : "";
    }

    public static String trimmed(TextView view) {
        String value = text(view);
        return value.trim();
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
