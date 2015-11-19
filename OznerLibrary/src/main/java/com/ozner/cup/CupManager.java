package com.ozner.cup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import com.ozner.bluetooth.BaseBluetoothDeviceManager;
import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.DeviceNotReadyException;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

import java.util.ArrayList;

@SuppressLint("NewApi")
/**
 * 智能杯管理对象
 * @category 智能杯
 * @author zhiyongxu
 *
 */
public class CupManager extends BaseBluetoothDeviceManager {

    public CupManager(Context context) {
        super(context);
    }

    public static boolean IsCup(String Model) {
        if (Model == null) return false;
        return Model.trim().equals("CP001");
    }

    @Override
    protected OznerDevice loadDevice(String address,
                                     String Type, String Setting) {
        if (IsCup(Type)) {
            return new Cup(context(), address, Type, Setting);
        } else
            return null;
    }

    @Override
    protected OznerDevice getDevice(BaseDeviceIO io) throws DeviceNotReadyException {
        if (io instanceof BluetoothIO) {
            String address = io.getAddress();
            OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
            if (device != null) {
                return device;
            } else {
                if (IsCup(io.getType())) {
                    Cup c = new Cup(context(), address, io.getType(), "");
                    c.Bind(io);
                    return c;
                }
            }
        }
        return null;
    }

    @Override
    protected boolean chekcBindMode(String Model, int CustomType, byte[] CustomData) {
        if (IsCup(Model)) {
            if ((CustomType == Cup.AD_CustomType_Gravity) && (CustomData != null)) {
                CupGravity gravity = new CupGravity();
                gravity.FromBytes(CustomData, 0);
                return gravity.IsHandstand();
            } else
                return false;
        } else
            return false;
    }

    @Override
    public boolean isMyDevice(BaseDeviceIO io) {
        if (io == null) return false;
        return IsCup(io.getType());
    }
}
