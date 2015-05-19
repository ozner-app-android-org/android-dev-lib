package com.ozner.device;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Debug;
import android.os.Handler;

import com.ozner.bluetooth.AutoRefashDevice;
import com.ozner.util.dbg;

import java.io.IOException;
import java.util.Timer;

public abstract class OznerBluetoothDevice extends AutoRefashDevice {
	/**
	 * 配对状态
	 */
	public static final String ACTION_OZNER_BLUETOOTH_BIND_MODE="com.ozner.bluetooth.bind";

	public static final String ACTION_OZNER_BLUETOOTH_UPDATE_FIRMWARE_FAIL="com.ozner.bluetooth.update.firmware.fail";
	public static final String ACTION_OZNER_BLUETOOTH_UPDATE_FIRMWARE_START="com.ozner.bluetooth.update.firmware.start";
	public static final String ACTION_OZNER_BLUETOOTH_UPDATE_FIRMWARE_POSITION="com.ozner.bluetooth.update.firmware.position";
	public static final String ACTION_OZNER_BLUETOOTH_UPDATE_FIRMWARE_COMPLETE="com.ozner.bluetooth.update.firmware.complete";


	public OznerBluetoothDevice(Context context, BluetoothDevice device,
			BluetoothCloseCallback callback) {
		super(context, device, callback);
	}
	DeviceSetting mSetting;
	boolean misBindMode=false;
	//是否有未读的数据
	boolean misDataAvailable=true;
	/**
	 * 将设备和设置对象绑定
	 * @param setting
	 */
	public void bindSetting(DeviceSetting setting)
	{
		mSetting=setting;
	}
	/**
	 * 获取设置对象
	 * @return
	 */
	public DeviceSetting getSetting()
	{
		return mSetting;
	}
	protected void setBindMode(boolean mode) {
		misBindMode=mode;
		Intent intent = new Intent(ACTION_OZNER_BLUETOOTH_BIND_MODE);
		dbg.d("设备进入配对模式:%s", this.getAddress());
		intent.putExtra("Address", getAddress());
		intent.putExtra("bind", isBindMode());
		getContext().sendBroadcast(intent);
	}
	
	/**
	 * 设置是否有未读取的数据
	 * @param isAvailable
	 */
	protected void setDataAvailable(boolean isAvailable) {
		misDataAvailable=isAvailable;
	}
	
	/**
	 * 判断是否有未读取的数据
	 * @return
	 */
	protected boolean isDataAvailable() {
		return misDataAvailable;
	}
	
	@Override
	public boolean isBindMode() {
		return misBindMode;
	}
	@Override
	protected void onReadly() {
		super.onReadly();
		sleep();
		updateSetting();
	}
	protected abstract boolean updateSetting();
	/**
	 * 返回电压百分比
	 * @return <0,没取到电压,0-1范围
	 */
	public abstract float getPowerPer();


}
