package com.ozner.cup;

import android.content.Context;
import android.content.Intent;
import android.text.format.Time;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.DeviceNotReadlyException;
import com.ozner.device.DeviceSetting;
import com.ozner.device.OznerDevice;
import com.ozner.util.ByteUtil;
import com.ozner.util.dbg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 水杯对象
 * Created by zhiyongxu on 15/10/28.
 */
public class Cup extends OznerDevice implements
        BluetoothIO.OnInitCallback,
        BluetoothIO.OnRecvPacketCallback,
        BluetoothIO.StatusCallback {

    /**
     * 收到单条饮水记录
     */
    public final static String ACTION_BLUETOOTHCUP_RECORD = "com.ozner.cup.bluetooth.record";
    /**
     * 饮水记录传输完成
     */
    public final static String ACTION_BLUETOOTHCUP_RECORD_COMPLETE = "com.ozner.cup.bluetooth.record.complete";
    /**
     * 水杯连接成功
     */
    public final static String ACTION_BLUETOOTHCUP_CONNECTED = "com.ozner.cup.bluetooth.connected";

    /**
     * 水杯连接断开
     */
    public final static String ACTION_BLUETOOTHCUP_DISCONNECTED = "com.ozner.cup.bluetooth.disconnected";


    /**
     * 收到传感器信息
     */
    public final static String ACTION_BLUETOOTHCUP_SENSOR = "com.ozner.cup.bluetooth.sensor";
    public static final byte AD_CustomType_Gravity = 0x1;
    static final byte opCode_SetRemind = 0x11;
    static final byte opCode_ReadSensor = 0x12;
    static final byte opCode_ReadSensorRet = (byte) 0xA2;
    //    static final byte opCode_ReadGravity = 0x13;
//    static final byte opCode_ReadGravityRet = (byte) 0xA3;
    static final byte opCode_ReadRecord = 0x14;
    static final byte opCode_ReadRecordRet = (byte) 0xA4;
    static final byte opCode_UpdateTime = (byte) 0xF0;
    static final byte opCode_FrontMode = (byte) 0x21;
    final CupSensor mSensor = new CupSensor();
    final CupFirmwareTools firmwareTools = new CupFirmwareTools();
    final ArrayList<CupRecord> mRecords = new ArrayList<>();
    CupVolume mCupVolume;
    Date mLastDataTime = null;
    Timer autoUpdateTimer = new Timer();
    int RequestCount = 0;
    HashSet<String> dataHash = new HashSet<>();

    public Cup(Context context, String Address, String Model, String Setting) {
        super(context, Address, Model, Setting);
        initSetting(Setting);
        mCupVolume = new CupVolume(context, Address);
    }


    /**
     * 判断设备是否处于配对状态
     *
     * @param io 设备接口
     * @return true=配对状态
     */
    public static boolean isBindMode(BluetoothIO io) {
        if (!CupManager.IsCup(io.Model())) return false;
        if ((io.getCustomDataType() == AD_CustomType_Gravity) && (io.getCustomData() != null)) {
            CupGravity gravity = new CupGravity();
            gravity.FromBytes(io.getCustomData(), 0);
            return gravity.IsHandstand();
        }
        return false;
    }

    public CupSensor Sensor() {
        return mSensor;
    }

    public CupVolume Volume() {
        return mCupVolume;
    }

    public CupFirmwareTools firmwareTools() {
        return firmwareTools;
    }

    @Override
    protected DeviceSetting initSetting(String Setting) {
        DeviceSetting setting = new CupSetting();
        setting.load(Setting);
        return setting;
    }

    private boolean sendTime(BaseDeviceIO.DataSendProxy proxy) {
        dbg.i("开始设置时间:%s", Bluetooth().getAddress());
        Time time = new Time();
        time.setToNow();
        byte[] data = new byte[6];
        data[0] = (byte) (time.year - 2000);
        data[1] = (byte) (time.month + 1);
        data[2] = (byte) time.monthDay;
        data[3] = (byte) time.hour;
        data[4] = (byte) time.minute;
        data[5] = (byte) time.second;
        return proxy.send(BluetoothIO.makePacket(opCode_UpdateTime, data));
    }

    private void sendBackground(BaseDeviceIO.DataSendProxy proxy) {
        if (!isBackgroundMode()) {
            if (proxy != null) {
                proxy.send(BluetoothIO.makePacket(opCode_FrontMode, null));
            } else {
                send(opCode_FrontMode, null);
            }
        }
    }

    private boolean sendSetting(BaseDeviceIO.DataSendProxy proxy) {
        CupSetting setting = (CupSetting) Setting();
        if (setting == null)
            return false;
        byte[] data = new byte[19];
        ByteUtil.putInt(data, setting.remindStart(), 0);
        ByteUtil.putInt(data, setting.remindEnd(), 4);
        data[8] = (byte) setting.remindInterval();
        ByteUtil.putInt(data, setting.haloColor(), 9);
        data[13] = (byte) setting.haloMode();
        data[14] = (byte) setting.haloSpeed();
        data[15] = (byte) setting.haloConter();
        data[16] = (byte) setting.beepMode();
        data[17] = 0;// (byte) (isNewCup ? 1 : 0);
        data[18] = 0;
        dbg.i(Bluetooth().getAddress() + " 写入提醒数据", getContext());
        if (proxy != null) {
            if (proxy.send(BluetoothIO.makePacket(opCode_SetRemind, data))) {
                resetSettingUpdate();
                return true;
            } else {
                return false;
            }
        } else {
            if (send(opCode_SetRemind, data)) {
                resetSettingUpdate();
                return true;
            } else
                return false;
        }
    }

    private boolean send(byte opCode, byte[] data) {
        return Bluetooth() != null && Bluetooth().send(BluetoothIO.makePacket(opCode, data));
    }

    /**
     * 设置光环颜色
     *
     * @param Color 光环色值
     * @return True设置成功
     */
    public boolean changeHaloColor(int Color) {
        CupSetting setting = (CupSetting) Setting();
        if (setting == null)
            return false;
        byte[] data = new byte[19];
        ByteUtil.putInt(data, setting.remindStart(), 0);
        ByteUtil.putInt(data, setting.remindEnd(), 4);
        data[8] = (byte) setting.remindInterval();
        ByteUtil.putInt(data, Color, 9);
        data[13] = (byte) setting.haloMode();
        data[14] = (byte) setting.haloSpeed();
        data[15] = (byte) setting.haloConter();
        data[16] = (byte) setting.beepMode();
        data[17] = 0;// (byte) (isNewCup ? 1 : 0);
        data[18] = 0;
        return Bluetooth().send(BluetoothIO.makePacket(opCode_SetRemind, data));
    }

    @Override
    public boolean Bind(BaseDeviceIO bluetooth) throws DeviceNotReadlyException {
        if (bluetooth == this.Bluetooth()) return true;
        if (this.Bluetooth() != null) {
            Bluetooth().setOnInitCallback(null);
            Bluetooth().setRecvPacketCallback(null);
            Bluetooth().setStatusCallback(null);
            firmwareTools.bind(null);
        }

        if (bluetooth != null) {
            Bluetooth().setRecvPacketCallback(this);
            bluetooth.setOnInitCallback(this);
            bluetooth.setStatusCallback(this);
            firmwareTools.bind((BluetoothIO) bluetooth);
        }

        return super.Bind(bluetooth);
    }

    private void doTime() {
        if (mLastDataTime != null) {
            //如果上几次接收饮水记录的时间小于1秒,不进入定时循环,等待下条饮水记录
            Date dt = new Date();
            if ((dt.getTime() - mLastDataTime.getTime()) < 1000) {
                return;
            }
        }
        if ((RequestCount % 2) == 0) {
            requestRecord();
        } else {
            requestSensor();
        }
        RequestCount++;

    }

    @Override
    protected void resetSettingUpdate() {
        super.resetSettingUpdate();
        sendSetting(null);
    }

    @Override
    public boolean doInit(BaseDeviceIO.DataSendProxy sendHandle) {
        try {
            if (!sendTime(sendHandle))
                return false;
            Thread.sleep(100);

            if (!sendSetting(sendHandle))
                return false;

            Thread.sleep(100);
            sendBackground(sendHandle);
            Thread.sleep(100);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void requestSensor() {
        if (Bluetooth() != null) {
            Bluetooth().send(BluetoothIO.makePacket(opCode_ReadSensor, null));
        }
    }

    private void requestRecord() {
        if (Bluetooth() != null) {
            if (Bluetooth().send(BluetoothIO.makePacket(opCode_ReadRecord, null))) {
                mLastDataTime = null;
                synchronized (mRecords) {
                    mRecords.clear();
                }
            }
        }
    }

    @Override
    public void onRecvPacket(byte[] bytes) {
        if (bytes == null) return;
        if (bytes.length < 1) return;
        byte opCode = bytes[0];
        byte[] data = null;
        if (bytes.length > 1)
            data = Arrays.copyOfRange(bytes, 1, bytes.length);

        switch (opCode) {
            case opCode_ReadSensorRet: {
                dbg.i("读传感器完成");
                synchronized (this) {
                    mSensor.FromBytes(data, 0);
                }
                Intent intent = new Intent(ACTION_BLUETOOTHCUP_SENSOR);
                intent.putExtra("Address", Bluetooth().getAddress());
                intent.putExtra("Sensor", data);
                getContext().sendBroadcast(intent);
                break;
            }

            case opCode_ReadRecordRet: {
                if (data != null) {
                    CupRecord record = new CupRecord();
                    record.FromBytes(data);
                    if (record.Vol > 0) {
                        String hashKey = String.valueOf(record.time.getTime()) + "_" + String.valueOf(record.Vol);
                        if (dataHash.contains(hashKey)) {
                            dbg.e("收到水杯重复数据");
                            break;
                        } else
                            dataHash.add(hashKey);
                        synchronized (mRecords) {
                            mRecords.add(0, record);
                        }
                        Intent intent = new Intent(ACTION_BLUETOOTHCUP_RECORD);
                        intent.putExtra("Address", Bluetooth().getAddress());
                        intent.putExtra("Record", data);
                        getContext().sendBroadcast(intent);

                    }
                    mLastDataTime = new Date();
                    if (record.Index == 0) {
                        Intent comp_intent = new Intent(ACTION_BLUETOOTHCUP_RECORD_COMPLETE);
                        comp_intent.putExtra("Address", Bluetooth().getAddress());
                        getContext().sendBroadcast(comp_intent);
                        synchronized (mRecords) {
                            mCupVolume.addRecord(mRecords);
                        }

                    }
                }
                break;
            }
        }
    }

    @Override
    protected void doBackgroundModeChange() {
        if (!isBackgroundMode()) {
            sendBackground(null);
        }
    }


    @Override
    public void onConnected(BaseDeviceIO io) {
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTHCUP_CONNECTED);
        intent.putExtra("Address", Bluetooth().getAddress());
        getContext().sendBroadcast(intent);
    }

    @Override
    public void onDisconnected(BaseDeviceIO io) {
        autoUpdateTimer.cancel();
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTHCUP_DISCONNECTED);
        intent.putExtra("Address", Bluetooth().getAddress());
        getContext().sendBroadcast(intent);

    }

    @Override
    public void onReady(BaseDeviceIO io) {
        autoUpdateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                doTime();
            }
        }, 1000);
    }


}
