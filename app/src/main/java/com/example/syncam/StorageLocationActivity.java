package com.example.syncam;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;

public class StorageLocationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_location);
        //APIレベル、Androidバージョン、保存場所を取得して表示
        TextView tvAPILevel = findViewById(R.id.tvAPILevel);
        TextView tvAndroidVersion = findViewById(R.id.tvAndroidVersion);
        TextView tvPhotoDirectory = findViewById(R.id.tvPhotoDirectory);
        TextView tvMovieDirectory = findViewById(R.id.tvMovieDirectory);
        TextView tvVoiceDirectory = findViewById(R.id.tvVoiceDirectory);
        String photoDirectory,movieDirectory,voiceDirectory;
        tvAPILevel.setText(String.valueOf(Build.VERSION.SDK_INT));
        tvAndroidVersion.setText(String.valueOf(Build.VERSION.RELEASE));
        if(Build.VERSION.SDK_INT < 30){
            photoDirectory = Environment.getExternalStorageDirectory().getPath() + "/DCIM/SYNCAM";
            movieDirectory = Environment.getExternalStorageDirectory().getPath() + "/DCIM/SYNCAM";
            voiceDirectory = Environment.getExternalStorageDirectory().getPath() + "/MUSIC/AUDIO";
        }else{
            photoDirectory = String.valueOf(getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            movieDirectory = String.valueOf(getExternalFilesDir(Environment.DIRECTORY_MOVIES));
            voiceDirectory = String.valueOf(getExternalFilesDir(Environment.DIRECTORY_MUSIC));
        }
        tvPhotoDirectory.setText(photoDirectory.replace("/storage/emulated/0/",""));
        tvMovieDirectory.setText(movieDirectory.replace("/storage/emulated/0/",""));
        tvVoiceDirectory.setText(voiceDirectory.replace("/storage/emulated/0/",""));
    }
}