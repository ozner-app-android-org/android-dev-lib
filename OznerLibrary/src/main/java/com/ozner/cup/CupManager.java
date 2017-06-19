package com.ozner.cup;

import android.annotation.SuppressLint;
import android.content.Context;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.bluetooth.BluetoothIOMgr;
import com.ozner.bluetooth.BluetoothScanResponse;
import com.ozner.bluetooth.IBluetoothScanResponseParser;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.BaseDeviceManager;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

import java.util.Arrays;

@SuppressLint("NewApi")
/**
 * 智能杯管理对象
 * @category 智能杯
 * @author zhiyongxu
 *
 */
public class CupManager extends BaseDeviceManager {
    final static byte AD_CustomType_Gravity = 0x1;

    IBluetoothScanResponseParser bluetoothScanResponseParser = new IBluetoothScanResponseParser() {
        @Override
        public BluetoothScanResponse parseScanResponse(String name, byte[] Manufacturer_Specific, byte[] Service_Data) {
            if (name.equals("Ozner Cup")) {
                BluetoothScanResponse rep = new BluetoothScanResponse();
                if (Manufacturer_Specific != null) {
                    rep.Model = "CP001";
                    rep.Platform = "C01";
                    rep.ScanResponseData = Manufacturer_Specific;
                    //rep.Available = true;
                    rep.ScanResponseType = AD_CustomType_Gravity;
                } else {
                    rep.FromBytes(Service_Data);
                }
                return rep;
            }else
               return null;
        }
    };

    public CupManager(Context context) {
        super(context);
        //注册广播数据解析器，兼容老水杯数据
        OznerDeviceManager.Instance().ioManagerList().bluetoothIOMgr().registerScanResponseParser(bluetoothScanResponseParser);
    }

    @Override
    public boolean isMyDevice(String type) {
        return IsCup(type);
    }

    @Override
    protected OznerDevice createDevice(String address, String type, String settings) {
        if (isMyDevice(type)) {
            return new Cup(context(), address, type, settings);
        } else
            return null;
    }

    public static boolean IsCup(String Type) {
        if (Type == null) return false;
        return Type.trim().equals("CP001");
    }

    @Override
    public boolean checkIsBindMode(BaseDeviceIO io) {
        if (isMyDevice(io.getType())) {
            return Cup.isBindMode((BluetoothIO) io);
        } else
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
