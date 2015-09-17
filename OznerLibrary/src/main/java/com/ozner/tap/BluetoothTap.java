package com.ozner.tap;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;

import com.ozner.cup.CupSensor;
import com.ozner.device.OznerBluetoothDevice;
import com.ozner.device.TapFirmwareTools;
import com.ozner.util.ByteUtil;
import com.ozner.util.dbg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * @author zhiyongxu
 *         水探头蓝牙控制对象
 * @category 水探头
 */
public class BluetoothTap extends OznerBluetoothDevice {
    CupSensor mSensor = new CupSensor();
    boolean mRequestSensorFlag = false;
    boolean mConfigSending = false;
    long mRequestRecordFlag = 0;
    /**
     * 收到传感器数据
     */
    public final static String ACTION_BLUETOOTHTAP_SENSOR = "com.ozner.tap.bluetooth.sensor";
    /**
     * 收到单条饮水记录
     */
    public final static String ACTION_BLUETOOTHTAP_RECORD = "com.ozner.tap.bluetooth.record";
    final static String ACTION_BLUETOOTHTAP_RECORD_RECV_COMPLETE = "com.ozner.tap.bluetooth.record.recv.complete";
    /**
     * 水探头连接成功
     */
    public final static String ACTION_BLUETOOTHTAP_CONNECTED = "com.ozner.tap.bluetooth.connected";
    /**
     * 水探头连接断开
     */
    public final static String ACTION_BLUETOOTHTAP_DISCONNECTED = "com.ozner.tap.bluetooth.disconnected";
    /**
     * 收到水探头数据
     */
    public final static String ACTION_BLUETOOTHTAP_DEVICE = "com.ozner.tap.bluetooth.device";
    static final byte opCode_ReadSensor = 0x12;
    static final byte opCode_ReadSensorRet = (byte) 0xA2;

    static final byte opCode_ReadTDSRecord = 0x17;
    static final byte opCode_ReadTDSRecordRet = (byte) 0xA7;
    static final byte opCode_GetFirmwareSum = (byte) 0xc5;
    static final byte opCode_GetFirmwareSumRet = (byte) 0xc5;

    static final byte opCode_SetDetectTime = 0x10;

    TapRecord mlastRecord = null;

    public BluetoothTap(Context context, BluetoothCloseCallback callback, BluetoothDevice device, String Platform, String Model, long Firmware) {
        super(context, callback, device, Platform, Model, Firmware);
    }


    @Override
    protected void sendBroadcastConnected() {
        super.sendBroadcastConnected();
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTHTAP_CONNECTED);
        intent.putExtra("Address", getDevice().getAddress());
        getContext().sendBroadcast(intent);
    }

    @Override
    protected void sendroadcastDisconnected() {
        super.sendroadcastDisconnected();
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTHTAP_DISCONNECTED);
        intent.putExtra("Address", getDevice().getAddress());
        getContext().sendBroadcast(intent);
    }

    @Override
    protected void sendBroadcastDeviceInfo() {
        super.sendBroadcastDeviceInfo();
        Intent intent = new Intent(ACTION_BLUETOOTHTAP_DEVICE);
        intent.putExtra("Address", getDevice().getAddress());
        intent.putExtra("Model", getModel());
        intent.putExtra("Firmware", getFirmware());
        getContext().sendBroadcast(intent);
    }

    @Override
    protected void onRequest(BluetoothGatt gatt) throws InterruptedException {
        getSensor(gatt);
        Thread.sleep(100);
        getRecord(gatt);
    }

    ArrayList<TapRecord> mRecords = new ArrayList<TapRecord>();


    @Override
    public void updateCustomData(int CustomType, byte[] data) {
        if (data != null) {
            if (data.length > 0) {
                setBindMode(data[0] == 1 ? true : false);
            }
        }
    }

    /**
     * 获取传感器对象
     */
    @Override
    public Object getSensor() {
        return mSensor;
    }

    /**
     * 获取上次请求得到的TDS数据集合
     *
     * @return TDS数据集合
     */
    TapRecord[] GetReocrds() {
        synchronized (this) {
            return mRecords.toArray(new TapRecord[0]);
        }
    }

    private boolean getSensor(BluetoothGatt gatt) throws InterruptedException {
        if (sendOpCode(gatt, opCode_ReadSensor)) {
            waitNotfify(500);
            byte[] buff = popRecvPacket();
            if (buff == null) return false;
            if (buff.length > 0) {
                byte opCode = buff[0];
                byte[] data = Arrays.copyOfRange(buff, 1, buff.length);
                if (opCode == opCode_ReadSensorRet) {
                    synchronized (this) {
                        mSensor.FromBytes(data, 0);
                    }
                    Intent intent = new Intent(ACTION_BLUETOOTHTAP_SENSOR);
                    intent.putExtra("Address", getAddress());
                    intent.putExtra("Sensor", data);
                    getContext().sendBroadcast(intent);
                    return true;
                } else
                    return false;
            } else
                return false;
        } else
            return false;
    }

    HashSet<String> dataHash = new HashSet<String>();

    private boolean getRecord(BluetoothGatt gatt) throws InterruptedException {
        if (sendOpCode(gatt, opCode_ReadTDSRecord)) {
            waitNotfify(200);
            int lastCount = getRecvPacketCount();
            //循环接收数据,直到1秒内没数据接收
            while (true) {
                Thread.sleep(1000);
                if (lastCount == getRecvPacketCount())
                    break;
            }
            ArrayList<TapRecord> records = new ArrayList<>();
            byte[] buff = popRecvPacket();

            while (buff != null) {
                byte opCode = buff[0];
                byte[] data = Arrays.copyOfRange(buff, 1, buff.length);
                if (opCode == opCode_ReadTDSRecordRet) {
                    TapRecord record = new TapRecord();
                    record.FromBytes(data);

                    if (record.TDS > 0) {
                        String hashKey = String.valueOf(record.time.getTime()) + "_" + String.valueOf(record.TDS);
                        if (dataHash.contains(hashKey)) {
                            dbg.e("收到水杯重复数据");
                            break;
                        } else
                            dataHash.add(hashKey);

                        records.add(0, record);
                        Intent intent = new Intent(ACTION_BLUETOOTHTAP_RECORD);
                        intent.putExtra("Address", getAddress());
                        intent.putExtra("Record", data);
                        getContext().sendBroadcast(intent);
                    }
                }
                buff = popRecvPacket();
            }

            if (records.size() > 0) {

                synchronized (mRecords) {
                    mRecords.clear();
                    mRecords.addAll(records);
                    Intent comp_intent = new Intent(
                            ACTION_BLUETOOTHTAP_RECORD_RECV_COMPLETE);
                    comp_intent.putExtra("Address", getAddress());
                    mlastRecord = records.get(records.size() - 1);
                    getContext().sendBroadcast(comp_intent);
                }


                return true;
            } else
                return true;

        } else
            return false;
    }


    @Override
    protected boolean sendSetting(BluetoothGatt gatt) throws InterruptedException {
        TapSetting setting = (TapSetting) getSetting();
        if (setting == null)
            return false;
        byte[] data = new byte[16];

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

//		dbg.i(getAddress() + " 写入监测设置", getContext());
        if (send(gatt, opCode_SetDetectTime, data)) {
            return super.sendSetting(gatt);
        } else
            return false;
    }


    @Override
    protected boolean startFirmwareUpdate(BluetoothGatt gatt) throws InterruptedException {
        try {
            onFirmwareUpdateStart();

            TapFirmwareTools firmware = new TapFirmwareTools(firmwareFile, this.getAddress());
            if (firmware.bytes.length > 31 * 1024) {
                onFirmwareFail();
                return false;
            }
            if (!(firmware.Platform.equals("T01") || firmware.Platform.equals("T02") || (firmware.Platform.equals("T03")))) {
                onFirmwareFail();
                return false;
            }
//            if (!getPlatform().equals(firmware.Platform))
//            {
//                onFirmwareFail();
//                return false;
//            }

            if (firmware.Firmware == this.getFirmware()) {
                onFirmwareFail();
                return false;
            }


            byte[] data = new byte[20];
            data[0] = (byte) 0x89;

            for (int i = 0; i < firmware.Size; i += 8) {
                int p = i + 0x17c00;
                ByteUtil.putInt(data, p, 1);
                System.arraycopy(firmware.bytes, i, data, 5, 8);
                if (!send(gatt, data)) {
                    onFirmwareFail();
                    return false;
                } else {
                    onFirmwarePosition(i, firmware.Size);
                }
            }

            Thread.sleep(1000);
            byte[] checkSum = new byte[5];
            checkSum[0] = opCode_GetFirmwareSum;
            ByteUtil.putInt(checkSum, firmware.Size, 1);
            if (send(gatt, checkSum)) {
                Thread.sleep(200);
                checkSum = popRecvPacket();
                if (checkSum[0] == opCode_GetFirmwareSumRet) {
                    long sum = ByteUtil.getUInt(checkSum, 1);
                    if (sum == firmware.Cheksum) {
                        byte[] update = new byte[5];
                        ByteUtil.putInt(data, firmware.Size, 0);
                        if (send(gatt, (byte) 0xc3, update)) {
                            onFirmwareComplete();
                            Thread.sleep(5000);
                            return true;
                        } else {
                            onFirmwareFail();
                            return false;
                        }
                    } else {
                        onFirmwareFail();
                        return false;
                    }

                } else {
                    onFirmwareFail();
                    return false;
                }
            } else {
                onFirmwareFail();
                return false;
            }


        } catch (Exception e) {
            onFirmwareFail();
            return false;
        }
    }


    @Override
    public float getPowerPer() {
        if (mSensor == null) return -1;
        if (mSensor.BatteryFix > 3000) return 1;
        if (mSensor.BatteryFix >= 2900) return 0.9f;
        if (mSensor.BatteryFix >= 2800) return 0.7f;
        if (mSensor.BatteryFix >= 2700) return 0.5f;
        if (mSensor.BatteryFix >= 2600) return 0.3f;
        if (mSensor.BatteryFix >= 2500) return 0.17f;
        if (mSensor.BatteryFix >= 2400) return 0.16f;
        if (mSensor.BatteryFix >= 2300) return 0.15f;
        if (mSensor.BatteryFix >= 2200) return 0.07f;
        if (mSensor.BatteryFix >= 2100) return 0.03f;
        if (mSensor.BatteryFix == 0) return -1;
        return 0f;
    }

}
