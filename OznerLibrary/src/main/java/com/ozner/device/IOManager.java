package com.ozner.device;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by xzyxd on 2015/10/29.
 * IO管理基类
 */
public abstract class IOManager {

    /**
     * 当前设备列表
     */
    protected final HashMap<String, BaseDeviceIO> devices = new HashMap<>();
    IOManagerCallback ioManagerCallback;
    boolean isBackgroundMode = false;
    StatusCallbackImp statusCallback = new StatusCallbackImp();

    public IOManagerCallback getIoManagerCallback() {
        return ioManagerCallback;
    }

    /**
     * 设置IO接口状态回调
     */
    public void setIoManagerCallback(IOManagerCallback ioManagerCallback) {
        this.ioManagerCallback = ioManagerCallback;
    }

    protected void doAvailable(BaseDeviceIO io) {
        io.setStatusCallback(statusCallback);

        if (ioManagerCallback != null) {
            ioManagerCallback.onDeviceAvailable(this, io);
        }
    }

    protected void doUnavailable(BaseDeviceIO io) {
        io.setStatusCallback(null);
        if (ioManagerCallback != null) {
            ioManagerCallback.onDeviceUnavailable(this, io);
        }
    }

    /**
     * 获取可用的设备列表
     */
    public BaseDeviceIO[] getAvailableDevices() {
        synchronized (devices) {
            return devices.values().toArray(new BaseDeviceIO[devices.size()]);
        }
    }

    public boolean isBackgroundMode() {
        return isBackgroundMode;
    }

    public void setBackgroundMode(boolean isBackground) {
        isBackgroundMode = isBackground;
    }

    /**
     * 开始使用接口
     */
    public abstract void Start();

    /**
     * 停用接口
     */
    public abstract void Stop();

    /**
     * 关闭所有设备连接
     */
    public void closeAll() {
        ArrayList<BaseDeviceIO> list;
        synchronized (devices) {
            list = new ArrayList<>(devices.values());
        }
        for (BaseDeviceIO io : list) {
            io.close();
        }
    }

    public interface IOManagerCallback {
        /**
         * 当发现有可用设备时通知
         *
         * @param io 可用的设备接口
         */
        void onDeviceAvailable(IOManager manager, BaseDeviceIO io);

        /**
         * 当发现设备处于连接中断时通知
         */
        void onDeviceUnavailable(IOManager manager, BaseDeviceIO io);
    }

    //实现设备连接状态回调,在设备连接中断时,将设备设置成不可用状态,直到设备再次被发现
    class StatusCallbackImp implements BaseDeviceIO.StatusCallback {
        @Override
        public void onConnected(BaseDeviceIO io) {

        }

        @Override
        public void onDisconnected(BaseDeviceIO io) {
            doUnavailable(io);
            synchronized (devices) {
                devices.remove(io.getAddress());
            }
        }

        @Override
        public void onReady(BaseDeviceIO io) {

        }
    }

}
