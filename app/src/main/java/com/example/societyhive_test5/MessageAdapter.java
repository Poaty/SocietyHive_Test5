package com.example.societyhive_test5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

            // Show sender name above received messages
            if (vh.tvSenderName != null) {
                String name = message.getSenderName();
                if (name != null && !name.isEmpty()) {
                    vh.tvSenderName.setVisibility(View.VISIBLE);
                    vh.tvSenderName.setText(name);
                } else {
                    vh.tvSenderName.setVisibility(View.GONE);
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
        final TextView tvSenderName; // may be null if layout doesn't have it yet
        ReceivedVH(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageReceived);
            tvSenderName = itemView.findViewById(R.id.tvMessageSenderName);
        }
    }
}
