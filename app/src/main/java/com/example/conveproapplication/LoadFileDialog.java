package com.example.conveproapplication;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import java.io.IOException;
import java.util.Objects;

public class LoadFileDialog extends AppCompatDialogFragment implements AdapterView.OnItemClickListener {

    String selectedFilename = null;
    ListView listViewTextFiles;
    private LoadFilenameDialogListener listener;

    @NonNull
    public Dialog onCreateDialog (Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.load_text_dialog, null);

        Bundle mArgs = getArguments();
        assert mArgs != null;
        String[] filenames = Objects.requireNonNull(mArgs.getString("filenamesB")).split("/");
        listViewTextFiles = view.findViewById(R.id.listViewFileArray);
        ArrayAdapter<String> filenameAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_activated_1, filenames);
        listViewTextFiles.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listViewTextFiles.setAdapter(filenameAdapter);
        listViewTextFiles.setOnItemClickListener(this);


        builder.setView(view)
                .setTitle("Load File")
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("Load", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String filename = selectedFilename;
                        try {
                            listener.applyTexts(filename);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });





        return builder.create();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        selectedFilename = parent.getItemAtPosition(position).toString();
        Toast.makeText(getContext(), "clicked " + selectedFilename, Toast.LENGTH_SHORT).show();
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
        void applyTexts(String filename) throws IOException;
    }
}
