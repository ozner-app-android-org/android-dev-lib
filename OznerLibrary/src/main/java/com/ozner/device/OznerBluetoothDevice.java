package com.ozner.device;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.util.dbg;

public abstract class OznerBluetoothDevice extends BluetoothIO {
	/**
	 * 配对状态
	 */
	public static final String ACTION_OZNER_BLUETOOTH_BIND_MODE="com.ozner.bluetooth.bind";

	public static final String ACTION_OZNER_BLUETOOTH_UPDATE_FIRMWARE_FAIL="com.ozner.bluetooth.update.firmware.fail";
	public static final String ACTION_OZNER_BLUETOOTH_UPDATE_FIRMWARE_START="com.ozner.bluetooth.update.firmware.start";
	public static final String ACTION_OZNER_BLUETOOTH_UPDATE_FIRMWARE_POSITION="com.ozner.bluetooth.update.firmware.position";
	public static final String ACTION_OZNER_BLUETOOTH_UPDATE_FIRMWARE_COMPLETE="com.ozner.bluetooth.update.firmware.complete";

	DeviceSetting mSetting;

	public OznerBluetoothDevice(Context context, BluetoothCloseCallback callback, BluetoothDevice device, String Platform, String Model, long Firmware)
	{
		super(context, callback, device, Platform, Model, Firmware);
	}


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
		super.setBindMode(mode);
		Intent intent = new Intent(ACTION_OZNER_BLUETOOTH_BIND_MODE);
		if (isBindMode())
			dbg.d("设备进入配对模式:%s", this.getAddress());
		intent.putExtra("Address", getAddress());
		intent.putExtra("bind", isBindMode());
		getContext().sendBroadcast(intent);

	}



	/**
	 * 返回电压百分比
	 * @return <0,没取到电压,0-1范围
	 */
	public abstract float getPowerPer();
	public abstract Object getSensor();


}
