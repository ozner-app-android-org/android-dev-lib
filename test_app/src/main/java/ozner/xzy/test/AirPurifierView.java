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
import com.ozner.AirPurifier.AirPurifier_MXChip;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

/**
 * Created by zhiyongxu on 15/11/4.
 */
public class AirPurifierView extends BaseItemView {
    AirPurifier_MXChip device;
    TextView name;
    TextView mac;
    TextView status;

    CircularProgressButton power;

    ControlImp controlImp = new ControlImp();
    Monitor monitor = new Monitor();
    Handler handler = new Handler();

    public AirPurifierView(Context context, AttributeSet attrs) {
        super(context, attrs);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_CONNECTED);
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_CONNECTING);
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_DISCONNECTED);
        context.registerReceiver(monitor, intentFilter);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        name = (TextView) findViewById(R.id.name);
        mac = (TextView) findViewById(R.id.mac);
        status = (TextView) findViewById(R.id.status);

        loadStatus();

        findViewById(R.id.delete).setOnClickListener(controlImp);
        findViewById(R.id.power).setOnClickListener(controlImp);


    }

    private void loadStatus() {
        if (device == null) {
            return;
        }

        name.setText(device.getName());
        mac.setText(device.Address());
        status.setText("连接状态:" + (device.isOffline() ? "离线" : "在线"));

    }

    @Override
    public void loadDevice(OznerDevice device) throws ClassCastException {
        if (!(device instanceof AirPurifier_MXChip)) {
            throw new ClassCastException();
        }
        this.device = (AirPurifier_MXChip) device;
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

    class ControlImp implements OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.power:
                    device.airStatus().setPower(true, null);

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
}
