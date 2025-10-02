package com.example.uttoapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class FlashlightActivity extends AppCompatActivity {

    private ToggleButton toggleBack, toggleFront, toggleBoth;
    private TextView textBack, textFront, textBoth;
    private CameraManager cameraManager;
    private String backCameraId;
    private String frontCameraId;
    private boolean hasFlash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.flashlight_activity);

        toggleBack = findViewById(R.id.toggleButtonBackFlashlight);
        toggleFront = findViewById(R.id.toggleButtonFrontFlashlight);
        toggleBoth = findViewById(R.id.toggleButtonBothFlashlight);

        textBack = findViewById(R.id.textViewBackFlashlight);
        textFront = findViewById(R.id.textViewFrontFlashlight);
        textBoth = findViewById(R.id.textViewBothFlashlight);

        // Check device flashlight
        hasFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (!hasFlash) {
            Toast.makeText(this, "No flashlight available", Toast.LENGTH_SHORT).show();
            toggleBack.setEnabled(false);
            toggleFront.setEnabled(false);
            toggleBoth.setEnabled(false);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                for (String id : cameraManager.getCameraIdList()) {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                    Boolean flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);

                    if (flashAvailable != null && flashAvailable) {
                        if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                            backCameraId = id;
                        } else if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                            frontCameraId = id;
                        }
                    }
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        // 🔹 Back toggle
        toggleBack.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                toggleFlash(backCameraId, isChecked);
                textBack.setText(isChecked ? "Back ON" : "Back OFF");
                if (isChecked) {
                    toggleFront.setChecked(false);
                    toggleBoth.setChecked(false);
                }
            }
        });

        // 🔹 Front toggle
        toggleFront.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                toggleFlash(frontCameraId, isChecked);
                textFront.setText(isChecked ? "Front ON" : "Front OFF");
                if (isChecked) {
                    toggleBack.setChecked(false);
                    toggleBoth.setChecked(false);
                }
            }
        });

        // 🔹 Both toggle
        toggleBoth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                toggleFlash(backCameraId, isChecked);
                toggleFlash(frontCameraId, isChecked);
                textBoth.setText(isChecked ? "Both ON" : "Both OFF");
                if (isChecked) {
                    toggleBack.setChecked(false);
                    toggleFront.setChecked(false);
                }
            }
        });

        // Use BottomNavigationView
        // Use BottomNavigationView
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener((item) -> { // Corrected line
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
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void toggleFlash(String cameraId, boolean status) {
        if (cameraId == null) return; // Camera not available
        try {
            cameraManager.setTorchMode(cameraId, status);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not toggle flashlight", Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Turn off all lights
        try {
            if (backCameraId != null) cameraManager.setTorchMode(backCameraId, false);
            if (frontCameraId != null) cameraManager.setTorchMode(frontCameraId, false);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
