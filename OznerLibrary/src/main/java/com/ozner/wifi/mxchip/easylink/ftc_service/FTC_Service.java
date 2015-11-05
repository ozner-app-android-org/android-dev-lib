package com.ozner.wifi.mxchip.easylink.ftc_service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.ozner.wifi.mxchip.easylink.easylink_plus.EasyLink_plus;
import com.ozner.wifi.mxchip.easylink.helper.Helper;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class FTC_Service {
    private static final int REQUEST_TIMEOUT = 5 * 1000;// ��������ʱ10����
    private static final int SO_TIMEOUT = 20 * 1000; // ���õȴ����ݳ�ʱʱ��10����
    // wifi模块连接FTC的标记，0 未连接，1 已连接
    public static int ftcConTag = 0;
    private static boolean listening;
    private static ServerSocket server = null;
    private static ServiceThread service;
    private static FTC_Service ftc = null;
    private Thread listen;
    private EasyLink_plus easylink_plus;
    private Context ctx;

    private FTC_Service() {
        listening = true;
    }

    public static FTC_Service getInstence() {
        if (ftc == null) {
            ftc = new FTC_Service();
        }
        return ftc;
    }

    @SuppressLint("NewApi")
    /**
     *
     * @param context activity的Context
     * @param ssid wifi的SSID
     * @param key wifi密码
     * @param phone_ip 手机ip
     * @param listener 回调
     */
    public void transmitSettings(final Context context, final String ssid,
                                 final String key, final int phone_ip, final FTC_Listener listener) {
        listening = true;
        if (null == server) {
            try {
                server = new ServerSocket();
                server.setReuseAddress(true);
                server.bind(new InetSocketAddress(8000));
                // server.setSoTimeout(0);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        ctx = context;
        easylink_plus = EasyLink_plus.getInstence();
        try {
            NetworkInterface intf = NetworkInterface.getByName("wlan0");
            if (intf.getMTU() < 1500) {
                easylink_plus.setSmallMtu(true);
                listener.isSmallMTU(intf.getMTU());
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                listen = new Thread(new MyService(listener));
                listen.start();

                int ipAddress = phone_ip;
                byte[] userinfo = new byte[5];
                userinfo[0] = 0x23; // #
                String strIP = String.format("%08x", ipAddress);
                System.arraycopy(Helper.hexStringToBytes(strIP), 0, userinfo,
                        1, 4);


                try {
                    easylink_plus.transmitSettings(ssid.getBytes("UTF-8"),
                            key.getBytes("UTF-8"), userinfo, ipAddress);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // send(ssid, key, phone_ip);
            }
        }).start();
    }

    public void stopTransmitting() {
        listening = false;
        ftcConTag = 0;
        try {
            if (null != server) {
                server.close();
                server = null;
            }
            easylink_plus = EasyLink_plus.getInstence();
            easylink_plus.stopTransmitting();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    /**
     * �������ʱʱ��͵ȴ�ʱ��
     *
     * @return HttpClient����
     */
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
                // Log.e("BufferedReader", lineStr);
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
        // ���屣�����е�Socket
        public final static List<Socket> socketList = new ArrayList<Socket>();
        private FTC_Listener listener;
        private Thread t;

        // private ServiceThread service;

        public MyService(FTC_Listener listener) {
            this.listener = listener;
        }

        public void run() {
            while (listening == true) {
                Socket s = null;
                try {
                    s = server.accept();
                    if (s != null) {
                        ftcConTag = 1;
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
            // try {
            // service.recv = false;
            // server = null;
            // } catch (Exception e) {
            // e.printStackTrace();
            // }
        }
    }
}

