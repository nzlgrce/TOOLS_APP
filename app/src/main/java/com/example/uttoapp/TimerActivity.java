package com.example.uttoapp;

import android.animation.ObjectAnimator;
import android.app.TimePickerDialog;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class TimerActivity extends AppCompatActivity {

    private TextView textViewTimer;
    private ImageButton imageButtonTimer;
    private Button btnResetTimer, btnPickTime;

    private CountDownTimer countDownTimer;
    private long selectedTimeMillis = 0; // total picked time
    private long timeLeftMillis = 0; // remaining time
    private long totalDurationMillis = 0; // total for animation reference
    private boolean isRunning = false;

    private Ringtone ringtone; // For alarm sound

    private ProgressBar progressCircle;
    private ObjectAnimator progressAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        textViewTimer = findViewById(R.id.textViewTimer);
        imageButtonTimer = findViewById(R.id.imageButtonTimer);
        btnResetTimer = findViewById(R.id.btnResetTimer);
        btnPickTime = findViewById(R.id.btnPickTime);
        progressCircle = findViewById(R.id.progressCircle);

        // Ensure a consistent progress range: use 1000 for smoothness
        progressCircle.setMax(1000);
        progressCircle.setProgress(0);

        btnResetTimer.setEnabled(false);
        imageButtonTimer.setEnabled(false);

        btnResetTimer.setOnClickListener(v -> {
            if (selectedTimeMillis > 0) {
                resetTimer();
            }
        });

        // Start / Pause button
        imageButtonTimer.setOnClickListener(v -> {
            if (!isRunning) {
                if (timeLeftMillis > 0) {
                    startTimer(timeLeftMillis);
                } else if (selectedTimeMillis > 0) {
                    startTimer(selectedTimeMillis);
                } else {
                    Toast.makeText(this, "Please set a time first", Toast.LENGTH_SHORT).show();
                }
            } else {
                pauseTimer();
            }
        });

        // Pick time button
        btnPickTime.setOnClickListener(v -> openCustomTimePicker());
    }

    private void openCustomTimePicker() {
        View view = getLayoutInflater().inflate(R.layout.dialog_time_picker, null);

        NumberPicker npHours = view.findViewById(R.id.npHours);
        NumberPicker npMinutes = view.findViewById(R.id.npMinutes);
        NumberPicker npSeconds = view.findViewById(R.id.npSeconds);

        // Set ranges
        npHours.setMinValue(0);
        npHours.setMaxValue(23);
        npMinutes.setMinValue(0);
        npMinutes.setMaxValue(59);
        npSeconds.setMinValue(0);
        npSeconds.setMaxValue(59);

        // Default values
        npHours.setValue(0);
        npMinutes.setValue(1);
        npSeconds.setValue(0);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Set Timer")
                .setView(view)
                .setPositiveButton("OK", (dialog, which) -> {
                    int hours = npHours.getValue();
                    int minutes = npMinutes.getValue();
                    int seconds = npSeconds.getValue();

                    selectedTimeMillis = (hours * 3600L + minutes * 60L + seconds) * 1000L;

                    if (selectedTimeMillis < 5000) {
                        // too short
                        selectedTimeMillis = 0;
                        timeLeftMillis = 0;
                        totalDurationMillis = 0;
                        textViewTimer.setText("00:00");
                        Toast.makeText(this, "Please select at least 5 seconds!", Toast.LENGTH_SHORT).show();

                        btnResetTimer.setEnabled(false);
                        imageButtonTimer.setEnabled(false);
                        if (progressAnimator != null) { progressAnimator.cancel(); progressAnimator = null; }
                        progressCircle.setProgress(0);
                    } else {
                        timeLeftMillis = selectedTimeMillis;
                        totalDurationMillis = selectedTimeMillis;
                        textViewTimer.setText(formatTime(timeLeftMillis / 1000));
                        Toast.makeText(this, "Timer set!", Toast.LENGTH_SHORT).show();

                        // initialize the circle to full
                        progressCircle.setMax(1000);
                        progressCircle.setProgress(1000);

                        btnResetTimer.setEnabled(true);
                        imageButtonTimer.setEnabled(true);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startTimer(long startTimeMillis) {
        // startTimeMillis is the duration we should run now (either selectedTimeMillis or remaining)
        // Use timeLeftMillis to keep sync
        isRunning = true;

        // Cancel any previous animator
        if (progressAnimator != null) {
            progressAnimator.cancel();
            progressAnimator = null;
        }

        // compute starting progress (0..1000) relative to totalDurationMillis
        int startProgress = 1000;
        if (totalDurationMillis > 0) {
            // If we're resuming mid-way, compute progress from timeLeftMillis
            long usedMillis = (totalDurationMillis - startTimeMillis);
            float fractionRemaining = (float) startTimeMillis / (float) totalDurationMillis;
            startProgress = Math.max(0, Math.min(1000, (int) (fractionRemaining * 1000f)));
        }

        // create animator from startProgress -> 0 over duration startTimeMillis
        progressAnimator = ObjectAnimator.ofInt(progressCircle, "progress", startProgress, 0);
        progressAnimator.setDuration(startTimeMillis);
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.start();

        // Start the CountDownTimer for the text updates and final actions
        countDownTimer = new CountDownTimer(startTimeMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMillis = millisUntilFinished;
                textViewTimer.setText(formatTime(timeLeftMillis / 1000));
            }

            @Override
            public void onFinish() {
                isRunning = false;
                timeLeftMillis = 0;
                textViewTimer.setText("00:00");

                // ensure animation ends at 0
                if (progressAnimator != null) {
                    progressAnimator.cancel();
                    progressAnimator = null;
                }
                progressCircle.setProgress(0);

                playAlarm();
                vibratePhone();
                Toast.makeText(TimerActivity.this, "Time's up!", Toast.LENGTH_SHORT).show();
            }
        }.start();

        // change play/pause icon (optional)
        imageButtonTimer.setImageResource(android.R.drawable.ic_media_pause);
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        // stop animator and preserve current progress
        if (progressAnimator != null) {
            progressAnimator.cancel();
            progressAnimator = null;
        }

        isRunning = false;
        imageButtonTimer.setImageResource(android.R.drawable.ic_media_play);
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if (progressAnimator != null) {
            progressAnimator.cancel();
            progressAnimator = null;
        }

        isRunning = false;
        timeLeftMillis = selectedTimeMillis;
        totalDurationMillis = selectedTimeMillis;

        // reset UI
        if (timeLeftMillis > 0) {
            textViewTimer.setText(formatTime(timeLeftMillis / 1000));
            progressCircle.setProgress(1000); // full
        } else {
            textViewTimer.setText("00:00");
            progressCircle.setProgress(0);
        }

        imageButtonTimer.setImageResource(R.drawable.timer_app);
        stopAlarm();
        Toast.makeText(this, "Timer reset", Toast.LENGTH_SHORT).show();
    }

    private String formatTime(long totalSeconds) {
        long hrs = totalSeconds / 3600;
        long mins = (totalSeconds % 3600) / 60;
        long secs = totalSeconds % 60;

        if (hrs > 0) {
            return String.format("%02d:%02d:%02d", hrs, mins, secs);
        } else {
            return String.format("%02d:%02d", mins, secs);
        }
    }

    private void playAlarm() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (notification == null) {
            notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        if (ringtone != null && !ringtone.isPlaying()) {
            ringtone.play();
        }
    }

    private void stopAlarm() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    private void vibratePhone() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(2000); // vibrate for 2 seconds
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
        stopAlarm();
    }
}
