package com.example.uttoapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class StopwatchActivity extends AppCompatActivity {

    private TextView textViewTime;
    private Button btnStartPause, btnLap, btnReset;
    private RecyclerView recyclerViewLaps;

    private boolean isRunning = false;
    private long startTime = 0L;
    private long timeInMillis = 0L;
    private long timeBuffer = 0L;
    private long updateTime = 0L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<String> lapList = new ArrayList<>();
    private LapAdapter lapAdapter;

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            timeInMillis = System.currentTimeMillis() - startTime;
            updateTime = timeBuffer + timeInMillis;

            int seconds = (int) (updateTime / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            int milliseconds = (int) (updateTime % 1000) / 10; // 2 digits
            seconds = seconds % 60;
            minutes = minutes % 60;

            textViewTime.setText(String.format(Locale.getDefault(),
                    "%02d:%02d:%02d.%02d", hours, minutes, seconds, milliseconds));

            handler.postDelayed(this, 10);
        }
    };

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stopwatch);

        textViewTime = findViewById(R.id.textViewTime);
        btnStartPause = findViewById(R.id.btnStartPause);
        btnLap = findViewById(R.id.btnLap);
        btnReset = findViewById(R.id.btnReset);
        recyclerViewLaps = findViewById(R.id.recyclerViewLaps);

        // RecyclerView setup
        lapAdapter = new LapAdapter(lapList);
        recyclerViewLaps.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewLaps.setAdapter(lapAdapter);

        // Start / Pause button
        btnStartPause.setOnClickListener(v -> {
            if (!isRunning) {
                startTime = System.currentTimeMillis();
                handler.post(runnable);
                isRunning = true;
                btnStartPause.setText("Pause");
                btnStartPause.setBackgroundTintList(getColorStateList(android.R.color.holo_orange_light));
                Toast.makeText(this, "Stopwatch started", Toast.LENGTH_SHORT).show();
            } else {
                timeBuffer += System.currentTimeMillis() - startTime;
                handler.removeCallbacks(runnable);
                isRunning = false;
                btnStartPause.setText("Resume");
                btnStartPause.setBackgroundTintList(getColorStateList(android.R.color.holo_green_light));
                Toast.makeText(this, "Stopwatch paused", Toast.LENGTH_SHORT).show();
            }
        });

        // Lap button
        btnLap.setOnClickListener(v -> {
            if (isRunning) {
                String lapTime = textViewTime.getText().toString();
                lapList.add(0, lapTime); // Add newest lap at the top
                lapAdapter.notifyItemInserted(0);
                recyclerViewLaps.scrollToPosition(0);
            } else {
                Toast.makeText(this, "Start the stopwatch first", Toast.LENGTH_SHORT).show();
            }
        });

        // Reset button
        btnReset.setOnClickListener(v -> {
            handler.removeCallbacks(runnable);
            isRunning = false;
            startTime = 0L;
            timeInMillis = 0L;
            timeBuffer = 0L;
            updateTime = 0L;
            textViewTime.setText("00:00:00.00");
            btnStartPause.setText("Start");
            btnStartPause.setBackgroundTintList(getColorStateList(android.R.color.holo_green_light));
            lapList.clear();
            lapAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Stopwatch reset", Toast.LENGTH_SHORT).show();
        });

        // Image Button Back
        ImageButton backBtn = findViewById(R.id.imageButtonBack);
        backBtn.setOnClickListener(v -> {
            finish();
        });
    }
}
