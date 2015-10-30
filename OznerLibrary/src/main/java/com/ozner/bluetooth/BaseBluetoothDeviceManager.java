package com.ozner.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.ozner.device.BaseDeviceManager;
import com.ozner.util.dbg;

import java.util.HashSet;

/**
 * Created by zhiyongxu on 15/10/30.
 */
public abstract class BaseBluetoothDeviceManager extends BaseDeviceManager {
    final Monitor monitor = new Monitor();
    /**
     * 配对状态
     */
    public static final String ACTION_OZNER_BLUETOOTH_BIND_MODE = "com.ozner.bluetooth.bind";
    final HashSet<String> bindDevices = new HashSet<>();

    public BaseBluetoothDeviceManager(Context context) {
        super(context);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothScan.ACTION_SCANNER_FOUND);
        context.registerReceiver(monitor, filter);
    }

    protected abstract boolean chekcBindMode(String Model, int CustomType, byte[] CustomData);

    class Monitor extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothScan.ACTION_SCANNER_FOUND)) {
                if (chekcBindMode(intent.getStringExtra(BluetoothScan.Extra_Model),
                        intent.getIntExtra(BluetoothScan.Extra_CustomType, 0),
                        intent.getByteArrayExtra(BluetoothScan.Extra_CustomData))) {
                    String address = intent.getStringExtra(BluetoothScan.Extra_Address);
                    synchronized (bindDevices) {
                        if (!bindDevices.contains(address)) {
                            bindDevices.add(address);
                            Intent data = new Intent(ACTION_OZNER_BLUETOOTH_BIND_MODE);
                            dbg.d("设备进入配对模式:%s", address);
                            data.putExtra("Address", address);
                            data.putExtra("bind", true);
                            context.sendBroadcast(intent);
                        }
                    }

                } else {
                    String address = intent.getStringExtra(BluetoothScan.Extra_Address);
                    synchronized (bindDevices) {
                        if (bindDevices.contains(address)) {
                            Intent data = new Intent(ACTION_OZNER_BLUETOOTH_BIND_MODE);
                            data.putExtra("Address", address);
                            data.putExtra("bind", false);
                            context.sendBroadcast(intent);
                            bindDevices.remove(address);
                        }
                    }

                }
            }

        }
    }
}
