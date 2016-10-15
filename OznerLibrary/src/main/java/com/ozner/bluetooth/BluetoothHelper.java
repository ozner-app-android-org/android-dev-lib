package com.ozner.bluetooth;

import java.util.UUID;

/**
 * Created by zhiyongxu on 16/9/14.
 */
public class BluetoothHelper {
    public static UUID GetUUID(int id) {
        return UUID.fromString(String.format(
                "%1$08x-0000-1000-8000-00805f9b34fb", id));
    }
}
