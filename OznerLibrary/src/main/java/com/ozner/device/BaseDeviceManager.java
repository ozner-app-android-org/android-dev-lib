package com.ozner.device;

import android.content.Context;

import com.ozner.XObject;

/**
 * 设备管理基类
 *
 * @author zhiyongxu
 * @category Device
 */
public abstract class BaseDeviceManager extends XObject {

    public BaseDeviceManager(Context context) {
        super(context);
        OznerDeviceManager.Instance().registerManager(this);
    }


    protected abstract OznerDevice getDevice(BaseDeviceIO io) throws DeviceNotReadyException;

    protected abstract OznerDevice loadDevice(String address, String Model, String Setting);


    protected void remove(OznerDevice device) {

    }

    protected void update(OznerDevice device) {

    }

    protected void add(OznerDevice device) {
    }

    public abstract boolean isMyDevice(BaseDeviceIO io);
}
