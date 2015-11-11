package com.ozner.AirPurifier;

import android.content.Context;

import com.ozner.device.BaseDeviceIO;
import com.ozner.device.DeviceSetting;
import com.ozner.device.OperateCallback;
import com.ozner.device.OznerDevice;
import com.ozner.oznerlibrary.R;
import com.ozner.util.ByteUtil;
import com.ozner.util.Helper;
import com.ozner.wifi.mxchip.MXChipIO;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by xzyxd on 2015/11/2.
 */
public class AirPurifier_MXChip extends OznerDevice {
    public static final String ACTION_WATER_PURIFIER_STATUS_CHANGE = "com.ozner.air.purifier.status.change";
    public static final byte CMD_SET_PROPERTY=(byte)0x2;
    public static final byte CMD_REQUEST_PROPERTY=(byte)0x1;
    public static final byte CMD_RECV_PROPERTY=(byte)0x4;


    public static final byte PROPERTY_POWER = 0x00;
    public static final byte PROPERTY_SPEED = 0x01;
    public static final byte PROPERTY_LIGHT = 0x02;
    public static final byte PROPERTY_LOCK = 0x03;
    public static final byte PROPERTY_ONTIME = 0x04;
    public static final byte PROPERTY_PM25 = 0x11;
    public static final byte PROPERTY_TEMPERATURE = 0x12;
    public static final byte PROPERTY_VOC = 0x13;
    public static final byte PROPERTY_LIGHTSENSOR = 0x14;
    public static final byte PROPERTY_FILTER = 0x15;
    public static final byte PROPERTY_TIME = 0x16;
    public static final byte PROPERTY_PERIOD=0x17;

    public static final byte PROPERTY_MODEL = 0x21;
    public static final byte PROPERTY_TYPE = 0x22;
    public static final byte PROPERTY_MAINBOARD = 0x23;
    public static final byte PROPERTY_CONTROLBOARD = 0x24;


    public static final byte PROPERTY_MESSAGES = 0x25;
    public static final int ErrorValue = 0xffff;
    private static final int Timeout=5000;
    private static String SecureCode = "580c2783";
    final AirPurifierImp airPurifierImp = new AirPurifierImp();
    boolean isOffline = true;
    Timer autoUpdateTimer = null;
    HashMap<Byte, byte[]> property = new HashMap<>();
    Sensor sensor = new Sensor();
    AirStatus airStatus = new AirStatus();
    OnTimeInfo onTimeInfo = new OnTimeInfo();

    public AirPurifier_MXChip(Context context, String Address, String Model, String Setting) {
        super(context, Address, Model, Setting);
    }

    public Sensor sensor() {
        return sensor;
    }

    public AirStatus airStatus() {
        return airStatus;
    }

    @Override
    protected DeviceSetting initSetting(String Setting) {
        return super.initSetting(Setting);
    }

    @Override
    public void UpdateSetting() {
        Setting().put("ontime", onTimeInfo.ToJSON());

        super.UpdateSetting();
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
            oldIO.unRegisterStatusCallback(airPurifierImp);
            oldIO.setOnInitCallback(null);
        }
        if (newIO != null) {
            MXChipIO io = (MXChipIO) newIO;
            io.setSecureCode(SecureCode);
            io.setOnTransmissionsCallback(airPurifierImp);
            io.registerStatusCallback(airPurifierImp);
            io.setOnInitCallback(airPurifierImp);
        }
    }

    private void requestProperty(HashSet<Byte> propertys, OperateCallback<Void> cb) {
        if (super.connectStatus() != BaseDeviceIO.ConnectStatus.Connected) {
            if (cb != null)
                cb.onFailure(null);
            return;
        }
        byte[] bytes = new byte[14 + propertys.size()];
        bytes[0] = (byte) 0xfb;
        ByteUtil.putShort(bytes, (short) bytes.length, 1);
        bytes[3] = (byte) CMD_REQUEST_PROPERTY;
        byte[] macs = Helper.HexString2Bytes(this.Address().replace(":", ""));
        System.arraycopy(macs, 0, bytes, 4, 6);

        bytes[12] = (byte) propertys.size();
        int p = 13;
        for (Byte id : propertys) {
            bytes[p] = id;
            p++;
        }
        airPurifierImp.send(bytes, cb);
    }

    private void setProperty(byte propertyId, byte[] value, OperateCallback<Void> cb) {
        if (super.connectStatus() != BaseDeviceIO.ConnectStatus.Connected) {
            if (cb != null)
                cb.onFailure(null);
            return;
        }

        byte[] bytes = new byte[13 + value.length];

        bytes[0] = (byte) 0xfb;
        ByteUtil.putShort(bytes, (short) bytes.length, 1);
        bytes[3] =  CMD_SET_PROPERTY;
        byte[] macs = Helper.HexString2Bytes(this.Address().replace(":", ""));
        System.arraycopy(macs, 0, bytes, 4, 6);
        bytes[12] = propertyId;
        System.arraycopy(value, 0, bytes, 13, value.length);
        airPurifierImp.send(bytes, cb);
    }

    private int getIntValueByShort(short property) {
        synchronized (this.property) {
            if (this.property.containsKey(property)) {
                byte[] data = this.property.get(property);
                if (data != null)
                    return ByteUtil.getShort(data, 0);
                else
                    return ErrorValue;
            } else
                return ErrorValue;
        }
    }

    private boolean getBoolValue(short property) {
        synchronized (this.property) {
            if (this.property.containsKey(property)) {
                byte[] data = this.property.get(property);
                if (data != null)
                    return data[0] != 0;
                else
                    return false;
            } else
                return false;
        }
    }

    private int getIntValueByByte(short property) {
        synchronized (this.property) {
            if (this.property.containsKey(property)) {
                byte[] data = this.property.get(property);
                if (data != null)
                    return data[0];
                else
                    return ErrorValue;
            } else
                return ErrorValue;
        }
    }

    private int getIntValueByInt(short property) {
        synchronized (this.property) {
            if (this.property.containsKey(property)) {
                byte[] data = this.property.get(property);
                if (data != null)
                    return ByteUtil.getInt(data, 0);
                else
                    return ErrorValue;
            } else
                return ErrorValue;
        }
    }


    public enum SpeedValue {Auto, High, Mid, Low, Power}

    public class AirStatus {

        public boolean Power() {
            return getBoolValue(PROPERTY_POWER);
        }

        public void setPower(boolean power, OperateCallback<Void> cb) {
            setProperty(PROPERTY_POWER, new byte[]{power ? (byte) 1 : (byte) 0}, cb);
        }

        public int Speed() {

            return getIntValueByByte(PROPERTY_SPEED);
        }

        public void setSpeed(SpeedValue value, OperateCallback<Void> cb) {
            byte v = 0;
            switch (value) {
                case Auto:
                    v = 0;
                    break;
                case High:
                    v = 1;
                    break;
                case Mid:
                    v = 2;
                    break;
                case Low:
                    v = 3;
                    break;
                case Power:
                    v = 4;
                    break;
            }
            setProperty(PROPERTY_SPEED, new byte[]{v}, cb);
        }

        public int Light() {
            return getIntValueByByte(PROPERTY_LIGHT);
        }

        public int Lock() {
            return getIntValueByByte(PROPERTY_LOCK);
        }

        public void setLock(boolean lock, OperateCallback<Void> cb) {
            setProperty(PROPERTY_POWER, new byte[]{lock ? (byte) 1 : (byte) 0}, cb);
        }
    }


    public class Sensor {
        /**
         * PM2.5
         *
         * @return pm25
         */
        public int PM25() {
            return getIntValueByShort(PROPERTY_PM25);
        }

        /**
         * 环境温度
         *
         * @return 温度
         */
        public int Temperature() {
            return getIntValueByShort(PROPERTY_TEMPERATURE);
        }

        /**
         * VOC
         *
         * @return voc
         */
        public int VOC() {
            return getIntValueByShort(PROPERTY_VOC);
        }

        /**
         * 环境光亮度
         *
         * @return 亮度
         */
        public int LIGHT() {
            return getIntValueByInt(PROPERTY_LIGHTSENSOR);
        }
    }

    class AirPurifierImp implements
            BaseDeviceIO.OnTransmissionsCallback,
            BaseDeviceIO.StatusCallback,
            BaseDeviceIO.OnInitCallback {
        private boolean Respone=false;
        public AirPurifierImp() {
        }

        @Override
        public void onConnected(BaseDeviceIO io) {

        }

        @Override
        public void onDisconnected(BaseDeviceIO io) {
            cancelTimer();
            isOffline = true;

        }

        private void setNowTime() {
            byte[] time = new byte[4];
            ByteUtil.putInt(time, (int) (System.currentTimeMillis() / 1000), 0);
            setProperty(PROPERTY_TIME, time, null);
        }

        private void requestInfo() {
            HashSet<Byte> ps = new HashSet<>();
            ps.add(PROPERTY_MODEL);
            ps.add(PROPERTY_TYPE);
            ps.add(PROPERTY_CONTROLBOARD);
            ps.add(PROPERTY_MAINBOARD);
            requestProperty(ps, null);
        }

        private void requestStatus() {
            HashSet<Byte> ps = new HashSet<>();
            ps.add(PROPERTY_POWER);
            ps.add(PROPERTY_SPEED);
            ps.add(PROPERTY_LIGHT);
            ps.add(PROPERTY_LOCK);
            ps.add(PROPERTY_PM25);
            ps.add(PROPERTY_TEMPERATURE);
            ps.add(PROPERTY_VOC);
            ps.add(PROPERTY_LIGHTSENSOR);
            ps.add(PROPERTY_FILTER);
            requestProperty(ps, null);
        }
        private void setAutoReflash(short period,HashSet<Byte> propertys,OperateCallback<Void> cb)
        {
            byte[] bytes=new byte[3+propertys.size()];
            ByteUtil.putShort(bytes,period,0);
            bytes[2]=(byte)propertys.size();
            int i=3;
            for (Byte p : propertys)
            {
                bytes[i]=p;
                i++;
            }
            setProperty(PROPERTY_PERIOD,bytes,cb);

        }
        @Override
        public void onReady(BaseDeviceIO io) {
            //setNowTime();
            //requestInfo();

            FilterStatus filterStatus=new FilterStatus();
            filterStatus.lastTime=new Date();
            filterStatus.stopTime=new Date(filterStatus.lastTime.getTime()+1000*60*60*24);
            filterStatus.WorkTime=1000;
            filterStatus.MaxWorkTime=2000;

            setProperty(PROPERTY_FILTER,filterStatus.toBytes(),null);

            HashSet<Byte> list=new HashSet<>();
            list.add(PROPERTY_FILTER);
            requestProperty(list,null);
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

        class SendOperateCallbackProxy implements OperateCallback<Void>
        {
            OperateCallback<Void> callback;
            public SendOperateCallbackProxy(OperateCallback<Void> callback)
            {
                this.callback=callback;
            }

            @Override
            public void onSuccess(Void var1) {
                try {
                    waitObject(Timeout);
                    if (callback!=null) {
                        if (Respone)
                            callback.onSuccess(null);
                        else
                            this.callback.onFailure(null);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    if (callback!=null)
                        callback.onFailure(e);
                }

            }
            @Override
            public void onFailure(Throwable var1) {
                callback.onFailure(var1);
            }
        }

        private boolean send(byte[] data,OperateCallback<Void> cb)
        {
            if (IO()!=null)
            {
                Respone=false;
                return IO().send(data, new SendOperateCallbackProxy(cb));
            }else
                return false;
        }

        @Override
        public void onIORecv(byte[] bytes) {
            if ((bytes == null) || (bytes.length<= 0)) {
                return;
            }

            if (bytes[0] != (byte) 0xFA) return;

            int len = ByteUtil.getShort(bytes, 1);
            if (len <= 0) return;
            byte cmd = bytes[3];
            switch (cmd)
            {
                case CMD_RECV_PROPERTY:
                    int count = bytes[12];
                    int p = 13;
                    HashMap<Byte, byte[]> set = new HashMap<>();
                    for (int i = 0; i < count; i++) {
                        byte id = bytes[p];
                        p++;

                        byte size = bytes[p];
                        p++;

                        byte[] data = new byte[size];

                        if (p >= bytes.length) return;
                        if (p + size > bytes.length) return;

                        System.arraycopy(bytes, p, data, 0, size);
                        p += size;
                        set.put(id, data);
                    }
                    synchronized (property) {
                        for (Byte id : set.keySet()) {
                            property.put(id, set.get(id));
                        }
                        if (property.containsKey(PROPERTY_FILTER))
                        {
                            byte[] b=property.get(PROPERTY_FILTER);
                            FilterStatus fs=new FilterStatus();
                            fs.fromBytes(b);
                        }
                    }

                    Respone=true;
                    setObject();
                    break;
            }

        }


        @Override
        public boolean onIOInit() {
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
