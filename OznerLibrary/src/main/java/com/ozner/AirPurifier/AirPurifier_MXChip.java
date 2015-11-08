package com.ozner.AirPurifier;

import android.content.Context;

import com.ozner.WaterPurifier.WaterPurifierStatus;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.OznerDevice;
import com.ozner.oznerlibrary.R;
import com.ozner.wifi.mxchip.MXChipIO;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by xzyxd on 2015/11/2.
 */
public class AirPurifier_MXChip extends OznerDevice {
    public static final String ACTION_WATER_PURIFIER_STATUS_CHANGE = "com.ozner.water.purifier.status.change";

    private static String SecureCode = "580c2783";
    final WaterPurifierStatus status = new WaterPurifierStatus();
    final WaterPurifierImp waterPurifierImp = new WaterPurifierImp();
    boolean isOffline = true;
    Timer autoUpdateTimer = null;

    public AirPurifier_MXChip(Context context, String Address, String Model, String Setting) {
        super(context, Address, Model, Setting);
    }

    @Override
    protected String getDefaultName() {
        return context().getString(R.string.air_purifier_name);
    }

    public boolean isOffline() {
        return isOffline;
    }

    @Override
    public Class<?> getIOType() {
        return MXChipIO.class;
    }

    @Override
    protected void doSetDeviceIO(BaseDeviceIO oldIO, BaseDeviceIO newIO) {
        if (oldIO != null) {
            oldIO.setOnTransmissionsCallback(null);
            oldIO.unRegisterStatusCallback(waterPurifierImp);
            oldIO.setOnInitCallback(null);
        }
        if (newIO != null) {
            MXChipIO io = (MXChipIO) newIO;
            io.setSecureCode(SecureCode);
            io.setOnTransmissionsCallback(waterPurifierImp);
            io.registerStatusCallback(waterPurifierImp);
            io.setOnInitCallback(waterPurifierImp);
        }
    }


    class WaterPurifierImp implements
            BaseDeviceIO.OnTransmissionsCallback,
            BaseDeviceIO.StatusCallback,
            BaseDeviceIO.OnInitCallback {

        @Override
        public void onConnected(BaseDeviceIO io) {

        }

        @Override
        public void onDisconnected(BaseDeviceIO io) {
            cancelTimer();
            isOffline = true;
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
            }
        }

        private void doTime() {
        }

        @Override
        public void onIOSend(byte[] bytes) {

        }


        @Override
        public void onIORecv(byte[] bytes) {
            if (bytes == null) {
                return;
            }
        }

        @Override
        public boolean onIOInit(BaseDeviceIO.DataSendProxy sendHandle) {
            try {
                isOffline = true;
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        private void cancelTimer() {
            if (autoUpdateTimer != null) {
                autoUpdateTimer.cancel();
                autoUpdateTimer.purge();
                autoUpdateTimer = null;
            }
        }

    }


}
nb5