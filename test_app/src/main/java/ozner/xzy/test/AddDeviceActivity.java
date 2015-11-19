package ozner.xzy.test;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ozner.bluetooth.BaseBluetoothDeviceManager;
import com.ozner.bluetooth.BluetoothIO;
import com.ozner.bluetooth.BluetoothScan;
import com.ozner.cup.Cup;
import com.ozner.cup.CupManager;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.NotSupportDeviceException;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;
import com.ozner.tap.Tap;
import com.ozner.tap.TapManager;
import com.ozner.wifi.mxchip.Pair.Helper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class AddDeviceActivity extends Activity {
    ListView list;
    ListAdapter adapter;
    Monitor mMonitor = new Monitor();


    @Override
    protected void onDestroy() {
        this.unregisterReceiver(mMonitor);
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothScan.ACTION_SCANNER_FOUND);
        filter.addAction(BaseBluetoothDeviceManager.ACTION_OZNER_BLUETOOTH_BIND_MODE);

        this.registerReceiver(mMonitor, filter);
        setTitle("设备配对");
        setContentView(R.layout.activity_add);
        adapter = new ListAdapter(this);
        list = (ListView) findViewById(R.id.deviceList);
        list.setAdapter(adapter);
    }

    class Monitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
                adapter.Reload();
        }
    }

    class ListAdapter extends BaseAdapter implements View.OnClickListener {
        ArrayList<BaseDeviceIO> list = new ArrayList<>();
        Context mContext;
        LayoutInflater mInflater;

        public ListAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
            this.Reload();
        }

        private void Reload() {
            list.clear();
            for (BaseDeviceIO device : OznerDeviceManager.Instance().getNotBindDevices()) {
                list.add(device);
            }
            this.notifyDataSetInvalidated();
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.add_device_item, null);
            }
            BaseDeviceIO device = (BaseDeviceIO) getItem(position);
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            if (device != null) {
                ((TextView) convertView.findViewById(R.id.Device_Name)).setText(
                        device.getName() + "(" + device.getAddress() + ")");
                ((TextView) convertView.findViewById(R.id.Device_Model)).setText(
                        device.getType());
                if (device instanceof BluetoothIO) {
                    BluetoothIO bluetoothIO = (BluetoothIO) device;
                    ((TextView) convertView.findViewById(R.id.Device_Platfrom)).setText(
                            bluetoothIO.getPlatform());
                    ((TextView) convertView.findViewById(R.id.Device_Firmware)).setText(
                            fmt.format(new Date(bluetoothIO.getFirmware())));
                    ((TextView) convertView.findViewById(R.id.Device_Custom)).setText(
                            ((BluetoothIO) device).getCustomData() != null ?
                                    Helper.ConvertHexByteArrayToString(((BluetoothIO) device).getCustomData())
                                    : "");
                    if (CupManager.IsCup(device.getType())) {
                        if (Cup.isBindMode(bluetoothIO))
                            convertView.findViewById(R.id.addDeviceButton).setEnabled(true);
                        else
                            convertView.findViewById(R.id.addDeviceButton).setEnabled(false);
                    }
                    if (TapManager.IsTap(device.getType())) {
                        if (Tap.isBindMode(bluetoothIO))
                            convertView.findViewById(R.id.addDeviceButton).setEnabled(true);
                        else
                            convertView.findViewById(R.id.addDeviceButton).setEnabled(false);
                    }


                }
                convertView.findViewById(R.id.addDeviceButton).setTag(device);
                convertView.findViewById(R.id.addDeviceButton).setOnClickListener(this);

            }

            return convertView;
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {

                case R.id.addDeviceButton: {
                    //获取点击的蓝牙设备
                    BaseDeviceIO bluetooth = (BaseDeviceIO) v.getTag();

                    OznerDevice device;
                    try {
                        //通过找到的蓝牙对象控制对象获取设备对象
                        device = OznerDeviceManager.Instance().getDevice(bluetooth);
                        if (device != null) {
                            //保存设备
                            OznerDeviceManager.Instance().save(device);
                            //配对完成,重新加载配对设备列表
                            this.Reload();
                        }
                    } catch (NotSupportDeviceException e) {
                        e.printStackTrace();
                    }

                }
                break;

            }

        }
    }
}
