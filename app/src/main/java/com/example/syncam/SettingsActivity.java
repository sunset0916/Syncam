package com.example.syncam;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    //画面遷移の状態を格納する変数
    boolean endFlag = false;

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

        //画面遷移の状態を格納する変数の初期化
        endFlag = false;
    }

    //設定画面の項目の読み取り・反映
    public static class SettingsFragment extends PreferenceFragmentCompat {
        //SettingsActivityのインスタンス化
        SettingsActivity activity;
        @Override
        public void onAttach(@NonNull Context activity) {
            super.onAttach(activity);
            this.activity = (SettingsActivity) activity;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            //今すぐ撮影モードの状態の確認
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
            //今すぐ撮影モードが有効な場合、タイマーと解像度の設定を無効化
            Preference timer = findPreference("Syncam-Setting-timer");
            Preference resolution = findPreference("Syncam-Setting-resolution");
            if(sp.getBoolean("Syncam-Setting-quickShot",false)){
                timer.setEnabled(false);
                resolution.setEnabled(false);
            }
            //今すぐ撮影モードのCheckBoxがタップされたときタイマーと解像度の設定を有効/無効にする
            Preference quickShot = findPreference("Syncam-Setting-quickShot");
            quickShot.setOnPreferenceClickListener(preference -> {
                if(sp.getBoolean("Syncam-Setting-quickShot",false)){
                    timer.setEnabled(false);
                    resolution.setEnabled(false);
                }else{
                    timer.setEnabled(true);
                    resolution.setEnabled(true);
                }
                return true;
            });
        }
    }

    //◁ボタンを押されたときの動作
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        HostActivity.flag = true;
    }

    //画面が停止されたときの動作
    @Override
    protected void onStop() {
        super.onStop();
        if (!HostActivity.flag) {
            if (!endFlag) {
                //◁ボタン以外押下時とonRestartからのfinish()以外のonStop()時にFirebaseからルームを削除する動作
                ReadWrite.ref.child(MainActivity.rn).removeValue();
                MainActivity.rn = null;
            }
        }
    }

    //画面再開時の動作
    @Override
    protected void onRestart() {
        super.onRestart();
        ReadWrite.ref.get().addOnCompleteListener(task -> {
            //Firebase上からルーム番号が削除されているときの動作
            if (!String.valueOf(Objects.requireNonNull(task.getResult()).getValue()).contains("roomNumber=" + MainActivity.rn)) {
                finish();
                MainActivity.rn = null;
                endFlag = true;
            }
        });
    }
}