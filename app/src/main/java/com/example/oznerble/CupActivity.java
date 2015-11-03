package com.example.oznerble;

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
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
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
import com.ozner.bluetooth.BluetoothIO;
import com.ozner.cup.Cup;
import com.ozner.cup.CupRecord;
import com.ozner.cup.Record;
import com.ozner.device.FirmwareTools;
import com.ozner.util.GetPathFromUri4kitkat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

@SuppressLint("SimpleDateFormat")
public class CupActivity extends Activity implements OnClickListener, FirmwareTools.FirmwareUpateInterface {
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
		filter.addAction(BluetoothIO.ACTION_BLUETOOTH_READY);
		filter.addAction(BluetoothIO.ACTION_BLUETOOTH_DISCONNECTED);
		filter.addAction(BluetoothIO.ACTION_BLUETOOTH_CONNECTED);
		filter.addAction(Cup.ACTION_BLUETOOTHCUP_SENSOR);
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

		((TextView) findViewById(id.Device_Name)).setText(mCup.Setting().name()
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
		if (mCup.Bluetooth() != null) {
			mCup.firmwareTools().setFirmwareUpateInterface(this);
		}
	}

	@Override
	protected void onDestroy() {
		this.unregisterReceiver(mMonitor);
		super.onDestroy();
	}

	@Override
	public void onFirmwareUpdateStart(String Address) {
		((TextView) findViewById(id.Update_Message)).setText("开始升级....");

	}

	@Override
	public void onFirmwarePosition(String Address, int Position, int size) {
		TextView tv = (TextView) findViewById(id.Update_Message);
		tv.setText(String.format("进度:%d/%d", Position, size));
	}

	@Override
	public void onFirmwareComplete(String Address) {
		((TextView) findViewById(id.Update_Message)).setText("升级完成");
	}

	@Override
	public void onFirmwareFail(String Address) {
		((TextView) findViewById(id.Update_Message)).setText("升级失败");
	}

	class Monitr extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			String Address = intent.getStringExtra("Address");
			if (!Address.equals(mCup.Address()))
				return;
			if (mCup.GetBluetooth()==null) return;
			if (action.equals(Cup.ACTION_BLUETOOTHCUP_SENSOR)) {
					
				((TextView) findViewById(id.Device_Message)).setText(mCup
						.GetBluetooth().getSensor().toString());
				return;
			}
			if (action.equals(BluetoothIO.ACTION_BLUETOOTH_READY)) {
				((TextView) findViewById(id.Device_Name)).setText(mCup
						.Setting().name() + "(设备已连接)");
				if (mCup.Bluetooth() != null) {
					mCup.firmwareTools().setFirmwareUpateInterface(CupActivity.this);
				}
			}
			if (action.equals(BluetoothIO.ACTION_BLUETOOTH_DISCONNECTED)) {
				((TextView) findViewById(id.Device_Name)).setText(mCup
						.Setting().name() + "(设备未连接)");
			}
			if (action.equals(Cup.ACTION_BLUETOOTHCUP_RECORD_COMPLETE)) {
				load();
				return;
			}
		}
	}

	final static int FIRMWARE_SELECT_CODE = 0x1111;

	private void updateFirmware() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*/*");
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
	/**
	 * Get the value of the data column for this Uri. This is useful for
	 * MediaStore Uris, and other file-based ContentProviders.
	 *
	 * @param context
	 *            The context.
	 * @param uri
	 *            The Uri to query.
	 * @param selection
	 *            (Optional) Filter used in the query.
	 * @param selectionArgs
	 *            (Optional) Selection arguments used in the query.
	 * @return The value of the _data column, which is typically a file path.
	 */
	public static String getDataColumn(Context context, Uri uri, String selection,
									   String[] selectionArgs) {

		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = { column };

		try {
			cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
					null);
			if (cursor != null && cursor.moveToFirst()) {
				final int column_index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(column_index);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode==FIRMWARE_SELECT_CODE)
		{
			if (data!=null)
			{
				String path= GetPathFromUri4kitkat.getPath(this,data.getData());
				Toast.makeText(this, path, Toast.LENGTH_LONG).show();
				if (mCup.Bluetooth() != null) {
					mCup.firmwareTools().udateFirmware(path);
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
