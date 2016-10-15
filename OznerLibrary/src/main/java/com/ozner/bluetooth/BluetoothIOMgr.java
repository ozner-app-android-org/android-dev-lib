package com.ozner.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.ozner.device.IOManager;

import java.util.ArrayList;

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
    BluetoothAdapter adapter;
    final ArrayList<IBluetoothScanResponseParser> parsers=new ArrayList<>();
    BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if (action.equals(BluetoothScan.ACTION_SCANNER_FOUND))
            {
                String address=intent.getStringExtra(BluetoothScan.Extra_Address);
                try {
                    BluetoothDevice device= adapter.getRemoteDevice(address);
                    BluetoothIO bluetoothIO = (BluetoothIO) getAvailableDevice(device.getAddress());
                    BluetoothScanResponse rs=null;
                    byte[] service_data=intent.getByteArrayExtra(BluetoothScan.Extra_Service_Data);
                    synchronized (parsers) {
                        for (IBluetoothScanResponseParser parse : parsers) {
                            rs=parse.parseScanResponse(
                                    device.getName(),
                                    intent.getByteArrayExtra(BluetoothScan.Extra_Manufacturer_Specific),
                                    service_data);
                            if (rs!=null) break;
                        }
                    }
                    //如果没找到解析器来解析扫描结果，使用默认的方法加载数据
                    if ((rs==null) && (service_data!=null)) {
                        rs=new BluetoothScanResponse();
                        rs.FromBytes(service_data);
                    }
                    if (rs!=null)
                    {
                        if (bluetoothIO == null) {
                            bluetoothIO = new BluetoothIO(context(), device, rs.Model, rs.Platform, rs.Firmware == null ? 0 : rs.Firmware.getTime());
                            bluetoothIO.name = device.getName();
                        }
                        bluetoothIO.updateScanResponse(rs.ScanResponseType, rs.ScanResponseData);
                        doAvailable(bluetoothIO);
                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    };

    /**
     * 注册一个解析器来处理蓝牙扫描到的广播数据
     * @param parser
     */
    public void registerScanResponseParser(IBluetoothScanResponseParser parser)
    {
        synchronized (parser)
        {
            if (parsers.indexOf(parser)<0) {
                parsers.add(parser);
            }
        }
    }

    public BluetoothIOMgr(Context context) {
        super(context);
        BluetoothManager bluetoothManager = (BluetoothManager) context
                .getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = bluetoothManager.getAdapter();
    }

    /**
     * 启动蓝牙io接口，并启动扫描器来扫描蓝牙设备
     * @param user
     * @param token
     */
    @Override
    public void Start(String user,String token) {
        bluetoothScan = new BluetoothScan(context());
        IntentFilter intentFilter=new IntentFilter(BluetoothScan.ACTION_SCANNER_FOUND);

        context().registerReceiver(broadcastReceiver,intentFilter);
        bluetoothScan.StartScan();
    }

    @Override
    public void Stop() {
        context().unregisterReceiver(broadcastReceiver);
        bluetoothScan.StopScan();
    }




}
