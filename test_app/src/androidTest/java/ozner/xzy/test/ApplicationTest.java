package ozner.xzy.test;

import android.app.Application;
import android.test.ApplicationTestCase;

import com.ozner.util.HttpUtil;

import java.io.IOException;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    public void test()
    {
        String json="{ \"N\": \"EMW3162(43FF3C)\", \"C\": [ { \"N\": \"MICO SYSTEM\", \"C\": [ { \"N\": \"Device Name\", \"C\": \"EMW3162 EASYCLOUD\", \"P\": \"RW\" }, { \"N\": \"Bonjour\", \"C\": true, \"P\": \"RW\" }, { \"N\": \"RF power save\", \"C\": false, \"P\": \"RW\" }, { \"N\": \"MCU power save\", \"C\": false, \"P\": \"RW\" } ] }, { \"N\": \"WLAN\", \"C\": [ { \"N\": \"Wi-Fi\", \"C\": \"ITDEV\", \"P\": \"RW\" }, { \"N\": \"Password\", \"C\": \"87654321\", \"P\": \"RW\" }, { \"N\": \"DHCP\", \"C\": true, \"P\": \"RW\" }, { \"N\": \"IP address\", \"C\": \"10.203.1.76\", \"P\": \"RW\" }, { \"N\": \"Net Mask\", \"C\": \"255.255.255.0\", \"P\": \"RW\" }, { \"N\": \"Gateway\", \"C\": \"10.203.1.1\", \"P\": \"RW\" }, { \"N\": \"DNS Server\", \"C\": \"192.168.1.239\", \"P\": \"RW\" } ] }, { \"N\": \"Cloud info\", \"C\": [ { \"N\": \"activated\", \"C\": false, \"P\": \"RO\" }, { \"N\": \"connected\", \"C\": false, \"P\": \"RO\" }, { \"N\": \"rom version\", \"C\": \"MXCHIP_HAOZE_Water@023\", \"P\": \"RO\" }, { \"N\": \"device_id\", \"C\": \"none\", \"P\": \"RW\" }, { \"N\": \"Cloud settings\", \"C\": [ { \"N\": \"Authentication\", \"C\": [ { \"N\": \"login_id\", \"C\": \"admin\", \"P\": \"RW\" }, { \"N\": \"devPasswd\", \"C\": \"12345678\", \"P\": \"RW\" } ] } ] } ] } ], \"PO\": \"com.mxchip.easycloud\", \"HD\": \"3162\", \"FW\": \"MXCHIP_HAOZE_Water@\" }";
        try {
            HttpUtil.postJSON("http://10.203.1.78:8000",json,null);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}


