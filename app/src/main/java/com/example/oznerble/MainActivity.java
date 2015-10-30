package com.example.oznerble;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import com.ozner.application.OznerBLEService;
import com.ozner.bluetooth.BluetoothIO;
import com.ozner.cup.Cup;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;
import com.ozner.tap.Tap;
import com.ozner.util.dbg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener,View.OnClickListener {
	ListView list;
	ListAdpater adpater;
	Monitor mMonitor=new Monitor();
	TextView mDbgText=null;
	String Dbg="";
	DbgHandler handler=new DbgHandler();

	class DbgHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg) {
			String text=msg.obj.toString() + "\n";
			mDbgText.append(text);
			if (mDbgText.getScrollY()>=mDbgText.getHeight()) {
				mDbgText.scrollTo(0, mDbgText.getHeight());
			}
			try {
				File file=new File(Environment.getExternalStorageDirectory(),"Ble.txt");
				FileOutputStream stream=new FileOutputStream(file,true);
				OutputStreamWriter writer= new OutputStreamWriter(stream, Charset.forName("UTF-8"));
				Date time=new Date();
				SimpleDateFormat fmt=new SimpleDateFormat("hh:mm:ss");
				writer.write(fmt.format(time)+" "+text);
				writer.flush();
				stream.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

			super.handleMessage(msg);
		}
	}

	@Override
	protected void onStart() {
		dbg.setMessageListener(new dbg.IDbgMessage() {
			@Override
			public void OnMessage(String message) {
				Message m=new Message();
				m.obj=message;
				handler.sendMessage(m);
			}
		});
		super.onStart();
	}

	@Override
	protected void onStop() {
		dbg.setMessageListener(null);
		super.onStop();
	}

	protected void onCreate(Bundle savedInstanceState) {
//		CupFirmwareTools tools= null;
//		try {
//			tools = new CupFirmwareTools("/storage/emulated/0/#Cup#C03-Mar-27-2015-121118.bin","37:16:12:24:03:65");
//			if (tools.Checksum!=29578524)
//			{
//				return;
//			}
//		} catch (CupFirmwareTools.FirmwareException firmwareExcpetion) {
//			firmwareExcpetion.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		setContentView(R.layout.activity_main);
		mDbgText=(TextView)findViewById(R.id.messageList);
		mDbgText.setMovementMethod(new ScrollingMovementMethod());
		TabHost tab=(TabHost)findViewById(R.id.tabHost);

		tab.setup();
		tab.addTab(tab.newTabSpec("tab01").setIndicator("设备列表").setContent(R.id.tab1));
		tab.addTab(tab.newTabSpec("tab02").setIndicator("日志").setContent(R.id.tab2));

		IntentFilter filter=new IntentFilter();
		filter.addAction(OznerDeviceManager.ACTION_OZNER_MANAGER_DEVICE_ADD);
		filter.addAction(OznerDeviceManager.ACTION_OZNER_MANAGER_DEVICE_REMOVE);
		filter.addAction(OznerDeviceManager.ACTION_OZNER_MANAGER_DEVICE_CHANGE);
		filter.addAction(BluetoothIO.ACTION_BLUETOOTH_READY);
		filter.addAction(BluetoothIO.ACTION_BLUETOOTH_DISCONNECTED);
		filter.addAction(Cup.ACTION_BLUETOOTHCUP_SENSOR);
		filter.addAction(Tap.ACTION_BLUETOOTHTAP_SENSOR);
		filter.addAction(OznerApplication.ACTION_ServiceInit);
		filter.addAction(BluetoothIO.ACTION_BLUETOOTH_CONNECTED);
		this.registerReceiver(mMonitor, filter);
		adpater=new ListAdpater();
		list = (ListView) findViewById(R.id.deviceList);
		list.setAdapter(adpater);
		list.setOnItemClickListener(this);
		super.onCreate(savedInstanceState);
		findViewById(R.id.Device_Bind).setOnClickListener(this);
		LoadServiceStatus();

		//Intent intent = new Intent(this, AddDeviceActivity.class);
		//startActivity(intent);
	}
	
	private void LoadServiceStatus() {
		OznerBLEApplication app=(OznerBLEApplication)getApplication();
		OznerBLEService.OznerBLEBinder service=app.getService();
	}
	@Override
	protected void onDestroy() {
		this.unregisterReceiver(mMonitor);
		super.onDestroy();
	}
	@Override
	protected void onPause() {
		super.onPause();
	}
	@Override
	protected void onResume() {
		super.onResume();
		adpater.Reload();
		LoadServiceStatus();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId())
		{
			case R.id.Device_Bind:
				Intent intent=new Intent(this, AddDeviceActivity.class);
				startActivity(intent);
				break;
		}
	}

	class Monitor extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(OznerApplication.ACTION_ServiceInit))
			{
				LoadServiceStatus();
			}
			adpater.Reload();
			
		}
	}
	private OznerBLEService.OznerBLEBinder getService()
	{
		OznerBLEApplication app=(OznerBLEApplication)getApplication();
		return app.getService();
	}
	class ListAdpater extends BaseAdapter
	{
		ArrayList<OznerDevice> mDevices=new ArrayList<OznerDevice>();
		LayoutInflater mInflater;
		public void Reload()
		{
			OznerBLEService.OznerBLEBinder service=getService();
			if (service==null) return;
			mDevices.clear();
			for (OznerDevice device :  service.getDeviceManager().getDevices())
			{
				mDevices.add(device);
			}
			this.notifyDataSetInvalidated();
		}
		public ListAdpater() {
			mInflater=LayoutInflater.from(MainActivity.this);
		}
		@Override
		public int getCount() {
			return mDevices.size();
		}

		@Override
		public Object getItem(int position) {
			return mDevices.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView==null)
			{
				convertView=mInflater.inflate(R.layout.list_device_item, null);
			}
			OznerDevice device=(OznerDevice)getItem(position);
			convertView.setTag(device);
			((TextView) convertView.findViewById(R.id.Device_Name)).setText(
					device.getName()+(device.connected()?"(已连接)":""));
			((TextView) convertView.findViewById(R.id.Device_Address)).setText(
					device.Address());
			String msg="";
			if (device.connected())
			{
				if (device instanceof Cup) {
					Cup cup = (Cup) device;
					msg = cup.GetBluetooth().getSensor() != null ? cup.GetBluetooth().getSensor().toString() : "";
				}
				if (device instanceof Tap) {
					Tap tap = (Tap) device;
					msg = tap.GetBluetooth().getSensor() != null ? tap.GetBluetooth().getSensor().toString() : "";
				}
			}
			((TextView) convertView.findViewById(R.id.Device_Message)).setText(msg);
			return convertView;
		}
	}
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		OznerDevice device=(OznerDevice)view.getTag();
		if (device instanceof Cup)
		{
			Intent intent=new Intent(this, CupActivity.class);
			intent.putExtra("Address", device.Address());
			startActivity(intent);
		}
		if (device instanceof Tap)
		{
			Intent intent=new Intent(this, TapActivity.class);
			intent.putExtra("Address", device.Address());
			startActivity(intent);
		}
	}


}
