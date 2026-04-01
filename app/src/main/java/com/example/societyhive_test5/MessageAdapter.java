package com.example.societyhive_test5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_RECEIVED = 0;
    private static final int TYPE_SENT = 1;

    private final List<Message> messages;

    public MessageAdapter(@NonNull List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isSentByMe() ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        if (holder instanceof SentVH) {
            ((SentVH) holder).tvMessage.setText(message.getText());
        } else if (holder instanceof ReceivedVH) {
            ReceivedVH vh = (ReceivedVH) holder;
            vh.tvMessage.setText(message.getText());

            if (vh.tvSenderName != null) {
                String name = message.getSenderName();
                if (name != null && !name.isEmpty()) {
                    vh.tvSenderName.setVisibility(View.VISIBLE);
                    vh.tvSenderName.setText(name);
                } else {
                    vh.tvSenderName.setVisibility(View.GONE);
                }
            }

            if (vh.ivAvatar != null) {
                String photoUrl = message.getSenderPhotoUrl();
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    vh.ivAvatar.setPadding(0, 0, 0, 0);
                    vh.ivAvatar.setBackground(null);
                    Glide.with(vh.ivAvatar.getContext())
                            .load(photoUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_profile)
                            .into(vh.ivAvatar);
                } else {
                    vh.ivAvatar.setPadding(4, 4, 4, 4);
                    vh.ivAvatar.setBackgroundResource(R.drawable.bg_circle_neutral);
                    vh.ivAvatar.setImageResource(R.drawable.ic_profile);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateList(@NonNull List<Message> newMessages) {
        messages.clear();
        messages.addAll(new java.util.ArrayList<>(newMessages));
        notifyDataSetChanged();
    }

    // -------------------------------------------------------------------------
    // ViewHolders
    // -------------------------------------------------------------------------

    static class SentVH extends RecyclerView.ViewHolder {
        final TextView tvMessage;
        SentVH(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageSent);
        }
    }

    static class ReceivedVH extends RecyclerView.ViewHolder {
        final TextView tvMessage;
        final TextView tvSenderName;
        final ImageView ivAvatar;
        ReceivedVH(@NonNull View itemView) {
            super(itemView);
            tvMessage    = itemView.findViewById(R.id.tvMessageReceived);
            tvSenderName = itemView.findViewById(R.id.tvMessageSenderName);
            ivAvatar     = itemView.findViewById(R.id.ivSenderAvatar);
        }
    }
}
