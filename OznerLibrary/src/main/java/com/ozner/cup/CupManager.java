package com.ozner.cup;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import com.ozner.device.DeviceManager;
import com.ozner.device.NotSupportDevcieException;
import com.ozner.device.OznerBluetoothDevice;
import com.ozner.device.OznerContext;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

import java.util.ArrayList;

@SuppressLint("NewApi")
/**
 * 智能杯管理对象
 * @category 智能杯
 * @author zhiyongxu
 *
 */
public class CupManager extends DeviceManager {

	public CupManager(OznerContext context, OznerDeviceManager bluetoothManger) {
		super(context, bluetoothManger);
	}

	/**
	 * 新增一个配对的水杯
	 */
	public final static String ACTION_CUP_MANAGER_CUP_ADD = "com.ozner.cup.CupManager.Cup.Add";
	/**
	 * 删除配对水杯
	 */
	public final static String ACTION_CUP_MANAGER_CUP_REMOVE = "com.ozner.cup.CupManager.Cup.Remove";
	/**
	 * 更新配对水杯
	 */
	public final static String ACTION_CUP_MANAGER_CUP_CHANGE = "com.ozner.cup.CupManager.Cup.Change";

	/**
	 * 获取为配对的水杯蓝牙控制对象集合
	 * 
	 * @return
	 */
	public BluetoothCup[] getNotBindCups() {
		ArrayList<BluetoothCup> rets = new ArrayList<BluetoothCup>();
		for (OznerBluetoothDevice device : getBluetoothManager()
				.getNotBindDevices()) {
			if (device instanceof BluetoothCup) {
				rets.add((BluetoothCup) device);
			}
		}
		return rets.toArray(new BluetoothCup[0]);
	}

	/**
	 * 通过MAC地址获取指定的智能杯
	 * 
	 * @param address
	 *            水杯MAC地址
	 * @return 返回NULL，说明没有杯子
	 */
	public Cup getCup(String address) {
		return (Cup) getBluetoothManager().getDevice(address);
	}

	/**
	 * 通过一个蓝牙控制设备返回对应的智能杯对象
	 * 
	 * @param cup
	 *            蓝牙设备
	 * @return 智能杯对象，NULL未找到
	 * @throws NotSupportDevcieException
	 *             对象不是智能杯个蓝牙控制对象
	 */
	public Cup getCup(BluetoothCup cup) throws NotSupportDevcieException {
		return (Cup) getBluetoothManager().getDevice(cup);
	}

	/**
	 * 在数据库中构造一个新的水杯
	 * 
	 * @param address
	 *            水杯地址
	 * @param SettingJson
	 *            配置 JSON
	 * @return 水杯实例
	 */
	public Cup newCup(String address, String Name,String SettingJson) {
		Cup c = new Cup(getContext(), address, address, "CP001", SettingJson, getDB());
		c.Setting().name(Name);
		getBluetoothManager().save(c);
		return c;
	}

	/**
	 * 获取所有者是其他人的杯子
	 * 
	 * @return
	 */
	public Cup[] getOtherCupList() {
		ArrayList<Cup> list = new ArrayList<Cup>();
		for (OznerDevice cup : getBluetoothManager().getDevices()) {
			if (cup instanceof Cup) {
				if (!((CupSetting) cup.Setting()).isMe())
					list.add((Cup) cup);
			}
		}
		return list.toArray(new Cup[0]);

	}

	/**
	 * 获取所有配对过的杯子
	 * 
	 * @return
	 */
	public Cup[] getCupList() {
		ArrayList<Cup> list = new ArrayList<Cup>();
		for (OznerDevice cup : getBluetoothManager().getDevices()) {
			if (cup instanceof Cup) {
				list.add((Cup) cup);
			}
		}
		return list.toArray(new Cup[0]);

	}

	/**
	 * 获取所有人是我的智能杯
	 * 
	 * @return
	 */
	public Cup[] getMyCupList() {
		ArrayList<Cup> list = new ArrayList<Cup>();
		for (OznerDevice cup : getBluetoothManager().getDevices()) {
			if (cup instanceof Cup) {
				if (((CupSetting) cup.Setting()).isMe())
					list.add((Cup) cup);
			}
		}
		return list.toArray(new Cup[0]);

	}

	protected boolean IsCup(BluetoothDevice device, String Model) {
		if (Model==null) return false;

		return Model.trim().equals("CP001");
	}

	@Override
	protected OznerBluetoothDevice getBluetoothDevice(BluetoothDevice device,
			BluetoothIO.BluetoothCloseCallback bluetoothCallback, String Paltform,
			String Model, long Firewarm){
		if (IsCup(device, Model)) {
			return new BluetoothCup(getApplication(),bluetoothCallback,device,Paltform,Model,Firewarm);
		} else
			return null;
	}

	@Override
	protected void update(OznerDevice device) {
		if (device instanceof Cup) {
			Intent intent = new Intent(ACTION_CUP_MANAGER_CUP_CHANGE);
			intent.putExtra("Address", device.Address());
			getApplication().sendBroadcast(intent);
		}
	}

	@Override
	protected void add(OznerDevice device) {
		if (device instanceof Cup) {
			Intent intent = new Intent(ACTION_CUP_MANAGER_CUP_ADD);
			intent.putExtra("Address", device.Address());
			getApplication().sendBroadcast(intent);
		}
		super.add(device);
	}

	@Override
	protected void remove(OznerDevice device) {
		if (device instanceof Cup) {
			Intent intent = new Intent(ACTION_CUP_MANAGER_CUP_REMOVE);
			intent.putExtra("Address", device.Address());
			getApplication().sendBroadcast(intent);
		}
		super.remove(device);
	}

	@Override
	protected OznerDevice getDevice(OznerBluetoothDevice bluetooth) {
		if (bluetooth instanceof BluetoothCup) {
			String address = bluetooth.getAddress();
			OznerDevice device = getBluetoothManager().getDevice(address);
			if (device != null) {
				return device;
			} else {
				Cup c = new Cup(getContext(), address, bluetooth.getSerial(),
						bluetooth.getModel(), "", getDB());
				c.Setting().name(bluetooth.getName());
				c.Bind(bluetooth);
				return c;
			}
		}
		return null;

	}

	@Override
	protected OznerDevice loadDevice(String address, String Serial,
			String Model, String Setting) {
		if (IsCup(null, Model)) {
			return new Cup(getContext(), address, Serial, Model, Setting,
					getDB());
		} else
			return null;
	}
}
