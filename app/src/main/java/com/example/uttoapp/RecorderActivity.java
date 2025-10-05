package com.example.uttoapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class RecorderActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_CODE = 1001;
    private static final String RECORDING_FILE_NAME = "Recording_";

    private ImageButton btnRecorder;
    private ListView listViewRecordings;

    MediaRecorder mediaRecorder;
    private String outputFilePath;
    boolean isRecording = false;
    boolean isPaused = false;

    MediaPlayer mediaPlayer;

    private RecordingAdapter adapter; // custom adapter
    private final ArrayList<String> recordingList = new ArrayList<>();
    private File recordingsDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);

        btnRecorder = findViewById(R.id.btnRecorder);
        TextView statusText = findViewById(R.id.textViewRecorder);
        listViewRecordings = findViewById(R.id.listViewRecordings);

        // Directory for recordings
        recordingsDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "MyRecordings");
        if (!recordingsDir.exists()) recordingsDir.mkdirs();


        // Adapter for ListView
        adapter = new RecordingAdapter(this, recordingList, recordingsDir, this);
        listViewRecordings.setAdapter(adapter);

        loadSavedRecordings();

        btnRecorder.setOnClickListener(v -> showRecorderDialog());

        // Back button
        ImageButton imageButtonBack = findViewById(R.id.imageButtonBack);
        imageButtonBack.setOnClickListener(v -> {
            finish();
        });

    }

    // ------------------ Options dialog ------------------
    public void showOptionsDialog(String selectedFileName, File selectedFile) {
        String[] options = {"Rename", "Delete"};

        new AlertDialog.Builder(this)
                .setTitle("Choose Action")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Rename
                            showRenameDialog(selectedFile);
                            break;

                        case 1: // Delete
                            new AlertDialog.Builder(this)
                                    .setTitle("Delete Recording")
                                    .setMessage("Are you sure you want to delete \"" + selectedFileName + "\"?")
                                    .setPositiveButton("Yes", (confirmDialog, w) -> {
                                        if (selectedFile.delete()) {
                                            Toast.makeText(this, "Deleted: " + selectedFileName, Toast.LENGTH_SHORT).show();
                                            loadSavedRecordings();
                                        } else {
                                            Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .setNegativeButton("No", null)
                                    .show();
                            break;
                    }
                })
                .show();
    }

    // ------------------ Rename ------------------
    public void showRenameDialog(File oldFile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Recording");

        final EditText input = new EditText(this);
        input.setHint("Enter new name");
        builder.setView(input);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                if (!newName.endsWith(".m4a")) newName += ".m4a";
                File newFile = new File(oldFile.getParent(), newName);
                if (oldFile.renameTo(newFile)) {
                    Toast.makeText(this, "Renamed to " + newName, Toast.LENGTH_SHORT).show();
                    loadSavedRecordings();
                } else {
                    Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ------------------ Recorder dialog ------------------
    private void showRecorderDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_recorder_controls, null);

        ImageButton btnStart = dialogView.findViewById(R.id.btnStart);
        ImageButton btnPause = dialogView.findViewById(R.id.btnPause);
        ImageButton btnSave = dialogView.findViewById(R.id.btnSave);
        TextView statusView = dialogView.findViewById(R.id.textViewRecorderStatus);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Start / Stop Recording
        btnStart.setOnClickListener(v -> {
            if (!isRecording) {
                if (checkPermissions()) {
                    startRecording();
                    isRecording = true;
                    isPaused = false;
                    statusView.setText("🎙 Recording...");
                    statusView.setTextColor(getColor(android.R.color.holo_red_dark));
                } else {
                    requestPermissions();
                }
            } else {
                stopRecording();
                isRecording = false;
                isPaused = false;
                statusView.setText("🟥 Stopped");
                statusView.setTextColor(getColor(android.R.color.darker_gray));
                loadSavedRecordings(); // Refresh the ListView
            }
        });

        // Pause / Resume Recording
        btnPause.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording) {
                if (!isPaused) {
                    mediaRecorder.pause();
                    isPaused = true;
                    statusView.setText("⏸ Paused");
                    statusView.setTextColor(getColor(android.R.color.holo_orange_dark));
                } else {
                    mediaRecorder.resume();
                    isPaused = false;
                    statusView.setText("🎙 Recording...");
                    statusView.setTextColor(getColor(android.R.color.holo_red_dark));
                }
            } else {
                Toast.makeText(this, "Pause not supported on your device", Toast.LENGTH_SHORT).show();
            }
        });

        // Save Recording (already stopped)
        btnSave.setOnClickListener(v -> {
            if (!isRecording && outputFilePath != null) {
                File recordedFile = new File(outputFilePath);
                addRecordingToMediaStore(recordedFile);
                statusView.setText("💾 Saved successfully");
                statusView.setTextColor(getColor(android.R.color.holo_green_dark));
                loadSavedRecordings();
            } else {
                Toast.makeText(this, "Stop recording first", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }


    private void startRecording() {
        try {
            // Use public Music folder
            recordingsDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MUSIC), "MyRecordings");
            if (!recordingsDir.exists()) recordingsDir.mkdirs();

            // Generate timestamped filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            outputFilePath = new File(recordingsDir, RECORDING_FILE_NAME + timestamp + ".m4a").getAbsolutePath();

            // Release any previous MediaRecorder
            if (mediaRecorder != null) {
                mediaRecorder.release();
                mediaRecorder = null;
            }

            // Initialize MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(outputFilePath);

            mediaRecorder.prepare();
            mediaRecorder.start();

            Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show();

        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this, "Permission denied for microphone", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error preparing recorder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Recording failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------ Stop recording ------------------
    void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;

                // Add recording to MediaStore so it's visible in File Manager
                File recordedFile = new File(outputFilePath);
                addRecordingToMediaStore(recordedFile);

                Toast.makeText(this, "Recording saved to Music/MyRecordings", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Stop recording failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }


    // ------------------ Load recordings ------------------
    public void loadSavedRecordings() {
        recordingList.clear();
        File[] files = recordingsDir.listFiles();
        if (files != null) {
            for (File f : files) recordingList.add(f.getName());
        }
        adapter.notifyDataSetChanged();
    }

    // ------------------ Play recording ------------------
    public void playRecording(String filePath) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();

            Toast.makeText(this, "Playing...", Toast.LENGTH_SHORT).show();

            mediaPlayer.setOnCompletionListener(mp -> {
                Toast.makeText(this, "Playback finished", Toast.LENGTH_SHORT).show();
                mediaPlayer.release();
                mediaPlayer = null;
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Playback failed", Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------ MediaStore ------------------
    private void addRecordingToMediaStore(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, file.getName());
            values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/m4a");
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/MyRecordings");

            getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
        } else {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        }
    }

    // ------------------ Permissions ------------------
    private boolean checkPermissions() {
        int recordPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return recordPermission == PackageManager.PERMISSION_GRANTED &&
                (storagePermission == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_PERMISSION_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_CODE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) startRecording();
            else Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
        }
    }
}
