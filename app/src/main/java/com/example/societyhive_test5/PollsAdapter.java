package com.example.societyhive_test5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PollsAdapter extends RecyclerView.Adapter<PollsAdapter.ViewHolder> {

    public interface VoteListener {
        void onVote(Poll poll, int optionIndex);
    }

    public interface DeleteListener {
        void onDelete(Poll poll);
    }

    public interface CloseListener {
        void onClose(Poll poll);
    }

    private final List<Poll> polls = new ArrayList<>();
    private final VoteListener voteListener;
    @Nullable private final DeleteListener deleteListener;
    @Nullable private final CloseListener closeListener;

    public PollsAdapter(@NonNull VoteListener voteListener,
                        @Nullable DeleteListener deleteListener,
                        @Nullable CloseListener closeListener) {
        this.voteListener   = voteListener;
        this.deleteListener = deleteListener;
        this.closeListener  = closeListener;
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
        boolean closed = poll.isClosed();


        String societyName = poll.getSocietyName();
        if (societyName != null && !societyName.isEmpty()) {
            holder.tvSocietyName.setVisibility(View.VISIBLE);
            holder.tvSocietyName.setText(societyName);
        } else {
            holder.tvSocietyName.setVisibility(View.GONE);
        }


        holder.tvClosedBadge.setVisibility(closed ? View.VISIBLE : View.GONE);

        holder.tvTitle.setText(poll.getTitle());


        if (poll.getEndsAt() != null) {
            String formatted = new SimpleDateFormat("d MMM yyyy", Locale.UK)
                    .format(poll.getEndsAt().toDate());
            holder.tvClosesOn.setVisibility(View.VISIBLE);
            holder.tvClosesOn.setText(closed ? "Closed " + formatted : "Closes " + formatted);
        } else {
            holder.tvClosesOn.setVisibility(View.GONE);
        }

        holder.tvQuestion.setText(poll.getQuestion());

        List<String> options = poll.getOptions();
        List<Integer> counts = poll.getVoteCounts();
        int total = poll.getTotalVotes();
        boolean hasVoted = poll.isHasVoted();


        holder.optionsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

        for (int i = 0; i < options.size(); i++) {
            final int index = i;
            View optionView = inflater.inflate(R.layout.item_poll_option, holder.optionsContainer, false);

            TextView tvText      = optionView.findViewById(R.id.tvOptionText);
            ImageView ivCheckbox = optionView.findViewById(R.id.ivCheckbox);
            View layoutResult    = optionView.findViewById(R.id.layoutVoteResult);
            ProgressBar progress = optionView.findViewById(R.id.progressVotes);
            TextView tvCount     = optionView.findViewById(R.id.tvVoteCount);

            tvText.setText(options.get(i));

            boolean isVotedOption    = hasVoted && poll.getVotedOptionIndex() == i;
            boolean isSelectedOption = !hasVoted && !closed && poll.getSelectedOptionIndex() == i; // tapped but not yet submitted

            if (isVotedOption || isSelectedOption) {
                ivCheckbox.setImageResource(R.drawable.ic_check);
                ivCheckbox.setColorFilter(
                        holder.itemView.getContext().getResources().getColor(R.color.button1));
            } else {
                ivCheckbox.setImageResource(R.drawable.ic_checkbox_empty);
                ivCheckbox.clearColorFilter();
            }


            // show results once the user has voted or the poll is closed
            if ((hasVoted || closed) && !counts.isEmpty() && i < counts.size()) {
                layoutResult.setVisibility(View.VISIBLE);
                int voteCount = counts.get(i);
                int pct = total > 0 ? (int) Math.round((voteCount * 100.0) / total) : 0;
                progress.setProgress(pct);
                tvCount.setText(String.format(Locale.UK, "%d vote%s (%d%%)",
                        voteCount, voteCount == 1 ? "" : "s", pct));
            } else {
                layoutResult.setVisibility(View.GONE);
                if (!closed) {
                    optionView.setOnClickListener(v -> {
                        poll.setSelectedOptionIndex(index);
                        notifyItemChanged(holder.getAdapterPosition());
                    });
                }
            }

            holder.optionsContainer.addView(optionView);
        }


        if (hasVoted || closed) {
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


        if (closeListener != null && !closed) {
            holder.btnClose.setVisibility(View.VISIBLE);
            holder.btnClose.setOnClickListener(v -> closeListener.onClose(poll));
        } else {
            holder.btnClose.setVisibility(View.GONE);
        }

        if (deleteListener != null) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> deleteListener.onDelete(poll));
        } else {
            holder.btnDelete.setVisibility(View.GONE);
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
        final TextView tvClosedBadge;
        final TextView tvTitle;
        final TextView tvClosesOn;
        final TextView tvQuestion;
        final LinearLayout optionsContainer;
        final TextView tvTotalVotes;
        final MaterialButton btnVote;
        final MaterialButton btnClose;
        final MaterialButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSocietyName    = itemView.findViewById(R.id.tvSocietyName);
            tvClosedBadge    = itemView.findViewById(R.id.tvClosedBadge);
            tvTitle          = itemView.findViewById(R.id.tvPollTitle);
            tvClosesOn       = itemView.findViewById(R.id.tvClosesOn);
            tvQuestion       = itemView.findViewById(R.id.tvPollQuestion);
            optionsContainer = itemView.findViewById(R.id.optionsContainer);
            tvTotalVotes     = itemView.findViewById(R.id.tvTotalVotes);
            btnVote          = itemView.findViewById(R.id.btnVote);
            btnClose         = itemView.findViewById(R.id.btnClosePoll);
            btnDelete        = itemView.findViewById(R.id.btnDelete);
        }
    }
}
