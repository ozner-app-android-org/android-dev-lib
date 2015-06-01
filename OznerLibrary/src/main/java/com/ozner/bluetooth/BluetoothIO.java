
package com.ozner.bluetooth;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.ozner.util.dbg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by xzy on 2015/6/1.
 */
public class BluetoothIO extends BluetoothGattCallback implements Runnable {
    private static final int ServiceId = 0xFFF0;
    final UUID Characteristic_Input = GetUUID(0xFFF2);
    final UUID Characteristic_Output = GetUUID(0xFFF1);
    final UUID GATT_CLIENT_CHAR_CFG_UUID = GetUUID(0x2902);
    BluetoothGattCharacteristic mInput = null;
    BluetoothGattCharacteristic mOutput = null;
    BluetoothGattService mService = null;

    static int getShotUUID(UUID id) {
        return (int) (id.getMostSignificantBits() >> 32);
    }

    static UUID GetUUID(int id) {
        return UUID.fromString(String.format(
                "%1$08x-0000-1000-8000-00805f9b34fb", id));
    }

    /**
     */
    boolean mReadly = false;
    /**
     * 连接中
     */
    public final static int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    /**
     * 已连接
     */
    public final static int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;
    /**
     * 连接断开
     */
    public final static int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    /**
     * 关闭中
     */
    public final static int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;


    static final int Write_Timeout = 2000;
    static final int Connect_Timeout = 60000;

    BluetoothDevice mDevice = null;
    BluetoothGatt mGatt = null;
    Context mContext;
    Thread mRunThread = null;
    Looper mLooper = null;
    BluetoothIOCallback mCallback;
    private Object mLock = new Object();
    private int mConnectStatus = STATE_DISCONNECTING;
    private int mStatus = -1;

    public int getConnectStatus() {
        return mConnectStatus;
    }

    private void waitStatus(int timeout) {
        try {
            synchronized (mLock) {
                mStatus = -1;
                mLock.wait(timeout);
            }
        } catch (InterruptedException e) {
        }
    }

    private void setWait(int Status) {
        synchronized (mLock) {
            mStatus = Status;
            mLock.notifyAll();
        }
    }

    public interface BluetoothIOCallback {
        void onConnected(BluetoothIO bluetooth);

        void onConnecting(BluetoothIO bluetooth);

        void onDisConnecting(BluetoothIO bluetooth);

        void onDisConnected(BluetoothIO bluetooth);

        void onConnectFailure(BluetoothIO bluetooth);

        void OnData(BluetoothIO bluetooth, byte[] data);

        void OnReadly(BluetoothIO bluetooth);

        void OnError(BluetoothIO bluetooth, String Message);
    }

    protected Context getContext() {
        return mContext;
    }

    /**
     * 获取设备地址
     *
     * @return
     */
    public String getAddress() {
        return mDevice.getAddress();
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    @Override
    public void run() {
        synchronized (mLock) {
            Looper.prepare();
            mLooper = Looper.myLooper();
            mLock.notify();
        }
        Looper.loop();
    }

    public class BluetoothException extends Exception {

    }

    MessageHandler mHandler;

    class MessageHandler extends Handler {
        public static final int MSG_Connect = 0x11;
        public static final int MSG_Write = 0x12;
        public static final int MSG_InitService = 0x13;

        public MessageHandler() {
            super(mLooper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_Connect:
                    synchronized (BluetoothStatusChecker.getLockObject()) {
                        dbg.i("开始连接");
                        synchronized (this) {
                            BluetoothStatusChecker.Busy(getAddress());
                            try {
                                if (!mGatt.connect()) {
                                    dbg.e("连接错误");
                                    close();
                                    return;
                                } else {
                                    waitStatus(Connect_Timeout);
                                    if (mGatt == null) return;
                                    if (!mReadly) {
                                        dbg.e("%s ConnectTimeout", getAddress());
                                        close();
                                    }
                                }
                            } finally {
                                BluetoothStatusChecker.Idle(getAddress());
                            }
                        }
                    }
                    break;
                case MSG_Write:
                    if (msg.obj == null) {
                        SendOpCode(msg.arg1);
                    } else
                        Send((byte) msg.arg1, (byte[]) msg.obj);
                    break;
                case MSG_InitService:
                    initService();
                    break;
            }
            super.handleMessage(msg);
        }
    }

    public boolean isReadly() {
        return mReadly;
    }

    public BluetoothIO(Context context, BluetoothDevice device, BluetoothIOCallback callback) throws BluetoothException {
        mCallback = callback;
        mContext = context;
        mDevice = device;
        mGatt = mDevice.connectGatt(mContext, false, this);
        if (mGatt == null) {
            throw new BluetoothException();
        }
        mRunThread = new Thread(this);
        mRunThread.start();
        synchronized (mLock) {
            while (mLooper == null) {
                try {
                    mLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        mHandler = new MessageHandler();

    }

    public void connect() {
        if (BluetoothStatusChecker.hashBluetoothBusy()) {
            return;
        }
        if (mStatus != STATE_DISCONNECTED) return;
        dbg.d("device:%s connect", getAddress());
        mStatus = STATE_CONNECTING;
        mGatt = mDevice.connectGatt(mContext, false, this);
        if (mGatt == null) {
            mStatus = STATE_DISCONNECTED;
            return;
        }
        mHandler.sendEmptyMessage(MessageHandler.MSG_Connect);
    }


    public void close() {
        if (mGatt != null) {
            mGatt.close();
            mGatt = null;
        }
        mLooper.quit();
        setWait(BluetoothGatt.GATT_FAILURE);
    }

    private void initService() {
        dbg.i("initService");
        if (mGatt == null) {
            dbg.i("mGatt is Null");
            return;
        }
        mService = mGatt.getService(GetUUID(ServiceId));
        if (mService != null) {
            mInput = mService.getCharacteristic(Characteristic_Input);
            if (mInput == null) return;
            mInput.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            mOutput = mService.getCharacteristic(Characteristic_Output);
            if (mOutput != null) {
                BluetoothGattDescriptor desc = mOutput
                        .getDescriptor(GATT_CLIENT_CHAR_CFG_UUID);
                if (desc != null) {
                    desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mGatt.setCharacteristicNotification(mOutput, true);
                    mGatt.writeDescriptor(desc);
                    dbg.i("writeDescriptor");
                } else {
                    close();
                }
            } else {
                close();
            }
        } else {
            dbg.i("start discoverServices");
            if (!mGatt.discoverServices()) {
                mCallback.onConnectFailure(this);
                close();
                dbg.e("discoverServices error");
            }
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                        int newState) {
        if (status == BluetoothGatt.GATT_FAILURE) {
            dbg.e("%s onConnectionStateFailure:%d", getAddress(), newState);
            mCallback.onConnectFailure(this);
            mCallback.OnError(this, String.format("onConnectionStateFailure:%d", newState));
            return;
        }

        super.onConnectionStateChange(gatt, status, newState);

        mConnectStatus = newState;
        dbg.i("%s onConnectionStateChange:%d", getAddress(), newState);
        switch (newState) {
            case BluetoothProfile.STATE_CONNECTED: {
                mReadly = false;
                mCallback.onConnected(this);
                mHandler.sendEmptyMessage(MessageHandler.MSG_InitService);

                break;
            }
            case BluetoothProfile.STATE_CONNECTING:
                mCallback.onDisConnecting(this);
                break;

            case BluetoothProfile.STATE_DISCONNECTING: {
                mCallback.onDisConnecting(this);
                break;
            }
            case BluetoothProfile.STATE_DISCONNECTED: {
                dbg.e("%s onDisconnected:%d", getAddress(), status);
                mCallback.onDisConnected(this);
                close();
                break;
            }
        }

    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
        synchronized (this) {
            dbg.d("onCharacteristicChanged:%d", characteristic.getValue().length);
            if (characteristic.getUuid().equals(Characteristic_Output)) {
                mCallback.OnData(this, characteristic.getValue());
            }
        }
        super.onCharacteristicChanged(gatt, characteristic);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        setWait(status);
        if (status != BluetoothGatt.GATT_SUCCESS) {
            mCallback.OnError(this, String.format("onCharacteristicWrite:%d", status));
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt,
                                  BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            mReadly = true;
            setWait(status);
            dbg.i(String.format("设备连接就绪:%s", getAddress()));
            mCallback.OnReadly(this);
        } else {
            mCallback.OnError(this, String.format("onCharacteristicWrite:%d", status));

            mCallback.onConnectFailure(this);
            close();
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (status == BluetoothGatt.GATT_SUCCESS)
            mHandler.sendEmptyMessage(MessageHandler.MSG_InitService);
        else {
            mCallback.OnError(this, String.format("onCharacteristicWrite:%d", status));
            mCallback.onConnectFailure(this);
            close();
        }
    }

    private boolean writeCharacteristic() {
        synchronized (BluetoothStatusChecker.getLockObject()) {
            if (mGatt.writeCharacteristic(mInput)) {
                waitStatus(Write_Timeout);
                if (mStatus != BluetoothGatt.GATT_SUCCESS) {
                    return false;
                } else
                    return true;
            } else
                return false;
        }
    }

    public void SendAsync(byte opCode, byte[] data) {
        Message m = new Message();
        m.arg1 = opCode;
        m.obj = data;
        m.what = MessageHandler.MSG_Write;
        mHandler.sendMessage(m);
    }

    public boolean Send(byte opCode, byte[] data) {
        synchronized (this) {
            if (mGatt == null) return false;
            if (mInput != null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(20);
                try {
                    try {
                        out.write(opCode);
                        if (data != null)
                            out.write(data);
                        mInput.setValue(out.toByteArray());
                    } finally {
                        out.close();
                    }
                } catch (IOException e) {
                    return false;
                }
                try {
                    return writeCharacteristic();
                } catch (Exception e) {
                    close();
                }
            }
            return false;
        }
    }

    public boolean SendOpCode(int opCode) {
        synchronized (this) {
            if (mGatt == null) return false;
            mInput.setValue(opCode, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            try {
                return writeCharacteristic();
            } catch (Exception e) {
                close();
            }
            return false;
        }
    }

}
