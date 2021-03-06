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
import android.os.StrictMode;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
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
        LoadFileDialog.LoadFilenameDialogListener, DuplicateFileDialog.DuplicateFileDialogListener,
        LoadImageDialog.LoadImageDialogListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    static final int PHOTO_REQUEST_CODE = 1;
    private static final int STORAGE_LOAD_IMAGE = 2;
    private static final int EDIT_TEXT_REQUEST_CODE = 100;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 300;
    private static final String TESSDATA = "tessdata";
    private static final String lang = "eng";
    private static final double IMG_MAX_RES = 12_000_000.0;

    private TessBaseAPI tessBaseApi;
    private Uri outputFileUri;
    private String result = "empty";
    private boolean cameraNotStorage;

    private TextView changeImageTextView;
    private TextView filenameTextView;
    private TextView textViewResult;
    private ImageButton mainImageBtn;
    private ImageButton enlargeImgBtn;
    private Button buttonSaveText;
    private Button textToSpeechBtn;
    private Button googleSearchBtn;
    private static Bitmap mainImage;

    private TextToSpeech textToSpeech;
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

        Button buttonLoadText = findViewById(R.id.loadBtn);
        buttonLoadText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLoadDialog(true);
            }
        });

        textViewResult = findViewById(R.id.textResult);
        textViewResult.addTextChangedListener(saveTextWatcher);
        textViewResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), TextEditActivity.class);
                intent.putExtra("fileName", filenameTextView.getText().toString());
                intent.putExtra("fileContent", textViewResult.getText().toString());
                startActivityForResult(intent, EDIT_TEXT_REQUEST_CODE);
            }
        });

        changeImageTextView = findViewById(R.id.changeImageTextView);
        changeImageTextView.setVisibility(View.INVISIBLE);

        filenameTextView = findViewById(R.id.fileNameTextView);
        filenameTextView.setText(R.string.text_not_saved);

        spinnerProgressImage = findViewById(R.id.progressBarImage);
        spinnerProgressImage.setVisibility(View.GONE);

        mainImageBtn = findViewById(R.id.convertedImgBtn);
        mainImageBtn.setImageResource(R.drawable.start_convert_icon3);
        mainImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLoadImageDialog();
            }
        });

        enlargeImgBtn = findViewById(R.id.popupEnlargeImgBtn);
        enlargeImgBtn.setClickable(false);
        enlargeImgBtn.setVisibility(View.GONE);
        enlargeImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enlargeImg(v);
            }
        });

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

            }
        });

        textToSpeechBtn = findViewById(R.id.readButton);
        textToSpeechBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                read();
            }
        });

        googleSearchBtn = findViewById(R.id.googleBtn);
        googleSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                google();
            }
        });

    }


    //////////////////////////////////////////////////////////////
    // to load text files stored in internal storage

    public void openLoadImageDialog() {
        LoadImageDialog loadImageDialog = new LoadImageDialog();
        loadImageDialog.show(getSupportFragmentManager(), "load image dialog");
    }

    @Override
    public void loadImageSource(boolean cameraNotStorageDialog) {
        this.cameraNotStorage = cameraNotStorageDialog;
        if(cameraNotStorage) {
            // Checks for camera permission
            if (checkCameraPermission()) {
                startCameraActivity();
            } else {
                requestCameraPermission();
            }
        }
        else {
            if (checkStoragePermission()) {

                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, STORAGE_LOAD_IMAGE);

            } else {
                requestStoragePermission();
            }
        }
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////
    // to save text in the editText as a text file in document folder

    public void openSaveDialog() {
        // open dialog for user to enter filename to save text file
        String filename = filenameTextView.getText().toString();
        if (filename.endsWith(" (not saved)")) {
            filename = filename.substring(0, filename.length() - " (not saved)".length());
        }
        Bundle args = new Bundle();
        args.putString("filenameToBeSaved", filename);

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
            fileWriter.append(textViewResult.getText().toString().trim());
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
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            buttonSaveText.setEnabled(!textViewResult.getText().toString().trim().isEmpty());
            textToSpeechBtn.setEnabled(!textViewResult.getText().toString().trim().isEmpty());
            googleSearchBtn.setEnabled(!textViewResult.getText().toString().trim().isEmpty());
        }
        @Override
        public void afterTextChanged(Editable s) { }
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
        mainImageBtn.setImageResource(R.drawable.start_convert_icon3);
        enlargeImgBtn.setClickable(false);
        enlargeImgBtn.setVisibility(View.GONE);
        changeImageTextView.setVisibility(View.INVISIBLE);

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
                loadedText = loadedText + "\n" + textViewResult.getText().toString().trim();
                textViewResult.setText(loadedText);
                filenameTextView.setText(filename);
                save(filename, false);
            }
            else{
                textViewResult.setText(loadedText);
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
    // Using the camera to obtain image

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
    // Tesseract OCR and Leptonica
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {

            // choose image from camera
            if (requestCode == PHOTO_REQUEST_CODE) {
                changeImageTextView.setVisibility(View.INVISIBLE);
                spinnerProgressImage.setVisibility(View.VISIBLE);
                mainImageBtn.setVisibility(View.INVISIBLE);
                mainImageBtn.setClickable(false);
                textViewResult.setText("");
                textViewResult.setClickable(false);
                startThread();
            }

            // load image from internal storage
            else if (requestCode == STORAGE_LOAD_IMAGE) {
                changeImageTextView.setVisibility(View.INVISIBLE);
                outputFileUri = data.getData();
                spinnerProgressImage.setVisibility(View.VISIBLE);
                mainImageBtn.setVisibility(View.INVISIBLE);
                mainImageBtn.setClickable(false);
                textViewResult.setText("");
                textViewResult.setClickable(false);
                startThread();
            }

            // returns edited text to main activity
            else if(requestCode == EDIT_TEXT_REQUEST_CODE) {
                filenameTextView.setText(data.getStringExtra("fileNameback"));
                textViewResult.setText(data.getStringExtra("fileContentback"));
            }
        }
    }

    public void startThread() {
        ocrRunnable runnable = new ocrRunnable();
        new Thread(runnable).start();
    }

    class ocrRunnable implements Runnable {
        @Override
        public void run() {
            doOCR();
        }
    }

    private void doOCR() {
        prepareTesseract();
        startOCR(outputFileUri);
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

    // Copy tessdata files (located on assets/tessdata) to destination directory
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

    private void startOCR(Uri imgUri) {
        try {
            if (mainImage != null)
                mainImage.recycle();
            mainImage = getImage(imgUri);
            final Bitmap imageBitmap = mainImage.copy(mainImage.getConfig(), true);

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mainImageBtn.setImageBitmap(mainImage);
                    mainImageBtn.setVisibility(View.VISIBLE);
                    enlargeImgBtn.setClickable(true);
                    enlargeImgBtn.setVisibility(View.VISIBLE);
                }
            });

            result = extractText(imageBitmap);
            imageBitmap.recycle();

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    spinnerProgressImage.setVisibility(View.GONE);
                    textViewResult.setText(result);
                    filenameTextView.setText(R.string.text_not_saved);
                    mainImageBtn.setClickable(true);
                    changeImageTextView.setVisibility(View.VISIBLE);
                    textViewResult.setClickable(true);
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
//        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "@#$%^&*[]}{'\\|~`/<>?");

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

    // deletes stored images and shutdown services when exiting application
    @Override protected void onDestroy() {
        super.onDestroy();
        if(!isChangingConfigurations()) {
            if(!deleteTempFiles(Objects.requireNonNull(getExternalFilesDir(Environment.DIRECTORY_PICTURES))))
                Log.e(TAG, "onDestroy: error deleting pictures");
        }
        textToSpeech.shutdown();
        if(tessBaseApi != null)
            tessBaseApi.end();
    }

    private boolean deleteTempFiles(File file) {
        boolean deleteSuccess = true;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteSuccess = f.delete();
                }
            }
        }
        else
            deleteSuccess = file.delete();
        return deleteSuccess;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // This method enlarges image on a popupwindow upon pressing the enlarge image button
    private void enlargeImg(View v) {
        LayoutInflater inflater = getLayoutInflater();
        View popupView = inflater.inflate(R.layout.popupwindow_image, (ViewGroup)findViewById(R.id.layout_root));

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);

        ImageView enlargedImgView = popupWindow.getContentView().findViewById(R.id.enlargeImgView);
        enlargedImgView.setImageBitmap(mainImage);

        popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);

        View container = popupWindow.getContentView().getRootView();
        if(container != null) {
            WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
            WindowManager.LayoutParams p = (WindowManager.LayoutParams)container.getLayoutParams();
            p.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            p.dimAmount = 0.5f;
            if(wm != null) {
                wm.updateViewLayout(container, p);
            }
        }
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

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // This method returns image from internal storage, limits resolution to 12MP
    // Rotates image taken by camera intent to the correct orientation

    public Bitmap getImage(Uri uri) throws IOException{
        InputStream input = this.getContentResolver().openInputStream(uri);

        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        assert input != null;
        input.close();

        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1)) {
            return null;
        }

        int imgSize = onlyBoundsOptions.outHeight * onlyBoundsOptions.outWidth;
        Log.i(TAG, "getImage: " + imgSize);

        double ratio = (imgSize > IMG_MAX_RES) ? Math.ceil(imgSize / IMG_MAX_RES) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = (int) ratio;
        input = this.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        assert input != null;
        input.close();
        if(cameraNotStorage)
            bitmap = rotateImageIfRequired(bitmap, uri);
        return bitmap;
    }

    private static Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {
        Log.i(TAG, "rotateImageIfRequired: before exifinterface");
        ExifInterface ei = new ExifInterface(Objects.requireNonNull(selectedImage.getPath()));
        Log.i(TAG, "rotateImageIfRequired: after exifinterface");
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

                    cameraNotStorage = true;
                    startCameraActivity();

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

    /////////////////////////////////////////////////////////////////////////
    // Text to speech

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void read() {
        if(!textToSpeech.isSpeaking()) {
            textToSpeech.speak(textViewResult.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, null);
        }
        else {
            textToSpeech.stop();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(textToSpeech.isSpeaking())
            textToSpeech.stop();
    }

    //////////////////////////////////////////////////////////////////////////
    // google
    public void google() {
        String mtext = textViewResult.getText().toString().trim().replaceAll("\\s","%20");
        String google = "http://www.google.com/search?q=" + mtext + "";
        //String google = "http://www.google.com";
        Uri webAddress = Uri.parse(google);
        Intent gotoGoogle = new Intent(Intent.ACTION_VIEW, webAddress);
        if (gotoGoogle.resolveActivity(getPackageManager()) != null) {
            startActivity(gotoGoogle);
        }

    }

}