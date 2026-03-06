package com.example.societyhive_test5;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Views
        MaterialToolbar toolbar = findViewById(R.id.mainToolbar);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // Toolbar as ActionBar
        setSupportActionBar(toolbar);

        // NavController from NavHost
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.navHost);

        if (navHostFragment == null) {
            // If this happens, your activity_main.xml id/name is wrong
            return;
        }

        navController = navHostFragment.getNavController();

        // These are your TOP LEVEL destinations (no back arrow on these)
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.homeFragment,
                R.id.eventsFragment,
                R.id.chatsFragment,
                R.id.qrFragment
        ).build();

        // Hook up toolbar + bottom nav
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(bottomNav, navController);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}