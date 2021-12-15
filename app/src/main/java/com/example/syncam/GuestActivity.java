package com.example.syncam;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import java.util.Objects;

public class GuestActivity extends AppCompatActivity {

    static String deviceNumber = "";
    String roomNumber;
    DatabaseReference devices = ReadWrite.ref.child("room").child(roomNumber).child("devices");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest);
        roomNumber = MainActivity.rn;
    }

    @Override
    protected void onStop() {
        super.onStop();
        devices.child(deviceNumber).removeValue();
        MainActivity.rn = null;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        devices.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if(!Objects.requireNonNull(Objects.requireNonNull(task.getResult()).getValue()).toString().contains(roomNumber)){
                    finish();
                }
            }
        });
    }
}