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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Polls screen.
 *
 * Loads active polls from Firestore and shows only those belonging to
 * the user's societies. Users can vote once per poll; their vote is
 * persisted to polls/{pollId}/votes/{userId}.
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

                        // Only show polls for the user's societies (or open polls with no society)
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

                    checkVoteStatusForAll();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Could not load polls.", Toast.LENGTH_SHORT).show();
                });
    }

    /** For each loaded poll, check whether the current user has already voted. */
    private void checkVoteStatusForAll() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || polls.isEmpty()) {
            adapter.updateList(polls);
            return;
        }

        // Use a counter to know when all async reads are done
        final int[] remaining = {polls.size()};

        for (Poll poll : polls) {
            FirebaseFirestore.getInstance()
                    .collection("polls")
                    .document(poll.getId())
                    .collection("votes")
                    .document(user.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (isAdded() && task.isSuccessful() && task.getResult().exists()) {
                            Long idx = task.getResult().getLong("optionIndex");
                            if (idx != null) {
                                poll.setHasVoted(true);
                                poll.setVotedOptionIndex(idx.intValue());
                            }
                        }
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            // All checks done — update the UI
                            if (isAdded()) adapter.updateList(polls);
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
            Toast.makeText(requireContext(),
                    "Please log in to vote.", Toast.LENGTH_SHORT).show();
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
                    poll.setHasVoted(true);
                    poll.setVotedOptionIndex(optionIndex);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(requireContext(),
                            "Vote recorded!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to submit vote.", Toast.LENGTH_SHORT).show();
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
