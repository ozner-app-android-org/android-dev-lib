package com.ozner.wifi.mxchip;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ozner.util.HttpUtil;
import com.ozner.util.dbg;
import com.ozner.wifi.mxchip.ftc_service.FTC_Listener;
import com.ozner.wifi.mxchip.ftc_service.FTC_Service;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by zhiyongxu on 15/10/27.
 */
public class WifiConfiguration implements FTC_Listener {
    /**
     * 默认1分钟配网超时
     */
    final static int ConfigurationTimeout = 60000;
    WifiConfigurationListener wifiConfigurationListener = null;
    boolean isConfigurationMode = false;
    FTC_Service ftc_service;
    Context context;
    WifiManager wifiManager;
    Handler handler;
    public WifiConfiguration(Context context) {
        this.context = context;
        handler = new Handler(Looper.getMainLooper());
        wifiManager = (WifiManager) context.getSystemService(Activity.WIFI_SERVICE);
        ftc_service = FTC_Service.getInstence();
    }

    @Override
    public void onFTCfinished(Socket s, String jsonString) {

        ConfigurationDevice device = ConfigurationDevice.loadByFTCJson(jsonString);
        if ((wifiConfigurationListener != null) && (device != null)) {
            wifiConfigurationListener.onWifiConfiguration(device);
            wifiConfigurationListener.onWifiConfigurationStop();
        }

        dbg.d(device.name);
    }

    @Override
    public void isSmallMTU(int i) {

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

    private String getToken() {
        return String.valueOf(System.currentTimeMillis());
    }

    public String ActiveDevice(ConfigurationDevice device) throws IOException {
        JSONObject jsonObject = new JSONObject();
        String url = "http://" + device.localIP + ":" + device.localPort + "/dev-activate";
        jsonObject.put("login_id", device.loginId);
        jsonObject.put("dev_passwd", device.devPasswd);
        jsonObject.put("user_token", getToken());
        String retString = HttpUtil.postJSON(url, jsonObject.toJSONString());
        JSONObject ret = (JSONObject) JSON.parse(retString);
        return ret.getString("device_id");
    }

    public String CloudReset(ConfigurationDevice device) throws IOException {
        JSONObject jsonObject = new JSONObject();
        String url = "http://" + device.localIP + ":" + device.localPort + "/dev-cloud_reset";
        jsonObject.put("login_id", device.loginId);
        jsonObject.put("dev_passwd", device.devPasswd);
        jsonObject.put("user_token", getToken());
        String retString = HttpUtil.postJSON(url, jsonObject.toJSONString());
        JSONObject ret = (JSONObject) JSON.parse(retString);
        return ret.getString("device_id");
    }

    /**
     * 配置WIFI设备回调接口
     */
    public interface WifiConfigurationListener {
        void onWifiConfiguration(ConfigurationDevice device);

        void onWifiConfigurationStart();

        void onWifiConfigurationStop();
    }


}
