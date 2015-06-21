package com.ozner.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.text.format.Time;

import com.ozner.util.dbg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by zhiyongxu on 15/6/11.
 */
public abstract class BluetoothIO extends BluetoothGattCallback implements Runnable {
    Object waitObject = new Object();
    Object NotifyObject = new Object();

    Context context;
    BluetoothDevice device;
    boolean isQuit = false;
    Thread thread = null;
    BluetoothGattCharacteristic mInput = null;
    BluetoothGattCharacteristic mOutput = null;
    BluetoothGattService mService = null;
    boolean isBindMode = false;
    boolean setChanged = false;

    protected BluetoothGattCharacteristic getOutput() {
        return mOutput;
    }

    String Model = "";
    String Platform = "";
    long Firmware = 0;
    BluetoothCloseCallback closeCallback=null;
    /**
     * 蓝牙设备关闭回调
     *
     * @author xzy
     *
     */
    public interface BluetoothCloseCallback {
        void OnOznerBluetoothClose(BluetoothIO device);
    }
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
     * 获取到设备信息广播
     */
    public final static String ACTION_BLUETOOTH_DEVICE = "com.ozner.bluetooth.device";


    private static final int ServiceId = 0xFFF0;
    final UUID Characteristic_Input = GetUUID(0xFFF2);
    final UUID Characteristic_Output = GetUUID(0xFFF1);
    final UUID GATT_CLIENT_CHAR_CFG_UUID = GetUUID(0x2902);
    static final byte opCode_UpdateTime = (byte) 0xF0;
    static final byte opCode_FrontMode = (byte) 0x21;
    static final byte opCode_DeviceInfo = (byte) 0x15;
    static final byte opCode_DeviceInfoRet = (byte) 0xA5;
    static final byte opCode_SetName = (byte) 0x80;
    static final byte opCode_SetBackgroundMode = (byte) 0x21;
    static final byte opCode_GetFirmware = (byte) 0x82;
    static final byte opCode_GetFirmwareRet = (byte) -126;

    private ArrayList<byte[]> recvPackets = new ArrayList<>();
    boolean Background = true;

    protected int getRecvPacketCount() {
        synchronized (recvPackets) {
            return recvPackets.size();
        }
    }

    protected byte[] popRecvPacket() {
        synchronized (recvPackets) {
            if (recvPackets.size() > 0) {
                byte[] data = recvPackets.get(0);
                recvPackets.remove(0);
                return data;
            } else
                return null;
        }
    }

    static UUID GetUUID(int id) {
        return UUID.fromString(String.format(
                "%1$08x-0000-1000-8000-00805f9b34fb", id));
    }

    protected void waitNotfify(int time) throws InterruptedException {
        synchronized (NotifyObject) {
            NotifyObject.wait(time);
        }
    }

    protected void setNotify() {
        synchronized (NotifyObject) {
            NotifyObject.notify();
        }
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

    public class BlueDeviceNotReadlyException extends Exception
    {

    }
    public String getName()
    {
        return device.getName();
    }

    public void start() throws BlueDeviceNotReadlyException {
        if (BluetoothSynchronizedObject.hashBluetoothBusy()) throw new BlueDeviceNotReadlyException();
        if (isRuning())
        {
            throw new BlueDeviceNotReadlyException();
        }
        thread = new Thread(this);
        thread.start();
    }

    public BluetoothIO(Context context,BluetoothCloseCallback callback,
                       BluetoothDevice device, String Platform, String Model, long Firmware) {
        this.closeCallback=callback;
        this.Model = Model;
        this.Platform = Platform;
        this.Firmware = Firmware;
        this.context = context;
        this.device = device;
    }

    public boolean isRuning() {
        return thread != null && thread.isAlive();
    }

    public void close() {
        if (isRuning()) {
            isQuit = true;
            thread.interrupt();
            thread = null;
        }
    }

    private int connectionState = BluetoothGatt.STATE_DISCONNECTED;
    private int lastStatus = BluetoothGatt.GATT_FAILURE;

    private boolean checkStatus() {
        return (connectionState == BluetoothGatt.STATE_CONNECTED) && (lastStatus == BluetoothGatt.GATT_SUCCESS);
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public Context getContext() {
        return context;
    }


    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        connectionState = newState;
        lastStatus = status;
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            sendBroadcastConnected();
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED)
            sendroadcastDisconnected();
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
        synchronized (recvPackets) {
            recvPackets.add(characteristic.getValue());
        }
        setNotify();
        super.onCharacteristicChanged(gatt, characteristic);
    }

    protected boolean send(BluetoothGatt gatt, byte opCode, byte[] data) throws InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(20);
        try {
            try {
                out.write(opCode);
                if (data != null)
                    out.write(data);
                mInput.setValue(out.toByteArray());
                synchronized (BluetoothSynchronizedObject.getLockObject()) {
                    if (!gatt.writeCharacteristic(mInput)) {
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

    protected boolean sendOpCode(BluetoothGatt gatt, int opCode) throws InterruptedException {
        mInput.setValue(opCode, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        synchronized (BluetoothSynchronizedObject.getLockObject()) {
            if (!gatt.writeCharacteristic(mInput)) {
                return false;
            }
        }
        wait(10000);
        return checkStatus();
    }

    private boolean sendTime(BluetoothGatt gatt) throws InterruptedException {
        dbg.i("开始设置时间:%s", device.getAddress());

        Time time = new Time();
        time.setToNow();
        byte[] data = new byte[6];
        data[0] = (byte) (time.year - 2000);
        data[1] = (byte) (time.month + 1);
        data[2] = (byte) time.monthDay;
        data[3] = (byte) time.hour;
        data[4] = (byte) time.minute;
        data[5] = (byte) time.second;
        return send(gatt, opCode_UpdateTime, data);
    }

    /**
     * 设置设备前后台模式
     *
     * @param isBackground
     */
    public void setBackgroundMode(boolean isBackground) {
        this.Background = isBackground;
    }

    protected void OnReadly() {

    }

    @Override
    public void run() {
        BluetoothGatt gatt = device.connectGatt(context, false, this);
        try {
            BluetoothSynchronizedObject.Busy(device.getAddress());
            synchronized (BluetoothSynchronizedObject.getLockObject()) {
                if (connect(gatt)) {
                    dbg.i("连接成功:%s", device.getAddress());
                } else {
                    dbg.w("连接失败:%s", device.getAddress());
                    return;
                }
                if (discoverServices(gatt)) {
                    dbg.i("发现成功:%s", device.getAddress());
                } else {
                    dbg.w("发现失败:%s", device.getAddress());
                    return;
                }
                if (setNotification(gatt)) {
                    dbg.i("通知设置成功:%s", device.getAddress());
                } else {
                    dbg.w("通知设置失败:%s", device.getAddress());
                    return;
                }

                if (sendTime(gatt)) {
                    dbg.i("时间设置成功:%s", device.getAddress());
                } else {
                    dbg.w("时间设置失败:%s", device.getAddress());
                    return;
                }

                Thread.sleep(100);
                if (sendSetting(gatt)) {
                    dbg.i("发送设置成功:%s", device.getAddress());
                } else {
                    dbg.w("发送设置失败:%s", device.getAddress());
                    return;
                }
                Thread.sleep(100);
                if (Background) {
                    if (sendOpCode(gatt, opCode_FrontMode)) {
                        dbg.i("前台设置成功:%s", device.getAddress());
                    } else {
                        dbg.w("前台设置失败:%s", device.getAddress());
                        return;
                    }
                }
                Thread.sleep(100);
                OnReadly();
            }
            sendBroadcastReadly();
            dbg.i("初始化成功:%s", device.getAddress());
            getFirmware(gatt);
            onRequest(gatt);
            Date lastRequestTime=new Date();
            while (!Background) {
                if (connectionState!=BluetoothGatt.STATE_CONNECTED) break;
                if (setChanged)
                {
                    sendSetting(gatt);
                }
                thread.sleep(500);
                Date dt=new Date();
                if ((dt.getTime()-lastRequestTime.getTime())>5000) {
                    lastRequestTime = new Date();
                }
                onRequest(gatt);

            }
        } catch (InterruptedException ignore) {
            return;
        } finally {
            if (closeCallback != null) {
                closeCallback.OnOznerBluetoothClose(this);
            }
            BluetoothSynchronizedObject.Idle(device.getAddress());
            //gatt.disconnect();
            gatt.close();
        }
    }

    private boolean setNotification(BluetoothGatt gatt) throws InterruptedException {
        dbg.i("开始设置通知:%s", device.getAddress());

        BluetoothGattDescriptor desc = mOutput.getDescriptor(GATT_CLIENT_CHAR_CFG_UUID);
        if (desc != null) {
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.setCharacteristicNotification(mOutput, true);
            if (!gatt.writeDescriptor(desc)) {
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

    private boolean discoverServices(BluetoothGatt gatt) throws InterruptedException {
        dbg.i("开始发现:%s", device.getAddress());

        if (gatt.discoverServices()) {
            wait(10000);
            if ((mInput == null) || (mOutput == null) || (!checkStatus())) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    private boolean connect(BluetoothGatt gatt) throws InterruptedException {
        dbg.i("开始连接:%s", device.getAddress());
        if (gatt.connect()) {
            wait(10000);
            if (connectionState != BluetoothGatt.STATE_CONNECTED) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    protected boolean getFirmware(BluetoothGatt gatt) throws InterruptedException {
        if (sendOpCode(gatt, (int) opCode_GetFirmware)) {
            waitNotfify(1000);
            byte[] data = popRecvPacket();
            if (data[0] == opCode_GetFirmwareRet) {
                if (data.length < 14) return false;
                String temp = new String(data, 1, 18, Charset.forName("US-ASCII"));
                Platform = temp.substring(0, 3);
                String mon = temp.substring(3, 6);
                String day = temp.substring(6, 8);
                String year = temp.substring(8, 12);
                String hour = temp.substring(12, 14);
                String min = temp.substring(14, 16);
                String sec = temp.substring(16, 18);
                try {
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss", Locale.US);
                    Date date = df.parse(year + "-" + mon + "-" + day + " " + hour + ":" + min + ":" + sec);
                    Firmware = date.getTime();
                } catch (ParseException e) {
                    return false;
                }
                return true;

            } else
                return false;
        } else
            return false;
    }

    protected void sendroadcastDisconnected() {
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTH_DISCONNECTED);
        intent.putExtra("Address", device.getAddress());
        context.sendBroadcast(intent);
    }

    protected void sendBroadcastConnected() {
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTH_CONNECTED);
        intent.putExtra("Address", device.getAddress());
        context.sendBroadcast(intent);
    }

    protected void sendBroadcastReadly() {
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTH_READLY);
        intent.putExtra("Address", device.getAddress());
        context.sendBroadcast(intent);
    }

    protected void sendBroadcastDeviceInfo() {
        Intent intent = new Intent(ACTION_BLUETOOTH_DEVICE);
        intent.putExtra("Address", device.getAddress());
        //intent.putExtra("Model", Model);
        //intent.putExtra("Firmware", Firmware);
        context.sendBroadcast(intent);
    }

    public String getAddress() {
        return device.getAddress();
    }


    protected abstract void onRequest(BluetoothGatt gatt) throws InterruptedException;


    protected void setBindMode(boolean bindMode) {
        isBindMode = bindMode;
    }

    public boolean isBindMode() {
        return this.isBindMode;
    }

    public Object getCustomObject() {
        return null;
    }

    public void updateCustomData(int CustomType, byte[] data) {

    }

    public String getSerial() {
        return getDevice().getAddress();
    }

    public String getModel() {
        return this.Model;
    }
    public String getPlatform() {return this.Platform;}
    public long getFirmware() {
        return this.Firmware;
    }

    public void SetChanged() {
        setChanged = true;
    }
    public int getStatus(){return connectionState;}
    protected boolean sendSetting(BluetoothGatt gatt) throws InterruptedException
    {
        setChanged=false;
        return true;
    }



}
