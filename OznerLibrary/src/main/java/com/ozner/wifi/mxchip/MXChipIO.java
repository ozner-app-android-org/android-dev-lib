package com.ozner.wifi.mxchip;

import android.content.Context;

import com.ozner.device.BaseDeviceIO;
import com.ozner.device.DeviceNotReadyException;
import com.ozner.device.OperateCallback;
import com.ozner.util.Helper;

import org.fusesource.mqtt.client.Callback;

/**
 * Created by xzyxd on 2015/10/31.
 */
public class MXChipIO extends BaseDeviceIO {
    final MQTTProxy.MQTTListener listener = new MQTTListenerImp();
    String address = "";
    String name = "";
    MQTTProxy proxy;
    String out = null;
    String in = null;
    boolean isInited = false;

    public MXChipIO(Context context, MQTTProxy proxy, String Name, String Model, String address) {
        super(context, Model);
        this.address = address;
        this.name = Name;
        this.proxy = proxy;
        proxy.registerListener(listener);
        doConnecting();
    }

    /**
     * 将指定字符串src，以每两个字符分割转换为16进制形式 如："2B44EFD9" --> byte[]{0x2B, 0x44, 0xEF,
     * 0xD9}
     *
     * @param src String
     * @return byte[]
     */
    public static byte[] HexString2Bytes(String src) {
        byte[] tmp = src.getBytes();
        int len = tmp.length / 2;
        byte[] ret = new byte[len];
        for (int i = 0; i < len; i++) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
        }
        return ret;
    }

    /**
     * 将两个ASCII字符合成一个字节； 如："EF"--> 0xEF
     *
     * @param src0 byte
     * @param src1 byte
     * @return byte
     */
    public static byte uniteBytes(byte src0, byte src1) {
        byte _b0 = Byte.decode("0x" + new String(new byte[]{src0}))
                .byteValue();
        _b0 = (byte) (_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[]{src1}))
                .byteValue();
        byte ret = (byte) (_b0 ^ _b1);
        return ret;
    }

    private void doSubscribeComplete(byte[] bytes) {
        synchronized (this) {
            if (isInited) return;
            isInited = true; //防止多次进入
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (doInit(new MQTTSendProxy())) {
                    doReady();
                } else
                    doDisconnected();
            }
        }).start();


    }

    @Override
    protected void doDisconnected() {
        isInited = false;
        super.doDisconnected();
    }

    /**
     * 设置MQTT设备的分类ID,每个庆科设备都有一个"分类ID/MAC"组成的MQTT订阅主题,在订阅消息前调用
     */
    public void setSecureCode(String secureCode) {
        in = secureCode + "/" + address.replace(":", "").toLowerCase() + "/in";
        out = secureCode + "/" + address.replace(":", "").toLowerCase() + "/out";
    }

    @Override
    public boolean send(byte[] bytes) {
        if (proxy.isConnected()) {
            proxy.publish(in, bytes, null);
            doSend(bytes);
            return true;
        } else
            return false;
    }

    @Override
    protected void doConnected() {
        super.doConnected();
    }

    @Override
    protected void doReady() {
        super.doReady();
    }

    @Override
    public boolean send(byte[] bytes, OperateCallback<Void> callback) {
        if (proxy.isConnected()) {
            proxy.publish(in, bytes, callback);
            doSend(bytes);
            return true;
        } else
            return false;
    }

    @Override
    public void close() {
        if (proxy.isConnected()) {
            proxy.unsubscribe(out);
            proxy.unregisterListener(listener);
        }
    }

    @Override
    public void open() throws DeviceNotReadyException {
        if (Helper.StringIsNullOrEmpty(out))
            throw new DeviceNotReadyException();
        if (proxy.isConnected()) {
            this.proxy.subscribe(out, new Callback<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    doSubscribeComplete(bytes);
                }

                @Override
                public void onFailure(Throwable throwable) {

                }
            });
        }
    }

    @Override
    public ConnectStatus connectStatus() {
        if (proxy.connected) {
            if (isReady())
                return ConnectStatus.Connected;
            else
                return ConnectStatus.Connecting;
        } else
            return ConnectStatus.Disconnect;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAddress() {
        return address;
    }

    public class MQTTSendProxy extends DataSendProxy {
        @Override
        public boolean send(byte[] data) {
            return MXChipIO.this.send(data);
        }

        @Override
        public boolean send(byte[] data, OperateCallback<Void> callback) {
            return MXChipIO.this.send(data, callback);
        }
    }

    class MQTTListenerImp implements MQTTProxy.MQTTListener {
        @Override
        public void onConnected(MQTTProxy proxy) {
            doConnected();
            proxy.subscribe(out, new Callback<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    doSubscribeComplete(bytes);
                }

                @Override
                public void onFailure(Throwable throwable) {

                }
            });
        }


        @Override
        public void onDisconnected(MQTTProxy proxy) {
            doDisconnected();
        }

        @Override
        public void onPublish(MQTTProxy proxy, String topic, byte[] data) {
            if (topic.equals(out)) {
                doRecv(data);
            }
        }
    }
}
