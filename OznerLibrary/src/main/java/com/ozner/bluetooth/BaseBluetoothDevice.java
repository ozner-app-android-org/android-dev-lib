package com.ozner.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.Time;

import com.ozner.bluetooth.BluetoothIO.BluetoothIOCallback;
import com.ozner.device.FirmwareTools;
import com.ozner.util.ByteUtil;
import com.ozner.util.dbg;

import org.apache.http.util.ByteArrayBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressLint({"NewApi", "HandlerLeak"})
public abstract class BaseBluetoothDevice extends BluetoothGattCallback implements BluetoothIOCallback {
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
    static final byte opCode_DeviceInfo = (byte) 0x15;
    static final byte opCode_DeviceInfoRet = (byte) 0xA5;
    static final byte opCode_UpdateTime = (byte) 0xF0;
    static final byte opCode_SetName = (byte) 0x80;
    static final byte opCode_SetBackgroundMode = (byte) 0x21;
    static final byte opCode_GetFirmware = (byte) 0x82;
    static final byte opCode_GetFirmwareRet = (byte) -126;
    static final byte opCode_ReadSensor = 0x12;
    protected boolean isUpdateFirmware = false;
    String Address = "";
    String Serial = "";
    String Model = "";
    String Platform = "";
    long Firmware = 0;
    Context mContext;
    BluetoothCloseCallback mCloseCallback = null;

    BluetoothIO mBluetoothIO = null;
    RunHandler mHandler = new RunHandler();
    boolean misBackground = false;

    public BaseBluetoothDevice(Context context, BluetoothDevice device, BluetoothCloseCallback callback) {
        super();
        Address = device.getAddress();
        mCloseCallback = callback;
        mContext = context;
    }

    public Context getContext() {
        return mContext;
    }

    public String getAddress() {
        return Address;
    }

    /**
     * 获取当前设备连接状态
     *
     * @return
     */
    public int getStatus() {
        return mBluetoothIO == null ? BluetoothProfile.STATE_DISCONNECTED : mBluetoothIO.getConnectStatus();
    }

    /**
     * 获取设备序列号
     *
     * @return
     */
    public String getSerial() {
        return Serial;
    }

    /**
     * 获取设备硬解平台
     *
     * @return
     */
    public String getPlatform() {
        return Platform;
    }

    /**
     * 获取设备固件版本日期
     *
     * @return
     */
    public long getFirmware() {
        return Firmware;
    }

    /**
     * 获取设备型号
     *
     * @return
     */
    public String getModel() {
        return Model;
    }

    /**
     * 获取底层蓝牙通讯设备实例
     *
     * @return
     */
    public BluetoothDevice getDevice() {
        return mBluetoothIO == null ? null : mBluetoothIO.getDevice();
    }

    protected void onReadly() {

    }

    public void sleep() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void ProcData(byte opCode, byte[] Data) {
        switch (opCode) {
            case opCode_DeviceInfoRet:
                Serial = new String(Data, 0, 10);
                Model = new String(Data, 11, 6);
                Firmware = ByteUtil.getShort(Data, 16);

                sendBroadcastDeviceInfo();

                dbg.d("收到数据:Serial:%s Model:%s Firmware:%s", Serial, Model, Firmware);
                break;
            case opCode_GetFirmwareRet:
                if (Data.length < 14) return;
                String temp = new String(Data, Charset.forName("US-ASCII"));
                try {
                    Platform = temp.substring(0, 2);
                    String mon = temp.substring(3, 6);
                    String day = temp.substring(6, 8);
                    String year = temp.substring(8, 12);
                    String hour = temp.substring(12, 14);
                    String min = temp.substring(14, 16);
                    String sec = temp.substring(16, 18);
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss", Locale.US);
                    Date date = df.parse(year + "-" + mon + "-" + day + " " + hour + ":" + min + ":" + sec);
                    Firmware = date.getTime();
                } catch (Exception e) {
                    dbg.e(e.toString());
                }
                break;
        }
    }

    public boolean sendOpCode(int opCode) {
        if (mBluetoothIO != null) {
            return mBluetoothIO.SendOpCode(opCode);
        } else
            return false;
    }

    public boolean isBusy() {
        return false;
    }

    public boolean send(byte opCode, byte[] data) {
        if (mBluetoothIO != null) {
            return mBluetoothIO.Send(opCode, data);
        } else
            return false;
    }

    public void connect() {
        if (mBluetoothIO != null) {
            mBluetoothIO.connect();
        }
    }

    public void updateBluetooth(BluetoothDevice device) {
        synchronized (this) {
            if (mBluetoothIO != null) {
                if (mBluetoothIO.getDevice() != device) {
                    mBluetoothIO.close();
                } else
                    return;
            }


            try {
                mBluetoothIO = new BluetoothIO(mContext, device, this);

            } catch (BluetoothIO.BluetoothException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 关闭连接
     */
    public void close() {
        mBluetoothIO.close();
        if (mCloseCallback != null) {
            mCloseCallback.OnOznerBluetoothClose(BaseBluetoothDevice.this);
        }
        mBluetoothIO = null;
    }

    private boolean sendTime() {
        if (mBluetoothIO == null) return false;
        Time time = new Time();
        time.setToNow();
        byte[] data = new byte[6];
        data[0] = (byte) (time.year - 2000);
        data[1] = (byte) (time.month + 1);
        data[2] = (byte) time.monthDay;
        data[3] = (byte) time.hour;
        data[4] = (byte) time.minute;
        data[5] = (byte) time.second;
        dbg.i(mBluetoothIO.getAddress() + " 同步时间", mContext);
        return mBluetoothIO.Send(opCode_UpdateTime, data);
    }

    /**
     * 设置设备名称
     *
     * @param name 设备名称
     * @return TRUE成功，FALSE失败
     */
    public boolean setName(String name) {
        if (mBluetoothIO == null) return false;
        byte[] data = new byte[18];
        for (int i = 0; i < 18; i++)
            data[i] = 0;
        byte[] buff = name.getBytes();
        System.arraycopy(buff, 0, data, 0, buff.length < 18 ? buff.length : 18);
        return mBluetoothIO.Send(opCode_SetName, data);
    }

    protected void onPause() {

    }


    @Override
    public void OnReadly(BluetoothIO bluetooth) {
        mHandler.sendEmptyMessage(RunHandler.Msg_Readly);
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTH_READLY);
        intent.putExtra("Address", bluetooth.getAddress());
        mContext.sendBroadcast(intent);

    }

    protected void sendBroadcastConnected() {
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTH_CONNECTED);
        intent.putExtra("Address", getAddress());
        mContext.sendBroadcast(intent);
    }


    protected void sendroadcastDisconnected() {
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTH_DISCONNECTED);
        intent.putExtra("Address", getAddress());
        mContext.sendBroadcast(intent);
    }


    protected void sendBroadcastDeviceInfo() {
        Intent intent = new Intent(ACTION_BLUETOOTH_DEVICE);
        intent.putExtra("Address", getAddress());
        intent.putExtra("Model", Model);
        intent.putExtra("Firmware", Firmware);
        mContext.sendBroadcast(intent);
    }

    @Override
    public void onConnected(BluetoothIO bluetooth) {
        sendBroadcastConnected();
    }

    @Override
    public void onDisConnected(BluetoothIO bluetooth) {
        sendroadcastDisconnected();
    }

    @Override
    public void OnData(BluetoothIO bluetooth, byte[] data) {
        Message m = new Message();
        m.what = RunHandler.Msg_Data;
        m.obj = data;
        mHandler.sendMessage(m);
    }

    @Override
    public void OnError(BluetoothIO bluetooth, String Message) {
        sendroadcastError(Message);
    }

    @Override
    public void onDisConnecting(BluetoothIO bluetooth) {

    }

    @Override
    public void onConnecting(BluetoothIO bluetooth) {

    }

    @Override
    public void onConnectFailure(BluetoothIO bluetooth) {
        sendroadcastError("Connect Failure");
    }

    /**
     * 获取设备名称
     *
     * @return
     */
    public String getName() {
        if (mBluetoothIO != null) return mBluetoothIO.getDevice().getName();
        else
            return "";
    }


    /**
     * 设置广播附加数据
     *
     * @param CustomType
     * @param data
     */
    public void updateCustomData(int CustomType, byte[] data) {

    }

    /**
     * 获取广播附加数据
     *
     * @return
     */
    public Object getCustomObject() {
        return null;
    }

    /**
     * 获取设备是否在配对模式
     *
     * @return TRUE配对模式，FALSE正常模式
     */
    public boolean isBindMode() {
        return false;
    }


    protected void sendroadcastError(String Message) {
        dbg.e(Message);
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTH_ERROR);
        intent.putExtra("Address", getAddress());
        intent.putExtra("Message", Message);
        mContext.sendBroadcast(intent);

    }

    /**
     * 设置设备前后台模式
     *
     * @param isBackground
     */
    public void setBackgroundMode(boolean isBackground) {
        if (misBackground == isBackground) return;
        this.misBackground = isBackground;

        updateBackgroundMode();

        //如果在后台模式,判断是否处于忙碌状态,如果是等待40秒关闭连接
        if (misBackground) {
            Handler handler = new Handler(mContext.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (misBackground)
                        close();
                }
            }, 40000);
        }
    }

    private void updateBackgroundMode() {
        if (mBluetoothIO != null) {
            if (mBluetoothIO.isReadly()) {
                dbg.i("发送0X21");
                mBluetoothIO.SendOpCode(opCode_SetBackgroundMode);
            }
        }
    }

    protected boolean isBackground() {
        return misBackground;
    }

    /**
     * 设备是否就绪
     *
     * @return TRUE=就绪可操作
     */
    public boolean isReadly() {
        return mBluetoothIO == null ? false : mBluetoothIO.isReadly();
    }

    public void updateInfo(String Model, String Platform, long Firmware) {
        this.Model = Model;
        this.Platform = Platform;
        this.Firmware = Firmware;
    }

    /**
     * 获取传感器
     *
     * @return
     */
    public Object getSensor() {
        return null;
    }

    /**
     * 必须异步调用
     */

    private boolean eraseRom() {
        if (!send((byte) 0xc0, new byte[]{0}))

        {
            return false;
        }
        sleep();
        sleep();
        if (!send((byte) 0xc0, new byte[]{1}))

        {
            return false;
        }
        sleep();
        sleep();

        return true;
//
//		send((byte) 0x0c, new byte[]{0});
//		try
//
//		{
//			mCheckObject.wait(10000);
//
//		}catch (InterruptedException ex)
//		{
//
//		}
//		catch ( IllegalMonitorStateException ex)
//		{
//			return false;
//		}
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                send((byte) 0x0c, new byte[]{0});
//            }
//        });
//        sleep();
//        sleep();
//
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                send((byte) 0x0c, new byte[]{1});
//            }
//        });
//        sleep();
//        sleep();
    }
    FirmwareTools firmware=null;
    public void udateFirmware(String file, FirmwareUpateInterface OnUpdateInterface) throws IOException, FirmwareTools.FirmwareExcpetion {
        if (isUpdateFirmware) return;
        firmware = new FirmwareTools(file, getAddress());

        final FirmwareUpateInterface callback = OnUpdateInterface;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                if (mBluetoothIO!=null)
                {
                    if (!mBluetoothIO.isReadly())
                    {
                        callback.onFirmwareFail(getAddress());
                        return;
                    }

                }else {
                    callback.onFirmwareFail(getAddress());
                    return;
                }
                isUpdateFirmware = true;
                try
                {
                    BluetoothStatusChecker.Busy(getAddress());

                    callback.onFirmwareUpdateStart(getAddress(), firmware.Size);
                    if (eraseRom()) {
                        for (int i = 0; i < firmware.Size; i += 16) {
                            byte[] data = new byte[19];
                            short p = (short) (i / 16);
                            ByteUtil.putShort(data, p, 0);
                            System.arraycopy(firmware.bytes, i, data, 2, 16);
                            if (!send((byte) 0xc1, data)) {
                                callback.onFirmwareFail(getAddress());
                                return;
                            } else
                                callback.onFirmwarePosition(getAddress(), i, firmware.Size);

                        }

                    }else
                    {
                        callback.onFirmwareFail(getAddress());
                        return;
                    }
                    byte[] data = new byte[19];
                    ByteUtil.putInt(data, firmware.Size, 0);
                    data[4] = 'S';
                    data[5] = 'U';
                    data[6] = 'M';
                    ByteUtil.putInt(data, firmware.Cheksum, 7);
                    if (send((byte) 0xc3, data)) {
                        callback.onFirmwareComplete(getAddress());
                    } else
                        callback.onFirmwareFail(getAddress());
                }finally {
                    isUpdateFirmware = false;
                    BluetoothStatusChecker.Idle(getAddress());
                }
            }
        });
        thread.start();
    }

    /**
     * 蓝牙设备关闭回调
     *
     * @author xzy
     */
    public interface BluetoothCloseCallback {
        void OnOznerBluetoothClose(BaseBluetoothDevice device);
    }

    public interface FirmwareUpateInterface {
        void onFirmwareUpdateStart(String Address,int size);

        void onFirmwarePosition(String Address, int Position,int Size);

        void onFirmwareComplete(String Address);

        void onFirmwareFail(String Address);
    }

    class RunHandler extends Handler {
        public static final int Msg_Data = 0x11;
        public static final int Msg_Readly = 0x12;

        public RunHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case Msg_Readly:
                    updateBackgroundMode();
                    onReadly();
                    break;

                case Msg_Data: {
                    byte[] src = (byte[]) msg.obj;
                    byte opCode = src[0];
                    int len = src.length - 1;
                    byte[] data = null;
                    if (len > 0) {
                        data = new byte[len];
                        System.arraycopy(src, 1, data, 0, len);
                    }
                    try {
                        ProcData(opCode, data);
                    } catch (Exception e) {
                        dbg.e("Data is Null");
                        dbg.e(e.toString());
                    }
                    break;
                }
            }
            super.dispatchMessage(msg);
        }
    }
}
