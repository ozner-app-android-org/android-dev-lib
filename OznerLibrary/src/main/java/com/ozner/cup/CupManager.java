package com.ozner.cup;

import android.annotation.SuppressLint;
import android.content.Context;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.BaseDeviceManager;
import com.ozner.device.OznerDevice;

@SuppressLint("NewApi")
/**
 * 智能杯管理对象
 * @category 智能杯
 * @author zhiyongxu
 *
 */
public class CupManager extends BaseDeviceManager {

    public CupManager(Context context) {
        super(context);
    }

    @Override
    public boolean isMyDevice(String type) {
        return IsCup(type);
    }

    @Override
    protected OznerDevice createDevice(String address, String type, String settings) {
        if (isMyDevice(type)) {
            return new Cup(context(), address, type, settings);
        }else
            return null;
    }
    public static boolean IsCup(String Type) {
        if (Type == null) return false;
        return Type.trim().equals("CP001");
    }
    @Override
    public boolean checkIsBindMode(BaseDeviceIO io)
    {
        if (isMyDevice(io.getType()))
        {
            return Cup.isBindMode((BluetoothIO)io);
        }else
            return false;
    }
//    @Override
//    protected OznerDevice loadDevice(String address,
//                                     String Type, String Setting) {
//        if (IsCup(Type)) {
//            return new Cup(context(), address, Type, Setting);
//        } else
//            return null;
//    }
//
//    @Override
//    protected OznerDevice getDevice(BaseDeviceIO io) throws DeviceNotReadyException {
//        if (io instanceof BluetoothIO) {
//            String address = io.getAddress();
//            OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
//            if (device != null) {
//                return device;
//            } else {
//                if (IsCup(io.getType())) {
//                    Cup c = new Cup(context(), address, io.getType(), "");
//                    c.Bind(io);
//                    return c;
//                }
//            }
//        }
//        return null;
//    }

//    @Override
//    protected boolean checkBindMode(String Model, int CustomType, byte[] CustomData) {
//        if (IsCup(Model)) {
//            if ((CustomType == Cup.AD_CustomType_Gravity) && (CustomData != null)) {
//                CupGravity gravity = new CupGravity();
//                gravity.FromBytes(CustomData, 0);
//                return gravity.IsHandstand();
//            } else
//                return false;
//        } else
//            return false;
//    }

}
