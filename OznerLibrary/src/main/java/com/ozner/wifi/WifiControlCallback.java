package com.ozner.wifi;

/**
 * Created by zhiyongxu on 15/10/26.
 */
public interface WifiControlCallback {
    void onRecvProptry(String property, byte[] value);
}
