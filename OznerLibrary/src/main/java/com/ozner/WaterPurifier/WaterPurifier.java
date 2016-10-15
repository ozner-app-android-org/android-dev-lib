package com.ozner.WaterPurifier;

import android.content.Context;
import android.content.Intent;

import com.ozner.XObject;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.OperateCallback;
import com.ozner.device.OperateCallbackProxy;
import com.ozner.device.OznerDevice;
import com.ozner.oznerlibrary.R;
import com.ozner.util.ByteUtil;
import com.ozner.util.Helper;
import com.ozner.wifi.mxchip.MXChipIO;
import com.ozner.wifi.mxchip.Pair.CRC8;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by xzyxd on 2015/11/2.
 */
public abstract class WaterPurifier extends OznerDevice {
    public static final String ACTION_WATER_PURIFIER_STATUS_CHANGE = "com.ozner.water.purifier.statusPacket.change";
    private static final int defaultAutoUpdatePeriod=5000;

    final Sensor sensor=new Sensor();
    final Status status=new Status();
    protected final WaterPurifierInfo info=new WaterPurifierInfo();
    boolean isOffline = true;


    public WaterPurifier(Context context, String Address, String Type, String Setting) {
        super(context, Address, Type, Setting);
    }

    public class Sensor
    {
        public int TDS1()
        {
            return getTDS1();
        }
        public int TDS2()
        {
            return getTDS2();
        }
        @Override
        public String toString() {
            return String.format("TDS1:%d TDS2:%d", TDS1(),TDS2());
        }
    }
    public class Status
    {
        public boolean Power()
        {
            return getPower();
        }

        /**
         * 打开电源
         *
         * @param Power 开关
         * @param cb    状态回调
         */
        public void setPower(boolean Power, OperateCallback<Void> cb) {
            if (IO() == null) {
                cb.onFailure(null);
            }
            WaterPurifier.this.setPower(Power,cb);
        }
        public boolean Hot() {
            return getHot();
        }

        /**
         * 打开加热
         *
         * @param Hot 开关
         * @param cb  状态回调
         */
        public void setHot(boolean Hot, OperateCallback<Void> cb) {
            if (IO() == null) {
                cb.onFailure(null);
            }
            WaterPurifier.this.setHot(Hot,cb);
        }

        public boolean Cool() {
            return getCool();
        }

        /**
         * 打开制冷
         *
         * @param Cool 开关
         * @param cb   状态回调
         */
        public void setCool(boolean Cool, OperateCallback<Void> cb) {
            if (IO() == null) {
                cb.onFailure(null);
            }
            WaterPurifier.this.setCool(Cool,cb);
        }


        public boolean Sterilization() {
            return getSterilization();
        }

        /**
         * 打开杀菌
         *
         * @param Sterilization 开关
         * @param cb            状态回调
         */
        public void setSterilization(boolean Sterilization, OperateCallback<Void> cb) {
            if (IO() == null) {
                cb.onFailure(null);
            }
            WaterPurifier.this.setSterilization(Sterilization,cb);
        }

        @Override
        public String toString() {
            return String.format("Power:%s Hot:%s Cool:%s Sterilization:%s",
                    String.valueOf(Power()), String.valueOf(Hot()), String.valueOf(Cool()),
                    String.valueOf(Sterilization()));
        }
    }



    public Sensor sensor()
    {
        return sensor;
    }
    public Status status()
    {
        return status;
    }
    public WaterPurifierInfo info() {return info;}

    protected abstract void updateStatus(OperateCallback<Void> cb) ;


    @Override
    public String toString() {
        if (isOffline())
        {
            return "offline";
        }else {
            return String.format("Status:%s\nSensor:%s",status.toString(),sensor.toString());
        }
    }

    @Override
    public int getTimerDelay() {
        return defaultAutoUpdatePeriod;
    }

    @Override
    protected String getDefaultName() {
        return context().getString(R.string.water_purifier_name);
    }


    public boolean isOffline() {
        return isOffline;
    }


    @Override
    protected void doSetDeviceIO(BaseDeviceIO oldIO, BaseDeviceIO newIO) {
        if (newIO==null)
        {
            setOffline(true);
        }
    }


    protected void setOffline(boolean isOffline)
    {
        if (isOffline!=this.isOffline) {
            this.isOffline = isOffline;
            doUpdate();
        }
    }

    @Override
    protected void doTimer() {
        updateStatus(null);
    }



    protected abstract int getTDS1();
    protected abstract int getTDS2();

    protected boolean getPower()
    {
        return true;
    }

    protected void setPower(boolean Power, OperateCallback<Void> cb)
    {
        cb.onFailure(new UnsupportedOperationException());
    }


    protected boolean getHot(){
        return false;
    }
    protected void setHot(boolean Hot, OperateCallback<Void> cb)
    {
        cb.onFailure(new UnsupportedOperationException());
    }


    protected boolean getCool()
    {
        return false;
    }

    protected void setCool(boolean Cool, OperateCallback<Void> cb)
    {
        cb.onFailure(new UnsupportedOperationException());
    }


    protected boolean getSterilization()
    {
        return false;
    }

    protected void setSterilization(boolean Sterilization, OperateCallback<Void> cb)
    {
        cb.onFailure(new UnsupportedOperationException());
    }

}
