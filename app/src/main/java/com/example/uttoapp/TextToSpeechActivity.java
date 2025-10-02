package com.example.uttoapp;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
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
    private Button btnSpeak, btnStopClear;
    private ImageButton btnSelectFile;
    private TextToSpeech textToSpeech;

    private ActivityResultLauncher<String[]> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_to_speech);

        // Initialize views
        editTextInput = findViewById(R.id.editTextInput);
        btnSpeak = findViewById(R.id.btnSpeak);
        btnStopClear = findViewById(R.id.btnStopClear);
        btnSelectFile = findViewById(R.id.btnSelectFile);

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_SHORT).show();
            }
        });

        // Speak button
        btnSpeak.setOnClickListener(v -> {
            String text = editTextInput.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(this, "Please enter text", Toast.LENGTH_SHORT).show();
                return;
            }
            speakText(text);
        });

        // Stop/Clear button
        btnStopClear.setOnClickListener(v -> {
            if (textToSpeech != null && textToSpeech.isSpeaking()) {
                textToSpeech.stop();
            }
            editTextInput.setText("");
        });
        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                String text = readFile(uri);
                if (!TextUtils.isEmpty(text.trim())) {
                    editTextInput.setText(text.trim());
                    speakText(text.trim());
                } else {
                    Toast.makeText(this, "File is empty or unreadable", Toast.LENGTH_SHORT).show();
                }
            }
        });


        // File select button
        btnSelectFile.setOnClickListener(v -> {
            filePickerLauncher.launch(new String[]{
                    "text/plain",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            });
        });
    }

    private void speakText(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1");
        } else {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
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
