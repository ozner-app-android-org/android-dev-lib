package com.ozner.tap;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import com.ozner.cup.CupSensor;
import com.ozner.device.OznerBluetoothDevice;
import com.ozner.util.ByteUtil;
import com.ozner.util.dbg;
/**
 * @category 水探头
 * @author zhiyongxu
 * 水探头蓝牙控制对象
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

	static final byte opCode_SetDetectTime = 0x10;

	public static final int AuotUpdate_Sensor = 1;
	public static final int AuotUpdate_Record = 4;
	public static final int AuotUpdate_All = 7;
	
	int mAutoUpdateType = AuotUpdate_Sensor | AuotUpdate_Record;
	TapRecord mlastRecord=null;

	/**
	 * 设置自动更新数据的类型
	 * 
	 * @param UpdateType
	 *            AuotUpdate_Sensor,AuotUpdate_Gravity,AuotUpdate_Record,
	 *            AuotUpdate_All
	 */
	public void setAuotUpdateType(int UpdateType) {
		mAutoUpdateType = UpdateType;
	}

	@Override
	protected void onAutoUpdate() {
		if ((mAutoUpdateType & AuotUpdate_Sensor) == AuotUpdate_Sensor) {
			requestSensor();
			sleep();
		}
		if ((mAutoUpdateType & AuotUpdate_Record) == AuotUpdate_Record) {
			requestRecord();
			sleep();
		}
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

	ArrayList<TapRecord> mRecords = new ArrayList<TapRecord>();

	public BluetoothTap(Context context, BluetoothDevice device,
			BluetoothCloseCallback callback) {
		super(context, device, callback);
	}

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
	 * @return TDS数据集合
	 */
	TapRecord[] GetReocrds() {
		synchronized (this) {
			return mRecords.toArray(new TapRecord[0]);
		}
	}
	
	@Override
	public boolean isBusy() {
		return (mRequestSensorFlag || (mRequestRecordFlag > 0));
	}
	/**
	 * 请求传感器信息
	 * @return 设备忙碌或失败时放回FALSE，成功返回TRUE
	 */
	public boolean requestSensor() {
		if (isBusy())
			return false;
		mRequestSensorFlag = true;
//		dbg.i(getAddress() + " 请求传感器记录", getContext());
		return sendOpCode(opCode_ReadSensor);
	}
	
	/**
	 * 请求TDS自动测试记录
	 * @return 设备忙碌或失败时放回FALSE，成功返回TRUE
	 */
	public boolean requestRecord() {
		if (isBusy())
			return false;
		mRequestRecordFlag = new Date().getTime();
		mRecords.clear();
//		dbg.i(getAddress() + " 请求TDS监测记录", getContext());
		return sendOpCode(opCode_ReadTDSRecord);
	}

	@Override
	protected boolean updateSetting() {
		TapSetting setting = (TapSetting) getSetting();
		if (setting == null)
			return false;
		byte[] data = new byte[16];

		if (setting.isDetectTime1()) {
			data[0] = (byte) (setting.DetectTime1() / 3600);
			data[1] = (byte) (setting.DetectTime1() % 3600 / 60);
			data[2] = (byte) (setting.DetectTime1() % 60);
			// ByteUtil.putInt(data, setting.DetectTime1(), 0);
		} else
		{
			data[0] =0;
			data[1] =0;
			data[2] =0;
		}
		if (setting.isDetectTime2()) {
			data[3] = (byte) (setting.DetectTime2() / 3600);
			data[4] = (byte) (setting.DetectTime2() % 3600 / 60);
			data[5] = (byte) (setting.DetectTime2() % 60);
			// ByteUtil.putInt(data, setting.DetectTime1(), 0);
		} else
		{
			data[3] =0;
			data[4] =0;
			data[5] =0;
		}

		if (setting.isDetectTime3()) {
			data[6] = (byte) (setting.DetectTime3() / 3600);
			data[7] = (byte) (setting.DetectTime3() % 3600 / 60);
			data[8] = (byte) (setting.DetectTime3() % 60);
			// ByteUtil.putInt(data, setting.DetectTime1(), 0);
		} else
		{
			data[6] =0;
			data[7] =0;
			data[8] =0;
		}

		if (setting.isDetectTime4()) {
			data[9] = (byte) (setting.DetectTime4() / 3600);
			data[10] = (byte) (setting.DetectTime4() % 3600 / 60);
			data[11] = (byte) (setting.DetectTime4() % 60);
			// ByteUtil.putInt(data, setting.DetectTime1(), 0);
		} else
		{
			data[9] =0;
			data[10] =0;
			data[11] =0;
		}

//		dbg.i(getAddress() + " 写入监测设置", getContext());
		return send(opCode_SetDetectTime, data);
	}

	@Override
	public float getPowerPer() {
		if (mSensor==null) return -1;
		if (mSensor.BatteryFix>3000) return 1;
		if (mSensor.BatteryFix>=2900) return 0.9f;
		if (mSensor.BatteryFix>=2800) return 0.7f;
		if (mSensor.BatteryFix>=2700) return 0.5f;
		if (mSensor.BatteryFix>=2600) return 0.3f;
		if (mSensor.BatteryFix>=2500) return 0.17f;
		if (mSensor.BatteryFix>=2400) return 0.16f;
		if (mSensor.BatteryFix>=2300) return 0.15f;
		if (mSensor.BatteryFix>=2200) return 0.07f;
		if (mSensor.BatteryFix>=2100) return 0.03f;
		if (mSensor.BatteryFix==0) return -1;
		return 0f;
	}
	@Override
	protected void onReadly() {
		super.onReadly();
		mRequestRecordFlag = 0;
		mRequestSensorFlag = false;
		sendOpCode(opCode_ReadSensor);
		sleep();
		sendOpCode(opCode_ReadTDSRecord);
	}
	HashSet<String> dataHash=new HashSet<String> ();
	@Override
	protected void ProcData(byte opCode, byte[] Data) {
		// TODO Auto-generated method stub
		super.ProcData(opCode, Data);
		switch (opCode) {
		case opCode_ReadSensorRet: {
			if (Data != null) {
				mRequestSensorFlag = false;
				synchronized (this) {
					mSensor.FromBytes(Data);
				}

//				dbg.i(String
//						.format("收到数据: BatteryFix:%d TemperatureFix:%d WeigthFix:%d TDSFix:%d",
//								mSensor.BatteryFix, mSensor.TemperatureFix,
//								mSensor.WeigthFix, mSensor.TDSFix),
//						getContext());

				Intent intent = new Intent(ACTION_BLUETOOTHTAP_SENSOR);
				intent.putExtra("Address", getAddress());
				intent.putExtra("Sensor", Data);
				getContext().sendBroadcast(intent);
				break;
			}
		}
		case opCode_ReadTDSRecordRet: {
			if (Data != null) {
				mRequestRecordFlag = new Date().getTime();
				TapRecord record = new TapRecord();
				
				record.FromBytes(Data);
				String hashKey=String.valueOf(record.time.getTime())+"_"+String.valueOf(record.TDS);
				if (dataHash.contains(hashKey))
				{
					dbg.e("收到探头重复数据");
					break;
				}else
					dataHash.add(hashKey);
				
				if (mlastRecord!=null)
				{
					if (record.time.equals(mlastRecord.time))
					{
						break;
					}
				}
				mlastRecord=record;
				if (record.TDS > 0) {
					//dbg.i("收到监测数据 TDS:%s", record.toString());
					synchronized (this) {
						mRecords.add(record);
					}
				}
				//dbg.i(String.format("%s 收到监测数据%d\\%d", getAddress(),
				//		record.Index, record.Count), getContext());
				Intent intent = new Intent(ACTION_BLUETOOTHTAP_RECORD);
				intent.putExtra("Address", getAddress());
				intent.putExtra("Record", Data);
				getContext().sendBroadcast(intent);
				if (record.Index >= record.Count) {
					mRequestRecordFlag = 0;
					if (mRecords.size() > 0) {
						Intent comp_intent = new Intent(
								ACTION_BLUETOOTHTAP_RECORD_RECV_COMPLETE);
						comp_intent.putExtra("Address", getAddress());
						comp_intent.putExtra("Record", Data);
						getContext().sendBroadcast(comp_intent);
					}
				}
			} else {
				
			}
			break;
		}
		}
	}
}
