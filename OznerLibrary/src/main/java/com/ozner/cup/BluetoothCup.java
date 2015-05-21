package com.ozner.cup;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import com.ozner.bluetooth.AutoRefashDevice;
import com.ozner.bluetooth.BluetoothScan;
import com.ozner.bluetooth.BaseBluetoothDevice;
import com.ozner.bluetooth.BaseBluetoothDevice.BluetoothCloseCallback;
import com.ozner.device.OznerBluetoothDevice;
import com.ozner.util.ByteUtil;
import com.ozner.util.dbg;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
/**
 * 水杯蓝牙控制对象
 * @category 智能杯
 * @author zhiyongxu
 *
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
	 * 
	 * 收到设备信息
	 * 
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

	public BluetoothCup(Context context, BluetoothDevice device,
			BluetoothCloseCallback callback) {
		super(context, device, callback);
	}

	boolean mRequestSensorFlag = false;
	boolean mRequestGravityFlag = false;
	boolean mConfigSending = false;
	long mRequestRecordFlag = 0;
	/**
	 * 自动刷机传感器
	 */
	public static final int AuotUpdate_Sensor = 1;

	static final int AuotUpdate_Gravity = 2;
	/**
	 * 自动刷新饮水记录
	 */
	public static final int AuotUpdate_Record = 4;

	public static final int AuotUpdate_All = 7;
	int mAutoUpdateType = AuotUpdate_Sensor | AuotUpdate_Record;

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

	boolean mSettingUpdating = false;

	ArrayList<CupRecord> mRecords = new ArrayList<CupRecord>();
	CupSensor mSensor = new CupSensor();
	CupGravity mGravity = new CupGravity();
	CupRecord lastCupRecord = null;

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

	@Override
	public String getSerial() {
		return getDevice().getAddress();
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
		synchronized (this) {
			return mRecords.toArray(new CupRecord[0]);
		}
	}

	@Override
	public boolean isBusy() {
		return (mRequestGravityFlag || mRequestSensorFlag || (mRequestRecordFlag > 0));
	}

	/**
	 * 请求传感器信息
	 * 
	 * @return TRUE成功，FALSE失败
	 */
	public boolean requestSensor() {
		if (isBusy())
			return false;
		dbg.i(getAddress() + " 请求传感器", getContext());
		mRequestSensorFlag = true;
		return sendOpCode(opCode_ReadSensor);
	}

	boolean requestGravity() {
		if (isBusy())
			return false;
		dbg.i(getAddress() + " 请求加速度", getContext());
		mRequestGravityFlag = true;
		return sendOpCode(opCode_ReadGravity);
	}

	/**
	 * 请求饮水记录
	 * 
	 * @return TRUE成功,FALSE失败
	 */
	public boolean requestRecord() {
		if (isBusy())
			return false;
		mRequestRecordFlag = new Date().getTime();
		mRecords.clear();
		dbg.i(getAddress() + " 请求饮水记录", getContext());
		return sendOpCode(opCode_ReadRecord);
	}

	// boolean mLastIsHandstand = false;

	@Override
	public void updateCustomData(int CustomType, byte[] data) {
		if (CustomType == AD_CustomType_Gravity) {
			mGravity.FromBytes(data, 0);
			if (mGravity.IsHandstand())
				setBindMode(true);
			else
				setBindMode(false);
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
	protected boolean updateSetting() {
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
		return send(opCode_SetRemind, data);
	}

	@Override
	protected void onAutoUpdate() {
		if ((mAutoUpdateType & AuotUpdate_Sensor) == AuotUpdate_Sensor) {
			requestSensor();
			sleep();
		}
		if ((mAutoUpdateType & AuotUpdate_Gravity) == AuotUpdate_Gravity) {
			requestGravity();
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

	HashSet<String> dataHash=new HashSet<String> ();
	@SuppressLint("DefaultLocale")
	@Override
	protected void onData(byte opCode, byte[] Data) {
		super.onData(opCode, Data);
		switch (opCode) {

		case opCode_ReadSensorRet: {
			if (Data != null) {
				mRequestSensorFlag = false;
				synchronized (this) {
					mSensor.FromBytes(Data);
				}

				dbg.i(String
						.format("收到数据: BatteryFix:%d TemperatureFix:%d WeigthFix:%d TDSFix:%d",
								mSensor.BatteryFix, mSensor.TemperatureFix,
								mSensor.WeigthFix, mSensor.TDSFix),
						getContext());

				Intent intent = new Intent(ACTION_BLUETOOTHCUP_SENSOR);
				intent.putExtra("Address", getAddress());
				intent.putExtra("Sensor", Data);
				getContext().sendBroadcast(intent);
				break;
			}
		}

		case opCode_ReadGravityRet: {
			if (Data != null) {
				mRequestGravityFlag = false;
				synchronized (this) {
					mGravity.FromBytes(Data);
				}
				dbg.i(String.format("%s 收到加速度数据 x:%f y:%f z:%f", getAddress(),
						mGravity.x, mGravity.y, mGravity.z), getContext());
				Intent intent = new Intent(ACTION_BLUETOOTHCUP_GRAVITY);
				intent.putExtra("Address", getAddress());
				intent.putExtra("Gravity", Data);
				getContext().sendBroadcast(intent);
			}
			break;
		}

		case opCode_ReadRecordRet: {
			if (Data != null) {
				mRequestRecordFlag = new Date().getTime();
				CupRecord record = new CupRecord();
				record.FromBytes(Data);
				String hashKey=String.valueOf(record.time.getTime())+"_"+String.valueOf(record.Vol);
				if (dataHash.contains(hashKey))
				{
					dbg.e("收到水杯重复数据");
					break;
				}else
					dataHash.add(hashKey);
				
				if (lastCupRecord != null) {
					if (record.time.equals(lastCupRecord.time)) {
						break;
					}
				}
				if (record.Vol > 0) {
					dbg.i("收到饮水数据 Vol:%d Temp:%d TDS:%d Index:%d Count:%d",
							record.Vol, record.Temperature,record.TDS,record.Index,record.Count);
					synchronized (this) {
						mRecords.add(record);
					}
				}
				dbg.i(String.format("%s 收到饮水数据%d\\%d", getAddress(),
						record.Index, record.Count), getContext());
				Intent intent = new Intent(ACTION_BLUETOOTHCUP_RECORD);
				intent.putExtra("Address", getAddress());
				intent.putExtra("Record", Data);
				getContext().sendBroadcast(intent);
				if (record.Index >= record.Count) {
					mRequestRecordFlag = 0;
					if (mRecords.size() > 0) {
						
						Intent comp_intent = new Intent(
								ACTION_BLUETOOTHCUP_RECORD_RECV_COMPLETE);
						comp_intent.putExtra("Address", getAddress());
						comp_intent.putExtra("Record", Data);
						dbg.i("send ACTION_BLUETOOTHCUP_RECORD_RECV_COMPLETE:"+mRecords.size());
						synchronized (this) {
							ArrayList<CupRecord> tmp=new ArrayList<CupRecord>(mRecords);
							mRecords.clear();
							for (int i=tmp.size()-1;i>=0;i--)
							{
								mRecords.add(tmp.get(i));
							}
							lastCupRecord=mRecords.get(mRecords.size()-1);
						}
						getContext().sendBroadcast(comp_intent);
					}
				}
			} else {

			}
			break;
		}
		}
	}
	@Override
	public float getPowerPer() {
		if (mSensor==null) return -1;
		
		int battery = mSensor.BatteryFix;
		if (battery < 3200) {
			battery = 3200;
		}
		if (battery > 4100) {
			battery = 4100;
		}
		return (battery-3200)/(4100-3200f);
	}
	public void sensorZero()
	{
		sendOpCode(0x8c);
	}
	@Override
	public Object getSensor() {
		return mSensor;
	}

	@Override
	protected void onReadly() {
		super.onReadly();
		sendOpCode(opCode_ReadSensor);
		sleep();
		sendOpCode(opCode_ReadRecord);
		//sleep();
		//requestSensor();
		//sleep();
		//requestRecord();
	}

}
