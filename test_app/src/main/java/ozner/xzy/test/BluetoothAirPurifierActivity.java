package ozner.xzy.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.ozner.AirPurifier.AirPurifier_Bluetooth;
import com.ozner.AirPurifier.AirPurifier_MXChip;
import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.OperateCallback;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BluetoothAirPurifierActivity extends AppCompatActivity {

    TextView status;
    ControlImp controlImp = new ControlImp();
    final Monitor monitor = new Monitor();

    TextView sendStatus;
    final Handler handler = new Handler();
    AirPurifier_Bluetooth airPurifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_air_purifier);
        airPurifier = (AirPurifier_Bluetooth) OznerDeviceManager.Instance().getDevice(getIntent().getStringExtra("Address"));
        status = (TextView) findViewById(R.id.status);
        findViewById(R.id.power).setOnClickListener(controlImp);
        findViewById(R.id.resetFilter).setOnClickListener(controlImp);
        findViewById(R.id.speak).setOnClickListener(controlImp);
        findViewById(R.id.remove).setOnClickListener(controlImp);
        sendStatus = (TextView) findViewById(R.id.sendStatus);
        loadStatus();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_CONNECTED);
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_CONNECTING);
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_DISCONNECTED);
        intentFilter.addAction(OznerDeviceManager.ACTION_OZNER_MANAGER_DEVICE_CHANGE);
        intentFilter.addAction(AirPurifier_MXChip.ACTION_AIR_PURIFIER_SENSOR_CHANGED);
        intentFilter.addAction(AirPurifier_MXChip.ACTION_AIR_PURIFIER_STATUS_CHANGED);
        this.registerReceiver(monitor, intentFilter);

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
        setText(R.id.Device_Name, airPurifier.getName());
        setText(R.id.Address, airPurifier.Address());
        BluetoothIO io=(BluetoothIO)airPurifier.IO();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (airPurifier.IO()==null)
        {
            setText(R.id.Version,"固件版本:-");
        }else
        {
            Date time=new Date(io.getFirmware());
            setText(R.id.Version,"固件版本:"+dateFormat.format(time));
        }


        setText(R.id.status, "设备状态:" + airPurifier.connectStatus().toString());
        setText(R.id.pm25, "PM2.5:" + getValue(airPurifier.sensor().PM25()));
        setText(R.id.temperature, "温度:" + getValue(airPurifier.sensor().Temperature()));
        setText(R.id.humidity, "湿度:" + getValue(airPurifier.sensor().Humidity()));

        setText(R.id.A2DP, String.format( "A2DP Support:%b" , airPurifier.status().A2DP()));
        setText(R.id.A2DPMAC, String.format("A2DP MAC:%s", airPurifier.status().A2DP_MAC()));
        setText(R.id.A2DPEnable, String.format("A2DP Enable:%s", airPurifier.status().A2DP_Enable()));

        setText(R.id.powerStatus, "电源:" + (airPurifier.status().Power() ? "开" : "关"));

        setText(R.id.workTime, "电机工作时间:" + String.valueOf(airPurifier.sensor().FilterStatus().workTime) + "分钟");
        setText(R.id.maxWorkTime, "最大工作时间:" + String.valueOf(airPurifier.sensor().FilterStatus().maxWorkTime) + "分钟");

        setText(R.id.lastTime, "上次更换时间:" + dateFormat.format(airPurifier.sensor().FilterStatus().lastTime));
        setText(R.id.maxTime, "到期时间:" + dateFormat.format(airPurifier.sensor().FilterStatus().stopTime));

    }



    class Monitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra(OznerDevice.Extra_Address).equals(airPurifier.Address())) {
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
                    sendStatus.setText("发送状态:正在发送");
                    airPurifier.status().setPower(!airPurifier.status().Power(), this);
                    break;


                case R.id.resetFilter: {
                    sendStatus.setText("发送状态:正在发送");
                    airPurifier.ResetFilter(this);
                    break;
                }
                case R.id.speak:
                {
                    airPurifier.setA2DPEnable(true,this);
                    break;
                }
                case R.id.remove: {
                    new android.app.AlertDialog.Builder(BluetoothAirPurifierActivity.this).setTitle("删除").setMessage("是否要删除设备")
                            .setPositiveButton("是", new android.app.AlertDialog.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    OznerDeviceManager.Instance().remove(airPurifier);
                                    finish();
                                }
                            })
                            .setNegativeButton("否", new android.app.AlertDialog.OnClickListener() {
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
