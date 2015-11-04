package com.example.oznerble;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.example.oznerble.R.id;
import com.example.oznerble.R.layout;
import com.ozner.application.OznerBLEService.OznerBLEBinder;
import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.FirmwareTools;
import com.ozner.device.OznerDevice;
import com.ozner.tap.Record;
import com.ozner.tap.Tap;
import com.ozner.util.GetPathFromUri4kitkat;
import com.ozner.util.dbg;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TapActivity extends Activity implements View.OnClickListener, FirmwareTools.FirmwareUpateInterface {
    Tap mTap;
    OznerBLEBinder service = null;
    Monitr mMonitor = new Monitr();
    ArrayAdapter<String> adapter;
    ListView record_list;
    RadioButton record_now;
    RadioButton record_hour;
    RadioButton record_day;

    private void WriteMessage(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
        TextView tv = (TextView) this.findViewById(id.messageList);
        tv.append(sdf.format(new Date()) + " " + message + "\n");
    }

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
        if (mTap.Bluetooth() != null) {
            mTap.firmwareTools().setFirmwareUpateInterface(this);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Tap.ACTION_BLUETOOTHTAP_RECORD_COMPLETE);
        filter.addAction(Tap.ACTION_BLUETOOTHTAP_SENSOR);
        filter.addAction(BluetoothIO.ACTION_BLUETOOTH_READY);
        filter.addAction(BluetoothIO.ACTION_BLUETOOTH_DISCONNECTED);
        filter.addAction(BluetoothIO.ACTION_BLUETOOTH_CONNECTED);

        this.registerReceiver(mMonitor, filter);
        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1);
        record_now = (RadioButton) findViewById(id.record_now);
        record_hour = (RadioButton) findViewById(id.record_hour);
        record_day = (RadioButton) findViewById(id.record_day);
        findViewById(id.Device_Remove).setOnClickListener(this);
        findViewById(id.Device_Setup).setOnClickListener(this);
        findViewById(id.Device_Sensor).setOnClickListener(this);

        findViewById(id.UpdateFirmware).setOnClickListener(this);

        record_list = (ListView) findViewById(id.record_list);
        record_list.setAdapter(adapter);
        load();

        TabHost tab = (TabHost) findViewById(R.id.tabHost);

        tab.setup();
        tab.addTab(tab.newTabSpec("tab01").setIndicator("数据").setContent(R.id.tab1));
        tab.addTab(tab.newTabSpec("tab02").setIndicator("日志").setContent(R.id.tab2));
        super.onCreate(savedInstanceState);

        ((TextView) this.findViewById(id.messageList)).setMovementMethod(new ScrollingMovementMethod());
    }

    private void load() {

        ((TextView) findViewById(id.Device_Name)).setText(mTap.getName() +
                (mTap.connectStatus() == BaseDeviceIO.ConnectStatus.Connected ? "(已连接)" : "(未连接)"));

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
        } else

        {
            ((TextView) findViewById(id.Device_Message)).setText("");
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

    int ConnectCount = 0;
    int ErrorCount = 0;
    Date closeTime = new Date();

    class Monitr extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String Address = intent.getStringExtra("Address");
            OznerDevice device = service.getDeviceManager().getDevice(Address);
            if (device != null)
                dbg.i("广播:%s Name:%s", Address, device.getName());

            if (!Address.equals(mTap.Address()))
                return;

            if (action.equals(Tap.ACTION_BLUETOOTHTAP_SENSOR)) {
                ((TextView) findViewById(id.Device_Message)).setText(mTap
                        .GetBluetooth().getSensor().toString());
                return;
            }
            if (action.equals(BluetoothIO.ACTION_BLUETOOTH_READY)) {
                ((TextView) findViewById(id.Device_Name)).setText(mTap.getName() + "(设备已连接)");
                Date now = new Date();
                long time = now.getTime() - closeTime.getTime();
                WriteMessage(String.format("设备就绪 距离上次关闭时间%f秒", time / 1000f));
                ConnectCount++;
                WriteMessage(String.format("成功:%d 失败:%d", ConnectCount, ErrorCount));
            }
            if (action.equals(BluetoothIO.ACTION_BLUETOOTH_DISCONNECTED)) {
                ((TextView) findViewById(id.Device_Name)).setText(mTap.getName() + "(设备未连接)");
                WriteMessage("设备连接断开");
            }

            if (action.equals(BluetoothIO.ACTION_BLUETOOTH_CONNECTED)) {
                WriteMessage("设备连接成功");

            }

            if (action.equals(Tap.ACTION_BLUETOOTHTAP_RECORD_COMPLETE)) {
                load();
                return;
            }
            if (mTap.Bluetooth() != null) {
                mTap.firmwareTools().setFirmwareUpateInterface(TapActivity.this);
            }
        }
    }

    final static int FIRMWARE_SELECT_CODE = 0x1111;

    private void updateFirmware() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*.bin/getFirmware");
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
    public void onFirmwareUpdateStart(String Address) {
        ((TextView) findViewById(id.Update_Message)).setText("开始升级....");

    }

    @Override
    public void onFirmwarePosition(String Address, int Position, int size) {
        TextView tv = (TextView) findViewById(id.Update_Message);
        tv.setText(String.format("进度:%d/%d", Position, size));
        tv.invalidate();
    }

    @Override
    public void onFirmwareComplete(String Address) {
        ((TextView) findViewById(id.Update_Message)).setText("升级完成");
    }

    @Override
    public void onFirmwareFail(String Address) {
        ((TextView) findViewById(id.Update_Message)).setText("升级失败");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FIRMWARE_SELECT_CODE) {
            if (data != null) {

                String path = GetPathFromUri4kitkat.getPath(this,data.getData());

                Toast.makeText(this, path, Toast.LENGTH_LONG).show();
                if (mTap.Bluetooth() != null) {
                    mTap.firmwareTools().udateFirmware(path);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case id.Device_Remove:
                new AlertDialog.Builder(this).setTitle("删除").setMessage("是否要删除设备")
                        .setPositiveButton("是", new AlertDialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                service.getDeviceManager().remove(mTap);
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
                Intent intent = new Intent(this, TapSetupActivity.class);
                intent.putExtra("Address", mTap.Address());
                startActivity(intent);
            }
            break;

            case id.Device_Sensor: {
                break;
            }
            case id.UpdateFirmware: {
                updateFirmware();
                break;
            }

        }
    }


}
