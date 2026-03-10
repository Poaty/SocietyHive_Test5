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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatsFragment extends Fragment {

    private final List<Chat> allChats = new ArrayList<>();
    private final List<Chat> filteredChats = new ArrayList<>();
    private ChatAdapter adapter;

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
                    b.putString("chatId", chat.getId());
                    b.putString("chatTitle", chat.getTitle());
                    b.putString("chatColor", chat.getSocietyColor());
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.chatConversationFragment, b);
                }
        );

        rv.setAdapter(adapter);

        seedDummyChats();
        applySearch(view);
        hookSearch(view);
    }

    private void seedDummyChats() {
        if (!allChats.isEmpty()) return;

        allChats.add(new Chat("c1", "Motorsport Society", "You: Last night was crazy!", "18:42", "#8D2E3A"));
        allChats.add(new Chat("c2", "Chess Society", "John: Tournament next week?", "17:10", "#2E7D32"));
        allChats.add(new Chat("c3", "Computing Society", "Sarah: Push to GitHub please", "16:25", "#1565C0"));
        allChats.add(new Chat("c4", "Music Society", "You: See you there!", "15:54", "#6A1B9A"));
        allChats.add(new Chat("c5", "Film Society", "Alex: What time is the screening?", "14:08", "#EF6C00"));
        allChats.add(new Chat("c6", "Photography Society", "You: That shot was cold", "Yesterday", "#455A64"));
        allChats.add(new Chat("c7", "Football Society", "Coach: Training at 7", "Yesterday", "#C62828"));
    }

    private void hookSearch(@NonNull View view) {
        View et = view.findViewById(R.id.etSearchChats);
        if (!(et instanceof android.widget.EditText)) return;

        android.widget.EditText etSearch = (android.widget.EditText) et;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearch(view);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applySearch(@NonNull View view) {
        String query = "";
        View et = view.findViewById(R.id.etSearchChats);
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
}
