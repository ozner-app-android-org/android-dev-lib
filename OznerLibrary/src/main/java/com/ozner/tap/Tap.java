package com.ozner.tap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.ozner.cup.Cup;
import com.ozner.device.DeviceSetting;
import com.ozner.device.OznerBluetoothDevice;
import com.ozner.device.OznerContext;
import com.ozner.device.OznerDevice;
import com.ozner.util.SQLiteDB;
/**
 * @category 水探头
 * @author zhiyongxu
 * 水探头对象
 */
public class Tap extends OznerDevice {
	TapDatas mDatas;
	/**
	 * 水探头自动监测记录接收完成
	 */
	public final static String ACTION_BLUETOOTHTAP_RECORD_COMPLETE = "com.ozner.tap.bluetooth.record.complete";
	
	public Tap(OznerContext context, String Address, String Serial, String Model, String Setting,
			SQLiteDB db) {
		super(context, Address, Serial,Model, Setting, db);
		mDatas=new TapDatas(Address, getDB());
	}
	
	@Override
	protected DeviceSetting initSetting(String Setting) {
		DeviceSetting setting=new TapSetting();
		setting.load(Setting);
		return setting;
	};
	/**
	 * 获取水探头设置对象
	 */
	public TapSetting Setting()
	{
		return (TapSetting)super.Setting();
	}
	/**
	 * 获取控制蓝牙对象
	 * @return NULL=没有连接
	 */
	public BluetoothTap GetBluetooth()
	{
		return (BluetoothTap)this.Bluetooth();
	}
	/**
	 * 获取数据存储对象
	 * @return
	 */
	public TapDatas Datas() {
		return mDatas;
	}
	TapMonitor mTapMonitor = new TapMonitor();
	@Override
	public boolean Bind(OznerBluetoothDevice bluetooth) {
		if (bluetooth==GetBluetooth()) return false;
		if ((!(bluetooth instanceof BluetoothTap)) && (bluetooth!=null))
		{
			throw new ClassCastException("错误的类型");
		}
		if (bluetooth != null) {
			IntentFilter filter = new IntentFilter();
			filter.addAction(BluetoothTap.ACTION_BLUETOOTHTAP_RECORD_RECV_COMPLETE);
			getContext().registerReceiver(mTapMonitor, filter);
		} else {
			getContext().unregisterReceiver(mTapMonitor);
		}
		return super.Bind(bluetooth);
	}	
	class TapMonitor extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (!intent.getStringExtra("Address").equals(Address()))
				return;
			if (action.equals(BluetoothTap.ACTION_BLUETOOTHTAP_RECORD_RECV_COMPLETE)) {
				if (GetBluetooth() != null) {
					mDatas.addRecord(GetBluetooth().GetReocrds());
					Intent comp_intent = new Intent(
							ACTION_BLUETOOTHTAP_RECORD_COMPLETE);
					comp_intent.putExtra("Address",Tap.this.Address());
					getContext().sendBroadcast(comp_intent);
				}
			}
		}
	}
}
