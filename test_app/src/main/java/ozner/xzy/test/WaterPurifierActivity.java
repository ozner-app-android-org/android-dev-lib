package ozner.xzy.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.ozner.WaterPurifier.WaterPurifier;
import com.ozner.WaterPurifier.WaterPurifier_RO_BLE;
import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.OperateCallback;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

import java.util.Date;

public class WaterPurifierActivity extends AppCompatActivity {

    TextView status;
    TextView sendStatus;
    ControlImp controlImp = new ControlImp();
    final Monitor monitor = new Monitor();
    final Handler handler = new Handler();
    WaterPurifier waterPurifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_purifier_acivity);
        waterPurifier = (WaterPurifier) OznerDeviceManager.Instance().getDevice(getIntent().getStringExtra("Address"));
        status = (TextView) findViewById(R.id.status);
        findViewById(R.id.power).setOnClickListener(controlImp);
        findViewById(R.id.remove).setOnClickListener(controlImp);
        findViewById(R.id.hot).setOnClickListener(controlImp);
        findViewById(R.id.cool).setOnClickListener(controlImp);
        findViewById(R.id.resetFilter).setOnClickListener(controlImp);


        if (waterPurifier instanceof WaterPurifier_RO_BLE)
        {
            findViewById(R.id.cool).setEnabled(false);
            findViewById(R.id.hot).setEnabled(false);
            findViewById(R.id.power).setEnabled(false);
            findViewById(R.id.resetFilter).setEnabled(true);
        }
        else
        {
            findViewById(R.id.resetFilter).setEnabled(false);
            findViewById(R.id.cool).setEnabled(true);
            findViewById(R.id.hot).setEnabled(true);
            findViewById(R.id.power).setEnabled(true);
        }
        sendStatus = (TextView) findViewById(R.id.sendStatus);
        loadStatus();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_CONNECTED);
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_CONNECTING);
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_DISCONNECTED);
        intentFilter.addAction(OznerDeviceManager.ACTION_OZNER_MANAGER_DEVICE_CHANGE);
        intentFilter.addAction(WaterPurifier.ACTION_WATER_PURIFIER_STATUS_CHANGE);
        this.registerReceiver(monitor, intentFilter);

    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(monitor);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        loadStatus();
    }

    private void setText(int id, String text) {
        TextView tv = (TextView) findViewById(id);
        if (tv != null) {
            tv.setText(text);
        }
    }

    private String getValue(int v) {
        String text = String.valueOf(v);
        if (v == 0xffff) {
            text = "-";
        }
        return text;
    }

    private void loadStatus() {
        setText(R.id.Device_Name, waterPurifier.getName());
        setText(R.id.Address, waterPurifier.Address());

        setText(R.id.Model, "型号:"+waterPurifier.info().Model);
        setText(R.id.Type, "机型:"+waterPurifier.info().Type);
        if (waterPurifier instanceof WaterPurifier_RO_BLE) {
            WaterPurifier_RO_BLE ro=(WaterPurifier_RO_BLE)waterPurifier;
            if (ro.IO()!=null)
            {
                BluetoothIO bluetoothIO=(BluetoothIO)ro.IO();
                setText(R.id.Type, "固件版本:" + new Date(bluetoothIO.getFirmware()));
            }
        }
        setText(R.id.status, "设备状态:" + (waterPurifier.isOffline() ? "离线" : "在线"));
        setText(R.id.tds1, "TDS1:" + getValue(waterPurifier.sensor().TDS1()));
        setText(R.id.tds2, "TDS2:" + getValue(waterPurifier.sensor().TDS2()));
        setText(R.id.powerStatus, "电源:" + (waterPurifier.status().Power() ? "开" : "关"));
        setText(R.id.hotStatus, "加热:" + (waterPurifier.status().Hot() ? "开" : "关"));
        setText(R.id.coolStatus, "制冷:" + (waterPurifier.status().Cool() ? "开" : "关"));
    }



    class Monitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra(OznerDevice.Extra_Address).equals(waterPurifier.Address())) {
                loadStatus();
            }
        }
    }

    class ControlImp implements View.OnClickListener, OperateCallback<Void> {
        @Override
        public void onFailure(Throwable var1) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    loadStatus();
                    sendStatus.setText("发送状态:发送失败");
                }
            });
        }

        @Override
        public void onSuccess(Void var1) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    loadStatus();
                    sendStatus.setText("发送状态:发送成功");
                }
            });
        }

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.power:
                    if (waterPurifier instanceof WaterPurifier_RO_BLE) return;

                    sendStatus.setText("发送状态:正在发送");
                    waterPurifier.status().setPower(!waterPurifier.status().Power(), this);
                    break;

                case R.id.cool:
                    if (waterPurifier instanceof WaterPurifier_RO_BLE) return;

                    sendStatus.setText("发送状态:正在发送");
                    waterPurifier.status().setCool(!waterPurifier.status().Cool(), this);
                    break;

                case R.id.hot:
                    if (waterPurifier instanceof WaterPurifier_RO_BLE) return;

                    sendStatus.setText("发送状态:正在发送");
                    waterPurifier.status().setHot(!waterPurifier.status().Hot(), this);
                    break;
                case R.id.resetFilter:
                    sendStatus.setText("发送状态:正在发送");
                    if (waterPurifier instanceof WaterPurifier_RO_BLE)
                    {
                        WaterPurifier_RO_BLE ro=(WaterPurifier_RO_BLE)waterPurifier;
                        ro.resetFilter(this);
                    }
                    //waterPurifier.status().setHot(!waterPurifier..status().(), this);
                    break;
                case R.id.remove: {
                    new android.app.AlertDialog.Builder(WaterPurifierActivity.this).setTitle("删除").setMessage("是否要删除设备")
                            .setPositiveButton("是", new AlertDialog.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    OznerDeviceManager.Instance().remove(waterPurifier);
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
                }
            }
        }

    }
}

