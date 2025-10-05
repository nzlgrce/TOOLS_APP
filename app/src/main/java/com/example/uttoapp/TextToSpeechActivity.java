package com.example.uttoapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;

public class TextToSpeechActivity extends AppCompatActivity {

    private EditText editTextInput;
    private Button btnSpeak, btnPauseContinue, btnClear;
    private ImageButton btnSelectFile;
    private TextToSpeech textToSpeech;

    private ActivityResultLauncher<String[]> filePickerLauncher;

    private String[] words;       // Split text into words
    private int currentWordIndex = 0;
    private boolean isPaused = false;
    private boolean isSpeaking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_to_speech);

        // Initialize views
        editTextInput = findViewById(R.id.editTextInput);
        btnSpeak = findViewById(R.id.btnSpeak);
        btnPauseContinue = findViewById(R.id.btnPauseContinue);
        btnClear = findViewById(R.id.btnClear);
        btnSelectFile = findViewById(R.id.btnSelectFile);

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show();
                }

                // Listen for progress updates
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        isSpeaking = true;
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        isSpeaking = false;
                        currentWordIndex++;
                        // Speak next word automatically if not paused
                        if (!isPaused && currentWordIndex < words.length) {
                            speakSentence(words[currentWordIndex]);
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        isSpeaking = false;
                    }
                });
            } else {
                Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_SHORT).show();
            }
        });

        btnSpeak.setOnClickListener(v -> {
            String text = editTextInput.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(this, "Please enter text", Toast.LENGTH_SHORT).show();
                return;
            }
            isPaused = false;
            currentWordIndex = 0;
            String[] sentences = text.split("(?<=[.!?])\\s+");
            words = sentences;
            speakSentence(words[currentWordIndex]);
            btnPauseContinue.setBackgroundTintList(getColorStateList(android.R.color.holo_red_dark));
        });

        btnPauseContinue.setOnClickListener(v -> {
            if (isPaused) {
                continueSpeech();
            } else {
                pauseSpeech();
            }
        });

        btnClear.setOnClickListener(v -> {
            // Stop any ongoing speech
            if (textToSpeech != null && textToSpeech.isSpeaking()) {
                textToSpeech.stop();
            }

            // Reset variables
            isPaused = false;
            isSpeaking = false;
            currentWordIndex = 0;
            words = null;

            // Clear the text field
            editTextInput.setText("");

            // Reset pause/continue button
            btnPauseContinue.setText("Pause");
            btnPauseContinue.setBackgroundTintList(getColorStateList(android.R.color.holo_red_dark));

            Toast.makeText(this, "Text cleared", Toast.LENGTH_SHORT).show();
        });


        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                String text = readFile(uri);
                if (!TextUtils.isEmpty(text.trim())) {
                    editTextInput.setText(text.trim());
                } else {
                    Toast.makeText(this, "File is empty or unreadable", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSelectFile.setOnClickListener(v -> {
            filePickerLauncher.launch(new String[]{
                    "text/plain",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            });
        });

        // Hide file button when text appears
        editTextInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSelectFile.setVisibility(s.length() > 0 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Image Button Back
        ImageButton backBtn = findViewById(R.id.imageButtonBack);
        backBtn.setOnClickListener(v -> {
            finish();
        });
    }

    // Speak individual word
    private void speakSentence(String sentence) {
        if (textToSpeech == null) return;

        sentence = sentence.replaceAll("\\s+", " ").trim();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, "tts_sentence_" + currentWordIndex);
        } else {
            textToSpeech.speak(sentence, TextToSpeech.QUEUE_FLUSH, null);
        }
    }


    // ⏸ Pause
    private void pauseSpeech() {
        if (textToSpeech != null && isSpeaking) {
            textToSpeech.stop();
            isPaused = true;
            isSpeaking = false;
            btnPauseContinue.setText("Continue");
            btnPauseContinue.setBackgroundTintList(getColorStateList(android.R.color.holo_green_dark));
        }
    }

    private void continueSpeech() {
        if (isPaused && words != null && currentWordIndex < words.length) {
            isPaused = false;
            speakSentence(words[currentWordIndex]);
            btnPauseContinue.setText("Pause");
            btnPauseContinue.setBackgroundTintList(getColorStateList(android.R.color.holo_red_dark));
        }
    }

    private String readFile(Uri uri) {
        String mimeType = getContentResolver().getType(uri);
        try {
            if (mimeType != null) {
                if (mimeType.equals("text/plain")) {
                    return readTextFile(uri);
                } else if (mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                    return readDocxFile(uri);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private String readTextFile(Uri uri) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        }
        return builder.toString();
    }

    private String readDocxFile(Uri uri) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph para : paragraphs) {
                builder.append(para.getText()).append("\n");
            }
        }
        return builder.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
