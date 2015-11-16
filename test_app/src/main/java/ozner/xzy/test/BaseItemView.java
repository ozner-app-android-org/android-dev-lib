package ozner.xzy.test;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

/**
 * Created by zhiyongxu on 15/11/4.
 */
public class BaseItemView extends LinearLayout {
    TextView name;
    TextView mac;
    TextView type;
    OznerDevice device = null;
    public BaseItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.base_item_view, this);
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
        type = (TextView) findViewById(R.id.type);
        findViewById(R.id.delete).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        });
    }

    public void loadDevice(OznerDevice device) throws ClassCastException {
        if (this.device == device) return;
        if (device != null) {
            if (this.device != null) {
                if (!this.device.getClass().equals(device.getClass())) {
                    throw new ClassCastException();
                }
            }
        }
        this.device = device;
        name.setText(device.getName());
        mac.setText(device.Address());
        type.setText(device.Type());
    }

}
