package com.example.syncam;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HostActivity extends AppCompatActivity {
    @Override
    public boolean onCreateOptionsMenu (Menu menu){
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
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hostactivity);

        TextView tvc = (TextView)findViewById(R.id.tvCount);
        TextView textView = (TextView)findViewById(R.id.tvNumber);
        textView.setText(MainActivity.rn);
        LinearLayout l2 = (LinearLayout) findViewById(R.id.ll1);

        DatabaseReference room = ReadWrite.ref.child(MainActivity.rn);
        room.child("devices").addChildEventListener(new ChildEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String s = String.valueOf(snapshot.getValue());
                int start,end;
                String model,deviceNumber,manufacturer;
                if(s.contains(", model=")){
                    start = s.indexOf(", model=") + 8;
                    if((s.indexOf(", deviceNumber=") - start) < 0 && (s.indexOf(", manufacturer=") - start) < 0){
                        end = s.indexOf("}");
                        model = s.substring(start,end);
                        if(s.indexOf("deviceNumber=") < s.indexOf("manufacturer=")){
                            start = 20;
                            end = s.indexOf(", manufacturer=");
                            deviceNumber = s.substring(start,end);
                            start += 17;
                            end = s.indexOf(", model=");
                            manufacturer = s.substring(start,end);
                        }else{
                            start = 14;
                            end = s.indexOf(", deviceNumber=");
                            manufacturer = s.substring(start,end);
                            start = end + 21;
                            end = s.indexOf(", model=");
                            deviceNumber = s.substring(start,end);
                        }
                    }else if(s.indexOf("deviceNumber=") < s.indexOf("manufacturer=")){
                        end = s.indexOf(", manufacturer=");
                        model = s.substring(start,end);
                        start = 20;
                        end = s.indexOf(", model=");
                        deviceNumber = s.substring(start,end);
                        start = s.indexOf(", manufacturer=") + 15;
                        end = s.indexOf("}");
                        manufacturer = s.substring(start,end);
                    }else{
                        end = s.indexOf(", deviceNumber=");
                        model = s.substring(start,end);
                        start = 14;
                        end = s.indexOf(", model=");
                        manufacturer = s.substring(start,end);
                        start = s.indexOf(", deviceNumber=") + 21;
                        end = s.indexOf("}");
                        deviceNumber = s.substring(start,end);
                    }
                }else{
                    start = 7;
                    if(s.indexOf(", deviceNumber=") < s.indexOf(", manufacturer=")){
                        end = s.indexOf(", deviceNumber=");
                        model = s.substring(start,end);
                        start = end + 21;
                        end = s.indexOf(", manufacturer=");
                        deviceNumber = s.substring(start,end);
                        start = end + 15;
                        end = s.indexOf("}");
                        manufacturer = s.substring(start,end);
                    }else{
                        end = s.indexOf(", manufacturer=");
                        model = s.substring(start,end);
                        start = end + 15;
                        end = s.indexOf(", deviceNumber=");
                        manufacturer = s.substring(start,end);
                        start = end + 21;
                        end = s.indexOf("}");
                        deviceNumber = s.substring(start,end);
                    }
                }
                TextView tv = new TextView(HostActivity.this);
                tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                tv.setText(deviceNumber + " " + manufacturer + " " + model);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                tv.setId(getResources().getIdentifier(deviceNumber,"id","com.example.syncam"));
                l2.addView(tv);
                tvc.setText(String.valueOf(Integer.parseInt(tvc.getText().toString().substring(0,1)) + 1) + tvc.getText().toString().substring(1,5));
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }
            @SuppressLint("SetTextI18n")
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                l2.removeView(findViewById(getResources().getIdentifier(String.valueOf(snapshot.getKey()).substring(6,8),"id","com.example.syncam")));
                tvc.setText(String.valueOf(Integer.parseInt(tvc.getText().toString().substring(0,1)) - 1) + tvc.getText().toString().substring(1,5));
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
    static boolean flag = true;
    @Override
    protected void onStop() {
        super.onStop();
        if(flag) {
            ReadWrite.ref.child(MainActivity.rn).removeValue();
            MainActivity.rn = null;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        ReadWrite.ref.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!String.valueOf(Objects.requireNonNull(task.getResult()).getValue()).contains("roomNumber=" + MainActivity.rn)) {
                    finish();
                    MainActivity.rn = null;
                }
            }
        });
    }
}