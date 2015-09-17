package com.ozner.device;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.util.SQLiteDB;
/**
 * 设备管理基类
 * @author zhiyongxu
 * @category Device
 */
public abstract class DeviceManager {
	OznerContext mContext;
	OznerDeviceManager mBluetoothManager;
	public DeviceManager(OznerContext context,OznerDeviceManager bluetoothManger)
	{
		mBluetoothManager=bluetoothManger;
		mContext=context;
		bluetoothManger.registerManager(this);
	}
	
	protected Context getApplication() {
		return mContext.getApplication();
	}
	protected OznerContext getContext() {
		return mContext;
	}
	protected SQLiteDB getDB() {
		return mContext.getDB();
	}
	protected OznerDeviceManager getBluetoothManager() {
		return mBluetoothManager;
	}

	protected abstract OznerBluetoothDevice getBluetoothDevice(BluetoothDevice device,
			BluetoothIO.BluetoothCloseCallback bluetoothCallback,
			String Paltform, String Model, long Firewarm);

	protected abstract OznerDevice getDevice(OznerBluetoothDevice bluetooth);
	protected abstract OznerDevice loadDevice(String address,String Serial,String Model,String Setting);
	
	protected void lostDevice(String Address)
	{
		
	}
	
	protected void remove(OznerDevice device)
	{
		
	}
	protected void update(OznerDevice device) {
		
	}
	protected void add(OznerDevice device)
	{
	}
}
