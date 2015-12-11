package com.ozner.wifi.mxchip;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.ozner.device.BaseDeviceIO;
import com.ozner.device.IOManager;

import java.util.HashMap;


/**
 * Created by xzyxd on 2015/10/31.
 */
public class MXChipIOManager extends IOManager {
    HashMap<String,String> listenDeviceList=new HashMap<>();

    final MQTTProxyImp mqttProxyImp = new MQTTProxyImp();
    MQTTProxy proxy;

    public MXChipIOManager(Context context) {
        super(context);
        proxy = new MQTTProxy(context);
        proxy.registerListener(mqttProxyImp);
    }


    public MXChipIO addListenerAddress(String address, String type) throws ClassCastException {
        synchronized (listenDeviceList) {
            try {
                if (!listenDeviceList.containsKey(address))
                {
                    listenDeviceList.put(address,type);
                    if (proxy.isConnected()) {
                        MXChipIO io=new MXChipIO(context(),proxy, address,type);
                        doAvailable(io);
                        return io;
                    }else
                    {
                        return null;
                    }
                }else
                {
                    return new MXChipIO(context(),proxy, address,type);
                }

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
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
            m.obj = io.getAddress();
            handler.sendMessageDelayed(m, 5000); //如果IO被关闭了,MQTT还是连接中的情况下,重新激活IO
        }
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
            synchronized (listenDeviceList) {
                for (String address:listenDeviceList.keySet()) {
                    MXChipIO io=new MXChipIO(context(),proxy,address,listenDeviceList.get(address));
                    doAvailable(io);
                }
            }
        }

        @Override
        public void onDisconnected(MQTTProxy proxy) {
            synchronized (listenDeviceList) {
                for (String address:listenDeviceList.keySet()) {
                    BaseDeviceIO io=getAvailableDevice(address);
                    if (io!=null)
                        doUnavailable(io);
                }
            }
        }

        @Override
        public void onPublish(MQTTProxy proxy, String topic, byte[] data) {

        }
    }

}
