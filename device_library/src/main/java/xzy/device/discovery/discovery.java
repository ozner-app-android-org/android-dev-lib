package xzy.device.discovery;

import android.content.Context;

import xzy.device.DeviceObject;

/**
 * Created by zhiyongxu on 2017/3/14.
 */

public abstract class discovery {
    Context context;
    discoveryCallback callback=null;

    protected DeviceObject callCallback(byte[] bytes)
    {
        if (callback!=null)
            return callback.onFoundDevice(bytes);
        else
            return null;
    }
    public void setCallback(discoveryCallback callback) {
        this.callback = callback;
    }

    public Context getContext() {
        return context;
    }

    public discovery(Context context) {
        this.context=context;
    }

    public abstract void start();
    public abstract void stop();
}
