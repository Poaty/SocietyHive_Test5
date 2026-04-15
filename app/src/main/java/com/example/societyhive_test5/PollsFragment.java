package com.example.societyhive_test5;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
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
 * Regular users: see only active (open) polls for their societies.
 * Admins / society admins: also see a "Closed Polls" section below,
 * showing final vote counts. Delete button visible to admins on all polls.
 */
public class PollsFragment extends Fragment {

    private final List<Poll> activePolls = new ArrayList<>();
    private final List<Poll> closedPolls = new ArrayList<>();

    private PollsAdapter activeAdapter;
    private PollsAdapter closedAdapter;

    private final Set<String> userSocietyIds = new HashSet<>();
    private boolean isAdmin = false;
    private boolean isSocietyAdmin = false;

    private View progressPolls;
    private TextView tvEmptyPolls;
    private TextView tvClosedPollsLabel;
    private RecyclerView rvClosedPolls;

    public PollsFragment() {
        super(R.layout.fragment_polls);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressPolls      = view.findViewById(R.id.progressPolls);
        tvEmptyPolls       = view.findViewById(R.id.tvEmptyPolls);
        tvClosedPollsLabel = view.findViewById(R.id.tvClosedPollsLabel);
        rvClosedPolls      = view.findViewById(R.id.rvClosedPolls);

        // Active polls RecyclerView — delete listener wired after we know role
        RecyclerView rvPolls = view.findViewById(R.id.rvPolls);
        rvPolls.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPolls.setHasFixedSize(false);

        rvClosedPolls.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvClosedPolls.setHasFixedSize(false);

        // Adapters created without delete listener for now; replaced once role is known
        activeAdapter = new PollsAdapter(this::submitVote, null);
        closedAdapter = new PollsAdapter(this::submitVote, null);
        rvPolls.setAdapter(activeAdapter);
        rvClosedPolls.setAdapter(closedAdapter);

        loadUserSocietiesThenPolls();
    }

    // -------------------------------------------------------------------------
    // Step 1 — User societies + role
    // -------------------------------------------------------------------------

    private void loadUserSocietiesThenPolls() {
        if (progressPolls != null) progressPolls.setVisibility(View.VISIBLE);
        if (tvEmptyPolls != null) tvEmptyPolls.setVisibility(View.GONE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { loadPolls(); return; }

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid()).get()
                .addOnSuccessListener((DocumentSnapshot doc) -> {
                    if (!isAdded()) return;

                    isAdmin = "admin".equalsIgnoreCase(doc.getString("role"));

                    List<?> adminOf = (List<?>) doc.get("adminOf");
                    isSocietyAdmin = !isAdmin && adminOf != null && !adminOf.isEmpty();

                    userSocietyIds.clear();
                    List<?> ids = (List<?>) doc.get("societyIds");
                    if (ids != null) {
                        for (Object id : ids) {
                            if (id instanceof String) userSocietyIds.add((String) id);
                        }
                    }

                    // Re-create adapters with delete listener for admins
                    if (isAdmin || isSocietyAdmin) {
                        RecyclerView rvPolls = requireView().findViewById(R.id.rvPolls);
                        activeAdapter = new PollsAdapter(this::submitVote, this::deletePoll);
                        closedAdapter = new PollsAdapter(this::submitVote, this::deletePoll);
                        rvPolls.setAdapter(activeAdapter);
                        rvClosedPolls.setAdapter(closedAdapter);
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
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    activePolls.clear();
                    closedPolls.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String societyId = doc.getString("societyId");

                        // Visibility: admins see all; others see only their societies
                        if (!isAdmin && !isSocietyAdmin
                                && societyId != null && !societyId.isEmpty()
                                && !userSocietyIds.contains(societyId)) continue;

                        Poll poll = new Poll();
                        poll.setId(doc.getId());
                        poll.setTitle(safeString(doc.getString("title"), "Untitled Poll"));
                        poll.setQuestion(safeString(doc.getString("question"), ""));
                        poll.setActive(true);
                        poll.setSocietyId(societyId != null ? societyId : "");
                        poll.setEndsAt(doc.getTimestamp("endsAt"));

                        // Split into active vs closed
                        if (poll.isClosed()) {
                            // Only admins/society admins see closed polls
                            if (isAdmin || isSocietyAdmin) closedPolls.add(poll);
                        } else {
                            activePolls.add(poll);
                        }
                    }

                    fetchSocietyNamesThenVotes();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    if (progressPolls != null) progressPolls.setVisibility(View.GONE);
                    if (tvEmptyPolls != null) {
                        tvEmptyPolls.setText("Could not load polls.");
                        tvEmptyPolls.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(requireContext(), "Could not load polls.", Toast.LENGTH_SHORT).show();
                });
    }

    // -------------------------------------------------------------------------
    // Step 3 — Fetch society names
    // -------------------------------------------------------------------------

    private void fetchSocietyNamesThenVotes() {
        Set<String> ids = new HashSet<>();
        for (Poll p : activePolls) if (!p.getSocietyId().isEmpty()) ids.add(p.getSocietyId());
        for (Poll p : closedPolls) if (!p.getSocietyId().isEmpty()) ids.add(p.getSocietyId());

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
                            for (Poll p : activePolls) {
                                String n = nameMap.get(p.getSocietyId());
                                if (n != null) p.setSocietyName(n);
                            }
                            for (Poll p : closedPolls) {
                                String n = nameMap.get(p.getSocietyId());
                                if (n != null) p.setSocietyName(n);
                            }
                            if (isAdded()) loadAllVotes();
                        }
                    });
        }
    }

    // -------------------------------------------------------------------------
    // Step 4 — Load votes
    // -------------------------------------------------------------------------

    private void loadAllVotes() {
        List<Poll> all = new ArrayList<>(activePolls);
        all.addAll(closedPolls);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (all.isEmpty()) {
            publishPolls();
            return;
        }

        final int[] remaining = {all.size()};

        for (Poll poll : all) {
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
        if (remaining[0] == 0 && isAdded()) publishPolls();
    }

    // -------------------------------------------------------------------------
    // Publish to UI
    // -------------------------------------------------------------------------

    private void publishPolls() {
        if (progressPolls != null) progressPolls.setVisibility(View.GONE);

        activeAdapter.updateList(activePolls);

        // Empty state only considers active polls for regular users
        if (activePolls.isEmpty()) {
            if (tvEmptyPolls != null) {
                tvEmptyPolls.setText("No polls available yet.");
                tvEmptyPolls.setVisibility(View.VISIBLE);
            }
        } else {
            if (tvEmptyPolls != null) tvEmptyPolls.setVisibility(View.GONE);
        }

        // Closed section — only for admins/society admins
        if ((isAdmin || isSocietyAdmin) && !closedPolls.isEmpty()) {
            closedAdapter.updateList(closedPolls);
            if (tvClosedPollsLabel != null) tvClosedPollsLabel.setVisibility(View.VISIBLE);
            if (rvClosedPolls != null) rvClosedPolls.setVisibility(View.VISIBLE);
        } else {
            if (tvClosedPollsLabel != null) tvClosedPollsLabel.setVisibility(View.GONE);
            if (rvClosedPolls != null) rvClosedPolls.setVisibility(View.GONE);
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
                .collection("polls").document(poll.getId())
                .collection("votes").document(user.getUid())
                .set(voteData)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    List<Integer> counts = new ArrayList<>(poll.getVoteCounts());
                    while (counts.size() < poll.getOptions().size()) counts.add(0);
                    counts.set(optionIndex, counts.get(optionIndex) + 1);
                    poll.setVoteCounts(counts);
                    poll.setTotalVotes(poll.getTotalVotes() + 1);
                    poll.setHasVoted(true);
                    poll.setVotedOptionIndex(optionIndex);
                    activeAdapter.notifyDataSetChanged();
                    Toast.makeText(requireContext(), "Vote recorded!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to submit vote.", Toast.LENGTH_SHORT).show();
                });
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    private void deletePoll(@NonNull Poll poll) {
        FirebaseFirestore.getInstance()
                .collection("polls").document(poll.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    activePolls.remove(poll);
                    closedPolls.remove(poll);
                    publishPolls();
                    Toast.makeText(requireContext(), "Poll deleted.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to delete poll: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // -------------------------------------------------------------------------

    @NonNull
    private String safeString(@Nullable String value, @NonNull String fallback) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : fallback;
    }
}
