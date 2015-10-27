package com.ozner.wifi;

import java.util.HashMap;

/**
 * Created by zhiyongxu on 15/10/26.
 */
public interface WifiControlInterface {
    boolean setProprtys(HashMap<String, byte[]> proprtys);

    boolean queryProprtys(String[] propertys);

    void setCallback(WifiControlCallback callback);
}
