package com.ozner.device;

import android.content.Context;

/**
 * 设备管理基类
 *
 * @author zhiyongxu
 * @category Device
 */
public abstract class BaseDeviceManager {
    Context context;

    public BaseDeviceManager(Context context) {
        this.context = context;
        OznerDeviceManager.Instance().registerManager(this);
    }


    protected Context context() {
        return context;
    }


    protected abstract OznerDevice getDevice(BaseDeviceIO io) throws DeviceNotReadlyException;

    protected abstract OznerDevice loadDevice(String address, String Model, String Setting);


    protected void remove(OznerDevice device) {

    }

    protected void update(OznerDevice device) {

    }

    protected void add(OznerDevice device) {
    }
}
