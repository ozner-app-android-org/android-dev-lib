package com.ozner.wifi.mxchip;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ozner.util.Helper;
import com.ozner.wifi.OperationCallback;
import com.ozner.wifi.WifiDevice;
import com.ozner.wifi.WifiDeviceClass;

/**
 * Created by zhiyongxu on 15/10/26.
 */
public abstract class MxChipDevice extends WifiDevice {
    protected MXChip mxChip;
    protected String id;

    /**
     * 将JSON初始化成对应的设备实例
     *
     * @param mxChip
     * @param json
     * @return
     */
    public static MxChipDevice newInstance(MXChip mxChip, String json) {
        if (Helper.StringIsNullOrEmpty(json)) return null;
        JSONObject jsonObject = JSON.parseObject(json);
        String id = jsonObject.getString("deviceID");
        String name = jsonObject.getString("name");
        String classType = jsonObject.getString("classType");

        if (Helper.StringIsNullOrEmpty(name)) return null;
        if (Helper.StringIsNullOrEmpty(id)) return null;
        if (Helper.StringIsNullOrEmpty(classType)) return null;

        switch (WifiDeviceClass.valueOf(classType)) {
            case AirPurifier: {
                WaterPurifier device = new WaterPurifier();
                device.mxChip = mxChip;
                device.id = id;
                device.name = name;
                return device;
            }

            case WaterPurifier: {
                WaterPurifier device = new WaterPurifier();
                device.mxChip = mxChip;
                device.id = id;
                device.name = name;
                return device;
            }
        }
        return null;
    }

    @Override
    public String toJSON(JSONObject jsonObject) {
        jsonObject.put("name", name);
        jsonObject.put("deviceID", id);
        return jsonObject.toJSONString();
    }

    @Override
    protected void sendData(byte[] data, OperationCallback<Void> cb) {
        mxChip.sendCommand(this.id, data, cb);
    }

    @Override
    public void close() {
        mxChip.close(this);
    }

    public abstract void onRecvData(byte[] data);
}
