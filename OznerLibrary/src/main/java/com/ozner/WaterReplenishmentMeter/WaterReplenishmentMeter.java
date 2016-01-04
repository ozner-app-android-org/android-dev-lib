package com.ozner.WaterReplenishmentMeter;

import android.content.Context;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.AutoUpdateClass;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.OperateCallback;
import com.ozner.device.OznerDevice;
import com.ozner.oznerlibrary.R;
/**
 * Created by zhiyongxu on 15/12/21.
 */
public class WaterReplenishmentMeter extends OznerDevice {
    private static final int defaultAutoUpdatePeriod=5000;

    private static final byte opCode_RequestStatus = 0x20;
    private static final byte opCode_StatusResp = 0x21;

    private static final byte opCode_StartTest = 0x32;
    private static final byte opCode_TestResp = 0x33;

    public enum TestParts {Face,Hand,Eye,Other}

    final WaterReplenishmentMeterIMP waterReplenishmentMeterIMP = new WaterReplenishmentMeterIMP(defaultAutoUpdatePeriod);

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
        waterReplenishmentMeterIMP.stop();
        if (newIO != null) {
            newIO.setOnTransmissionsCallback(waterReplenishmentMeterIMP);
            newIO.setOnInitCallback(waterReplenishmentMeterIMP);
            newIO.registerStatusCallback(waterReplenishmentMeterIMP);
            newIO.setCheckTransmissionsCompleteCallback(waterReplenishmentMeterIMP);
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

    class TestRunnableProxy  implements BluetoothIO.BluetoothRunnable
    {
        OperateCallback<Float> cb;
        TestParts testParts;
        public TestRunnableProxy(TestParts testParts,OperateCallback<Float> cb)
        {
            this.testParts=testParts;
            this.cb=cb;
        }

        @Override
        public void run() {
            if ((IO()==null) || (!IO().isReady()))
            {
                cb.onFailure(null);
                return ;
            }
            byte[] data=new byte[1];

            switch (testParts)
            {
                case Face:data[0]=0;
                    break;
                case Hand:data[0]=1;
                    break;
                case Eye:data[0]=2;
                    break;
                case Other:
                    data[0] = 4;
                    break;
            }
            IO().clearLastRecvPacket();
            if (IO().send(BluetoothIO.makePacket(opCode_StartTest, data))) {
                try {
                    waitObject(10000);

                    byte[] bytes = IO().getLastRecvPacket();
                    if ((bytes != null) && (bytes.length >= 3)) {
                        if (bytes[0] == opCode_TestResp) {
                            short sv= (short) (((bytes[2] & 0xFF) << 8) + (bytes[1] & 0xFF));
                            float value = sv / 10.0f;
                            cb.onSuccess(new Float(value));
                            return;
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            cb.onFailure(null);

        }
    }

    /**
     * 开始测试
     *
     * @param testParts 测试部位
     * @param cb 结果回调
     */
    public void startTest(TestParts testParts,OperateCallback<Float> cb) {

        if ((IO()==null) || (!IO().isReady()))
        {
            cb.onFailure(null);
            return ;
        }
        ((BluetoothIO)IO()).post(new TestRunnableProxy(testParts,cb));
    }

    public class Status {
        boolean power = false;

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
        public float battery;

        public float battery() {
            return battery;
        }


        @Override
        public String toString() {
            return String.format("Power:%b Battery:%f", power(), battery());
        }

    }


    private class WaterReplenishmentMeterIMP extends AutoUpdateClass implements
            BaseDeviceIO.StatusCallback,
            BaseDeviceIO.OnInitCallback,
            BaseDeviceIO.OnTransmissionsCallback,
            BaseDeviceIO.CheckTransmissionsCompleteCallback
    {

        public WaterReplenishmentMeterIMP(long period) {
            super(period);
        }

        @Override
        protected void doTime() {
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

        @Override
        public void onIORecv(byte[] bytes) {
            if (bytes == null) return;
            if (bytes.length < 1) return;
            byte opCode = bytes[0];

            switch (opCode) {
                case opCode_StatusResp: {
                    status.power = bytes[1] == 1;
                    status.battery=bytes[2]/100.0f;
                    doUpdate();
                    break;
                }
                case opCode_TestResp:
                {
                    setObject();
                }
                break;
            }
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
            requestStatus();
            if (getRunningMode() == RunningMode.Foreground) {
                start(100);
            }

        }

        @Override
        public boolean CheckTransmissionsComplete(BaseDeviceIO io) {
            return true;
        }
    }

}
