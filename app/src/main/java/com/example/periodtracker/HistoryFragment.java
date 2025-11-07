package com.example.periodtracker;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";

    private LinearLayout historyContainer;
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        // Add a container for dynamically adding history cards
        historyContainer = view.findViewById(R.id.history_container);
        if (historyContainer == null) {
            // If no container in XML, create one
            historyContainer = new LinearLayout(requireContext());
            historyContainer.setOrientation(LinearLayout.VERTICAL);
            ((ViewGroup) view).addView(historyContainer);
        }

        loadSymptomHistory();

        return view;
    }

    private void loadSymptomHistory() {
        String uid = mAuth.getCurrentUser().getUid();

        dbRef.child("symptoms").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<Map<String, Object>> historyList = new ArrayList<>();

                    for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("date", dateSnapshot.child("date").getValue(String.class));
                        entry.put("mood", dateSnapshot.child("mood").getValue(String.class));
                        entry.put("flow", dateSnapshot.child("flow").getValue(String.class));

                        List<String> symptoms = new ArrayList<>();
                        if (dateSnapshot.child("physicalSymptoms").exists()) {
                            for (DataSnapshot symptom : dateSnapshot.child("physicalSymptoms").getChildren()) {
                                symptoms.add(symptom.getValue(String.class));
                            }
                        }
                        entry.put("symptoms", symptoms);
                        entry.put("notes", dateSnapshot.child("notes").getValue(String.class));

                        historyList.add(entry);
                    }

                    // Sort by date (newest first)
                    Collections.reverse(historyList);

                    displayHistory(historyList);
                } else {
                    showNoDataMessage();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading history: " + error.getMessage());
                Toast.makeText(requireContext(),
                        "Error loading history",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayHistory(List<Map<String, Object>> historyList) {
        historyContainer.removeAllViews();

        if (historyList.isEmpty()) {
            showNoDataMessage();
            return;
        }

        for (Map<String, Object> entry : historyList) {
            CardView card = createHistoryCard(entry);
            historyContainer.addView(card);
        }
    }

    private CardView createHistoryCard(Map<String, Object> entry) {
        CardView card = new CardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 24);
        card.setLayoutParams(cardParams);
        card.setRadius(20f);
        card.setCardElevation(6f);
        card.setContentPadding(32, 32, 32, 32);

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);

        // Date
        TextView dateText = new TextView(requireContext());
        dateText.setText(entry.get("date").toString());
        dateText.setTextSize(18);
        dateText.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
        dateText.setPadding(0, 0, 0, 16);
        content.addView(dateText);

        // Mood
        if (entry.get("mood") != null && !entry.get("mood").equals("Not specified")) {
            TextView moodText = new TextView(requireContext());
            moodText.setText("Mood: " + entry.get("mood"));
            moodText.setTextSize(15);
            content.addView(moodText);
        }

        // Flow
        if (entry.get("flow") != null && !entry.get("flow").equals("Not specified")) {
            TextView flowText = new TextView(requireContext());
            flowText.setText("Flow: " + entry.get("flow"));
            flowText.setTextSize(15);
            content.addView(flowText);
        }

        // Symptoms
        @SuppressWarnings("unchecked")
        List<String> symptoms = (List<String>) entry.get("symptoms");
        if (symptoms != null && !symptoms.isEmpty()) {
            TextView symptomsText = new TextView(requireContext());
            symptomsText.setText("Symptoms: " + String.join(", ", symptoms));
            symptomsText.setTextSize(15);
            content.addView(symptomsText);
        }

        // Notes
        if (entry.get("notes") != null && !entry.get("notes").toString().isEmpty()) {
            TextView notesText = new TextView(requireContext());
            notesText.setText("Notes: " + entry.get("notes"));
            notesText.setTextSize(14);
            notesText.setPadding(0, 8, 0, 0);
            content.addView(notesText);
        }

        card.addView(content);
        return card;
    }

    private void showNoDataMessage() {
        historyContainer.removeAllViews();

        TextView noDataText = new TextView(requireContext());
        noDataText.setText("No symptom history yet.\nStart logging your symptoms!");
        noDataText.setTextSize(16);
        noDataText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        noDataText.setPadding(0, 100, 0, 0);

        historyContainer.addView(noDataText);
    }
}