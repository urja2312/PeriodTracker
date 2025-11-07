package com.example.periodtracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SetupActivity extends AppCompatActivity {

    private static final String TAG = "SetupActivity";

    private EditText etName, etAge, etCycleLength;
    private TextView tvSelectedDate;
    private Button btnSelectDate, btnSaveContinue;
    private String selectedDate = null;

    private FirebaseAuth mAuth;
    private DatabaseReference db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        Log.d(TAG, "SetupActivity onCreate");

        etName = findViewById(R.id.et_name);
        etAge = findViewById(R.id.et_age);
        etCycleLength = findViewById(R.id.et_cycle_length);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        btnSelectDate = findViewById(R.id.btn_select_date);
        btnSaveContinue = findViewById(R.id.btn_save_continue);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "ERROR: No user logged in!");
            Toast.makeText(this, "Error: Not logged in!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Log.d(TAG, "User UID: " + mAuth.getCurrentUser().getUid());
        Log.d(TAG, "User Email: " + mAuth.getCurrentUser().getEmail());

        // DatePicker for last period
        btnSelectDate.setOnClickListener(view -> {
            Log.d(TAG, "Date picker button clicked");
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog picker = new DatePickerDialog(this, (datePicker, y, m, d) -> {
                selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d);
                tvSelectedDate.setText(selectedDate);
                Log.d(TAG, "Date selected: " + selectedDate);
            }, year, month, day);
            picker.show();
        });

        btnSaveContinue.setOnClickListener(view -> {
            Log.d(TAG, "===== SAVE BUTTON CLICKED =====");

            String name = etName.getText().toString().trim();
            String ageStr = etAge.getText().toString().trim();
            String cycleStr = etCycleLength.getText().toString().trim();
            String lastPeriodDate = selectedDate;

            Log.d(TAG, "Name: " + name);
            Log.d(TAG, "Age: " + ageStr);
            Log.d(TAG, "Cycle: " + cycleStr);
            Log.d(TAG, "Date: " + lastPeriodDate);

            // Validation
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Validation failed: Name empty");
                return;
            }

            if (ageStr.isEmpty()) {
                Toast.makeText(this, "Please enter your age", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Validation failed: Age empty");
                return;
            }

            if (cycleStr.isEmpty()) {
                Toast.makeText(this, "Please enter cycle length", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Validation failed: Cycle empty");
                return;
            }

            if (lastPeriodDate == null) {
                Toast.makeText(this, "Please select last period date", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Validation failed: Date not selected");
                return;
            }

            int age;
            int cycleLength;
            try {
                age = Integer.parseInt(ageStr);
                cycleLength = Integer.parseInt(cycleStr);
                Log.d(TAG, "Parsed - Age: " + age + ", Cycle: " + cycleLength);
            } catch (Exception e) {
                Toast.makeText(this, "Enter valid numbers for age and cycle", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Parse error: " + e.getMessage());
                return;
            }

            if (age < 10 || age > 100) {
                Toast.makeText(this, "Please enter valid age (10-100)", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Invalid age range");
                return;
            }

            if (cycleLength < 20 || cycleLength > 40) {
                Toast.makeText(this, "Cycle length should be 20-40 days", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Invalid cycle length");
                return;
            }

            // All validations passed
            String uid = mAuth.getCurrentUser().getUid();
            Log.d(TAG, "Starting Firebase write for UID: " + uid);

            Map<String, Object> profile = new HashMap<>();
            profile.put("name", name);
            profile.put("age", age);
            profile.put("cycleLength", cycleLength);
            profile.put("lastPeriodDate", lastPeriodDate);

            Log.d(TAG, "Data to save: " + profile.toString());

            // Disable button to prevent double-click
            btnSaveContinue.setEnabled(false);
            btnSaveContinue.setText("Saving...");

            db.child("users").child(uid).setValue(profile)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✓✓✓ SUCCESS! Data saved to Firebase");
                        Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();

                        // Small delay to ensure data is written
                        new android.os.Handler().postDelayed(() -> {
                            Log.d(TAG, "Opening MainActivity...");
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }, 500);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "✗✗✗ FIREBASE WRITE FAILED!");
                        Log.e(TAG, "Error: " + e.getMessage());
                        Log.e(TAG, "Error class: " + e.getClass().getName());
                        e.printStackTrace();

                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();

                        // Re-enable button
                        btnSaveContinue.setEnabled(true);
                        btnSaveContinue.setText("Save & Continue");
                    })
                    .addOnCompleteListener(task -> {
                        Log.d(TAG, "Save operation completed. Success: " + task.isSuccessful());
                    });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart - Firebase Database URL: " +
                FirebaseDatabase.getInstance().getReference().toString());
    }
}