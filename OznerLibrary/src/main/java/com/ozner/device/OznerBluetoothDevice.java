package com.ozner.device;

import android.content.Context;

import com.ozner.bluetooth.BluetoothScanResponse;

/**
 * Created by zhiyongxu on 16/9/14.
 */
public abstract class OznerBluetoothDevice extends OznerDevice {

    public OznerBluetoothDevice(Context context, String Address, String Type, String Setting) {
        super(context, Address, Type, Setting);
    }

    public static BluetoothScanResponse parseScanResponse(byte[] data)
    {
        BluetoothScanResponse reponse=new BluetoothScanResponse();
        reponse.FromBytes(data);
        return reponse;
    }


}
