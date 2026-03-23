package com.example.societyhive_test5;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Preferences screen.
 *
 * Currently implements:
 *   - Colour scheme selection (5 options, saved to SharedPreferences)
 *   - Compact events toggle (saved to SharedPreferences, read by EventsFragment later)
 *
 * The chosen colour hex is stored under the key "pref_accent_color" in the
 * "societyhive_prefs" SharedPreferences file. You can read it anywhere with:
 *
 *   String hex = context.getSharedPreferences("societyhive_prefs", Context.MODE_PRIVATE)
 *                       .getString("pref_accent_color", "#8D2E3A");
 *
 * Applying it app-wide at runtime requires a theme change + Activity restart,
 * which is noted in the UI. For a lightweight version, the toolbar color and
 * button tint can be updated programmatically in MainActivity after this is saved.
 */
public class PreferencesFragment extends Fragment {

    private static final String PREFS_NAME = "societyhive_prefs";
    private static final String KEY_ACCENT = "pref_accent_color";
    private static final String KEY_COMPACT = "pref_compact_events";

    // The 5 colour options
    private static final String[][] SCHEMES = {
            {"crimson",  "#8D2E3A", "Crimson"},
            {"blue",     "#1A3A6B", "Midnight"},
            {"green",    "#1E5C2E", "Forest"},
            {"purple",   "#4A2372", "Slate"},
            {"charcoal", "#2E2E2E", "Charcoal"},
    };

    public PreferencesFragment() {
        super(R.layout.fragment_preferences);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String savedAccent = prefs.getString(KEY_ACCENT, "#8D2E3A");

        // Make swatch circles round via code (avoids needing 5 separate drawables)
        makeCircle(view, R.id.circCrimson,  "#8D2E3A");
        makeCircle(view, R.id.circBlue,     "#1A3A6B");
        makeCircle(view, R.id.circGreen,    "#1E5C2E");
        makeCircle(view, R.id.circPurple,   "#4A2372");
        makeCircle(view, R.id.circCharcoal, "#2E2E2E");

        // Wire up each swatch click
        int[] swatchIds = {
                R.id.swatchCrimson, R.id.swatchBlue, R.id.swatchGreen,
                R.id.swatchPurple, R.id.swatchCharcoal
        };

        for (int i = 0; i < SCHEMES.length; i++) {
            final String[] scheme = SCHEMES[i];
            View swatch = view.findViewById(swatchIds[i]);
            if (swatch != null) {
                swatch.setOnClickListener(v -> selectScheme(view, prefs, scheme[1], scheme[2]));
            }
        }

        // Restore saved selection tick
        refreshTicks(view, savedAccent);

        // Compact events toggle
        com.google.android.material.switchmaterial.SwitchMaterial swCompact =
                view.findViewById(R.id.switchCompactEvents);
        if (swCompact != null) {
            swCompact.setChecked(prefs.getBoolean(KEY_COMPACT, false));
            swCompact.setOnCheckedChangeListener((btn, checked) ->
                    prefs.edit().putBoolean(KEY_COMPACT, checked).apply());
        }
    }

    // -------------------------------------------------------------------------

    private void selectScheme(@NonNull View root,
                               @NonNull SharedPreferences prefs,
                               @NonNull String hex,
                               @NonNull String name) {
        prefs.edit().putString(KEY_ACCENT, hex).apply();
        refreshTicks(root, hex);

        TextView tvSelected = root.findViewById(R.id.tvSelectedScheme);
        if (tvSelected != null) tvSelected.setText("Selected: " + name);

        Toast.makeText(requireContext(),
                name + " selected — restart the app to apply",
                Toast.LENGTH_SHORT).show();
    }

    private void refreshTicks(@NonNull View root, @NonNull String selectedHex) {
        int[] tickIds = {
                R.id.tickCrimson, R.id.tickBlue, R.id.tickGreen,
                R.id.tickPurple, R.id.tickCharcoal
        };

        for (int i = 0; i < SCHEMES.length; i++) {
            View tick = root.findViewById(tickIds[i]);
            if (tick != null) {
                tick.setVisibility(
                        SCHEMES[i][1].equalsIgnoreCase(selectedHex)
                                ? View.VISIBLE : View.INVISIBLE);
            }
        }

        // Update the label
        TextView tvSelected = root.findViewById(R.id.tvSelectedScheme);
        if (tvSelected != null) {
            for (String[] scheme : SCHEMES) {
                if (scheme[1].equalsIgnoreCase(selectedHex)) {
                    tvSelected.setText("Selected: " + scheme[2]);
                    break;
                }
            }
        }
    }

    /** Turns the plain View swatch into a filled circle at runtime. */
    private void makeCircle(@NonNull View root, int viewId, @NonNull String hex) {
        View v = root.findViewById(viewId);
        if (v == null) return;
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        try {
            circle.setColor(android.graphics.Color.parseColor(hex));
        } catch (IllegalArgumentException e) {
            circle.setColor(android.graphics.Color.GRAY);
        }
        v.setBackground(circle);
    }
}
