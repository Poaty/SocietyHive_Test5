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
            etMessage.setText("");
            sendMessage(text);
        });


        resolveCurrentUserName(() -> {
            if (societyId != null && !societyId.isEmpty()) {
                startListening();
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





    // real-time listener, updates whenever a new message is sent
    private void startListening() {
        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null || societyId == null) return;

        final String myUid = user.getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference societiesCollection = db.collection("societies");
        DocumentReference societyDocument = societiesCollection.document(societyId);
        CollectionReference messagesCollection = societyDocument.collection("messages");
        messageListener = messagesCollection
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;

                    if (error != null) {
                        Toast.makeText(requireContext(),
                                "Chat error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null) return;

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

                    enrichWithPhotos(messages);
                });
    }





    // fetch profile pics for senders we havent seen before
    private void enrichWithPhotos(@NonNull List<Message> msgs) {
        java.util.Set<String> toFetch = new java.util.HashSet<>();
        for (Message m : msgs) {
            if (!m.isSentByMe() && !photoCache.containsKey(m.getSenderId())) {
                toFetch.add(m.getSenderId());
            }
        }

        if (toFetch.isEmpty()) {
            applyPhotosAndShow(msgs);
            return;
        }

        java.util.concurrent.atomic.AtomicInteger remaining =
                new java.util.concurrent.atomic.AtomicInteger(toFetch.size());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usersCollection = db.collection("users");
        for (String uid : toFetch) {
            DocumentReference userDocument = usersCollection.document(uid);
            userDocument.get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null
                                && task.getResult().exists()) {
                            String url = task.getResult().getString("profileImageUrl");
                            photoCache.put(uid, url != null ? url : "");
                        } else {
                            photoCache.put(uid, "");
                        }
                        if (remaining.decrementAndGet() == 0 && isAdded()) {
                            applyPhotosAndShow(msgs);
                        }
                    });
        }
    }

    private void applyPhotosAndShow(@NonNull List<Message> msgs) {
        for (Message m : msgs) {
            if (!m.isSentByMe()) {
                m.setSenderPhotoUrl(photoCache.getOrDefault(m.getSenderId(), ""));
            }
        }
        adapter.updateList(msgs);
        if (!msgs.isEmpty()) rv.scrollToPosition(msgs.size() - 1);
    }

    private void sendMessage(@NonNull String text) {
        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null || societyId == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        data.put("senderId", user.getUid());
        data.put("senderName", currentUserName);
        data.put("timestamp", Timestamp.now());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference societiesCollection = db.collection("societies");
        DocumentReference societyDocument = societiesCollection.document(societyId);
        CollectionReference messagesCollection = societyDocument.collection("messages");
        messagesCollection.add(data)
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Send failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }





    private void resolveCurrentUserName(@NonNull Runnable onReady) {
        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null) {
            onReady.run();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usersCollection = db.collection("users");
        DocumentReference userDocument = usersCollection.document(user.getUid());
        userDocument.get()
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
    private String safeString(@Nullable String value) {
        return value != null ? value : "";
    }
}
