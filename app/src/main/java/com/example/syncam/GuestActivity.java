package com.example.syncam;

import static android.provider.MediaStore.MediaColumns.MIME_TYPE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Size;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
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
import androidx.preference.PreferenceManager;

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

public class GuestActivity extends AppCompatActivity implements ImageAnalysis.Analyzer {

    //デバイス番号を格納する変数
    String deviceNumber;
    //ルーム番号を格納する変数
    String roomNumber;
    //デバイス情報を格納するFirebaseのノード
    DatabaseReference devices;
    //共有プリファレンス
    SharedPreferences sharedPreferences;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    //VIEW変数
    PreviewView previewView;
    private ImageCapture imageCapture;
    private VideoCapture videoCapture;

    //カウンター
    int count = 0;
    //画面を暗くするかどうかの設定
    boolean dark;
    //横の解像度
    int resolutionX = 3840;
    //縦の解像度
    int resolutionY = 2160;
    //撮影開始時間
    int startTime;
    //撮影終了時間
    int endTime;
    //撮影モード（動画・静止画）
    boolean videoMode;
    //端末固有のラグを修正する設定
    int deviceCameraLag;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_NoTitleBar);
        setContentView(R.layout.activity_guest);

        //PreviewViewの位置を画面中央に調整
        //PreviewViewの結びつけ
        previewView = findViewById(R.id.previewView);
        //ハードウェアの画面解像度を取得
        Point pt = getRealDisplaySize();
        //PreviewViewを表示した余りを取得（長い画面では＋、正方形に近い画面は−、16:9の画面では0）
        int totalMargin = pt.x - ((pt.y / 9) * 16);
        //16:9より長い画面の場合（最近のスマホ等）
        if(totalMargin > 0){
            //画面の余りの部分の半分を左側のMarginとして設定する
            int startMargin = totalMargin / 2;
            ViewGroup.LayoutParams lp = previewView.getLayoutParams();
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            mlp.setMarginStart(startMargin);
            previewView.setLayoutParams(mlp);
        }
        //16:9より正方形に近い画面の場合（タブレット等）
        if(totalMargin < 0){
            //画面の余りの部分を半分にして上と下のMarginとして設定する
            ViewGroup.LayoutParams lp = previewView.getLayoutParams();
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            totalMargin = pt.y - ((pt.x / 16) * 9);
            mlp.setMargins(0,totalMargin / 2,0,totalMargin / 2);
            previewView.setLayoutParams(mlp);
        }

        //ルーム番号とデバイス番号をMainActivityに保存した変数から読み込む
        roomNumber = MainActivity.roomNumber;
        deviceNumber = MainActivity.deviceNumber;

        //画面が暗くなるか否かの設定を取得
        dark = getDarkSetting();

        //デバイス情報を格納するFirebaseの場所を代入
        devices = ReadWrite.ref.child(roomNumber).child("devices");

        //Firebase上のルームが変更されたことを検知するリスナー
        ReadWrite.ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            //参加しているルームが削除されたときの動作
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
        //今すぐ撮影モード有効時の撮影命令を受け取るリスナー
        ReadWrite.ref.child(roomNumber).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {
                if(snapshot.getKey().equals("QuickShot")){
                    capturePhoto();
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
        //Firebase上の設定関連の変更を検知するリスナー
        ReadWrite.ref.child(roomNumber).child("Settings").addChildEventListener(new ChildEventListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {
                //受け取った情報をSwitch文で処理
                switch (Objects.requireNonNull(snapshot.getKey())) {
                    //解像度の設定
                    case "resolution":
                        switch (String.valueOf(snapshot.getValue())) {
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
                    //撮影開始時間
                    case "start":
                        startTime = Integer.parseInt((String) Objects.requireNonNull(snapshot.getValue()));
                        break;
                    //動画・静止画モードの設定
                    case "video":
                        videoMode = String.valueOf(snapshot.getValue()).equals("true");
                        break;
                    //撮影終了
                    case "end":
                        endTime = Integer.parseInt((String) Objects.requireNonNull(snapshot.getValue()));
                        //NTPサーバーとの差を考慮して撮影終了時間を算出
                        int i = endTime - MainActivity.getToday() + MainActivity.timeLag - deviceCameraLag;
                        //撮影終了時間になったら撮影終了する
                        new Handler().postDelayed(funcVe, i);
                }
                count++;
                //撮影開始処理
                if (count == 3) {
                    //NTPサーバーとの差を考慮して撮影開始時間を算出
                    int i = startTime - MainActivity.getToday() + MainActivity.timeLag - deviceCameraLag;
                    //撮影モードの判定
                    if (videoMode) {
                        //撮影開始までのカウントダウンを開始
                        new Handler().postDelayed(funcV, i);
                        //動画モードでカメラを起動
                        previewView.post(() -> cameraProviderFuture.addListener(() -> {
                            try {
                                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                                startCameraXv(cameraProvider);
                            } catch (ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }, getExecutor()));
                    } else {
                        //撮影開始までのカウントダウンを開始
                        new Handler().postDelayed(funcC, i);
                        //静止画モードでカメラを起動
                        previewView.post(() -> cameraProviderFuture.addListener(() -> {
                            try {
                                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                                startCameraX(cameraProvider);
                            } catch (ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }, getExecutor()));
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

        //下のバー消去
        immersiveMode();

        previewView = findViewById(R.id.previewView);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        //静止画画面作成
        previewView.post(() -> cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, getExecutor()));
    }

    //画面停止時にFirebaseからデバイス情報を削除
    @Override
    protected void onStop() {
        super.onStop();
        devices.child(deviceNumber).removeValue();
    }

    //再開時にMainActivityに戻る
    @Override
    protected void onRestart() {
        super.onRestart();
        finish();
    }

    //画面出力　
    Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    //静止画画面作成
    @SuppressLint({"RestrictedApi", "SetTextI18n"})
    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // preview
        Preview preview = new Preview.Builder()
                //ここにアスペクト比解像度
                .setTargetResolution(new Size(resolutionX, resolutionY))
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());


        // Image capture use case
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                //ここにアスペクト比解像度
                .setTargetResolution(new Size(resolutionX, resolutionY))
                .build();

        // Image analysis use case
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(getExecutor(), this);

        //bind to lifecycle:
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        TextView textView = findViewById(R.id.tvData);
        textView.setText("　　　" + roomNumber + " " + deviceNumber.substring(6, 8) + " " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
    }

    //動画画面作成
    @SuppressLint({"RestrictedApi", "SetTextI18n"})
    private void startCameraXv(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // preview
        Preview preview = new Preview.Builder()
                //ここにアスペクト比解像度
                .setTargetResolution(new Size(resolutionX, resolutionY))
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image capture use case
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                //ここにアスペクト比解像度
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
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);
        TextView textView = findViewById(R.id.tvData);
        //ゲスト画面上に情報を表示
        textView.setText("　　　" + roomNumber + " " + deviceNumber.substring(6, 8) + " " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        image.close();
    }

    //動画保存メソッド
    @SuppressLint("RestrictedApi")
    private void recordVideo() {

        //画面を暗くする
        if (dark) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = 0.01F;
            getWindow().setAttributes(lp);
        }

//        File movieDir;

        //API Levelによってフォルダの変更
        if (videoCapture != null) {

            long timestamp = System.currentTimeMillis();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
//            int apiInt = Build.VERSION.SDK_INT;
//            if (apiInt <= 29) {
//                final String SAVE_DIR = "/DCIM/SYNCAM";
//                movieDir = new File(Environment.getExternalStorageDirectory().getPath() + SAVE_DIR);
//            } else {
//                movieDir = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES) + "/");
//            }
//
//            if (!movieDir.exists()) movieDir.mkdir();
//
//            //ファイル名作成
//            Date date = new Date();
//            String timestamp = String.valueOf(date.getTime());
//            String vidFilePath = movieDir.getAbsolutePath() + "/" + android.os.Build.MODEL + "_" + timestamp + ".mp4";
//            File vidFile = new File(vidFilePath);
            //保存
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                videoCapture.startRecording(
                        new VideoCapture.OutputFileOptions.Builder(
                                getContentResolver(),
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                        ).build(),
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
                                if (dark) {
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

            //フォト内に表示&フォーマットなどの選択
//            ContentValues values = new ContentValues();
//            ContentResolver contentResolver = getContentResolver();
//            values.put(MIME_TYPE,"video/mp4");
//            values.put(MediaStore.Video.Media.TITLE, vidFilePath);
//            values.put("_data", vidFilePath);
//            contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        }
    }

    //画像保存メソッド
    private void capturePhoto() {

        long timestamp = System.currentTimeMillis();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
//        File photoDir;
//        //API Levelによってフォルダの変更
//        int apiInt = Build.VERSION.SDK_INT;
//        if (apiInt <= 29) {
//            final String SAVE_DIR = "/DCIM/SYNCAM";
//            photoDir = new File(Environment.getExternalStorageDirectory().getPath() + SAVE_DIR);
//        } else {
//            photoDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/");
//        }
//        if (!photoDir.exists())
//            photoDir.mkdir();
//        //ファイル名作成
//        Date date = new Date();
//        String timestamp = String.valueOf(date.getTime());
//        String photoFilePath = photoDir.getAbsolutePath() + "/" + android.os.Build.MODEL + "_" + timestamp + ".jpg";
//        File photoFile = new File(photoFilePath);

        //保存
        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues).build(),
                getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(GuestActivity.this, "保存しました", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                    }
                }
        );
        //フォト内に表示&フォーマットなどの選択
//        ContentValues values = new ContentValues();
//        ContentResolver contentResolver = getContentResolver();
//        values.put(MIME_TYPE, "image/jpeg");
//        values.put(MediaStore.Images.Media.TITLE, photoFilePath);
//        values.put("_data", photoFilePath);
//        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    //下のバー消去メソッド
    private void immersiveMode() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );
        Handler h = new Handler();
        decorView.setOnSystemUiVisibilityChangeListener
                (visibility -> {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        //3秒でもう一度下のバー消去
                        h.postDelayed(this::immersiveMode, 3 * 1000);
                    }
                });
    }

    //写真撮影
    private final Runnable funcC = () -> {
        //写真撮影
        capturePhoto();
        //カウンターのリセット
        count = 0;
        //解像度のリセット
        resolutionX = 3840;
        resolutionY = 2160;
        new Handler().postDelayed(() ->{
            //静止画画面で待機
            previewView.post(() -> cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    startCameraX(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }, getExecutor()));
        },1500);
    };

    //動画撮影開始
    private final Runnable funcV = () -> {
        //動画撮影開始
        recordVideo();
        //カウンターのリセット
        count = 0;
    };

    //動画撮影終了
    private final Runnable funcVe = new Runnable() {
        @SuppressLint("RestrictedApi")
        @Override
        public void run() {
            //動画撮影終了
            videoCapture.stopRecording();
            //カウンターのリセット
            count = 0;
            //解像度のリセット
            resolutionX = 3840;
            resolutionY = 2160;
            new Handler().postDelayed(() ->{
                //静止画画面で待機
                previewView.post(() -> cameraProviderFuture.addListener(() -> {
                    try {
                        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                        startCameraX(cameraProvider);
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }, getExecutor()));
            },1500);
        }
    };

    //画面が暗くなるか否かの設定と端末のラグ修正設定を取得
    boolean getDarkSetting() {
        //共有プリファレンスの準備
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(GuestActivity.this);
        //端末固有のラグを修正する設定を取得
        if(sharedPreferences.getString("Syncam-Setting-CameraLag", "0").equals("")){
            deviceCameraLag = 0;
        }else {
            deviceCameraLag = Integer.parseInt(sharedPreferences.getString("Syncam-Setting-CameraLag", "0"));
        }
        //画面を暗くする設定値の返却
        return sharedPreferences.getBoolean("Syncam-Setting-dark", true);
    }

    //ディスプレイ解像度を取得
    public Point getRealDisplaySize(){
        Display display = GuestActivity.this.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getRealSize(point);
        return point;
    }
}