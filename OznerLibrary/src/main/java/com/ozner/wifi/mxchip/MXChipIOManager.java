package com.ozner.wifi.mxchip;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.ozner.device.BaseDeviceIO;
import com.ozner.device.IOManager;


/**
 * Created by xzyxd on 2015/10/31.
 */
public class MXChipIOManager extends IOManager {

    final MQTTProxyImp mqttProxyImp = new MQTTProxyImp();
    MQTTProxy proxy;

    public MXChipIOManager(Context context) {
        super(context);
        proxy = new MQTTProxy(context);
        proxy.registerListener(mqttProxyImp);
    }


    public MXChipIO createNewIO(String address, String Type) throws ClassCastException {
        try {
            MXChipIO mxChipIO = (MXChipIO) super.getAvailableDevice(address);
            if (mxChipIO == null) {
                mxChipIO = new MXChipIO(context(), proxy,  Type, address);
            }
            if (proxy.isConnected())
                doAvailable(mxChipIO);
            return mxChipIO;
        }catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

    }
    final static int delayedAvailableMessage=0x1000;

    @Override
    protected void doUnavailable(BaseDeviceIO io) {
        super.doUnavailable(io);
        Handler handler=new Handler(Looper.getMainLooper())
        {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what==delayedAvailableMessage)
                {
                    if (proxy.isConnected()) {
                        doAvailable((BaseDeviceIO) msg.obj);
                    }
                }
            }
        };
        if (proxy.isConnected()) {
            Message m = new Message();
            m.what = delayedAvailableMessage;
            m.obj = io;
            handler.sendMessageDelayed(m, 5000); //如果IO被关闭了,MQTT还是连接中的情况下,重新激活IO
        }
    }

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
            BaseDeviceIO[] list=MXChipIOManager.super.getAvailableDevices();
            if (list!=null) {
                for (BaseDeviceIO io : list) {
                    if (io instanceof MXChipIO) {
                        doAvailable(io);
                    }
                }
            }

        }

        @Override
        public void onDisconnected(MQTTProxy proxy) {
            BaseDeviceIO[] list=MXChipIOManager.super.getAvailableDevices();
            if (list!=null) {
                for (BaseDeviceIO io : list) {
                    if (io instanceof MXChipIO) {
                        doUnavailable(io);
                    }
                }
            }
        }

        @Override
        public void onPublish(MQTTProxy proxy, String topic, byte[] data) {

        }
    }

}
