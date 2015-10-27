package com.ozner.wifi.mxchip;

import android.content.Context;

import com.ozner.wifi.OperationCallback;

import java.util.HashMap;

/**
 * Created by zhiyongxu on 15/10/15.
 */
public class MXChip implements MQTTService.onPublishCallback {
    MQTTService mqttService;
    Context context;
    HashMap<String, MxChipDevice> deviceList = new HashMap<>();

    public MXChip(Context context) {
        this.context = context;
        mqttService = new MQTTService(context);
    }


    @Override
    public void onPublish(String topic, byte[] data) {
        String deviceId = topic.replace("/out", "");
        MxChipDevice device = null;
        synchronized (deviceList) {
            device = deviceList.get(deviceId);
        }
        if (device != null) {
            device.onRecvData(data);
        }
    }

    protected void sendCommand(String deviceId, byte[] data, OperationCallback<Void> cb) {
        String topic = deviceId + "/in";
        mqttService.publish(topic, data, cb);
    }


    public void close(MxChipDevice device) {
        synchronized (deviceList) {
            deviceList.remove(device.name);
        }
        mqttService.unSubscribe(device.id + "/out");
    }

    public MxChipDevice createDevice(String Name, String json) {
        synchronized (deviceList) {
            if (deviceList.containsKey(Name)) {
                return deviceList.get(Name);
            }
        }
        MxChipDevice device = MxChipDevice.newInstance(this, json);
        mqttService.subscribe(device.id + "/out");
        return device;
    }



}
