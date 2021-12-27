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
    //MainActivityのインスタンス化
    public MainActivity activity;

    @Override
    public void onAttach(@NonNull Context activity) {
        super.onAttach(activity);
        this.activity = (MainActivity) activity;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        //EditTextの生成
        EditText editText =new EditText(getActivity());
        //入力できる文字を数字に限定
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        //ダイアログ生成
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("ルーム参加")
                //OKを選択したときの動作,Firebaseへの問い合わせ
                .setPositiveButton("OK", (dialog, which) -> ReadWrite.ref.get().addOnCompleteListener(task -> {
                    //入力されたルーム番号とFirebase上にあるルーム番号を照らし合わせる
                    if(String.valueOf(Objects.requireNonNull(task.getResult()).getValue()).contains("roomNumber=" + editText.getText()) && editText.getText().toString().length() == 6){
                        //読み取り・書き込み場所の指定
                        DatabaseReference room = ReadWrite.ref.child(String.valueOf(editText.getText()));
                        DatabaseReference devices = room.child("devices");
                        //参加デバイス数の取得
                        devices.get().addOnCompleteListener(task1 -> {
                            String s1 = "device11";
                            //デバイス番号の割当（プログラム上で10台に制限中）
                            for(int i = 1; i < 12; i++){
                                if(String.valueOf(i).length() == 1){
                                    s1 = "device" + "0" + i;
                                }else{
                                    s1 = "device" + i;
                                }
                                //同一のデバイス番号がFirebase上に存在するかの確認
                                if(String.valueOf(Objects.requireNonNull(task1.getResult()).getValue()).contains(s1)) {
                                    s1 = "device11";
                                }else{
                                    break;
                                }
                            }
                            //接続デバイスが10台を超えていないかの判断
                            if(!s1.equals("device11")){
                                String s = String.valueOf(editText.getText());
                                //Firebaseにデバイス情報を送信
                                ReadWrite.SendDeviceInfo(s,s1,android.os.Build.MANUFACTURER,android.os.Build.MODEL);
                                //グローバル変数にルーム番号・デバイス番号を代入
                                MainActivity.roomNumber = editText.getText().toString();
                                MainActivity.deviceNumber = s1;
                                //ゲスト画面に遷移
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
        this.setCancelable(false);
        return builder.create();
    }
}