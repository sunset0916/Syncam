package com.example.syncam;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class HostActivity extends AppCompatActivity implements View.OnClickListener {
    boolean endFlag = false;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.setting, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_button) {
            flag = false;
            Intent intent = new Intent(HostActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hostactivity);

        TextView tvc = findViewById(R.id.tvCount);
        TextView textView = findViewById(R.id.tvNumber);
        textView.setText(MainActivity.rn);
        LinearLayout l2 = findViewById(R.id.ll1);
        ImageButton IBC = findViewById(R.id.imageC);
        IBC.setOnClickListener(this);
        ImageButton IBV = findViewById(R.id.imageV);
        IBV.setOnClickListener(this);
        TextView tv1= findViewById(R.id.tvTime);

        IBC.setEnabled(false);
        IBV.setEnabled(true);
        tv1.setVisibility(View.INVISIBLE);

        endFlag = false;

        DatabaseReference room = ReadWrite.ref.child(MainActivity.rn);
        room.child("devices").addChildEventListener(new ChildEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String s = String.valueOf(snapshot.getValue());
                int start, end;
                String model, deviceNumber, manufacturer;
                if (s.contains(", model=")) {
                    start = s.indexOf(", model=") + 8;
                    if ((s.indexOf(", deviceNumber=") - start) < 0 && (s.indexOf(", manufacturer=") - start) < 0) {
                        end = s.indexOf("}");
                        model = s.substring(start, end);
                        if (s.indexOf("deviceNumber=") < s.indexOf("manufacturer=")) {
                            start = 20;
                            end = s.indexOf(", manufacturer=");
                            deviceNumber = s.substring(start, end);
                            start += 17;
                            end = s.indexOf(", model=");
                            manufacturer = s.substring(start, end);
                        } else {
                            start = 14;
                            end = s.indexOf(", deviceNumber=");
                            manufacturer = s.substring(start, end);
                            start = end + 21;
                            end = s.indexOf(", model=");
                            deviceNumber = s.substring(start, end);
                        }
                    } else if (s.indexOf("deviceNumber=") < s.indexOf("manufacturer=")) {
                        end = s.indexOf(", manufacturer=");
                        model = s.substring(start, end);
                        start = 20;
                        end = s.indexOf(", model=");
                        deviceNumber = s.substring(start, end);
                        start = s.indexOf(", manufacturer=") + 15;
                        end = s.indexOf("}");
                        manufacturer = s.substring(start, end);
                    } else {
                        end = s.indexOf(", deviceNumber=");
                        model = s.substring(start, end);
                        start = 14;
                        end = s.indexOf(", model=");
                        manufacturer = s.substring(start, end);
                        start = s.indexOf(", deviceNumber=") + 21;
                        end = s.indexOf("}");
                        deviceNumber = s.substring(start, end);
                    }
                } else {
                    start = 7;
                    if (s.indexOf(", deviceNumber=") < s.indexOf(", manufacturer=")) {
                        end = s.indexOf(", deviceNumber=");
                        model = s.substring(start, end);
                        start = end + 21;
                        end = s.indexOf(", manufacturer=");
                        deviceNumber = s.substring(start, end);
                        start = end + 15;
                        end = s.indexOf("}");
                        manufacturer = s.substring(start, end);
                    } else {
                        end = s.indexOf(", manufacturer=");
                        model = s.substring(start, end);
                        start = end + 15;
                        end = s.indexOf(", deviceNumber=");
                        manufacturer = s.substring(start, end);
                        start = end + 21;
                        end = s.indexOf("}");
                        deviceNumber = s.substring(start, end);
                    }
                }
                TextView tv = new TextView(HostActivity.this);
                tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                tv.setText(deviceNumber + " " + manufacturer + " " + model);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                tv.setId(getResources().getIdentifier(deviceNumber, "id", "com.example.syncam"));
                l2.addView(tv);
                tvc.setText((Integer.parseInt(tvc.getText().toString().substring(0, 1)) + 1) + tvc.getText().toString().substring(1, 5));
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                l2.removeView(findViewById(getResources().getIdentifier(String.valueOf(snapshot.getKey()).substring(6, 8), "id", "com.example.syncam")));
                tvc.setText((Integer.parseInt(tvc.getText().toString().substring(0, 1)) - 1) + tvc.getText().toString().substring(1, 5));
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
        findViewById(R.id.bStart).setOnClickListener(v -> {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(HostActivity.this);
            Button b = (Button) v;
            b.setEnabled(false);
            boolean record = pref.getBoolean("Syncam-Setting-record", false);
            if(b.getText().equals("撮影終了")) {
                String end;
                switch (pref.getString("Syncam-Setting-timer","5秒")){
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
                ReadWrite.ref.child(MainActivity.rn).child("Settings").setValue(new EndTime(end));
                ReadWrite.ref.child(MainActivity.rn).child("Settings").removeValue();
                b.setText("撮影開始");
                new Handler().postDelayed(endTimer,Integer.parseInt(end) - MainActivity.getToday() + MainActivity.timeLag);
                if(record) {
                    new Handler().postDelayed(funcAs, Integer.parseInt(end) - MainActivity.getToday() + MainActivity.timeLag);
                }
                new Handler().postDelayed(buttonEnabled,Integer.parseInt(end) - MainActivity.getToday() + MainActivity.timeLag);
            }else{
                String dark, video, start, resolution;
                dark = String.valueOf(pref.getBoolean("Syncam-Setting-dark", true));
                video = String.valueOf(videoMode);
                switch (pref.getString("Syncam-Setting-timer","5秒")){
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
                resolution = pref.getString("Syncam-Setting-resolution", "1080p FHD");
                ReadWrite.SendSettings(dark, video, start, resolution);
                if (videoMode) {
                    b.setText("撮影終了");
                    new Handler().postDelayed(startTimer,Integer.parseInt(start) - MainActivity.getToday() + MainActivity.timeLag);
                    if(record) {
                        new Handler().postDelayed(funcA, Integer.parseInt(start) - MainActivity.getToday() + MainActivity.timeLag);
                    }
                    new Handler().postDelayed(buttonEnabled,Integer.parseInt(start) - MainActivity.getToday() + MainActivity.timeLag);
                }else{
                    ReadWrite.ref.child(MainActivity.rn).child("Settings").removeValue();
                    new Handler().postDelayed(buttonEnabled,Integer.parseInt(start) - MainActivity.getToday() + MainActivity.timeLag);
                }
            }
        });
    }
    @SuppressLint("SetTextI18n")
    private final Runnable timerReset= () -> {
        TextView tv1 = findViewById(R.id.tvTime);
        tv1.setText("00:00:00");
    };
    private final Runnable startTimer= () -> {
        stopwatch();
        TimerStart();
    };
    private final Runnable endTimer= () -> {
        TimerStop();
        new Handler().postDelayed(timerReset,1000);
    };
    private final Runnable buttonEnabled= () -> {
        Button bStart = findViewById(R.id.bStart);
        bStart.setEnabled(true);
    };
    private Timer timer;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private TextView timerText;
    private long count,delay,period;

    protected void stopwatch(){
        long nowInmillis=System.currentTimeMillis();
        Date nowDate=new Date(nowInmillis);

        @SuppressLint("SimpleDateFormat") DateFormat format = new SimpleDateFormat("HH:mm:ss");
        String text = format.format(nowDate);

        delay=0;
        period=10;

        timerText=findViewById(R.id.tvTime);
        timerText.setText(text);
    }

    void TimerStart(){
        if(null !=timer){
            timer.cancel();
        }
        timer=new Timer();
        CountUpTimerTask timerTask = new CountUpTimerTask();

        timer.schedule(timerTask,delay,period);

        count=0;
    }
    void TimerStop(){
        if(null!=timer){
            timer.cancel();
        }
    }

    class CountUpTimerTask extends TimerTask{
        @Override
        public void run(){
            handler.post(() -> {
                count++;
                long hh=count/100/60/60;
                long mm=count/100/60%60;
                long ss=count/100%60;
                timerText.setText(
                        String.format(Locale.US,"%1$02d:%2$02d:%3$02d",hh,mm,ss));
            });
        }
    }

    static boolean flag = true;

    @Override
    protected void onStop() {
        super.onStop();
        if (flag) {
            if(!endFlag) {
                ReadWrite.ref.child(MainActivity.rn).removeValue();
                MainActivity.rn = null;
            }
        }
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        ReadWrite.ref.get().addOnCompleteListener(task -> {
            if (!String.valueOf(Objects.requireNonNull(task.getResult()).getValue()).contains("roomNumber=" + MainActivity.rn)) {
                finish();
                MainActivity.rn = null;
                endFlag = true;
            }
        });
    }

    boolean videoMode = false;

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        ImageButton IBC = findViewById(R.id.imageC);
        IBC.setOnClickListener(this);
        ImageButton IBV = findViewById(R.id.imageV);
        IBV.setOnClickListener(this);
        TextView tv1= findViewById(R.id.tvTime);
        Button bStart = findViewById(R.id.bStart);
        switch (view.getId()) {
            case R.id.imageC:
                IBC.setEnabled(false);
                IBV.setEnabled(true);
                videoMode = false;
                bStart.setText("　撮影　");
                tv1.setVisibility(View.INVISIBLE);
                break;
            case R.id.imageV:
                IBC.setEnabled(true);
                IBV.setEnabled(false);
                videoMode = true;
                bStart.setText("撮影開始");
                tv1.setVisibility(View.VISIBLE);
                break;
        }
    }
    private static File fileName;
    private MediaRecorder recorder = null;

    private void startRecording() {

        final String SAVE_DIR = "/MUSIC/AUDIO";
        fileName=new File(Environment.getExternalStorageDirectory().getPath() + SAVE_DIR);

        if (!fileName.exists())
            fileName.mkdir();

        java.util.Date date = new java.util.Date();
        String timestamp = String.valueOf(date.getTime());
        String fileNamePath = fileName.getAbsolutePath() + "/" + android.os.Build.MODEL + "_"  + timestamp + ".wav";

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        recorder.setOutputFile(fileNamePath);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try { recorder.prepare();
        } catch (IOException e) {
        }

        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    private final Runnable funcA = new Runnable() {
        @Override
        public void run() {
            startRecording();
        }
    };

    private final Runnable funcAs = new Runnable() {
        @Override
        public void run() {
            stopRecording();
        }
    };
}