package com.ozner.tap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import com.ozner.bluetooth.BaseBluetoothDeviceManager;
import com.ozner.bluetooth.BluetoothIO;
import com.ozner.cup.Cup;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.DeviceNotReadyException;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

@SuppressLint("NewApi")
/**
 * 水探头管理器
 * @category 水探头
 * @author zhiyongxu
 *
 */
public class TapManager extends BaseBluetoothDeviceManager {

    final static int AD_CustomType_BindStatus = 0x10;

    public TapManager(Context context) {
        super(context);
    }

    public static boolean IsTap(String model) {
        return model.equals("SC001");
    }


    @Override
    protected OznerDevice getDevice(BaseDeviceIO io) throws DeviceNotReadyException {
        if (io instanceof BluetoothIO) {
            String address = io.getAddress();
            OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
            if (device != null) {
                return device;
            } else {
                if (IsTap(io.getType())) {
                    Tap c = new Tap(context(), address, io.getType(), "");
                    c.Bind(io);
                    return c;
                }
            }
        }
        return null;
    }

    @Override
    protected OznerDevice loadDevice(String address, String Type,
                                     String Setting) {
        if (IsTap(Type)) {
            return new Tap(context(), address, Type, Setting);
        } else
            return null;
    }

    @Override
    protected boolean chekcBindMode(String Model, int CustomType, byte[] CustomData) {
        if (IsTap(Model)) {
            if ((CustomType == AD_CustomType_BindStatus) && (CustomData != null) && (CustomData.length > 0)) {
                return CustomData[0] == 1;
            }
        }
        return false;
    }

    @Override
    public boolean isMyDevice(BaseDeviceIO io) {
        if (io == null) return false;
        return IsTap(io.getType());
    }
}
