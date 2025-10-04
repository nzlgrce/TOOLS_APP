package com.example.uttoapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;

public class RecordingAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> recordings;
    private final File recordingsDir;
    private final RecorderActivity activity;

    public RecordingAdapter(Context context, ArrayList<String> recordings, File recordingsDir, RecorderActivity activity) {
        super(context, 0, recordings);
        this.context = context;
        this.recordings = recordings;
        this.recordingsDir = recordingsDir;
        this.activity = activity;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_recording, parent, false);
        }

        String fileName = recordings.get(position);
        TextView txtFileName = convertView.findViewById(R.id.txtFileName);
        ImageButton btnPlay = convertView.findViewById(R.id.btnPlay);
        ImageButton btnStop = convertView.findViewById(R.id.btnStop);

        txtFileName.setText(fileName);

        // Play button
        btnPlay.setOnClickListener(v -> {
            File fileToPlay = new File(recordingsDir, fileName);
            if (fileToPlay.exists()) {
                activity.playRecording(fileToPlay.getAbsolutePath());
            } else {
                Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show();
            }
        });

        // Stop button
        btnStop.setOnClickListener(v -> {
            if (activity.mediaPlayer != null && activity.mediaPlayer.isPlaying()) {
                activity.mediaPlayer.stop();
                activity.mediaPlayer.release();
                activity.mediaPlayer = null;
                Toast.makeText(context, "Playback stopped", Toast.LENGTH_SHORT).show();
            }

            if (activity.mediaRecorder != null && activity.isRecording) {
                activity.stopRecording();
                activity.isRecording = false;
                activity.isPaused = false;
                Toast.makeText(context, "Recording stopped", Toast.LENGTH_SHORT).show();
            }
        });

        // Clicking the filename shows options
        txtFileName.setOnClickListener(v -> {
            File fileClicked = new File(recordingsDir, fileName);
            activity.showOptionsDialog(fileName, fileClicked);
        });

        return convertView;
    }
}
