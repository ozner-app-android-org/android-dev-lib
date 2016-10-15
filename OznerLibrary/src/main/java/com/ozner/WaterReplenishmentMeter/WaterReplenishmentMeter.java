package com.ozner.WaterReplenishmentMeter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.DeviceSetting;
import com.ozner.device.OperateCallback;
import com.ozner.device.OznerDevice;
import com.ozner.oznerlibrary.R;
import com.ozner.util.ByteUtil;

import java.util.Date;

/**
 * Created by zhiyongxu on 15/12/21.
 */
public class WaterReplenishmentMeter extends OznerDevice {
    private static final int defaultAutoUpdatePeriod = 5000;

    private static final byte opCode_RequestStatus = 0x20;
    private static final byte opCode_StatusResp = 0x21;

    private static final byte opCode_StartTest = 0x32;
    private static final byte opCode_TestResp = 0x33;
    private static final byte opCode_Testing = 0x34;
    private static final byte opCode_SendSetting = 0x35;



    public enum TestParts {Face, Hand, Eye, Other}

    final WaterReplenishmentMeterIMP waterReplenishmentMeterIMP = new WaterReplenishmentMeterIMP();

    final Status status = new Status();
    WaterReplenishmentMeterFirmwareTools firmwareTools = new WaterReplenishmentMeterFirmwareTools();

    public WaterReplenishmentMeterFirmwareTools firmwareTools() {
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


    public WaterReplenishmentMeter(Context context, String Address, String Type, String Setting) {
        super(context, Address, Type, Setting);
    }

    @Override
    public int getTimerDelay() {
        return defaultAutoUpdatePeriod;
    }

    @Override
    protected String getDefaultName() {
        return context().getString(R.string.water_replenishment_meter);
    }

    @Override
    public Class<?> getIOType() {
        return BluetoothIO.class;
    }

    @Override
    protected void doSetDeviceIO(BaseDeviceIO oldIO, BaseDeviceIO newIO) {
        if (oldIO != null) {
            oldIO.setOnInitCallback(null);
            oldIO.unRegisterStatusCallback(waterReplenishmentMeterIMP);
            oldIO.setOnTransmissionsCallback(null);
            oldIO.setCheckTransmissionsCompleteCallback(null);
            firmwareTools.bind(null);
        }
        if (newIO != null) {
            newIO.setOnTransmissionsCallback(waterReplenishmentMeterIMP);
            newIO.setOnInitCallback(waterReplenishmentMeterIMP);
            newIO.registerStatusCallback(waterReplenishmentMeterIMP);
            newIO.setCheckTransmissionsCompleteCallback(waterReplenishmentMeterIMP);
            firmwareTools.bind((BluetoothIO) newIO);
        }
    }

    @Override
    protected DeviceSetting initSetting(String Setting) {
        DeviceSetting setting = new WaterReplenishmentSetting();
        setting.load(Setting);
        return setting;
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

    @Override
    public void updateSettings() {
        if ((IO() != null) && (IO().isReady()))
            waterReplenishmentMeterIMP.sendSetting();
    }
    //    class TestRunnableProxy  implements BluetoothIO.BluetoothRunnable
//    {
//        OperateCallback<Float> cb;
//        TestParts testParts;
//        public TestRunnableProxy(TestParts testParts,OperateCallback<Float> cb)
//        {
//            this.testParts=testParts;
//            this.cb=cb;
//        }
//
//        @Override
//        public void run() {
//            if ((IO()==null) || (!IO().isReady()))
//            {
//                cb.onFailure(null);
//                return ;
//            }
//            byte[] data=new byte[1];
//
//            switch (testParts)
//            {
//                case Face:data[0]=0;
//                    break;
//                case Hand:data[0]=1;
//                    break;
//                case Eye:data[0]=2;
//                    break;
//                case Other:
//                    data[0] = 4;
//                    break;
//            }
//            IO().clearLastRecvPacket();
//            if (IO().send(BluetoothIO.makePacket(opCode_StartTest, data))) {
//                try {
//                    waitObject(10000);
//
//                    byte[] bytes = IO().getLastRecvPacket();
//                    if ((bytes != null) && (bytes.length >= 3)) {
//                        if (bytes[0] == opCode_TestResp) {
//                            float value=(bytes[1]*0xff+bytes[2])/10.0f;
//
//                            cb.onSuccess(value);
//                            return;
//                        }
//                    }
//
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//            }
//            cb.onFailure(null);
//
//        }
//    }

    //    /**
//     * 开始测试
//     *
//     * @param testParts 测试部位
//     * @param cb 结果回调
//     */
//    public void startTest(TestParts testParts,OperateCallback<Float> cb) {
//
//        if ((IO()==null) || (!IO().isReady()))
//        {
//            cb.onFailure(null);
//            return ;
//        }
//        ((BluetoothIO)IO()).post(new TestRunnableProxy(testParts,cb));
//    }
    public class TestValue {
        /**
         * 水份
         */
        public float moisture;
        /**
         * 油份
         */
        public float oil;

        @Override
        public String toString() {
            return String.format("moisture:%f oil:%f",moisture,oil);
        }
    }


    public static double[][] testValueTable =
            {
                    {8, 220, -1, -1, -1, -1, 0, 0},
                    {220,300,0.093,20 ,28 ,0.042,9 ,13},
                    {300,350,0.092,28 ,32 ,0.041,12 ,14},
                    {350,400,0.09,32 ,36 ,0.04,14 ,16},
                    {400,450,0.089,36 ,40 ,0.039,16 ,18},
                    {450,500,0.088,40 ,44 ,0.038,17 ,19},
                    {500,600,0.087,44 ,52 ,0.037,19 ,22},
                    {600,700,0.086,52 ,60 ,0.036,22 ,25},
                    {700,800,0.085,60 ,68 ,0.035,25 ,28},
                    {800,1023,0.084,67 ,86 ,0.034,27 ,35}

//                    {8, 200, 0, 0, 0, 0, 0, 0},
//                    {200, 250, 0.082, 16.4, 20.5, 0.036, 7.2, 9.0},
//                    {250, 300, 0.081, 20.3, 24.3, 0.0355, 8.9, 10.7},
//                    {300, 350, 0.08, 24.0, 28.0, 0.035, 10.5, 12.3},
//                    {350, 380, 0.079, 27.7, 30.0, 0.0345, 12.1, 13.1},
//                    {380, 450, 0.079, 30.0, 35.6, 0.034, 12.9, 15.3},
//                    {450, 500, 0.078, 35.1, 39.0, 0.0335, 15.1, 16.8},
//                    {500, 550, 0.077, 38.5, 42.4, 0.033, 16.5, 18.2},
//                    {550, 600, 0.0765, 42.1, 45.9, 0.0325, 17.9, 19.5},
//                    {600, 650, 0.076, 45.6, 49.4, 0.032, 19.2, 20.8},
//                    {650, 700, 0.0755, 49.1, 52.9, 0.0315, 20.5, 22.1},
//                    {700, 750, 0.075, 52.5, 56.3, 0.031, 21.7, 23.3},
//                    {750, 800, 0.0745, 55.9, 59.6, 0.0305, 22.9, 24.4},
//                    {800, 850, 0.074, 59.2, 62.9, 0.03, 24.0, 25.5},
//                    {850, 900, 0.0735, 62.5, 66.2, 0.0295, 25.1, 26.6},
//                    {900, 1023, 0.073, 65.7, 74.7, 0.029, 26.1, 29.7},
            };

    public class Status {
        boolean power = false;
        boolean testing = false;
        int adc = 0;
        long testTime=0;
        float battery;


        public void reset() {
            battery = -1;
            testing = false;
        }

        /**
         * 电源状态
         *
         * @return TRUE=开,FALSE=关
         */
        public boolean power() {
            return power;
        }

        /**
         * 电量百分比
         */
        public float battery() {
            return battery;
        }

        /**
         * 正在测试中
         *
         * @return true测试中
         */
        public boolean isTesting() {
            return testing;
        }

        /**
         * 测试结果
         *
         * @return
         */
        public TestValue testValue() {
            TestValue tv=new TestValue();
            tv.moisture=0;
            tv.oil=0;
            for (double[] value : testValueTable ) {
                if (adc>=value[0] && adc<=value[1])
                {
                    tv.moisture=(float)(Math.abs(value[1]-value[0])*value[2]+value[3]);
                    tv.oil=(float)(Math.abs(value[1]-value[0])*value[5]+value[6]);
                    return tv;
                }
            }
            return tv;
        }


        @Override
        public String toString() {
            return String.format("Power:%b Battery:%f Testing:%b %s", power(), battery(), isTesting(), testValue().toString());
    }


    }

    @Override
    protected void doTimer() {
        waterReplenishmentMeterIMP.doTime();
    }

    private class WaterReplenishmentMeterIMP implements
            BaseDeviceIO.StatusCallback,
            BaseDeviceIO.OnInitCallback,
            BaseDeviceIO.OnTransmissionsCallback,
            BaseDeviceIO.CheckTransmissionsCompleteCallback {

        public void doTime() {
            if (IO() == null) return;
            requestStatus();
        }


        private boolean send(byte opCode, byte[] data, OperateCallback<Void> cb) {
            return IO() != null && IO().send(BluetoothIO.makePacket(opCode, data), cb);
        }

        private boolean requestStatus() {
            return send(opCode_RequestStatus, null, null);
        }


        @Override
        public boolean onIOInit() {
            return true;
        }

        @Override
        public void onIOSend(byte[] bytes) {

        }

        private Handler testHandler = new Handler(Looper.getMainLooper())
        {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what==1)
                {
                    Long time=(Long)msg.obj;
                    if (time.equals(status.testTime))
                    {
                        if (status.testing) {
                            status.testing = false;
                            doUpdate();
                        }
                    }
                }
                super.handleMessage(msg);
            }
        };

        public boolean sendSetting()
        {
            WaterReplenishmentSetting setting =(WaterReplenishmentSetting)Setting();
            if (setting == null)
                return false;

            byte[] data = new byte[4];
            data[0]=(byte)setting.atomization();
            data[1]=0;
            data[2]=(byte)setting.massage();
            data[3]=0;
            return this.send(opCode_SendSetting, data,null);
        }

        @Override
        public void onIORecv(byte[] bytes) {
            if (bytes == null) return;
            if (bytes.length < 1) return;
            byte opCode = bytes[0];

            switch (opCode) {
                case opCode_StatusResp: {
                    status.power = bytes[1] == 1;
                    status.battery = bytes[2] / 100.0f;
                    doUpdate();
                    break;
                }
                case opCode_Testing: {
                    status.testing = true;
                    status.adc=0;
                    Date dt=new Date();
                    status.testTime=dt.getTime();
                    doUpdate();
                    Message msg=new Message();
                    msg.what=1;
                    msg.obj=new Long(status.testTime);
                    testHandler.sendMessageDelayed(msg,6000);
                    break;
                }
                case opCode_TestResp: {
                    if (!status.testing) return;
                    status.adc=ByteUtil.getShort(bytes, 1);
                    status.testing = false;
                    doUpdate();
                    break;
                }
            }
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
            return true;
        }
    }

}
