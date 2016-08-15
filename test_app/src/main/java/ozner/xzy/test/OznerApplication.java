package ozner.xzy.test;

import android.content.Intent;

public class OznerApplication extends OznerBaseApplication {
    public static final String ACTION_ServiceInit = "ozner.service.init";

    @Override
    protected void onBindService() {
        getService().getDeviceManager().setOwner("18001919461","eyJ1dWlkIjoiZDY5OGFlOWItYzhjOS00NmQ2LWJjZGQtMjEzNDc3NTI5ZDgyIiwic2Vzc2lvbmlkIjoicHB4ZWlubXIxMnZjbDNrcWplM3NjZGRtIiwidHlwZSI6ImFjY2Vzc190b2tlbiJ9");

        this.sendBroadcast(new Intent(ACTION_ServiceInit));
    }

}
