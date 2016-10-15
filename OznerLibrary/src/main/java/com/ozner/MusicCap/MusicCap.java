package com.ozner.MusicCap;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.OperateCallback;
import com.ozner.device.OznerDevice;
import com.ozner.oznerlibrary.R;
import com.ozner.util.ByteUtil;
import com.ozner.util.dbg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

/**
 * Created by zhiyongxu on 15/12/21.
 */
public class MusicCap extends OznerDevice {
    private static final int defaultAutoUpdatePeriod=10000;
    /**
     * 收到单条饮水记录
     */
    public final static String ACTION_MUSICCAP_RECORD = "com.ozner.music.record";
    /**
     * 记录传输完成
     */
    public final static String ACTION_MUSICCAP_RECORD_COMPLETE = "com.ozner.music.record.complete";
    private static final byte opCode_RequestStatus=(byte)0x20;
    private static final byte opCode_StatusResp=(byte)0x21;
    private static final byte opCode_StartA2DP=(byte)0x22;


    private static final byte opCode_StartTest=(byte)0x32;
    private static final byte opCode_TestResp=(byte)0x33;
    private static final byte opCode_Testing=(byte)0x34;

    private static final byte opCode_RequsetRecord=(byte)0x41;
    private static final byte opCode_ResponseRecord=(byte)0x42;

    private static final byte opCode_SendTime=(byte)0xA1;
    private static final byte opCode_SendFirmware=(byte)0xC1;
    private static final byte opCode_EraseFirmware=(byte)0xC2;
    private static final byte opCode_UpdateFirmware=(byte)0xC3;


    public enum TestParts {Face,Hand,Eye,Other}

    final MusicCapIMP musicCapIMP = new MusicCapIMP();
    SportRecordList mRecordList;
    final Status status = new Status();
    MusicCapFirmwareTools firmwareTools = new MusicCapFirmwareTools();

    public MusicCapFirmwareTools firmwareTools() {
        return firmwareTools;
    }
    /**
     * 返回设备状态
     *
     * @return
     */
    public Status status() {
        return status;
    }
    public SportRecordList SportRecords() {
        return mRecordList;
    }

    public MusicCap(Context context, String Address, String Type, String Setting) {
        super(context, Address, Type, Setting);
        mRecordList=new SportRecordList(context,Address);
    }



    @Override
    protected String getDefaultName() {
        return context().getString(R.string.music_cap);
    }

    @Override
    public Class<?> getIOType() {
        return BluetoothIO.class;
    }

    @Override
    protected void doSetDeviceIO(BaseDeviceIO oldIO, BaseDeviceIO newIO) {
        if (oldIO != null) {
            oldIO.setOnInitCallback(null);
            oldIO.unRegisterStatusCallback(musicCapIMP);
            oldIO.setOnTransmissionsCallback(null);
            oldIO.setCheckTransmissionsCompleteCallback(null);
            firmwareTools.bind(null);
        }
        if (newIO != null) {
            newIO.setOnTransmissionsCallback(musicCapIMP);
            newIO.setOnInitCallback(musicCapIMP);
            newIO.registerStatusCallback(musicCapIMP);
            newIO.setCheckTransmissionsCompleteCallback(musicCapIMP);
            firmwareTools.bind((BluetoothIO) newIO);
        }
    }


    @Override
    public String toString() {
        return status().toString();
    }

    private String getValue(int value) {
        if (value == 0xFFFF) {
            return "-";
        } else
            return String.valueOf(value);
    }


    public class Status {
        boolean charging = false;
        boolean testing=false;
        String a2dp="";

        float testValue=0;
        int battery;
        public void reset()
        {
            testValue=0;
            charging=false;
            testing=false;
        }

        /**
         * 是否在充电
         */
        public boolean charging() {
            return charging;
        }

        /**
         * 电量百分比
         */
        public int battery() {
            return battery;
        }

        public String A2DP() {return a2dp;}

        /**
         * 正在测试中
         * @return true测试中
         */
        public boolean isTesting()
        {
            return testing;
        }

        /**
         * 测试结果
         * @return
         */
        public float testValue()
        {
            return testValue;
        }

        @Override
        public String toString() {
            return String.format("Battery:%d%% Charging:%b Testing:%b TestValue:%2.1f A2DP:%s",  battery(),charging(),isTesting(),testValue(),A2DP());
        }
        public void loadBytes(byte[] bytes)
        {

            battery=bytes[0];
            charging=bytes[1]!=0;

            a2dp = String.format("%02X:%02X:%02X:%02X:%02X:%02X",
                    bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7]);

        }


    }

    @Override
    protected void doTimer() {
        musicCapIMP.doTime();
    }

    public boolean autoTest(boolean auto)
    {
        return true;
        //return musicCapIMP.send(opCode_StartTest,new byte[]{auto?(byte)0:(byte)1},null);
    }
//    @Override
//    protected boolean doCheckAvailable(BaseDeviceIO io) {
////        if (io==null)
////        {
////            BluetoothIO bluetoothIO=(BluetoothIO)io;
////            if (((BluetoothIO) io).getScanResponseType()==0x10)
////            {
////                byte[] bytes=bluetoothIO.getScanResponseData();
////                if (bytes.length>8)
////                {
////                    return bytes[8]>0;
////                }
////            }
////        }
//        return false;
//    }
    private class MusicCapIMP  implements
            BaseDeviceIO.StatusCallback,
            BaseDeviceIO.OnInitCallback,
            BaseDeviceIO.OnTransmissionsCallback,
            BaseDeviceIO.CheckTransmissionsCompleteCallback
    {
        Date mLastDataTime = new Date();
        int RequestCount = 0;
        HashSet<String> dataHash = new HashSet<>();
        final ArrayList<RawRecord> mRawRecords = new ArrayList<>();




        public void doTime() {
            if (IO() == null) return;
            if (mLastDataTime != null) {
                //如果上几次接收饮水记录的时间小于2秒,不进入定时循环,等待下条饮水记录
                Date dt = new Date();
                if ((dt.getTime() - mLastDataTime.getTime()) < 2000) {
                    return;
                }
            }
            if (status.testing) return;
            if ((RequestCount % 5) == 0) {
                requestStatus();
            } else {
                requestRecord();
            }
            RequestCount++;
        }





        private boolean send(byte opCode, byte[] data, OperateCallback<Void> cb) {
            return IO() != null && IO().send(BluetoothIO.makePacket(opCode, data), cb);
        }

        private boolean requestStatus() {
            return send(opCode_RequestStatus, null, null);
        }




        @Override
        public boolean onIOInit() {
            try
            {
                if (!sendTime())
                    return false;

                Thread.sleep(100);
                autoTest(true);
                return true;
            }catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public void onIOSend(byte[] bytes) {

        }
        private Handler testHandler=new Handler(Looper.getMainLooper());

        private void requestRecord() {
            if (IO() != null) {
                if (IO().send(BluetoothIO.makePacket(opCode_RequsetRecord, null))) {
                    dbg.i("请求记录");
                }
            }
        }


        @Override
        public void onIORecv(final byte[] bytes) {
            if (bytes == null) return;
            if (bytes.length < 1) return;
            byte opCode = bytes[0];

            byte[] data = Arrays.copyOfRange(bytes, 1, bytes.length);

            switch (opCode) {
                case opCode_StatusResp: {
                    status().loadBytes(data);
                    doUpdate();
                    break;
                }
                case opCode_Testing:
                {
                    status.testing=true;
                    status.testValue=0;
                    doUpdate();
//                    testHandler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (status.testing) {
//                                status.testing = false;
//                                doUpdate();
//                                send(opCode_StartTest,new byte[]{1},null);
//                            }
//                        }
//                    },10000);

                    break;
                }
                case opCode_TestResp:
                {
                    status.testValue=(float)ByteUtil.getShort(data,1)/10.0f;
                    status.testing=false;
                    send(opCode_StartTest,new byte[]{0},null);
                    doUpdate();
                    break;
                }
                case opCode_ResponseRecord: {
                    if (data != null) {
                        RawRecord rawRecord = new RawRecord();
                        rawRecord.FromBytes(data);
                        if ((rawRecord.Index == rawRecord.Count) && (rawRecord.Count == 0) && (rawRecord.sportCount == 0)) {
                            return;
                        }

                        if (rawRecord.sportCount > 0) {
                            String hashKey = String.valueOf(rawRecord.time.getTime()) + "_" + String.valueOf(rawRecord.sportCount);
                            synchronized (mRawRecords) {
                                if (dataHash.contains(hashKey)) {
                                    dbg.e("收到水杯重复数据");
                                    break;
                                } else
                                    dataHash.add(hashKey);
                                mRawRecords.add(rawRecord);
                            }
                            Intent intent = new Intent(ACTION_MUSICCAP_RECORD);
                            intent.putExtra("Address", IO().getAddress());
                            intent.putExtra("CupRecord", data);
                            context().sendBroadcast(intent);
                        }

                        mLastDataTime = new Date();
                        if ((mRawRecords.size() > 0) && (rawRecord.Index == rawRecord.Count)) {
                            dbg.i("收到记录完成");
                            synchronized (mRawRecords) {
                                mRecordList.addRecord(mRawRecords.toArray(new RawRecord[mRawRecords.size()]));
                                mRawRecords.clear();
                                dataHash.clear();
                            }
                            Intent comp_intent = new Intent(ACTION_MUSICCAP_RECORD_COMPLETE);
                            comp_intent.putExtra("Address", IO().getAddress());
                            context().sendBroadcast(comp_intent);
                            doUpdate();
                        }
                    }
                    break;
                }
            }
        }

        private boolean sendTime() {
            dbg.i("开始设置时间:%s", IO().getAddress());
            byte[] time = new byte[4];
            ByteUtil.putInt(time, (int) (System.currentTimeMillis() / 1000), 0);
            return send(opCode_SendTime,time,null);
        }

        @Override
        public void onConnected(BaseDeviceIO io) {
            status.reset();
        }

        @Override
        public void onDisconnected(BaseDeviceIO io) {
            status.reset();
        }

        @Override
        public void onReady(BaseDeviceIO io) {
            status.reset();
            requestStatus();

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
