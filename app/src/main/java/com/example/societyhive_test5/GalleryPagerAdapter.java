package com.example.societyhive_test5;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class GalleryPagerAdapter extends FragmentStateAdapter {

    private final List<String> societyIds;
    private final String currentUid;
    private final boolean isAdmin;

    public GalleryPagerAdapter(@NonNull Fragment fragment,
                               List<String> societyIds,
                               String currentUid,
                               boolean isAdmin) {
        super(fragment);
        this.societyIds = societyIds;
        this.currentUid = currentUid;
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return GalleryPageFragment.newInstance(societyIds.get(position), currentUid, isAdmin);
    }

    @Override
    public int getItemCount() { return societyIds.size(); }
}
