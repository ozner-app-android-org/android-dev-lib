package com.ozner.wifi.mxchip;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * Created by zhiyongxu on 15/10/18.
 */
public class WifiDevice {
    public String name;
    public String ap;
    public String localIP;
    public String firmware;
    public boolean activated;
    public boolean connected;
    public String Type;

    public static WifiDevice loadByFTCJson(String jsonString) {
        if ((jsonString == null) || (jsonString.equals(""))) {
            return null;
        }
        try {
            JSONObject json = JSON.parseObject(jsonString);
            if (json == null) return null;

            WifiDevice device = new WifiDevice();
            device.name = json.getString("N");
            device.Type = json.getString("FW").replace("@", "");
            JSONArray array = json.getJSONArray("C");
            for (int i = 0; i < array.size(); i++) {

                JSONObject object = array.getObject(i, JSONObject.class);
                if (object.getString("N").equals("WLAN")) {
                    JSONArray jsonArray = object.getJSONArray("C");
                    for (int x = 0; x < jsonArray.size(); x++) {
                        JSONObject sub = jsonArray.getObject(x, JSONObject.class);
                        String name = sub.getString("N");
                        if (name.equals("Wi-Fi")) {
                            device.ap = sub.getString("C");
                            continue;
                        }
                        if (name.equals("IP address")) {
                            device.localIP = sub.getString("C");
                            continue;
                        }
                    }
                    continue;
                }


                if (object.getString("N").equals("Cloud info")) {
                    JSONArray jsonArray = object.getJSONArray("C");
                    for (int x = 0; x < jsonArray.size(); x++) {
                        JSONObject sub = jsonArray.getObject(x, JSONObject.class);
                        String name = sub.getString("N");
                        if (name.equals("activated")) {
                            device.activated = sub.getBoolean("C");
                            continue;
                        }
                        if (name.equals("connected")) {
                            device.connected = sub.getBoolean("C");
                            continue;
                        }
                        if (name.equals("rom version")) {
                            device.firmware = sub.getString("C");
                            continue;
                        }
                    }
                    continue;
                }


            }
            return device;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
