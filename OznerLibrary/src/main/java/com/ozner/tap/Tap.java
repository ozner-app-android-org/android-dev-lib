package com.ozner.tap;

import android.content.Context;
import android.content.Intent;
import android.text.format.Time;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.AutoUpdateClass;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.DeviceSetting;
import com.ozner.device.OznerDevice;
import com.ozner.oznerlibrary.R;
import com.ozner.util.dbg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

/**
 * Created by zhiyongxu on 15/10/28.
 * 水探头
 */
public class Tap extends OznerDevice {
    private static final int defaultAutoUpdatePeriod=5000;
    /**
     * 收到传感器数据
     */
    public final static String ACTION_BLUETOOTHTAP_SENSOR = "com.ozner.tap.bluetooth.sensor";
    /**
     * 收到单条饮水记录
     */
    public final static String ACTION_BLUETOOTHTAP_RECORD = "com.ozner.tap.bluetooth.record";


    /**
     * 水探头自动监测记录接收完成
     */
    public final static String ACTION_BLUETOOTHTAP_RECORD_COMPLETE = "com.ozner.tap.bluetooth.record.complete";

    static final byte opCode_ReadSensor = 0x12;
    static final byte opCode_ReadSensorRet = (byte) 0xA2;

    static final byte opCode_ReadTDSRecord = 0x17;
    static final byte opCode_ReadTDSRecordRet = (byte) 0xA7;
    //static final byte opCode_GetFirmwareSum = (byte) 0xc5;
    //static final byte opCode_GetFirmwareSumRet = (byte) 0xc5;

    static final byte opCode_SetDetectTime = 0x10;
    static final byte opCode_UpdateTime = (byte) 0xF0;
    static final byte opCode_FrontMode = (byte) 0x21;

    //   static final byte opCode_DeviceInfo = (byte) 0x15;
    //   static final byte opCode_DeviceInfoRet = (byte) 0xA5;
    //  static final byte opCode_SetName = (byte) 0x80;
    //  static final byte opCode_GetFirmware = (byte) 0x82;
    //  static final byte opCode_GetFirmwareRet = (byte) -126;


    final TapSensor mSensor = new TapSensor();
    final TapRecordList mTapRecordList;


    final TapIMP tapIMP = new TapIMP(defaultAutoUpdatePeriod);

    TapFirmwareTools firmwareTools = new TapFirmwareTools();


    public Tap(Context context, String Address, String Type, String Setting) {
        super(context, Address, Type, Setting);
        initSetting(Setting);
        mTapRecordList = new TapRecordList(context, Address());
    }

    /**
     * 判断设备是否处于配对状态
     *
     * @param io 设备接口
     * @return true=配对状态
     */
    public static boolean isBindMode(BluetoothIO io) {
        if (!TapManager.IsTap(io.getType())) return false;
        if (io.getScanResponseData() != null) {
            byte[] data = io.getScanResponseData();
            if (data.length > 0) {
                return data[0] == 1;
            }
        }
        return false;
    }

    @Override
    protected boolean doCheckAvailable(BaseDeviceIO io) {
        if (io==null)
        {
            BluetoothIO bluetoothIO=(BluetoothIO)io;
            if (((BluetoothIO) io).getScanResponseType()==0x10)
            {
                byte[] bytes=bluetoothIO.getScanResponseData();
                if (bytes.length>8)
                {
                    return bytes[8]>0;
                }
            }
        }
        return false;
    }

    @Override
    protected String getDefaultName() {
        return context().getString(R.string.tap_name);
    }


    @Override
    public Class<?> getIOType() {
        return BluetoothIO.class;
    }

    public TapSensor Sensor() {
        return mSensor;
    }

    public TapRecordList TapRecordList() {
        return mTapRecordList;
    }

    public TapFirmwareTools firmwareTools() {
        return firmwareTools;
    }


    @Override
    protected void doChangeRunningMode() {
        tapIMP.sendBackground();
    }



    @Override
    public void updateSettings() {
        if ((IO() != null) && (IO().isReady()))
            tapIMP.sendSetting();
    }

    public TapSetting Setting() {
        return (TapSetting) super.Setting();
    }

    @Override
    protected DeviceSetting initSetting(String Setting) {
        DeviceSetting setting = new TapSetting();
        setting.load(Setting);
        return setting;
    }


    @Override
    protected void doSetDeviceIO(BaseDeviceIO oldIO, BaseDeviceIO newIO) {
        if (oldIO != null) {
            oldIO.setOnInitCallback(null);
            oldIO.setOnTransmissionsCallback(null);
            oldIO.unRegisterStatusCallback(tapIMP);
            oldIO.setCheckTransmissionsCompleteCallback(null);
            firmwareTools.bind(null);
        }
        tapIMP.stop();
        if (newIO != null) {
            newIO.setOnTransmissionsCallback(tapIMP);
            newIO.setOnInitCallback(tapIMP);
            newIO.registerStatusCallback(tapIMP);
            newIO.setCheckTransmissionsCompleteCallback(tapIMP);
            firmwareTools.bind((BluetoothIO) newIO);
        }
    }



    @Override
    public String toString() {
        if (connectStatus()== BaseDeviceIO.ConnectStatus.Connected)
        {
            return String.format("TDS:%d Power:%d%%",Sensor().TDSFix,(int)(Sensor().getPower()*100));
        }else
        {
            return connectStatus().toString();
        }
    }
    class TapIMP extends AutoUpdateClass implements
            BluetoothIO.OnInitCallback,
            BluetoothIO.OnTransmissionsCallback,
            BluetoothIO.StatusCallback,
            BluetoothIO.CheckTransmissionsCompleteCallback {
        Date mLastDataTime = null;
        int RequestCount = 0;
        final ArrayList<RawRecord> mRecords = new ArrayList<>();
        final HashSet<String> dataHash = new HashSet<>();

        private boolean send(byte opCode, byte[] data) {
            return IO() != null && IO().send(BluetoothIO.makePacket(opCode, data));
        }
        private void sendBackground() {
            if (getRunningMode() == RunningMode.Foreground) {
                send(opCode_FrontMode, null);
            }
        }
        private boolean sendTime() {
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
            return send(opCode_UpdateTime, data);
        }

        private boolean sendSetting() {
            TapSetting setting = Setting();
            if (setting == null)
                return false;

            byte[] data = new byte[12];
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
            return this.send(opCode_SetDetectTime, data);
        }
        private void requestSensor() {
            if (IO() != null) {
                IO().send(BluetoothIO.makePacket(opCode_ReadSensor, null));
            }
        }

        private void requestRecord() {
            if (IO() != null) {
                if (IO().send(BluetoothIO.makePacket(opCode_ReadTDSRecord, null))) {
                    dbg.i("请求记录");
                }
            }
        }


        public TapIMP(long period) {
            super(period);
        }

        @Override
        public void onConnected(BaseDeviceIO io) {

        }

        @Override
        public void onDisconnected(BaseDeviceIO io) {
            stop();
        }

        @Override
        public void onReady(BaseDeviceIO io) {
            start(100);
        }

        @Override
        public boolean onIOInit() {
            try {
                if (!sendTime())
                    return false;
                Thread.sleep(100);

                if (!sendSetting())
                    return false;
                Thread.sleep(100);

                sendBackground();
                Thread.sleep(100);

                return true;
            } catch (Exception e) {
                return false;
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
                    Intent intent = new Intent(ACTION_BLUETOOTHTAP_SENSOR);
                    intent.putExtra("Address", IO().getAddress());
                    intent.putExtra("Sensor", data);
                    context().sendBroadcast(intent);
                    doUpdate();

                    break;
                }

                case opCode_ReadTDSRecordRet: {
                    if (data != null) {
                        RawRecord record = new RawRecord();
                        record.FromBytes(data);
                        if (record.TDS > 0) {
                            String hashKey = String.valueOf(record.time.getTime()) + "_" + String.valueOf(record.TDS);
                            if (dataHash.contains(hashKey)) {
                                dbg.e("收到水杯重复数据");
                                break;
                            } else
                                dataHash.add(hashKey);
                            synchronized (mRecords) {
                                mRecords.add(record);
                            }
                            Intent intent = new Intent(ACTION_BLUETOOTHTAP_RECORD);
                            intent.putExtra("Address", IO().getAddress());
                            intent.putExtra("CupRecord", data);
                            context().sendBroadcast(intent);

                        }
                        mLastDataTime = new Date();

                        if ((mRecords.size()>0)&&(record.Index == record.Count)) {
                            synchronized (mRecords) {
                                mTapRecordList.addRecord(mRecords.toArray(new RawRecord[mRecords.size()]));
                                mRecords.clear();
                                dataHash.clear();
                            }
                            Intent comp_intent = new Intent(ACTION_BLUETOOTHTAP_RECORD_COMPLETE);
                            comp_intent.putExtra("Address", IO().getAddress());
                            context().sendBroadcast(comp_intent);
                            doUpdate();
                        }
                    }
                    break;
                }
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


        @Override
        protected void doTime() {
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
    }
}
