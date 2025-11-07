package com.example.periodtracker;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import android.view.MenuItem;
import com.example.periodtracker.R;
import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        // Load user data for navigation header
        loadUserDataToNavHeader();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.nav_drawer_open, R.string.nav_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Open DashboardFragment by default
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
            navView.setCheckedItem(R.id.nav_dashboard);
        }

        navView.setNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                selectedFragment = new DashboardFragment();
            } else if (itemId == R.id.nav_calendar) {
                selectedFragment = new CalendarFragment();
            } else if (itemId == R.id.nav_symptoms) {
                selectedFragment = new SymptomsFragment();
            } else if (itemId == R.id.nav_history) {
                selectedFragment = new HistoryFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            } else if (itemId == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void loadUserDataToNavHeader() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        View headerView = navView.getHeaderView(0);

        TextView tvNavName = headerView.findViewById(R.id.tv_user_name);
        TextView tvNavEmail = headerView.findViewById(R.id.tv_user_email);
        TextView tvNavAge = headerView.findViewById(R.id.tv_user_age);
        TextView tvNavAvatar = headerView.findViewById(R.id.tv_user_avatar);

        // Set email immediately
        if (email != null) {
            tvNavEmail.setText(email);
        }

        // Load name and age from database
        db.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    Integer age = snapshot.child("age").getValue(Integer.class);

                    if (name != null && !name.isEmpty()) {
                        tvNavName.setText(name);
                        // Set avatar initial (first letter of name)
                        tvNavAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
                    } else {
                        tvNavName.setText("User");
                        tvNavAvatar.setText("U");
                    }

                    if (age != null) {
                        tvNavAge.setText("Age: " + age);
                    } else {
                        tvNavAge.setText("Age: N/A");
                    }
                } else {
                    tvNavName.setText("User");
                    tvNavAvatar.setText("U");
                    tvNavAge.setText("Age: N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvNavName.setText("User");
                tvNavAvatar.setText("U");
                tvNavAge.setText("Age: N/A");
            }
        });
    }
}