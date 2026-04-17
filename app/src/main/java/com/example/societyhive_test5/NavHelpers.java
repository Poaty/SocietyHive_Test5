package com.example.societyhive_test5;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

public final class NavHelpers {

    private NavHelpers() {}

    public static void navigate(Fragment fragment, @IdRes int destinationId) {
        NavController navController = NavHostFragment.findNavController(fragment);
        navController.navigate(destinationId);
    }

    public static void navigate(Fragment fragment, @IdRes int destinationId, android.os.Bundle args) {
        NavController navController = NavHostFragment.findNavController(fragment);
        navController.navigate(destinationId, args);
    }

    public static void navigateUp(Fragment fragment) {
        NavController navController = NavHostFragment.findNavController(fragment);
        navController.navigateUp();
    }
}
