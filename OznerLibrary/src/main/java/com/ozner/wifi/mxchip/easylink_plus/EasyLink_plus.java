/**
 *
 */
package com.ozner.wifi.mxchip.easylink_plus;

import android.content.Context;
import android.net.wifi.WifiManager;

import com.ozner.wifi.mxchip.easylink_v3.EasyLink_v3;
import com.ozner.wifi.mxchip.helper.Helper;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Perry
 * @date 2014-10-21
 */
public class EasyLink_plus {
    //private static EasyLink_v2 e2;
    private static EasyLink_v3 e3;
    //    private static EasyLink_minus minus;
    private static EasyLink_plus me;
    boolean sending = true;
    ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    private WifiManager wifiManager;

    private EasyLink_plus(Context ctx) {
        try {
            wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
//            e2 = EasyLink_v2.getInstence();
            e3 = EasyLink_v3.getInstence();
//            minus = new EasyLink_minus(ctx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static EasyLink_plus getInstence(Context ctx) {
        if (me == null) {
            me = new EasyLink_plus(ctx);
        }
        return me;
    }

    public void setSmallMtu(boolean onoff) {
        e3.SetSmallMTU(onoff);
    }

    public void transmitSettings(final String ssid, final String key,
                                 final int ipAddress) {
        try {
            final byte[] ssid_byte = ssid.getBytes("UTF-8");
            final byte[] key_byte = key.getBytes("UTF-8");
            final byte[] userinfo = new byte[5];
            userinfo[0] = 0x23; // #
            String strIP = String.format("%08x", ipAddress);
            System.arraycopy(Helper.hexStringToBytes(strIP), 0, userinfo, 1, 4);
            //WifiInfo info= wifiManager.getConnectionInfo();


            singleThreadExecutor = Executors.newSingleThreadExecutor();
            sending = true;
            singleThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    while (sending) {
                        try {
                            //minus.transmitSettings(ssid, key, ipAddress);
                            int broadcatIp = 0xFF000000 | ipAddress;

                            String ipString = ((broadcatIp & 0xff) + "." + (broadcatIp >> 8 & 0xff) + "."
                                    + (broadcatIp >> 16 & 0xff) + "." + (broadcatIp >> 24 & 0xff));
                            //ipString="255.255.255.255";
                            //e2.transmitSettings(ssid_byte, key_byte, userinfo);
                            e3.transmitSettings(ssid_byte, key_byte, ipString, userinfo);
                            // Log.e("minus--->", "sending");
                            try {
                                Thread.sleep(10 * 1000);
                                //e2.stopTransmitting();
                                e3.stopTransmitting();
                                //minus.stopTransmitting();
                                // Log.e("easylink", "STOP!!!!");
                                //Thread.sleep(3 * 1000);
                                Thread.sleep(10 * 1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void stopTransmitting() {
        sending = false;
        singleThreadExecutor.shutdown();
//        e2.stopTransmitting();
        e3.stopTransmitting();
//        minus.stopTransmitting();
    }
}
