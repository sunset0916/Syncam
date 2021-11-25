package com.example.syncam;

import androidx.fragment.app.DialogFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class myDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@NonNull Bundle savedInstanceState){
        EditText editText =new EditText(getActivity());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());



        builder.setTitle("ルーム参加")

                .setTitle("ここにルーム番号入力してください")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setView(editText)
                .setNegativeButton("キャンセル",null);
        return builder.create();
    }
}
