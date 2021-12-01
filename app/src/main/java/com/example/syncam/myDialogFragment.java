package com.example.syncam;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;

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
                                if(String.valueOf(Objects.requireNonNull(task.getResult()).getValue()).contains("number=" + String.valueOf(editText.getText()))){
                                    ReadWrite.ref.child(String.valueOf(editText.getText())).removeValue();
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