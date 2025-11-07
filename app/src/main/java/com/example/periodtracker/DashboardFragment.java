package com.example.periodtracker;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";

    private TextView tvWelcomeMessage;
    private TextView tvNextPeriodIn;
    private TextView tvFertileWindowDates;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Initialize views
        tvWelcomeMessage = view.findViewById(R.id.tv_welcome_message);
        tvNextPeriodIn = view.findViewById(R.id.tv_next_period_in);
        tvFertileWindowDates = view.findViewById(R.id.tv_fertile_window_dates);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        // Load user data
        loadUserData();

        return view;
    }

    private void loadUserData() {
        String uid = mAuth.getCurrentUser().getUid();

        Log.d(TAG, "Loading data for UID: " + uid);

        dbRef.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "Data snapshot exists: " + snapshot.toString());

                    // Get user data
                    String name = snapshot.child("name").getValue(String.class);
                    Integer cycleLength = snapshot.child("cycleLength").getValue(Integer.class);
                    String lastPeriodDate = snapshot.child("lastPeriodDate").getValue(String.class);

                    Log.d(TAG, "Name: " + name);
                    Log.d(TAG, "Cycle Length: " + cycleLength);
                    Log.d(TAG, "Last Period Date: " + lastPeriodDate);

                    // Display welcome message
                    if (name != null && !name.isEmpty()) {
                        tvWelcomeMessage.setText("Hello, " + name + "!");
                    } else {
                        tvWelcomeMessage.setText("Hello, User!");
                    }

                    // Calculate and display cycle predictions
                    if (cycleLength != null && lastPeriodDate != null) {
                        calculateCyclePredictions(lastPeriodDate, cycleLength);
                    } else {
                        tvNextPeriodIn.setText("Setup your profile to see predictions");
                        tvFertileWindowDates.setText("No data available");
                        Toast.makeText(requireContext(),
                                "Please complete your profile setup",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "No data found for user");
                    tvWelcomeMessage.setText("Hello, User!");
                    tvNextPeriodIn.setText("No data found");
                    tvFertileWindowDates.setText("Please complete setup");
                    Toast.makeText(requireContext(),
                            "No profile data found. Please complete setup.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                Toast.makeText(requireContext(),
                        "Error loading data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateCyclePredictions(String lastPeriodDateStr, int cycleLength) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date lastPeriodDate = sdf.parse(lastPeriodDateStr);

            if (lastPeriodDate == null) {
                tvNextPeriodIn.setText("Invalid date");
                tvFertileWindowDates.setText("Invalid date");
                return;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(lastPeriodDate);

            // Calculate next period date (lastPeriodDate + cycleLength days)
            calendar.add(Calendar.DAY_OF_MONTH, cycleLength);
            Date nextPeriodDate = calendar.getTime();

            // Calculate days until next period
            long diffInMillis = nextPeriodDate.getTime() - System.currentTimeMillis();
            long daysUntilPeriod = diffInMillis / (1000 * 60 * 60 * 24);

            if (daysUntilPeriod > 0) {
                tvNextPeriodIn.setText("In " + daysUntilPeriod + " days");
            } else if (daysUntilPeriod == 0) {
                tvNextPeriodIn.setText("Today");
            } else {
                tvNextPeriodIn.setText("Period may be late");
            }

            // Calculate fertile window (typically ovulation day ± 5 days)
            // Ovulation typically occurs 14 days before next period
            calendar.setTime(lastPeriodDate);
            calendar.add(Calendar.DAY_OF_MONTH, cycleLength - 14 - 5); // Start of fertile window
            Date fertileStart = calendar.getTime();

            calendar.add(Calendar.DAY_OF_MONTH, 10); // Fertile window is about 10 days
            Date fertileEnd = calendar.getTime();

            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
            String fertileWindowText = displayFormat.format(fertileStart) + " - " +
                    displayFormat.format(fertileEnd);
            tvFertileWindowDates.setText(fertileWindowText);

            Log.d(TAG, "Next period in " + daysUntilPeriod + " days");
            Log.d(TAG, "Fertile window: " + fertileWindowText);

        } catch (ParseException e) {
            Log.e(TAG, "Date parsing error: " + e.getMessage());
            tvNextPeriodIn.setText("Error calculating dates");
            tvFertileWindowDates.setText("Error");
            Toast.makeText(requireContext(),
                    "Error calculating cycle dates",
                    Toast.LENGTH_SHORT).show();
        }
    }
}