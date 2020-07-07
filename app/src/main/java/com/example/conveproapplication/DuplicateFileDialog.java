package com.example.conveproapplication;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import java.io.IOException;
import java.util.Objects;

public class DuplicateFileDialog extends AppCompatDialogFragment {

    private TextView textView;
    private DuplicateFileDialogListener listener;
    String filename;
    String message;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.duplicate_file_dialog, null);

        textView = view.findViewById(R.id.duplicateMessageTextView);

        Bundle mArgs = getArguments();
        assert mArgs != null;
        filename = mArgs.getString("filenameD");
        message = filename + " already exists!\n" +
                "Do you wish to overwrite\n" +
                "the current file?";

        textView.setText(message);

        builder.setView(view)
                .setTitle("Duplicate file")
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("Overwrite", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            listener.overwriteDuplicateFile(filename);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (DuplicateFileDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    "must implement DuplicateFileDialogListener!");
        }

    }

    public interface DuplicateFileDialogListener{
        void overwriteDuplicateFile(String filename) throws IOException;
    }

}

