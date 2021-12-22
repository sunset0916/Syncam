package com.example.syncam;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

    int REQUEST_CODE_FOR_PERMISSIONS = 1234;
    final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.RECORD_AUDIO"};

    public String ntphh;
    public String ntpmm;
    public String ntpss;
    public String ntpSSS;

    static int timeLag;

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        setContentView(R.layout.activity_main);
        Button btn1 = findViewById(R.id.bJoin);
        btn1.setOnClickListener(this);
        Button btn2 = findViewById(R.id.bSet);
        btn2.setOnClickListener(this);

        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_FOR_PERMISSIONS);
        }

        new NTPTask() {
            @SuppressLint({"StaticFieldLeak", "SetTextI18n"})
            @Override
            protected void onPostExecute(String text) {
                super.onPostExecute(text);

                ntphh = text.substring(0, 2);
                ntpmm = text.substring(3, 5);
                ntpss = text.substring(6, 8);
                ntpSSS = text.substring(9, 12);

                int ntptimeh = Integer.parseInt(ntphh);
                int ntptimem = Integer.parseInt(ntpmm);
                int ntptimes = Integer.parseInt(ntpss);
                int ntptimeS = Integer.parseInt(ntpSSS);

                ntptimeh = ntptimeh * 60;
                ntptimem = ntptimem + ntptimeh;
                ntptimem = ntptimem * 60;
                ntptimes = ntptimes + ntptimem;
                ntptimes = ntptimes * 1000;
                ntptimeS = ntptimeS + ntptimes;
                Log.d("ntptime", String.valueOf(ntptimeS) );
                timeLag = getToday() - ntptimeS;

                Log.d("Lagtime", String.valueOf(timeLag) );
            }
        }
                .execute();
    }

    @SuppressLint("NewApi")
    public static int getToday() {
        Date date = new Date();

        @SuppressLint("SimpleDateFormat") SimpleDateFormat hourformat = new SimpleDateFormat("HH");
        String HHformat=hourformat.format(date);
        int hour=Integer.parseInt(HHformat);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat minuteformat = new SimpleDateFormat("mm");
        String mmformat=minuteformat.format(date);
        int minute=Integer.parseInt(mmformat);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat secondformat = new SimpleDateFormat("ss");
        String ssformat=secondformat.format(date);
        int second=Integer.parseInt(ssformat);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat millisecondformat = new SimpleDateFormat("SSS");
        String SSSformat=millisecondformat.format(date);
        int millisecond=Integer.parseInt(SSSformat);
        int phonetimeh = hour * 60;
        int phonetimem = minute + phonetimeh;
        phonetimem = phonetimem * 60;
        int phonetimes = second + phonetimem;
        phonetimes = phonetimes * 1000;
        int phonetimeS = millisecond + phonetimes;
        Log.d("phonetime", String.valueOf(phonetimeS) );
        return phonetimeS;
    }
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
    static String rn = null;
    static String roomNumber;
    static String deviceNumber;
    boolean connect = false;

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
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
        if(connect) {
            if (view.getId() == R.id.bJoin) {
                DialogFragment dialogFragment = new myDialogFragment();
                dialogFragment.show(getSupportFragmentManager(), "my_dialog");
            } else if (view.getId() == R.id.bSet) {
                Random r = new Random();
                rn = String.valueOf(r.nextInt(1000000));
                for (int i = rn.length(); i < 6; i++) {
                    rn = "0" + rn;
                }
                ReadWrite.ref.get().addOnCompleteListener(task -> {
                    if (String.valueOf(Objects.requireNonNull(task.getResult()).getValue()).contains("roomNumber=" + rn)) {
                        onClick(findViewById(R.id.bSet));
                    } else {
                        HostActivity.flag = true;
                        ReadWrite.SendRoomNumber(rn);
                        Intent intent = new Intent(MainActivity.this, HostActivity.class);
                        startActivity(intent);
                    }
                });
            }
        }else{
            Toast.makeText(MainActivity.this,"データベースに接続できませんでした",Toast.LENGTH_SHORT).show();
        }
    }
}

class RoomInfo{
    String roomNumber;
    RoomInfo(String s){
        roomNumber = s;
    }
    public String getRoomNumber() {
        return roomNumber;
    }
}

class DeviceInfo{
    String deviceNumber;
    String manufacturer;
    String model;
    DeviceInfo(String s1,String s2,String s3){
        deviceNumber = s1;
        manufacturer = s2;
        model = s3;
    }
    public String getDeviceNumber(){
        return deviceNumber;
    }
    public String getManufacturer(){
        return manufacturer;
    }
    public String getModel(){
        return model;
    }
}

class Settings{
    String dark;
    String video;
    String start;
    String resolution;
    Settings(String a,String b,String c,String d){
        dark = a;
        video = b;
        start = c;
        resolution = d;
    }
    public String getDark() {
        return dark;
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

class EndTime{
    String end;
    EndTime(String s){
        end = s;
    }
    public String getEnd() {
        return end;
    }
}

class ReadWrite extends AppCompatActivity{
    static final FirebaseDatabase database = FirebaseDatabase.getInstance();
    static DatabaseReference ref = database.getReference("room");
    static void SendRoomNumber(String s){
        ref.child(s).setValue(new RoomInfo(s));
    }
    static void SendDeviceInfo(String s, String s1, String s2, String s3){
        DatabaseReference room = ref.child(s);
        DatabaseReference devices = room.child("devices");
        devices.child(s1).setValue(new DeviceInfo(s1,s2,s3));
    }
    static void SendSettings(String a,String b,String c,String d){
        DatabaseReference settings = ref.child(MainActivity.rn).child("Settings");
        settings.setValue(new Settings(a, b, c, d));
    }
}