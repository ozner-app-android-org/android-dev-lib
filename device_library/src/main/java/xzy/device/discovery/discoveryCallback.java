package xzy.device.discovery;

import xzy.device.DeviceObject;

/**
 * Created by zhiyongxu on 2017/3/14.
 */

public interface discoveryCallback {
    DeviceObject onFoundDevice(byte[] bytes);
}
