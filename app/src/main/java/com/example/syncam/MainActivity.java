package com.example.syncam;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.firebase.client.Firebase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.Objects;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //権限の変数
    int REQUEST_CODE_FOR_PERMISSIONS = 1234;
    final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.RECORD_AUDIO"};

    //NTPから取得した時刻を格納する変数
    public String ntphh;
    public String ntpmm;
    public String ntpss;
    public String ntpSSS;

    //NTPから取得した時刻と端末の時刻との差をミリ秒単位で保持する変数
    static int timeLag;

    //ホスト側で生成したルーム番号を保持する変数
    static String rn = null;

    //ゲスト側で入力されたルーム番号を保持する変数
    static String roomNumber;

    //ゲスト側で生成されたデバイス番号を保持する変数
    static String deviceNumber;

    //Firebaseとの接続状況を格納する変数
    boolean connect;

    //ネット未接続時のダイアログ
    AlertDialog alertDialog;

    //タイトルバーの生成
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.setting, menu);
        return true;
    }

    //設定（歯車）ボタンが押されたときの動作
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_button) {
            //連打防止の為全ボタン無効化
            findViewById(R.id.bSet).setEnabled(false);
            findViewById(R.id.bJoin).setEnabled(false);
            findViewById(R.id.action_button).setEnabled(false);
            //設定画面に遷移
            Intent intent = new Intent(MainActivity.this, GuestSettingsActivity.class);
            startActivity(intent);
            //0.5秒後にボタン有効化
            new Handler().postDelayed(() -> {
                findViewById(R.id.bSet).setEnabled(true);
                findViewById(R.id.bJoin).setEnabled(true);
                findViewById(R.id.action_button).setEnabled(true);
            }, 500);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_Syncam);

        //Firebaseの準備
        Firebase.setAndroidContext(this);

        //ボタン等の準備
        setContentView(R.layout.activity_main);
        Button btn1 = findViewById(R.id.bJoin);
        btn1.setOnClickListener(this);
        Button btn2 = findViewById(R.id.bSet);
        btn2.setOnClickListener(this);

        //ダークモード無効
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        //権限取得
        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_FOR_PERMISSIONS);
        }

        //インターネット接続の有無を取得
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            //NTPサーバーと端末から時刻を取得して差を算出する
            new NTPTask() {
                @SuppressLint({"StaticFieldLeak", "SetTextI18n"})
                @Override
                protected void onPostExecute(String text) {
                    super.onPostExecute(text);

                    //HH:mm:ss:SSSのフォーマットの文字列から数字を取り出す
                    ntphh = text.substring(0, 2);
                    ntpmm = text.substring(3, 5);
                    ntpss = text.substring(6, 8);
                    ntpSSS = text.substring(9, 12);

                    //取り出した数字を数値化に型変換
                    int ntptimeh = Integer.parseInt(ntphh);
                    int ntptimem = Integer.parseInt(ntpmm);
                    int ntptimes = Integer.parseInt(ntpss);
                    int ntptimeS = Integer.parseInt(ntpSSS);

                    //時間をミリ秒に統一
                    ntptimeh = ntptimeh * 60;
                    ntptimem = ntptimem + ntptimeh;
                    ntptimem = ntptimem * 60;
                    ntptimes = ntptimes + ntptimem;
                    ntptimes = ntptimes * 1000;
                    ntptimeS = ntptimeS + ntptimes;

                    //端末の時刻からNTPの時刻を引いて差を算出
                    timeLag = getToday() - ntptimeS;
                }
            }
                    .execute();
        } else {
            //ネットに接続していない場合、ダイアログを出して終了
            alertDialog = new AlertDialog.Builder(MainActivity.this)
                    .setCancelable(false)
                    .setTitle("起動できません")
                    .setMessage("インターネット接続がありません。" + "\n" + "インターネットに接続してからやり直してください。")
                    .setPositiveButton("終了", (dialogInterface, i) -> finish())
                    .show();
        }
        //一回問い合わせておかないとボタン押したときに接続できていない扱いになるので何もしないけど問い合わせ
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                connect = snapshot.getValue(Boolean.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    //端末の時刻をミリ秒単位で整数型で取得
    @SuppressLint("NewApi")
    public static int getToday() {
        //端末の時刻をDate型で取得
        Date date = new Date();

        //文字列型として時・分・秒・ミリ秒の数字部分を抜き出して数値型に変換
        @SuppressLint("SimpleDateFormat") SimpleDateFormat hourformat = new SimpleDateFormat("HH");
        String HHformat = hourformat.format(date);
        int hour = Integer.parseInt(HHformat);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat minuteformat = new SimpleDateFormat("mm");
        String mmformat = minuteformat.format(date);
        int minute = Integer.parseInt(mmformat);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat secondformat = new SimpleDateFormat("ss");
        String ssformat = secondformat.format(date);
        int second = Integer.parseInt(ssformat);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat millisecondformat = new SimpleDateFormat("SSS");
        String SSSformat = millisecondformat.format(date);
        int millisecond = Integer.parseInt(SSSformat);

        //時間をミリ秒に統一し、返却
        int phonetimeh = hour * 60;
        int phonetimem = minute + phonetimeh;
        phonetimem = phonetimem * 60;
        int phonetimes = second + phonetimem;
        phonetimes = phonetimes * 1000;
        return millisecond + phonetimes;
    }

    //権限取得メソッド
    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    //ボタンを押されたときの動作
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        //連打防止の為全ボタン無効化
        findViewById(R.id.bSet).setEnabled(false);
        findViewById(R.id.bJoin).setEnabled(false);
        findViewById(R.id.action_button).setEnabled(false);
        //Firebaseへの接続状況をconnectに格納
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                connect = snapshot.getValue(Boolean.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
        //Firebaseに接続できているかの判断
        if (connect) {
            //サーバーメンテナンス中かどうか確認
            DatabaseReference status = FirebaseDatabase.getInstance().getReference("status");
            status.get().addOnCompleteListener(task -> {
                if (task.getResult().child("active").getValue().toString().equals("false")) {
                    alertDialog = new AlertDialog.Builder(MainActivity.this)
                            .setCancelable(false)
                            .setTitle("サーバーメンテナンス")
                            .setMessage(task.getResult().child("info").getValue().toString())
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    //メンテナンス中でない場合の処理
                    if (view.getId() == R.id.bJoin) {
                        //ルーム参加ダイアログの表示
                        DialogFragment dialogFragment = new myDialogFragment();
                        dialogFragment.show(getSupportFragmentManager(), "my_dialog");
                    } else if (view.getId() == R.id.bSet) {
                        //ルームの生成
                        Random r = new Random();
                        //0~999999の間で乱数を生成
                        rn = String.valueOf(r.nextInt(1000000));
                        //6桁に満たない場合は6桁になるまで先頭に0を追加する
                        for (int i = rn.length(); i < 6; i++) {
                            rn = "0" + rn;
                        }
                        //Firebaseへ問い合わせ
                        ReadWrite.ref.get().addOnCompleteListener(task2 -> {
                            //生成したルーム番号がFirebaseに存在するかの判断
                            if (String.valueOf(Objects.requireNonNull(task2.getResult()).getValue()).contains("roomNumber=" + rn)) {
                                //番号生成やり直し
                                onClick(findViewById(R.id.bSet));
                            } else {
                                //画面遷移の状態を保持する変数を初期化
                                HostActivity.flag = true;
                                //Firebaseに生成したルーム番号を送信
                                ReadWrite.SendRoomNumber(rn);
                                //ホスト画面に遷移
                                Intent intent = new Intent(MainActivity.this, HostActivity.class);
                                startActivity(intent);
                            }
                        });
                    }
                }
            });
        } else {
            Toast.makeText(MainActivity.this, "データベースに接続できませんでした", Toast.LENGTH_SHORT).show();
        }
        //2秒後にボタン有効化
        new Handler().postDelayed(() -> {
            findViewById(R.id.bSet).setEnabled(true);
            findViewById(R.id.bJoin).setEnabled(true);
            findViewById(R.id.action_button).setEnabled(true);
        }, 2000);
    }
}

//ここから下Firebaseに送信するデータと送信機能

//ルーム番号
class RoomInfo {
    String roomNumber;

    RoomInfo(String s) {
        roomNumber = s;
    }

    public String getRoomNumber() {
        return roomNumber;
    }
}

//ゲスト端末のデバイス情報
class DeviceInfo {
    String deviceNumber;
    String manufacturer;
    String model;

    DeviceInfo(String s1, String s2, String s3) {
        deviceNumber = s1;
        manufacturer = s2;
        model = s3;
    }

    public String getDeviceNumber() {
        return deviceNumber;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }
}

//撮影開始時間とカメラ設定
class Settings {
    String video;
    String start;
    String resolution;

    Settings(String a, String b, String c) {
        video = a;
        start = b;
        resolution = c;
    }

    public String getVideo() {
        return video;
    }

    public String getStart() {
        return start;
    }

    public String getResolution() {
        return resolution;
    }
}

//撮影終了時間
class EndTime {
    String end;

    EndTime(String s) {
        end = s;
    }

    public String getEnd() {
        return end;
    }
}

//Firebaseへのデータ送信関係をまとめたやつ
class ReadWrite extends AppCompatActivity {
    //Firebaseの準備
    static final FirebaseDatabase database = FirebaseDatabase.getInstance();
    static DatabaseReference ref = database.getReference("room");

    //ルーム番号を送信
    static void SendRoomNumber(String s) {
        ref.child(s).setValue(new RoomInfo(s));
    }

    //デバイス情報の送信
    static void SendDeviceInfo(String s, String s1, String s2, String s3) {
        DatabaseReference room = ref.child(s);
        DatabaseReference devices = room.child("devices");
        devices.child(s1).setValue(new DeviceInfo(s1, s2, s3));
    }

    //撮影開始時間とカメラ設定の送信
    static void SendSettings(String a, String b, String c) {
        DatabaseReference settings = ref.child(MainActivity.rn).child("Settings");
        settings.setValue(new Settings(a, b, c));
    }
}