package com.example.societyhive_test5;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class GalleryPagerAdapter extends FragmentStateAdapter {

    private final List<String> societyIds;

    public GalleryPagerAdapter(@NonNull Fragment fragment, List<String> societyIds) {
        super(fragment);
        this.societyIds = societyIds;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return GalleryPageFragment.newInstance(societyIds.get(position));
    }

    @Override
    public int getItemCount() { return societyIds.size(); }
}
