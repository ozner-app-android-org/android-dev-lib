package ozner.xzy.test;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ozner.application.OznerBLEService.OznerBLEBinder;
import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.FirmwareTools;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;
import com.ozner.tap.Tap;
import com.ozner.tap.TapRecord;
import com.ozner.util.GetPathFromUri4kitkat;
import com.ozner.util.dbg;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TapActivity extends Activity implements View.OnClickListener, FirmwareTools.FirmwareUpateInterface {
    final static int FIRMWARE_SELECT_CODE = 0x1111;
    Tap mTap;
    Monitor mMonitor = new Monitor();
    class MyAdapter extends ArrayAdapter<String>
    {
        public MyAdapter(Context context, int resource) {
            super(context, resource);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView=(TextView) super.getView(position, convertView, parent);
            textView.setTextColor(getContext().getResources().getColor(R.color.secondary_text_default_material_light));
            return textView;
        }
    }

    MyAdapter adapter;

    ListView record_list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_tap);
        mTap =(Tap)OznerDeviceManager.Instance().getDevice(getIntent().getStringExtra("Address"));
        if (mTap == null)
            return;

        mTap.firmwareTools().setFirmwareUpateInterface(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Tap.ACTION_BLUETOOTHTAP_RECORD_COMPLETE);
        filter.addAction(Tap.ACTION_BLUETOOTHTAP_SENSOR);
        filter.addAction(BaseDeviceIO.ACTION_DEVICE_CONNECTING);
        filter.addAction(BaseDeviceIO.ACTION_DEVICE_DISCONNECTED);
        filter.addAction(BaseDeviceIO.ACTION_DEVICE_CONNECTED);
        this.registerReceiver(mMonitor, filter);
        adapter = new MyAdapter(this, android.R.layout.simple_list_item_activated_1);

        findViewById(R.id.Device_Remove).setOnClickListener(this);
        findViewById(R.id.Device_Setup).setOnClickListener(this);
        findViewById(R.id.Device_Sensor).setOnClickListener(this);

        findViewById(R.id.UpdateFirmware).setOnClickListener(this);

        record_list = (ListView) findViewById(R.id.record_list);
        record_list.setAdapter(adapter);
        load();
        super.onCreate(savedInstanceState);
    }

    private void load() {
        ((TextView) findViewById(R.id.Device_Name)).setText(String.format("%s (%s)",mTap.getName(),mTap.connectStatus()));

        if (mTap.connectStatus()== BaseDeviceIO.ConnectStatus.Connected) {
            BluetoothIO io=(BluetoothIO)mTap.IO();
                    ((TextView) findViewById(R.id.Device_Model)).setText(io.getType());
            ((TextView) findViewById(R.id.Device_Platfrom)).setText(io.getPlatform());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            ((TextView) findViewById(R.id.Device_Firmware)).setText(sdf
                    .format(new Date(io.getFirmware())));
            ((TextView) findViewById(R.id.Device_Message)).setText(mTap.Sensor().toString());
        } else

        {
            ((TextView) findViewById(R.id.Device_Message)).setText("");
        }

    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(mMonitor);
        super.onDestroy();
    }

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
        ((TextView) findViewById(R.id.Update_Message)).setText("开始升级....");

    }

    @Override
    public void onFirmwarePosition(String Address, int Position, int size) {
        TextView tv = (TextView) findViewById(R.id.Update_Message);
        tv.setText(String.format("进度:%d/%d", Position, size));
        tv.invalidate();
    }

    @Override
    public void onFirmwareComplete(String Address) {
        ((TextView) findViewById(R.id.Update_Message)).setText("升级完成");
    }

    @Override
    public void onFirmwareFail(String Address) {
        ((TextView) findViewById(R.id.Update_Message)).setText("升级失败");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FIRMWARE_SELECT_CODE) {
            if (data != null) {

                String path = GetPathFromUri4kitkat.getPath(this, data.getData());

                Toast.makeText(this, path, Toast.LENGTH_LONG).show();
                if (mTap.connectStatus()== BaseDeviceIO.ConnectStatus.Connected) {
                    mTap.firmwareTools().udateFirmware(path);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.Device_Remove:
                new AlertDialog.Builder(this).setTitle("删除").setMessage("是否要删除设备")
                        .setPositiveButton("是", new AlertDialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                OznerDeviceManager.Instance().remove(mTap);
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
            case R.id.Device_Setup: {
                Intent intent = new Intent(this, TapSetupActivity.class);
                intent.putExtra("Address", mTap.Address());
                startActivity(intent);
            }
            break;

            case R.id.Device_Sensor: {
                break;
            }
            case R.id.UpdateFirmware: {
                updateFirmware();
                break;
            }

        }
    }

    class Monitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String Address = intent.getStringExtra("Address");
            OznerDevice device = OznerDeviceManager.Instance().getDevice(Address);
            if (device != null)
                dbg.i("广播:%s Name:%s", Address, device.getName());


            if (!Address.equals(mTap.Address()))
                return;

            load();

            if (action.equals(Tap.ACTION_BLUETOOTHTAP_SENSOR)) {
                ((TextView) findViewById(R.id.Device_Message)).setText(mTap.Sensor().toString());
                return;
            }
            if (action.equals(Tap.ACTION_BLUETOOTHTAP_RECORD_COMPLETE)) {
                adapter.clear();
                for (TapRecord r : mTap.TapRecordList().getRecordsByDate(new Date(0))) {
                    adapter.add(r.toString());
                }
                return;
            }
        }
    }


}
