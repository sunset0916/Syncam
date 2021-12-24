package com.example.syncam;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NTPTask extends AsyncTask<Integer, Integer, String> {
    @Override
    protected String doInBackground(Integer... params) {
        //NTPサーバ名の指定
        final String NTP_SERVER = "ntp.nict.jp";

        //フォーマットの指定
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formater = new SimpleDateFormat("HH:mm:ss.SSS");

        // プロトコルの呼び出し
        NTPUDPClient client = new NTPUDPClient();
        String result = "";
        try {
            //クライアントの呼び出しおよびNTPサーバへの接続
            client.open();
            InetAddress host = InetAddress.getByName(NTP_SERVER);

            //NTPサーバから時間の取得
            TimeInfo info = client.getTime(host);
            info.computeDetails();

            //Date型の宣言
            Date exactTime;

            //処理時間の計算、正確な時間の取得
            exactTime = new Date(System.currentTimeMillis() + info.getOffset());

            //取得した時間を指定したフォーマットに変換
            result = formater.format(exactTime);

        } catch (Exception e) {

            //スタックトレースの出力
            e.printStackTrace();
        } finally {

            //クライアントの終了
            client.close();
        }
        //フォーマットに変換した時間を返す
        return result;
    }
}