package com.ozner.wifi.mxchip;

import android.content.Context;

import com.ozner.device.BaseDeviceIO;
import com.ozner.device.IOManager;

import java.util.ArrayList;

//import com.mxchip.jmdns.JmdnsAPI;

/**
 * Created by xzyxd on 2015/10/31.
 */
public class MXChipIOManager extends IOManager {

    final MQTTProxyImp mqttProxyImp = new MQTTProxyImp();
    MQTTProxy proxy;
    MXChipIOManager ioManager;


    public MXChipIOManager(Context context) {
        super(context);
        proxy = new MQTTProxy(context);
        proxy.registerListener(mqttProxyImp);
    }

//    private void load()
//    {
//        OznerDevice[] list=OznerDeviceManager.Instance().getDevices();
//        synchronized (localList) {
//            for (OznerDevice device : list) {
//                if (device.getIOType().equals(MXChipIO.class)) {
//                    if (!localList.containsKey(device.Address())) {
//                        MXChipIO io = new MXChipIO(context(), proxy, device.getName(), device.Model(), device.Address());
//                        localList.put(io.getAddress(), io);
//                    }
//                }
//            }
//        }
//    }

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
//
//    @Override
//    protected void doUnavailable(BaseDeviceIO io) {
//        super.doUnavailable(io);
//        if (proxy.connected) {
//            Handler handler = new Handler(Looper.getMainLooper())
//            {
//                @Override
//                public void handleMessage(Message msg) {
//                    doAvailable((BaseDeviceIO)msg.obj);
//                }
//            };
//            Message msg=new Message();
//            msg.obj=io;
//            handler.sendMessageDelayed(msg,1000);
//
//        }
//    }

    @Override
    public BaseDeviceIO getAvailableDevice(String address) {
        if (!proxy.isConnected()) {
            return null;
        } else
            return super.getAvailableDevice(address);
    }

    @Override
    public BaseDeviceIO[] getAvailableDevices() {
        if (!proxy.isConnected()) {
            return null;
        } else
            return super.getAvailableDevices();

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

    class MQTTProxyImp implements MQTTProxy.MQTTListener {

        @Override
        public void onConnected(MQTTProxy proxy) {
            ArrayList<BaseDeviceIO> list = null;
            synchronized (devices) {
                list = new ArrayList<>(devices.values());
            }

            for (BaseDeviceIO io : list) {
                doAvailable(io);
            }

        }

        @Override
        public void onDisconnected(MQTTProxy proxy) {
            ArrayList<BaseDeviceIO> list = null;
            synchronized (devices) {
                list = new ArrayList<>(devices.values());
            }

            for (BaseDeviceIO io : list) {
                doAvailable(io);
            }
        }

        @Override
        public void onPublish(MQTTProxy proxy, String topic, byte[] data) {

        }
    }

}
