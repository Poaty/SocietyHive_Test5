package com.example.societyhive_test5;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

// derived palette for a single chat. takes the society's hex colour and builds
// the few shades the chat UI needs — a faint tint for received bubbles, the
// primary for sent bubbles + avatar ring, and a text colour for sent bubbles
// picked by luminance so dark primaries get white text and light ones get dark.
// keeping this in a small data class so the adapter doesn't have to re-derive
// anything on every bind — the fragment builds it once and hands it over.
final class ChatTheme {

    @ColorInt final int primary;
    @ColorInt final int receivedBg;
    @ColorInt final int receivedStroke;
    @ColorInt final int sentStroke;
    @ColorInt final int sentText;
    @ColorInt final int avatarFill;

    private ChatTheme(@ColorInt int primary) {
        this.primary        = primary;
        // 12% mixes a tint that's clearly the society colour without drowning the text
        this.receivedBg     = ColorUtils.blendARGB(Color.WHITE, primary, 0.12f);
        this.receivedStroke = ColorUtils.blendARGB(Color.WHITE, primary, 0.30f);
        // slightly darker edge on the sent bubble so the shape reads against light backgrounds
        this.sentStroke     = ColorUtils.blendARGB(primary, Color.BLACK, 0.18f);
        this.avatarFill     = ColorUtils.blendARGB(Color.WHITE, primary, 0.06f);
        // 0.55 threshold leaves a bit of margin either side of the 0.5 midpoint —
        // colours right on the edge are rare and this keeps borderline cases readable
        this.sentText = ColorUtils.calculateLuminance(primary) > 0.55
                ? 0xFF1E1E1E
                : Color.WHITE;
    }

    @NonNull
    static ChatTheme from(@NonNull String hex) {
        int c;
        try {
            c = Color.parseColor(hex);
        } catch (IllegalArgumentException e) {
            // fallback matches the app's default crimson so a bad hex doesn't blank the UI
            c = Color.parseColor("#8D2E3A");
        }
        return new ChatTheme(c);
    }
}
