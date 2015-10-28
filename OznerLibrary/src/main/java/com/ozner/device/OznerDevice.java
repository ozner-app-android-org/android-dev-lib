package com.ozner.device;

import android.content.Context;

import com.ozner.util.SQLiteDB;
/**
 * @category Device
 * @author zhiyongxu
 * 浩泽设备基类
 */
public abstract class OznerDevice {
	public String mAddress;
	private BaseDeviceIO mDeviceIO;
	private OznerContext mContext;
	private DeviceSetting mSetting;
	private String mSerial;
	private String mModel;
	private boolean mSetChanged = false;

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
		return mContext.getDB();
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
		return mContext.getApplication();
	}
	/**
	 * 蓝牙控制对象
	 * @return NULL=没有蓝牙连接
	 */
	public BaseDeviceIO Bluetooth() {
		return mDeviceIO;
	}


	/**
	 * 判断设备是否连接
	 * @return
	 */
	public boolean connected() {
		if (mDeviceIO != null) {
			return mDeviceIO.connected();
		} else
			return false;
	}
	protected DeviceSetting initSetting(String Setting)
	{
		DeviceSetting setting = new DeviceSetting();
		setting.load(Setting);
		return setting;
	}

    public OznerDevice(OznerContext context, String Address, String Serial, String Model, String Setting, SQLiteDB db) {
        mAddress = Address;
		mSerial=Serial;
		mModel=Model;
		mContext = context;
		mSetting=initSetting(Setting);
	}

	/**
	 * 通知设备设置变更
	 */
	public void UpdateSetting() {
		mSetChanged = true;
	}

	protected void resetSettingUpdate() {
		mSetChanged = false;
	}

	protected boolean isSetChanged() {
		return mSetChanged;
	}


	protected boolean Bind(BaseDeviceIO bluetooth) {
		if (bluetooth == mDeviceIO)
			return false;
		mDeviceIO = bluetooth;

		return true;
	}
	
}
