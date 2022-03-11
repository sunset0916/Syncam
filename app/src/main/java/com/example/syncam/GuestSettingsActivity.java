package com.example.syncam;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class GuestSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        //設定画面の準備
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
    }

    //設定画面の項目の読み取り・反映
    public static class SettingsFragment extends PreferenceFragmentCompat {

        //GuestSettingActivityのインスタンス化
        public GuestSettingsActivity activity;

        @Override
        public void onAttach(@NonNull Context activity) {
            super.onAttach(activity);
            this.activity = (GuestSettingsActivity) activity;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.guest_preferences, rootKey);

            //EditTextPreferenceのEditText部分を取り出す
            EditTextPreference editTextPreference = findPreference("Syncam-Setting-CameraLag");
            editTextPreference.setOnBindEditTextListener(editText -> {
                //入力できる文字を数字に限定
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                //入力可能文字数を3文字に制限
                editText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(3)});
                //001等が入力されていた際に正しい数値へ変更
                String cameraLag;
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
                if(sp.getString("Syncam-Setting-CameraLag", "0").equals("")){
                    cameraLag = "0";
                }else{
                    cameraLag = sp.getString("Syncam-Setting-CameraLag", "0");
                }
                editText.setText(String.valueOf(Integer.parseInt(cameraLag)));
            });

            //「詳細設定マニュアル」ボタンを押されたときの動作
            Preference manualPreference = findPreference("Syncam-Manual");
            manualPreference.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(activity,ManualActivity.class);
                activity.startActivity(intent);
                return true;
            });

            //「保存場所について」ボタンを押されたときの動作
            Preference storageLocationPreference = findPreference("Syncam-Storage-Location");
            storageLocationPreference.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(activity,StorageLocationActivity.class);
                activity.startActivity(intent);
                return true;
            });
        }
    }
}