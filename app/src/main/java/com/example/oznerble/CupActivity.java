package com.example.oznerble;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.oznerble.R.id;
import com.example.oznerble.R.layout;
import com.ozner.application.OznerBLEService.OznerBLEBinder;
import com.ozner.cup.BluetoothCup;
import com.ozner.cup.Cup;
import com.ozner.cup.CupRecord;
import com.ozner.cup.Record;
import com.ozner.device.FirmwareTools;
import com.ozner.device.FirmwareTools.FirmwareExcpetion;
import com.ozner.device.OznerBluetoothDevice;
import com.ozner.util.dbg;

@SuppressLint("SimpleDateFormat")
public class CupActivity extends Activity implements OnClickListener {
	Cup mCup;
	OznerBLEBinder service = null;
	Monitr mMonitor = new Monitr();
	ArrayList<CupRecord> mRecords = new ArrayList<CupRecord>();
	ArrayAdapter<String> adapter;
	ListView record_list;
	RadioButton record_now;
	RadioButton record_hour;
	RadioButton record_day;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(layout.activity_cup);
		OznerBLEApplication app = (OznerBLEApplication) this.getApplication();
		service = app.getService();
		if (service == null)
			return;
		mCup = service.getCupManager().getCup(
				getIntent().getStringExtra("Address"));
		if (mCup == null)
			return;
		IntentFilter filter = new IntentFilter();
		filter.addAction(Cup.ACTION_BLUETOOTHCUP_RECORD_COMPLETE);
		filter.addAction(OznerBluetoothDevice.ACTION_BLUETOOTH_READLY);
		filter.addAction(OznerBluetoothDevice.ACTION_BLUETOOTH_DISCONNECTED);
		filter.addAction(OznerBluetoothDevice.ACTION_BLUETOOTH_CONNECTED);
		filter.addAction(BluetoothCup.ACTION_BLUETOOTHCUP_SENSOR);
		this.registerReceiver(mMonitor, filter);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		record_now = (RadioButton) findViewById(id.record_now);
		record_hour = (RadioButton) findViewById(id.record_hour);
		record_day = (RadioButton) findViewById(id.record_day);
		findViewById(id.Device_Remove).setOnClickListener(this);
		findViewById(id.Device_Setup).setOnClickListener(this);
		findViewById(id.record_now).setOnClickListener(this);
		findViewById(id.record_hour).setOnClickListener(this);
		findViewById(id.record_day).setOnClickListener(this);
		findViewById(id.UpdateFirmware).setOnClickListener(this);
		
		record_list = (ListView) findViewById(id.record_list);
		record_list.setAdapter(adapter);
		load();
		super.onCreate(savedInstanceState);
	}

	private void load() {

		((TextView) findViewById(id.Device_Name)).setText(mCup.getName()
				+ (mCup.connected() ? "(设备已连接)" : "(设备未连接)"));
		if (mCup.Bluetooth() != null) {
			((TextView) findViewById(id.Device_Model)).setText(mCup.Bluetooth()
					.getModel());
			((TextView) findViewById(id.Device_Platfrom)).setText(mCup
					.Bluetooth().getPlatform());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			((TextView) findViewById(id.Device_Firmware)).setText(sdf
					.format(new Date(mCup.Bluetooth().getFirmware())));
			((TextView) findViewById(id.Device_Message)).setText(mCup
					.GetBluetooth().getSensor().toString());
		}
		if (record_now.isChecked()) {
			adapter.clear();
			for (CupRecord r : mRecords) {
				adapter.add(r.toString());
			}
		}
		if (record_day.isChecked()) {
			adapter.clear();
			for (Record r : mCup.Volume().getRecordsByDate(new Date(0))) {
				adapter.add(r.toString());
			}
		}
		if (record_hour.isChecked()) {
			adapter.clear();
			for (Record r : mCup.Volume().getToday()) {
				adapter.add(r.toString());
			}
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
			if (!Address.equals(mCup.Address()))
				return;
			if (mCup.GetBluetooth()==null) return;
			if (action.equals(BluetoothCup.ACTION_BLUETOOTHCUP_SENSOR)) {
					
				((TextView) findViewById(id.Device_Message)).setText(mCup
						.GetBluetooth().getSensor().toString());
				return;
			}
			if (action.equals(BluetoothCup.ACTION_BLUETOOTH_READLY)) {
				((TextView) findViewById(id.Device_Name)).setText(mCup
						.getName() + "(设备已连接)");
			}
			if (action.equals(BluetoothCup.ACTION_BLUETOOTH_DISCONNECTED)) {
				((TextView) findViewById(id.Device_Name)).setText(mCup
						.getName() + "(设备未连接)");
			}
			if (action.equals(Cup.ACTION_BLUETOOTHCUP_RECORD_COMPLETE)) {
				CupRecord[] records = mCup.GetBluetooth().GetReocrds();
				if (records != null) {
					dbg.i("ACTION_BLUETOOTHCUP_RECORD_COMPLETE:"
							+ records.length);
					for (CupRecord record : mCup.GetBluetooth().GetReocrds()) {
						mRecords.add(record);
					}
				}
				load();
				return;
			}
		}
	}

	final static int FIRMWARE_SELECT_CODE = 0x1111;

	private void updateFirmware() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*.bin/Firmware");
		intent.addCategory(Intent.CATEGORY_OPENABLE);

		try {
			startActivityForResult(
					Intent.createChooser(intent, "Select a File to Upload"),
					FIRMWARE_SELECT_CODE);
			
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(this, "Please install a File Manager.",
					Toast.LENGTH_SHORT).show();
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode==FIRMWARE_SELECT_CODE)
		{
			if (data!=null)
			{
				Uri uri = data.getData();
				String[] proj = { MediaStore.Images.Media.DATA };
				Cursor actualimagecursor = managedQuery(uri,proj,null,null,null);
				int actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
				actualimagecursor.moveToFirst();
				String path = actualimagecursor.getString(actual_image_column_index);
            	Toast.makeText(this, path, Toast.LENGTH_LONG).show();
            	try {
					FirmwareTools tools=new FirmwareTools(path);
					tools.toString();
				} catch (Exception e) {
					Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}
            	
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case id.record_day:
		case id.record_hour:
		case id.record_now:
			load();
			break;
		case id.Device_Remove:
			new AlertDialog.Builder(this).setTitle("删除").setMessage("是否要删除设备")
					.setPositiveButton("是", new AlertDialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							service.getDeviceManager().remove(mCup);
							finish();
						}
					})
					.setNegativeButton("否", new AlertDialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					}).show();
			break;
		case id.Device_Setup: {
			Intent intent = new Intent(this, CupSetupActivity.class);
			intent.putExtra("Address", mCup.Address());
			startActivity(intent);
		}
			break;
		case id.UpdateFirmware: {
			updateFirmware();
			break;
		}
		}
	}

}
