package com.example.oznerble;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.oznerble.R.id;
import com.example.oznerble.R.layout;
import com.ozner.application.OznerBLEService.OznerBLEBinder;
import com.ozner.bluetooth.BluetoothScan;
import com.ozner.device.NotSupportDevcieException;
import com.ozner.device.OznerBluetoothDevice;
import com.ozner.device.OznerDevice;

public class AddDeviceActivity extends Activity {
	ListView list;
	ListAdpater adpater;
	Monitor mMonitor=new Monitor();

	class Monitor extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) {
			adpater.Reload();
		}
	}
	@Override
	protected void onDestroy() {
		this.unregisterReceiver(mMonitor);
		super.onDestroy();
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		IntentFilter filter=new IntentFilter();
		filter.addAction(BluetoothScan.ACTION_SCANNER_FOUND);
		filter.addAction(BluetoothScan.ACTION_SCANNER_LOST);
		filter.addAction(OznerBluetoothDevice.ACTION_OZNER_BLUETOOTH_BIND_MODE);
		this.registerReceiver(mMonitor, filter);
		setTitle("设备配对");
		setContentView(layout.activity_add);
		adpater=new ListAdpater(this);
		list=(ListView)findViewById(R.id.devcieList);
		list.setAdapter(adpater);
	}	
	
	class ListAdpater extends BaseAdapter implements View.OnClickListener
	{
		ArrayList<OznerBluetoothDevice> list=new ArrayList<OznerBluetoothDevice>();
		Context mContext;
		LayoutInflater mInflater;
		public ListAdpater(Context context)
		{
			mContext=context;
			mInflater=LayoutInflater.from(context); 
			this.Reload();
		}
		private void Reload()
		{
			OznerBLEApplication app=(OznerBLEApplication)getApplication();
			OznerBLEBinder service=app.getService();
			if (service==null) return;
			list.clear();
			for (OznerBluetoothDevice device : service.getDeviceManager().getNotBindDevices())
			{
				list.add(device);
			}
			this.notifyDataSetInvalidated();
		}
		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView==null)
			{
				convertView=mInflater.inflate(R.layout.add_device_item, null);
			}
			OznerBluetoothDevice device=(OznerBluetoothDevice)getItem(position);
			SimpleDateFormat fmt=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			if (device!=null)
			{
				((TextView) convertView.findViewById(id.Device_Name)).setText(
						device.getName()+"("+device.getAddress()+")");
				((TextView) convertView.findViewById(id.Device_Model)).setText(
						device.getModel());
				((TextView) convertView.findViewById(id.Device_Platfrom)).setText(
						device.getPlatform());
				((TextView) convertView.findViewById(id.Device_Firmware)).setText(
						fmt.format(new Date(device.getFirmware())));
				
				((TextView) convertView.findViewById(id.Device_Custom)).setText(
						device.getCustomObject()!=null?device.getCustomObject().toString():"");
				if (device.isBindMode())
					convertView.findViewById(id.addDeviceButton).setEnabled(true);
				else
					convertView.findViewById(id.addDeviceButton).setEnabled(false);
				convertView.findViewById(id.addDeviceButton).setTag(device);
				convertView.findViewById(id.addDeviceButton).setOnClickListener(this);
				
			}
			
			return convertView;
		}
		@Override
		public void onClick(View v) {
			if (v.getId()==id.addDeviceButton)
			{
				//获取点击的蓝牙设备
				OznerBluetoothDevice bluetooth=(OznerBluetoothDevice)v.getTag();
				
				OznerBLEApplication app=(OznerBLEApplication)getApplication();
				//获取服务
				OznerBLEBinder service=app.getService();
				OznerDevice device;
				try {
					//通过找到的蓝牙对象控制对象获取设备对象
					device = service.getDeviceManager().getDevice(bluetooth);
					//设置设备名称
					device.Setting().name("test");
					if (device!=null)
					{
						//保存设备
						service.getDeviceManager().save(device);
						//配对完成,重新加载配对设备列表
						this.Reload();
					}
				} catch (NotSupportDevcieException e) {
					e.printStackTrace();
				}
				
			}
		}
	}
}
