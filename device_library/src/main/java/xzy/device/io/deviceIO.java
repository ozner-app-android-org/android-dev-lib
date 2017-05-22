package xzy.device.io;

/**
 * Created by zhiyongxu on 2017/3/14.
 */

public abstract class deviceIO {
    deviceIOCallback callback;
    public void setCallback(deviceIOCallback callback)
    {
        this.callback=callback;
    }
    public abstract void open(deviceIOCallback callback);
    public abstract void close();
    public abstract void write(byte[] bytes);
}