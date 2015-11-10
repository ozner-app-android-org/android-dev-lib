package com.ozner.cup;

import android.content.Context;
import android.content.Intent;
import android.text.format.Time;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.DeviceSetting;
import com.ozner.device.OznerDevice;
import com.ozner.oznerlibrary.R;
import com.ozner.util.ByteUtil;
import com.ozner.util.dbg;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

/**
 * 水杯对象
 * Created by zhiyongxu on 15/10/28.
 */
public class Cup extends OznerDevice {

    /**
     * 收到单条饮水记录
     */
    public final static String ACTION_BLUETOOTHCUP_RECORD = "com.ozner.cup.bluetooth.record";
    /**
     * 饮水记录传输完成
     */
    public final static String ACTION_BLUETOOTHCUP_RECORD_COMPLETE = "com.ozner.cup.bluetooth.record.complete";


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
    final TreeSet<CupRecord> mRecords = new TreeSet<>();
    final BluetoothIOImp bluetoothIOImp = new BluetoothIOImp();
    CupVolume mCupVolume;
    Date mLastDataTime = new Date();
    Timer autoUpdateTimer = null;
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
        if (!CupManager.IsCup(io.getModel())) return false;
        if ((io.getCustomDataType() == AD_CustomType_Gravity) && (io.getCustomData() != null)) {
            CupGravity gravity = new CupGravity();
            gravity.FromBytes(io.getCustomData(), 0);
            return gravity.IsHandstand();
        }
        return false;
    }


    @Override
    protected String getDefaultName() {
        return context().getString(R.string.cup_name);
    }


    @Override
    public Class<?> getIOType() {
        return BluetoothIO.class;
    }

    @Override
    protected void doSetDeviceIO(BaseDeviceIO oldIO, BaseDeviceIO newIO) {
        if (oldIO != null) {
            oldIO.setOnInitCallback(null);
            oldIO.unRegisterStatusCallback(bluetoothIOImp);
            newIO.setOnTransmissionsCallback(null);
            oldIO.setCheckTransmissionsCompleteCallback(null);
            firmwareTools.bind(null);
        }
        cancelTimer();
        if (newIO != null) {
            newIO.setOnTransmissionsCallback(bluetoothIOImp);
            newIO.setOnInitCallback(bluetoothIOImp);
            newIO.registerStatusCallback(bluetoothIOImp);
            newIO.setCheckTransmissionsCompleteCallback(bluetoothIOImp);
            firmwareTools.bind((BluetoothIO) newIO);
        }
    }

    public CupSetting Setting() {
        return (CupSetting) super.Setting();
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
        dbg.i("开始设置时间:%s", IO().getAddress());
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

    @Override
    protected void doChangeRunningMode() {
        sendBackground(null);
    }

    private void sendBackground(BaseDeviceIO.DataSendProxy proxy) {
        if (getRunningMode() == RunningMode.Foreground) {
            if (proxy != null) {
                proxy.send(BluetoothIO.makePacket(opCode_FrontMode, null));
            } else {
                send(opCode_FrontMode, null);
            }
        }
    }

    private boolean sendSetting(BaseDeviceIO.DataSendProxy proxy) {
        CupSetting setting = Setting();
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
        dbg.i(IO().getAddress() + " 写入提醒数据", context());
        if (proxy != null) {
            return proxy.send(BluetoothIO.makePacket(opCode_SetRemind, data));
        } else {
            return send(opCode_SetRemind, data);
        }
    }

    private boolean send(byte opCode, byte[] data) {
        return IO() != null && IO().send(BluetoothIO.makePacket(opCode, data));
    }

    /**
     * 设置光环颜色
     *
     * @param Color 光环色值
     * @return True设置成功
     */
    public boolean changeHaloColor(int Color) {
        CupSetting setting = Setting();
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
        return IO().send(BluetoothIO.makePacket(opCode_SetRemind, data));
    }

    private void doTime() {
        if (IO() == null) return;
        if (mLastDataTime != null) {
            //如果上几次接收饮水记录的时间小于2秒,不进入定时循环,等待下条饮水记录
            Date dt = new Date();
            if ((dt.getTime() - mLastDataTime.getTime()) < 2000) {
                return;
            }
        }
        if ((RequestCount % 2) == 0) {
            requestSensor();
        } else {
            requestRecord();
        }
        RequestCount++;

    }

    @Override
    public void UpdateSetting() {

        if ((IO() != null) && (IO().isReady()))
            sendSetting(null);

    }

    private void requestSensor() {
        if (IO() != null) {
            IO().send(BluetoothIO.makePacket(opCode_ReadSensor, null));
            dbg.i("请求传感器");
        }
    }

    private void requestRecord() {
        if (IO() != null) {
            if (IO().send(BluetoothIO.makePacket(opCode_ReadRecord, null))) {
                dbg.i("请求记录");
            }
        }
    }

    private void cancelTimer() {
        if (autoUpdateTimer != null) {
            autoUpdateTimer.cancel();
            autoUpdateTimer.purge();
            autoUpdateTimer = null;
        }
    }


    class BluetoothIOImp implements
            BluetoothIO.OnInitCallback,
            BluetoothIO.OnTransmissionsCallback,
            BluetoothIO.StatusCallback,
            BluetoothIO.CheckTransmissionsCompleteCallback {
        @Override
        public void onConnected(BaseDeviceIO io) {
        }

        @Override
        public void onDisconnected(BaseDeviceIO io) {
            cancelTimer();
        }

        @Override
        public void onReady(BaseDeviceIO io) {
            if (getRunningMode() == RunningMode.Foreground) {
                if (autoUpdateTimer != null)
                    cancelTimer();
                autoUpdateTimer = new Timer();
                autoUpdateTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        doTime();
                    }
                }, 100, 5000);
            } else {
                requestRecord();
            }
        }


        @Override
        public void onIOSend(byte[] bytes) {

        }

        @Override
        public void onIORecv(byte[] bytes) {
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
                    intent.putExtra("Address", IO().getAddress());
                    intent.putExtra("Sensor", data);
                    context().sendBroadcast(intent);
                    break;
                }

                case opCode_ReadRecordRet: {
                    if (data != null) {
                        CupRecord record = new CupRecord();
                        record.FromBytes(data);
                        if ((record.Index == record.Count) && (record.Count == 0) && (record.Vol == 0)) {
                            return;
                        }

                        if (record.Vol > 0) {
                            String hashKey = String.valueOf(record.time.getTime()) + "_" + String.valueOf(record.Vol);
                            synchronized (mRecords) {
                                if (dataHash.contains(hashKey)) {
                                    dbg.e("收到水杯重复数据");
                                    break;
                                } else
                                    dataHash.add(hashKey);
                                mRecords.add(record);
                            }
                            Intent intent = new Intent(ACTION_BLUETOOTHCUP_RECORD);
                            intent.putExtra("Address", IO().getAddress());
                            intent.putExtra("Record", data);
                            context().sendBroadcast(intent);
                        }

                        mLastDataTime = new Date();
                        if (record.Index == record.Count) {
                            dbg.i("收到记录完成");
                            synchronized (mRecords) {
                                mCupVolume.addRecord(mRecords.toArray(new CupRecord[mRecords.size()]));
                                mRecords.clear();
                                dataHash.clear();
                            }
                            Intent comp_intent = new Intent(ACTION_BLUETOOTHCUP_RECORD_COMPLETE);
                            comp_intent.putExtra("Address", IO().getAddress());
                            context().sendBroadcast(comp_intent);
                        }
                    }
                    break;
                }
            }
        }

        @Override
        public boolean onIOInit(BaseDeviceIO.DataSendProxy sendHandle) {
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

        @Override
        public boolean CheckTransmissionsComplete(BaseDeviceIO io) {
            if (mLastDataTime != null) {
                //如果上几次接收饮水记录的时间小于2秒,不进入定时循环,等待下条饮水记录
                Date dt = new Date();
                return (dt.getTime() - mLastDataTime.getTime()) >= 2000;
            } else
                return true;
        }
    }
}
