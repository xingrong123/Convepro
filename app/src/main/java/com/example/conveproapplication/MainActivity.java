package com.example.conveproapplication;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.googlecode.leptonica.android.GrayQuant;
import com.googlecode.leptonica.android.MorphApp;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.leptonica.android.WriteFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    static final int PHOTO_REQUEST_CODE = 1;
    private static int GALLERY_LOAD_IMAGE = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;
    private static final int GALLERY_PERMISSION_REQUEST_CODE = 201;
    private static final String TESSDATA = "tessdata";
    private static final String lang = "eng";

    private TessBaseAPI tessBaseApi;
    TextView textView;
    Uri outputFileUri;
    String result = "empty";


    String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button captureImg = findViewById(R.id.convertBtn);
        if (captureImg != null) {
            captureImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // Checks for camera permission
                    if (checkCameraPermission()) {
                        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                        StrictMode.setVmPolicy(builder.build());
                        startCameraActivity();
                    }
                    else {
                        requestCameraPermission();
                    }
                }
            });
        }

        Button buttonLoadImage = findViewById(R.id.loadBtn);
        buttonLoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (checkGalleryPermission()) {

                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, GALLERY_LOAD_IMAGE);

                }
                else {
                    requestGalleryPermission();
                }
            }
        });

        textView  = findViewById(R.id.textResult);

    }




    /**
     * to get high resolution image from camera
     */
    private void startCameraActivity() {
        try {

            // take photo with camera app
            final Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    Log.e(TAG, Objects.requireNonNull(ex.getMessage()));
                    // Error occurred while creating the File...
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    outputFileUri = Uri.fromFile(photoFile);
                    Log.i(TAG, "uri at startcameraactivity is " + outputFileUri.toString());

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                    startActivityForResult(takePictureIntent, PHOTO_REQUEST_CODE);

                }
            }
        } catch (Exception e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_hhmmss", Locale.getDefault());
        String timeStamp = simpleDateFormat.format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        assert storageDir != null;
        Log.i(TAG, "storageDir is " + storageDir.toString());
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        Log.i(TAG, "image file is " + image.toString());
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        // choose image from camera
        if (requestCode == PHOTO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            doOCR();
        }

        // load image from gallery
        else if (requestCode == GALLERY_LOAD_IMAGE && resultCode == Activity.RESULT_OK) {
            outputFileUri = data.getData();

            doOCR();
        }
        else {
            Toast.makeText(this, "ERROR: Image was not obtained.", Toast.LENGTH_SHORT).show();
        }
    }



    //////////////////////////////////////////////////////////////////////////////////////////////////////
    // Tesseract OCR stuff

    private void doOCR() {
        prepareTesseract();
        Log.i(TAG, "uri is " + outputFileUri.toString());
        startOCR(outputFileUri);
    }

//    /**
//     * Prepare directory on external storage
//     *
//     * @param path
//     * @throws Exception
//     */
    private void prepareDirectory(String path) {

        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "ERROR: Creation of directory " + path + " failed, " +
                        "check does Android Manifest have permission to write to external storage.");
            }
        } else {
            Log.i(TAG, "Created directory " + path);
        }
    }


    private void prepareTesseract() {
        try {
            String DATA_PATH = this.getApplicationContext().getFilesDir() + "/TesseractSample/";
            prepareDirectory(DATA_PATH + TESSDATA);
            Log.i(TAG, "prepare tesseract data path is  " + DATA_PATH + TESSDATA);
        } catch (Exception e) {
            e.printStackTrace();
        }

        copyTessDataFiles();
    }

    /**
     * Copy tessdata files (located on assets/tessdata) to destination directory
     *
     */
    private void copyTessDataFiles() {
        Log.i(TAG, "copy tess data files");
        try {
            String[] fileList = getAssets().list(MainActivity.TESSDATA);
            String DATA_PATH = this.getApplicationContext().getFilesDir() + "/TesseractSample/";

            assert fileList != null;
            for (String fileName : fileList) {

                // open file within the assets folder
                // if it is not already there copy it to the sdcard
                String pathToDataFile = DATA_PATH + MainActivity.TESSDATA + "/" + fileName;
                if (!(new File(pathToDataFile)).exists()) {

                    InputStream in = getAssets().open(MainActivity.TESSDATA + "/" + fileName);

                    OutputStream out = new FileOutputStream(pathToDataFile);

                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;

                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();

                    Log.d(TAG, "Copied " + fileName + "to tessdata");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to copy files to tessdata " + e.toString());
        }
    }


//    /**
//     * don't run this code in main thread - it stops UI thread. Create AsyncTask instead.
//     * http://developer.android.com/intl/ru/reference/android/os/AsyncTask.html
//     *
//     * @param imgUri
//     */
    private void startOCR(Uri imgUri) {
        try {
            final InputStream imageStream = getContentResolver().openInputStream(imgUri);

            Bitmap originalBitmap = BitmapFactory.decodeStream(imageStream);
            Pix convertedPix = ReadFile.readBitmap(originalBitmap);
            originalBitmap.recycle();

            // Various image processing algorithms using leptonica library
            // Image processing will increase accuracy of OCR
            convertedPix = MorphApp.pixFastTophatBlack(convertedPix);
            convertedPix = GrayQuant.pixThresholdToBinary(convertedPix, 19);
//            convertedPix = Enhance.unsharpMasking(convertedPix);
//            convertedPix = AdaptiveMap.pixContrastNorm(convertedPix);
//            convertedPix = Binarize.otsuAdaptiveThreshold(convertedPix);
//            convertedPix = Binarize.sauvolaBinarizeTiled(convertedPix);

            Bitmap convertedBitmap = WriteFile.writeBitmap(convertedPix);
            convertedPix.recycle();

            ImageView mainImage = findViewById(R.id.convertedImageView);
            mainImage.setImageBitmap(convertedBitmap);

            result = extractText(convertedBitmap);
            Log.i(TAG, "result is " + result);

            textView.setText(result);



        } catch (Exception e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
        }
    }

    private String extractText(Bitmap bitmap) {
        String DATA_PATH = this.getApplicationContext().getFilesDir() + "/TesseractSample/";
        Log.i(TAG, "Data path is " + DATA_PATH);

        try {
            tessBaseApi = new TessBaseAPI();
        } catch (Exception e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            if (tessBaseApi == null) {
                Log.e(TAG, "TessBaseAPI is null. TessFactory not returning tess object.");
            }
        }

        tessBaseApi.init(DATA_PATH, lang);

//       //EXTRA SETTINGS
//        //For example if we only want to detect numbers
//        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "1234567890");
//
//        //blackList Example
//        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-qwertyuiop[]}{POIU" +
//                "YTRWQasdASDfghFGHjklJKLl;L:'\"\\|~`xcvXCVbnmBNM,./<>?");

        Log.d(TAG, "Training file loaded");
        tessBaseApi.setImage(bitmap);
        String extractedText = "empty result";
        try {
            extractedText = tessBaseApi.getUTF8Text();
        } catch (Exception e) {
            Log.e(TAG, "Error in recognizing text.");
        }
        tessBaseApi.end();
        return extractedText;
    }









    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Camera access permission

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST_CODE);
    }

    // Gallery access permission

    private boolean checkGalleryPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestGalleryPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                GALLERY_PERMISSION_REQUEST_CODE);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Camera Permission Granted", Toast.LENGTH_SHORT).show();

                    // main logic
                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                    StrictMode.setVmPolicy(builder.build());
                    startCameraActivity();
                } else {
                    Toast.makeText(getApplicationContext(), "Camera Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel(
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            requestCameraPermission();
                                        }
                                    });
                        }
                    }
                }
            case GALLERY_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Gallery Permission Granted", Toast.LENGTH_SHORT).show();

                    // main logic
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, GALLERY_LOAD_IMAGE);

                } else {
                    Toast.makeText(getApplicationContext(), "Gallery Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel(
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            requestGalleryPermission();
                                        }
                                    });
                        }
                    }
                }
            }
    }

    private void showMessageOKCancel(DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage("You need to allow access permissions")
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }


}