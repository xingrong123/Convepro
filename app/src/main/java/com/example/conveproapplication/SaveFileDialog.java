package com.example.conveproapplication;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import java.util.Objects;

public class SaveFileDialog  extends AppCompatDialogFragment {

    private EditText editTextFilename;
    private SaveFileDialogListener listener;
    private Button btnSave;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.save_text_dialog, null);

        editTextFilename = view.findViewById(R.id.saveFileNameEdit);
        btnSave = view.findViewById(R.id.btnDialogSave);
        Button btnAppend = view.findViewById(R.id.btnDialogAppend);
        Button btnClose = view.findViewById(R.id.btnDialogClose);

        Button btnPopup = view.findViewById(R.id.btnDialogToolTip);

        Bundle mArgs = getArguments();
        assert mArgs != null;
        String filename = mArgs.getString("filenameToBeSaved");
        assert filename != null;
        if ( filename.endsWith(".txt") ) {
            editTextFilename.setText(filename.substring(0, filename.length() - 4));
        }

        builder.setView(view);

        final AlertDialog dialog = builder.create();
        dialog.show();


        btnPopup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTips(v);
            }
        });


        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String filename = editTextFilename.getText().toString() + ".txt";
                listener.saveText(filename);
                dialog.dismiss();
            }
        });

        btnAppend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.appendText();
                dialog.dismiss();
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });


        // disable save button if edit text is empty
        if (editTextFilename.getText().toString().trim().isEmpty())
            btnSave.setEnabled(false);
        else
            btnSave.setEnabled(true);

        editTextFilename.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (!editTextFilename.getText().toString().trim().isEmpty()) {
                    btnSave.setEnabled(true);
                }
                else {
                    btnSave.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        return dialog;
    }

    public void showTips (View v){

        LayoutInflater inflater = getLayoutInflater();
        //TODO no idea how to get rid of warning but the app still works
        View popupView = inflater.inflate(R.layout.popupwindow_tooltip, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);

        TextView textViewToolTip = popupWindow.getContentView().findViewById(R.id.textViewToolTip);
        textViewToolTip.setText(R.string.popup_string_save);

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        popupWindow.showAsDropDown(v); //showAtLocation(v, Gravity.NO_GRAVITY, x, y);

        // dismiss the popup window when touched
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        popupWindow.dismiss();
                        break;
                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
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
        void saveText(String filename);
        void appendText();
    }

}
