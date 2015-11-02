package ozner.xzy.test;

import android.content.Intent;

public class OznerApplication extends OznerBaseApplication {
    public static final String ACTION_ServiceInit = "ozner.service.init";

    @Override
    protected void onBindService() {
        getService().getDeviceManager().setOwner("xzy");
        this.sendBroadcast(new Intent(ACTION_ServiceInit));
    }

}
