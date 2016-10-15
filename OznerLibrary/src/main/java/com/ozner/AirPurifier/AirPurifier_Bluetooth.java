package com.ozner.AirPurifier;

import android.content.Context;
import android.content.Intent;
import android.text.format.Time;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.OperateCallback;
import com.ozner.oznerlibrary.R;
import com.ozner.util.ByteUtil;
import com.ozner.util.Helper;
import com.ozner.util.dbg;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by zhiyongxu on 15/11/17.
 */
public class AirPurifier_Bluetooth extends AirPurifier {
    private static final int defaultAutoUpdatePeriod=5000;
    private static final byte opCode_UpdateTime = 0x40;
    private static final byte opCode_Request = 0x20;
    private static final byte opCode_UpdateStatus = 0x10;
    private static final byte opCode_StatusResp = 0x21;
    private static final byte opCode_SensorResp = 0x22;
    private static final byte opCode_A2DPResp = 0x24;
    private static final byte opCode_FilterResp = 0x23;
    private static final byte opCode_ResetFilter = 0x41;
    private static final byte opCode_A2DPPair= 0x42;



    private static final byte type_status = 1;
    private static final byte type_sensor = 2;
    private static final byte type_filter = 3;
    private static final byte type_a2dp = 4;


    final AirPurifierIMP airPurifierIMP = new AirPurifierIMP();
    final Sensor sensor = new Sensor();
    final Status status = new Status();
    A2DP a2dp=null;
    int requestCount=0;
    final FilterStatus filterStatus = new FilterStatus();
    /**
     * 返回传感器状态
     *
     * @return
     */
    public Sensor sensor() {
        return sensor;
    }

    /**
     * 返回设备状态
     *
     * @return
     */
    public Status status() {
        return status;
    }

    public A2DP a2dp()
    {
        return a2dp;
    }

    public AirPurifier_Bluetooth(Context context, String Address, String Type, String Setting) {
        super(context, Address, Type, Setting);
        a2dp=new A2DP(context);
        a2dp.setCallback(airPurifierIMP);
    }

    @Override
    public int getTimerDelay() {
        return defaultAutoUpdatePeriod;
    }

    @Override
    public String Model() {
        return "FLT001";
    }

    @Override
    protected String getDefaultName() {
        return context().getString(R.string.air_bluetooth_purifier_name);
    }

    @Override
    public Class<?> getIOType() {
        return BluetoothIO.class;
    }

    @Override
    protected void doSetDeviceIO(BaseDeviceIO oldIO, BaseDeviceIO newIO) {
        if (oldIO != null) {
            oldIO.setOnInitCallback(null);
            oldIO.unRegisterStatusCallback(airPurifierIMP);
            oldIO.setOnTransmissionsCallback(null);
            oldIO.setCheckTransmissionsCompleteCallback(null);
        }
        if (newIO != null) {
            newIO.setOnTransmissionsCallback(airPurifierIMP);
            newIO.setOnInitCallback(airPurifierIMP);
            newIO.registerStatusCallback(airPurifierIMP);
            newIO.setCheckTransmissionsCompleteCallback(airPurifierIMP);
            //firmwareTools.bind((BluetoothIO) newIO);
        }
    }




    @Override
    public String toString() {
        return status().toString() + "\n" + sensor().toString();
    }

    private String getValue(int value) {
        if (value == 0xFFFF) {
            return "-";
        } else
            return String.valueOf(value);
    }

    /**
     * 重置滤芯
     * @param cb
     */
    public void ResetFilter(OperateCallback<Void> cb) {
//        FilterStatus filterStatus = new FilterStatus();
//        filterStatus.lastTime = new Date();
//        filterStatus.workTime = 0;
//        Calendar calendar = Calendar.getInstance();
//        calendar.add(Calendar.YEAR, 1);
//        filterStatus.stopTime = calendar.getTime();
//        filterStatus.maxWorkTime = 60 * 1000;

        Date time = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);
        //time.setToNow();

        byte[] data = new byte[16];
        data[0] = (byte) (calendar.get(Calendar.YEAR) - 2000);
        data[1] = (byte) (calendar.get(Calendar.MONTH)+1);
        data[2] = (byte) calendar.get(Calendar.DAY_OF_MONTH);
        data[3] = (byte) calendar.get(Calendar.HOUR);
        data[4] = (byte) calendar.get(Calendar.MINUTE);
        data[5] = (byte) calendar.get(Calendar.SECOND);


        calendar.add(Calendar.MONTH,3);

        data[6] = (byte) (calendar.get(Calendar.YEAR) - 2000);
        data[7] = (byte) (calendar.get(Calendar.MONTH) +1);
        data[8] = (byte) calendar.get(Calendar.DAY_OF_MONTH);
        data[9] = (byte) calendar.get(Calendar.HOUR);
        data[10] = (byte) calendar.get(Calendar.MINUTE);
        data[11] = (byte) calendar.get(Calendar.SECOND);

        ByteUtil.putInt(data,60*1000,12);
        airPurifierIMP.send(opCode_ResetFilter, data, cb);
        airPurifierIMP.requestFilter();
    }
    public class Status {
        boolean power = false;
        byte RPM=0;
        /**
         * 电源状态
         *
         * @return TRUE=开,FALSE=关
         */
        public boolean Power() {
            return power;
        }
        public int RPM()
        {
            return RPM;
        }

        private void sendStatus(OperateCallback<Void> cb)
        {
            airPurifierIMP.send(opCode_UpdateStatus, new byte[]{(power ? (byte) 1 : (byte) 0),RPM}, cb);
        }

        public void setPower(boolean power, OperateCallback<Void> cb) {
            this.power=power;
            sendStatus(cb);
        }

        /**
         * 设置风扇转速
         * @param RPM 风扇转速百分比1-100
         */
        public void setRPM(byte RPM, OperateCallback<Void> cb)
        {
            if (RPM>100) RPM=100;
            if (RPM<0) RPM=1;
            this.RPM=RPM;
            sendStatus(cb);
        }
        /**
         * 滤芯状态,如果没收到返回NULL
         *
         * @return 没收到状态时返回NULL
         */
        public FilterStatus FilterStatus() {
            return filterStatus;
        }


        @Override
        public String toString() {
            return String.format("Power:%b RPM:%d",Power(),RPM());
        }
    }

    public class Sensor {
        int temperature = 0xffff;
        int humidity = 0xffff;
        int pm25 = 0xffff;

        /**
         * 环境温度
         *
         * @return 温度
         */
        public int Temperature() {
            return temperature;
        }

        /**
         * 湿度
         *
         * @return 湿度
         */
        public int Humidity() {
            return humidity;
        }

        /**
         * pm2.5
         *
         * @return pm2.5
         */
        public int PM25() {
            return pm25;
        }

        public FilterStatus FilterStatus()
        {
            return filterStatus;
        }
        @Override
        public String toString() {
            return String.format("temperature:%s humidity:%s pm25:%s",
                    getValue(Temperature()), getValue(Humidity()), getValue(PM25()));
        }
    }

    @Override
    protected void doTimer() {
        if (IO() == null) return;
        if ((requestCount % 2) == 0) {
            airPurifierIMP.requestStatus();
        } else {
            airPurifierIMP.requestSensor();
        }
        requestCount++;
    }

    private class AirPurifierIMP implements
            BaseDeviceIO.StatusCallback,
            BaseDeviceIO.OnInitCallback,
            BaseDeviceIO.OnTransmissionsCallback,
            BaseDeviceIO.CheckTransmissionsCompleteCallback,
            A2DP.A2DPCallback
    {

        public AirPurifierIMP() {
            super();
        }


        public void OpenA2DP(OperateCallback<Void> cb)
        {
            if (Helper.StringIsNullOrEmpty(a2dp.mac))
            {
                cb.onFailure(null);
            }
            if (IO()!=null && (IO().isReady()))
            {
                airPurifierIMP.send(opCode_A2DPPair, null,cb);
            }else
                cb.onFailure(null);
        }

        int requestCount = 0;

        private byte[] makePacket(byte opCode, byte[] data) {
            int len = data.length + 2;
            byte[] buffer = new byte[len];
            buffer[0] = opCode;
            System.arraycopy(data, 0, buffer, 1, data.length);
            byte checksum = 0;

            for (int i = 0; i < len - 1; i++) {
                checksum += buffer[i];
            }
            buffer[len - 1] = checksum;
            return buffer;
        }



        private boolean send(byte opCode, byte[] data, OperateCallback<Void> cb) {
            return IO() != null && IO().send(makePacket(opCode, data), cb);
        }

        public boolean requestStatus() {
            return send(opCode_Request, new byte[]{type_status}, null);
        }

        public boolean requestSensor() {
            return send(opCode_Request, new byte[]{type_sensor}, null);
        }

        private boolean requestA2DP() {
            return send(opCode_Request, new byte[]{type_a2dp}, null);
        }

        private boolean requestFilter() {
            return send(opCode_Request, new byte[]{type_filter}, null);
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
            return send(opCode_UpdateTime, data, null);
        }


        @Override
        public boolean onIOInit() {
            try {
                if (!sendTime())
                    return false;
                Thread.sleep(100);

                if (!requestStatus())
                    return false;
                Thread.sleep(100);

                if (!requestA2DP())
                    return false;
                Thread.sleep(100);

                if (!requestFilter())
                    return false;
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

            switch (opCode) {
                case opCode_StatusResp: {
                    status.power = bytes[1] == 1;
                    if (bytes.length>18)
                        status.RPM=bytes[18];

                    filterStatus.lastTime= new Date(bytes[8] + 2000 - 1900, bytes[9] - 1, bytes[10], bytes[11],
                            bytes[12], bytes[13]);
                    filterStatus.workTime =ByteUtil.getInt(bytes, 14);

                    Intent intent = new Intent(ACTION_AIR_PURIFIER_STATUS_CHANGED);
                    intent.putExtra(Extra_Address, Address());
                    context().sendBroadcast(intent);
                    doUpdate();
                    break;
                }
                case opCode_SensorResp: {
                    sensor.temperature = bytes[1];
                    sensor.humidity = bytes[2];
                    sensor.pm25 = ByteUtil.getShort(bytes, 3);
                    Intent intent = new Intent(ACTION_AIR_PURIFIER_SENSOR_CHANGED);
                    intent.putExtra(Extra_Address, Address());
                    context().sendBroadcast(intent);
                    doUpdate();

                    break;
                }
                case opCode_FilterResp: {
                    Date time = new Date(bytes[7] + 2000 - 1900, bytes[8] - 1, bytes[9], bytes[10],
                            bytes[11], bytes[12]);
                    filterStatus.stopTime=time;
                    filterStatus.maxWorkTime =ByteUtil.getInt(bytes, 13);
                    break;
                }
                case opCode_A2DPResp: {
                    a2dp.load(bytes);
                    Setting().put("A2DP",a2dp.mac);
                    break;
                }
            }
        }

        @Override
        public void onConnected(BaseDeviceIO io) {

        }

        @Override
        public void onDisconnected(BaseDeviceIO io) {

        }


        @Override
        public void onReady(BaseDeviceIO io) {
            requestSensor();
        }

        @Override
        public boolean CheckTransmissionsComplete(BaseDeviceIO io) {
            return true;
        }
    }
}
