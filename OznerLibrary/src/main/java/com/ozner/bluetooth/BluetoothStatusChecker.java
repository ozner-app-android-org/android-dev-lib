package com.ozner.bluetooth;

import java.util.HashSet;
import java.util.Objects;

/**
 * Created by zhiyongxu on 15/5/25.
 */
public class BluetoothStatusChecker {
    private static Object mLockObject=new Object();

    public static Object getLockObject()
    {
        return mLockObject;
    }

    static HashSet<String> mConnectingDevices = new HashSet<>();
    public static boolean hashBluetoothBusy() {
        synchronized (mConnectingDevices) {
            return mConnectingDevices.size() > 0;
        }
    }

    public static void Busy(String Address)
    {
        synchronized (mConnectingDevices)
        {
            if (!mConnectingDevices.contains(Address))
            {
                mConnectingDevices.add(Address);
            }
        }
    }

    public static void Idle(String Address) {
        synchronized (mConnectingDevices) {
            if (mConnectingDevices.contains(Address)) {
                mConnectingDevices.remove(Address);
            }
        }
    }
}
