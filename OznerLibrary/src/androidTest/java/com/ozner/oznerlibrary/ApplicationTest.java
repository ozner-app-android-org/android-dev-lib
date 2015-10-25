package com.ozner.oznerlibrary;

import android.app.Application;
import android.test.ApplicationTestCase;

import com.ozner.wifi.mxchip.MXWifiChip;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    public void testWifi() {
        MXWifiChip mxchip = new MXWifiChip(this.getContext());
        //MXWifiChip.startWifiConfiguration("ITDEV", "87654321");

    }

}