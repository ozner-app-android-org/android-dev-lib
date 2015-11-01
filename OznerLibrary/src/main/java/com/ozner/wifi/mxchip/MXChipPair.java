package com.ozner.wifi.mxchip;

import android.content.Context;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.alibaba.fastjson.JSON;
import com.mxchip.jmdns.JmdnsAPI;
import com.mxchip.jmdns.JmdnsListener;
import com.ozner.device.OperateCallback;
import com.ozner.util.Helper;
import com.ozner.util.HttpUtil;
import com.ozner.wifi.mxchip.ftc_service.FTC_Listener;
import com.ozner.wifi.mxchip.ftc_service.FTC_Service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by xzyxd on 2015/11/1.
 * 庆科配网工具类
 */
public class MXChipPair {
    /**
     * 默认1分钟配网超时
     */
    final static int ConfigurationTimeout = 60000;
    final static int MDNSTimeout = 60000;
    static Context context;
    static final MXChipPairImp mxChipPairImp = new MXChipPairImp();
    static MXChipPairCallback callback;

    static Thread runThread = null;
    static String SSID = "";
    static String password = "";
    static String deviceMAC = "";

    /**
     * 无线设备没打开或无连接
     */
    public static class WifiException extends Exception {
    }

    public static class NullSSIDException extends Exception {
    }

    public static class TimeoutException extends Exception {
    }

    public interface MXChipPairCallback {
        /**
         * 开始发送WIFI设置信息
         */
        void onSendConfiguration();

        /**
         * 等待设备连接WIFI
         */
        void onWaitConnectWifi();

        /**
         * 等待设备激活
         */
        void onActivate();

        /**
         * 配网完成
         *
         * @param DeviceId 收到的设备ID
         */
        void onPairComplete(String DeviceId);

        /**
         * 配网失败
         *
         * @param e 失败的异常
         */
        void onPairFailure(Exception e);
    }


    static class MXChipPairImp implements FTC_Listener, Runnable, JmdnsListener {

        final Object waitObject = new Object();
        ConfigurationDevice device = null;

        private void wait(int time) throws InterruptedException {
            synchronized (waitObject) {
                waitObject.wait(time);
            }
        }

        private void set() {
            synchronized (waitObject) {
                waitObject.notify();
            }
        }

        @Override
        public void onFTCfinished(Socket s, String jsonString) {
            device = ConfigurationDevice.loadByFTCJson(jsonString);
            callback.onWaitConnectWifi();
            set();
        }

        @Override
        public void isSmallMTU(int MTU) {

        }

        private String getToken() {
            return String.valueOf(System.currentTimeMillis());
        }

        private String ActiveDevice() throws IOException {

            com.alibaba.fastjson.JSONObject jsonObject = new com.alibaba.fastjson.JSONObject();
            String url = "http://" + device.localIP + ":" + device.localPort + "/dev-activate";
            jsonObject.put("login_id", device.loginId);
            jsonObject.put("dev_passwd", device.devPasswd);
            jsonObject.put("user_token", getToken());
            String retString = HttpUtil.postJSON(url, jsonObject.toJSONString(), "US-ASCII");
            com.alibaba.fastjson.JSONObject ret = (com.alibaba.fastjson.JSONObject) JSON.parse(retString);
            return ret.getString("device_id");
        }

//        private String CloudReset(ConfigurationDevice device) throws IOException {
//            com.alibaba.fastjson.JSONObject jsonObject = new com.alibaba.fastjson.JSONObject();
//            String url = "http://" + device.localIP + ":" + device.localPort + "/dev-cloud_reset";
//            jsonObject.put("login_id", device.loginId);
//            jsonObject.put("dev_passwd", device.devPasswd);
//            jsonObject.put("user_token", getToken());
//            String retString = HttpUtil.postJSON(url, jsonObject.toJSONString(),"US-ASCII");
//            com.alibaba.fastjson.JSONObject ret = (com.alibaba.fastjson.JSONObject) JSON.parse(retString);
//            return ret.getString("device_id");
//        }

        @Override
        public void run() {
            try {
                FTC_Service ftc_service = FTC_Service.getInstence();
                JmdnsAPI mdnsApi = new JmdnsAPI(context);
                device = null;
                deviceMAC = null;
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wifiManager.getConnectionInfo();

                if (info.getSupplicantState() != SupplicantState.COMPLETED) {
                    callback.onPairFailure(new WifiException());
                }
                callback.onSendConfiguration();
                ftc_service.transmitSettings(context, SSID.trim(), password, info.getIpAddress(),
                        mxChipPairImp);
                wait(ConfigurationTimeout);
                ftc_service.stopTransmitting();
                if (device == null) {
                    callback.onPairFailure(new TimeoutException());
                }
                callback.onWaitConnectWifi();

                mdnsApi.startMdnsService("_easylink._tcp.local.", this);
                wait(MDNSTimeout);
                mdnsApi.stopMdnsService();
                if (Helper.StringIsNullOrEmpty(deviceMAC)) {
                    callback.onPairFailure(new TimeoutException());
                }
                callback.onActivate();

                String deviceId = ActiveDevice();
                if (Helper.StringIsNullOrEmpty(deviceId)) {
                    callback.onPairFailure(null);
                }
                callback.onPairComplete(deviceId);
            } catch (Exception e) {
                callback.onPairFailure(e);
            } finally {
                runThread = null;
            }
        }

        @Override
        public void onJmdnsFind(JSONArray jsonArray) {
            if (device == null) return;
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject object = jsonArray.getJSONObject(i);
                    if (object.getString("deviceIP").equals(device.localIP)) {
                        deviceMAC = object.getString("deviceMac");
                        set();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private MXChipPair() {

    }


    /**
     * 开始庆科配网
     *
     * @param SSID     ssid
     * @param password 密码
     * @param callback 配网回调
     */
    public static void Pair(Context context, String SSID, String password, OperateCallback<String> callback)
            throws InstantiationException, NullSSIDException {
        MXChipPair.context = context;
        if (runThread != null) throw new InstantiationException();
        if (Helper.StringIsNullOrEmpty(SSID)) throw new NullSSIDException();
        MXChipPair.SSID = SSID;
        MXChipPair.password = password;
        runThread = new Thread(mxChipPairImp);
        runThread.start();
    }

}
