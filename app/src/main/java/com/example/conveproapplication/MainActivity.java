package com.example.conveproapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.googlecode.leptonica.android.GrayQuant;
import com.googlecode.leptonica.android.MorphApp;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.leptonica.android.WriteFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;



public class MainActivity extends AppCompatActivity implements SaveFileDialog.SaveFileDialogListener,
        LoadFileDialog.LoadFilenameDialogListener, DuplicateFileDialog.DuplicateFileDialogListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    static final int PHOTO_REQUEST_CODE = 1;
    private static final int STORAGE_LOAD_IMAGE = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 201;
    private static final String TESSDATA = "tessdata";
    private static final String lang = "eng";

    private TessBaseAPI tessBaseApi;
    Uri outputFileUri;
    String result = "empty";
    private boolean cameraNotStorage;

    String mtext;
    private TextToSpeech textToSpeech;

    // for google vision api
    final boolean tessNotGoogleVision = true;
    private SurfaceView surfaceView;
    private CameraSource cameraSource;
    private TextRecognizer textRecognizer;
    private String stringResult = null;

    TextView filenameTextView;
    TextView editTextResult;
    ImageView mainImage;
    Button buttonSaveText;
    Button buttonLoadText;
    Button captureImg;
    Button buttonLoadImage;

    private ProgressBar spinnerProgressImage;

    // handler to access ui thread when doing work in background thread
    final private Handler mainHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonSaveText = findViewById(R.id.saveBtn);
        buttonSaveText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSaveDialog();

            }
        });

        buttonLoadText = findViewById(R.id.loadBtn);
        buttonLoadText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                openLoadDialog(true);
            }
        });

        captureImg = findViewById(R.id.cameraBtn);
        if (captureImg != null) {
            captureImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // Checks for camera permission
                    if (checkCameraPermission()) {
                        if (tessNotGoogleVision) {
                            cameraNotStorage = true;
                            startCameraActivity();
                        } else { // using google vision
                            setContentView(R.layout.surfaceview);
                            textRecognizer();
                        }
                    } else {
                        requestCameraPermission();
                    }
                }
            });
        }

        buttonLoadImage = findViewById(R.id.galleryBtn);
        buttonLoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (checkStoragePermission()) {
                    cameraNotStorage = false;

                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, STORAGE_LOAD_IMAGE);

                } else {
                    requestStoragePermission();
                }
            }
        });


        editTextResult = findViewById(R.id.textResult);
        editTextResult.addTextChangedListener(saveTextWatcher);

        filenameTextView = findViewById(R.id.fileNameView);
        filenameTextView.setText(R.string.text_not_saved);

        spinnerProgressImage = findViewById(R.id.progressBarImage);
        spinnerProgressImage.setVisibility(View.GONE);

        mainImage = findViewById(R.id.convertedImageView);
        mainImage.setVisibility(View.GONE);

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

            }
        });

    }

    ////////////////////////////////////////////////////////
    // start background thread
    public void startThread() {
        ExampleRunnable runnable = new ExampleRunnable();
        new Thread(runnable).start();
    }

    class ExampleRunnable implements Runnable {

        @Override
        public void run() {
            // time consuming task is run on the background
            doOCR();
        }
    }

    // may need so left here
    public void stopThread() {

    }

    class ExampleThread extends Thread {

        @Override
        public void run() {

        }
    }



    //////////////////////////////////////////////////////////////

    /*
        Contents:
            Dialog for saving text file
            Dialog for loading text file
            Tesseract OCR and Leptonica stuff
            Google vision (currently not in use)
            Rotation of image taken by camera intent
            Permissions stuff
     */







    //////////////////////////////////////////////////////////////////////////////////////////////////
    // to save text in the editText as a text file in document folder

    public void openSaveDialog() {
        // look at SaveFileDialog.java
        // open dialog for user to enter filename to save text file
        String filename = filenameTextView.getText().toString();
        if (filename.endsWith(" (not saved)")) {
            filename = filename.substring(0,
                    filename.length() - " (not saved)".length());
        }
        Bundle args = new Bundle();
        args.putString("filenameA", filename);

        SaveFileDialog saveFileDialog = new SaveFileDialog();
        saveFileDialog.setArguments(args);
        saveFileDialog.show(getSupportFragmentManager(), "save dialog");
    }

    @Override
    public void saveText(String filename) {
        if (checkDuplicateFile(filename)){
            openDuplicateFileDialog(filename);
        }
        else{
            filenameTextView.setText(filename);
            save(filename, true);
        }

    }

    @Override
    public void appendText() {
        openLoadDialog(false);
    }

    public void save(String filename, Boolean saveNotAppend) {

        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), filename);
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.append(editTextResult.getText().toString().trim());
            fileWriter.flush();
            fileWriter.close();
            Toast.makeText(this, (saveNotAppend ? "Saved in " : "Appended and saved in ") + filename,
                    Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // to disable save button when edit text is empty
    private final TextWatcher saveTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {


            String editTextInput = editTextResult.getText().toString().trim();
            buttonSaveText.setEnabled(!editTextInput.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable s) {
            String filenameFromTextView = filenameTextView.getText().toString();
            if (!filenameFromTextView.equals(getResources().getString(R.string.text_not_saved))
                    && !filenameFromTextView.endsWith(" (not saved)")) {
                String message = filenameFromTextView + " (not saved)";
                filenameTextView.setText(message);
            }
        }
    };

    public void openDuplicateFileDialog(String filename) {
        Bundle args = new Bundle();
        args.putString("filenameD", filename);

        DuplicateFileDialog duplicateFileDialog = new DuplicateFileDialog();
        duplicateFileDialog.setArguments(args);
        duplicateFileDialog.show(getSupportFragmentManager(), "duplicate file dialog");
    }

    @Override
    public void overwriteDuplicateFile(String filename) {
        filenameTextView.setText(filename);
        save(filename, true);
    }

    private Boolean checkDuplicateFile(String filename) {
        // to find the files in the folder and pass all file names into the dialog
        String path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "";
        File directory = new File(path);
        File[] files = directory.listFiles();
        assert files != null;
        // using "/" to separate the file names
        for (File file : files) {
            if (file.getName().equals(filename))
                return true;
        }
        return false;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // to load text from text file detected in documents folder

    public void openLoadDialog(Boolean loadNotAppend) {
        // look at LoadFileDialog.java
        StringBuilder filenames = new StringBuilder();

        // to find the files in the folder and pass all file names into the dialog
        String path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "";
        File directory = new File(path);
        File[] files = directory.listFiles();
        assert files != null;
        // using "/" to separate the file names
        for (File file : files) {
            filenames.append(file.getName()).append("/");
        }

        Bundle args = new Bundle();
        args.putString("filenamesB", filenames.toString());
        args.putBoolean("loadNotAppend", loadNotAppend);

        LoadFileDialog loadFileDialog = new LoadFileDialog();
        loadFileDialog.setArguments(args);
        loadFileDialog.show(getSupportFragmentManager(), "load dialog");
    }

    @Override
    public void loadText(String filename) {
        load(filename, true);
    }

    @Override
    public void appendText(String filename) {
        load(filename, false);
    }

    public void load(String filename, Boolean loadNotAppend) {

        String loadedText;
        mainImage.setVisibility(View.GONE);

        try {
            File selectedTextFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + filename);
            FileInputStream inputStream = new FileInputStream(selectedTextFile);

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String receiveString;
            StringBuilder stringBuilder = new StringBuilder();

            while ((receiveString = bufferedReader.readLine()) != null) {
                stringBuilder.append("\n").append(receiveString);
            }

            inputStream.close();
            loadedText = stringBuilder.toString().trim();

            if (!loadNotAppend) {
                // append and save
                loadedText = loadedText + "\n" + editTextResult.getText().toString().trim();
                editTextResult.setText(loadedText);
                filenameTextView.setText(filename);
                save(filename, false);
            }
            else{
                editTextResult.setText(loadedText);
                filenameTextView.setText(filename);
                Toast.makeText(this, filename + " loaded", Toast.LENGTH_SHORT).show();
            }

        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
            Toast.makeText(this, "File not found: " + e.toString(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
            Toast.makeText(this, "Can not read file: " + e.toString(), Toast.LENGTH_SHORT).show();
        }

    }




    /////////////////////////////////////////////////////////////////////////////////////////////////
    // Using the camera to obtain image for OCR

    private void startCameraActivity() {
        try {
            // take photo with camera app
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
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

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                    startActivityForResult(takePictureIntent, PHOTO_REQUEST_CODE);

                }
            }
        } catch (Exception e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
        }
    }

    // saving image that is taken from the camera
    private File createImageFile() throws IOException {
        // Create an image file name
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_hhmmss", Locale.getDefault());
        String timeStamp = simpleDateFormat.format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        assert storageDir != null;

        // Save a file: path for use with ACTION_VIEW intents
        return File.createTempFile(
                imageFileName,           /* prefix */
                ".jpg",           /* suffix */
                storageDir              /* directory */
        );
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////
    // Tesseract OCR and Leptonica stuff

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        // choose image from camera
        if (requestCode == PHOTO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            spinnerProgressImage.setVisibility(View.VISIBLE);
            mainImage.setVisibility(View.GONE);
            editTextResult.setText("");
//            doOCR();
            startThread();
        }

        // load image from gallery
        else if (requestCode == STORAGE_LOAD_IMAGE && resultCode == Activity.RESULT_OK) {
            outputFileUri = data.getData();
//            doOCR();
            spinnerProgressImage.setVisibility(View.VISIBLE);
            mainImage.setVisibility(View.GONE);
            editTextResult.setText("");
            startThread();
        } else {
            Toast.makeText(this, "ERROR: Image was not obtained.", Toast.LENGTH_SHORT).show();
        }
    }

    private void doOCR() {
        prepareTesseract();
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
            final InputStream imageStream;
            final Bitmap originalBitmap;

            if (cameraNotStorage) {
                originalBitmap = handleSamplingAndRotationBitmap(getApplicationContext(), imgUri);
            }
            else {
                imageStream = getContentResolver().openInputStream(imgUri);
                originalBitmap = BitmapFactory.decodeStream(imageStream);
            }

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mainImage.setImageBitmap(originalBitmap);
                    mainImage.setVisibility(View.VISIBLE);

                }
            });


            Pix convertedPix = ReadFile.readBitmap(originalBitmap);

            // Various image processing algorithms using leptonica library
            // Image processing will increase accuracy of OCR

            convertedPix = MorphApp.pixFastTophatBlack(convertedPix);
            convertedPix = GrayQuant.pixThresholdToBinary(convertedPix, 18);
//            convertedPix = Enhance.unsharpMasking(convertedPix);
//            convertedPix = AdaptiveMap.pixContrastNorm(convertedPix);
//            convertedPix = Binarize.otsuAdaptiveThreshold(convertedPix);
//            convertedPix = Binarize.sauvolaBinarizeTiled(convertedPix);

            Bitmap convertedBitmap = WriteFile.writeBitmap(convertedPix);
            convertedPix.recycle();




            result = extractText(convertedBitmap);
            convertedBitmap.recycle();
            Log.i(TAG, "result is " + result);

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    spinnerProgressImage.setVisibility(View.GONE);
                    editTextResult.setText(result);
                    filenameTextView.setText(R.string.text_not_saved);
                }
            });



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


    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Google mobile vision

    private void textRecognizer() {

        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                .setRequestedPreviewSize(1280, 1024)
                .build();

        surfaceView = findViewById(R.id.surfaceView);

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.

                        return;
                    }
                    cameraSource.start(surfaceView.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {

                SparseArray<TextBlock> sparseArray = detections.getDetectedItems();
                StringBuilder stringBuilder = new StringBuilder();

                for (int i = 0; i<sparseArray.size(); ++i) {
                    TextBlock textBlock = sparseArray.valueAt(i);
                    if (textBlock != null && textBlock.getValue() != null) {
                        stringBuilder.append(textBlock.getValue()).append(" ");
                    }
                }

                final String stringText = stringBuilder.toString();

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void run() {

                        stringResult = stringText;
                        Log.i(TAG, "String result: " + stringResult);
                        resultObtained();

                    }
                });

            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void resultObtained() {

        setContentView(R.layout.activity_main);
        editTextResult.setText(stringResult);
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        cameraSource.release();
//    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if(!isChangingConfigurations()) {
            deleteTempFiles(Objects.requireNonNull(getExternalFilesDir(Environment.DIRECTORY_PICTURES)));
        }
    }

    private void deleteTempFiles(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteTempFiles(f);
                    } else {
                        f.delete();
                    }
                }
            }
        }
        else
            file.delete();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Rotates image taken by camera intent to the correct orientation

    /**
     * This method is responsible for solving the rotation issue if exist. Also scale the images to
     * 1024x1024 resolution
     *
     * @param context       The current context
     * @param selectedImage The Image URI
     * @return Bitmap image results
     * @throws IOException  idk
     */
    public static Bitmap handleSamplingAndRotationBitmap(Context context, Uri selectedImage)
            throws IOException {
        int MAX_HEIGHT = 1024;
        int MAX_WIDTH = 1024;

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream imageStream = context.getContentResolver().openInputStream(selectedImage);
        BitmapFactory.decodeStream(imageStream, null, options);
        assert imageStream != null;
        imageStream.close();

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        imageStream = context.getContentResolver().openInputStream(selectedImage);
        Bitmap img = BitmapFactory.decodeStream(imageStream, null, options);

        img = rotateImageIfRequired(img, selectedImage);
        return img;
    }

    /**
     * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link BitmapFactory}. This implementation calculates
     * the closest inSampleSize that will result in the final decoded bitmap having a width and
     * height equal to or larger than the requested width and height. This implementation does not
     * ensure a power of 2 is returned for inSampleSize which can be faster when decoding but
     * results in a larger bitmap which isn't as useful for caching purposes.
     *
     * @param options   An options object with out* params already populated (run through a decode*
     *                  method with inJustDecodeBounds==true
     * @param reqWidth  The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee a final image
            // with both dimensions larger than or equal to the requested height and width.
            inSampleSize = Math.min(heightRatio, widthRatio);

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down further
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

    /**
     * Rotate an image if required.
     *
     * @param img           The image bitmap
     * @param selectedImage Image URI
     * @return The resulted Bitmap after manipulation
     */
    private static Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {

        ExifInterface ei = new ExifInterface(Objects.requireNonNull(selectedImage.getPath()));
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        Log.i(TAG, "degree is " + degree);
        return rotatedImg;
    }





    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Permissions

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST_CODE);
    }


    private boolean checkStoragePermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_REQUEST_CODE);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Camera Permission Granted", Toast.LENGTH_SHORT).show();

                    // main logic
                    if (tessNotGoogleVision) {
                        cameraNotStorage = true;
                        startCameraActivity();
                    } else { // using google vision
                        setContentView(R.layout.surfaceview);
                        textRecognizer();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Camera Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!checkCameraPermission()) {
                            showMessageOKCancel(
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            requestCameraPermission();
                                        }
                                    });
                        }
                    }
                } break;
            case STORAGE_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Storage Permission Granted", Toast.LENGTH_SHORT).show();

                    // main logic
                    cameraNotStorage = false;

                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, STORAGE_LOAD_IMAGE);

                } else {
                    Toast.makeText(getApplicationContext(), "Storage Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!checkStoragePermission()) {
                            showMessageOKCancel(
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            requestStoragePermission();
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void read(View view) {

        EditText input = (EditText) findViewById(R.id.textResult);
        mtext = input.getText().toString();
        textToSpeech.speak(mtext, TextToSpeech.QUEUE_FLUSH, null, null);

    }

    public void google(View view) {
        EditText input = (EditText) findViewById(R.id.textResult);
        mtext = input.getText().toString();
        mtext = mtext.trim();
        mtext = mtext.replaceAll("\\s","%20");
        String google = "http://www.google.com/search?q="+mtext+"";
        //String google = "http://www.google.com";
        Uri webaddress = Uri.parse(google);
        Intent gotoGoogle = new Intent(Intent.ACTION_VIEW, webaddress);
        if (gotoGoogle.resolveActivity(getPackageManager()) != null) {
            startActivity(gotoGoogle);
        }

    }






}