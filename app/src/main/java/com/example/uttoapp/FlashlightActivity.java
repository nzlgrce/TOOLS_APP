package com.example.uttoapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class FlashlightActivity extends AppCompatActivity {

    private SwitchCompat switchBack, switchFront, switchBoth;
    private CameraManager cameraManager;
    private String backCameraId, frontCameraId;
    private boolean hasFlash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.flashlight_activity);

        // Link SwitchCompat views
        switchBack = findViewById(R.id.switchBackFlashlight);
        switchFront = findViewById(R.id.switchFrontFlashlight);
        switchBoth = findViewById(R.id.switchBothFlashlight);

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
            Intent intent = new Intent(FlashlightActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void setupSwitchListeners() {
        // Back switch
        switchBack.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                toggleFlash(backCameraId, isChecked);

                if (isChecked) { // Ensure mutual exclusivity
                    switchFront.setChecked(false);
                    switchBoth.setChecked(false);
                }
            }
        });

        // Front switch
        switchFront.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                toggleFlash(frontCameraId, isChecked);

                if (isChecked) {
                    switchBack.setChecked(false);
                    switchBoth.setChecked(false);
                }
            }
        });

        // Both switch
        switchBoth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                toggleFlash(backCameraId, isChecked);
                toggleFlash(frontCameraId, isChecked);

                if (isChecked) {
                    switchBack.setChecked(false);
                    switchFront.setChecked(false);
                }
            }
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
        // Turn off all lights when exiting activity
        try {
            if (backCameraId != null) cameraManager.setTorchMode(backCameraId, false);
            if (frontCameraId != null) cameraManager.setTorchMode(frontCameraId, false);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
