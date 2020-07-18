package com.example.conveproapplication;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import java.util.Objects;

public class LoadFileDialog extends AppCompatDialogFragment implements AdapterView.OnItemClickListener {

    String selectedFilename = null;
    ListView listViewTextFiles;
    private LoadFilenameDialogListener listener;
    AlertDialog dialog;

    private Button btnLoad;


    @NonNull
    public Dialog onCreateDialog (Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.load_text_dialog, null);

        Bundle mArgs = getArguments();
        assert mArgs != null;
        String[] filenames = Objects.requireNonNull(mArgs.getString("filenamesB")).split("/");
        final boolean loadNotAppend = mArgs.getBoolean("loadNotAppend");
        listViewTextFiles = view.findViewById(R.id.listViewFileArray);
        ArrayAdapter<String> filenameAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_activated_1, filenames);
        listViewTextFiles.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listViewTextFiles.setAdapter(filenameAdapter);
        listViewTextFiles.setOnItemClickListener(this);

        TextView textViewLoadFile = view.findViewById(R.id.textViewLoadFile);
        btnLoad = view.findViewById(R.id.btnDialogLoad);
        Button btnClose = view.findViewById(R.id.btnDialogCloseLoad);

        Button btnPopup = view.findViewById(R.id.btnDialogToolTipLoad);

        builder.setView(view);
        dialog = builder.create();
        dialog.show();

        textViewLoadFile.setText(loadNotAppend ? "load file" : "append file");
        btnLoad.setText(loadNotAppend ? "load" : "append");

        if (selectedFilename == null)
            btnLoad.setEnabled(false);

        btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loadNotAppend) {
                    listener.loadText(selectedFilename);
                }
                else {
                    listener.appendText(selectedFilename);
                }
                dialog.dismiss();
            }
        });

        btnPopup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTipsLoad(v, loadNotAppend);
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });


        return dialog;
    }

    public void showTipsLoad (View v, boolean loadNotAppend){


        LayoutInflater inflater = getLayoutInflater();
        View popupView = inflater.inflate(R.layout.popupwindow_tooltip, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        popupWindow.showAsDropDown(v);

        TextView textViewToolTip = popupWindow.getContentView().findViewById(R.id.textViewToolTip);
        if(loadNotAppend)
            textViewToolTip.setText(R.string.popup_string_load);
        else
            textViewToolTip.setText(R.string.popup_string_append);

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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //enable load/append button when item is selected
        btnLoad.setEnabled(true);
        selectedFilename = parent.getItemAtPosition(position).toString();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (LoadFilenameDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                "must implement LoadFilenameDialogListener!");
        }

    }

    public interface LoadFilenameDialogListener {
        void loadText(String filename);

        void appendText(String filename);
    }
}
