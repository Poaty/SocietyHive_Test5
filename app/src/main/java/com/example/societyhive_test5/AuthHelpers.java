package com.example.societyhive_test5;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// thin wrapper around FirebaseAuth so fragments don't each import it directly
public final class AuthHelpers {

    private AuthHelpers() {}

    public static FirebaseUser currentUser() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        return auth.getCurrentUser();
    }

    public static void signOut() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signOut();
    }
}
