package xzy.device.codec;

import xzy.device.DeviceObject;

/**
 * Created by zhiyongxu on 2017/3/15.
 */

public abstract class codec {
    public abstract DeviceObject decodeDeviceInfo(byte[] data);
}
