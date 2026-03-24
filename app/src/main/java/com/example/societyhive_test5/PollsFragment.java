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
 * Loading sequence:
 *   1. Load user's societyIds
 *   2. Load active polls, filter by societyId
 *   3. Fetch society names for display
 *   4. Load all votes per poll (counts + user's own vote status)
 *      Falls back to a direct document read for the user's vote if the
 *      collection query is denied (e.g. restrictive rules).
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
    // Step 1 — User societies
    // -------------------------------------------------------------------------

    private void loadUserSocietiesThenPolls() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { loadPolls(); return; }

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid()).get()
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

    // -------------------------------------------------------------------------
    // Step 2 — Load polls
    // -------------------------------------------------------------------------

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

                    fetchSocietyNamesThenVotes();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Could not load polls.", Toast.LENGTH_SHORT).show();
                });
    }

    // -------------------------------------------------------------------------
    // Step 3 — Fetch society names
    // -------------------------------------------------------------------------

    private void fetchSocietyNamesThenVotes() {
        Set<String> ids = new HashSet<>();
        for (Poll p : polls) {
            if (!p.getSocietyId().isEmpty()) ids.add(p.getSocietyId());
        }

        if (ids.isEmpty()) { loadAllVotes(); return; }

        final int[] remaining = {ids.size()};
        final Map<String, String> nameMap = new HashMap<>();

        for (String sid : ids) {
            FirebaseFirestore.getInstance()
                    .collection("societies").document(sid).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            String name = task.getResult().getString("name");
                            if (name != null) nameMap.put(sid, name);
                        }
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            for (Poll p : polls) {
                                String name = nameMap.get(p.getSocietyId());
                                if (name != null) p.setSocietyName(name);
                            }
                            if (isAdded()) loadAllVotes();
                        }
                    });
        }
    }

    // -------------------------------------------------------------------------
    // Step 4 — Load votes (counts + hasVoted check)
    // -------------------------------------------------------------------------

    private void loadAllVotes() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (polls.isEmpty()) { adapter.updateList(polls); return; }

        final int[] remaining = {polls.size()};

        for (Poll poll : polls) {
            // Try reading the whole votes collection (requires permissive read rules).
            // If that fails, fall back to reading just the current user's vote document.
            FirebaseFirestore.getInstance()
                    .collection("polls").document(poll.getId())
                    .collection("votes")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (!isAdded()) { finishOne(remaining); return; }

                        if (task.isSuccessful()) {
                            applyVoteDocs(poll, task.getResult(), user);
                            finishOne(remaining);
                        } else {
                            // Permission denied — fall back to user's own document only
                            if (user == null) { finishOne(remaining); return; }
                            FirebaseFirestore.getInstance()
                                    .collection("polls").document(poll.getId())
                                    .collection("votes").document(user.getUid())
                                    .get()
                                    .addOnCompleteListener(fallback -> {
                                        if (isAdded() && fallback.isSuccessful()
                                                && fallback.getResult().exists()) {
                                            Long idx = fallback.getResult().getLong("optionIndex");
                                            if (idx != null) {
                                                poll.setHasVoted(true);
                                                poll.setVotedOptionIndex(idx.intValue());
                                            }
                                        }
                                        finishOne(remaining);
                                    });
                        }
                    });
        }
    }

    private void applyVoteDocs(Poll poll, QuerySnapshot voteDocs, FirebaseUser user) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (QueryDocumentSnapshot voteDoc : voteDocs) {
            Long idx = voteDoc.getLong("optionIndex");
            if (idx == null) continue;
            int i = idx.intValue();
            counts.put(i, counts.containsKey(i) ? counts.get(i) + 1 : 1);

            if (user != null && voteDoc.getId().equals(user.getUid())) {
                poll.setHasVoted(true);
                poll.setVotedOptionIndex(i);
            }
        }

        List<Integer> countList = new ArrayList<>();
        for (int i = 0; i < poll.getOptions().size(); i++) {
            countList.add(counts.containsKey(i) ? counts.get(i) : 0);
        }
        poll.setVoteCounts(countList);
        poll.setTotalVotes(voteDocs.size());
    }

    private void finishOne(int[] remaining) {
        remaining[0]--;
        if (remaining[0] == 0 && isAdded()) adapter.updateList(polls);
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
                .collection("polls").document(poll.getId())
                .collection("votes").document(user.getUid())
                .set(voteData)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    // Optimistic local update
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
