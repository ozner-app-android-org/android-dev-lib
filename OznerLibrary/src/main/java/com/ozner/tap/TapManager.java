package com.ozner.tap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ozner.bluetooth.BaseBluetoothDevice.BluetoothCloseCallback;
import com.ozner.bluetooth.BluetoothScan;
import com.ozner.bluetooth.BaseBluetoothDevice;
import com.ozner.cup.BluetoothCup;
import com.ozner.cup.Cup;
import com.ozner.device.DeviceManager;
import com.ozner.device.OznerBluetoothDevice;
import com.ozner.device.OznerContext;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;
import com.ozner.util.SQLiteDB;
import com.ozner.util.dbg;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;

@SuppressLint("NewApi")
/**
 * 水探头管理器
 * @category 水探头
 * @author zhiyongxu
 *
 */
public class TapManager extends DeviceManager {

	public TapManager(OznerContext context, OznerDeviceManager bluetoothManger) {
		super(context, bluetoothManger);
	}
	private boolean isTap(BluetoothDevice device,String model)
	{
		if (model.equals("SC001"))
		{
			return true;
		}else
			return false;
	}
	/**
	 * 通过指定地址获取水探头设备
	 * @param address 设备MAC地址
	 * @return 水探头实例
	 */
	public Tap getTap(String address) {
		return (Tap) getBluetoothManager().getDevice(address);
	}
	
	@Override
	protected OznerBluetoothDevice getBluetoothDevice(BluetoothDevice device,
			BluetoothCloseCallback bluetoothCallback, String Paltform,
			String Model, long Firewarm) {
		if (isTap(device, Model))
		{
			return new BluetoothTap(getApplication(), device, bluetoothCallback);
		}
		return null;
	}
	/**
	 * 在数据库中构造一个新的水探头
	 * 
	 * @param address
	 *            水杯地址
	 * @param SettingJson
	 *            配置 JSON
	 * @param Name
	 *            名称
	 * @return 水探头实例
	 */
	public Cup newCup(String address, String Name,String SettingJson) {
		Cup c = new Cup(getContext(), address, address, "SC001", SettingJson, getDB());
		c.Setting().name(Name);
		getBluetoothManager().save(c);
		return c;
	}
	
	@Override
	protected OznerDevice getDevice(OznerBluetoothDevice bluetooth) {
		String address = bluetooth.getAddress();
		OznerDevice device = getBluetoothManager().getDevice(address);
		if (device != null) {
			return device;
		} else {
			Tap tap = new Tap(getContext(), address, bluetooth.getSerial(),bluetooth.getModel(), "",
					getDB());
			tap.Setting().name(bluetooth.getName());
			tap.Bind(bluetooth);
			return tap;
		}
	}

	@Override
	protected OznerDevice loadDevice(String address, String Serial,String Model,
			String Setting) {
		if (isTap(null, Model))
			return new Tap(getContext(), address, Serial, Model,Setting,getDB());
		else
			return null;
	}
	
	


}
