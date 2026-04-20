package com.example.societyhive_test5;

import android.graphics.Color;
import android.view.View;
import android.widget.EditText;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatConversationFragment extends Fragment {

    private final List<Message> messages = new ArrayList<>();
    private final Map<String, String> photoCache = new HashMap<>();
    private MessageAdapter adapter;
    private RecyclerView rv;

    private String societyId;
    private String currentUserName = "Member";

    private ListenerRegistration messageListener;

    public ChatConversationFragment() {
        super(R.layout.fragment_chat_conversation);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // get args passed from wherever we navigated from
        String chatTitle = "Chat";
        String chatColor = "#8D2E3A";
        societyId = null;

        if (getArguments() != null) {
            chatTitle = getArguments().getString("chatTitle", "Chat");
            chatColor = getArguments().getString("chatColor", "#8D2E3A");
            societyId = getArguments().getString("societyId", null);
        }

        TextView tvHeader = view.findViewById(R.id.tvChatHeaderTitle);
        View headerBar = view.findViewById(R.id.viewChatHeaderAccent);
        tvHeader.setText(chatTitle);
        try {
            headerBar.setBackgroundColor(Color.parseColor(chatColor));
        } catch (IllegalArgumentException e) {
            headerBar.setBackgroundColor(Color.parseColor("#8D2E3A"));
        }

        rv = view.findViewById(R.id.rvMessages);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setHasFixedSize(false);

        adapter = new MessageAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        EditText etMessage = view.findViewById(R.id.etMessage);
        View btnSend = view.findViewById(R.id.btnSendMessage);

        btnSend.setOnClickListener(v -> {
            String text = TextHelpers.trimmed(etMessage);
            if (text.isEmpty()) return;
            // TODO add a max length (maybe 1000 chars?) before someone pastes a whole book in here
            etMessage.setText("");
            postMessage(text);
        });

        // resolve the display name before attaching the listener so outgoing messages
        // already have the right senderName instead of the default "Member"
        loadDisplayName(() -> {
            if (societyId != null && !societyId.isEmpty()) {
                attachMessageStream();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
    }


    // real-time listener — fires on every new message so the chat stays live
    private void attachMessageStream() {
        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null || societyId == null) return;

        final String myUid = user.getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference societies = db.collection("societies");
        DocumentReference societyDoc = societies.document(societyId);
        CollectionReference msgCol = societyDoc.collection("messages");
        messageListener = msgCol
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;

                    if (error != null) {
                        Toast.makeText(requireActivity(),
                                "Chat error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null) return;

                    // TODO: clearing and rebuilding the whole list on every snapshot is wasteful once
                    //  chats get long. should switch to DiffUtil + documentChanges() at some point
                    messages.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Message msg = new Message();
                        msg.setId(doc.getId());
                        msg.setText(safeString(doc.getString("text")));
                        msg.setSenderId(safeString(doc.getString("senderId")));
                        msg.setSenderName(safeString(doc.getString("senderName")));
                        msg.setTimestamp(doc.getTimestamp("timestamp"));
                        msg.setSentByMe(myUid.equals(msg.getSenderId()));
                        messages.add(msg);
                    }

                    fetchMissingAvatars(messages);
                });
    }


    // fetch profile pics for senders we haven't seen yet — cached so we don't re-fetch on every update
    private void fetchMissingAvatars(@NonNull List<Message> msgs) {
        java.util.Set<String> toFetch = new java.util.HashSet<>();
        for (Message m : msgs) {
            if (!m.isSentByMe() && !photoCache.containsKey(m.getSenderId())) {
                toFetch.add(m.getSenderId());
            }
        }

        if (toFetch.isEmpty()) {
            renderMessages(msgs);
            return;
        }

        java.util.concurrent.atomic.AtomicInteger remaining =
                new java.util.concurrent.atomic.AtomicInteger(toFetch.size());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference users = db.collection("users");
        for (String uid : toFetch) {
            DocumentReference userRef = users.document(uid);
            userRef.get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null
                                && task.getResult().exists()) {
                            String url = task.getResult().getString("profileImageUrl");
                            photoCache.put(uid, url != null ? url : "");
                        } else {
                            photoCache.put(uid, "");
                        }
                        if (remaining.decrementAndGet() == 0 && isAdded()) {
                            renderMessages(msgs);
                        }
                    });
        }
    }

    private void renderMessages(@NonNull List<Message> msgs) {
        for (Message m : msgs) {
            if (!m.isSentByMe()) {
                m.setSenderPhotoUrl(photoCache.getOrDefault(m.getSenderId(), ""));
            }
        }
        adapter.updateList(msgs);
        if (!msgs.isEmpty()) rv.scrollToPosition(msgs.size() - 1);
    }

    private void postMessage(@NonNull String text) {
        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null || societyId == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        data.put("senderId", user.getUid());
        data.put("senderName", currentUserName);
        data.put("timestamp", Timestamp.now());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference societyDoc = db.collection("societies").document(societyId);
        CollectionReference messagesCollection = societyDoc.collection("messages");
        messagesCollection.add(data)
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "couldn't send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void loadDisplayName(@NonNull Runnable onReady) {
        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null) {
            onReady.run();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usersCollection = db.collection("users");
        DocumentReference userDoc = usersCollection.document(user.getUid());
        userDoc.get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    String name = doc.getString("fullName");

                    if (name != null && !TextHelpers.isBlank(name)) {
                        currentUserName = name.trim();
                    }
                    onReady.run();
                })
                .addOnFailureListener(e -> onReady.run());
    }


    @NonNull
    private String safeString(String value) {
        return value != null ? value : "";
    }
}
