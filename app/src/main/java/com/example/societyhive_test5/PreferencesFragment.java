package com.example.societyhive_test5;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Preferences screen — colour scheme selection.
 *
 * Selecting a scheme:
 *   1. Saves the theme key to SharedPreferences (instant, local).
 *   2. Saves it to Firestore users/{uid}.themeKey (multi-device sync).
 *   3. Calls Activity.recreate() so the new theme is applied immediately.
 */
public class PreferencesFragment extends Fragment {

    private static final String KEY_COMPACT = "pref_compact_events";

    // [themeKey, swatchHex, displayName]
    private static final String[][] SCHEMES = {
            {"crimson",  "#B91C1C", "Crimson"},
            {"midnight", "#0061A4", "Midnight"},
            {"forest",   "#006E21", "Forest"},
            {"slate",    "#6750A4", "Slate"},
            {"charcoal", "#4A6267", "Charcoal"},
    };

    public PreferencesFragment() {
        super(R.layout.fragment_preferences);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String savedKey = ThemeHelper.current(requireContext());

        makeCircle(view, R.id.circCrimson,  "#B91C1C");
        makeCircle(view, R.id.circBlue,     "#0061A4");
        makeCircle(view, R.id.circGreen,    "#006E21");
        makeCircle(view, R.id.circPurple,   "#6750A4");
        makeCircle(view, R.id.circCharcoal, "#4A6267");

        int[] swatchIds = {
                R.id.swatchCrimson, R.id.swatchBlue, R.id.swatchGreen,
                R.id.swatchPurple, R.id.swatchCharcoal
        };

        for (int i = 0; i < SCHEMES.length; i++) {
            final String[] scheme = SCHEMES[i];
            View swatch = view.findViewById(swatchIds[i]);
            if (swatch != null) {
                swatch.setOnClickListener(v -> selectScheme(view, scheme[0], scheme[2]));
            }
        }

        refreshTicks(view, savedKey);

        com.google.android.material.switchmaterial.SwitchMaterial swCompact =
                view.findViewById(R.id.switchCompactEvents);
        if (swCompact != null) {
            android.content.SharedPreferences prefs = requireContext()
                    .getSharedPreferences(ThemeHelper.PREFS, android.content.Context.MODE_PRIVATE);
            swCompact.setChecked(prefs.getBoolean(KEY_COMPACT, false));
            swCompact.setOnCheckedChangeListener((btn, checked) ->
                    prefs.edit().putBoolean(KEY_COMPACT, checked).apply());
        }
    }

    private void selectScheme(@NonNull View root,
                               @NonNull String themeKey,
                               @NonNull String displayName) {
        // 1 — save locally
        ThemeHelper.save(requireContext(), themeKey);
        refreshTicks(root, themeKey);

        TextView tvSelected = root.findViewById(R.id.tvSelectedScheme);
        if (tvSelected != null) tvSelected.setText("Selected: " + displayName);

        // 2 — save to Firestore so other devices pick it up
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Map<String, Object> update = new HashMap<>();
            update.put("themeKey", themeKey);
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .update(update);
        }

        // 3 — recreate so the new theme takes effect immediately
        requireActivity().recreate();
    }

    private void refreshTicks(@NonNull View root, @NonNull String selectedKey) {
        int[] tickIds = {
                R.id.tickCrimson, R.id.tickBlue, R.id.tickGreen,
                R.id.tickPurple, R.id.tickCharcoal
        };

        for (int i = 0; i < SCHEMES.length; i++) {
            View tick = root.findViewById(tickIds[i]);
            if (tick != null) {
                tick.setVisibility(
                        SCHEMES[i][0].equals(selectedKey)
                                ? View.VISIBLE : View.INVISIBLE);
            }
        }

        TextView tvSelected = root.findViewById(R.id.tvSelectedScheme);
        if (tvSelected != null) {
            for (String[] scheme : SCHEMES) {
                if (scheme[0].equals(selectedKey)) {
                    tvSelected.setText("Selected: " + scheme[2]);
                    break;
                }
            }
        }
    }

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
