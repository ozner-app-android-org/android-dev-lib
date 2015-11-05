package com.example.oznerble;

import android.content.Intent;

public class OznerApplication extends OznerBLEApplication {
    public static final String ACTION_ServiceInit = "ozner.service.init";

    @Override
    protected void onBindService() {
        getService().getDeviceManager().setOwner("xzy");
        this.sendBroadcast(new Intent(ACTION_ServiceInit));
    }

}
