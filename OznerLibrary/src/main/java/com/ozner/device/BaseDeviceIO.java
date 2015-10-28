package com.ozner.device;

/**
 * Created by zhiyongxu on 15/10/28.
 */
public abstract class BaseDeviceIO {
    /**
     * 蓝牙设备关闭回调
     *
     * @author xzy
     */
    public interface CloseCallback {
        void onClose(BaseDeviceIO device);
    }

    public interface StatusCallback {
        void onConnected(BaseDeviceIO io);

        void onDisconnected(BaseDeviceIO io);

        void onReadly(BaseDeviceIO io);
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

    public abstract class DataSendProxy {
        public abstract boolean send(byte[] data);
    }

    OnRecvPacketCallback recvPacketCallback;
    boolean isBackgroundMode = false;
    CloseCallback closeCallback;
    OnInitCallback onInitCallback = null;
    StatusCallback statusCallback = null;

    public abstract boolean send(byte[] bytes);

    public abstract void close();

    public abstract void open() throws DeviceNotReadlyException;

    public abstract boolean connected();


    /**
     * 设置设备前后台模式
     *
     * @param isBackground
     */
    public void setBackgroundMode(boolean isBackground) {
        isBackgroundMode = isBackground;
    }

    public boolean getBackgroundMode() {
        return isBackgroundMode;
    }

    /**
     * 设置数据接收回调
     *
     * @param callback
     */
    public void setRecvPacketCallback(OnRecvPacketCallback callback) {
        recvPacketCallback = callback;
    }

    public OnRecvPacketCallback getRecvPacketCallback() {
        return recvPacketCallback;
    }


    public void setStatusCallback(StatusCallback statusCallback) {
        this.statusCallback = statusCallback;
    }

    public StatusCallback getStatusCallback() {
        return statusCallback;
    }

    protected void doConnected() {
        if (statusCallback != null) {
            statusCallback.onConnected(this);
        }
    }

    protected void doDisconnected() {
        if (statusCallback != null) {
            statusCallback.onDisconnected(this);
        }
    }

    protected void doReadly() {
        if (statusCallback != null) {
            statusCallback.onReadly(this);
        }
    }

    public OnInitCallback getOnInitCallback() {
        return onInitCallback;
    }

    /**
     * 蓝牙连接初始化完成时的回调
     *
     * @param onInitCallback
     */
    public void setOnInitCallback(OnInitCallback onInitCallback) {
        this.onInitCallback = onInitCallback;
    }

    /**
     * 设置接口关闭回调
     *
     * @param closeCallback
     */
    public void setCloseCallback(CloseCallback closeCallback) {
        this.closeCallback = closeCallback;
    }


    public CloseCallback getCloseCallback() {
        return closeCallback;
    }

    public interface OnRecvPacketCallback {
        void onRecvPacket(byte[] bytes);
    }


    protected boolean doInit(DataSendProxy handle) {
        if (onInitCallback != null) {
            return onInitCallback.doInit(handle);
        } else
            return true;
    }

    protected void doClose() {
        if (closeCallback != null) {
            closeCallback.onClose(this);
        }
    }

    protected void doRecvPacket(byte[] bytes) {
        if (recvPacketCallback != null) {
            recvPacketCallback.onRecvPacket(bytes);
        }
    }

    public abstract String getAddress();
}
