package com.ozner.kettle;

import android.content.Context;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.BaseDeviceManager;
import com.ozner.device.OznerDevice;

/**
 * Created by zhiyongxu on 15/12/21.
 */
public class KettleMgr extends BaseDeviceManager {

    public KettleMgr(Context context) {
        super(context);
    }

    @Override
    public boolean isMyDevice(String type) {
        return isKettle(type);
    }

    @Override
    protected OznerDevice createDevice(String address, String type, String settings) {
        if (isMyDevice(type)) {
            return new Kettle(context(), address, type, settings);
        }else
            return null;
    }
    public static boolean isKettle(String Type) {
        if (Type == null) return false;
        return Type.trim().startsWith("DRH001");
    }

    @Override
    public boolean checkIsBindMode(BaseDeviceIO io)
    {
        if (isMyDevice(io.getType()))
        {
            BluetoothIO bluetoothIO=(BluetoothIO)io;
            if (bluetoothIO.getScanResponseType()==0x20)
            {
                if ((bluetoothIO.getScanResponseData()!=null) && (bluetoothIO.getScanResponseData().length>1))
                {
                    return bluetoothIO.getScanResponseData()[0]!=0;
                }
            }
        }else
            return false;
        return false;
    }
}
