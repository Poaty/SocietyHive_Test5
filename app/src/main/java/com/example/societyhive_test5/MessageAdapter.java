package com.example.societyhive_test5;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
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

    // two view types so sent and received messages can use different layouts and alignment
    private static final int TYPE_RECEIVED = 0;
    private static final int TYPE_SENT = 1;

    private final List<Message> messages;
    private final ChatTheme theme;

    public MessageAdapter(@NonNull List<Message> messages, @NonNull ChatTheme theme) {
        this.messages = messages;
        this.theme = theme;
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
            SentVH vh = (SentVH) holder;
            vh.tvMessage.setText(message.getText());
            // tint the bubble fill + stroke to the society's primary. the asGradient
            // helper handles mutate() so other views using the same drawable are safe
            paintSentBubble(vh.tvMessage);
        } else if (holder instanceof ReceivedVH) {
            ReceivedVH vh = (ReceivedVH) holder;
            vh.tvMessage.setText(message.getText());
            paintReceivedBubble(vh.tvMessage);

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
                    // with a real photo Glide takes over — drop the ring so the image shows full-bleed
                    vh.ivAvatar.setPadding(0, 0, 0, 0);
                    vh.ivAvatar.setBackground(null);
                    Glide.with(vh.ivAvatar.getContext())
                            .load(photoUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_profile)
                            .into(vh.ivAvatar);
                } else {
                    // no photo → show the default icon inside a themed ring
                    int pad = dp(vh.ivAvatar.getResources(), 4);
                    vh.ivAvatar.setPadding(pad, pad, pad, pad);
                    vh.ivAvatar.setBackgroundResource(R.drawable.bg_avatar_ring);
                    vh.ivAvatar.setImageResource(R.drawable.ic_profile);
                    paintAvatarRing(vh.ivAvatar);
                }
            }
        }
    }

    // bubble tinting helpers -----------------------------------------------------

    private void paintSentBubble(@NonNull TextView bubble) {
        GradientDrawable gd = asGradient(bubble.getBackground());
        if (gd != null) {
            gd.setColor(theme.primary);
            gd.setStroke(dp(bubble.getResources(), 1), theme.sentStroke);
        }
        bubble.setTextColor(theme.sentText);
    }

    private void paintReceivedBubble(@NonNull TextView bubble) {
        GradientDrawable gd = asGradient(bubble.getBackground());
        if (gd != null) {
            gd.setColor(theme.receivedBg);
            gd.setStroke(dp(bubble.getResources(), 1), theme.receivedStroke);
        }
        // received text stays near-black — the tinted bg is light enough that dark text always reads
    }

    private void paintAvatarRing(@NonNull View avatar) {
        GradientDrawable gd = asGradient(avatar.getBackground());
        if (gd != null) {
            gd.setColor(theme.avatarFill);
            gd.setStroke(dp(avatar.getResources(), 2), theme.primary);
        }
    }

    // mutate() forks the drawable's constant state so tinting this bubble doesn't
    // leak into every other view using the same XML drawable resource. mutate is
    // idempotent, so calling it on every bind is safe. returns null if the
    // background isn't a shape drawable — e.g. after Glide swaps in a bitmap —
    // so callers can no-op rather than crash
    private static GradientDrawable asGradient(Drawable bg) {
        return bg instanceof GradientDrawable ? (GradientDrawable) bg.mutate() : null;
    }

    private static int dp(@NonNull Resources res, int dp) {
        return Math.round(dp * res.getDisplayMetrics().density);
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
