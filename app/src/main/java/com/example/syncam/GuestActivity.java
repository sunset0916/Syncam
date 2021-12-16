package com.example.syncam;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.util.Objects;

public class GuestActivity extends AppCompatActivity {

    String deviceNumber;
    String roomNumber;
    DatabaseReference devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest);
        roomNumber = MainActivity.roomNumber;
        deviceNumber = MainActivity.deviceNumber;
        devices = ReadWrite.ref.child(roomNumber).child("devices");
        Log.d("variable",roomNumber + deviceNumber);
        ReadWrite.ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                if(Objects.requireNonNull(snapshot.getKey()).toString().equals(roomNumber)){
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
}