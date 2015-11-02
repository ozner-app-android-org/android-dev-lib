package com.ozner.wifi.mxchip;

import android.content.Context;

import com.ozner.device.BaseDeviceIO;
import com.ozner.device.DeviceNotReadyException;
import com.ozner.device.OperateCallback;
import com.ozner.util.Helper;

/**
 * Created by xzyxd on 2015/10/31.
 */
public class MXChipIO extends BaseDeviceIO {
    String address="";
    String name = "";
    MQTTProxy proxy;
    final MQTTProxy.MQTTListener listener=new MQTTListenerImp();
    String out=null;
    String in=null;

    public MXChipIO(Context context, MQTTProxy proxy, String Name, String Model, String address)
    {
        super(context,Model);
        this.address=address;
        this.name = name;
        this.proxy=proxy;
        proxy.registerListener(listener);
    }

    class MQTTListenerImp implements MQTTProxy.MQTTListener
    {
        @Override
        public void onConnected(MQTTProxy proxy) {
            proxy.subscribe(out);
            doConnected();
        }

        @Override
        public void onDisconnected(MQTTProxy proxy) {
            doDisconnected();
        }

        @Override
        public void onPublish(MQTTProxy proxy, String topic, byte[] data) {
            if (topic.equals(in))
            {
                doRecv(data);
            }
        }
    }

    /**
     * 设置MQTT设备的分类ID,每个庆科设备都有一个"分类ID/MAC"组成的MQTT订阅主题,在订阅消息前调用
     * @param secureCode
     */
    public void setSecureCode(String secureCode)
    {
        out=secureCode+"/"+address+"in";
        in=secureCode+"/"+address+"out";
    }

    @Override
    public boolean send(byte[] bytes) {
        if (proxy.isConnected()) {
            proxy.publish(out, bytes,null);
            doSend(bytes);
            return true;
        }else
            return false;
    }

    @Override
    public boolean send(byte[] bytes, OperateCallback<Void> callback) {
        if (proxy.isConnected()) {
            proxy.publish(out, bytes,callback);
            doSend(bytes);
            return true;
        }else
            return false;
    }

    @Override
    public void close() {
        proxy.unsubscribe(in);
        proxy.unregisterListener(listener);
    }

    @Override
    public void open() throws DeviceNotReadyException {
        if (Helper.StringIsNullOrEmpty(in))
            throw new DeviceNotReadyException();
        if (proxy.isConnected())
        {
            this.proxy.subscribe(in);
        }

    }

    @Override
    public boolean connected() {
        return proxy.isConnected();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAddress() {
        return address;
    }
}
