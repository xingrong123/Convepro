package com.example.conveproapplication;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import java.util.Objects;

public class LoadImageDialog  extends AppCompatDialogFragment {

    private AlertDialog dialog;
    private LoadImageDialogListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.load_image_dialog, null);

        Button cameraBtn = view.findViewById(R.id.btn_dialog_camera);
        Button storageBtn = view.findViewById(R.id.btn_dialog_storage);
        Button btnClose = view.findViewById(R.id.btn_dialog_close_load_img);

        builder.setView(view);
        dialog = builder.create();
        dialog.show();

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.loadImageSource(true);
                dialog.dismiss();
            }
        });
        storageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.loadImageSource(false);
                dialog.dismiss();
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

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (LoadImageDialog.LoadImageDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    "must implement LoadImageDialogListener!");
        }

    }

    public interface LoadImageDialogListener{
        void loadImageSource(boolean cameraNotStorage);
    }


}
