package com.example.conveproapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TextEditActivity extends AppCompatActivity {

    private TextView fileNameTextView;
    private EditText resultEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_edit);

        String fileName = getIntent().getStringExtra("fileName");
        String fileContent = getIntent().getStringExtra("fileContent");

        ImageButton backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("fileNameback", fileNameTextView.getText().toString());
                resultIntent.putExtra("fileContentback", resultEditText.getText().toString());

                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        fileNameTextView = findViewById(R.id.fileNameTextView);
        fileNameTextView.setText(fileName);

        resultEditText = findViewById(R.id.textResult);
        resultEditText.setText(fileContent);
        resultEditText.addTextChangedListener(saveTextWatcher);
    }

    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("fileNameback", fileNameTextView.getText().toString());
        resultIntent.putExtra("fileContentback", resultEditText.getText().toString());

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    // to disable save button when edit text is empty
    private final TextWatcher saveTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            String filenameFromTextView = fileNameTextView.getText().toString();
            if (!filenameFromTextView.equals(getResources().getString(R.string.text_not_saved))
                    && !filenameFromTextView.endsWith(" (not saved)")) {
                String message = filenameFromTextView + " (not saved)";
                fileNameTextView.setText(message);
            }
        }
    };
}