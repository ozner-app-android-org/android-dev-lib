package com.ozner.kettle;

import android.content.Context;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.DeviceSetting;
import com.ozner.device.OperateCallback;
import com.ozner.device.OznerDevice;
import com.ozner.oznerlibrary.R;

/**
 * Created by zhiyongxu on 15/12/21.
 */
public class Kettle extends OznerDevice {
    private static final int defaultAutoUpdatePeriod = 5000;

    private static final byte opCode_RequestStatus = 0x20;
    private static final byte opCode_StatusResp = 0x21;

    private static final byte opCode_SendSetting = 0x33;
    private static final byte opCode_StartHot = 0x34;


    final KettleIMP kettleIMP = new KettleIMP();

    final KettleStatus status = new KettleStatus();
    KettleFirmwareTools firmwareTools = new KettleFirmwareTools();

    public KettleFirmwareTools firmwareTools() {
        return firmwareTools;
    }

    /**
     * 返回设备状态
     *
     * @return
     */
    public KettleStatus status() {
        return status;
    }


    public Kettle(Context context, String Address, String Type, String Setting) {
        super(context, Address, Type, Setting);
    }

    @Override
    public int getTimerDelay() {
        return defaultAutoUpdatePeriod;
    }

    @Override
    protected String getDefaultName() {
        return context().getString(R.string.kettle);
    }

    @Override
    public Class<?> getIOType() {
        return BluetoothIO.class;
    }

    @Override
    protected void doSetDeviceIO(BaseDeviceIO oldIO, BaseDeviceIO newIO) {
        if (oldIO != null) {
            oldIO.setOnInitCallback(null);
            oldIO.unRegisterStatusCallback(kettleIMP);
            oldIO.setOnTransmissionsCallback(null);
            oldIO.setCheckTransmissionsCompleteCallback(null);
            firmwareTools.bind(null);
        }
        if (newIO != null) {
            newIO.setOnTransmissionsCallback(kettleIMP);
            newIO.setOnInitCallback(kettleIMP);
            newIO.registerStatusCallback(kettleIMP);
            newIO.setCheckTransmissionsCompleteCallback(kettleIMP);
            firmwareTools.bind((BluetoothIO) newIO);
        }
    }

    @Override
    protected DeviceSetting initSetting(String Setting) {
        DeviceSetting setting = new KettleSetting();
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
            kettleIMP.sendSetting();
    }


    @Override
    protected void doTimer() {
        kettleIMP.doTime();
    }

    private class KettleIMP implements
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


        public boolean sendSetting() {
            KettleSetting setting = (KettleSetting) Setting();
            if (setting == null)
                return false;

            byte[] data = new byte[4];
            data[0] = (byte) setting.atomization();
            data[1] = 0;
            data[2] = (byte) setting.massage();
            data[3] = 0;
            return this.send(opCode_SendSetting, data, null);
        }

        @Override
        public void onIORecv(byte[] bytes) {
            if (bytes == null) return;
            if (bytes.length < 1) return;
            byte opCode = bytes[0];

            switch (opCode) {
                case opCode_StatusResp: {
                    status.load(bytes);
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
