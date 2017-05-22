package xzy.device.io;

/**
 * Created by zhiyongxu on 2017/3/16.
 */

public interface deviceIOCallback {
    void onDataAvailabl(byte[] bytes);
    void onDisconnected();
    void onConnected();
}
