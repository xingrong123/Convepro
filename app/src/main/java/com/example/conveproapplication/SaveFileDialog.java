package com.example.conveproapplication;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import java.io.IOException;
import java.util.Objects;

public class SaveFileDialog  extends AppCompatDialogFragment {

    private EditText editTextFilename;
    private SaveFileDialogListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.save_text_dialog, null);

        editTextFilename = view.findViewById(R.id.saveFileNameEdit);

        Bundle mArgs = getArguments();
        assert mArgs != null;
        String filename = mArgs.getString("filenameA");
        assert filename != null;
        if ( filename.endsWith(".txt") ) {
            editTextFilename.setText(filename.substring(0, filename.length() - 4));
        }

        builder.setView(view)
                .setTitle("Save file")
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNeutralButton("append", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            listener.appendText();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String filename = editTextFilename.getText().toString() + ".txt";
                        try {
                            listener.saveText(filename);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });



        final AlertDialog dialog = builder.create();
        dialog.show();

        ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE)
                .setEnabled(false);

        editTextFilename.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (!editTextFilename.getText().toString().trim().isEmpty()) {
                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
                else {
                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (SaveFileDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    "must implement SaveFileDialogListener!");
        }

    }

    public interface SaveFileDialogListener{
        void saveText(String filename) throws IOException;

        void appendText() throws IOException;
    }

}
