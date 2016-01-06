package com.ozner.AirPurifier;

import android.content.Context;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.BaseDeviceManager;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

/**
 * Created by xzyxd on 2015/11/7.
 */
public class AirPurifierManager extends BaseDeviceManager {


    public AirPurifierManager(Context context) {
        super(context);
    }

//    @Override
//    protected boolean checkBindMode(String Model, int CustomType, byte[] CustomData) {
//        if (CustomType==0x20)
//        {
//            return CustomData[0]!=0;
//        }
//        return false;
//    }

    @Override
    public boolean checkIsBindMode(BaseDeviceIO io) {
        if (IsBluetoothAirPurifier(io.getType()))
        {
            BluetoothIO bluetoothIO=(BluetoothIO)io;
            if (bluetoothIO.getScanResponseType()==0x20)
            {
                if ((bluetoothIO.getScanResponseData()!=null) && (bluetoothIO.getScanResponseData().length>1))
                {
                    return bluetoothIO.getScanResponseData()[1]!=0;
                }

            }
        }
        return false;
    }

    @Override
    public boolean isMyDevice(String type) {
        return IsBluetoothAirPurifier(type) || IsWifiAirPurifier(type);
    }

    @Override
    protected OznerDevice createDevice(String address, String type, String settings) {
        if (isMyDevice(type)) {
            if (IsBluetoothAirPurifier(type)) {
                return new AirPurifier_Bluetooth(context(), address, type, settings);
            } else if (IsWifiAirPurifier(type)) {
                OznerDevice airPurifier = new AirPurifier_MXChip(context(), address, type, settings);
                OznerDeviceManager.Instance().ioManagerList().mxChipIOManager()
                        .createMXChipDevice(airPurifier.Address(), airPurifier.Type());
                return airPurifier;
            }
            return null;
        }
        else
            return null;
    }


    public static boolean IsWifiAirPurifier(String Type) {
        if (Type == null) return false;
        return Type.trim().equals("FOG_HAOZE_AIR");
    }


    public static boolean IsBluetoothAirPurifier(String Type) {
        if (Type == null) return false;
        return Type.trim().equals("FLT001");
    }

//    @Override
//    public OznerDevice loadDevice(String address, String Type, String Settings) {
//        if (IsWifiAirPurifier(Type)) {
//            OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
//            if (device == null) {
//                device = new AirPurifier_MXChip(context(), address, Type, "");
//            }
//            return device;
//        } else if (IsBluetoothAirPurifier(Type)) {
//            OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
//            if (device == null) {
//                device = new AirPurifier_Bluetooth(context(), address, Type, "");
//            }
//            return device;
//        } else
//            return null;
//    }
    //    public OznerDevice newAirPurifier(Context context, String address) {
//        OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
//        if (device != null) {
//            return device;
//        } else {
//            AirPurifier_MXChip waterPurifier = new AirPurifier_MXChip(context(), address, "MXCHIP_HAOZE_Air", "");
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


//    @Override
//    protected OznerDevice getDevice(BaseDeviceIO io) throws DeviceNotReadyException {
//        if (io instanceof MXChipIO) {
//            String address = io.getAddress();
//            OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
//            if (device != null) {
//                return device;
//            } else {
//                if (IsWifiAirPurifier(io.getType())) {
//                    AirPurifier_MXChip c = new AirPurifier_MXChip(context(), address, io.getType(), "");
//                    c.Bind(io);
//                    return c;
//                }
//            }
//        } else if (io instanceof BluetoothIO) {
//            String address = io.getAddress();
//            OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
//            if (device != null) {
//                return device;
//            } else {
//                if (IsBluetoothAirPurifier(io.getType())) {
//                    AirPurifier_Bluetooth c = new AirPurifier_Bluetooth(context(), address, io.getType(), "");
//                    c.Bind(io);
//                    return c;
//                }
//            }
//        }
//        return null;
//    }
//
//
//
//
//    @Override
//    protected OznerDevice loadDevice(String address, String Type, String Setting) {
//        if (IsWifiAirPurifier(Type)) {
//            AirPurifier_MXChip waterPurifier = new AirPurifier_MXChip(context(), address, Type, Setting);
//            OznerDeviceManager.Instance().ioManagerList().mxChipIOManager()
//                    .createNewIO(waterPurifier.Setting().name(), waterPurifier.Address(), waterPurifier.Type());
//            return waterPurifier;
//        } else if (IsBluetoothAirPurifier(Type)) {
//            return new AirPurifier_Bluetooth(context(), address, Type, Setting);
//        } else
//            return null;
//    }

//    @Override
//    public boolean isMyDevice(BaseDeviceIO io) {
//        if (io instanceof MXChipIO) {
//            return IsWifiAirPurifier(io.getType());
//        } else if (io instanceof BluetoothIO) {
//            return IsBluetoothAirPurifier(io.getType());
//        } else
//            return false;
//    }
}
