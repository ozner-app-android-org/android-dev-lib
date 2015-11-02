package com.ozner.device;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by zhiyongxu on 15/11/2.
 */
public class GenIOManager extends IOManager {
    final ArrayList<IOManager> managers = new ArrayList<>();
    IOManagerCallback ioManagerCallback = null;

    public GenIOManager(Context context) {
        super(context);
    }

    @Override
    public void Start() {
        for (IOManager mgr : managers) {
            mgr.Start();
        }
    }

    @Override
    public void Stop() {
        for (IOManager mgr : managers) {
            mgr.Stop();
        }
    }

    @Override
    public void closeAll() {
        for (IOManager mgr : managers) {
            mgr.closeAll();
        }
    }

    @Override
    public void setIoManagerCallback(IOManagerCallback ioManagerCallback) {
        this.ioManagerCallback = ioManagerCallback;
        for (IOManager mgr : managers) {
            mgr.setIoManagerCallback(ioManagerCallback);
        }

    }

    @Override
    public BaseDeviceIO getAvailableDevice(String address) {
        for (IOManager mgr : managers) {
            BaseDeviceIO io = mgr.getAvailableDevice(address);
            if (io != null) {
                return io;
            }
        }
        return null;
    }

    @Override
    public BaseDeviceIO[] getAvailableDevices() {
        ArrayList<BaseDeviceIO> list = new ArrayList<>();
        for (IOManager mgr : managers) {
            Collections.addAll(list, mgr.getAvailableDevices());
        }
        return list.toArray(new BaseDeviceIO[list.size()]);
    }

    public void register(IOManager ioManager) {
        if (!managers.contains(ioManager)) {
            managers.add(ioManager);
            ioManager.setIoManagerCallback(ioManagerCallback);
        }

    }


}
