package com.example.societyhive_test5;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatsFragment extends Fragment {

    private final List<Chat> allChats = new ArrayList<>();
    private final List<Chat> filteredChats = new ArrayList<>();
    private ChatAdapter adapter;
    private View rootView;


    private final List<com.google.firebase.firestore.ListenerRegistration> listeners =
            new ArrayList<>();

    public ChatsFragment() {
        super(R.layout.fragment_chats);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;

        RecyclerView rv = view.findViewById(R.id.rvChats);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setHasFixedSize(true);

        adapter = new ChatAdapter(
                new ArrayList<>(),
                chat -> {
                    android.os.Bundle b = new android.os.Bundle();
                    b.putString("societyId", chat.getId());
                    b.putString("chatTitle", chat.getTitle());
                    b.putString("chatColor", chat.getSocietyColor());
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.chatConversationFragment, b);
                }
        );

        rv.setAdapter(adapter);
        hookSearch(view);
        loadChatsForUser();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        for (com.google.firebase.firestore.ListenerRegistration reg : listeners) {
            reg.remove();
        }
        listeners.clear();
        rootView = null;
    }





    // admin sees all societies, regular users only see ones they joined
    private void loadChatsForUser() {
        FirebaseUser user = AuthHelpers.currentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (!isAdded()) return;

                    boolean isAdmin = "admin".equalsIgnoreCase(userDoc.getString("role"));

                    if (isAdmin) {

                        db.collection("societies")
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    if (!isAdded()) return;
                                    allChats.clear();
                                    if (querySnapshot.isEmpty()) { applySearch(); return; }
                                    AtomicInteger remaining = new AtomicInteger(querySnapshot.size());
                                    for (QueryDocumentSnapshot societyDoc : querySnapshot) {
                                        addChatFromSociety(societyDoc, db, remaining);
                                    }
                                });
                        return;
                    }


                    List<String> societyIds = (List<String>) userDoc.get("societyIds");
                    if (societyIds == null || societyIds.isEmpty()) {
                        allChats.clear();
                        applySearch();
                        return;
                    }

                    allChats.clear();
                    AtomicInteger remaining = new AtomicInteger(societyIds.size());

                    for (String societyId : societyIds) {
                        db.collection("societies")
                                .document(societyId)
                                .get()
                                .addOnSuccessListener(societyDoc -> {
                                    if (!isAdded()) return;
                                    addChatFromSociety(societyDoc, db, remaining);
                                });
                    }
                });
    }


    private void addChatFromSociety(
            @NonNull DocumentSnapshot societyDoc,
            @NonNull FirebaseFirestore db,
            @NonNull AtomicInteger remaining) {

        if (!societyDoc.exists()) {
            checkAllLoaded(remaining);
            return;
        }

        String societyId = societyDoc.getId();
        String name = societyDoc.getString("name");
        String colorHex = societyDoc.getString("hexColor");
        String iconUrl = societyDoc.getString("iconUrl");
        if (iconUrl == null) iconUrl = "";


        if (name == null || TextHelpers.isBlank(name)) name = "Society Chat";
        if (colorHex == null || TextHelpers.isBlank(colorHex)) colorHex = "#8D2E3A";

        final String finalName = name;
        final String finalColor = colorHex;
        final String finalIconUrl = iconUrl;


        // placeholder while we wait for the last message
        Chat placeholder = new Chat(societyId, finalName, "Loading…", "", finalColor, finalIconUrl);
        allChats.add(placeholder);
        checkAllLoaded(remaining);

        // live listener so new messages update the preview without refresh
        com.google.firebase.firestore.ListenerRegistration reg =
                db.collection("societies")
                        .document(societyId)
                        .collection("messages")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(1)
                        .addSnapshotListener((snapshots, error) -> {
                            if (!isAdded()) return;
                            if (error != null || snapshots == null || snapshots.isEmpty()) {
                                updateChatPreview(societyId, finalName, "No messages yet", "", finalColor, finalIconUrl);
                                return;
                            }

                            DocumentSnapshot lastMsg = snapshots.getDocuments().get(0);
                            String text = lastMsg.getString("text");
                            String senderName = lastMsg.getString("senderName");


                            String preview;
                            FirebaseUser me = AuthHelpers.currentUser();
                            String senderId = lastMsg.getString("senderId");

                            if (me != null && me.getUid().equals(senderId)) {
                                preview = "You: " + (text != null ? text : "");
                            } else {
                                preview = (senderName != null && !senderName.isEmpty())
                                        ? senderName + ": " + (text != null ? text : "")
                                        : (text != null ? text : "");
                            }


                            com.google.firebase.Timestamp ts = lastMsg.getTimestamp("timestamp");
                            String timeLabel = formatTimestamp(ts);

                            updateChatPreview(societyId, finalName, preview, timeLabel, finalColor, finalIconUrl);
                        });

        listeners.add(reg);
    }


    private void updateChatPreview(String societyId, String name,
                                   String preview, String time, String color, String iconUrl) {
        for (int i = 0; i < allChats.size(); i++) {
            if (allChats.get(i).getId().equals(societyId)) {
                allChats.set(i, new Chat(societyId, name, preview, time, color, iconUrl));
                applySearch();
                return;
            }
        }

        allChats.add(new Chat(societyId, name, preview, time, color, iconUrl));
        applySearch();
    }

    private void checkAllLoaded(@NonNull AtomicInteger remaining) {
        if (remaining.decrementAndGet() <= 0) {
            applySearch();
        }
    }





    private void hookSearch(@NonNull View view) {
        View et = view.findViewById(R.id.etSearchChats);
        if (!(et instanceof android.widget.EditText)) return;

        android.widget.EditText etSearch = (android.widget.EditText) et;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearch();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applySearch() {
        if (rootView == null || !isAdded()) return;

        String query = "";
        View et = rootView.findViewById(R.id.etSearchChats);
        if (et instanceof android.widget.EditText) {
            query = ((android.widget.EditText) et).getText().toString().trim().toLowerCase(Locale.UK);
        }

        filteredChats.clear();
        for (Chat chat : allChats) {
            if (!query.isEmpty() && !chat.getTitle().toLowerCase(Locale.UK).contains(query)) {
                continue;
            }
            filteredChats.add(chat);
        }

        adapter.updateList(filteredChats);
    }





    // formats timestamp like whatsapp basically
    private String formatTimestamp(@Nullable com.google.firebase.Timestamp ts) {
        if (ts == null) return "";

        java.util.Date msgDate = ts.toDate();
        java.util.Date now = new java.util.Date();

        long diffMs = now.getTime() - msgDate.getTime();
        long diffDays = diffMs / (1000 * 60 * 60 * 24);

        if (diffDays == 0) {

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", Locale.UK);
            return sdf.format(msgDate);
        } else if (diffDays == 1) {
            return "Yesterday";
        } else if (diffDays < 7) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE", Locale.UK);
            return sdf.format(msgDate);
        } else {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM", Locale.UK);
            return sdf.format(msgDate);
        }
    }
}
