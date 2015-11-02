package com.ozner.wifi.mxchip;

import android.content.Context;

import com.ozner.device.BaseDeviceIO;
import com.ozner.device.IOManager;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

import java.util.HashMap;

//import com.mxchip.jmdns.JmdnsAPI;

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
                        MXChipIO io = new MXChipIO(context(), proxy, device.getName(), device.Model(), device.Address());
                        localList.put(io.getAddress(), io);
                    }
                }
            }
        }
    }

    public MXChipIO createNewIO(String Name, String address, String Model) throws ClassCastException {
        synchronized (devices) {
            if (devices.containsKey(address)) {
                return (MXChipIO) devices.get(address);
            }
            MXChipIO io = new MXChipIO(context(), proxy, Name, Model, address);
            devices.put(address, io);
            return io;
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
