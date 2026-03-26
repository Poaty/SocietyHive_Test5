package com.example.societyhive_test5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class UserManagementAdapter
        extends RecyclerView.Adapter<UserManagementAdapter.ViewHolder> {

    public interface OnRoleToggleListener {
        void onRoleToggled(String uid, boolean makeAdmin);
    }

    // -------------------------------------------------------------------------

    public static class UserItem {
        final String uid;
        final String fullName;
        boolean isAdmin;

        public UserItem(String uid, String fullName, boolean isAdmin) {
            this.uid      = uid;
            this.fullName = fullName;
            this.isAdmin  = isAdmin;
        }
    }

    // -------------------------------------------------------------------------

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView     tvUserName;
        final SwitchMaterial switchAdmin;

        ViewHolder(View v) {
            super(v);
            tvUserName  = v.findViewById(R.id.tvUserName);
            switchAdmin = v.findViewById(R.id.switchAdmin);
        }
    }

    // -------------------------------------------------------------------------

    private final List<UserItem>       allItems     = new ArrayList<>();
    private final List<UserItem>       displayItems = new ArrayList<>();
    private final OnRoleToggleListener listener;

    public UserManagementAdapter(OnRoleToggleListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<UserItem> users) {
        allItems.clear();
        allItems.addAll(users);
        displayItems.clear();
        displayItems.addAll(users);
        notifyDataSetChanged();
    }

    /** Filters the displayed list by the given query (case-insensitive name match). */
    public void filter(String query) {
        displayItems.clear();
        if (query == null || query.trim().isEmpty()) {
            displayItems.addAll(allItems);
        } else {
            String lower = query.toLowerCase();
            for (UserItem item : allItems) {
                if (item.fullName.toLowerCase().contains(lower)) {
                    displayItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    // -------------------------------------------------------------------------

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_management, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserItem item = displayItems.get(position);
        holder.tvUserName.setText(item.fullName);

        // Update switch without firing the listener
        holder.switchAdmin.setOnCheckedChangeListener(null);
        holder.switchAdmin.setChecked(item.isAdmin);

        holder.switchAdmin.setOnCheckedChangeListener((btn, isChecked) -> {
            item.isAdmin = isChecked;
            listener.onRoleToggled(item.uid, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }
}
