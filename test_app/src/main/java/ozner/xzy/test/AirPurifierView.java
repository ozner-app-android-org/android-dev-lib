package ozner.xzy.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.TimePicker;

import com.ozner.AirPurifier.AirPurifier_MXChip;
import com.ozner.AirPurifier.PowerTimer;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.OperateCallback;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by zhiyongxu on 15/11/4.
 */
public class AirPurifierView extends DeviceItemView {
    TextView status;
    TextView sendStatus;
    ControlImp controlImp = new ControlImp();
    final Monitor monitor = new Monitor();
    final Handler handler = new Handler();
    AirPurifier_MXChip airPurifier;

    public AirPurifierView(Context context, AttributeSet attrs) {
        super(context, attrs);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_CONNECTED);
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_CONNECTING);
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_DISCONNECTED);
        intentFilter.addAction(AirPurifier_MXChip.ACTION_AIR_PURIFIER_SENSOR_CHANGED);
        intentFilter.addAction(AirPurifier_MXChip.ACTION_AIR_PURIFIER_STATUS_CHANGED);
        context.registerReceiver(monitor, intentFilter);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        status = (TextView) findViewById(R.id.status);
        findViewById(R.id.power).setOnClickListener(controlImp);
        findViewById(R.id.fanSpeed).setOnClickListener(controlImp);
        findViewById(R.id.lock).setOnClickListener(controlImp);
        findViewById(R.id.resetFilter).setOnClickListener(controlImp);

        findViewById(R.id.powerTimerOn).setOnClickListener(controlImp);
        findViewById(R.id.powerTimerOff).setOnClickListener(controlImp);
        findViewById(R.id.powerTimerSet).setOnClickListener(controlImp);
        findViewById(R.id.powerTimerEnable).setOnClickListener(controlImp);


        sendStatus = (TextView) findViewById(R.id.sendStatus);

        loadStatus();
    }

    private void loadStatus() {
        if (device == null) {
            return;
        }
        setText(R.id.status, "设备状态:" + (airPurifier.isOffline() ? "离线" : "在线"));
        int v = airPurifier.sensor().PM25();
        String text = String.valueOf(v);
        if (v == 0xffff) {
            text = "错误";
        }
        setText(R.id.pm25, "PM2.5:" + text);

        v = airPurifier.sensor().Temperature();
        text = String.valueOf(v);
        if (v == 0xffff) {
            text = "错误";
        }
        setText(R.id.temperature, "温度:" + text);

        v = airPurifier.sensor().VOC();
        text = String.valueOf(v);
        if (v == 0xffff) {
            text = "错误";
        }
        setText(R.id.voc, "VOC:" + text);


        v = airPurifier.sensor().Light();
        text = String.valueOf(v);
        if (v == 0xffff) {
            text = "错误";
        }
        setText(R.id.lightSensor, "环境光:" + text);

        setText(R.id.powerStatus, "电源:" + (airPurifier.airStatus().Power() ? "开" : "关"));

        String speed = "自动";
        switch (airPurifier.airStatus().speed()) {
            case AirPurifier_MXChip.FAN_SPEED_AUTO:
                speed = "自动";
                break;
            case AirPurifier_MXChip.FAN_SPEED_HIGH:
                speed = "高";
                break;
            case AirPurifier_MXChip.FAN_SPEED_MID:
                speed = "中";
                break;
            case AirPurifier_MXChip.FAN_SPEED_LOW:
                speed = "低";
                break;
            case AirPurifier_MXChip.FAN_SPEED_SILENT:
                speed = "静音";
                break;
            case AirPurifier_MXChip.FAN_SPEED_POWER:
                speed = "强力";
                break;
        }
        setText(R.id.speed, "风速:" + speed);

        v = airPurifier.airStatus().Light();
        text = String.valueOf(v);
        if (v == 0xffff) {
            text = "错误";
        }
        setText(R.id.light, "亮度:" + text);

        setText(R.id.lock, "童锁:" + (airPurifier.airStatus().Lock() ? "开" : "关"));
        setText(R.id.workTime, "电机工作时间:" + String.valueOf(airPurifier.sensor().FilterStatus().WorkTime) + "分钟");
        setText(R.id.maxWorkTime, "最大工作时间:" + String.valueOf(airPurifier.sensor().FilterStatus().MaxWorkTime) + "分钟");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        setText(R.id.lastTime, "上次更换时间:" + dateFormat.format(airPurifier.sensor().FilterStatus().lastTime));
        setText(R.id.maxTime, "到期时间:" + dateFormat.format(airPurifier.sensor().FilterStatus().stopTime));




    }

    @Override
    public void loadDevice(OznerDevice device) throws ClassCastException {
        super.loadDevice(device);
        airPurifier = (AirPurifier_MXChip) device();
        loadStatus();
        CheckBox checkBox = (CheckBox) findViewById(R.id.powerTimerEnable);
        checkBox.setChecked(airPurifier.PowerTimer().Enable);

        String text = String.format("关机时间:%02d:%02d", airPurifier.PowerTimer().PowerOffTime / 60,
                airPurifier.PowerTimer().PowerOffTime % 60);
        setText(R.id.powerTimerOff, text);

        text = String.format("开机时间:%02d:%02d", airPurifier.PowerTimer().PowerOnTime / 60,
                airPurifier.PowerTimer().PowerOnTime % 60);
        setText(R.id.powerTimerOn, text);
    }

    class Monitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra(OznerDevice.Extra_Address).equals(device.Address())) {
                loadStatus();
            }
        }
    }

    class ControlImp implements OnClickListener, OperateCallback<Void> {
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

                    airPurifier.airStatus().setPower(!airPurifier.airStatus().Power(),
                            this);
                    break;

                case R.id.fanSpeed:
                    int speedValue = airPurifier.airStatus().speed();
                    speedValue++;
                    if (speedValue > AirPurifier_MXChip.FAN_SPEED_POWER)
                        speedValue = AirPurifier_MXChip.FAN_SPEED_AUTO;
                    sendStatus.setText("发送状态:正在发送");

                    airPurifier.airStatus().setSpeed(speedValue, this);

                    break;

                case R.id.lock:
                    sendStatus.setText("发送状态:正在发送");

                    airPurifier.airStatus().setLock(!airPurifier.airStatus().Lock(),
                            this);
                    break;

                case R.id.powerTimerOn: {
                    final TimePicker picker = new TimePicker(getContext());
                    picker.setIs24HourView(true);
                    short time = airPurifier.PowerTimer().PowerOnTime;
                    int hour = time / 60;
                    int min = time % 60;

                    picker.setCurrentHour(hour);
                    picker.setCurrentMinute(min);

                    new AlertDialog.Builder(getContext()).setTitle("").setView(picker)
                            .setPositiveButton("确定", new AlertDialog.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    airPurifier.PowerTimer().PowerOnTime =
                                            (short) (picker.getCurrentHour() * 60 + picker.getCurrentMinute());
                                    String text = String.format("开机时间:%02d:%02d",
                                            picker.getCurrentHour(), picker.getCurrentMinute());
                                    setText(R.id.powerTimerOn, text);
                                }
                            }).show();
                }
                break;

                case R.id.powerTimerOff: {
                    final TimePicker picker = new TimePicker(getContext());
                    picker.setIs24HourView(true);
                    short time = airPurifier.PowerTimer().PowerOffTime;
                    int hour = time / 60;
                    int min = time % 60;

                    picker.setCurrentHour(hour);
                    picker.setCurrentMinute(min);

                    new AlertDialog.Builder(getContext()).setTitle("").setView(picker)
                            .setPositiveButton("确定", new AlertDialog.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    airPurifier.PowerTimer().PowerOffTime =
                                            (short) (picker.getCurrentHour() * 60 + picker.getCurrentMinute());

                                    String text = String.format("关机时间:%02d:%02d",
                                            picker.getCurrentHour(), picker.getCurrentMinute());

                                    setText(R.id.powerTimerOff, text);
                                }
                            }).show();
                }
                break;
                case R.id.powerTimerEnable: {
                    CheckBox cb = (CheckBox) view;
                    airPurifier.PowerTimer().Enable = cb.isChecked();
                    break;
                }

                case R.id.powerTimerSet: {
                    airPurifier.PowerTimer().Week =
                            PowerTimer.Monday | PowerTimer.Tuesday | PowerTimer.Wednesday |
                                    PowerTimer.Trusday | PowerTimer.Firday | PowerTimer.Sturday |
                                    PowerTimer.Sunday;

                    OznerDeviceManager.Instance().save(device);
                    break;
                }
                case R.id.resetFilter: {
                    sendStatus.setText("发送状态:正在发送");

                    airPurifier.ResetFilter(this);
                    break;
                }
            }

        }
    }
}
