package com.ozner.bluetooth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;

import com.ozner.cup.BluetoothCup;
import com.ozner.util.dbg;

@SuppressLint("NewApi")
public class BluetoothScan implements LeScanCallback {
	Context mContext;
	BluetoothAdapter mAdapter;
	Timer mScanTimer;
	public final static int AD_CustomType_BindStatus = 0x10;

	public static final String Extra_Address = "Address";
	public static final String Extra_Model = "Model";
	public static final String Extra_Firmware = "Firmware";
	public static final String Extra_Platform = "Platform";

	public static final String Extra_CustomType = "CustomType";
	public static final String Extra_CustomData = "CustomData";
	public static final String Extra_Rssi = "Rssi";
	public static final String Extra_DataAvailable = "DataAvailable";

	/**
	 * 扫描开始广播,无附加数据
	 */
	public final static String ACTION_SCANNER_START = "com.ozner.bluetooth.sanner.start";
	/**
	 * 找到设备广播,附加设备的MAC地址
	 */
	public final static String ACTION_SCANNER_FOUND = "com.ozner.bluetooth.sanner.found";

	/**
	 * 设备脱离范围广播,附加设备设备MAC地址
	 */
	public final static String ACTION_SCANNER_LOST = "com.ozner.bluetooth.sanner.lost";
	/**
	 * 扫描停止广播
	 */
	public final static String ACTION_SCANNER_STOP = "com.ozner.bluetooth.sanner.stop";
	BluetoothMonitor mMonitor = new BluetoothMonitor();

	public BluetoothScan(Context context) {
		mContext = context;
		BluetoothManager bluetoothManager = (BluetoothManager) context
				.getSystemService(Context.BLUETOOTH_SERVICE);
		mAdapter = bluetoothManager.getAdapter();
	}

	/**
	 * 用来接收系统蓝牙开关信息,打开开启自动扫描,关闭就关掉
	 */
	class BluetoothMonitor extends BroadcastReceiver {
		@SuppressWarnings("deprecation")
		public void onReceive(Context context, Intent intent) {
			if (BluetoothAdapter.ACTION_STATE_CHANGED
					.equals(intent.getAction())) {
				if (mAdapter.getState() == BluetoothAdapter.STATE_OFF) {
					StopScan();
				} else if (mAdapter.getState() == BluetoothAdapter.STATE_ON) {
					StartScan();
				}
			}
		}
	}

	/**
	 * 获取当前附近的设备列表
	 * 
	 * @return
	 */
	public BluetoothDevice[] getDevices() {
		ArrayList<BluetoothDevice> list = new ArrayList<BluetoothDevice>();
		synchronized (mDevices) {
			for (String address : mDevices.keySet()) {
				BluetoothDevice device = mAdapter.getRemoteDevice(address);
				if (device != null) {
					list.add(device);
				}
			}
		}
		return list.toArray(new BluetoothDevice[0]);
	}

	/**
	 * 通过MAC地址获取列表
	 */
	public BluetoothDevice getDevice(String address) {
		return mAdapter.getRemoteDevice(address);
	}

	HashMap<String, Date> mDevices = new HashMap<String, Date>();
	Handler mScanHandler = new Handler(Looper.getMainLooper());
	boolean mRuning = false;

	private void scan() {
		if (mAdapter.startLeScan(this)) {
			mScanHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					
					UUID[] services=new UUID[]{UUID.fromString(String.format(
                            "%08x-0000-1000-8000-00805f9b34fb", 0xfff0))};
					
					mAdapter.startLeScan(services, BluetoothScan.this);
					//mAdapter.stopLeScan(BluetoothScan.this,);
				}
			}, 2000);
		}
	}

	private void StartScan() {
		if (mScanTimer != null)
			return;
		StartCheckTimer();
		Intent intent = new Intent(ACTION_SCANNER_START);
		mContext.sendBroadcast(intent);
		mScanTimer = new Timer();
		mScanTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				mScanHandler.post(new Runnable() {
					@Override
					public void run() {
						scan();
					}
				});
			}
		}, 100, 4000);
		mRuning = true;
	}

	private void StopScan() {
		try {
			if (mScanTimer != null) {

				mScanTimer.cancel();
				mAdapter.stopLeScan(this);
				mScanTimer.purge();
				mScanTimer = null;
			}
			StopCheckTimer();
			synchronized (mDevices) {
				mDevices.clear();
			}
			Intent intent = new Intent(ACTION_SCANNER_STOP);
			mContext.sendBroadcast(intent);
			mRuning = false;
		} catch (Exception e) {

		}
	}

	public boolean isRuning() {
		return mRuning;
	}

	@SuppressWarnings("deprecation")
	public void Start() {
		if (mRuning)
			return;
		IntentFilter filter = new IntentFilter(
				BluetoothAdapter.ACTION_STATE_CHANGED);
		mContext.registerReceiver(mMonitor, filter);
		if (!mAdapter.isEnabled()) {
			mAdapter.enable();
		} else
			StartScan();
	}

	public void Stop() {
		mContext.unregisterReceiver(mMonitor);
		StopScan();
	}

	Timer mDeviceCheckTimer = null;

	private void StartCheckTimer() {
		if (mDeviceCheckTimer != null)
			return;
		mDeviceCheckTimer = new Timer();
		mDeviceCheckTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				synchronized (mDevices) {
					Date now = new Date();
					HashSet<String> list = new HashSet<String>(mDevices
							.keySet());
					for (String address : list) {
						Date t = mDevices.get(address);
						if ((now.getTime() - t.getTime()) > 5000) {
							mDevices.remove(address);
							Intent intent = new Intent(ACTION_SCANNER_LOST);
							intent.putExtra("Address", address);
							mContext.sendBroadcast(intent);
						}
					}
				}
			}
		}, 0, 5000);
	}

	private void StopCheckTimer() {
		if (mDeviceCheckTimer == null)
			return;
		mDeviceCheckTimer.cancel();
		mDeviceCheckTimer.purge();
		mDeviceCheckTimer = null;
	}

	final static byte GAP_ADTYPE_MANUFACTURER_SPECIFIC = (byte) 0xff;
	final static byte GAP_ADTYPE_SERVICE_DATA = 0x16;

	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
		String address = device.getAddress();
		// dbg.d("device:%s",device.getAddress());
		// 是否发送广播标记
		boolean send = false;
		synchronized (mDevices) {
			Date now = new Date();
			if (mDevices.containsKey(address)) {
				Date time = mDevices.get(address);
				if (now.compareTo(time) > 1000) // 每2秒发送一个发现广播
				{
					send = true;
					mDevices.put(address, now);
				}
			} else {
				send = true;
				mDevices.put(address, now);
			}
		}

		if (send) {
			String Model = "";
			Date Firmware = null;
			byte[] CustomData = null;
			String Platform = "";
			boolean Available = false;
			int CustomType = 0;
			int pos = 0;
			while (true) {
				try {
					int len = scanRecord[pos];
					pos++;
					if (len > 0) {
						byte flag = scanRecord[pos];
						if (len > 1) {
							if (flag == GAP_ADTYPE_MANUFACTURER_SPECIFIC) {
								//dbg.d("send GAP_ADTYPE_MANUFACTURER_SPECIFIC:%s",
								//		device.getAddress());
								// 老固件水杯兼容
								byte[] data = Arrays.copyOfRange(scanRecord,
										pos + 1, pos + len);
								if (device.getName().equals("Ozner Cup")) {
									CustomType = BluetoothCup.AD_CustomType_Gravity;
									Model = "CP001";
									Platform = "C01";
									CustomData = data;
									Available = true;
								}
							}
							if (flag == GAP_ADTYPE_SERVICE_DATA) {
								byte[] data = Arrays.copyOfRange(scanRecord,
										pos + 1, pos + len);
								BluetoothScanRep rep = new BluetoothScanRep();
								rep.FromBytes(data);
								Model = rep.Model;
								Platform = rep.Platform;
								Firmware = rep.Firmware;
								CustomType = rep.CustomDataType;
								CustomData = rep.CustomData;
								Available = rep.Available;
							}
						}
					}
					pos += len;
					if (pos >= scanRecord.length)
						break;
				} catch (Exception e) {
					dbg.e(e.toString());
					return;
				}
			}
			Intent intent = new Intent(ACTION_SCANNER_FOUND);
			intent.putExtra(Extra_Address, address);
			intent.putExtra(Extra_Model, Model);
			intent.putExtra(Extra_Platform, Platform);
			intent.putExtra(Extra_Rssi, rssi);
			if (Firmware != null)
				intent.putExtra(Extra_Firmware, Firmware.getTime());
			intent.putExtra(Extra_CustomType, CustomType);
			intent.putExtra(Extra_CustomData, CustomData);
			intent.putExtra(Extra_DataAvailable, Available);
			mContext.sendBroadcast(intent);
		}

	}

}
