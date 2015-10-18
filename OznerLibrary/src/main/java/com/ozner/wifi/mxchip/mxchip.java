package com.ozner.wifi.mxchip;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;

import com.mxchip.jmdns.JmdnsAPI;
import com.mxchip.jmdns.JmdnsListener;
import com.ozner.util.dbg;
import com.ozner.wifi.mxchip.ftc_service.FTC_Listener;
import com.ozner.wifi.mxchip.ftc_service.FTC_Service;

import org.json.JSONArray;

import java.net.Socket;

/**
 * Created by zhiyongxu on 15/10/15.
 */
public class mxchip implements FTC_Listener,JmdnsListener {
    Context context;
    WifiManager wifiManager;
    FTC_Service ftc_service;
    JmdnsAPI mdnsApi;
    public interface onBindWifiDevice
    {
        void onBindWifiDevice(WifiDevice device);
    }

    public interface SearchWifiDevice
    {
        void onFoundDevice(WifiDevice device);
    }


    public mxchip(Context context) {
        this.context = context;
        wifiManager = (WifiManager) context.getSystemService(Activity.WIFI_SERVICE);
        ftc_service = FTC_Service.getInstence();
        mdnsApi = new JmdnsAPI(context);
        mdnsApi.startMdnsService("_easylink._tcp.local.",this);
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
    }

    @Override
    public void isSmallMTU(int i) {

    }

    @Override
    public void onJmdnsFind(JSONArray jsonArray) {

        dbg.d(jsonArray.toString());

    }
}
