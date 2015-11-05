package ozner.xzy.test;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.dd.CircularProgressButton;
import com.ozner.WaterPurifier.WaterPurifier;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.OperateCallback;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

/**
 * Created by zhiyongxu on 15/11/4.
 */
public class WaterPurifierView extends BaseItemView {
    WaterPurifier device;
    TextView name;
    TextView mac;
    TextView status;
    TextView tds1;
    TextView tds2;
    TextView powerStatus;
    TextView hotStatus;
    TextView coolStatus;

    CircularProgressButton power;
    CircularProgressButton cool;
    CircularProgressButton hot;
    ControlImp controlImp = new ControlImp();
    Monitor monitor = new Monitor();

    class Monitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra(OznerDevice.Extra_Address).equals(device.Address())) {
                loadStatus();
            }
        }
    }

    public WaterPurifierView(Context context, AttributeSet attrs) {
        super(context, attrs);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_CONNECTED);
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_CONNECTING);
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_DISCONNECTED);
        intentFilter.addAction(WaterPurifier.ACTION_WATER_PURIFIER_STATUS_CHANGE);
        context.registerReceiver(monitor, intentFilter);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        name = (TextView) findViewById(R.id.name);
        mac = (TextView) findViewById(R.id.mac);
        status = (TextView) findViewById(R.id.status);
        tds1 = (TextView) findViewById(R.id.tds1);
        tds2 = (TextView) findViewById(R.id.tds2);

        powerStatus = (TextView) findViewById(R.id.powerStatus);
        hotStatus = (TextView) findViewById(R.id.hotStatus);
        coolStatus = (TextView) findViewById(R.id.coolStatus);


        power = (CircularProgressButton) findViewById(R.id.power);
        power.setOnClickListener(controlImp);

        cool = (CircularProgressButton) findViewById(R.id.cool);
        cool.setOnClickListener(controlImp);

        hot = (CircularProgressButton) findViewById(R.id.hot);
        hot.setOnClickListener(controlImp);
        loadStatus();

        findViewById(R.id.delete).setOnClickListener(controlImp);

    }

    Handler handler = new Handler();

    private void loadStatus() {
        if (device == null) {
            return;
        }

        name.setText(device.getName());
        mac.setText(device.Address());
        status.setText("连接状态:" + (device.isOffline() ? "离线" : "在线"));
        tds1.setText("TDS1:" + String.valueOf(device.TDS1()));
        tds2.setText("TDS2:" + String.valueOf(device.TDS2()));
        powerStatus.setText("电源:" + (device.Power() ? "开" : "关"));
        coolStatus.setText("制冷:" + (device.Cool() ? "开" : "关"));
        hotStatus.setText("加热:" + (device.Hot() ? "开" : "关"));

    }

    class ControlImp implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.power:
                    power.setProgress(0);
                    power.setProgress(50);
                    device.setPower(!device.Power(), new OperateCallback<Void>() {
                        @Override
                        public void onSuccess(Void var1) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    loadStatus();
                                    power.setProgress(100);
                                }
                            });
                        }

                        @Override
                        public void onFailure(Throwable var1) {
                            loadStatus();
                            power.setProgress(-1);
                        }
                    });

                    break;
                case R.id.cool:
                    cool.setProgress(0);
                    cool.setProgress(50);
                    device.setCool(!device.Cool(), new OperateCallback<Void>() {
                        @Override
                        public void onSuccess(Void var1) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    loadStatus();
                                    cool.setProgress(100);
                                }
                            });
                        }

                        @Override
                        public void onFailure(Throwable var1) {
                            loadStatus();
                            cool.setProgress(-1);
                        }
                    });
                    break;

                case R.id.hot:
                    hot.setProgress(0);
                    hot.setProgress(50);

                    device.setHot(!device.Hot(), new OperateCallback<Void>() {
                        @Override
                        public void onSuccess(Void var1) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    loadStatus();
                                    hot.setProgress(100);
                                }
                            });
                        }

                        @Override
                        public void onFailure(Throwable var1) {
                            loadStatus();
                            hot.setProgress(-1);
                        }
                    });

                    break;

                case R.id.delete:
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setMessage("是否解除设备配对?")
                            .setCancelable(false)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    OznerDeviceManager.Instance().remove(device);
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                    break;
            }

        }
    }


    @Override
    public void loadDevice(OznerDevice device) throws ClassCastException {
        if (!(device instanceof WaterPurifier)) {
            throw new ClassCastException();
        }
        this.device = (WaterPurifier) device;
        loadStatus();
    }
}
