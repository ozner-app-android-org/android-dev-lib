package com.ozner.wifi;

import android.content.Intent;
import android.content.SharedPreferences;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ozner.BaseOznerManager;
import com.ozner.device.OznerContext;
import com.ozner.util.Helper;
import com.ozner.wifi.mxchip.MXChip;
import com.ozner.wifi.mxchip.MxChipDevice;

import java.util.HashMap;

/**
 * Created by zhiyongxu on 15/10/26.
 */
public class WifiDeviceManager extends BaseOznerManager {
    MXChip mxChip;
    HashMap<String, WifiDevice> deviceList = new HashMap<>();

    SharedPreferences preferences;

    @Override
    public void setOwner(String Owner) {
        super.setOwner(Owner);
        closeAll();
        loadDevice();
    }

    protected void closeAll() {
        synchronized (deviceList) {
            for (WifiDevice device : deviceList.values()) {
                device.close();
            }
        }
    }


    public WifiDeviceManager(OznerContext context) {
        super(context);
        ;
        mxChip = new MXChip(getContext());
    }


    protected void loadDevice() {
        synchronized (deviceList) {
            deviceList.clear();
        }
        String deviceString = preferences.getString(getOwner() + ".devices", "");

        if (!Helper.StringIsNullOrEmpty(deviceString)) {
            JSONArray jsonArray = null;
            try {
                jsonArray = JSON.parseArray(deviceString);
            } catch (Exception e) {
                return;
            }

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                WifiChipType chipType = WifiChipType.valueOf(jsonObject.getString("wifiType"));
                String name = jsonObject.getString("name");
                if (Helper.StringIsNullOrEmpty(name)) continue;
                if (deviceList.containsKey(name)) continue;
                WifiDevice device = null;
                switch (chipType) {
                    case MXChip:
                        device = mxChip.createDevice(name, jsonObject.toJSONString());
                        if (device != null) {
                            deviceList.put(name, device);
                        }
                        break;
                }
            }
        }
    }

    /**
     * 列举所有已配网的设备
     *
     * @return
     */
    public WifiDevice[] listDevices() {
        synchronized (deviceList) {
            return deviceList.values().toArray(new WifiDevice[0]);
        }
    }

    /**
     * 通过名称获取配网的设备
     *
     * @param Name
     * @return
     */
    public WifiDevice getDeivce(String Name) {
        synchronized (deviceList) {
            return deviceList.get(Name);
        }
    }

    /**
     * 删除已配网的设备
     *
     * @param device
     */
    public void remove(WifiDevice device) {
        synchronized (deviceList) {
            deviceList.remove(device.name);
        }
        device.close();
    }

    /**
     * 保存已配网的WIFI设备
     *
     * @param device
     * @return
     */
    public boolean save(WifiDevice device) {
        boolean isNew = false;
        if (getOwner() == null)
            return false;
        if (getOwner().isEmpty())
            return false;
        JSONArray jsonArray = new JSONArray();
        synchronized (deviceList) {
            if (!deviceList.containsKey(device.name)) {
                deviceList.put(device.name, device);
                isNew = false;
            } else
                isNew = true;

            for (WifiDevice d : deviceList.values()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", device.name);
                if (device instanceof MxChipDevice) {
                    jsonObject.put("wifiType", WifiChipType.MXChip);
                }
                device.toJSON(jsonObject);
                jsonArray.add(jsonObject);
            }
        }
        Intent intent = new Intent();
        intent.putExtra("Address", device.name);
        intent.setAction(isNew ? ACTION_OZNER_MANAGER_DEVICE_ADD
                : ACTION_OZNER_MANAGER_DEVICE_CHANGE);
        getContext().sendBroadcast(intent);

        return true;
    }


}
