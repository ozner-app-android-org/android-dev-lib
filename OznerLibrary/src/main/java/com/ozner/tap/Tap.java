package com.ozner.tap;

import android.content.Intent;
import android.text.format.Time;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.cup.CupSensor;
import com.ozner.cup.CupSetting;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.DeviceSetting;
import com.ozner.device.OznerContext;
import com.ozner.device.OznerDevice;
import com.ozner.util.SQLiteDB;
import com.ozner.util.dbg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by zhiyongxu on 15/10/28.
 */
public class Tap extends OznerDevice implements
        BluetoothIO.OnInitCallback,
        BluetoothIO.OnRecvPacketCallback,
        BluetoothIO.StatusCallback {
    /**
     * 收到传感器数据
     */
    public final static String ACTION_BLUETOOTHTAP_SENSOR = "com.ozner.tap.bluetooth.sensor";
    /**
     * 收到单条饮水记录
     */
    public final static String ACTION_BLUETOOTHTAP_RECORD = "com.ozner.tap.bluetooth.record";
    final static String ACTION_BLUETOOTHTAP_RECORD_RECV_COMPLETE = "com.ozner.tap.bluetooth.record.recv.complete";
    /**
     * 水探头连接成功
     */
    public final static String ACTION_BLUETOOTHTAP_CONNECTED = "com.ozner.tap.bluetooth.connected";
    /**
     * 水探头连接断开
     */
    public final static String ACTION_BLUETOOTHTAP_DISCONNECTED = "com.ozner.tap.bluetooth.disconnected";

    /**
     * 水探头自动监测记录接收完成
     */
    public final static String ACTION_BLUETOOTHTAP_RECORD_COMPLETE = "com.ozner.tap.bluetooth.record.complete";

    static final byte opCode_ReadSensor = 0x12;
    static final byte opCode_ReadSensorRet = (byte) 0xA2;

    static final byte opCode_ReadTDSRecord = 0x17;
    static final byte opCode_ReadTDSRecordRet = (byte) 0xA7;
    static final byte opCode_GetFirmwareSum = (byte) 0xc5;
    static final byte opCode_GetFirmwareSumRet = (byte) 0xc5;

    static final byte opCode_SetDetectTime = 0x10;
    static final byte opCode_UpdateTime = (byte) 0xF0;
    static final byte opCode_FrontMode = (byte) 0x21;
    static final byte opCode_DeviceInfo = (byte) 0x15;
    static final byte opCode_DeviceInfoRet = (byte) 0xA5;
    static final byte opCode_SetName = (byte) 0x80;
    static final byte opCode_GetFirmware = (byte) 0x82;
    static final byte opCode_GetFirmwareRet = (byte) -126;


    CupSensor mSensor = new CupSensor();
    ArrayList<TapRecord> mRecords = new ArrayList<>();
    Date mLastDataTime = null;
    boolean updateSetting = false;

    public Tap(OznerContext context, String Address, String Serial, String Model, String Setting, SQLiteDB db) {
        super(context, Address, Serial, Model, Setting, db);
        initSetting(Setting);
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

    private boolean sendSetting(BaseDeviceIO.DataSendProxy proxy) {
        TapSetting setting = (TapSetting) Setting();
        if (setting == null)
            return false;
        byte[] data = new byte[16];

        if (setting.isDetectTime1()) {
            data[0] = (byte) (setting.DetectTime1() / 3600);
            data[1] = (byte) (setting.DetectTime1() % 3600 / 60);
            data[2] = (byte) (setting.DetectTime1() % 60);
            // ByteUtil.putInt(data, setting.DetectTime1(), 0);
        } else {
            data[0] = 0;
            data[1] = 0;
            data[2] = 0;
        }
        if (setting.isDetectTime2()) {
            data[3] = (byte) (setting.DetectTime2() / 3600);
            data[4] = (byte) (setting.DetectTime2() % 3600 / 60);
            data[5] = (byte) (setting.DetectTime2() % 60);
            // ByteUtil.putInt(data, setting.DetectTime1(), 0);
        } else {
            data[3] = 0;
            data[4] = 0;
            data[5] = 0;
        }

        if (setting.isDetectTime3()) {
            data[6] = (byte) (setting.DetectTime3() / 3600);
            data[7] = (byte) (setting.DetectTime3() % 3600 / 60);
            data[8] = (byte) (setting.DetectTime3() % 60);
            // ByteUtil.putInt(data, setting.DetectTime1(), 0);
        } else {
            data[6] = 0;
            data[7] = 0;
            data[8] = 0;
        }

        if (setting.isDetectTime4()) {
            data[9] = (byte) (setting.DetectTime4() / 3600);
            data[10] = (byte) (setting.DetectTime4() % 3600 / 60);
            data[11] = (byte) (setting.DetectTime4() % 60);
            // ByteUtil.putInt(data, setting.DetectTime1(), 0);
        } else {
            data[9] = 0;
            data[10] = 0;
            data[11] = 0;
        }
        if (proxy.send(BluetoothIO.makePacket(opCode_SetDetectTime, data))) {
            resetSettingUpdate();
            return true;
        } else {
            return false;
        }
    }


    Timer autoUpdateTimer = new Timer();

    @Override
    protected boolean Bind(BaseDeviceIO bluetooth) {
        if (bluetooth == this.Bluetooth()) return true;
        if (this.Bluetooth() != null) {
            Bluetooth().setOnInitCallback(null);
            Bluetooth().setRecvPacketCallback(null);
            Bluetooth().setStatusCallback(null);
        }

        if (bluetooth != null) {
            Bluetooth().setRecvPacketCallback(this);
            bluetooth.setOnInitCallback(this);
            bluetooth.setStatusCallback(this);
        }

        return super.Bind(bluetooth);
    }

    int RequestCount = 0;

    private void doTime() {
        if (mLastDataTime != null) {
            //如果上几次接收饮水记录的时间小于1秒,不进入定时循环,等待下条饮水记录
            Date dt = new Date();
            if ((dt.getTime() - mLastDataTime.getTime()) < 1000) {
                return;
            }
        }
        try {
            if ((RequestCount % 2) == 0) {
                requestRecord();
            } else {
                requestSensor();
            }
            RequestCount++;
        } catch (Exception e) {

        }
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
            if (Bluetooth().send(BluetoothIO.makePacket(opCode_ReadTDSRecord, null))) {
                mLastDataTime = null;
                synchronized (mRecords) {
                    mRecords.clear();
                }
            }
        }
    }


    HashSet<String> dataHash = new HashSet<>();

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
                Intent intent = new Intent(ACTION_BLUETOOTHTAP_SENSOR);
                intent.putExtra("Address", Bluetooth().getAddress());
                intent.putExtra("Sensor", data);
                getContext().sendBroadcast(intent);
                break;
            }

            case opCode_ReadTDSRecordRet: {
                if (data != null) {
                    TapRecord record = new TapRecord();
                    record.FromBytes(data);
                    if (record.TDS > 0) {
                        String hashKey = String.valueOf(record.time.getTime()) + "_" + String.valueOf(record.TDS);
                        if (dataHash.contains(hashKey)) {
                            dbg.e("收到水杯重复数据");
                            break;
                        } else
                            dataHash.add(hashKey);
                        synchronized (mRecords) {
                            mRecords.add(0, record);
                        }
                        Intent intent = new Intent(ACTION_BLUETOOTHTAP_RECORD);
                        intent.putExtra("Address", Bluetooth().getAddress());
                        intent.putExtra("Record", data);
                        getContext().sendBroadcast(intent);

                    }
                    mLastDataTime = new Date();
                    if (record.Index == 0) {
                        Intent comp_intent = new Intent(ACTION_BLUETOOTHTAP_RECORD_COMPLETE);
                        comp_intent.putExtra("Address", Bluetooth().getAddress());
                        getContext().sendBroadcast(comp_intent);
                    }
                }
                break;
            }
        }
    }


    @Override
    public void onConnected(BaseDeviceIO io) {
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTHTAP_CONNECTED);
        intent.putExtra("Address", Bluetooth().getAddress());
        getContext().sendBroadcast(intent);
    }

    @Override
    public void onDisconnected(BaseDeviceIO io) {
        autoUpdateTimer.cancel();
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTHTAP_DISCONNECTED);
        intent.putExtra("Address", Bluetooth().getAddress());
        getContext().sendBroadcast(intent);

    }

    @Override
    public void onReadly(BaseDeviceIO io) {
        autoUpdateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                doTime();
            }
        }, 1000);
    }
}
