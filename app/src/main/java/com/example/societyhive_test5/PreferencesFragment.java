package com.example.societyhive_test5;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PreferencesFragment extends Fragment {


    private static final String[][] SCHEMES = {
            {"crimson", "#F6BFC6", "Crimson"},
            {"ocean",   "#4472C4", "Ocean"},
            {"violet",  "#9B80C0", "Violet"},
    };

    public PreferencesFragment() {
        super(R.layout.fragment_preferences);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String savedKey = ThemeHelper.current(requireContext());

        int[] circIds  = {R.id.circCrimson, R.id.circOcean, R.id.circViolet};
        int[] swatchIds = {R.id.swatchCrimson, R.id.swatchOcean, R.id.swatchViolet};
        int[] tickIds  = {R.id.tickCrimson, R.id.tickOcean, R.id.tickViolet};

        for (int i = 0; i < SCHEMES.length; i++) {
            makeCircle(view, circIds[i], SCHEMES[i][1], SCHEMES[i][0].equals(savedKey));
        }

        for (int i = 0; i < SCHEMES.length; i++) {
            final String[] scheme = SCHEMES[i];
            view.findViewById(swatchIds[i])
                    .setOnClickListener(v -> selectScheme(view, scheme[0], tickIds, circIds));
        }

        refreshTicks(view, savedKey, tickIds);

    }

    private void selectScheme(@NonNull View root, @NonNull String themeKey,
                               int[] tickIds, int[] circIds) {
        ThemeHelper.save(requireContext(), themeKey);
        refreshTicks(root, themeKey, tickIds);
        refreshBorders(root, themeKey, circIds);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Map<String, Object> update = new HashMap<>();
            update.put("themeKey", themeKey);
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .update(update);
        }

        requireActivity().recreate();
    }

    private void refreshTicks(@NonNull View root, @NonNull String selectedKey, int[] tickIds) {
        for (int i = 0; i < SCHEMES.length; i++) {
            View tick = root.findViewById(tickIds[i]);
            if (tick != null) {
                tick.setVisibility(
                        SCHEMES[i][0].equals(selectedKey) ? View.VISIBLE : View.INVISIBLE);
            }
        }
    }

    private void refreshBorders(@NonNull View root, @NonNull String selectedKey, int[] circIds) {
        for (int i = 0; i < SCHEMES.length; i++) {
            View v = root.findViewById(circIds[i]);
            if (v == null) continue;

            makeCircle(root, circIds[i], SCHEMES[i][1], SCHEMES[i][0].equals(selectedKey));
        }
    }

    private void makeCircle(@NonNull View root, int viewId,
                             @NonNull String hex, boolean selected) {
        View v = root.findViewById(viewId);
        if (v == null) return;

        int density = (int) root.getResources().getDisplayMetrics().density;
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        try {
            circle.setColor(android.graphics.Color.parseColor(hex));
        } catch (IllegalArgumentException e) {
            circle.setColor(android.graphics.Color.GRAY);
        }


        if (selected) {
            circle.setStroke(density * 3, android.graphics.Color.WHITE);
        } else {
            circle.setStroke(density * 2, 0x60FFFFFF);
        }
        v.setBackground(circle);
    }
}
