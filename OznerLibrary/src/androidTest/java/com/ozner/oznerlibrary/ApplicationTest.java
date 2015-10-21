package com.ozner.oznerlibrary;

import android.app.Application;
import android.test.ApplicationTestCase;

import com.ozner.wifi.mxchip.mxchip;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    public void testWifi() {
        mxchip mxchip = new mxchip(this.getContext());
        mxchip.start("ITDEV", "87654321");

    }

}