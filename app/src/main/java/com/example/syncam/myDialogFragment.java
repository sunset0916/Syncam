package com.example.syncam;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Debug;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import java.util.Objects;

public class myDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        EditText editText =new EditText(getActivity());
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("ルーム参加")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ReadWrite.ref.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DataSnapshot> task) {
                                if(String.valueOf(Objects.requireNonNull(task.getResult()).getValue()).contains("number=" + editText.getText())){
                                    DatabaseReference cref = ReadWrite.ref.child(String.valueOf(editText.getText()));
                                    cref.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                                            String s1 = "device11",s2,s3;
                                            for(int i = 1; i < 12; i++){
                                                if(String.valueOf(Objects.requireNonNull(task.getResult()).getValue()).contains("device" + String.valueOf(i))) {
                                                }else{
                                                    s1 = "device" + String.valueOf(i);
                                                    break;
                                                }
                                            }
                                            if(!s1.equals("device11")){
                                                String s = String.valueOf(editText.getText());
                                                ReadWrite.SendDeviceInfo(s,s1,android.os.Build.MANUFACTURER,android.os.Build.MODEL);
                                            }
                                        }
                                    });
                                }
                            }
                        });
                    }
                })
                .setView(editText)
                .setNegativeButton("キャンセル",null);
        return builder.create();
    }
}