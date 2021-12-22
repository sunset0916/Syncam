package com.example.syncam;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    boolean endFlag = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        endFlag = false;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        HostActivity.flag = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!HostActivity.flag){
            if(!endFlag) {
                ReadWrite.ref.child(MainActivity.rn).removeValue();
                MainActivity.rn = null;
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        ReadWrite.ref.get().addOnCompleteListener(task -> {
            if (!String.valueOf(Objects.requireNonNull(task.getResult()).getValue()).contains("roomNumber=" + MainActivity.rn)) {
                finish();
                MainActivity.rn = null;
                endFlag = true;
            }
        });
    }
}