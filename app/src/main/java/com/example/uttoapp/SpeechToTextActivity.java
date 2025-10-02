package com.example.uttoapp;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class SpeechToTextActivity extends AppCompatActivity {

    private ImageButton btnButtonSpeechtoText;
    private TextView textViewSpeechText;

    private ActivityResultLauncher<Intent> speechToTextLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_to_text);

        btnButtonSpeechtoText = findViewById(R.id.btnButtonSpeechtoText);
        textViewSpeechText = findViewById(R.id.textViewSpeechText);

        // Register ActivityResultLauncher for speech recognition
        speechToTextLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            textViewSpeechText.setText(matches.get(0)); // Show first recognized result
                        }
                    } else {
                        Toast.makeText(this, "Speech recognition failed", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Button click to start Speech Recognition
        btnButtonSpeechtoText.setOnClickListener(v -> startSpeechToText());
    }

    private void startSpeechToText() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);

        speechToTextLauncher.launch(intent);
    }
}
