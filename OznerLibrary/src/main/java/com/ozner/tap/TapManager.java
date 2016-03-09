package com.ozner.tap;

import android.annotation.SuppressLint;
import android.content.Context;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.BaseDeviceManager;
import com.ozner.device.OznerDevice;

@SuppressLint("NewApi")
/**
 * 水探头管理器
 * @category 水探头
 * @author zhiyongxu
 *
 */
public class TapManager extends BaseDeviceManager {

    //final static int AD_CustomType_BindStatus = 0x10;

    public TapManager(Context context) {
        super(context);
    }

    public static boolean IsTap(String Type) {
        return Type.equals("SC001") || Type.equals("SCP001");
    }

    @Override
    public boolean isMyDevice(String type) {
        return IsTap(type);
    }

    @Override
    protected OznerDevice createDevice(String address, String type, String settings) {
        if (isMyDevice(type))
        {
            return new Tap(context(), address, type, settings);
        }else
            return null;
    }
    @Override
    public boolean checkIsBindMode(BaseDeviceIO io)
    {
        if (isMyDevice(io.getType()))
        {
            return Tap.isBindMode((BluetoothIO)io);
        }else
            return false;
    }
    //    @Override
//    public OznerDevice loadDevice(String address, String Type, String Settings)
//    {
//        if (IsTap(Type))
//        {
//            OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
//            if (device==null) {
//                device = new Tap(context(), address, Type, Settings);
//            }
//            return device;
//        }else
//            return null;
//    }

//    @Override
//    protected OznerDevice getDevice(BaseDeviceIO io) throws DeviceNotReadyException {
//        if (io instanceof BluetoothIO) {
//            String address = io.getAddress();
//            OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
//            if (device != null) {
//                return device;
//            } else {
//                if (IsTap(io.getType())) {
//                    Tap c = new Tap(context(), address, io.getType(), "");
//                    c.Bind(io);
//                    return c;
//                }
//            }
//        }
//        return null;
//    }
//
//    @Override
//    protected OznerDevice loadDevice(String address, String Type,
//                                     String Setting) {
//        if (IsTap(Type)) {
//            return new Tap(context(), address, Type, Setting);
//        } else
//            return null;
//    }

//    @Override
//    protected boolean checkBindMode(String Model, int CustomType, byte[] CustomData) {
//        if (IsTap(Model)) {
//            if ((CustomType == AD_CustomType_BindStatus) && (CustomData != null) && (CustomData.length > 0)) {
//                return CustomData[0] == 1;
//            }
//        }
//        return false;
//    }
//
//    @Override
//    public boolean isMyDevice(BaseDeviceIO io) {
//        if (io == null) return false;
//        return IsTap(io.getType());
//    }
}
