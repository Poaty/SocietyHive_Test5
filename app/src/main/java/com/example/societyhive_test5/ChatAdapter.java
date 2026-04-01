package com.example.societyhive_test5;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatVH> {

    public interface OnChatClickListener {
        void onChatClick(@NonNull Chat chat);
    }

    private final List<Chat> chats;
    private final OnChatClickListener listener;

    public ChatAdapter(@NonNull List<Chat> chats, @NonNull OnChatClickListener listener) {
        this.chats = chats;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatVH holder, int position) {
        holder.bind(chats.get(position));
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    public void updateList(@NonNull List<Chat> newChats) {
        chats.clear();
        chats.addAll(new java.util.ArrayList<>(newChats));
        notifyDataSetChanged();
    }

    class ChatVH extends RecyclerView.ViewHolder {
        private final View accentBar;
        private final TextView tvTitle;
        private final TextView tvPreview;
        private final TextView tvTime;
        private final ImageView ivIcon;

        ChatVH(@NonNull View itemView) {
            super(itemView);
            accentBar = itemView.findViewById(R.id.viewAccent);
            tvTitle = itemView.findViewById(R.id.tvChatTitle);
            tvPreview = itemView.findViewById(R.id.tvChatPreview);
            tvTime = itemView.findViewById(R.id.tvChatTime);
            ivIcon = itemView.findViewById(R.id.ivSocietyIcon);
        }

        void bind(@NonNull Chat chat) {
            tvTitle.setText(chat.getTitle());
            tvPreview.setText(chat.getLastMessage());
            tvTime.setText(chat.getTime());

            try {
                accentBar.setBackgroundColor(Color.parseColor(chat.getSocietyColor()));
            } catch (IllegalArgumentException e) {
                accentBar.setBackgroundColor(Color.parseColor("#8D2E3A"));
            }

            if (ivIcon != null) {
                String iconUrl = chat.getIconUrl();
                if (iconUrl != null && !iconUrl.isEmpty()) {
                    ivIcon.setPadding(0, 0, 0, 0);
                    ivIcon.setBackground(null);
                    Glide.with(ivIcon.getContext())
                            .load(iconUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_profile)
                            .into(ivIcon);
                } else {
                    ivIcon.setPadding(4, 4, 4, 4);
                    ivIcon.setBackgroundResource(R.drawable.bg_circle_neutral);
                    ivIcon.setImageResource(R.drawable.ic_profile);
                }
            }

            itemView.setOnClickListener(v -> listener.onChatClick(chat));
        }
    }
}
