package com.example.periodtracker;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import org.threeten.bp.LocalDate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private static final String TAG = "CalendarFragment";

    private MaterialCalendarView calendarView;
    private TextView tvCalendarTitle;
    private final HashSet<CalendarDay> periodDays = new HashSet<>();
    private final HashSet<CalendarDay> fertileDays = new HashSet<>();
    private final HashSet<CalendarDay> ovulationDays = new HashSet<>();

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        calendarView = view.findViewById(R.id.calendarView);
        tvCalendarTitle = view.findViewById(R.id.tv_calendar_title);

        // Initialize ThreeTenABP
        AndroidThreeTen.init(requireContext());

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        // Display today's date
        String todayDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        tvCalendarTitle.setText("My Cycle Calendar 🌸\n" + todayDate);

        // Load real cycle data from Firebase
        loadCycleData();

        // Date click listener
        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                String dateStr = date.getDay() + " " + getMonthName(date.getMonth() - 1) + " " + date.getYear();
                String info = "Selected: " + dateStr;

                if (periodDays.contains(date)) {
                    info = "🩸 Period Day - " + dateStr;
                } else if (fertileDays.contains(date)) {
                    info = "💖 Fertile Window - " + dateStr;
                } else if (ovulationDays.contains(date)) {
                    info = "🥚 Ovulation Day - " + dateStr;
                }

                Toast.makeText(requireContext(), info, Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void loadCycleData() {
        String uid = mAuth.getCurrentUser().getUid();

        dbRef.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer cycleLength = snapshot.child("cycleLength").getValue(Integer.class);
                    String lastPeriodDate = snapshot.child("lastPeriodDate").getValue(String.class);

                    if (cycleLength != null && lastPeriodDate != null) {
                        calculateCycleDates(lastPeriodDate, cycleLength);
                        applyDecorators();
                    } else {
                        Toast.makeText(requireContext(),
                                "Complete your profile to see cycle predictions",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading cycle data: " + error.getMessage());
            }
        });
    }

    private void calculateCycleDates(String lastPeriodDateStr, int cycleLength) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date lastPeriodDate = sdf.parse(lastPeriodDateStr);

            if (lastPeriodDate == null) return;

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(lastPeriodDate);

            // Current period (5 days)
            Calendar periodCal = (Calendar) calendar.clone();
            for (int i = 0; i < 5; i++) {
                periodDays.add(getCalendarDayFromCalendar(periodCal));
                periodCal.add(Calendar.DAY_OF_MONTH, 1);
            }

            // Next period (cycleLength days later)
            Calendar nextPeriodCal = (Calendar) calendar.clone();
            nextPeriodCal.add(Calendar.DAY_OF_MONTH, cycleLength);
            for (int i = 0; i < 5; i++) {
                periodDays.add(getCalendarDayFromCalendar(nextPeriodCal));
                nextPeriodCal.add(Calendar.DAY_OF_MONTH, 1);
            }

            // Fertile window (5 days before ovulation)
            Calendar fertileCal = (Calendar) calendar.clone();
            fertileCal.add(Calendar.DAY_OF_MONTH, cycleLength - 14 - 5);
            for (int i = 0; i < 6; i++) {
                fertileDays.add(getCalendarDayFromCalendar(fertileCal));
                fertileCal.add(Calendar.DAY_OF_MONTH, 1);
            }

            // Ovulation day (14 days before next period)
            Calendar ovulationCal = (Calendar) calendar.clone();
            ovulationCal.add(Calendar.DAY_OF_MONTH, cycleLength - 14);
            ovulationDays.add(getCalendarDayFromCalendar(ovulationCal));

            Log.d(TAG, "Period days marked: " + periodDays.size());
            Log.d(TAG, "Fertile days marked: " + fertileDays.size());
            Log.d(TAG, "Ovulation days marked: " + ovulationDays.size());

        } catch (ParseException e) {
            Log.e(TAG, "Date parsing error: " + e.getMessage());
        }
    }

    /** ✅ Converts java.util.Calendar to CalendarDay using ThreeTenABP */
    private CalendarDay getCalendarDayFromCalendar(Calendar calendar) {
        LocalDate localDate = LocalDate.of(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        return CalendarDay.from(localDate);
    }

    private void applyDecorators() {
        // Period days decorator (Red/Pink)
        calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return periodDays.contains(day);
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.period_day_circle));
            }
        });

        // Fertile days decorator (Light Pink)
        calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return fertileDays.contains(day);
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.fertilie_day_circle));
            }
        });

        // Ovulation day decorator (Green/Teal)
        calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return ovulationDays.contains(day);
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ovulation_day_circle));
            }
        });

        // Today decorator (Dot under today)
        calendarView.addDecorator(new DayViewDecorator() {
            private final CalendarDay today = CalendarDay.today();

            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return day.equals(today);
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.addSpan(new DotSpan(8, Color.parseColor("#E91E63")));
            }
        });
    }

    private String getMonthName(int monthIndex) {
        String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        return months[monthIndex];
    }
}