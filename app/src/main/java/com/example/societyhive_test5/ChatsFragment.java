package com.example.societyhive_test5;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatsFragment extends Fragment {

    private final List<Chat> allChats = new ArrayList<>();
    private final List<Chat> filteredChats = new ArrayList<>();
    private ChatAdapter adapter;
    // held as a field so filterAndRefresh doesn't need to re-find it on every keystroke
    private EditText etSearch;

    private final List<com.google.firebase.firestore.ListenerRegistration> listeners =
            new ArrayList<>();

    public ChatsFragment() {
        super(R.layout.fragment_chats);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        // wire the search box inline — only set up once so a separate method felt like overkill
        View searchView = view.findViewById(R.id.etSearchChats);
        if (searchView instanceof EditText) {
            etSearch = (EditText) searchView;
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterAndRefresh();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        refreshChatList();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // detach snapshot listeners so they don't fire after the view is gone and cause crashes
        for (com.google.firebase.firestore.ListenerRegistration l : listeners) {
            l.remove();
        }
        listeners.clear();
        etSearch = null;
    }


    // admin sees all societies, regular users only see ones they joined
    private void refreshChatList() {
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
                                    if (querySnapshot.isEmpty()) { filterAndRefresh(); return; }
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
                        filterAndRefresh();
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
            AtomicInteger remaining) {

        if (!societyDoc.exists()) {
            if (remaining.decrementAndGet() <= 0) filterAndRefresh();
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

        // placeholder while we wait for the last message to come back from firestore
        Chat placeholder = new Chat(societyId, finalName, "Loading…", "", finalColor, finalIconUrl);
        allChats.add(placeholder);
        if (remaining.decrementAndGet() <= 0) filterAndRefresh();

        // live listener so new messages update the preview without a manual refresh
        // TODO: track a per-society lastSeenTimestamp here so we can show unread badges
        com.google.firebase.firestore.ListenerRegistration l =
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
                            updateChatPreview(societyId, finalName, preview, formatTimestamp(ts), finalColor, finalIconUrl);
                        });

        listeners.add(l);
    }


    private void updateChatPreview(String societyId, String name,
                                   String preview, String time, String color, String iconUrl) {
        for (int i = 0; i < allChats.size(); i++) {
            if (allChats.get(i).getId().equals(societyId)) {
                allChats.set(i, new Chat(societyId, name, preview, time, color, iconUrl));
                filterAndRefresh();
                return;
            }
        }
        allChats.add(new Chat(societyId, name, preview, time, color, iconUrl));
        filterAndRefresh();
    }

    private void filterAndRefresh() {
        if (!isAdded()) return;

        String query = etSearch != null
                ? etSearch.getText().toString().trim().toLowerCase(Locale.UK)
                : "";

        filteredChats.clear();
        for (Chat c : allChats) {
            if (!query.isEmpty() && !c.getTitle().toLowerCase(Locale.UK).contains(query)) continue;
            filteredChats.add(c);
        }

        adapter.updateList(filteredChats);
    }


    // formats like whatsapp — calendar-based so the today/yesterday boundary is midnight not rolling 24h
    private String formatTimestamp(@Nullable com.google.firebase.Timestamp ts) {
        if (ts == null) return "";

        Calendar msgCal = Calendar.getInstance();
        msgCal.setTime(ts.toDate());
        Calendar now = Calendar.getInstance();

        boolean sameYear = msgCal.get(Calendar.YEAR) == now.get(Calendar.YEAR);
        int dayGap = now.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR);

        if (sameYear && dayGap == 0) {
            long ageMs = now.getTimeInMillis() - msgCal.getTimeInMillis();
            if (ageMs < 60_000) return "just now";
            return new SimpleDateFormat("HH:mm", Locale.UK).format(ts.toDate());
        } else if (sameYear && dayGap == 1) {
            return "Yesterday";
        } else if (sameYear && dayGap < 7) {
            return new SimpleDateFormat("EEE", Locale.UK).format(ts.toDate());
        } else {
            return new SimpleDateFormat("dd/MM/yy", Locale.UK).format(ts.toDate());
        }
    }
}
