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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
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


        RecyclerView rvPolls = view.findViewById(R.id.rvPolls);
        rvPolls.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPolls.setHasFixedSize(false);

        rvClosedPolls.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvClosedPolls.setHasFixedSize(false);


        activeAdapter = new PollsAdapter(this::submitVote, null, null);
        closedAdapter = new PollsAdapter(this::submitVote, null, null);
        rvPolls.setAdapter(activeAdapter);
        rvClosedPolls.setAdapter(closedAdapter);

        loadUserSocietiesThenPolls();
    }





    // need to know what societies the user is in before we can filter polls
    private void loadUserSocietiesThenPolls() {
        if (progressPolls != null) progressPolls.setVisibility(View.VISIBLE);
        if (tvEmptyPolls != null) tvEmptyPolls.setVisibility(View.GONE);

        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null) { loadPolls(); return; }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usersCollection = db.collection("users");
        DocumentReference userDocument = usersCollection.document(user.getUid());
        userDocument.get()
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


                    // only admins and society admins get the close/delete buttons
                    if (isAdmin || isSocietyAdmin) {
                        RecyclerView rvPolls = requireView().findViewById(R.id.rvPolls);
                        activeAdapter = new PollsAdapter(this::submitVote, this::deletePoll, this::closePoll);
                        closedAdapter = new PollsAdapter(this::submitVote, this::deletePoll, null);
                        rvPolls.setAdapter(activeAdapter);
                        rvClosedPolls.setAdapter(closedAdapter);
                    }

                    loadPolls();
                })
                .addOnFailureListener(e -> { if (isAdded()) loadPolls(); });
    }





    private void loadPolls() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference pollsCollection = db.collection("polls");
        pollsCollection
                .whereEqualTo("isActive", true)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    activePolls.clear();
                    closedPolls.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String societyId = doc.getString("societyId");


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

                        List<?> rawOptions = (List<?>) doc.get("options");
                        List<String> options = new ArrayList<>();
                        if (rawOptions != null) {
                            for (Object o : rawOptions) {
                                if (o instanceof String) options.add((String) o);
                            }
                        }
                        poll.setOptions(options);


                        if (poll.isClosed()) {

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





    // collect unique society ids from all polls then look them up in one go
    private void fetchSocietyNamesThenVotes() {
        Set<String> ids = new HashSet<>();
        for (Poll p : activePolls) if (!p.getSocietyId().isEmpty()) ids.add(p.getSocietyId());
        for (Poll p : closedPolls) if (!p.getSocietyId().isEmpty()) ids.add(p.getSocietyId());

        if (ids.isEmpty()) { loadAllVotes(); return; }

        final int[] remaining = {ids.size()};
        final Map<String, String> nameMap = new HashMap<>();

        for (String sid : ids) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference societiesCollection = db.collection("societies");
            DocumentReference societyDocument = societiesCollection.document(sid);
            societyDocument.get()
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





    // get votes for all polls at once so we can show counts and highlight which one they picked
    private void loadAllVotes() {
        List<Poll> all = new ArrayList<>(activePolls);
        all.addAll(closedPolls);

        FirebaseUser user = AuthHelpers.currentUser();

        if (all.isEmpty()) {
            publishPolls();
            return;
        }

        final int[] remaining = {all.size()};

        for (Poll poll : all) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference pollsCollection = db.collection("polls");
            DocumentReference pollDocument = pollsCollection.document(poll.getId());
            CollectionReference votesCollection = pollDocument.collection("votes");
            votesCollection.get()
                    .addOnCompleteListener(task -> {
                        if (!isAdded()) { finishOne(remaining); return; }

                        if (task.isSuccessful()) {
                            applyVoteDocs(poll, task.getResult(), user);
                            finishOne(remaining);
                        } else {
                            if (user == null) { finishOne(remaining); return; }
                            DocumentReference userVoteDocument = votesCollection.document(user.getUid());
                            userVoteDocument.get()
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





    private void publishPolls() {
        if (progressPolls != null) progressPolls.setVisibility(View.GONE);

        activeAdapter.updateList(activePolls);


        if (activePolls.isEmpty()) {
            if (tvEmptyPolls != null) {
                tvEmptyPolls.setText("No polls available yet.");
                tvEmptyPolls.setVisibility(View.VISIBLE);
            }
        } else {
            if (tvEmptyPolls != null) tvEmptyPolls.setVisibility(View.GONE);
        }


        if ((isAdmin || isSocietyAdmin) && !closedPolls.isEmpty()) {
            closedAdapter.updateList(closedPolls);
            if (tvClosedPollsLabel != null) tvClosedPollsLabel.setVisibility(View.VISIBLE);
            if (rvClosedPolls != null) rvClosedPolls.setVisibility(View.VISIBLE);
        } else {
            if (tvClosedPollsLabel != null) tvClosedPollsLabel.setVisibility(View.GONE);
            if (rvClosedPolls != null) rvClosedPolls.setVisibility(View.GONE);
        }
    }





    // vote is stored as a doc with the uid as the key so you cant vote twice
    private void submitVote(@NonNull Poll poll, int optionIndex) {
        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please log in to vote.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> voteData = new HashMap<>();
        voteData.put("optionIndex", optionIndex);
        voteData.put("votedAt", Timestamp.now());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference pollsCollection = db.collection("polls");
        DocumentReference pollDocument = pollsCollection.document(poll.getId());
        CollectionReference votesCollection = pollDocument.collection("votes");
        DocumentReference userVoteDocument = votesCollection.document(user.getUid());
        userVoteDocument.set(voteData)
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





    private void deletePoll(@NonNull Poll poll) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference pollsCollection = db.collection("polls");
        DocumentReference pollDocument = pollsCollection.document(poll.getId());
        pollDocument.delete()
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

    // closing just sets endsAt to a second ago so isClosed() returns true
    private void closePoll(@NonNull Poll poll) {
        com.google.firebase.Timestamp pastTime =
                new com.google.firebase.Timestamp(com.google.firebase.Timestamp.now().getSeconds() - 1, 0);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference pollsCollection = db.collection("polls");
        DocumentReference pollDocument = pollsCollection.document(poll.getId());
        pollDocument.update("endsAt", pastTime)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    activePolls.remove(poll);
                    poll.setEndsAt(pastTime);
                    if (isAdmin || isSocietyAdmin) closedPolls.add(poll);
                    publishPolls();
                    Toast.makeText(requireContext(), "Poll closed.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to close poll: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    @NonNull
    private String safeString(@Nullable String value, @NonNull String fallback) {

        return (value != null && !TextHelpers.isBlank(value)) ? value.trim() : fallback;
    }
}
