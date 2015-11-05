//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.ozner.wifi.mxchip.easylink.easylink_plus;

import com.ozner.wifi.mxchip.easylink.easylink_v2.EasyLink_v2;
import com.ozner.wifi.mxchip.easylink.easylink_v3.EasyLink_v3;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EasyLink_plus {
    private static EasyLink_v2 e2;
    private static EasyLink_v3 e3;
    private static EasyLink_plus me;
    boolean sending = true;
    ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    private EasyLink_plus() {
        try {
            e2 = EasyLink_v2.getInstence();
            e3 = EasyLink_v3.getInstence();
        } catch (Exception var2) {
            var2.printStackTrace();
        }

    }

    public static EasyLink_plus getInstence() {
        if (me == null) {
            me = new EasyLink_plus();
        }

        return me;
    }

    public void setSmallMtu(boolean onoff) {
        e3.SetSmallMTU(onoff);
    }

    public void transmitSettings(final byte[] ssid, final byte[] key, final byte[] userinfo, final int ipAddress) {
        this.singleThreadExecutor = Executors.newSingleThreadExecutor();
        this.sending = true;
        this.singleThreadExecutor.execute(new Runnable() {
            public void run() {
                while (EasyLink_plus.this.sending) {
                    try {
                        //int broadcatIp = 0xFF000000 | ipAddress;

                        //String ipString = ((broadcatIp & 0xff) + "." + (broadcatIp >> 8 & 0xff) + "."
                        //        + (broadcatIp >> 16 & 0xff) + "." + (broadcatIp >> 24 & 0xff));
                        String ipString = "255.255.255.255";

                        EasyLink_plus.e2.transmitSettings(ssid, key, userinfo);
                        EasyLink_plus.e3.transmitSettings(ssid, key, ipString, userinfo);

                        try {
                            Thread.sleep(10000L);
                            EasyLink_plus.e2.stopTransmitting();
                            EasyLink_plus.e3.stopTransmitting();
                            Thread.sleep(10000L);
                        } catch (InterruptedException var2) {
                            var2.printStackTrace();
                        }
                    } catch (Exception var3) {
                        var3.printStackTrace();
                    }
                }

            }
        });
    }

    public void stopTransmitting() {
        this.sending = false;
        this.singleThreadExecutor.shutdown();
        e2.stopTransmitting();
        e3.stopTransmitting();
    }
}
