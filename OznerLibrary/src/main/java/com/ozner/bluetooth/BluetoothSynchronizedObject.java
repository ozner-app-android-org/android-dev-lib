package com.ozner.bluetooth;

import java.util.HashSet;

/**
 * 蓝牙同步对象,用来保持所有蓝牙设备只有一个在操作
 * Created by zhiyongxu on 15/6/11.
 */
public final class BluetoothSynchronizedObject {
    private final static Object lockObject = new Object();
    private final static HashSet<String> mConnectingDevices = new HashSet<>();

    private BluetoothSynchronizedObject() {

    }

    public static Object getLockObject() {
        return lockObject;
    }


    /*public static boolean hashBluetoothBusy() {
        synchronized (mConnectingDevices) {
            return mConnectingDevices.size() > 0;
        }
    }*/

    public static boolean hashBluetoothBusy(String address) {
        synchronized (mConnectingDevices) {
            return mConnectingDevices.contains(address);
        }
    }

    public static void Busy(String Address) {
        synchronized (mConnectingDevices) {
            if (!mConnectingDevices.contains(Address)) {
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
