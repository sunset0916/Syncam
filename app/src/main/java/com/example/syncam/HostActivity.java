package com.example.syncam;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HostActivity extends AppCompatActivity {
    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.setting, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_button:
                Intent intent = new Intent(HostActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hostactivity);

        TextView textView=(TextView)findViewById(R.id.tvNumber);
        textView.setText(MainActivity.rn);


    }

    @Override
    protected void onPause() {
        super.onPause();
        ReadWrite.ref.child(MainActivity.rn).removeValue();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        finish();
    }
}