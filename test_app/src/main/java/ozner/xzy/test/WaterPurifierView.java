package ozner.xzy.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ozner.WaterPurifier.WaterPurifier;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.OperateCallback;
import com.ozner.device.OznerDevice;

/**
 * Created by zhiyongxu on 15/11/4.
 *
 */
public class WaterPurifierView extends BaseItemView {
    TextView status;
    TextView tds1;
    TextView tds2;
    TextView powerStatus;
    TextView hotStatus;
    TextView coolStatus;
    TextView sendStatus;
    Button power;
    Button cool;
    Button hot;
    ControlImp controlImp = new ControlImp();
    Monitor monitor = new Monitor();
    Handler handler = new Handler();

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
        status = (TextView) findViewById(R.id.status);
        tds1 = (TextView) findViewById(R.id.tds1);
        tds2 = (TextView) findViewById(R.id.tds2);

        powerStatus = (TextView) findViewById(R.id.powerStatus);
        hotStatus = (TextView) findViewById(R.id.hotStatus);
        coolStatus = (TextView) findViewById(R.id.coolStatus);
        sendStatus = (TextView) findViewById(R.id.sendStatus);

        power = (Button) findViewById(R.id.power);
        power.setOnClickListener(controlImp);

        cool = (Button) findViewById(R.id.cool);
        cool.setOnClickListener(controlImp);

        hot = (Button) findViewById(R.id.hot);
        hot.setOnClickListener(controlImp);
        loadStatus();

        findViewById(R.id.delete).setOnClickListener(controlImp);

    }

    private void loadStatus() {
        if (device() == null) {
            return;
        }
        WaterPurifier waterPurifier = (WaterPurifier) device();
        name.setText(device.getName());
        mac.setText(device.Address());

        status.setText("连接状态:" + (waterPurifier.isOffline() ? "离线" : "在线"));
        tds1.setText("TDS1:" + String.valueOf(waterPurifier.TDS1()));
        tds2.setText("TDS2:" + String.valueOf(waterPurifier.TDS2()));
        powerStatus.setText("电源:" + (waterPurifier.Power() ? "开" : "关"));
        coolStatus.setText("制冷:" + (waterPurifier.Cool() ? "开" : "关"));
        hotStatus.setText("加热:" + (waterPurifier.Hot() ? "开" : "关"));
    }

    @Override
    public void loadDevice(OznerDevice device) throws ClassCastException {
        super.loadDevice(device);
        loadStatus();
    }


    class Monitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra(OznerDevice.Extra_Address).equals(device.Address())) {
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
            WaterPurifier waterPurifier = (WaterPurifier) device();
            switch (view.getId()) {
                case R.id.power:
                    sendStatus.setText("发送状态:正在发送中");
                    waterPurifier.setPower(!waterPurifier.Power(), this);
                    break;
                case R.id.cool:
                    sendStatus.setText("发送状态:正在发送中");
                    waterPurifier.setCool(!waterPurifier.Cool(), this);
                    break;
                case R.id.hot:
                    sendStatus.setText("发送状态:正在发送中");
                    waterPurifier.setHot(!waterPurifier.Hot(), this);
                    break;

            }

        }
    }
}
