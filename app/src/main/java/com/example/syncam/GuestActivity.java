package com.example.syncam;

import static android.provider.MediaStore.MediaColumns.MIME_TYPE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.io.File;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class GuestActivity extends AppCompatActivity implements ImageAnalysis.Analyzer, View.OnClickListener {

    String deviceNumber;
    String roomNumber;
    DatabaseReference devices;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private static final String TAG = "MyApp";
    PreviewView previewView;
    private ImageCapture imageCapture;
    private VideoCapture videoCapture;
    private Button bRecord;
    private Button bCapture;
    private Button bChange;
    int REQUEST_CODE_FOR_PERMISSIONS = 1234;
    final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.RECORD_AUDIO"};
    int count = 0;
    boolean dark;
    int resolutionX = 1920;
    int resolutionY = 1080;
    int startTime;
    int endTime;
    boolean videoMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_NoTitleBar);
        setContentView(R.layout.activity_guest);
        roomNumber = MainActivity.roomNumber;
        deviceNumber = MainActivity.deviceNumber;
        devices = ReadWrite.ref.child(roomNumber).child("devices");
        Log.d("variable", roomNumber + deviceNumber);
        ReadWrite.ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                if (Objects.requireNonNull(snapshot.getKey()).equals(roomNumber)) {
                    finish();
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        ReadWrite.ref.child(roomNumber).child("Settings").addChildEventListener(new ChildEventListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {
                switch (Objects.requireNonNull(snapshot.getKey())) {
                    case "dark":
                        dark = Boolean.parseBoolean(String.valueOf(snapshot.getValue()));
                        break;
                    case "resolution":
                        switch (String.valueOf(snapshot.getValue())){
                            case "720p HD":
                                resolutionX = 1280;
                                resolutionY = 720;
                                break;
                            case "1080p FHD":
                                resolutionX = 1920;
                                resolutionY = 1080;
                                break;
                            case "2160p 4K":
                                resolutionX = 3840;
                                resolutionY = 2160;
                                break;
                        }
                        break;
                    case "start":
                        startTime = Integer.parseInt((String) Objects.requireNonNull(snapshot.getValue()));
                        break;
                    case "video":
                        videoMode = Boolean.parseBoolean(String.valueOf(snapshot.getValue()));
                        break;
                    case "end":
                        endTime = Integer.parseInt((String) Objects.requireNonNull(snapshot.getValue()));
                        int i = endTime - MainActivity.getToday() + MainActivity.timeLag;
                        new Handler().postDelayed(funcVe,i);
                        count = 0;
                }
                count++;
                if (count == 4) {
                    int i = startTime - MainActivity.getToday() + MainActivity.timeLag;
                    if(videoMode){
                        new Handler().postDelayed(funcV,i);
                        previewView.post((Runnable) (new Runnable() {
                            public final void run() {
                                cameraProviderFuture.addListener(() -> {
                                    try {
                                        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                                        startCameraXv(cameraProvider);
                                    } catch (ExecutionException | InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }, getExecutor());
                            }
                        }));
                    }else{
                        new Handler().postDelayed(funcC,i);
                        previewView.post((Runnable) (new Runnable() {
                            public final void run() {
                                cameraProviderFuture.addListener(() -> {
                                    try {
                                        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                                        startCameraX(cameraProvider);
                                    } catch (ExecutionException | InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }, getExecutor());
                            }
                        }));
                        count = 0;
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        immersiveMode();

        previewView = findViewById(R.id.previewView);
        bCapture = findViewById(R.id.bCapture);
        bRecord = findViewById(R.id.bRecord);
        bChange = findViewById(R.id.bChange);
        bChange.setText("☮");
        bRecord.setText("🔴");
        bChange.setOnClickListener(this);
        bCapture.setOnClickListener(this);
        bRecord.setOnClickListener(this);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);


        if (checkPermissions()) {
            previewView.post((Runnable) (new Runnable() {
                public final void run() {
                    cameraProviderFuture.addListener(() -> {
                        try {
                            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                            startCameraX(cameraProvider);
                        } catch (ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }, getExecutor());
                }
            }));
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_FOR_PERMISSIONS);
            ProcessCameraProvider cameraProvider = null;
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            startCameraX(cameraProvider);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        devices.child(deviceNumber).removeValue();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        finish();
    }

    //画面出力　↓↓
    Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    @SuppressLint("RestrictedApi")
    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // preview
        Preview preview = new Preview.Builder()
                .setTargetResolution(new Size(resolutionX, resolutionY))
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());


        // Image capture use case
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(new Size(resolutionX, resolutionY))
                .build();


        // Video capture use case
        videoCapture = new VideoCapture.Builder()
                .setVideoFrameRate(60)
                .build();

        // Image analysis use case
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(getExecutor(), this);

        //bind to lifecycle:
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);

    }

    @SuppressLint("RestrictedApi")
    private void startCameraXv(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // preview
        Preview preview = new Preview.Builder()
                .setTargetResolution(new Size(resolutionX, resolutionY))
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());


        // Image capture use case
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(new Size(resolutionX, resolutionY))
                .build();


        // Video capture use case
        videoCapture = new VideoCapture.Builder()
                .setVideoFrameRate(60)
                .build();

        // Image analysis use case
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(getExecutor(), this);

        //bind to lifecycle:
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, videoCapture);

    }


    @Override
    public void analyze(@NonNull ImageProxy image) {
        // image processing here for the current frame
        Log.d("TAG", "analyze: got the frame at: " + image.getImageInfo().getTimestamp());
        image.close();
    }
    //画面出力　↑↑

    //ボタン処理　↓↓
    @SuppressLint("RestrictedApi")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bCapture:
                capturePhoto();
                break;
            case R.id.bRecord:
                if (bRecord.getText() == "🔴") {
                    bRecord.setText("🔶");
                    recordVideo();
//                    LayoutParams lp = getWindow().getAttributes();
//                    lp.screenBrightness = 0.01F;
//                    getWindow().setAttributes(lp);
                } else {
                    bRecord.setText("🔴");
                    videoCapture.stopRecording();
//                    LayoutParams lp = getWindow().getAttributes();
//                    lp.screenBrightness = 1.0F;
//                    getWindow().setAttributes(lp);
                }
                break;
            case R.id.bChange:
                if (bChange.getText() == "☮") {
                    bChange.setText("☯");
                    previewView.post((Runnable) (new Runnable() {
                        public final void run() {
                            cameraProviderFuture.addListener(() -> {
                                try {
                                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                                    startCameraXv(cameraProvider);
                                } catch (ExecutionException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }, getExecutor());
                        }
                    }));
                } else {
                    bChange.setText("☮");
                    ProcessCameraProvider cameraProvider = null;
                    try {
                        cameraProvider = cameraProviderFuture.get();
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    startCameraX(cameraProvider);
                }
                break;

        }
    }
    //ボタン処理　↑↑

    //動画保存　↓↓
    @SuppressLint("RestrictedApi")
    private void recordVideo() {

        if (dark) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = 0.01F;
            getWindow().setAttributes(lp);
        }

        if (videoCapture != null) {
            //File movieDir = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES) + "/");
            final String SAVE_DIR = "/DCIM/SYNCAM";
            File movieDir = new File(Environment.getExternalStorageDirectory().getPath() + SAVE_DIR);

            if (!movieDir.exists())
                movieDir.mkdir();

            Date date = new Date();
            String timestamp = String.valueOf(date.getTime());
            String vidFilePath = movieDir.getAbsolutePath() + "/" + android.os.Build.MODEL + "_" + timestamp + ".mp4";
            File vidFile = new File(vidFilePath);
            String AttachName = vidFilePath;
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                videoCapture.startRecording(
                        new VideoCapture.OutputFileOptions.Builder(vidFile).build(),
                        getExecutor(),
                        new VideoCapture.OnVideoSavedCallback() {
                            @Override
                            public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                                Toast.makeText(GuestActivity.this, "保存しました", Toast.LENGTH_SHORT).show();
                                if (dark) {
                                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                                    lp.screenBrightness = 1.0F;
                                    getWindow().setAttributes(lp);
                                }
                            }

                            @Override
                            public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                                // Toast.makeText(MainActivity.this, "エラー:" + message, Toast.LENGTH_SHORT).show();
                                Toast.makeText(GuestActivity.this, "ビデオモードではありません", Toast.LENGTH_SHORT).show();
                                if(dark) {
                                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                                    lp.screenBrightness = 1.0F;
                                    getWindow().setAttributes(lp);
                                }
                            }

                        }
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            ContentValues values = new ContentValues();
            ContentResolver contentResolver = getContentResolver();
            values.put("image/mp4", MIME_TYPE);
            values.put(MediaStore.Video.Media.TITLE, vidFilePath);
            values.put("_data", AttachName);
            contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        }

    }
    //動画保存　↑↑

    //画像保存　↓↓
    private void capturePhoto() {
//        File photoDir = new File(Environment.getExternalStorageDirectory()+"/"+Environment.DIRECTORY_DCIM+"/cameraxt/");
        final String SAVE_DIR = "/DCIM/SYNCAM";
        File photoDir = new File(Environment.getExternalStorageDirectory().getPath() + SAVE_DIR);
//        File photoDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/");
        if (!photoDir.exists())
            photoDir.mkdir();

        Date date = new Date();
        String timestamp = String.valueOf(date.getTime());
        String photoFilePath = photoDir.getAbsolutePath() + "/" + android.os.Build.MODEL + "_" + timestamp + ".jpg";
        File photoFile = new File(photoFilePath);
        String AttachName = photoFilePath;

        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(photoFile).build(),
                getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(GuestActivity.this, "保存しました", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
//                         Toast.makeText(GuestActivity.this, "エラー" + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        Toast.makeText(GuestActivity.this, "カメラモードではありません", Toast.LENGTH_SHORT).show();

                    }
                }
        );
        ContentValues values = new ContentValues();
        ContentResolver contentResolver = getContentResolver();
        values.put(MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.TITLE, photoFilePath);
        values.put("_data", AttachName);
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

    }
    //画像保存　↑↑


    //バー消去　↓↓
    private void immersiveMode() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );
        Handler h = new Handler();
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        // Note that system bars will only be "visible" if none of the
                        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            Log.d("debug", "The system bars are visible");

                            h.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    immersiveMode();
                                }
                            }, 3 * 1000);

                        } else {
                            Log.d("debug", "The system bars are NOT visible");
                        }
                    }
                });
    }
    //バー消去　↑↑

    //判断　↓↓
    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    //判断　↑↑

    private final Runnable funcC= new Runnable() {
        @Override
        public void run() {
            capturePhoto();
        }
    };
    private final Runnable funcV= new Runnable() {
        @Override
        public void run() {
            recordVideo();
        }
    };
    private final Runnable funcVe= new Runnable() {
        @SuppressLint("RestrictedApi")
        @Override
        public void run() {
            videoCapture.stopRecording();
        }
    };
}