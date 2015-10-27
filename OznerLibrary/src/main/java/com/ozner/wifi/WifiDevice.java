package com.ozner.wifi;

import com.alibaba.fastjson.JSONObject;

/**
 * Created by zhiyongxu on 15/10/26.
 */
public abstract class WifiDevice {

    /**
     * 设备名称
     */
    public String name;

    public abstract String toJSON(JSONObject jsonObject);

    protected abstract void sendData(byte[] data, OperationCallback<Void> cb);

    public abstract void close();
}
