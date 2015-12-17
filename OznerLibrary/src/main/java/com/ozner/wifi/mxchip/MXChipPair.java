package com.ozner.wifi.mxchip;

import android.content.Context;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.alibaba.fastjson.JSON;
import com.mxchip.jmdns.JmdnsAPI;
import com.mxchip.jmdns.JmdnsListener;
import com.ozner.device.OznerDeviceManager;
import com.ozner.util.Helper;
import com.ozner.util.HttpUtil;
import com.ozner.wifi.mxchip.Pair.ConfigurationDevice;
import com.ozner.wifi.mxchip.Pair.EasyLinkSender;
import com.ozner.wifi.mxchip.Pair.FTC;
import com.ozner.wifi.mxchip.Pair.FTC_Listener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

/**
 * Created by xzyxd on 2015/11/1.
 * 庆科配网工具类
 */
public class MXChipPair {
    /**
     * 默认1分钟配网超时
     */
    final static int ConfigurationTimeout = 120000;
    final static int MDNSTimeout = 60000;
    static final MXChipPairImp mxChipPairImp = new MXChipPairImp();
    static Context context;
    static MXChipPairCallback callback;

    static Thread runThread = null;
    static String SSID = "";
    static String password = "";
    static String deviceMAC = "";


    private MXChipPair() {
    }

    /**
     * 开始庆科配网
     *
     * @param SSID     ssid
     * @param password 密码
     * @param callback 配网回调
     */
    public static void Pair(Context context, String SSID, String password, MXChipPairCallback callback)
            throws InstantiationException, NullSSIDException {
        MXChipPair.context = context;
        if (runThread != null) throw new InstantiationException();
        if (Helper.StringIsNullOrEmpty(SSID)) throw new NullSSIDException();
        MXChipPair.SSID = SSID;
        MXChipPair.password = password;
        MXChipPair.callback = callback;
        runThread = new Thread(mxChipPairImp);
        runThread.start();
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
         * @param io 配对好的设备IO接口
         */
        void onPairComplete(MXChipIO io);

        /**
         * 配网失败
         *
         * @param e 失败的异常
         */
        void onPairFailure(Exception e);
    }

    /**
     * 无线设备没打开或无连接
     */
    public static class WifiException extends Exception {
    }

    public static class NullSSIDException extends Exception {
    }

    public static class TimeoutException extends Exception {
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
        public void onFTCfinished(String jsonString) {
            device = ConfigurationDevice.loadByFTCJson(jsonString);
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

        private String Authorize() throws IOException {
            com.alibaba.fastjson.JSONObject jsonObject = new com.alibaba.fastjson.JSONObject();
            String url = "http://" + device.localIP + ":" + device.localPort + "/dev-authorize";
            jsonObject.put("login_id", device.loginId);
            jsonObject.put("dev_passwd", device.devPasswd);
            jsonObject.put("user_token", getToken());
            String retString = HttpUtil.postJSON(url, jsonObject.toJSONString(), "US-ASCII");
            com.alibaba.fastjson.JSONObject ret = (com.alibaba.fastjson.JSONObject) JSON.parse(retString);
            return ret.getString("device_id");
        }

        @Override
        public void run() {
            try {

                JmdnsAPI mdnsApi = new JmdnsAPI(context);
                device = null;
                deviceMAC = null;
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wifiManager.getConnectionInfo();

                if (info.getSupplicantState() != SupplicantState.COMPLETED) {
                    callback.onPairFailure(new WifiException());
                    return;
                }

                callback.onSendConfiguration();


//                FTC_Service ftc_service = FTC_Service.getInstence();
//                try {
//                    //ftc_service.transmitSettings();
//                    ftc_service.transmitSettings(context, SSID.trim(), password, info.getIpAddress(),
//                            mxChipPairImp);
//                    wait(ConfigurationTimeout);
//                } finally {
//                    ftc_service.stopTransmitting();
//                }

                FTC ftc = new FTC(context, this);
                try {
                    ftc.startListen();
                    EasyLinkSender sender = new EasyLinkSender();
                    sender.setSettings(SSID.trim(), password, info.getIpAddress());
                    Date t = new Date();

                    while (device == null) {
                        sender.send_easylink_v3();
                        Thread.sleep(100);

                        sender.send_easylink_v2();
                        Thread.sleep(100);
                        Date now = new Date();
                        if ((now.getTime() - t.getTime()) > ConfigurationTimeout) {
                            break;
                        }
                    }
                    sender.close();

                } finally {
                    ftc.stop();
                }
                if (device == null) {
                    callback.onPairFailure(new TimeoutException());
                    return;
                }
                if ((device.activated) && (!Helper.StringIsNullOrEmpty(device.activeDeviceID))) {
                    int p = device.activeDeviceID.indexOf("/");
                    if (p > 0) {
                        String tmp = device.activeDeviceID.substring(p + 1).toUpperCase();
                        if (tmp.length() == 12) {
                            String mac = tmp.substring(0, 2) + ":" +
                                    tmp.substring(2, 4) + ":" +
                                    tmp.substring(4, 6) + ":" +
                                    tmp.substring(6, 8) + ":" +
                                    tmp.substring(8, 10) + ":" +
                                    tmp.substring(10, 12);
                            MXChipIO io = OznerDeviceManager.Instance().ioManagerList().mxChipIOManager().
                                    createMXChipDevice(mac, device.Type);
                            if (io != null) {
                                callback.onPairComplete(io);
                            } else
                                callback.onPairFailure(null);
                            return;
                        }
                    }
                }
                callback.onWaitConnectWifi();

                //Thread.sleep(2000);
                mdnsApi.startMdnsService("_easylink._tcp.local.", this);
                wait(MDNSTimeout);
                mdnsApi.stopMdnsService();

                if (Helper.StringIsNullOrEmpty(deviceMAC)) {
                    callback.onPairFailure(new TimeoutException());
                    return;
                }
                callback.onActivate();

                String deviceId = ActiveDevice();
                if (Helper.StringIsNullOrEmpty(deviceId)) {
                    callback.onPairFailure(null);
                }
                //Authorize();

                if (Helper.StringIsNullOrEmpty(deviceId)) {
                    callback.onPairFailure(null);
                }

                MXChipIO io = OznerDeviceManager.Instance().ioManagerList().mxChipIOManager().
                        createMXChipDevice(deviceMAC, device.Type);
                io.name=device.name;
                if (io != null) {
                    callback.onPairComplete(io);
                } else
                    callback.onPairFailure(null);
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
                    String name = object.getString("deviceName");
                    int p = name.indexOf("#");
                    if (p > 0) {
                        name = name.substring(p + 1);
                        if (device.name.indexOf(name) > 0) {
                            deviceMAC = object.getString("deviceMac");
                            device.localIP = object.getString("deviceIP");
                            set();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
    }

}
