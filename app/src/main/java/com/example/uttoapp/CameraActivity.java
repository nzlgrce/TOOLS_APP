package com.example.uttoapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private ImageView photoView;
    private Button captureBtn;
    private String currentPhotoPath;
    private Uri photoUri;

    private ImageButton deleteBtn, shareBtn;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);

        photoView = findViewById(R.id.photoView);
        captureBtn = findViewById(R.id.captureButton);

        captureBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
            } else {
                dispatchTakePictureIntent();
            }
        });

        ImageButton backBtn = findViewById(R.id.imageButtonBack);
        backBtn.setOnClickListener(v -> {
            finish();
        });

        shareBtn = findViewById(R.id.imageButtonShare);

        // Hide share button at start
        shareBtn.setEnabled(false);
        shareBtn.setAlpha(0.5f);

        shareBtn.setOnClickListener(v -> {
            if (photoUri != null) {
                new androidx.appcompat.app.AlertDialog.Builder(CameraActivity.this)
                        .setTitle("Share Photo")
                        .setMessage("Do you want to share this photo?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("image/*");
                            shareIntent.putExtra(Intent.EXTRA_STREAM, photoUri);
                            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(shareIntent, "Share image via"));
                        })
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();
            } else {
                Toast.makeText(this, "Take a photo first", Toast.LENGTH_SHORT).show();
            }
        });



        deleteBtn = findViewById(R.id.imageButtonDelete);
        // Hide delete button at start
        deleteBtn.setEnabled(false);
        deleteBtn.setAlpha(0.5f); // faded look when disabled

        deleteBtn.setOnClickListener(v -> {
            if (photoUri != null) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Delete Photo")
                        .setMessage("Do you want to delete this photo?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            int rows = getContentResolver().delete(photoUri, null, null);
                            if (rows > 0) {
                                photoUri = null;
                                photoView.setImageDrawable(null); // clear preview
                                Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();

                                // Disable delete button again
                                deleteBtn.setEnabled(false);
                                deleteBtn.setAlpha(0.5f);
                                shareBtn.setEnabled(false);
                                shareBtn.setAlpha(0.5f);
                            } else {
                                Toast.makeText(this, "Failed to delete photo", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .show();
            } else {
                Toast.makeText(this, "No photo to delete", Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create an entry in MediaStore so image is visible in gallery
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "IMG_" + timeStamp + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/UttoApp");
        }

        photoUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (photoUri == null) {
            Toast.makeText(this, "Failed to create MediaStore entry", Toast.LENGTH_SHORT).show();
            return;
        }

        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission is required to take pictures", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                if (photoUri != null) {
                    try {
                        photoView.setImageBitmap(BitmapFactory.decodeStream(
                                getContentResolver().openInputStream(photoUri)
                        ));
                        Toast.makeText(this, "Photo saved to gallery", Toast.LENGTH_SHORT).show();
                        shareBtn.setEnabled(true);
                        shareBtn.setAlpha(1.0f);
                        deleteBtn.setEnabled(true);
                        deleteBtn.setAlpha(1.0f);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load photo", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "Picture wasn't taken", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
