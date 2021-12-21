package com.example.syncam;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NTPTask extends AsyncTask<Integer,Integer,String> {
    @Override
    protected String doInBackground(Integer... params) {
        final String NTP_SERVER = "ntp.nict.jp";
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formater = new SimpleDateFormat("HH:mm:ss.SSS");
        NTPUDPClient client = new NTPUDPClient();
        String result="";
        try {
            client.open();
            InetAddress host = InetAddress.getByName(NTP_SERVER);
            TimeInfo info = client.getTime(host);

            info.computeDetails();
            Date exactTime;

            exactTime = new Date(System.currentTimeMillis() + info.getOffset());
            Log.d("Time", String.valueOf(exactTime));
            result=formater.format(exactTime);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.close();
        }
        return result;
    }

}
