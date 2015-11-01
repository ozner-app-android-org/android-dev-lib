package com.ozner.wifi.mxchip;

import android.content.Context;
import android.net.wifi.WifiManager;

import com.mxchip.jmdns.JmdnsAPI;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.IOManager;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;
import com.ozner.util.Helper;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.MQTT;

import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * Created by xzyxd on 2015/10/31.
 */
public class MXChipIOManager extends IOManager {

    final HashMap<String,MXChipIO> localList=new HashMap<>();
    MQTTProxy proxy;

    private void load()
    {
        OznerDevice[] list=OznerDeviceManager.Instance().getDevices();
        synchronized (localList) {
            for (OznerDevice device : list) {
                if (device.getIOType().equals(MXChipIO.class)) {
                    if (!localList.containsKey(device.Address())) {
                        MXChipIO io = new MXChipIO(context(),proxy,device.Model(),device.Address());
                        localList.put(io.getAddress(), io);
                    }
                }
            }
        }
    }

    @Override
    public BaseDeviceIO getAvailableDevice(String address) {
        if (!proxy.isConnected())
        {
            return null;
        }else
            return super.getAvailableDevice(address);
    }

    @Override
    public BaseDeviceIO[] getAvailableDevices() {
        if (!proxy.isConnected())
        {
            return null;
        }else
            return super.getAvailableDevices();
    }


    public MXChipIOManager(Context context) {
        super(context);
        proxy=new MQTTProxy(context);
    }



    @Override
    public void Start() {
        proxy.start();
    }

    @Override
    public void Stop() {
        proxy.stop();
    }

    @Override
    protected void doChangeRunningMode() {
        super.doChangeRunningMode();
    }

}
