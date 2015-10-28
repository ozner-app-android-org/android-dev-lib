package com.ozner.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.ozner.device.BaseDeviceIO;
import com.ozner.device.DeviceNotReadlyException;
import com.ozner.util.dbg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by zhiyongxu on 15/10/28.
 */
public abstract class BluetoothIO extends BaseDeviceIO {
    /**
     * 设备连接成功广播
     */
    public final static String ACTION_BLUETOOTH_CONNECTED = "com.ozner.bluetooth.connected";
    public final static String ACTION_BLUETOOTH_ERROR = "com.ozner.bluetooth.error";

    /**
     * 设备就绪广播
     */
    public final static String ACTION_BLUETOOTH_READLY = "com.ozner.bluetooth.readly";
    /**
     * 设备连接断开广播
     */
    public final static String ACTION_BLUETOOTH_DISCONNECTED = "com.ozner.bluetooth.disconnected";
    /**
     * 连接中
     */
    public final static int STATE_CONNECTING = BluetoothGatt.STATE_CONNECTING;
    /**
     * 已连接
     */
    public final static int STATE_CONNECTED = BluetoothGatt.STATE_CONNECTED;
    /**
     * 连接断开
     */
    public final static int STATE_DISCONNECTED = BluetoothGatt.STATE_DISCONNECTED;
    /**
     * 关闭中
     */
    public final static int STATE_DISCONNECTING = BluetoothGatt.STATE_DISCONNECTING;

    String Model = "";
    String Platform = "";
    long Firmware = 0;

    /**
     * 获取最后一次收到的数据包
     */
    public byte[] getLastRecvPacket() {
        return bluetoothProxy.lastRecvPacket;
    }

    public String Model() {
        return this.Model;
    }

    public String Platform() {
        return Platform;
    }

    public long Firmware() {
        return Firmware;
    }

    public static byte[] makePacket(byte opCode, byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(data == null ? 1 : data.length + 1);
        buffer.put(opCode);
        if (data != null) {
            buffer.put(data);
        }
        return buffer.array();
    }

    Context context;
    BluetoothDevice device;
    BluetoothProxy bluetoothProxy;

    public BluetoothIO(Context context, BluetoothDevice device,
                       String Platform, String Model, long Firmware) {
        this.context = context;
        this.device = device;
        this.Platform = Platform;
        this.Firmware = Firmware;
        this.Model = Model;
        bluetoothProxy = new BluetoothProxy();
    }


    @Override
    public boolean send(byte[] bytes) {
        return bluetoothProxy.postSend(bytes);
    }

    /**
     * 设置一个循环发送runnable,来执行发送大数据包,比如挂件升级过程
     *
     * @param runnable
     * @return
     */
    public boolean post(BluetoothRunnable runnable) {
        return bluetoothProxy.postRunable(runnable);
    }

    @Override
    public void close() {
        bluetoothProxy.close();
    }

    @Override
    public void open() throws DeviceNotReadlyException {
        bluetoothProxy.start();
    }

    @Override
    public String getAddress() {
        return device.getAddress();
    }

    @Override
    public boolean connected() {
        return bluetoothProxy.connectionState == STATE_CONNECTED;
    }


    public interface BluetoothRunnable {
        void run(DataSendProxy sendHandle);
    }

    public class BluetoothSendProxy extends DataSendProxy {
        @Override
        public boolean send(byte[] data) {
            Looper looper = Looper.myLooper();
            if (!bluetoothProxy.mLooper.equals(looper)) {
                return false;
            }
            try {
                return bluetoothProxy.send(data);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    @Override
    protected void doConnected() {
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTH_CONNECTED);
        intent.putExtra("Address", device.getAddress());
        context.sendBroadcast(intent);
        super.doConnected();
    }

    @Override
    protected void doReadly() {

        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTH_READLY);
        intent.putExtra("Address", device.getAddress());
        context.sendBroadcast(intent);
        super.doReadly();
    }

    @Override
    protected void doDisconnected() {
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTH_DISCONNECTED);
        intent.putExtra("Address", device.getAddress());
        context.sendBroadcast(intent);
        super.doDisconnected();
    }

    private class BluetoothProxy extends BluetoothGattCallback implements Runnable {
        final static int MSG_SendData = 0x1000;
        final static int MSG_Runnable = 0x2000;
        private static final int ServiceId = 0xFFF0;
        final UUID Characteristic_Input = GetUUID(0xFFF2);
        final UUID Characteristic_Output = GetUUID(0xFFF1);
        final UUID GATT_CLIENT_CHAR_CFG_UUID = GetUUID(0x2902);
        Object waitObject = new Object();
        Thread thread = null;
        BluetoothGattCharacteristic mInput = null;
        BluetoothGattCharacteristic mOutput = null;
        BluetoothGattService mService = null;
        BluetoothGatt mGatt = null;
        Looper mLooper;
        MessageHandler mHandler;
        byte[] lastRecvPacket = null;
        private int connectionState = BluetoothGatt.STATE_DISCONNECTED;
        private int lastStatus = BluetoothGatt.GATT_FAILURE;

        private UUID GetUUID(int id) {
            return UUID.fromString(String.format(
                    "%1$08x-0000-1000-8000-00805f9b34fb", id));
        }


        private void wait(int time) throws InterruptedException {
            synchronized (waitObject) {
                waitObject.wait(time);
            }
        }

        private void set() {
            synchronized (waitObject) {
                waitObject.notify();
            }
        }

        private boolean checkStatus() {
            return (connectionState == BluetoothGatt.STATE_CONNECTED) && (lastStatus == BluetoothGatt.GATT_SUCCESS);
        }


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            connectionState = newState;
            lastStatus = status;
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                doConnected();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED)
                doDisconnected();
            set();
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            lastStatus = status;
            mService = gatt.getService(GetUUID(ServiceId));
            if (mService != null) {
                mInput = mService.getCharacteristic(Characteristic_Input);
                mOutput = mService.getCharacteristic(Characteristic_Output);
                mInput.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

            }
            set();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (descriptor.getUuid().equals(GATT_CLIENT_CHAR_CFG_UUID)) {
                lastStatus = status;
                set();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            lastStatus = status;
            set();
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            lastRecvPacket = characteristic.getValue();
            doRecvPacket(lastRecvPacket);
            super.onCharacteristicChanged(gatt, characteristic);
        }


        private boolean connect() throws InterruptedException {
            dbg.i("开始连接:%s", device.getAddress());
            if (mGatt.connect()) {
                wait(10000);
                if (connectionState != BluetoothGatt.STATE_CONNECTED) {
                    return false;
                }
            } else {
                return false;
            }
            return true;
        }

        private boolean discoverServices() throws InterruptedException {
            dbg.i("开始发现:%s", device.getAddress());

            if (mGatt.discoverServices()) {
                wait(10000);
                if ((mInput == null) || (mOutput == null) || (!checkStatus())) {
                    return false;
                }
            } else {
                return false;
            }
            return true;
        }

        private boolean setNotification() throws InterruptedException {
            dbg.i("开始设置通知:%s", device.getAddress());
            BluetoothGattDescriptor desc = mOutput.getDescriptor(GATT_CLIENT_CHAR_CFG_UUID);
            if (desc != null) {
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mGatt.setCharacteristicNotification(mOutput, true);
                if (!mGatt.writeDescriptor(desc)) {
                    return false;
                }
                wait(10000);
                if (!checkStatus()) {
                    return false;
                }
            } else {
                return false;
            }
            return true;
        }


        public boolean isRuning() {
            return thread != null && thread.isAlive();
        }

        public void start() throws DeviceNotReadlyException {
            if (BluetoothSynchronizedObject.hashBluetoothBusy(device.getAddress()))
                throw new DeviceNotReadlyException();
            if (isRuning()) {
                throw new DeviceNotReadlyException();
            }
            thread = new Thread(this);
            thread.start();
        }

        public void close() {
            if (isRuning()) {
                mLooper.quit();
            }
        }

        /**
         * 主运行循环
         */
        @Override
        public void run() {
            if (BluetoothSynchronizedObject.hashBluetoothBusy(device.getAddress())) return;
            mGatt = device.connectGatt(context, false, this);
            try {
                BluetoothSynchronizedObject.Busy(device.getAddress());
                synchronized (BluetoothSynchronizedObject.getLockObject()) {
                    if (connect()) {
                        dbg.i("连接成功:%s", device.getAddress());
                    } else {
                        dbg.w("连接失败:%s", device.getAddress());
                        return;
                    }
                    if (discoverServices()) {
                        dbg.i("发现成功:%s", device.getAddress());
                    } else {
                        dbg.w("发现失败:%s", device.getAddress());
                        return;
                    }

                    if (setNotification()) {
                        dbg.i("通知设置成功:%s", device.getAddress());
                    } else {
                        dbg.w("通知设置失败:%s", device.getAddress());
                        return;
                    }
                    Thread.sleep(100);
                    if (!doInit(new BluetoothSendProxy())) {
                        return;
                    }
                }
                doReadly();
                dbg.i("初始化成功:%s", device.getAddress());
                //连接完成以后建立一个HANDLE来接受发送的数据
                Looper.prepare();
                mLooper = Looper.myLooper();
                mHandler = new MessageHandler(mLooper);
                Looper.loop();

            } catch (InterruptedException ignore) {
                dbg.i("线程关闭:" + getAddress());
                ignore.printStackTrace();
                return;
            } finally {
                doClose();
                dbg.i("连接关闭:" + getAddress());
                BluetoothSynchronizedObject.Idle(device.getAddress());
                mGatt.close();
                mGatt = null;
            }
        }

        protected boolean send(byte[] data) throws InterruptedException {
            if (data == null) return false;
            if (mGatt == null) return false;
            ByteArrayOutputStream out = new ByteArrayOutputStream(20);
            try {
                try {
                    if (data != null)
                        out.write(data);
                    out.flush();
                    mInput.setValue(out.toByteArray());
                    synchronized (BluetoothSynchronizedObject.getLockObject()) {
                        if (!mGatt.writeCharacteristic(mInput)) {
                            return false;
                        }
                    }
                    wait(10000);
                    return checkStatus();
                } finally {
                    out.close();
                }
            } catch (IOException e) {
                return false;
            }
        }

        /**
         * 设置一个循环发送runnable,来执行发送大数据包,比如挂件升级过程
         *
         * @param runnable
         * @return
         */
        public boolean postRunable(BluetoothRunnable runnable) {
            if (mHandler == null) return false;
            Message message = new Message();
            message.what = MSG_Runnable;
            message.obj = runnable;
            return mHandler.sendMessage(message);
        }

        public boolean postSend(byte[] data) {
            if (mHandler == null) return false;
            Message message = new Message();
            message.what = MSG_SendData;
            message.obj = data;
            return mHandler.sendMessage(message);
        }

        private class MessageHandler extends Handler {
            public MessageHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                try {
                    if (msg.what == MSG_SendData) {
                        send((byte[]) msg.obj);
                    } else if (msg.what == MSG_Runnable) {
                        BluetoothRunnable runable = (BluetoothRunnable) msg.obj;
                        runable.run(new BluetoothSendProxy());
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                super.handleMessage(msg);
            }
        }


    }
}
