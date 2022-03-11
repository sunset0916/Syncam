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
            photoDirectory = "/DCIM/SYNCAM";
            movieDirectory = "/DCIM/SYNCAM";
            voiceDirectory = "/MUSIC/AUDIO";
        }else{
            photoDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath();
            movieDirectory = getExternalFilesDir(Environment.DIRECTORY_MOVIES).getPath();
            voiceDirectory = getExternalFilesDir(Environment.DIRECTORY_MUSIC).getPath();
        }
        tvPhotoDirectory.setText(photoDirectory.replace(Environment.getExternalStorageDirectory().getPath(),""));
        tvMovieDirectory.setText(movieDirectory.replace(Environment.getExternalStorageDirectory().getPath(),""));
        tvVoiceDirectory.setText(voiceDirectory.replace(Environment.getExternalStorageDirectory().getPath(),""));
    }
}