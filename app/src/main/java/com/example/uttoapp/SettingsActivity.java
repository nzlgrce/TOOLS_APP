package com.example.uttoapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    private Switch bluetoothSwitch, flashlightSwitch, darkModeSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bluetoothSwitch = findViewById(R.id.switchBluetooth);
        flashlightSwitch = findViewById(R.id.switchFlashlight);
        darkModeSwitch = findViewById(R.id.switchDarkMode);

        // Example action: Bluetooth toggle
        bluetoothSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                Toast.makeText(this, "Bluetooth Enabled (Mock)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth Disabled (Mock)", Toast.LENGTH_SHORT).show();
            }
        });

        // Example action: Flashlight toggle
        flashlightSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(this, "Flashlight Auto-Enable ON", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Flashlight Auto-Enable OFF", Toast.LENGTH_SHORT).show();
            }
        });

        // Example action: Dark Mode toggle
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(this, "Dark Mode Enabled", Toast.LENGTH_SHORT).show();
                // TODO: Apply dark mode theme here
            } else {
                Toast.makeText(this, "Dark Mode Disabled", Toast.LENGTH_SHORT).show();
            }

        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_back) {
                getOnBackPressedDispatcher().onBackPressed();
                return true;
            }
            return false;
        });
    }
}
