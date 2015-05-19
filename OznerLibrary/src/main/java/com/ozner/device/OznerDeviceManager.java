package com.ozner.device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.ozner.bluetooth.BluetoothScan;
import com.ozner.bluetooth.BaseBluetoothDevice;
import com.ozner.bluetooth.BaseBluetoothDevice.BluetoothCloseCallback;
import com.ozner.cup.BluetoothCup;
import com.ozner.cup.Cup;
import com.ozner.util.SQLiteDB;
import com.ozner.util.dbg;

@SuppressLint("NewApi")
public class OznerDeviceManager implements BluetoothCloseCallback {
	/**
	 * 新增一个配对设备广播
	 */
	public final static String ACTION_OZNER_MANAGER_DEVICE_ADD = "com.ozner.manager.device.add";
	/**
	 * 删除设备广播
	 */
	public final static String ACTION_OZNER_MANAGER_DEVICE_REMOVE = "com.ozner.manager.device.remove";
	/**
	 * 修改设备广播
	 */
	public final static String ACTION_OZNER_MANAGER_DEVICE_CHANGE = "com.ozner.manager.device.change";
	OznerContext mContext;
	BluetoothMonitor mMonitor = new BluetoothMonitor();
	BluetoothScan mScaner = null;
	String mOwner = "";
	HashMap<String, OznerDevice> mDevices = new HashMap<String, OznerDevice>();
	HashMap<String, OznerBluetoothDevice> mBluetooths = new HashMap<String, OznerBluetoothDevice>();
	boolean _isBackground = false;

	protected SQLiteDB getDB() {
		return mContext.getDB();
	}

	protected Context getContext() {
		return mContext.getApplication();
	}

	protected String getOwner() {
		return mOwner;
	}

	/**
	 * 获取发现并且未配对的设备集合
	 * 
	 * @return
	 */
	public OznerBluetoothDevice[] getNotBindDevices() {
		ArrayList<OznerBluetoothDevice> list = new ArrayList<OznerBluetoothDevice>();
		synchronized (this) {
			for (OznerBluetoothDevice blue : mBluetooths.values()) {
				if (!mDevices.containsKey(blue.getAddress())) {
					list.add(blue);
				}
			}
			return list.toArray(new OznerBluetoothDevice[0]);
		}
	}

	/**
	 * 设置绑定的用户
	 * 
	 * @param Owner
	 *            用户ID
	 */
	public void setOwner(String Owner) {
		if (Owner == null)
			return;
		if (Owner.isEmpty())
			return;
		if (mOwner.equals(Owner))
			return;
		mOwner = Owner;
		dbg.i("Set Owner:%s", Owner);
		synchronized (this) {
			mDevices.clear();
		}
		CloseAll();
		LoadDevices();
	}

	/**
	 * 删除所有配对的设备
	 */
	public void removeAllDevice() {
		getDB().execSQLNonQuery("delete from OznerDevices", new String[0]);
		synchronized (this) {
			mDevices.clear();
		}
		CloseAll();
	}

	protected void CloseAll() {
		synchronized (this) {
			ArrayList<BaseBluetoothDevice> list = new ArrayList<BaseBluetoothDevice>(
					mBluetooths.values());
			for (BaseBluetoothDevice device : list) {
				device.close();
			}
			mBluetooths.clear();
		}
	}

	private void LoadDevices() {
		synchronized (this) {
			List<String[]> list = getDB()
					.ExecSQL(
							"select Address,Serial,JSON,Model from OznerDevices where Owner=?",
							new String[] { getOwner() });
			for (String[] v : list) {
				String Address = v[0];
				String Serial = v[1];
				String Json = v[2];
				String Model = v[3];
				if (Model == null)
					Model = "CUP001";
				if (Model.isEmpty())
					Model = "CUP001";
				Model=Model.trim();
				if (!mDevices.containsKey(Address)) {
					OznerDevice device = null;
					ArrayList<DeviceManager> mgrs = getManagers();
					for (DeviceManager mgr : mgrs) {

						device = mgr.loadDevice(Address, Serial, Model, Json);
						if (device != null) {
							mDevices.put(Address, device);
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * 删除一个已经配对的设备
	 * 
	 * @param device
	 */
	public void remove(OznerDevice device) {
		getDB().execSQLNonQuery("delete from OznerDevices where Address=?",
				new String[] { device.Address() });
		String address = device.Address();
		synchronized (this) {
			if (mDevices.containsKey(address)) {
				mDevices.remove(address);
			}
	
			Intent intent = new Intent(ACTION_OZNER_MANAGER_DEVICE_REMOVE);
			intent.putExtra("Address", address);
			getContext().sendBroadcast(intent);

			ArrayList<DeviceManager> list = getManagers();
			for (DeviceManager mgr : list) {
				mgr.remove(device);
			}
			
			
			if (device.Bluetooth() != null) {
				device.Bluetooth().close();
			}
			device.Bind(null);
		}
	}

	/**
	 * 判断一个设备MAC地址是否属于配对过的设备
	 * 
	 * @param address
	 * @return
	 */
	public boolean isBindDevice(String address) {
		synchronized (this) {
			return mDevices.containsKey(address);
		}
	}

	/**
	 * 获取所有设备集合
	 * 
	 * @return
	 */
	public OznerDevice[] getDevices() {
		synchronized (this) {
			return mDevices.values().toArray(new OznerDevice[0]);
		}
	}

	/**
	 * 通过蓝牙设备获取一个设备控制对象
	 * 
	 */
	public OznerDevice getDevice(OznerBluetoothDevice bluetooth)
			throws NotSupportDevcieException {
		String address = bluetooth.getAddress();
		OznerDevice device = getDevice(address);
		if (device == null) {
			ArrayList<DeviceManager> list = getManagers();
			for (DeviceManager mgr : list) {
				device = mgr.getDevice(bluetooth);
				if (device != null)
					return device;
			}
		}
		return device;
	}

	/**
	 * 通过MAC地址获取已经绑定的设备
	 * 
	 * @param address
	 * @return
	 */
	public OznerDevice getDevice(String address) {
		synchronized (this) {
			if (mDevices.containsKey(address))
				return mDevices.get(address);
			else
				return null;
		}

	}

	/**
	 * 保存并绑定设备设置
	 */
	public void save(OznerDevice device) {
		synchronized (this) {
			String Addres = device.Address();
			boolean isNew = false;
			if (getOwner() == null)
				return;
			if (getOwner().isEmpty())
				return;
			if (!mDevices.containsKey(Addres)) {
				mDevices.put(Addres, device);
				isNew = false;
			} else
				isNew = true;
			getDB().execSQLNonQuery(
					"INSERT OR REPLACE INTO OznerDevices(Address,Serial,Owner,Model,JSON) VALUES (?,?,?,?,?);",
					new String[] { device.Address(), device.Serial(),
							getOwner(), device.Model(),
							device.Setting().toString() });

			Intent intent = new Intent();
			intent.putExtra("Address", Addres);
			intent.setAction(isNew ? ACTION_OZNER_MANAGER_DEVICE_ADD
					: ACTION_OZNER_MANAGER_DEVICE_CHANGE);
			getContext().sendBroadcast(intent);

			if (device.Bluetooth() != null) {
				device.Bluetooth().setBackgroundMode(_isBackground);
				if (_isBackground) {
					if (device.Bluetooth().isDataAvailable()) {
						device.Bluetooth().connect();
						device.Bluetooth().updateSetting();
					}
				} else {
					device.Bluetooth().connect();
					device.Bluetooth().updateSetting();
				}
			}

			ArrayList<DeviceManager> list = getManagers();
			if (isNew) {
				for (DeviceManager mgr : list) {
					mgr.add(device);
				}
			} else {
				for (DeviceManager mgr : list) {
					mgr.update(device);
				}
			}
		}
	}

	private ArrayList<DeviceManager> getManagers() {
		ArrayList<DeviceManager> list = new ArrayList<DeviceManager>();
		synchronized (this) {
			list.addAll(mManagers);
		}
		return list;
	}

	public OznerDeviceManager(OznerContext context, BluetoothScan scaner) {
		mScaner = scaner;
		mContext = context;
		getDB().execSQLNonQuery(
				"CREATE TABLE IF NOT EXISTS OznerDevices (Address VARCHAR PRIMARY KEY NOT NULL, Serial TEXT,Model Text,Owner TEXT, JSON TEXT)",
				new String[] {});
		/*
		 * getDB().execSQLNonQuery(
		 * "CREATE TABLE IF NOT EXISTS CupSetting (Address VARCHAR PRIMARY KEY NOT NULL, Serial TEXT,Model Text,Owner TEXT, JSON TEXT)"
		 * , new String[] {});
		 */
		try {
			getDB().execSQLNonQuery(
					"INSERT INTO OznerDevices (Address, Serial,Owner,JSON) SELECT Address,Serial,Owner,JSON from CupSetting",
					new String[] {});
			getDB().execSQLNonQuery("DROP TABLE CupSetting", new String[] {});
		} catch (Exception e) {

		}

		if (mOwner != "") {
			setOwner(mOwner);
		}
	}

	BluetoothMonitor mBluetoothMonitor = new BluetoothMonitor();

	class BluetoothMonitor extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			String Action = intent.getAction();
			if (BluetoothScan.ACTION_SCANNER_FOUND.equals(Action)) {
				String address = intent
						.getStringExtra(BluetoothScan.Extra_Address);
				BluetoothDevice bluetooth = mScaner.getDevice(address);

				if (bluetooth != null) {
					OznerBluetoothDevice device = foundDevice(bluetooth, intent
							.getStringExtra(BluetoothScan.Extra_Platform),
							intent.getStringExtra(BluetoothScan.Extra_Model),
							intent.getLongExtra(BluetoothScan.Extra_Firmware,
									Integer.MAX_VALUE));

					if (device != null) {
						device.updateCustomData(
								intent.getIntExtra(
										BluetoothScan.Extra_CustomType, 0),
								intent.getByteArrayExtra(BluetoothScan.Extra_CustomData));

						device.updateInfo(
								intent.getStringExtra(BluetoothScan.Extra_Model),
								intent.getStringExtra(BluetoothScan.Extra_Platform),
								intent.getLongExtra(
										BluetoothScan.Extra_Firmware,
										Long.MAX_VALUE));
						device.setDataAvailable(intent.getBooleanExtra(
								BluetoothScan.Extra_DataAvailable, false));
						
						// 如果是配对的设备，直接连接操作
						synchronized (this) {
							if (mDevices.containsKey(address)) {
								device.setBackgroundMode(_isBackground);
								OznerDevice d = mDevices.get(address);
								d.Bind(device);
								if (_isBackground) {
									// 如果有数据可用，连接设备
									if (device.isDataAvailable()) {
										device.connect();
									}
								} else {
									// 如果是已配对设备，并且在前台，直接连接操作
									device.connect();
								}
							}
						}
					}
				}
				dbg.d("Bluetooth Found Address:" + address);
				return;
			}

			if (BluetoothScan.ACTION_SCANNER_LOST.equals(Action)) {
				{
					lostDevice(intent
							.getStringExtra(BluetoothScan.Extra_Address));
				}
				dbg.i("Bluetooth Lost Address:"
						+ intent.getStringExtra("Address"));
				return;
			}
			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(Action)) {
				BluetoothManager bluetoothManager = (BluetoothManager) context
						.getSystemService(Context.BLUETOOTH_SERVICE);
				if (!bluetoothManager.getAdapter().isEnabled()) {
					CloseAll();
				}
			}
		}
	}

	/**
	 * 启动服务
	 */
	public void Start() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothScan.ACTION_SCANNER_FOUND);
		filter.addAction(BluetoothScan.ACTION_SCANNER_LOST);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		getContext().registerReceiver(mMonitor, filter);
	}

	/**
	 * 停止服务
	 */
	public void Stop() {
		CloseAll();
	}

	ArrayList<DeviceManager> mManagers = new ArrayList<DeviceManager>();

	/**
	 * 注册一个设备管理器
	 * 
	 * @param manager
	 */
	public void registerManager(DeviceManager manager) {
		synchronized (this) {
			if (!mManagers.contains(manager)) {
				mManagers.add(manager);
			}
		}
	}

	/**
	 * 注销设备管理器
	 * 
	 * @param manager
	 */
	public void unregisterManager(DeviceManager manager) {
		synchronized (this) {
			if (mManagers.contains(manager))
				mManagers.remove(manager);
		}
	}

	@Override
	public void OnOznerBluetoothClose(BaseBluetoothDevice device) {
		synchronized (this) {
			mBluetooths.remove(device.getAddress());
			if (mDevices.containsKey(device.getAddress())) {
				mDevices.get(device.getAddress()).Bind(null);
			}
		}
	}

	protected OznerBluetoothDevice foundDevice(BluetoothDevice device,
			String Paltform, String Model, long Firewarm) {
		if (mBluetooths.containsKey(device.getAddress())) {
			return mBluetooths.get(device.getAddress());
		}
		ArrayList<DeviceManager> list = getManagers();
		for (DeviceManager mgr : list) {
			OznerBluetoothDevice ret = mgr.getBluetoothDevice(device, this,
					Paltform, Model, Firewarm);
			if (ret != null) {
				mBluetooths.put(device.getAddress(), ret);
				return ret;
			}
		}
		return null;
	}

	protected void lostDevice(String Address) {
		ArrayList<DeviceManager> list = getManagers();
		for (DeviceManager mgr : list) {
			mgr.lostDevice(Address);
		}
	}
	public boolean isBackground()
	{
		return _isBackground;
	}
	/**
	 * 设置前后台模式
	 * 
	 * @param isBackground
	 */
	public void setBackgroundMode(boolean isBackground) {
		if (isBackground==_isBackground) return;
		this._isBackground = isBackground;
			// 如果设置到前台模式
		synchronized (this) {
				for (OznerDevice d : mDevices.values()) {
					if (d.Bluetooth() != null) {
						d.Bluetooth().setBackgroundMode(isBackground);
						if ((!d.connected()) && (!isBackground)) {
							d.Bluetooth().connect();
						}
					}
				}
			}
		
	}
}
