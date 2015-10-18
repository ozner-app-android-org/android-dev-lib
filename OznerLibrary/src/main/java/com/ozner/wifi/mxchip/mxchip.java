package com.ozner.wifi.mxchip;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;

import com.mxchip.jmdns.JmdnsAPI;
import com.ozner.util.dbg;
import com.ozner.wifi.mxchip.ftc_service.FTC_Listener;
import com.ozner.wifi.mxchip.ftc_service.FTC_Service;

import java.net.Socket;

/**
 * Created by zhiyongxu on 15/10/15.
 */
public class mxchip implements FTC_Listener {
    Context context;
    WifiManager wifiManager;
    FTC_Service ftc_service;
    JmdnsAPI mdnsApi;

    public mxchip(Context context) {
        this.context = context;
        wifiManager = (WifiManager) context.getSystemService(Activity.WIFI_SERVICE);
        ftc_service = FTC_Service.getInstence();
        mdnsApi = new JmdnsAPI(context);
    }

    public void start(String ssid, String password) {
        ftc_service.transmitSettings(context, ssid.trim(), password,
                wifiManager.getConnectionInfo().getIpAddress(),
                this);
    }


    @Override
    public void onFTCfinished(Socket s, String jsonString) {
        ftc_service.stopTransmitting();
        String text = "FTCEnd" + s + " " + jsonString;
        dbg.d(text);
        //elapi.stopEasyLink();
        //Toast.makeText(context,text
        //       , Toast.LENGTH_LONG).show();


    }

    @Override
    public void isSmallMTU(int i) {

    }
}
