package com.example.syncam;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class HostActivity extends AppCompatActivity implements View.OnClickListener {

    //画面遷移の状態を格納する変数
    boolean endFlag = false;

    //画面遷移の状態を格納する変数
    static boolean flag = true;

    //動画/静止画モードを格納する変数
    boolean videoMode = false;

    //レコーダー変数
    private MediaRecorder recorder = null;
    ParcelFileDescriptor file;

    //終了確認ダイアログ
    AlertDialog alertDialog;

    //タイトルバーの生成
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.setting, menu);
        return true;
    }

    //設定（歯車）ボタンが押されたときの動作
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_button) {
            //連打防止の為ボタン無効化
            findViewById(R.id.action_button).setEnabled(false);
            findViewById(R.id.bStart).setEnabled(false);
            //画面遷移の状態を保存
            flag = false;
            //設定画面に移動
            Intent intent = new Intent(HostActivity.this, SettingsActivity.class);
            startActivity(intent);
            //0.5秒後にボタン有効化
            new Handler().postDelayed(() -> {
                findViewById(R.id.action_button).setEnabled(true);
                findViewById(R.id.bStart).setEnabled(true);
            }, 500);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint({"SetTextI18n", "UseCompatLoadingForColorStateLists"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hostactivity);

        //接続台数を表示するTextView
        TextView tvc = findViewById(R.id.tvCount);
        //ルーム番号を表示するTextView
        TextView textView = findViewById(R.id.tvNumber);
        //MainActivityで生成したルーム番号をtextViewに表示
        textView.setText("ルーム：" + MainActivity.rn);
        //接続中のデバイス情報を表示するためのTextViewを配置するLinerLayout
        LinearLayout l2 = findViewById(R.id.ll1);

        //カメラマークのImageButton
        ImageButton IBC = findViewById(R.id.imageC);
        IBC.setOnClickListener(this);
        //ビデオカメラマークのImageButton
        ImageButton IBV = findViewById(R.id.imageV);
        IBV.setOnClickListener(this);

        //撮影時間を表示するTextView
        TextView tv1 = findViewById(R.id.tvTime);

        //ImageButtonを静止画モードの状態で初期化
        IBC.setEnabled(false);
        IBV.setEnabled(true);
        //撮影時間を表示するTextViewを非表示にする
        tv1.setVisibility(View.INVISIBLE);

        //画面遷移の状態を格納する変数の初期化
        endFlag = false;

        //Firebaseの問い合わせ
        DatabaseReference room = ReadWrite.ref.child(MainActivity.rn);
        room.child("devices").addChildEventListener(new ChildEventListener() {
            //デバイスが追加されたときの動作
            @SuppressLint("SetTextI18n")
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                DeviceInfo deviceInfo = new DeviceInfo(
                        snapshot.child("deviceNumber").getValue().toString(),
                        snapshot.child("manufacturer").getValue().toString(),
                        snapshot.child("model").getValue().toString()
                );
                //新たなTextViewを生成
                TextView tv = new TextView(HostActivity.this);
                //生成したTextViewのレイアウトの設定
                tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                //生成したTextViewに取り出したデバイス情報を格納
                tv.setText("  " + deviceInfo.getDeviceNumber().replace("device", "") + " " + deviceInfo.getManufacturer() + " " + deviceInfo.getModel());
                //生成したTextViewの文字サイズの設定
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                //生成したTextViewにIDを設定
                tv.setId(getResources().getIdentifier(deviceInfo.getDeviceNumber().replace("device", ""), "id", "com.example.syncam"));
                //生成したTextViewを表示
                l2.addView(tv);
                //接続台数の表示を増やす
                if (tvc.getText().toString().length() == 5) {
                    tvc.setText((Integer.parseInt(tvc.getText().toString().substring(0, 1)) + 1) + tvc.getText().toString().substring(1, 5));
                } else {
                    tvc.setText((Integer.parseInt(tvc.getText().toString().substring(0, 2)) + 1) + tvc.getText().toString().substring(2, 6));
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            //デバイスが削除されたときの動作
            @SuppressLint("SetTextI18n")
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                //TextViewを削除
                l2.removeView(findViewById(getResources().getIdentifier(snapshot.child("deviceNumber").getValue().toString().replace("device", ""), "id", "com.example.syncam")));
                //接続台数の表示を減らす
                if (tvc.getText().toString().length() == 5) {
                    tvc.setText((Integer.parseInt(tvc.getText().toString().substring(0, 1)) - 1) + tvc.getText().toString().substring(1, 5));
                } else {
                    tvc.setText((Integer.parseInt(tvc.getText().toString().substring(0, 2)) - 1) + tvc.getText().toString().substring(2, 6));
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        //撮影ボタンが押されたときの動作
        findViewById(R.id.bStart).setOnClickListener(v -> {

            //共有プリファレンスの準備
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(HostActivity.this);
            //View型になっている撮影ボタンをButtonにキャストして格納
            Button b = (Button) v;
            //撮影ボタンの無効化
            b.setEnabled(false);
            b.setBackgroundTintList(getResources().getColorStateList(R.color.gray));
            //設定（歯車）ボタンの無効化
            findViewById(R.id.action_button).setEnabled(false);
            //ImageButtonの無効化
            if (videoMode) {
                findViewById(R.id.imageC).setEnabled(false);
                findViewById(R.id.imageV).setBackgroundTintList(getResources().getColorStateList(R.color.gray));
            } else {
                findViewById(R.id.imageV).setEnabled(false);
                findViewById(R.id.imageC).setBackgroundTintList(getResources().getColorStateList(R.color.gray));
            }
            //録音機能の有効無効を格納
            boolean record = pref.getBoolean("Syncam-Setting-record", false);

            //撮影ボタンの状態の判定
            if (b.getText().equals("撮影終了")) {
                //動画撮影終了時の処理

                //撮影終了時間を格納する変数
                String end;

                //共有プリファレンスからタイマーの設定を取り出してNTPとの差を考慮して撮影終了時間を算出
                switch (pref.getString("Syncam-Setting-timer", "5秒")) {
                    case "10秒":
                        end = String.valueOf(MainActivity.getToday() + 10000 - MainActivity.timeLag);
                        break;
                    case "15秒":
                        end = String.valueOf(MainActivity.getToday() + 15000 - MainActivity.timeLag);
                        break;
                    default:
                        end = String.valueOf(MainActivity.getToday() + 5000 - MainActivity.timeLag);
                        break;
                }

                //撮影終了時間をFirebaseに送信
                ReadWrite.ref.child(MainActivity.rn).child("Settings").setValue(new EndTime(end));
                //カメラ設定関係をFirebaseから削除
                ReadWrite.ref.child(MainActivity.rn).child("Settings").removeValue();

                //撮影ボタンの表示を撮影開始に変更
                b.setText("撮影開始");

                //ストップウォッチの停止処理までのカウントダウンを開始
                new Handler().postDelayed(endTimer, Integer.parseInt(end) - MainActivity.getToday() + MainActivity.timeLag);

                //ホスト端末での録音が有効な場合、録音の停止までのカウントダウンを開始
                if (record) {
                    new Handler().postDelayed(funcAs, Integer.parseInt(end) - MainActivity.getToday() + MainActivity.timeLag);
                }

                //撮影ボタンの有効化までのカウントダウンを開始
                new Handler().postDelayed(buttonEnabled, Integer.parseInt(end) - MainActivity.getToday() + MainActivity.timeLag + 2000);
                //設定（歯車）ボタンの有効化までのカウントダウンを開始
                new Handler().postDelayed(settingButtonEnabled, Integer.parseInt(end) - MainActivity.getToday() + MainActivity.timeLag + 2000);

            } else {
                //今すぐ撮影モードが有効かどうかの判断
                if (pref.getBoolean("Syncam-Setting-quickShot", false) && !videoMode) {
                    new Handler().postDelayed(buttonEnabled, 2000);
                    new Handler().postDelayed(settingButtonEnabled, 2000);
                    ReadWrite.ref.child(MainActivity.rn).child("QuickShot").setValue("Start QuickShotMode");
                    ReadWrite.ref.child(MainActivity.rn).child("QuickShot").removeValue();
                } else {

                    //画面暗転・動画/静止画モード・撮影開始時間・解像度の設定を格納する変数
                    String video, start, resolution;

                    //動画/静止画モードを文字列型として格納
                    video = String.valueOf(videoMode);

                    //共有プリファレンスからタイマーの設定を取り出してNTPとの差を考慮して撮影開始時間を算出
                    switch (pref.getString("Syncam-Setting-timer", "5秒")) {
                        case "10秒":
                            start = String.valueOf(MainActivity.getToday() + 10000 - MainActivity.timeLag);
                            break;
                        case "15秒":
                            start = String.valueOf(MainActivity.getToday() + 15000 - MainActivity.timeLag);
                            break;
                        default:
                            start = String.valueOf(MainActivity.getToday() + 5000 - MainActivity.timeLag);
                            break;
                    }

                    //解像度の設定を共有プリファレンスから取り出して格納
                    resolution = pref.getString("Syncam-Setting-resolution", "1080p FHD");

                    //カメラ設定と撮影開始時間をFirebaseに送信
                    ReadWrite.SendSettings(video, start, resolution);

                    //動画/静止画モードの判定
                    if (videoMode) {
                        //動画モード時の動作

                        //撮影ボタンの表示を撮影終了に変更
                        b.setText("撮影終了");

                        //ストップウォッチの開始処理までのカウントダウンを開始
                        new Handler().postDelayed(startTimer, Integer.parseInt(start) - MainActivity.getToday() + MainActivity.timeLag);

                        //ホスト端末での録音が有効な場合、録音の開始までのカウントダウンを開始
                        if (record) {
                            new Handler().postDelayed(funcA, Integer.parseInt(start) - MainActivity.getToday() + MainActivity.timeLag);
                        }

                    } else {
                        //静止画モード時の動作

                        //カメラ設定関係をFirebaseから削除
                        ReadWrite.ref.child(MainActivity.rn).child("Settings").removeValue();

                        //設定（歯車）ボタンの有効化までのカウントダウンを開始
                        new Handler().postDelayed(settingButtonEnabled, Integer.parseInt(start) - MainActivity.getToday() + MainActivity.timeLag + 2000);

                    }
                    //撮影ボタンの有効化までのカウントダウンを開始
                    new Handler().postDelayed(buttonEnabled, Integer.parseInt(start) - MainActivity.getToday() + MainActivity.timeLag + 2000);
                }
            }
        });
    }

    //撮影時間を表示するTextViewの初期化
    @SuppressLint("SetTextI18n")
    private final Runnable timerReset = () -> {
        TextView tv1 = findViewById(R.id.tvTime);
        tv1.setText("00:00:00");
    };

    //ストップウォッチの開始
    private final Runnable startTimer = () -> {
        stopwatch();
        TimerStart();
    };

    //ストップウォッチの停止
    private final Runnable endTimer = () -> {
        TimerStop();
        new Handler().postDelayed(timerReset, 1000);
    };

    //撮影ボタンの有効化
    @SuppressLint("UseCompatLoadingForColorStateLists")
    private final Runnable buttonEnabled = () -> {
        Button bStart = findViewById(R.id.bStart);
        bStart.setEnabled(true);
        if (videoMode) {
            bStart.setBackgroundTintList(getResources().getColorStateList(R.color.red));
        } else {
            bStart.setBackgroundTintList(getResources().getColorStateList(R.color.darkBlue));
        }
    };

    //設定（歯車）ボタンとImageButtonの有効化
    @SuppressLint("UseCompatLoadingForColorStateLists")
    private final Runnable settingButtonEnabled = () -> {
        findViewById(R.id.action_button).setEnabled(true);
        if (videoMode) {
            findViewById(R.id.imageC).setEnabled(true);
            findViewById(R.id.imageV).setBackgroundTintList(getResources().getColorStateList(R.color.darkRed));
        } else {
            findViewById(R.id.imageV).setEnabled(true);
            findViewById(R.id.imageC).setBackgroundTintList(getResources().getColorStateList(R.color.midnight));
        }
    };

    //ストップウォッチ関連の変数
    private Timer timer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView timerText;
    private long count, delay, period;

    //ストップウォッチの動作
    protected void stopwatch() {
        long nowInmillis = System.currentTimeMillis();
        Date nowDate = new Date(nowInmillis);

        @SuppressLint("SimpleDateFormat") DateFormat format = new SimpleDateFormat("HH:mm:ss");
        String text = format.format(nowDate);

        delay = 0;
        period = 10;

        timerText = findViewById(R.id.tvTime);
        timerText.setText(text);
    }

    //ストップウォッチの開始
    void TimerStart() {
        if (null != timer) {
            timer.cancel();
        }
        timer = new Timer();
        CountUpTimerTask timerTask = new CountUpTimerTask();

        timer.schedule(timerTask, delay, period);

        count = 0;
    }

    //ストップウォッチの終了
    void TimerStop() {
        if (null != timer) {
            timer.cancel();
        }
    }

    //ストップウォッチ
    class CountUpTimerTask extends TimerTask {
        @Override
        public void run() {
            handler.post(() -> {
                count++;
                long hh = count / 100 / 60 / 60;
                long mm = count / 100 / 60 % 60;
                long ss = count / 100 % 60;
                timerText.setText(
                        String.format(Locale.US, "%1$02d:%2$02d:%3$02d", hh, mm, ss));
            });
        }
    }

    //画面停止時の動作
    @Override
    protected void onStop() {
        super.onStop();
        if (flag) {
            if (!endFlag) {
                //Firebaseからルームを削除
                ReadWrite.ref.child(MainActivity.rn).removeValue();
                MainActivity.rn = null;
            }
        }

        //録音していた場合、録音停止
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }

    //画面再開時の動作
    @Override
    protected void onRestart() {
        super.onRestart();
        ReadWrite.ref.get().addOnCompleteListener(task -> {
            if (!String.valueOf(Objects.requireNonNull(task.getResult()).getValue()).contains("roomNumber=" + MainActivity.rn)) {
                //Firebaseからルームが削除されていたときの動作
                MainActivity.rn = null;
                endFlag = true;
                finish();
            }
        });
    }

    //画面終了前の動作
    @Override
    public void finish() {
        if (!endFlag) {
            //終了確認ダイアログを表示
            alertDialog = new AlertDialog.Builder(HostActivity.this)
                    .setCancelable(false)
                    .setTitle("確認")
                    .setMessage("退出するとルームが削除されます。" + "\n" + "退出してよろしいですか？")
                    .setPositiveButton("はい", (dialogInterface, i) -> {
                        //HostActivityの終了
                        ActivityEnd();
                    })
                    .setNegativeButton("いいえ", null)
                    .show();
        } else {
            //Activityの終了
            ActivityEnd();
        }
    }

    //finish()の代わりにsuper.finish()を呼び出すメソッド
    public void ActivityEnd() {
        super.finish();
    }

    //ImageButtonを押したときの動作
    @SuppressLint({"NonConstantResourceId", "UseCompatLoadingForColorStateLists"})
    @Override
    public void onClick(View view) {
        ImageButton IBC = findViewById(R.id.imageC);
        IBC.setOnClickListener(this);
        ImageButton IBV = findViewById(R.id.imageV);
        IBV.setOnClickListener(this);
        TextView tv1 = findViewById(R.id.tvTime);
        Button bStart = findViewById(R.id.bStart);
        switch (view.getId()) {
            case R.id.imageC:
                IBC.setEnabled(false);
                IBV.setEnabled(true);
                videoMode = false;
                bStart.setBackgroundTintList(getResources().getColorStateList(R.color.darkBlue));
                bStart.setText("　撮影　");
                tv1.setVisibility(View.INVISIBLE);
                IBC.setBackgroundTintList(getResources().getColorStateList(R.color.midnight));
                IBV.setBackgroundTintList(getResources().getColorStateList(R.color.gray));
                break;
            case R.id.imageV:
                IBC.setEnabled(true);
                IBV.setEnabled(false);
                bStart.setBackgroundTintList(getResources().getColorStateList(R.color.red));
                videoMode = true;
                bStart.setText("撮影開始");
                tv1.setVisibility(View.VISIBLE);
                IBV.setBackgroundTintList(getResources().getColorStateList(R.color.darkRed));
                IBC.setBackgroundTintList(getResources().getColorStateList(R.color.gray));
                break;
        }
    }

    //録音動作
    private void startRecording() throws IOException {
        long timestamp = System.currentTimeMillis();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp + ".wav");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav");
        Uri uri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues);
        file = getContentResolver().openFileDescriptor(uri, "w");

        if (file != null) {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            recorder.setOutputFile(file.getFileDescriptor());
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            try {
                recorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //録音開始
            recorder.start();
        }
    }

    //録音停止メソッド
    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    //録音開始ハンドラー変数
    private final Runnable funcA = () -> {
        try {
            startRecording();
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    //録音停止ハンドラー変数
    private final Runnable funcAs = this::stopRecording;
}