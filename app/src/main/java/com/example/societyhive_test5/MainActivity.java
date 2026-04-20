package com.example.societyhive_test5;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessaging;

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
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationPermission();
        fetchAndSaveFcmToken();

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



            NavOptions bottomNavOptions = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.homeFragment, false, false)
                    .setRestoreState(false)
                    .build();




            // guard against double-tapping a nav item triggering two navigations at once
            // TODO: this flag is resetting synchronously, so the guard only actually works because nav is sync too.
            //  if we ever animate between destinations this will need to be moved to a proper debounce
            final boolean[] isNavigating = {false};

            bottomNav.setOnItemSelectedListener(item -> {
                if (isNavigating[0]) return true;
                isNavigating[0] = true;
                try {
                    navController.navigate(item.getItemId(), null, bottomNavOptions);
                } catch (Exception ignored) {
                    // swallowing IllegalArgumentException for unknown destination id — happens when the
                    // nav graph reloads mid-gesture. not a real error, but come back and log it later
                }
                isNavigating[0] = false;
                return true;
            });


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
            AuthHelpers.signOut();

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





    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    private void fetchAndSaveFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token ->
                        MyFirebaseMessagingService.saveTokenToFirestore(token));
    }

}
