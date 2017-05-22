package xzy.device.discovery.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import xzy.device.DeviceObject;
import xzy.device.discovery.discovery;

/**
 * Created by zhiyongxu on 2017/3/14.
 */

public class bluetoothDiscovery extends discovery {
    public bluetoothDiscovery(Context context) {
        super(context);
    }

    private final short Service_UUID = (short) 0xFFF0;
    private final static byte GAP_ADTYPE_MANUFACTURER_SPECIFIC = (byte) 0xff;
    private final static byte GAP_ADTYPE_SERVICE_DATA = 0x16;
    private final static int FrontPeriod = 500;
    private final static int BackgroundPeriod = 5000;
    private Context mContext;
    private BluetoothMonitor mMonitor;
    private Thread scanThread;
    private boolean isScanning = false;
    private int scanPeriod = FrontPeriod;
    private int stopPeriod = 3000;
    ScanRunnableIMP scanRunnableIMP =new ScanRunnableIMP();
    /**
     * 用来接收系统蓝牙开关信息,打开开启自动扫描,关闭就关掉
     */
    class BluetoothMonitor extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED
                    .equals(intent.getAction())) {
                BluetoothManager bluetoothManager = (BluetoothManager) mContext
                        .getSystemService(Context.BLUETOOTH_SERVICE);
                BluetoothAdapter adapter = bluetoothManager.getAdapter();
                if (adapter.getState() == BluetoothAdapter.STATE_OFF) {
                    stop();
                } else if (adapter.getState() == BluetoothAdapter.STATE_ON) {
                    start();
                }
            }
        }
    }

    private class ScanRunnableIMP implements Runnable, BluetoothAdapter.LeScanCallback {
        @Override
        public void run() {
            BluetoothManager bluetoothManager = (BluetoothManager) mContext
                    .getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter adapter = bluetoothManager.getAdapter();
            if (adapter == null) return;
            try {
                while (isScanning) {
                    if (adapter.isEnabled()) {
                        synchronized (BluetoothSynchronizedObject.getLockObject()) {
                            if (adapter.getState() == BluetoothAdapter.STATE_OFF) {
                                Thread.sleep(1000);
                                continue;
                            }
                            if (adapter.startDiscovery())
                            {
                                adapter.cancelDiscovery();
                            }

                           /* adapter.startLeScan(this);
                            Thread.sleep(scanPeriod);
                            adapter.stopLeScan(this);*/
                            //dbg.i("扫描结束");
                        }
                    }
                    Thread.sleep(stopPeriod);
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            } finally {
                adapter.cancelDiscovery();
            }
        }

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            DeviceObject deviceObject=callCallback(scanRecord);
            if (deviceObject!=null)
                deviceObject.rssi=rssi;
        }
    }

    public boolean isRunning() {
        return scanThread != null && scanThread.isAlive();
    }

    @Override
    public void start() {
        if (isRunning())
            return;

        if (scanThread != null) {
            scanThread.interrupt();
        }
        scanThread = new Thread(scanRunnableIMP);
        scanThread.setName(this.getClass().getName());
        isScanning = true;
        scanThread.start();
    }

    @Override
    public void stop() {
        isScanning = false;
        if (scanThread != null) {
            scanThread.interrupt();
            scanThread = null;
        }
    }
}
