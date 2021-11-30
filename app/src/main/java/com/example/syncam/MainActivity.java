package com.example.syncam;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.annotation.SuppressLint;
import android.app.AppComponentFactory;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.client.Firebase;

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

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.bJoin) {
            DialogFragment dialogFragment = new myDialogFragment();
            dialogFragment.show(getSupportFragmentManager(), "my_dialog");
        }else if(view.getId() == R.id.bSet){
            Random r = new Random();
            String rn = String.valueOf(r.nextInt(1000000));
            for(int i = rn.length(); i < 6; i++){
                rn = "0" + rn;
            }
            ReadWrite.SendRoomNumber(rn);
            Toast.makeText(MainActivity.this,"Number = " + rn,Toast.LENGTH_SHORT).show();
        }
    }
}

class RoomInfo{
    String number;
    RoomInfo(String s){
        number = s;
    }
}
@SuppressLint("NewApi")
class ReadWrite extends AppComponentFactory{
    static void SendRoomNumber(String s){
        Firebase rooms = new Firebase("https://sunset0916-syncam-default-rtdb.firebaseio.com/room");
        rooms.child(s).setValue(new RoomInfo(s));
    }
}