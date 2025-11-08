package com.example.periodtracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";

    private EditText etName, etAge, etCycleLength;
    private TextView tvSelectedDate, tvEmail;
    private Button btnSelectDate, btnSaveSettings;
    private String selectedDate = null;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        initializeViews(view);

        // Load current user data
        loadCurrentUserData();

        // Date picker
        btnSelectDate.setOnClickListener(v -> showDatePicker());

        // Save button
        btnSaveSettings.setOnClickListener(v -> saveSettings());

        return view;
    }

    private void initializeViews(View view) {
        etName = view.findViewById(R.id.et_name);
        etAge = view.findViewById(R.id.et_age);
        etCycleLength = view.findViewById(R.id.et_cycle_length);
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        btnSelectDate = view.findViewById(R.id.btn_select_date);
        btnSaveSettings = view.findViewById(R.id.btn_save_settings);
        tvEmail = view.findViewById(R.id.tv_email);

        // Display current user email
        String email = mAuth.getCurrentUser().getEmail();
        if (email != null && tvEmail != null) {
            tvEmail.setText(email);
        }
    }

    private void loadCurrentUserData() {
        String uid = mAuth.getCurrentUser().getUid();
        String email = mAuth.getCurrentUser().getEmail();

        dbRef.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    Integer age = snapshot.child("age").getValue(Integer.class);
                    Integer cycleLength = snapshot.child("cycleLength").getValue(Integer.class);
                    String lastPeriodDate = snapshot.child("lastPeriodDate").getValue(String.class);

                    if (name != null) etName.setText(name);
                    if (age != null) etAge.setText(String.valueOf(age));
                    if (cycleLength != null) etCycleLength.setText(String.valueOf(cycleLength));
                    if (lastPeriodDate != null) {
                        selectedDate = lastPeriodDate;
                        tvSelectedDate.setText(lastPeriodDate);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading user data: " + error.getMessage());
            }
        });
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog picker = new DatePickerDialog(requireContext(), (view, y, m, d) -> {
            selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d);
            tvSelectedDate.setText(selectedDate);
        }, year, month, day);
        picker.show();
    }

    private void saveSettings() {
        String name = etName.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String cycleStr = etCycleLength.getText().toString().trim();

        if (name.isEmpty() || ageStr.isEmpty() || cycleStr.isEmpty() || selectedDate == null) {
            Toast.makeText(requireContext(),
                    "Please fill all fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int age, cycleLength;
        try {
            age = Integer.parseInt(ageStr);
            cycleLength = Integer.parseInt(cycleStr);
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Enter valid numbers",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("age", age);
        updates.put("cycleLength", cycleLength);
        updates.put("lastPeriodDate", selectedDate);

        dbRef.child("users").child(uid).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(),
                            "Settings saved successfully!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save settings: " + e.getMessage());
                    Toast.makeText(requireContext(),
                            "Failed to save: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}