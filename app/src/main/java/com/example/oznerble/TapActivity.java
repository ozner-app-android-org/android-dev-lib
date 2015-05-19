package com.example.oznerble;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.example.oznerble.R.id;
import com.example.oznerble.R.layout;
import com.ozner.application.OznerBLEService.OznerBLEBinder;
import com.ozner.cup.BluetoothCup;
import com.ozner.device.OznerBluetoothDevice;
import com.ozner.device.OznerDevice;
import com.ozner.tap.BluetoothTap;
import com.ozner.tap.Record;
import com.ozner.tap.Tap;
import com.ozner.util.dbg;

public class TapActivity extends Activity implements View.OnClickListener {
	Tap mTap;
	OznerBLEBinder service = null;
	Monitr mMonitor = new Monitr();
	ArrayAdapter<String> adapter;
	ListView record_list;
	RadioButton record_now;
	RadioButton record_hour;
	RadioButton record_day;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(layout.activity_tap);
		OznerBLEApplication app = (OznerBLEApplication) this.getApplication();
		service = app.getService();
		if (service == null)
			return;
		mTap = service.getTapManager().getTap(getIntent().getStringExtra("Address"));
		if (mTap == null)
			return;
		IntentFilter filter = new IntentFilter();
		filter.addAction(Tap.ACTION_BLUETOOTHTAP_RECORD_COMPLETE);
		filter.addAction(BluetoothTap.ACTION_BLUETOOTHTAP_SENSOR);
		filter.addAction(OznerBluetoothDevice.ACTION_BLUETOOTH_READLY);
		filter.addAction(OznerBluetoothDevice.ACTION_BLUETOOTH_DISCONNECTED);
		filter.addAction(OznerBluetoothDevice.ACTION_BLUETOOTH_CONNECTED);
		
		this.registerReceiver(mMonitor, filter);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		record_now = (RadioButton) findViewById(id.record_now);
		record_hour = (RadioButton) findViewById(id.record_hour);
		record_day = (RadioButton) findViewById(id.record_day);
		findViewById(id.Device_Remove).setOnClickListener(this);
		findViewById(id.Device_Setup).setOnClickListener(this);
		findViewById(id.Device_Sensor).setOnClickListener(this);
		record_list=(ListView)findViewById(id.record_list);
		record_list.setAdapter(adapter);
		load();
		super.onCreate(savedInstanceState);
	}

	private void load() {

		((TextView) findViewById(id.Device_Name)).setText(mTap.getName()+
				(mTap.connected()?"(已连接)":"(未连接)"));
		
		if (mTap.Bluetooth() != null) {
			((TextView) findViewById(id.Device_Model)).setText(mTap.Bluetooth()
					.getModel());
			((TextView) findViewById(id.Device_Platfrom)).setText(mTap
					.Bluetooth().getPlatform());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			((TextView) findViewById(id.Device_Firmware)).setText(sdf
					.format(new Date(mTap.Bluetooth().getFirmware())));
			((TextView) findViewById(id.Device_Message)).setText(mTap
					.GetBluetooth().getSensor().toString());
		}
		adapter.clear();
		
		for (Record r : mTap.Datas().getRecordsByDate(new Date(0))) {
			adapter.add(r.toString());
		}
		
	}

	@Override
	protected void onDestroy() {
		this.unregisterReceiver(mMonitor);
		super.onDestroy();
	}

	class Monitr extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			String Address = intent.getStringExtra("Address");
			OznerDevice device=service.getDeviceManager().getDevice(Address);
			if (device!=null)
				dbg.i("广播:%s Name:%s",Address,device.getName());

			if (!Address.equals(mTap.Address()))
				return;
			
			if (action.equals(BluetoothTap.ACTION_BLUETOOTHTAP_SENSOR)) {
				((TextView) findViewById(id.Device_Message)).setText(mTap
						.GetBluetooth().getSensor().toString());
				return;
			}
			if (action.equals(BluetoothCup.ACTION_BLUETOOTH_READLY))
			{
				((TextView) findViewById(id.Device_Name)).setText(mTap.getName()+"(设备已连接)");
			}
			if (action.equals(BluetoothCup.ACTION_BLUETOOTH_DISCONNECTED))
			{
				((TextView) findViewById(id.Device_Name)).setText(mTap.getName()+"(设备未连接)");
			}
			if (action.equals(Tap.ACTION_BLUETOOTHTAP_RECORD_COMPLETE)) {
				load();
				return;
			}
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case id.Device_Remove:
			new AlertDialog.Builder(this).setTitle("提示ʾ").setMessage("是否要删除设备")
			.setPositiveButton("是", new AlertDialog.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					service.getDeviceManager().remove(mTap);
					finish();
				}
			}).show();
			break;
		case id.Device_Setup: {
			Intent intent = new Intent(this, TapSetupActivity.class);
			intent.putExtra("Address", mTap.Address());
			startActivity(intent);
		}
			break;
			
		case id.Device_Sensor:
		{
			if (mTap.GetBluetooth()!=null)
			{
				mTap.GetBluetooth().requestSensor();
			}
		}
		break;
		}
	}

	
}
