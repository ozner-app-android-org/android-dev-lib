package ozner.xzy.test;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ozner.AirPurifier.AirPurifier_MXChip;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

/**
 * Created by zhiyongxu on 15/11/4.
 */
public class DeviceItemView extends RelativeLayout {
    TextView name;
    TextView mac;
    TextView status;
    OznerDevice device = null;
    final Monitor monitor=new Monitor();

    class Monitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra(OznerDevice.Extra_Address).equals(device.Address())) {
                update();
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_CONNECTED);
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_CONNECTING);
        intentFilter.addAction(BaseDeviceIO.ACTION_DEVICE_DISCONNECTED);
        intentFilter.addAction(OznerDevice.ACTION_DEVICE_UPDATE);
        getContext().registerReceiver(monitor, intentFilter);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(monitor);
    }

    public DeviceItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OznerDevice device() {
        return this.device;
    }

    protected TextView setText(int id, String text) {
        TextView textView = (TextView) findViewById(id);
        textView.setText(text);
        return textView;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        name = (TextView) findViewById(R.id.name);
        mac = (TextView) findViewById(R.id.mac);
        status = (TextView) findViewById(R.id.status);
//        findViewById(R.id.delete).setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
//                builder.setMessage("是否解除设备配对?")
//                        .setCancelable(false)
//                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                OznerDeviceManager.Instance().remove(device);
//                            }
//                        })
//                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                dialog.cancel();
//                            }
//                        });
//                AlertDialog alert = builder.create();
//                alert.show();
//            }
//        });
    }
    private void update()
    {
        status.setText(device.toString());
    }



    public void loadDevice(OznerDevice device) throws ClassCastException {
        if (this.device == device) return;
        this.device = device;
        name.setText(device.getName());
        mac.setText(device.Address());
        status.setText(device.toString());
    }

}
