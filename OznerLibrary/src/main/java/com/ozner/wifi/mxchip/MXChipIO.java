package com.ozner.wifi.mxchip;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.ozner.device.BaseDeviceIO;
import com.ozner.device.DeviceNotReadyException;
import com.ozner.device.OperateCallback;
import com.ozner.util.Helper;

import org.fusesource.mqtt.client.Callback;

/**
 * Created by xzyxd on 2015/10/31.
 */
public class MXChipIO extends BaseDeviceIO {
    final MXChipIOImp mxChipIOImp = new MXChipIOImp();
    String address = "";
    String name = "";
    MQTTProxy proxy;
    String out = null;
    String in = null;

    public MXChipIO(Context context, MQTTProxy proxy, String Name, String Model, String address) {
        super(context, Model);
        this.address = address;
        this.name = Name;
        this.proxy = proxy;
        proxy.registerListener(mxChipIOImp);
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



    /**
     * 设置MQTT设备的分类ID,每个庆科设备都有一个"分类ID/MAC"组成的MQTT订阅主题,在订阅消息前调用
     */
    public void setSecureCode(String secureCode) {
        in = secureCode + "/" + address.replace(":", "").toLowerCase() + "/in";
        out = secureCode + "/" + address.replace(":", "").toLowerCase() + "/out";
    }

    @Override
    public boolean send(byte[] bytes) {
        return mxChipIOImp.postSend(bytes, null);
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
        mxChipIOImp.close();
    }

    @Override
    public void open() throws DeviceNotReadyException {
        if (Helper.StringIsNullOrEmpty(out))
            throw new DeviceNotReadyException();
        mxChipIOImp.start();
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

    public class MXChipIOSendProxy extends DataSendProxy {
        @Override
        public boolean send(byte[] data) {
            return mxChipIOImp.postSend(data, null);
        }

        @Override
        public boolean send(byte[] data, OperateCallback<Void> callback) {
            return mxChipIOImp.postSend(data, callback);
        }
    }

    public interface MXChipRunnable {
        void run(DataSendProxy sendHandle);
    }

    class MXChipIOImp implements MQTTProxy.MQTTListener, Runnable {
        final static int MSG_SendData = 0x1000;
        final static int MSG_Runnable = 0x2000;
        Thread thread = null;
        Looper looper = null;
        MessageHandler handler = null;
        static final int Timeout = 10000;

        class MessageHandler extends Handler {
            public MessageHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                try {
                    if (msg.what == MSG_SendData) {
                        AsyncObject object = (AsyncObject) msg.obj;
                        try {
                            if (send(object.data)) {
                                if (object.callback != null)
                                    object.callback.onSuccess(null);
                            } else {
                                if (object.callback != null)
                                    object.callback.onFailure(null);
                            }
                        } catch (Exception e) {
                            if (object.callback != null)
                                object.callback.onFailure(e);
                            throw e;
                        }

                    } else if (msg.what == MSG_Runnable) {
                        MXChipRunnable runable = (MXChipRunnable) msg.obj;
                        runable.run(new MXChipIOSendProxy());
                    }

                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.handleMessage(msg);
            }
        }

        class AsyncObject {
            public byte[] data;
            public OperateCallback<Void> callback;

            public AsyncObject(byte[] data, OperateCallback<Void> callback) {
                this.data = data;
                this.callback = callback;
            }

        }

        @Override
        public void onConnected(MQTTProxy proxy) {

        }


        @Override
        public void onDisconnected(MQTTProxy proxy) {
            close();
        }

        @Override
        public void onPublish(MQTTProxy proxy, String topic, byte[] data) {
            if (topic.equals(out)) {
                doRecv(data);
            }
        }


        /**
         * 设置一个循环发送runnable,来执行发送大数据包,比如挂件升级过程
         */
        public boolean postRunable(MXChipRunnable runnable) {
            if (handler == null) return false;
            Message message = new Message();
            message.what = MSG_Runnable;
            message.obj = runnable;
            return handler.sendMessage(message);
        }

        public boolean postSend(byte[] data, OperateCallback<Void> callback) {
            if (Thread.currentThread().getId() == thread.getId()) {
                try {
                    if (send(data)) {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                        return true;
                    } else {
                        if (callback != null) {
                            callback.onFailure(null);
                        }
                        return false;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onFailure(e);
                    return false;
                }
            } else {
                if (handler != null) {
                    Message message = new Message();
                    message.what = MSG_SendData;
                    message.obj = new AsyncObject(data, callback);
                    return handler.sendMessage(message);
                } else
                    return false;
            }

        }

        public boolean isRuning() {
            return thread != null && thread.isAlive();
        }

        public void start() throws DeviceNotReadyException {
            if (isRuning()) {
                throw new DeviceNotReadyException();
            }
            thread = new Thread(this);
            thread.start();
        }

        public void close() {
            if (isRuning()) {
                setObject();
                if (looper != null)
                    looper.quit();
                thread.interrupt();
            }
        }

        private boolean subscribed = false;

        @Override
        public void run() {
            try {
                if (!proxy.isConnected()) return;
                proxy.subscribe(out, new Callback<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        subscribed = true;
                        setObject();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        subscribed = false;
                        setObject();
                    }
                });

                waitObject(Timeout);
                if (!subscribed)
                    return;

                Looper.prepare();
                looper = Looper.myLooper();
                handler = new MessageHandler(looper);
                doReady();
                Looper.loop();

            } catch (InterruptedException ignore) {

            } finally {
                doDisconnected();
            }
        }
    }
}
