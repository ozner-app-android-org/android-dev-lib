package com.ozner.device;

import android.content.Context;
import com.ozner.cup.BluetoothCup;
import com.ozner.util.SQLiteDB;
/**
 * @category Device
 * @author zhiyongxu
 * 浩泽设备基类
 */
public abstract class OznerDevice {
	private String mAddress;
	private OznerBluetoothDevice mBluetooth;
	private OznerContext mConetxt;
	private DeviceSetting mSetting;
	private String mSerial;
	private String mModel;
	/**
	 * 设备型号
	 * @return
	 */
	public String Model()
	{
		return mModel;
	}
	/**
	 * 设备序列号
	 * @return
	 */
	public String Serial()
	{
		return mSerial;
	}
	protected SQLiteDB getDB() {
		return mConetxt.getDB();
	}
	/**
	 * 设置对象
	 * @return
	 */
	public DeviceSetting Setting()
	{
		return mSetting;
	}
	/**
	 * 地址
	 * @return
	 */
	public String Address() {
		return mAddress;
	}
	/**
	 * 名称
	 * @return
	 */
	public String getName()
	{
		return mSetting.name();
	}

	protected Context getContext() {
		return mConetxt.getApplication();
	}
	/**
	 * 蓝牙控制对象
	 * @return NULL=没有蓝牙连接
	 */
	public OznerBluetoothDevice Bluetooth() {
		return mBluetooth;
	}
	/**
	 * 判断设备是否连接
	 * @return
	 */
	public boolean connected() {
		if (mBluetooth != null) {
			return mBluetooth.getStatus()==BluetoothCup.STATE_CONNECTED;
		} else
			return false;
	}
	protected DeviceSetting initSetting(String Setting)
	{
		DeviceSetting setting = new DeviceSetting();
		setting.load(Setting);
		return setting;
	}
	public OznerDevice(OznerContext context, String Address,String Serial,String Model,String Setting, SQLiteDB db) {
		mAddress = Address;
		mSerial=Serial;
		mModel=Model;
		mConetxt = context;
		mSetting=initSetting(Setting);
	}
	
	protected boolean Bind(OznerBluetoothDevice bluetooth) {
		if (bluetooth==mBluetooth)
			return false;
		mBluetooth=bluetooth;
		if (bluetooth!=null)
		{
			bluetooth.bindSetting(mSetting);
		}
		return true;
	}
	
}
