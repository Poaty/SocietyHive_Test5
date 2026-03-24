package com.example.societyhive_test5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

        // Society name badge
        String societyName = poll.getSocietyName();
        if (societyName != null && !societyName.isEmpty()) {
            holder.tvSocietyName.setVisibility(View.VISIBLE);
            holder.tvSocietyName.setText(societyName);
        } else {
            holder.tvSocietyName.setVisibility(View.GONE);
        }

        holder.tvTitle.setText(poll.getTitle());
        holder.tvQuestion.setText(poll.getQuestion());

        List<String> options = poll.getOptions();
        List<Integer> counts = poll.getVoteCounts();
        int total = poll.getTotalVotes();
        boolean hasVoted = poll.isHasVoted();

        // Build option rows dynamically
        holder.optionsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

        for (int i = 0; i < options.size(); i++) {
            final int index = i;
            View optionView = inflater.inflate(R.layout.item_poll_option, holder.optionsContainer, false);

            TextView tvText        = optionView.findViewById(R.id.tvOptionText);
            ImageView ivCheckbox   = optionView.findViewById(R.id.ivCheckbox);
            View layoutResult      = optionView.findViewById(R.id.layoutVoteResult);
            ProgressBar progress   = optionView.findViewById(R.id.progressVotes);
            TextView tvCount       = optionView.findViewById(R.id.tvVoteCount);

            tvText.setText(options.get(i));

            boolean isVotedOption    = hasVoted && poll.getVotedOptionIndex() == i;
            boolean isSelectedOption = !hasVoted && poll.getSelectedOptionIndex() == i;

            if (isVotedOption || isSelectedOption) {
                ivCheckbox.setImageResource(R.drawable.ic_check);
                ivCheckbox.setColorFilter(
                        holder.itemView.getContext().getResources().getColor(R.color.button1));
            } else {
                ivCheckbox.setImageResource(R.drawable.ic_checkbox_empty);
                ivCheckbox.clearColorFilter();
            }

            if (hasVoted && !counts.isEmpty() && i < counts.size()) {
                // Show results row
                layoutResult.setVisibility(View.VISIBLE);
                int voteCount = counts.get(i);
                int pct = total > 0 ? (int) Math.round((voteCount * 100.0) / total) : 0;
                progress.setProgress(pct);
                tvCount.setText(String.format(Locale.UK, "%d vote%s (%d%%)",
                        voteCount, voteCount == 1 ? "" : "s", pct));
            } else {
                layoutResult.setVisibility(View.GONE);
                // Allow tapping if user hasn't voted yet
                optionView.setOnClickListener(v -> {
                    poll.setSelectedOptionIndex(index);
                    notifyItemChanged(holder.getAdapterPosition());
                });
            }

            holder.optionsContainer.addView(optionView);
        }

        // Footer: total votes label or Vote button
        if (hasVoted) {
            holder.tvTotalVotes.setVisibility(View.VISIBLE);
            holder.tvTotalVotes.setText(
                    String.format(Locale.UK, "%d total vote%s", total, total == 1 ? "" : "s"));
            holder.btnVote.setVisibility(View.GONE);
        } else {
            holder.tvTotalVotes.setVisibility(View.GONE);
            holder.btnVote.setVisibility(View.VISIBLE);
            holder.btnVote.setOnClickListener(v -> {
                int selected = poll.getSelectedOptionIndex();
                if (selected >= 0) voteListener.onVote(poll, selected);
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
        final TextView tvSocietyName;
        final TextView tvTitle;
        final TextView tvQuestion;
        final LinearLayout optionsContainer;
        final TextView tvTotalVotes;
        final com.google.android.material.button.MaterialButton btnVote;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSocietyName  = itemView.findViewById(R.id.tvSocietyName);
            tvTitle        = itemView.findViewById(R.id.tvPollTitle);
            tvQuestion     = itemView.findViewById(R.id.tvPollQuestion);
            optionsContainer = itemView.findViewById(R.id.optionsContainer);
            tvTotalVotes   = itemView.findViewById(R.id.tvTotalVotes);
            btnVote        = itemView.findViewById(R.id.btnVote);
        }
    }
}
