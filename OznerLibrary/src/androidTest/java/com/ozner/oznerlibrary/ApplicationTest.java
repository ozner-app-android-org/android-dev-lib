package com.ozner.oznerlibrary;

import android.app.Application;
import android.test.ApplicationTestCase;

import com.ozner.wifi.mxchip.MXChip;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    public void testWifi() {
        MXChip mxchip = new MXChip(this.getContext());
        //MXChip.startWifiConfiguration("ITDEV", "87654321");

    }

}