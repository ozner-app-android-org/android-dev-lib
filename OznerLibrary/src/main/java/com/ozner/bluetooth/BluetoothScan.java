package com.ozner.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.ozner.util.dbg;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

@SuppressLint("NewApi")
public class BluetoothScan implements LeScanCallback, Runnable {

    public final static int AD_CustomType_BindStatus = 0x10;
    public static final String Extra_Address = "Address";
    public static final String Extra_Model = "Model";
    public static final String Extra_Firmware = "Firmware";
    public static final String Extra_Platform = "Platform";
    public static final String Extra_ScanData = "scanData";
    public static final String Extra_CustomType = "CustomType";
    public static final String Extra_CustomData = "CustomData";
    public static final String Extra_RSSI = "RSSI";
    public static final String Extra_DataAvailable = "DataAvailable";

    /**
     * 扫描开始广播,无附加数据
     */
    public final static String ACTION_SCANNER_START = "com.ozner.bluetooth.sanner.start";

    /**
     * 扫描停止广播
     */
    public final static String ACTION_SCANNER_STOP = "com.ozner.bluetooth.sanner.stop";

    /**
     * 找到设备广播,附加设备的MAC地址
     */
    public final static String ACTION_SCANNER_FOUND = "com.ozner.bluetooth.sanner.found";


    final static byte GAP_ADTYPE_MANUFACTURER_SPECIFIC = (byte) 0xff;
    final static byte GAP_ADTYPE_SERVICE_DATA = 0x16;
    Context mContext;
    BluetoothMonitor mMonitor;
    public BluetoothScan(Context context) {
        mContext = context;
        mMonitor = new BluetoothMonitor();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mMonitor, filter);
    }

    /**
     * 用来接收系统蓝牙开关信息,打开开启自动扫描,关闭就关掉
     */
    class BluetoothMonitor extends BroadcastReceiver {
        @SuppressWarnings("deprecation")
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED
                    .equals(intent.getAction())) {
                BluetoothManager bluetoothManager = (BluetoothManager) mContext
                        .getSystemService(Context.BLUETOOTH_SERVICE);
                BluetoothAdapter adapter = bluetoothManager.getAdapter();
                if (adapter.getState() == BluetoothAdapter.STATE_OFF) {
                    StopScan();
                } else if (adapter.getState() == BluetoothAdapter.STATE_ON) {
                    StartScan();
                }
            }
        }
    }

    boolean isScanning = false;
    final static int FrontPeriod = 500;
    final static int BackgroundPeriod = 5000;
    int scanPeriod = FrontPeriod;

    private Thread scanThread;
    private boolean isBackground = false;


    public void run() {
        BluetoothManager bluetoothManager = (BluetoothManager) mContext
                .getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        if (adapter == null) return;
        try {
            while (isScanning) {
                synchronized (BluetoothSynchronizedObject.getLockObject()) {
                    synchronized (mFoundDevice) {
                        mFoundDevice.clear();
                    }

                    adapter.startLeScan(this);
                    Thread.sleep(scanPeriod);
                    adapter.stopLeScan(this);
                    //dbg.i("扫描结束");
                }
                Thread.sleep(scanPeriod);
                synchronized (mFoundDevice) {
                    if (mFoundDevice.size() > 0) {
                        for (FoundDevice found : mFoundDevice.values()) {
                            //if (BluetoothSynchronizedObject.hashBluetoothBusy())
                            //    break;
                            onFound(found.device, found.rssi, found.scanRecord);
                        }
                    }
                }
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            adapter.stopLeScan(this);
        }
    }


    public void StartScan() {
        if (isRuning())
            return;

        if (scanThread != null) {
            scanThread.interrupt();
        }
        scanThread = new Thread(this);
        scanThread.setName(this.getClass().getName());
        isScanning = true;
        scanThread.start();
    }

    public void StopScan() {
        isScanning = false;
        if (scanThread != null) {
            scanThread.interrupt();
            scanThread = null;
        }

    }


    public boolean isRuning() {
        return scanThread != null && scanThread.isAlive();
    }

    public void setBackgroundMode(boolean isBackground) {
        this.isBackground = isBackground;
        scanPeriod = isBackground ? BackgroundPeriod : FrontPeriod;
    }
    class FoundDevice {
        public BluetoothDevice device;
        public byte[] scanRecord;
        public int rssi;
    }
    HashMap<String, FoundDevice> mFoundDevice = new HashMap<>();
    private void onFound(BluetoothDevice device, int rssi, byte[] scanRecord) {
        String address = device.getAddress();
        byte[] repData = null;
        String Model = "";
        Date Firmware = null;
        byte[] CustomData = null;
        String Platform = "";
        boolean Available = false;
        int CustomType = 0;
        int pos = 0;
        while (true) {
            try {
                int len = scanRecord[pos];
                pos++;
                if (len > 0) {
                    byte flag = scanRecord[pos];
                    if (len > 1) {
                        if (flag == GAP_ADTYPE_MANUFACTURER_SPECIFIC) {
                            //dbg.d("send GAP_ADTYPE_MANUFACTURER_SPECIFIC:%s",
                            //		device.getAddress());
                            // 老固件水杯兼容
                            byte[] data = null;
                            try {
                                data = Arrays.copyOfRange(scanRecord,
                                        pos + 1, pos + len);
                            } catch (Exception e) {
                                dbg.e(e.toString());
                            }
                            if (device.getName().equals("Ozner Cup")) {
                                CustomType = BluetoothCup.AD_CustomType_Gravity;
                                Model = "CP001";
                                Platform = "C01";
                                CustomData = data;
                                Available = true;
                            }
                        }
                        if (flag == GAP_ADTYPE_SERVICE_DATA) {
                            byte[] data = Arrays.copyOfRange(scanRecord,
                                    pos + 1, pos + len);
                            BluetoothScanRep rep = new BluetoothScanRep();
                            rep.FromBytes(data);
                            repData = data;
                            Model = rep.Model;
                            Platform = rep.Platform;
                            Firmware = rep.Firmware;
                            CustomType = rep.CustomDataType;
                            CustomData = rep.CustomData;
                            Available = rep.Available;
                        }
                    }
                }
                pos += len;
                if (pos >= scanRecord.length)
                    break;
            } catch (Exception e) {
                dbg.e(e.toString());
                return;
            }
        }
        Intent intent = new Intent(ACTION_SCANNER_FOUND);
        intent.putExtra(Extra_Address, address);
        intent.putExtra(Extra_Model, Model);
        intent.putExtra(Extra_Platform, Platform);
        intent.putExtra(Extra_RSSI, rssi);
        if (Firmware != null)
            intent.putExtra(Extra_Firmware, Firmware.getTime());
        intent.putExtra(Extra_CustomType, CustomType);
        intent.putExtra(Extra_CustomData, CustomData);
        intent.putExtra(Extra_DataAvailable, Available);
        intent.putExtra(Extra_ScanData, repData);
        mContext.sendBroadcast(intent);
    }
    public BluetoothDevice getDevice(String address)
    {
        BluetoothManager bluetoothManager = (BluetoothManager) mContext
                .getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        return adapter.getRemoteDevice(address);
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        synchronized (mFoundDevice) {
            if (mFoundDevice.containsKey(device.getAddress())) {
                mFoundDevice.remove(device.getAddress());
            }
            FoundDevice found = new FoundDevice();
            found.device = device;
            found.rssi = rssi;
            found.scanRecord = scanRecord;
            mFoundDevice.put(device.getAddress(), found);
        }


    }

}
