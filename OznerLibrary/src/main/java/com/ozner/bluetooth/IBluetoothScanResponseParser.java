package com.ozner.bluetooth;

/**
 * Created by zhiyongxu on 16/9/14.
 */
public interface IBluetoothScanResponseParser {
    BluetoothScanResponse parseScanResponse(String name,byte[] Manufacturer_Specific,byte[] Service_Data);
}
