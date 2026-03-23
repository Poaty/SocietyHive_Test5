package com.example.societyhive_test5;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.navHost);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            AppBarConfiguration appBarConfiguration =
                    new AppBarConfiguration.Builder(
                            R.id.homeFragment,
                            R.id.eventsFragment,
                            R.id.chatsFragment,
                            R.id.qrFragment
                    ).build();

            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

            // Custom bottom nav setup: always navigate cleanly without saving/restoring
            // secondary screens (Profile, Preferences, Settings) opened from the toolbar.
            NavOptions bottomNavOptions = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.homeFragment, false, false)
                    .setRestoreState(false)
                    .build();

            bottomNav.setOnItemSelectedListener(item -> {
                try {
                    navController.navigate(item.getItemId(), null, bottomNavOptions);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });

            // Keep the bottom nav selected item in sync with the NavController
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                if (id == R.id.homeFragment || id == R.id.eventsFragment
                        || id == R.id.chatsFragment || id == R.id.qrFragment) {
                    bottomNav.setSelectedItemId(id);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_profile) {
            navController.navigate(R.id.profileFragment);
            return true;
        }

        if (id == R.id.menu_preferences) {
            navController.navigate(R.id.preferencesFragment);
            return true;
        }

        if (id == R.id.menu_settings) {
            navController.navigate(R.id.settingsFragment);
            return true;
        }

        if (id == R.id.menu_logout) {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}