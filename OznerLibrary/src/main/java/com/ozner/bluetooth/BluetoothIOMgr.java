package com.ozner.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.ozner.device.IOManager;

/**
 * 蓝牙接口管理类
 * <p/>
 * 统一处理蓝牙设备IO的管理
 * 在找到蓝牙设备以后激活 onDeviceAvailable 事件来通知外部管理器,设备在处于范围内
 * 连接中断以后激活 doUnavailable 来通知设备连接中断或者超出范围
 * Created by xzyxd on 2015/10/29.
 */
public class BluetoothIOMgr extends IOManager {
    BluetoothScan bluetoothScan;
    ScanCallbackImp scanCallback = new ScanCallbackImp();

    public BluetoothIOMgr(Context context) {
        super(context);
        bluetoothScan = new BluetoothScan(context);
        bluetoothScan.setScanCallback(scanCallback);
    }


    @Override
    public void Start() {
        bluetoothScan.StartScan();
    }

    @Override
    public void Stop() {
        bluetoothScan.StopScan();
    }

    class ScanCallbackImp implements BluetoothScan.BluetoothScanCallback {
        @Override
        public void onFoundDevice(BluetoothDevice device, BluetoothScanRep scanRep) {
            try {
                BluetoothIO bluetoothIO = (BluetoothIO) getAvailableDevice(device.getAddress());
                if (bluetoothIO == null) {
                    bluetoothIO = new BluetoothIO(context(), device, scanRep.Model, scanRep.Platform, scanRep.Firmware == null ? 0 : scanRep.Firmware.getTime());
                }
                bluetoothIO.updateScanResponse(scanRep.ScanResponseType, scanRep.ScanResponseData);
                doAvailable(bluetoothIO);
            }catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }


}
