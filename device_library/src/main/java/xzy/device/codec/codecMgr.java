package xzy.device.codec;

import android.content.Context;

import java.util.ArrayList;

import xzy.device.DeviceObject;

/**
 * Created by zhiyongxu on 2017/3/15.
 */

public class codecMgr {
    private ArrayList<codec> coders =new ArrayList<>();

    public codecMgr(Context context)
    {

    }


    public DeviceObject decodeDeviceInfo(byte[] bytes)
    {
        for (codec codec : coders)
        {
            DeviceObject device =codec.decodeDeviceInfo(bytes);
            if (device !=null)
                return device;
        }
        return null;
    }
}
