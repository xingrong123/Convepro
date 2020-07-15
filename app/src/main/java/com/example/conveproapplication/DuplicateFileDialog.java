package com.example.conveproapplication;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import java.util.Objects;

public class DuplicateFileDialog extends AppCompatDialogFragment {

    private DuplicateFileDialogListener listener;
    private String filename;
    private AlertDialog dialog;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.duplicate_file_dialog, null);

        TextView textView = view.findViewById(R.id.duplicateMessageTextView);
        Button dialogCloseBtn = view.findViewById(R.id.btn_dialog_dup_close);
        Button dialogOverwriteBtn = view.findViewById(R.id.btn_dialog_overwrite);

        Bundle mArgs = getArguments();
        assert mArgs != null;
        filename = mArgs.getString("filenameD");
        String message = filename + " already exists!\n" +
                "Do you wish to overwrite\n" +
                "the current file?";

        textView.setText(message);

        builder.setView(view);

        dialog = builder.create();
        dialog.show();

        dialogCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialogOverwriteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.overwriteDuplicateFile(filename);
                dialog.dismiss();
            }
        });

        return dialog;
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
        void overwriteDuplicateFile(String filename);
    }

}

