package com.example.societyhive_test5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PollsAdapter extends RecyclerView.Adapter<PollsAdapter.ViewHolder> {

    public interface VoteListener {
        void onVote(Poll poll, int optionIndex);
    }

    private final List<Poll> polls = new ArrayList<>();
    private final VoteListener voteListener;

    public PollsAdapter(VoteListener voteListener) {
        this.voteListener = voteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_poll, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Poll poll = polls.get(position);
        holder.tvTitle.setText(poll.getTitle());
        holder.tvQuestion.setText(poll.getQuestion());

        // Build option rows dynamically
        holder.optionsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
        List<String> options = poll.getOptions();

        for (int i = 0; i < options.size(); i++) {
            final int index = i;
            View optionView = inflater.inflate(R.layout.item_poll_option, holder.optionsContainer, false);

            TextView tvText = optionView.findViewById(R.id.tvOptionText);
            ImageView ivCheckbox = optionView.findViewById(R.id.ivCheckbox);

            tvText.setText(options.get(i));

            // Show selected or voted state
            boolean isVotedOption = poll.isHasVoted() && poll.getVotedOptionIndex() == i;
            boolean isSelectedOption = !poll.isHasVoted() && poll.getSelectedOptionIndex() == i;

            if (isVotedOption || isSelectedOption) {
                ivCheckbox.setImageResource(R.drawable.ic_check);
                ivCheckbox.setColorFilter(
                        holder.itemView.getContext().getResources().getColor(R.color.button1));
            } else {
                ivCheckbox.setImageResource(R.drawable.ic_checkbox_empty);
                ivCheckbox.clearColorFilter();
            }

            // Only allow tapping if user hasn't voted yet
            if (!poll.isHasVoted()) {
                optionView.setOnClickListener(v -> {
                    poll.setSelectedOptionIndex(index);
                    notifyItemChanged(holder.getAdapterPosition());
                });
            }

            holder.optionsContainer.addView(optionView);
        }

        // Already voted state
        if (poll.isHasVoted()) {
            holder.tvAlreadyVoted.setVisibility(View.VISIBLE);
            holder.btnVote.setVisibility(View.GONE);
        } else {
            holder.tvAlreadyVoted.setVisibility(View.GONE);
            holder.btnVote.setVisibility(View.VISIBLE);
            holder.btnVote.setOnClickListener(v -> {
                int selected = poll.getSelectedOptionIndex();
                if (selected >= 0) {
                    voteListener.onVote(poll, selected);
                }
            });
        }
    }

    @Override
    public int getItemCount() { return polls.size(); }

    public void updateList(@NonNull List<Poll> newList) {
        polls.clear();
        polls.addAll(newList);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvQuestion;
        final LinearLayout optionsContainer;
        final TextView tvAlreadyVoted;
        final com.google.android.material.button.MaterialButton btnVote;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvPollTitle);
            tvQuestion = itemView.findViewById(R.id.tvPollQuestion);
            optionsContainer = itemView.findViewById(R.id.optionsContainer);
            tvAlreadyVoted = itemView.findViewById(R.id.tvAlreadyVoted);
            btnVote = itemView.findViewById(R.id.btnVote);
        }
    }
}
