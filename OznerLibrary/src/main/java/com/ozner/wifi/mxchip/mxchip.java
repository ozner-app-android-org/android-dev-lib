package com.ozner.wifi.mxchip;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;

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
public class mxchip implements FTC_Listener, JmdnsListener {
    Context context;
    WifiManager wifiManager;
    FTC_Service ftc_service;
    JmdnsAPI mdnsApi;
    boolean isConfigurationMode = false;
    boolean isSearchMode = false;
    /**
     * 默认1分钟配网超时
     */
    final static int ConfigurationTimeout = 60000;
    final static int SearchTimeout = 60000;


    WifiConfigurationListener wifiConfigurationListener = null;
    WifiSearchDeviceListener wifiSearchDeviceListener = null;
    Handler handler;

    /**
     * 配置WIFI设备回调接口
     */
    public interface WifiConfigurationListener {
        void onWifiConfiguration(WifiDevice device);

        void onWifiConfigurationStart();

        void onWifiConfigurationStop();
    }

    public interface WifiSearchDeviceListener {
        void onWifiSearchFound(WifiDevice device);

        void onWifiSearchStart();

        void onWifiSearchStop();
    }


    public mxchip(Context context) {
        this.context = context;
        wifiManager = (WifiManager) context.getSystemService(Activity.WIFI_SERVICE);
        ftc_service = FTC_Service.getInstence();
        mdnsApi = new JmdnsAPI(context);

        handler = new Handler();

    }

    public boolean startWifiSearch(WifiSearchDeviceListener listener) {
        if (isSearchMode) return false;
        if (isConfigurationMode) return false;
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

    public void stopWifiConfiguration() {
        if (!isConfigurationMode)
            return;
        ftc_service.stopTransmitting();
        if (wifiConfigurationListener != null) {
            wifiConfigurationListener.onWifiConfigurationStop();
        }
        isConfigurationMode = false;
    }

    public boolean startWifiConfiguration(String ssid, String password, WifiConfigurationListener listener) {
        if (isSearchMode) return false;
        if (isConfigurationMode) return false;
        isConfigurationMode = true;
        wifiConfigurationListener = listener;

        if (wifiConfigurationListener != null) {
            wifiConfigurationListener.onWifiConfigurationStart();
        }
        ftc_service.transmitSettings(context, ssid.trim(), password,
                wifiManager.getConnectionInfo().getIpAddress(),
                this);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopWifiConfiguration();
            }
        }, ConfigurationTimeout);
        return true;
    }


    @Override
    public void onFTCfinished(Socket s, String jsonString) {
        ftc_service.stopTransmitting();
        WifiDevice device = WifiDevice.loadByFTCJson(jsonString);
        if ((wifiConfigurationListener != null) && (device != null)) {
            wifiConfigurationListener.onWifiConfiguration(device);
        }
        dbg.d(device.name);
    }

    @Override
    public void isSmallMTU(int i) {

    }

    @Override
    public void onJmdnsFind(JSONArray jsonArray) {
        String json = jsonArray.toString();
        WifiDevice device = WifiDevice.loadByFTCJson(json);
        if ((wifiSearchDeviceListener != null) && (device != null)) {
            wifiSearchDeviceListener.onWifiSearchFound(device);
        }
        dbg.d(jsonArray.toString());
    }
}
