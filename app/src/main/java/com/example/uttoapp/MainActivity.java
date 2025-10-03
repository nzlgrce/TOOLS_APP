package com.example.uttoapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Use BottomNavigationView
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_back) {
                // Proper back handling
                getOnBackPressedDispatcher().onBackPressed();
                return true;

            }  else if (id == R.id.action_settings) {
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            }
            return false;
        });

        ImageButton bluetoothBtn = findViewById(R.id.imageButtonBluetooth);
        bluetoothBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BluetoothActivity.class);
            startActivity(intent);
        });

        ImageButton cameraBtn = findViewById(R.id.imageCameraButton);
        cameraBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        });

        ImageButton recorderBtn = findViewById(R.id.imageRecorderButton);
        recorderBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecorderActivity.class);
            startActivity(intent);
        });

        ImageButton flashBtn = findViewById(R.id.imageButtonFlashlight);
        flashBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FlashlightActivity.class);
            startActivity(intent);
        });

        ImageButton textSpeechBtn = findViewById(R.id.imageButtonTextToSpeech);
        textSpeechBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TextToSpeechActivity.class);
            startActivity(intent);
        });

        ImageButton speechToTextBtn = findViewById(R.id.imageBtnSpeechtoText);
        speechToTextBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SpeechToTextActivity.class);
            startActivity(intent);
        });

        ImageButton timerBtn = findViewById(R.id.imageButtonTimer);
        timerBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TimerActivity.class);
            startActivity(intent);
        });

        ImageButton alarmBtn = findViewById(R.id.imageButtonAlarm);
        alarmBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AlarmActivity.class);
            startActivity(intent);
        });

        ImageButton hotspotBtn = findViewById(R.id.imageButtonCalculator);
        hotspotBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalculatorActivity.class);
            startActivity(intent);
        });
    }
}
