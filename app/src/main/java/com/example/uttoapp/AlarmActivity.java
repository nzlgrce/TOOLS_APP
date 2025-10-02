package com.example.uttoapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class AlarmActivity extends AppCompatActivity {

    private TextView textViewClock;
    private Button btnSetAlarm, btnSnooze, btnStop;
    private ImageView imgAlarm;

    private Ringtone ringtone;
    private Vibrator vibrator;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    private int alarmHour, alarmMinute;

    @SuppressLint("ScheduleExactAlarm")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        textViewClock = findViewById(R.id.textViewClock);
        btnSetAlarm = findViewById(R.id.btnSetAlarm);
        btnSnooze = findViewById(R.id.btnSnooze);
        btnStop = findViewById(R.id.btnStop);
        imgAlarm = findViewById(R.id.imgAlarm);

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Show current time in digital format
        Calendar calendar = Calendar.getInstance();
        String currentTime = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        textViewClock.setText(currentTime);

        // Set Alarm Button
        btnSetAlarm.setOnClickListener(v -> openTimePicker());

        // Snooze button (add 5 min)
        btnSnooze.setOnClickListener(v -> {
            if (ringtone != null && ringtone.isPlaying()) {
                ringtone.stop();
            }
            if (vibrator != null) vibrator.cancel();

            scheduleAlarm(5 * 60 * 1000); // snooze for 5 min
            Toast.makeText(this, "Snoozed for 5 minutes", Toast.LENGTH_SHORT).show();
        });

        // Stop button
        btnStop.setOnClickListener(v -> stopAlarm());
    }

    private void openTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        @SuppressLint("ScheduleExactAlarm") TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (TimePicker view, int hourOfDay, int minute1) -> {
                    alarmHour = hourOfDay;
                    alarmMinute = minute1;
                    String timeText = String.format("%02d:%02d", hourOfDay, minute1);
                    textViewClock.setText(timeText);

                    Calendar now = Calendar.getInstance();
                    Calendar setTime = Calendar.getInstance();
                    setTime.set(Calendar.HOUR_OF_DAY, alarmHour);
                    setTime.set(Calendar.MINUTE, alarmMinute);
                    setTime.set(Calendar.SECOND, 0);

                    long delayMillis = setTime.getTimeInMillis() - now.getTimeInMillis();
                    if (delayMillis <= 0) {
                        delayMillis += 24 * 60 * 60 * 1000; // next day
                    }
                    scheduleAlarm(System.currentTimeMillis() + delayMillis);

                    Toast.makeText(this, "Alarm set for " + timeText, Toast.LENGTH_SHORT).show();
                },
                hour, minute, true
        );
        timePickerDialog.show();
    }

    @SuppressLint("ScheduleExactAlarm")
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    private void scheduleAlarm(long triggerAtMillis) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
        );
    }


    private void stopAlarm() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        if (vibrator != null) vibrator.cancel();
        Toast.makeText(this, "Alarm stopped", Toast.LENGTH_SHORT).show();
    }

    // This will be called by AlarmReceiver when time is up
    @SuppressLint("ScheduleExactAlarm")
    public void triggerAlarm() {
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), alarmSound);
        if (ringtone != null) ringtone.play();

        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(2000); // 2 sec vibrate
        }

        // Show alert popup
        new AlertDialog.Builder(this)
                .setTitle("Alarm Ringing")
                .setMessage("Wake up! Do you want to snooze or stop?")
                .setPositiveButton("Snooze", (d, w) -> {
                    if (ringtone != null && ringtone.isPlaying()) ringtone.stop();
                    if (vibrator != null) vibrator.cancel();
                    scheduleAlarm(5 * 60 * 1000); // snooze 5 min
                })
                .setNegativeButton("Stop", (d, w) -> stopAlarm())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent().getBooleanExtra("alarm_trigger", false)) {
            triggerAlarm();
            getIntent().removeExtra("alarm_trigger");
        }
    }

}
