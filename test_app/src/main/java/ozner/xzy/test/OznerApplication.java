package ozner.xzy.test;

import android.content.Intent;

public class OznerApplication extends OznerBaseApplication {
    public static final String ACTION_ServiceInit = "ozner.service.init";

    @Override
    protected void onBindService() {
        getService().getDeviceManager().setOwner("18001919461","eyJ1dWlkIjoiZTJiNmE0MzUtODExZi00ODY5LWI3MGUtNWI4NWM4ZGQyYTZkIiwic2Vzc2lvbmlkIjoiMmYzdXF5bWFjNG90aTFtMDNyMXdseHFpIiwidHlwZSI6ImFjY2Vzc190b2tlbiJ9");

        this.sendBroadcast(new Intent(ACTION_ServiceInit));
    }

}
