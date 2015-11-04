package com.ozner.device;

import android.content.Context;

import com.ozner.bluetooth.BluetoothIOMgr;
import com.ozner.wifi.mxchip.MXChipIOManager;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by xzyxd on 2015/11/2.
 */
public class IOManagerList extends IOManager {

    BluetoothIOMgr bluetoothIOMgr;
    MXChipIOManager mxChipIOManager;

    public BluetoothIOMgr bluetoothIOMgr()
    {
        return bluetoothIOMgr;
    }
    public MXChipIOManager mxChipIOManager()
    {
        return mxChipIOManager;
    }


    public IOManagerList(Context context) {
        super(context);
        bluetoothIOMgr=new BluetoothIOMgr(context);
        mxChipIOManager=new MXChipIOManager(context);
    }

    @Override
    public void Start() {
        bluetoothIOMgr.Start();
        mxChipIOManager.Start();
    }

    @Override
    public void Stop() {
        bluetoothIOMgr.Stop();
        mxChipIOManager.Stop();
    }

    @Override
    public void closeAll() {
        bluetoothIOMgr.closeAll();
        mxChipIOManager.closeAll();
    }

    @Override
    public void setIoManagerCallback(IOManagerCallback ioManagerCallback) {
        bluetoothIOMgr.setIoManagerCallback(ioManagerCallback);
        mxChipIOManager.setIoManagerCallback(ioManagerCallback);

    }

    @Override
    public BaseDeviceIO getAvailableDevice(String address) {
        BaseDeviceIO io=null;
        if ((io=bluetoothIOMgr.getAvailableDevice(address))!=null)
        {
            return io;
        }
        if ((io=mxChipIOManager.getAvailableDevice(address))!=null)
        {
            return io;
        }
        return io;
    }

    @Override
    public BaseDeviceIO[] getAvailableDevices() {
        ArrayList<BaseDeviceIO> list = new ArrayList<>();

        Collections.addAll(list,  bluetoothIOMgr.getAvailableDevices());
        Collections.addAll(list,  mxChipIOManager.getAvailableDevices());

        return list.toArray(new BaseDeviceIO[list.size()]);
    }

}
