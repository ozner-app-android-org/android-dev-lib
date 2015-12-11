package com.ozner.WaterPurifier;

import android.content.Context;

import com.ozner.device.BaseDeviceManager;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

/**
 * Created by xzyxd on 2015/11/2.
 */
public class WaterPurifierManager extends BaseDeviceManager {


    public WaterPurifierManager(Context context) {
        super(context);
    }

    public static boolean IsWaterPurifier(String Model) {
        if (Model == null) return false;
        return Model.trim().equals("MXCHIP_HAOZE_Water");
    }

    @Override
    public boolean isMyDevice(String type) {
        return IsWaterPurifier(type);
    }

    @Override
    protected OznerDevice createDevice(String address, String type, String settings) {
        if (isMyDevice(type))
        {
            WaterPurifier waterPurifier = new WaterPurifier(context(), address, type, settings);
            OznerDeviceManager.Instance().ioManagerList().mxChipIOManager()
                    .addListenerAddress(waterPurifier.Address(), waterPurifier.Type());
            return waterPurifier;
        }else
            return null;
    }

//    @Override
//    public OznerDevice loadDevice(String address, String Type, String Settings) {
//        if (IsWaterPurifier(Type)) {
//            OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
//            if (device == null) {
//                device = new WaterPurifier(context(), address, Type, Settings);
//            }
//            return device;
//        }
//        else
//            return null;
//    }
//    @Override
//    protected OznerDevice getDevice(BaseDeviceIO io) throws DeviceNotReadyException {
//        if (io instanceof MXChipIO) {
//            String address = io.getAddress();
//            OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
//            if (device != null) {
//                return device;
//            } else {
//                if (IsWaterPurifier(io.getType())) {
//                    WaterPurifier c = new WaterPurifier(context(), address, io.getType(), "");
//                    c.Bind(io);
//                    return c;
//                }
//            }
//        }
//        return null;
//    }

//    public OznerDevice newWaterPurifier(Context context, String address) {
//        OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
//        if (device != null) {
//            return device;
//        } else {
//            WaterPurifier waterPurifier = new WaterPurifier(context(), address, "MXCHIP_HAOZE_Water", "");
//            MXChipIO io = OznerDeviceManager.Instance().ioManagerList().mxChipIOManager()
//                    .createNewIO(waterPurifier.Setting().name(), waterPurifier.Address(), waterPurifier.Type());
//            try {
//                waterPurifier.Bind(io);
//            } catch (DeviceNotReadyException e) {
//                e.printStackTrace();
//            }
//
//            return waterPurifier;
//        }
//    }
//
//
//    @Override
//    protected OznerDevice loadDevice(String address, String Type, String Setting) {
//        if (IsWaterPurifier(Type)) {
//            WaterPurifier waterPurifier = new WaterPurifier(context(), address, Type, Setting);
//            OznerDeviceManager.Instance().ioManagerList().mxChipIOManager()
//                    .createNewIO(waterPurifier.Setting().name(), waterPurifier.Address(), waterPurifier.Type());
//            return waterPurifier;
//        } else
//            return null;
//    }

//    @Override
//    public boolean isMyDevice(BaseDeviceIO io) {
//        if (io instanceof MXChipIO) {
//            return IsWaterPurifier(io.getType());
//        } else return false;
//    }
}
