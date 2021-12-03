package com.example.syncam;

import android.annotation.SuppressLint;
import android.app.AppComponentFactory;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.firebase.client.Firebase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        setContentView(R.layout.activity_main);
        Button btn1 = (Button) findViewById(R.id.bJoin);
        btn1.setOnClickListener(this);
        Button btn2 = (Button) findViewById(R.id.bSet);
        btn2.setOnClickListener(this);
    }
    static String rn;
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.bJoin) {
            DialogFragment dialogFragment = new myDialogFragment();
            dialogFragment.show(getSupportFragmentManager(), "my_dialog");
        }else if(view.getId() == R.id.bSet){
            Random r = new Random();
            rn = String.valueOf(r.nextInt(1000000));
            for(int i = rn.length(); i < 6; i++){
                rn = "0" + rn;
            }
            Toast.makeText(MainActivity.this,"Number = " + rn,Toast.LENGTH_SHORT).show();
            ReadWrite.ref.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if(String.valueOf(Objects.requireNonNull(task.getResult()).getValue()).contains("number=" + rn)) {
                        onClick(findViewById(R.id.bSet));
                    }else{
                        HostActivity.flag = true;
                        ReadWrite.SendRoomNumber(rn);
                        Intent intent = new Intent(MainActivity.this, HostActivity.class);
                        startActivity(intent);
                    }
                }
            });
        }
    }
}

class RoomInfo{
    String number;
    RoomInfo(String s){
        number = s;
    }
    public String getNumber() {
        return number;
    }
}

class DeviceInfo{
    String number;
    String manufacturer;
    String model;
    DeviceInfo(String s1,String s2,String s3){
        number = s1;
        manufacturer = s2;
        model = s3;
    }
    public String getNumber(){
        return number;
    }
    public String getManufacturer(){
        return manufacturer;
    }
    public String getModel(){
        return model;
    }
}
@SuppressLint("NewApi")
class ReadWrite extends AppComponentFactory{
    static final FirebaseDatabase database = FirebaseDatabase.getInstance();
    static DatabaseReference ref = database.getReference("room");
    static void SendRoomNumber(String s){
        ref.child(s).setValue(new RoomInfo(s));
    }
    static void SendDeviceInfo(String s, String s1, String s2, String s3){
        DatabaseReference cref = ref.child(s);
        cref.child(s1).setValue(new DeviceInfo(s1,s2,s3));
    }
}