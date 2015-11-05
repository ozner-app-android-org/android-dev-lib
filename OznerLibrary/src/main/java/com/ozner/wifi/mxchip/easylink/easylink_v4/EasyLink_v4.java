package com.ozner.wifi.mxchip.easylink.easylink_v4;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.ozner.wifi.mxchip.easylink.easylink_plus.EasyLink_plus;
import com.ozner.wifi.mxchip.easylink.ftc_service.FTC_Listener;
import com.ozner.wifi.mxchip.easylink.ftc_service.ServiceThread;
import com.ozner.wifi.mxchip.easylink.ftc_service.SoftAP_Listener;
import com.ozner.wifi.mxchip.easylink.helper.Helper;
import com.ozner.wifi.mxchip.easylink.helper.WifiAutoConnectManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class EasyLink_v4 {
    private static final int REQUEST_TIMEOUT = 5 * 1000;
    private static final int SO_TIMEOUT = 20 * 1000;
    private static boolean listening;
    private static ServerSocket server = null;
    private static ServiceThread service;
    private static EasyLink_v4 e4 = null;
    private String TAG = "====EasyLink_v4====";
    private Context context;
    private Thread listen;
    private EasyLink_plus easylink_plus;

    // private byte realSSID[] = new byte[65];
    // private byte realKey[] = new byte[65];
    //
    // private byte virturalSSID[] = new byte[65];
    // private byte virturalKey[] = new byte[65];
    private WifiManager wifiManager = null;
    private WifiAutoConnectManager wifiConnect = null;
    private String realSSID = "";
    private String realKey = "";
    private String virturalSSID = "";
    private String virturalKey = "88888888";
    // private boolean isSending = false;
    private Timer conTimer;
    private TimerTask conTimerTask;

    private EasyLink_v4() {
        listening = true;
    }

    public static EasyLink_v4 getInstence() {
        if (e4 == null) {
            e4 = new EasyLink_v4();
        }
        return e4;
    }

    public void transmitSettings(Context contxt, byte[] Ssid, byte[] Key,
                                 String strIP) {
        context = contxt;
        wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        wifiConnect = new WifiAutoConnectManager(wifiManager);
        // realSSID = Ssid;
        // realKey = Key;
        // int ipAddress = phone_ip;
        try {
            realSSID = new String(Ssid, "UTF-8");
            realKey = new String(Key, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        byte[] byteVirSSID = new byte[32];
        byteVirSSID[0] = 0x23; // #
        Log.e(TAG, "strIP:" + strIP);
        System.arraycopy(Helper.hexStringToBytes(strIP), 0, byteVirSSID, 2, 4);

        String tmpSsidAndKey = realSSID + "@" + realKey;
        Log.e(TAG, "tmpSsidAndKey:" + tmpSsidAndKey);
        byte[] byteTmpSsidAndKey = tmpSsidAndKey.getBytes();
        Log.e(TAG, "byteTmpSsidAndKey:" + byteTmpSsidAndKey);
        System.arraycopy(byteTmpSsidAndKey, 0, byteVirSSID, 6,
                byteTmpSsidAndKey.length);
        int flag = 0;
        // IP[3]>=128
        if (byteVirSSID[5] < 0) {
            flag = flag | 0x08;
            int tmpIP = byteVirSSID[5];
            tmpIP &= 0x7f;
            byteVirSSID[5] = (byte) tmpIP;
            Log.e(TAG, "byteVirSSID[5]:" + byteVirSSID[5]);
        }
        // IP[2]>=128
        if (byteVirSSID[4] < 0) {
            flag = flag | 0x04;
            int tmpIP = byteVirSSID[4];
            tmpIP &= 0x7f;
            byteVirSSID[4] = (byte) tmpIP;
            Log.e(TAG, "byteVirSSID[4]:" + byteVirSSID[4]);
        }
        // IP[1]>=128
        if (byteVirSSID[3] < 0) {
            flag = flag | 0x02;
            int tmpIP = byteVirSSID[3];
            tmpIP &= 0x7f;
            byteVirSSID[3] = (byte) tmpIP;
            Log.e(TAG, "byteVirSSID[3]:" + byteVirSSID[3]);
        }
        // IP[0]>=128
        if (byteVirSSID[2] < 0) {
            flag = flag | 0x01;
            int tmpIP = byteVirSSID[2];
            tmpIP &= 0x7f;
            byteVirSSID[2] = (byte) tmpIP;
            Log.e(TAG, "byteVirSSID[5]:" + byteVirSSID[2]);
        }
        // IP[2]=0
        if (byteVirSSID[4] == 0) {
            flag = flag | 0x40;
            int tmpIP = 1;
            byteVirSSID[4] = (byte) tmpIP;
            Log.e(TAG, "byteVirSSID[4]:" + byteVirSSID[4]);
        }
        // IP[1]=0
        if (byteVirSSID[3] == 0) {
            flag = flag | 0x20;
            int tmpIP = 1;
            byteVirSSID[3] = (byte) tmpIP;
            Log.e(TAG, "byteVirSSID[3]:" + byteVirSSID[3]);
        }
        // IP[0]=0
        if (byteVirSSID[2] == 0) {
            flag = flag | 0x10;
            int tmpIP = 1;
            byteVirSSID[2] = (byte) tmpIP;
            Log.e(TAG, "byteVirSSID[2]:" + byteVirSSID[2]);
        }
        if (flag == 0) {
            flag = 0x3f;
        }
        byteVirSSID[1] = (byte) flag;
        try {
            virturalSSID = (new String(byteVirSSID)).trim();
        } catch (Exception e) {
            Log.e(TAG, "e:" + e);
        }
        Log.e(TAG, "virturalSSID:" + virturalSSID);
        startConnect();
    }

    private Timer getTimerIntence() {
        if (conTimer == null) {
            conTimer = new Timer();
        }
        return conTimer;
    }

    private TimerTask getTimerTaskIntence() {
        if (conTimerTask == null) {
            conTimerTask = new TimerTask() {
                @Override
                public void run() {
                    Log.e(TAG, "connecting2");
                    Log.e(TAG, "virturalSSID:" + virturalSSID + " virturalKey:"
                            + virturalKey);
                    // boolean conResult = wifiConnect.Connect(virturalSSID,
                    // virturalKey, WifiCipherType.WIFICIPHER_WPA);
                    wifiConnect.connect(virturalSSID, virturalKey,
                            WifiAutoConnectManager.WifiCipherType.WIFICIPHER_WPA);
                    boolean reConResult = wifiManager.reconnect();
                    Log.e(TAG, "connecting2 end");
                }
            };
        }
        return conTimerTask;
    }

    private void startConnect() {
        getTimerIntence().schedule(getTimerTaskIntence(), 10, 1000 * 2);
    }

    private void stopConnect() {
        if (conTimerTask != null) {
            conTimerTask.cancel();
            conTimerTask = null;
        }
        if (conTimer != null) {
            conTimer.cancel();
            conTimer.purge();
            conTimer = null;
        }
    }

    public void stopTransmitting() {
        stopConnectWifi();
        listening = false;
        try {
            if (null != server) {
                server.close();
                server = null;
            }
            // EasyLink_plus = EasyLink_plus.getInstence();
            // EasyLink_plus.stopTransmitting();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopConnectWifi() {
        stopConnect();
        Log.e("------------stop------------", "realSSID = " + realSSID
                + " realKey = " + realKey);
        // boolean conResult = wifiConnect.Connect(realSSID, realKey,
        // WifiCipherType.WIFICIPHER_WPA);
        wifiConnect.connect(realSSID, realKey, WifiAutoConnectManager.WifiCipherType.WIFICIPHER_WPA);
        boolean reConResult = wifiManager.reconnect();
    }

    public void transmitSettings_softap(final String Ssid, final String Key,
                                        final SoftAP_Listener listener) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    HttpPostData(Ssid, Key, listener);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public HttpClient getHttpClient() {
        BasicHttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, REQUEST_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, SO_TIMEOUT);
        HttpClient client = new DefaultHttpClient(httpParams);
        return client;
    }

    private void HttpPostData(String Ssid, String Key, SoftAP_Listener listener) {
        String configString = null;
        String IPAddress = "10.10.10.1";
        String configRequestPort = "8000";
        String configRequestMethod = "/config-write";
        try {
            Log.e("HttpPostData", "HttpPostData...");
            configString = "{\"SSID\": \"" + Ssid + "\", " + "\"PASSWORD\": \""
                    + Key + "\"}";
            HttpClient httpclient = getHttpClient();
            String urlString = "http://" + IPAddress + ":" + configRequestPort
                    + configRequestMethod;
            HttpPost httppost = new HttpPost(urlString);
            httppost.setEntity(new StringEntity(configString));
            HttpResponse response;
            response = httpclient.execute(httppost);
            int respCode = response.getStatusLine().getStatusCode();
            if (respCode == HttpURLConnection.HTTP_OK) {
                listener.onSoftAPconfigOK(respCode);
            } else {
                listener.onSoftAPconfigFail(respCode);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent()));

            String lineStr = "";
            while ((lineStr = in.readLine()) != null) {
                if (lineStr.matches("DeviceRegisterOK")) {
                    listener.onDeviceRegisterOK();
                } else if (lineStr.matches("DeviceRegisterFail")) {
                    listener.onDeviceRegisterFail();
                } else if (lineStr.matches("APConnectOK")) {
                    listener.onAPConnectOK();
                } else if (lineStr.matches("APConnectFail")) {
                    listener.onAPConnectFail();
                } else if (lineStr.matches("BindFail")) {
                    listener.onBindFail();
                } else if (lineStr.contains("uuid")) {
                    listener.onBindOK(lineStr);
                } else {
                    httpclient.getConnectionManager().shutdown();
                    in.close();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            listener.onSoftAPconfigFail(600);
        }
    }

    public static class MyService implements Runnable {
        public final static List<Socket> socketList = new ArrayList<Socket>();
        private FTC_Listener listener;
        private Thread t;

        public MyService(FTC_Listener listener) {
            this.listener = listener;
        }

        public void run() {
            while (listening == true) {
                Socket s = null;
                try {
                    s = server.accept();
                    if (s != null) {
                        Log.e("client", "connectStatus!!");
                        socketList.add(s);
                        service = new ServiceThread(s, listener);
                        t = new Thread(service);
                        t.start();
                    } else
                        System.out
                                .println("------------socket s = null--------------");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
