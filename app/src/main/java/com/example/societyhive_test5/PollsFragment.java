package com.example.societyhive_test5;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Polls screen.
 *
 * Loads active polls for the user's societies. Reads ALL vote documents per poll
 * so we can show per-option counts and totals after the user votes.
 */
public class PollsFragment extends Fragment {

    private final List<Poll> polls = new ArrayList<>();
    private PollsAdapter adapter;
    private final Set<String> userSocietyIds = new HashSet<>();

    public PollsFragment() {
        super(R.layout.fragment_polls);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rvPolls);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PollsAdapter(this::submitVote);
        rv.setAdapter(adapter);

        loadUserSocietiesThenPolls();
    }

    // -------------------------------------------------------------------------
    // Loading sequence
    // -------------------------------------------------------------------------

    private void loadUserSocietiesThenPolls() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { loadPolls(); return; }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener((DocumentSnapshot doc) -> {
                    if (!isAdded()) return;
                    userSocietyIds.clear();
                    List<?> ids = (List<?>) doc.get("societyIds");
                    if (ids != null) {
                        for (Object id : ids) {
                            if (id instanceof String) userSocietyIds.add((String) id);
                        }
                    }
                    loadPolls();
                })
                .addOnFailureListener(e -> { if (isAdded()) loadPolls(); });
    }

    private void loadPolls() {
        FirebaseFirestore.getInstance()
                .collection("polls")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    polls.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String societyId = doc.getString("societyId");
                        if (societyId != null && !societyId.isEmpty()
                                && !userSocietyIds.contains(societyId)) continue;

                        Poll poll = new Poll();
                        poll.setId(doc.getId());
                        poll.setTitle(safeString(doc.getString("title"), "Untitled Poll"));
                        poll.setQuestion(safeString(doc.getString("question"), ""));
                        poll.setActive(true);
                        poll.setSocietyId(societyId != null ? societyId : "");

                        List<?> rawOptions = (List<?>) doc.get("options");
                        List<String> options = new ArrayList<>();
                        if (rawOptions != null) {
                            for (Object o : rawOptions) {
                                if (o instanceof String) options.add((String) o);
                            }
                        }
                        poll.setOptions(options);
                        polls.add(poll);
                    }

                    loadAllVotes();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Could not load polls.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * For each poll, reads the entire votes subcollection.
     * This gives us both the current user's vote status and the counts per option.
     */
    private void loadAllVotes() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (polls.isEmpty()) { adapter.updateList(polls); return; }

        final int[] remaining = {polls.size()};

        for (Poll poll : polls) {
            FirebaseFirestore.getInstance()
                    .collection("polls")
                    .document(poll.getId())
                    .collection("votes")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (isAdded() && task.isSuccessful()) {
                            QuerySnapshot voteDocs = task.getResult();

                            // Tally votes per option
                            Map<Integer, Integer> counts = new HashMap<>();
                            for (QueryDocumentSnapshot voteDoc : voteDocs) {
                                Long idx = voteDoc.getLong("optionIndex");
                                if (idx == null) continue;
                                int i = idx.intValue();
                                counts.put(i, counts.containsKey(i) ? counts.get(i) + 1 : 1);

                                // Check if this is the current user's vote
                                if (user != null && voteDoc.getId().equals(user.getUid())) {
                                    poll.setHasVoted(true);
                                    poll.setVotedOptionIndex(i);
                                }
                            }

                            // Store counts list aligned to options
                            List<Integer> countList = new ArrayList<>();
                            for (int i = 0; i < poll.getOptions().size(); i++) {
                                countList.add(counts.containsKey(i) ? counts.get(i) : 0);
                            }
                            poll.setVoteCounts(countList);
                            poll.setTotalVotes(voteDocs.size());
                        }

                        remaining[0]--;
                        if (remaining[0] == 0 && isAdded()) {
                            adapter.updateList(polls);
                        }
                    });
        }
    }

    // -------------------------------------------------------------------------
    // Voting
    // -------------------------------------------------------------------------

    private void submitVote(@NonNull Poll poll, int optionIndex) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please log in to vote.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> voteData = new HashMap<>();
        voteData.put("optionIndex", optionIndex);
        voteData.put("votedAt", Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection("polls")
                .document(poll.getId())
                .collection("votes")
                .document(user.getUid())
                .set(voteData)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    // Optimistically update local counts
                    List<Integer> counts = new ArrayList<>(poll.getVoteCounts());
                    while (counts.size() < poll.getOptions().size()) counts.add(0);
                    counts.set(optionIndex, counts.get(optionIndex) + 1);
                    poll.setVoteCounts(counts);
                    poll.setTotalVotes(poll.getTotalVotes() + 1);
                    poll.setHasVoted(true);
                    poll.setVotedOptionIndex(optionIndex);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(requireContext(), "Vote recorded!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to submit vote.", Toast.LENGTH_SHORT).show();
                });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @NonNull
    private String safeString(@Nullable String value, @NonNull String fallback) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : fallback;
    }
}
