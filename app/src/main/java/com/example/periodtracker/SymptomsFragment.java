package com.example.periodtracker;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SymptomsFragment extends Fragment {

    private static final String TAG = "SymptomsFragment";

    private RadioGroup rgMoods, rgFlow;
    private CheckBox cbCramps, cbHeadache, cbBloating, cbAcne, cbBackpain, cbBreastTenderness;
    private EditText etNotes;
    private Button btnSaveSymptoms;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_symptoms, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        rgMoods = view.findViewById(R.id.rg_moods);
        rgFlow = view.findViewById(R.id.rg_flow);

        cbCramps = view.findViewById(R.id.cb_cramps);
        cbHeadache = view.findViewById(R.id.cb_headache);
        cbBloating = view.findViewById(R.id.cb_bloating);
        cbAcne = view.findViewById(R.id.cb_acne);
        cbBackpain = view.findViewById(R.id.cb_backpain);
        cbBreastTenderness = view.findViewById(R.id.cb_breast_tenderness);

        etNotes = view.findViewById(R.id.et_notes);
        btnSaveSymptoms = view.findViewById(R.id.btn_save_symptoms);

        btnSaveSymptoms.setOnClickListener(v -> saveSymptoms());

        return view;
    }

    private void saveSymptoms() {
        String uid = mAuth.getCurrentUser().getUid();

        // Get selected mood
        String mood = getMoodFromRadioGroup();

        // Get selected flow
        String flow = getFlowFromRadioGroup();

        // Get physical symptoms
        ArrayList<String> physicalSymptoms = getPhysicalSymptoms();

        // Get notes
        String notes = etNotes.getText().toString().trim();

        // Create timestamp for this entry
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        // Create symptom entry
        Map<String, Object> symptomEntry = new HashMap<>();
        symptomEntry.put("date", dateKey);
        symptomEntry.put("timestamp", timestamp);
        symptomEntry.put("mood", mood);
        symptomEntry.put("flow", flow);
        symptomEntry.put("physicalSymptoms", physicalSymptoms);
        symptomEntry.put("notes", notes);

        Log.d(TAG, "Saving symptoms: " + symptomEntry.toString());

        // Save to Firebase under symptoms node
        dbRef.child("symptoms").child(uid).child(dateKey)
                .setValue(symptomEntry)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(),
                            "Symptoms saved successfully!",
                            Toast.LENGTH_SHORT).show();
                    clearForm();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save symptoms: " + e.getMessage());
                    Toast.makeText(requireContext(),
                            "Failed to save: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private String getMoodFromRadioGroup() {
        int selectedId = rgMoods.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_happy) return "Happy";
        if (selectedId == R.id.rb_sad) return "Sad";
        if (selectedId == R.id.rb_anxious) return "Anxious";
        if (selectedId == R.id.rb_tired) return "Tired";
        if (selectedId == R.id.rb_irritable) return "Irritable";
        return "Not specified";
    }

    private String getFlowFromRadioGroup() {
        int selectedId = rgFlow.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_light) return "Light";
        if (selectedId == R.id.rb_moderate) return "Moderate";
        if (selectedId == R.id.rb_heavy) return "Heavy";
        return "Not specified";
    }

    private ArrayList<String> getPhysicalSymptoms() {
        ArrayList<String> symptoms = new ArrayList<>();
        if (cbCramps.isChecked()) symptoms.add("Cramps");
        if (cbHeadache.isChecked()) symptoms.add("Headache");
        if (cbBloating.isChecked()) symptoms.add("Bloating");
        if (cbAcne.isChecked()) symptoms.add("Acne");
        if (cbBackpain.isChecked()) symptoms.add("Back Pain");
        if (cbBreastTenderness.isChecked()) symptoms.add("Breast Tenderness");
        return symptoms;
    }

    private void clearForm() {
        rgMoods.clearCheck();
        rgFlow.clearCheck();
        cbCramps.setChecked(false);
        cbHeadache.setChecked(false);
        cbBloating.setChecked(false);
        cbAcne.setChecked(false);
        cbBackpain.setChecked(false);
        cbBreastTenderness.setChecked(false);
        etNotes.setText("");
    }
}