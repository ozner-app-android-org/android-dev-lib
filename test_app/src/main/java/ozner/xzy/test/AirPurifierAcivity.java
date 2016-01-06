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

import com.ozner.AirPurifier.AirPurifier_MXChip;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.OperateCallback;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class AirPurifierAcivity extends AppCompatActivity {

    TextView status;
    TextView sendStatus;
    ControlImp controlImp = new ControlImp();
    final Monitor monitor = new Monitor();
    final Handler handler = new Handler();
    AirPurifier_MXChip airPurifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_air_purifier_acivity);
        airPurifier = (AirPurifier_MXChip) OznerDeviceManager.Instance().getDevice(getIntent().getStringExtra("Address"));
        status = (TextView) findViewById(R.id.status);
        findViewById(R.id.power).setOnClickListener(controlImp);
        findViewById(R.id.fanSpeed).setOnClickListener(controlImp);
        findViewById(R.id.lock).setOnClickListener(controlImp);
        findViewById(R.id.resetFilter).setOnClickListener(controlImp);
        findViewById(R.id.setup).setOnClickListener(controlImp);
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
        setText(R.id.Version,"固件版本:"+getValue(airPurifier.airStatus().Version()));

        setText(R.id.status, "设备状态:" + (airPurifier.isOffline() ? "离线" : "在线"));

        setText(R.id.pm25, "PM2.5:" + getValue(airPurifier.sensor().PM25()));

        setText(R.id.temperature, "温度:" + getValue(airPurifier.sensor().Temperature()));

        setText(R.id.voc, "VOC:" + getValue(airPurifier.sensor().VOC()));


        setText(R.id.lightSensor, "环境光:" + getValue(airPurifier.sensor().Light()));


        setText(R.id.humidity, "湿度:" + getValue(airPurifier.sensor().Humidity()));


        setText(R.id.powerStatus, "电源:" + (airPurifier.airStatus().Power() ? "开" : "关"));

        String speed = "自动";
        switch (airPurifier.airStatus().speed()) {
            case AirPurifier_MXChip.FAN_SPEED_AUTO:
                speed = "自动";
                break;
//            case AirPurifier_MXChip.FAN_SPEED_HIGH:
//                speed = "高";
//                break;
//            case AirPurifier_MXChip.FAN_SPEED_MID:
//                speed = "中";
//                break;
//            case AirPurifier_MXChip.FAN_SPEED_LOW:
//                speed = "低";
//                break;
            case AirPurifier_MXChip.FAN_SPEED_SILENT:
                speed = "静音";
                break;
            case AirPurifier_MXChip.FAN_SPEED_POWER:
                speed = "强力";
                break;
        }
        setText(R.id.speed, "风速:" + speed);

        setText(R.id.light, "亮度:" + getValue(airPurifier.airStatus().Light()));

        setText(R.id.lockStatus, "童锁:" + (airPurifier.airStatus().Lock() ? "开" : "关"));
        setText(R.id.workTime, "电机工作时间:" + String.valueOf(airPurifier.sensor().FilterStatus().workTime) + "分钟");
        setText(R.id.maxWorkTime, "最大工作时间:" + String.valueOf(airPurifier.sensor().FilterStatus().maxWorkTime) + "分钟");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
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
        int[] SpeedValues={AirPurifier_MXChip.FAN_SPEED_AUTO,AirPurifier_MXChip.FAN_SPEED_POWER,AirPurifier_MXChip.FAN_SPEED_SILENT};
        int sv=0;
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.power:
                    sendStatus.setText("发送状态:正在发送");
                    airPurifier.airStatus().setPower(!airPurifier.airStatus().Power(),
                            this);
                    break;

                case R.id.fanSpeed:
                    int speedValue = airPurifier.airStatus().speed();
                    switch (speedValue)
                    {
                        case AirPurifier_MXChip.FAN_SPEED_AUTO:
                            speedValue=AirPurifier_MXChip.FAN_SPEED_POWER;
                            break;
                        case AirPurifier_MXChip.FAN_SPEED_POWER:
                            speedValue=AirPurifier_MXChip.FAN_SPEED_SILENT;
                            break;
                        case AirPurifier_MXChip.FAN_SPEED_SILENT:
                            speedValue=AirPurifier_MXChip.FAN_SPEED_AUTO;
                            break;
                    }
                    sendStatus.setText("发送状态:正在发送");
                     airPurifier.airStatus().setSpeed(speedValue, this);
                    break;

                case R.id.lock:
                    sendStatus.setText("发送状态:正在发送");
                    airPurifier.airStatus().setLock(!airPurifier.airStatus().Lock(),
                            this);
                    break;

                case R.id.resetFilter: {
                    sendStatus.setText("发送状态:正在发送");
                    airPurifier.ResetFilter(this);
                    break;
                }
                case R.id.setup: {
                    Intent intent=new Intent(AirPurifierAcivity.this,AirPurifierSetupActivity.class);
                    intent.putExtra("Address",airPurifier.Address());
                    startActivityForResult(intent,0);
                    break;
                }
                case R.id.remove: {
                    new android.app.AlertDialog.Builder(AirPurifierAcivity.this).setTitle("删除").setMessage("是否要删除设备")
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

