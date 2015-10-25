package com.ozner.wifi.mxchip;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mxchip.jmdns.JmdnsAPI;
import com.mxchip.jmdns.JmdnsListener;
import com.ozner.util.Helper;
import com.ozner.util.HttpUtil;
import com.ozner.util.dbg;
import com.ozner.wifi.mxchip.ftc_service.FTC_Listener;
import com.ozner.wifi.mxchip.ftc_service.FTC_Service;

import org.json.JSONArray;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by zhiyongxu on 15/10/15.
 */
public class MXWifiChip implements FTC_Listener, JmdnsListener {
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


    public MXWifiChip(Context context) {
        this.context = context;
        wifiManager = (WifiManager) context.getSystemService(Activity.WIFI_SERVICE);
        ftc_service = FTC_Service.getInstence();
        mdnsApi = new JmdnsAPI(context);
        handler = new Handler(Looper.getMainLooper());
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
        isConfigurationMode = false;

        if (wifiConfigurationListener != null) {
            wifiConfigurationListener.onWifiConfigurationStop();
        }

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

        WifiDevice device = WifiDevice.loadByFTCJson(jsonString);
        if ((wifiConfigurationListener != null) && (device != null)) {
            wifiConfigurationListener.onWifiConfiguration(device);
            wifiConfigurationListener.onWifiConfigurationStop();
        }

        dbg.d(device.name);
    }

    @Override
    public void isSmallMTU(int i) {

    }
    private String getToken()
    {
        return String.valueOf(System.currentTimeMillis());
    }

    public String ActiveDevice(WifiDevice device) throws IOException {
        JSONObject jsonObject=new JSONObject();
        String url="http://"+device.localIP+":"+device.localPort+"/dev-activate";
        jsonObject.put("login_id",device.loginId);
        jsonObject.put("dev_passwd", device.devPasswd);
        jsonObject.put("user_token", getToken());
        String retString=HttpUtil.postJSON(url, jsonObject.toJSONString());
        JSONObject ret=(JSONObject)JSON.parse(retString);
        return ret.getString("device_id");
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
