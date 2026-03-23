package com.example.societyhive_test5;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Home screen.
 *
 * - Loads the signed-in user's first name from Firestore and shows
 *   "Welcome, <FirstName>" in tvWelcome.
 * - Wires the quick-access dashboard tiles to their nav destinations.
 */
public class HomeFragment extends Fragment {

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadWelcomeName(view);
        wireTiles(view);
    }

    // -------------------------------------------------------------------------
    // Firestore: load user's name
    // -------------------------------------------------------------------------

    private void loadWelcomeName(@NonNull View view) {
        TextView tvWelcome = view.findViewById(R.id.tvWelcome);
        if (tvWelcome == null) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            tvWelcome.setText("Welcome");
            return;
        }

        // Show a placeholder immediately so the screen isn't blank while loading
        tvWelcome.setText("Welcome back");

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return; // fragment may have been detached

                    String fullName = doc.getString("fullName");
                    if (fullName != null && !fullName.trim().isEmpty()) {
                        // Use only the first word (first name) to keep the greeting short
                        String firstName = fullName.trim().split("\\s+")[0];
                        tvWelcome.setText("Welcome, " + firstName);
                    } else {
                        tvWelcome.setText("Welcome");
                    }
                })
                .addOnFailureListener(e -> {
                    // Silent failure — placeholder text stays
                });
    }

    // -------------------------------------------------------------------------
    // Quick-access tile navigation
    // -------------------------------------------------------------------------

    private void wireTiles(@NonNull View view) {
        // Each tile navigates to the matching bottom-nav destination.
        // If a destination doesn't exist in your nav graph yet, comment out that line.

        View tileEvents = view.findViewById(R.id.tileEvents);
        if (tileEvents != null) {
            tileEvents.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.eventsFragment));
        }

        View tileChats = view.findViewById(R.id.tileChats);
        if (tileChats != null) {
            tileChats.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.chatsFragment));
        }

        View tileQr = view.findViewById(R.id.tileQr);
        if (tileQr != null) {
            tileQr.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.qrFragment));
        }

        // Calendar, Polls, Gallery — not yet wired to real destinations.
        // Uncomment and add nav IDs once those fragments exist.
        // view.findViewById(R.id.tileCalendar).setOnClickListener(...);
        // view.findViewById(R.id.tilePolls).setOnClickListener(...);
        // view.findViewById(R.id.tileGallery).setOnClickListener(...);
    }
}
