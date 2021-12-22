package com.example.syncam;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.database.DatabaseReference;

import java.util.Objects;

public class myDialogFragment extends DialogFragment {
    public MainActivity activity;

    @Override
    public void onAttach(@NonNull Context activity) {
        super.onAttach(activity);
        this.activity = (MainActivity) activity;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        EditText editText =new EditText(getActivity());
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("ルーム参加")
                .setPositiveButton("OK", (dialog, which) -> ReadWrite.ref.get().addOnCompleteListener(task -> {
                    if(String.valueOf(Objects.requireNonNull(task.getResult()).getValue()).contains("roomNumber=" + editText.getText()) && editText.getText().toString().length() == 6){
                        DatabaseReference room = ReadWrite.ref.child(String.valueOf(editText.getText()));
                        DatabaseReference devices = room.child("devices");
                        devices.get().addOnCompleteListener(task1 -> {
                            String s1 = "device11";
                            for(int i = 1; i < 12; i++){
                                if(String.valueOf(i).length() == 1){
                                    s1 = "device" + "0" + i;
                                }else{
                                    s1 = "device" + i;
                                }
                                if(String.valueOf(Objects.requireNonNull(task1.getResult()).getValue()).contains(s1)) {
                                    s1 = "device11";
                                }else{
                                    break;
                                }
                            }
                            if(!s1.equals("device11")){
                                String s = String.valueOf(editText.getText());
                                ReadWrite.SendDeviceInfo(s,s1,android.os.Build.MANUFACTURER,android.os.Build.MODEL);
                                MainActivity.roomNumber = editText.getText().toString();
                                MainActivity.deviceNumber = s1;
                                Intent intent = new Intent(activity,GuestActivity.class);
                                activity.startActivity(intent);
                            }else{
                                Toast.makeText(activity,"ルームの最大接続台数に達しています",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }else{
                        Toast.makeText(activity,"指定したルームは見つかりませんでした",Toast.LENGTH_SHORT).show();
                    }
                }))
                .setView(editText)
                .setNegativeButton("キャンセル",null);
        return builder.create();
    }
}