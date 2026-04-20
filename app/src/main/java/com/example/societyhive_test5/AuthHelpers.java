package com.example.societyhive_test5;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// thin wrapper around FirebaseAuth so fragments don't each import it directly
// TODO half the callers forget this can return null. maybe add a requireUser() that throws a
//  nicer exception, or have it route back to the login activity automatically
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
