package com.example.conveproapplication;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
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

        builder.setView(view)
                .setTitle("Save file")
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

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

        editTextFilename = view.findViewById(R.id.saveFileNameEdit);

        return builder.create();
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
    }

}
