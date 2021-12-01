package com.example.syncam;

import android.annotation.SuppressLint;
import android.app.AppComponentFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.firebase.client.Firebase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
    public String getNumber() {
        return number;
    }
}

@SuppressLint("NewApi")
class ReadWrite extends AppComponentFactory{
    static void SendRoomNumber(String s){
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("room");
        ref.child(s).setValue(new RoomInfo(s));
    }
}