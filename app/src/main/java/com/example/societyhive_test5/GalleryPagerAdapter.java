package com.example.societyhive_test5;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class GalleryPagerAdapter extends FragmentStateAdapter {

    private final List<String> tabSocietyIds;
    private final List<String> tabSocietyColors;
    private final String currentUid;
    private final boolean isAdmin;

    public GalleryPagerAdapter(@NonNull Fragment fragment,
                               List<String> tabSocietyIds,
                               List<String> tabSocietyColors,
                               String currentUid,
                               boolean isAdmin) {
        super(fragment);
        this.tabSocietyIds    = tabSocietyIds;
        this.tabSocietyColors = tabSocietyColors;
        this.currentUid       = currentUid;
        this.isAdmin          = isAdmin;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // build the full id→color map from all tabs so each page knows the colours for cross-society photos
        List<String> realIds    = new ArrayList<>();
        List<String> realColors = new ArrayList<>();
        for (int i = 0; i < tabSocietyIds.size(); i++) {
            String id = tabSocietyIds.get(i);
            if (!id.isEmpty()) {
                realIds.add(id);
                realColors.add(i < tabSocietyColors.size() ? tabSocietyColors.get(i) : "#8D2E3A");
            }
        }
        return GalleryPageFragment.newInstance(
                tabSocietyIds.get(position),
                currentUid,
                isAdmin,
                realIds.toArray(new String[0]),
                realColors.toArray(new String[0])
        );
    }

    @Override
    public int getItemCount() { return tabSocietyIds.size(); }
}
