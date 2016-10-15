package com.ozner.wifi.ayla;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.aylanetworks.aaml.AylaCache;
import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSetup;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.ozner.XObject;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.IOManager;
import com.ozner.util.dbg;

/**
 * Created by zhiyongxu on 16/4/26.
 */
public class AylaIOManager extends IOManager {
    public final static String gblAmlDeviceSsidRegex = "^OZNER_WATER-[0-9A-Fa-f]{12}";
    final static String appId="a_ozner_water_mobile-cn-id";
    static String lanIpServiceBaseURL(String lanIp) {
        String url = String.format("http://%s/", lanIp);
        return url;
    }

    public AylaIOManager(Context context) {
        super(context);

    }


    @Override
    public void removeDevice(BaseDeviceIO io) {
        final AylaIO aylaIO=(AylaIO)io;
        aylaIO.aylaDevice.unregisterDevice(new Handler()
        {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what==AylaNetworks.AML_ERROR_OK)
                {
                    dbg.i("Ayla unregisterDevice complete");
                }else
                {
                    dbg.i("Ayla unregisterDevice Error:"+msg.toString());
                }
                super.handleMessage(msg);
            }
        });
    }


    @Override
    public void Start(String user,String token) {
        AylaCache.clearAll();
        //AylaNetworks.init(context(),gblAmlDeviceSsidRegex,"super app");
        AylaSetup.init(context(), gblAmlDeviceSsidRegex, "super app");
        AylaSystemUtils.serviceType=AylaNetworks.AML_DEVELOPMENT_SERVICE;
        AylaSystemUtils.setServicelocationWithCountryCode("CN");
        AylaSystemUtils.loggingLevel=AylaNetworks.AML_LOGGING_LEVEL_INFO;
        AylaSystemUtils.slowConnection=AylaNetworks.YES;
        AylaSystemUtils.saveCurrentSettings();

        AylaUser.ssoLogin(new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what==AylaNetworks.AML_ERROR_OK)
                {
                    String jsonResults=msg.obj.toString();
                    AylaUser aylaUser = AylaSystemUtils.gson.fromJson(jsonResults,  AylaUser.class);
                    AylaUser.setCurrent(aylaUser);
                    dbg.i("AylaSSO Complete");
                    AylaDevice.getDevices(new Handler()
                    {
                        @Override
                        public void handleMessage(Message msg) {
                            if (msg.what==AylaNetworks.AML_ERROR_OK)
                            {
                                String jsonResults=msg.obj.toString();
                                AylaDevice[] devices = AylaSystemUtils.gson.fromJson(jsonResults,  AylaDevice[].class);
                                for (AylaDevice device : devices)
                                {
                                    dbg.i("load:"+device.toString());
                                    createAylaIO(device);

                                }
                            }

                            super.handleMessage(msg);
                        }
                    });
                }else
                {
                    dbg.e("AylaError:%d Msg:%s",msg.what,msg.toString());
                }
                super.handleMessage(msg);
            }

        },user,"",token,appId,"a_ozner_water_mobile-cn-7331816");

    }

    @Override
    protected void doChangeRunningMode() {
        if (XObject.getRunningMode()==RunningMode.Foreground)
        {
            AylaNetworks.onResume();
        }else
        {
            AylaNetworks.onPause(true);
        }
        super.doChangeRunningMode();
    }

    @Override
    public void Stop() {
        AylaSetup.exit();
    }

    public AylaIO createAylaIO(AylaDevice device)
    {
        AylaIO io=new AylaIO(context(),device);
        doAvailable(io);
        return io;
    }

}
