package ozner.xzy.test;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ozner.WaterReplenishmentMeter.WaterReplenishmentMeter;
import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.FirmwareTools;
import com.ozner.device.OperateCallback;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;
import com.ozner.util.GetPathFromUri4kitkat;
import com.ozner.util.dbg;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WaterReplenishmentMeterActivity extends Activity implements View.OnClickListener, FirmwareTools.FirmwareUpateInterface {
    final static int FIRMWARE_SELECT_CODE = 0x1111;
    WaterReplenishmentMeter waterReplenishmentMeter;
    Monitor mMonitor = new Monitor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_water_replenishment_meter);
        waterReplenishmentMeter = (WaterReplenishmentMeter) OznerDeviceManager.Instance().getDevice(getIntent().getStringExtra("Address"));
        if (waterReplenishmentMeter == null)
            return;

        waterReplenishmentMeter.firmwareTools().setFirmwareUpateInterface(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(OznerDevice.ACTION_DEVICE_UPDATE);
        filter.addAction(OznerDeviceManager.ACTION_OZNER_MANAGER_DEVICE_CHANGE);
        filter.addAction(BaseDeviceIO.ACTION_DEVICE_CONNECTING);
        filter.addAction(BaseDeviceIO.ACTION_DEVICE_DISCONNECTED);
        filter.addAction(BaseDeviceIO.ACTION_DEVICE_CONNECTED);
        this.registerReceiver(mMonitor, filter);

        findViewById(R.id.Device_Remove).setOnClickListener(this);
//        findViewById(R.id.Device_Test).setOnClickListener(this);
        findViewById(R.id.UpdateFirmware).setOnClickListener(this);

        load();
        super.onCreate(savedInstanceState);
    }

    private void load() {
        ((TextView) findViewById(R.id.Device_Name)).setText(String.format("%s (%s)",
                waterReplenishmentMeter.getName(), waterReplenishmentMeter.connectStatus()));
        ((TextView) findViewById(R.id.Address)).setText(waterReplenishmentMeter.Address());
        setText(R.id.Device_StatusText,waterReplenishmentMeter.toString());

        if (waterReplenishmentMeter.connectStatus() == BaseDeviceIO.ConnectStatus.Connected) {
            BluetoothIO io = (BluetoothIO) waterReplenishmentMeter.IO();
            ((TextView) findViewById(R.id.Device_Model)).setText(io.getType());
            ((TextView) findViewById(R.id.Device_Platform)).setText(io.getPlatform());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            ((TextView) findViewById(R.id.Device_Firmware)).setText(sdf
                    .format(new Date(io.getFirmware())));
        } else

        {
            ((TextView) findViewById(R.id.Device_Message)).setText("");
        }

    }


    private void setText(int id, String text) {
        TextView tv = (TextView) findViewById(id);
        if (tv != null) {
            tv.setText(text);
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
                if (waterReplenishmentMeter.connectStatus() == BaseDeviceIO.ConnectStatus.Connected) {
                    waterReplenishmentMeter.firmwareTools().udateFirmware(path);
                }
            }
        }
        load();
        super.onActivityResult(requestCode, resultCode, data);
    }
    Handler TestHandler=new Handler(Looper.getMainLooper())
    {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what==0x1234)
            {
                setText(R.id.Device_Message,msg.obj.toString());
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.Device_Remove:
                new AlertDialog.Builder(this).setTitle("删除").setMessage("是否要删除设备")
                        .setPositiveButton("是", new AlertDialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                OznerDeviceManager.Instance().remove(waterReplenishmentMeter);
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
//            case R.id.Device_Test: {
//                setText(R.id.Device_Message,"正在测试...");
//                waterReplenishmentMeter.startTest(Kettle.TestParts.Face, new OperateCallback<Float>() {
//                    @Override
//                    public void onSuccess(Float var1) {
//                        Message msg=new Message();
//                        msg.what=0x1234;
//                        msg.obj=String.format("测试结果:%s",var1);
//                        TestHandler.sendMessage(msg);
//                    }
//
//                    @Override
//                    public void onFailure(Throwable var1) {
//                        Message msg=new Message();
//                        msg.what=0x1234;
//                        msg.obj="测试失败";
//                        TestHandler.sendMessage(msg);
//                    }
//                });
//            }
//            break;

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

            if (!Address.equals(waterReplenishmentMeter.Address()))
                return;

            load();


        }
    }


}
