package com.example.uttoapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;

public class FlashlightActivity extends AppCompatActivity {

    private SwitchCompat switchBack, switchFront, switchBoth;
    private CameraManager cameraManager;
    private String backCameraId, frontCameraId;
    private boolean hasFlash;

    private TextView textBackFlashlight, textFrontFlashlight, textBothFlashlight;
    private CardView cardBackFlashlight, cardFrontFlashlight, cardBothFlashlight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.flashlight_activity);

        // Link SwitchCompat views
        switchBack = findViewById(R.id.switchBackFlashlight);
        switchFront = findViewById(R.id.switchFrontFlashlight);
        switchBoth = findViewById(R.id.switchBothFlashlight);

        textBackFlashlight = findViewById(R.id.textBackFlashlight);
        textFrontFlashlight = findViewById(R.id.textFrontFlashlight);
        textBothFlashlight = findViewById(R.id.textBothFlashlight);

        cardBackFlashlight = findViewById(R.id.cardBackFlashlight);
        cardFrontFlashlight = findViewById(R.id.cardFrontFlashlight);
        cardBothFlashlight = findViewById(R.id.cardBothFlashlight);

        // Check device flashlight availability
        hasFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (!hasFlash) {
            Toast.makeText(this, "No flashlight available", Toast.LENGTH_SHORT).show();
            switchBack.setEnabled(false);
            switchFront.setEnabled(false);
            switchBoth.setEnabled(false);
            return;
        }

        // Setup camera IDs
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

        setupSwitchListeners();

        // Image Button Back
        ImageButton backBtn = findViewById(R.id.imageButtonBack);
        backBtn.setOnClickListener(v -> {
            finish();
        });


    }

    @SuppressLint("SetTextI18n")
    private void setupSwitchListeners() {
        switchBack.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                toggleFlash(backCameraId, isChecked);
                if (isChecked) {
                    switchFront.setChecked(false);
                    switchBoth.setChecked(false);
                }
            }

            // UI color and text update
            textBackFlashlight.setText(isChecked ? "Back Flashlight - ON" : "Back Flashlight");
            textBackFlashlight.setTextColor(Color.parseColor(isChecked ? "#1B5E20" : "#333F79"));
            cardBackFlashlight.setCardBackgroundColor(Color.parseColor(isChecked ? "#C8E6C9" : "#FFFFFF"));
        });

        switchFront.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                toggleFlash(frontCameraId, isChecked);
                if (isChecked) {
                    switchBack.setChecked(false);
                    switchBoth.setChecked(false);
                }
            }

            textFrontFlashlight.setText(isChecked ? "Front Flashlight - ON" : "Front Flashlight");
            textFrontFlashlight.setTextColor(Color.parseColor(isChecked ? "#1B5E20" : "#333F79"));
            cardFrontFlashlight.setCardBackgroundColor(Color.parseColor(isChecked ? "#C8E6C9" : "#FFFFFF"));
        });

        switchBoth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                toggleFlash(backCameraId, isChecked);
                toggleFlash(frontCameraId, isChecked);
                if (isChecked) {
                    switchBack.setChecked(false);
                    switchFront.setChecked(false);
                }
            }

            textBothFlashlight.setText(isChecked ? "Both Flashlights - ON" : "Both Flashlights");
            textBothFlashlight.setTextColor(Color.parseColor(isChecked ? "#1B5E20" : "#333F79"));
            cardBothFlashlight.setCardBackgroundColor(Color.parseColor(isChecked ? "#C8E6C9" : "#FFFFFF"));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Turn off all lights when exiting activity
        try {
            if (backCameraId != null) cameraManager.setTorchMode(backCameraId, false);
            if (frontCameraId != null) cameraManager.setTorchMode(frontCameraId, false);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
