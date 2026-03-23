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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Society group chat screen.
 *
 * Reads and writes messages from:
 *   societies/{societyId}/messages/{messageId}
 *
 * Message fields:
 *   text       (String)
 *   senderId   (String)
 *   senderName (String)
 *   timestamp  (Timestamp)
 *
 * Messages load in real-time — when another user sends a message on a
 * different device/emulator it appears here instantly without refreshing.
 */
public class ChatConversationFragment extends Fragment {

    private final List<Message> messages = new ArrayList<>();
    private MessageAdapter adapter;
    private RecyclerView rv;

    private String societyId;
    private String currentUserName = "Member"; // resolved from Firestore

    private ListenerRegistration messageListener;

    public ChatConversationFragment() {
        super(R.layout.fragment_chat_conversation);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ---- Read navigation arguments ----
        String chatTitle = "Chat";
        String chatColor = "#8D2E3A";
        societyId = null;

        if (getArguments() != null) {
            chatTitle = getArguments().getString("chatTitle", "Chat");
            chatColor = getArguments().getString("chatColor", "#8D2E3A");
            societyId = getArguments().getString("societyId", null);
        }

        // ---- Header ----
        TextView tvHeader = view.findViewById(R.id.tvChatHeaderTitle);
        View headerBar = view.findViewById(R.id.viewChatHeaderAccent);
        tvHeader.setText(chatTitle);
        try {
            headerBar.setBackgroundColor(Color.parseColor(chatColor));
        } catch (IllegalArgumentException e) {
            headerBar.setBackgroundColor(Color.parseColor("#8D2E3A"));
        }

        // ---- RecyclerView ----
        rv = view.findViewById(R.id.rvMessages);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setHasFixedSize(false);

        adapter = new MessageAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        // ---- Send button ----
        EditText etMessage = view.findViewById(R.id.etMessage);
        View btnSend = view.findViewById(R.id.btnSendMessage);

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (text.isEmpty()) return;
            etMessage.setText("");
            sendMessage(text);
        });

        // ---- Load current user name then start listening ----
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

    // -------------------------------------------------------------------------
    // Real-time message listener
    // -------------------------------------------------------------------------

    private void startListening() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || societyId == null) return;

        final String myUid = user.getUid();

        messageListener = FirebaseFirestore.getInstance()
                .collection("societies")
                .document(societyId)
                .collection("messages")
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

                    adapter.updateList(messages);

                    // Scroll to the bottom
                    if (!messages.isEmpty()) {
                        rv.scrollToPosition(messages.size() - 1);
                    }
                });
    }

    // -------------------------------------------------------------------------
    // Sending a message
    // -------------------------------------------------------------------------

    private void sendMessage(@NonNull String text) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || societyId == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        data.put("senderId", user.getUid());
        data.put("senderName", currentUserName);
        data.put("timestamp", Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection("societies")
                .document(societyId)
                .collection("messages")
                .add(data)
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Send failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        // On success the snapshot listener fires automatically, updating the UI
    }

    // -------------------------------------------------------------------------
    // Resolve the signed-in user's display name from Firestore
    // -------------------------------------------------------------------------

    private void resolveCurrentUserName(@NonNull Runnable onReady) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            onReady.run();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    String name = doc.getString("fullName");
                    if (name != null && !name.trim().isEmpty()) {
                        currentUserName = name.trim();
                    }
                    onReady.run();
                })
                .addOnFailureListener(e -> onReady.run());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @NonNull
    private String safeString(@Nullable String value) {
        return value != null ? value : "";
    }
}
