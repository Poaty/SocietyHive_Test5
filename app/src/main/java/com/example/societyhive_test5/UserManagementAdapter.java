package com.example.societyhive_test5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class UserManagementAdapter extends RecyclerView.Adapter<UserManagementAdapter.UserVH> {

    public interface OnUserClickListener {
        void onUserClick(UserItem user);
    }

    private final List<UserItem> users = new ArrayList<>();
    private OnUserClickListener clickListener;

    public void setClickListener(OnUserClickListener listener) {
        this.clickListener = listener;
    }

    public void updateList(List<UserItem> newList) {
        users.clear();
        users.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_management, parent, false);
        return new UserVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserVH holder, int position) {
        UserItem user = users.get(position);
        holder.tvName.setText(user.getFullName().isEmpty() ? "No name" : user.getFullName());
        holder.tvEmail.setText(user.getEmail());
        holder.tvRole.setText(user.getRole());

        // Load profile picture if available - we reuse bg_circle_neutral as placeholder
        holder.ivAvatar.setPadding(6, 6, 6, 6);
        holder.ivAvatar.setBackgroundResource(R.drawable.bg_circle_neutral);
        holder.ivAvatar.setImageResource(R.drawable.ic_profile);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onUserClick(user);
        });
    }

    @Override
    public int getItemCount() { return users.size(); }

    static class UserVH extends RecyclerView.ViewHolder {
        final ImageView ivAvatar;
        final TextView tvName, tvEmail, tvRole;
        UserVH(@NonNull View v) {
            super(v);
            ivAvatar = v.findViewById(R.id.ivUserAvatar);
            tvName   = v.findViewById(R.id.tvUserName);
            tvEmail  = v.findViewById(R.id.tvUserEmail);
            tvRole   = v.findViewById(R.id.tvUserRole);
        }
    }
}
