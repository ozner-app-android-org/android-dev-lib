package com.ozner.cup;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;

import com.ozner.device.FirmwareTools;
import com.ozner.device.OznerBluetoothDevice;
import com.ozner.util.ByteUtil;
import com.ozner.util.dbg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * 水杯蓝牙控制对象
 *
 * @author zhiyongxu
 * @category 智能杯
 */
public class BluetoothCup extends OznerBluetoothDevice {
    /**
     * 收到单条饮水记录
     */
    public final static String ACTION_BLUETOOTHCUP_RECORD = "com.ozner.cup.bluetooth.record";
    final static String ACTION_BLUETOOTHCUP_RECORD_RECV_COMPLETE = "com.ozner.cup.bluetooth.record.recv.complete";
    /**
     * 水杯连接成功
     */
    public final static String ACTION_BLUETOOTHCUP_CONNECTED = "com.ozner.cup.bluetooth.connected";

    /**
     * 水杯连接断开
     */
    public final static String ACTION_BLUETOOTHCUP_DISCONNECTED = "com.ozner.cup.bluetooth.disconnected";
    /**
     * 收到设备信息
     */
    public final static String ACTION_BLUETOOTHCUP_DEVICE = "com.ozner.cup.bluetooth.device";

    /**
     * @deprecated
     */
    public final static String ACTION_BLUETOOTHCUP_GRAVITY = "com.ozner.cup.bluetooth.gravity";
    /**
     * 收到传感器信息
     */
    public final static String ACTION_BLUETOOTHCUP_SENSOR = "com.ozner.cup.bluetooth.sensor";

    /**
     * 水杯倒立
     */
    public final static String ACTION_BLUETOOTHCUP_GRAVITY_CHANGE = "com.ozner.cup.bluetooth.gravity.change";

    static final byte opCode_SetRemind = 0x11;
    static final byte opCode_ReadSensor = 0x12;
    static final byte opCode_ReadSensorRet = (byte) 0xA2;
    static final byte opCode_ReadGravity = 0x13;
    static final byte opCode_ReadGravityRet = (byte) 0xA3;
    static final byte opCode_ReadRecord = 0x14;
    static final byte opCode_ReadRecordRet = (byte) 0xA4;

    public static final byte AD_CustomType_Gravity = 0x1;

    ArrayList<CupRecord> mRecords = new ArrayList<CupRecord>();
    CupSensor mSensor = new CupSensor();
    CupGravity mGravity = new CupGravity();
    CupRecord lastCupRecord = null;

    public BluetoothCup(Context context, BluetoothCloseCallback callback, BluetoothDevice device, String Platform, String Model, long Firmware) {
        super(context, callback, device, Platform, Model, Firmware);
    }


    @Override
    protected void onRequest(BluetoothGatt gatt) throws InterruptedException {

        if (!getSensor(gatt)) {
            dbg.e("获取传感器失败:%s", getAddress());
        }
        if (!getRecord(gatt)) {
            dbg.e("获取饮水记录失败:%s", getAddress());
        }
        if (getIsBackgroundMode()) //如果是后台状态,等待30秒,直到水杯自动休眠断开
        {
            Thread.sleep(30000);
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
                    Intent intent = new Intent(ACTION_BLUETOOTHCUP_SENSOR);
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
        if (sendOpCode(gatt, opCode_ReadRecord)) {
            waitNotfify(200);
            int lastCount = getRecvPacketCount();
            //循环接收数据,直到1秒内没数据接收
            while (true) {
                Thread.sleep(100);
                int count = getRecvPacketCount();
                if (lastCount == count)
                    break;
                else
                    lastCount = count;
            }
            ArrayList<CupRecord> records = new ArrayList<>();
            byte[] buff = popRecvPacket();
            while (buff != null) {
                byte opCode = buff[0];
                byte[] data = Arrays.copyOfRange(buff, 1, buff.length);
                if (opCode == opCode_ReadRecordRet) {
                    CupRecord record = new CupRecord();
                    record.FromBytes(data);

                    if (record.Vol > 0) {
                        String hashKey = String.valueOf(record.time.getTime()) + "_" + String.valueOf(record.Vol);
                        if (dataHash.contains(hashKey)) {
                            dbg.e("收到水杯重复数据");
                            break;
                        } else
                            dataHash.add(hashKey);
                        records.add(0, record);
                        Intent intent = new Intent(ACTION_BLUETOOTHCUP_RECORD);
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
                    lastCupRecord = mRecords.get(mRecords.size() - 1);
                    Intent comp_intent = new Intent(
                            ACTION_BLUETOOTHCUP_RECORD_RECV_COMPLETE);
                    comp_intent.putExtra("Address", getAddress());
                    lastCupRecord = records.get(records.size() - 1);
                    getContext().sendBroadcast(comp_intent);
                }
                return true;
            } else
                return true;

        } else
            return false;
    }

    /**
     * 获取收到的最后一条饮水记录
     *
     * @return
     */
    public CupRecord getLastCupRecord() {
        synchronized (this) {
            return lastCupRecord;
        }
    }

    CupGravity GetGravity() {
        return mGravity;
    }

    /**
     * 获取最好一次请求的饮水记录集合
     *
     * @return
     */
    public CupRecord[] GetReocrds() {

        synchronized (mRecords) {
            return mRecords.toArray(new CupRecord[0]);
        }
    }


    @Override
    public void updateCustomData(int CustomType, byte[] data) {
        if (CustomType == AD_CustomType_Gravity) {
            mGravity.FromBytes(data, 0);
        }
    }


    @Override
    public Object getCustomObject() {
        return mGravity;
    }


    @Override
    public boolean isBindMode() {
        return mGravity.IsHandstand();
    }

    @Override
    protected boolean sendSetting(BluetoothGatt gatt) throws InterruptedException {
        CupSetting setting = (CupSetting) getSetting();
        if (setting == null)
            return false;
        byte[] data = new byte[19];
        ByteUtil.putInt(data, setting.remindStart(), 0);
        ByteUtil.putInt(data, setting.remindEnd(), 4);
        data[8] = (byte) setting.remindInterval();
        ByteUtil.putInt(data, setting.haloColor(), 9);
        data[13] = (byte) setting.haloMode();
        data[14] = (byte) setting.haloSpeed();
        data[15] = (byte) setting.haloConter();
        data[16] = (byte) setting.beepMode();
        data[17] = 0;// (byte) (isNewCup ? 1 : 0);
        data[18] = 0;
        dbg.i(getAddress() + " 写入提醒数据", getContext());
        return this.send(gatt, opCode_SetRemind, data);
    }


    @Override
    protected void sendBroadcastConnected() {
        super.sendBroadcastConnected();
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTHCUP_CONNECTED);
        intent.putExtra("Address", getDevice().getAddress());
        getContext().sendBroadcast(intent);
    }

    @Override
    protected void sendroadcastDisconnected() {
        super.sendroadcastDisconnected();
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTHCUP_DISCONNECTED);
        intent.putExtra("Address", getDevice().getAddress());
        getContext().sendBroadcast(intent);
    }

    @Override
    protected void sendBroadcastDeviceInfo() {
        super.sendBroadcastDeviceInfo();
        Intent intent = new Intent(ACTION_BLUETOOTHCUP_DEVICE);
        intent.putExtra("Address", getDevice().getAddress());
        intent.putExtra("Model", getModel());
        intent.putExtra("Firmware", getFirmware());
        getContext().sendBroadcast(intent);
    }


    @Override
    public float getPowerPer() {
        if (mSensor == null) return -1;

        int battery = mSensor.BatteryFix;
        if (battery < 3200) {
            battery = 3200;
        }
        if (battery > 4100) {
            battery = 4100;
        }
        return (battery - 3200) / (4100 - 3200f);
    }

    @Deprecated
    public void sensorZero() {
        //sendOpCode((byte)0x8c);
    }

    @Override
    public Object getSensor() {
        return mSensor;
    }

    private boolean eraseMCU(BluetoothGatt gatt) throws InterruptedException {
        if (send(gatt, (byte) 0x0c, new byte[]{0})) {
            Thread.sleep(1000);
            if (send(gatt, (byte) 0x0c, new byte[]{1})) {
                Thread.sleep(1000);
                return true;
            } else
                return false;
        } else
            return false;
    }

    @Override
    protected boolean startFirmwareUpdate(BluetoothGatt gatt) throws InterruptedException {
        try {
            onFirmwareUpdateStart();

            FirmwareTools firmware = new FirmwareTools(firmwareFile, this.getAddress());
            if (!(firmware.Platform.equals("C01") || firmware.Platform.equals("C02") || (firmware.Platform.equals("C03")))) {
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

            if (eraseMCU(gatt)) {
                byte[] data = new byte[20];
                data[0] = (byte) 0xc1;
                for (int i = 0; i < firmware.Size; i += 16) {
                    short p = (short) (i / 16);
                    ByteUtil.putShort(data, p, 1);
                    System.arraycopy(firmware.bytes, i, data, 3, 16);
                    if (!send(gatt, data)) {
                        onFirmwareFail();
                        return false;
                    } else {
                        onFirmwarePosition(i, firmware.Size);
                    }
                }
            }
            Thread.sleep(1000);
            byte[] data = new byte[19];
            ByteUtil.putInt(data, firmware.Size, 0);
            data[4] = 'S';
            data[5] = 'U';
            data[6] = 'M';
            ByteUtil.putInt(data, firmware.Cheksum, 7);
            if (send(gatt, (byte) 0xc3, data)) {
                onFirmwareComplete();
                Thread.sleep(5000);
                return true;
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
    protected boolean checkFirmwareUpdate() {
        return isUpdateFirmware;
    }

}
