package com.example.societyhive_test5;

import android.graphics.Color;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatConversationFragment extends Fragment {

    private final List<Message> messages = new ArrayList<>();
    private MessageAdapter adapter;

    public ChatConversationFragment() {
        super(R.layout.fragment_chat_conversation);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String chatTitle = "Chat";
        String chatColor = "#8D2E3A";

        if (getArguments() != null) {
            chatTitle = getArguments().getString("chatTitle", "Chat");
            chatColor = getArguments().getString("chatColor", "#8D2E3A");
        }

        TextView tvHeader = view.findViewById(R.id.tvChatHeaderTitle);
        View headerBar = view.findViewById(R.id.viewChatHeaderAccent);
        tvHeader.setText(chatTitle);

        try {
            headerBar.setBackgroundColor(Color.parseColor(chatColor));
        } catch (IllegalArgumentException e) {
            headerBar.setBackgroundColor(Color.parseColor("#8D2E3A"));
        }

        RecyclerView rv = view.findViewById(R.id.rvMessages);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setHasFixedSize(false);

        adapter = new MessageAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        seedDummyMessages();
        adapter.updateList(messages);
        rv.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));

        EditText etMessage = view.findViewById(R.id.etMessage);
        View btnSend = view.findViewById(R.id.btnSendMessage);

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (text.isEmpty()) return;

            messages.add(new Message("m" + System.currentTimeMillis(), text, true));
            adapter.updateList(messages);
            etMessage.setText("");
            rv.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
        });
    }

    private void seedDummyMessages() {
        if (!messages.isEmpty()) return;

        messages.add(new Message("m1", "I’m not sure what to write.", false));
        messages.add(new Message("m2", "Same.", true));
        messages.add(new Message("m3", "Hi...", false));
        messages.add(new Message("m4", "Hello.", true));
    }
}
