package com.ozner.device;

/**
 * Created by zhiyongxu on 15/10/28.
 */
public abstract class BaseDeviceIO {

    String Model = "";
    boolean isReady = false;

    OnRecvPacketCallback recvPacketCallback;
    boolean isBackgroundMode = false;
    OnInitCallback onInitCallback = null;
    StatusCallback statusCallback = null;


    public BaseDeviceIO(String Model) {
        this.Model = Model;
    }

    public String Model() {
        return this.Model;
    }

    public boolean isReady() {
        return isReady;
    }

    public abstract boolean send(byte[] bytes);

    public abstract void close();

    public abstract void open() throws DeviceNotReadlyException;

    public abstract boolean connected();

    protected abstract void doBackgroundModeChange();

    public boolean isBackgroundMode() {
        return isBackgroundMode;
    }

    /**
     * 设置设备前后台模式
     */
    public void setBackgroundMode(boolean isBackground) {
        if (this.isBackgroundMode != isBackground) {
            isBackgroundMode = isBackground;
            doBackgroundModeChange();
        }
    }

    public OnRecvPacketCallback getRecvPacketCallback() {
        return recvPacketCallback;
    }

    /**
     * 设置数据接收回调
     */
    public void setRecvPacketCallback(OnRecvPacketCallback callback) {
        recvPacketCallback = callback;
    }

    public StatusCallback getStatusCallback() {
        return statusCallback;
    }

    public void setStatusCallback(StatusCallback statusCallback) {
        this.statusCallback = statusCallback;
    }

    protected void doConnected() {
        isReady = false;
        if (statusCallback != null) {
            statusCallback.onConnected(this);
        }
    }

    protected void doDisconnected() {
        isReady = false;
        if (statusCallback != null) {
            statusCallback.onDisconnected(this);
        }
    }

    protected void doReady() {
        isReady = true;
        if (statusCallback != null) {
            statusCallback.onReady(this);
        }
    }

    public OnInitCallback getOnInitCallback() {
        return onInitCallback;
    }

    /**
     * 蓝牙连接初始化完成时的回调
     */
    public void setOnInitCallback(OnInitCallback onInitCallback) {
        this.onInitCallback = onInitCallback;
    }

    protected boolean doInit(DataSendProxy handle) {
        return onInitCallback == null || onInitCallback.doInit(handle);
    }

    protected void doRecvPacket(byte[] bytes) {
        if (recvPacketCallback != null) {
            recvPacketCallback.onRecvPacket(bytes);
        }
    }

    public abstract String getName();

    public abstract String getAddress();


    public interface StatusCallback {
        void onConnected(BaseDeviceIO io);

        void onDisconnected(BaseDeviceIO io);

        void onReady(BaseDeviceIO io);
    }


    public interface OnInitCallback {
        /**
         * 连接完成以后,通过回调来继续初始化操作
         *
         * @param sendHandle
         * @return 返回FALSE初始化失败
         */
        boolean doInit(DataSendProxy sendHandle);
    }

    public interface OnRecvPacketCallback {
        void onRecvPacket(byte[] bytes);
    }

    public abstract class DataSendProxy {
        public abstract boolean send(byte[] data);
    }


}
