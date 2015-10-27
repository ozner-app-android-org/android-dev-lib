package com.ozner.wifi.mxchip;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.mxchip.jmdns.JmdnsAPI;
import com.mxchip.jmdns.JmdnsListener;
import com.ozner.util.dbg;

import org.json.JSONArray;

/**
 * Created by zhiyongxu on 15/10/27.
 */
public class WifiSearch implements JmdnsListener {
    public interface WifiSearchDeviceListener {
        void onWifiSearchFound(ConfigurationDevice device);

        void onWifiSearchStart();

        void onWifiSearchStop();
    }

    final static int SearchTimeout = 60000;
    JmdnsAPI mdnsApi;
    boolean isSearchMode = false;
    WifiSearchDeviceListener wifiSearchDeviceListener = null;
    Handler handler;
    Context context;

    public WifiSearch(Context context) {
        this.context = context;
        handler = new Handler(Looper.getMainLooper());
    }


    public boolean startWifiSearch(WifiSearchDeviceListener listener) {
        if (isSearchMode) return false;
        isSearchMode = true;
        wifiSearchDeviceListener = listener;
        if (wifiSearchDeviceListener != null)
            wifiSearchDeviceListener.onWifiSearchStart();

        mdnsApi.startMdnsService("_easylink._tcp.local.", this);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopWifiSearch();
            }
        }, SearchTimeout);
        return true;
    }

    public void stopWifiSearch() {
        if (!isSearchMode) return;
        mdnsApi.stopMdnsService();
        isSearchMode = false;
        if (wifiSearchDeviceListener != null) {
            wifiSearchDeviceListener.onWifiSearchStop();
        }
    }

    @Override
    public void onJmdnsFind(JSONArray jsonArray) {
        String json = jsonArray.toString();
        ConfigurationDevice device = ConfigurationDevice.loadByFTCJson(json);
        if ((wifiSearchDeviceListener != null) && (device != null)) {
            wifiSearchDeviceListener.onWifiSearchFound(device);
        }
        dbg.d(jsonArray.toString());
    }
}
