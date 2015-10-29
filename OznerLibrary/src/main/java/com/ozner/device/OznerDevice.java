package com.ozner.device;

import android.content.Context;

/**
 * @author zhiyongxu
 *         浩泽设备基类
 * @category Device
 */
public abstract class OznerDevice {
    public String address;
    private BaseDeviceIO deviceIO;
    private Context context;
    private DeviceSetting setting;
    private String Model;
    private boolean setChanged = false;
    private boolean isBackgoundMode = false;

    public OznerDevice(Context context, String Address, String Model, String Setting) {
        this.address = Address;
        this.Model = Model;
        this.context = context;
        this.setting = initSetting(Setting);
    }

    /**
     * 设备型号
     *
     * @return
     */
    public String Model() {
        return Model;
    }

    /**
     * 设置对象
     *
     * @return
     */
    public DeviceSetting Setting() {
        return setting;
    }

    /**
     * 地址
     *
     * @return
     */
    public String Address() {
        return address;
    }

    /**
     * 名称
     *
     * @return
     */
    public String getName() {
        return setting.name();
    }

    protected Context getContext() {
        return context;
    }

    /**
     * 蓝牙控制对象
     *
     * @return NULL=没有蓝牙连接
     */
    public BaseDeviceIO Bluetooth() {
        return deviceIO;
    }

    /**
     * 判断设备是否连接
     *
     * @return
     */
    public boolean connected() {
        return ((deviceIO != null) && (deviceIO.isReady()));
    }

    protected DeviceSetting initSetting(String Setting) {
        DeviceSetting setting = new DeviceSetting();
        setting.load(Setting);
        return setting;
    }

    /**
     * 通知设备设置变更
     */
    public void UpdateSetting() {
        setChanged = true;
    }

    protected void resetSettingUpdate() {
        setChanged = false;
    }

    protected boolean isSetChanged() {
        return setChanged;
    }


    public boolean Bind(BaseDeviceIO deviceIO) throws DeviceNotReadlyException {
        if (this.deviceIO == deviceIO)
            return false;
        if (this.deviceIO != null) {
            this.deviceIO.close();
            this.deviceIO = null;
        }

        this.deviceIO = deviceIO;
        if (deviceIO != null) {
            deviceIO.setBackgroundMode(isBackgroundMode());
            deviceIO.open();
        }
        return true;
    }

    public void setBackground(boolean isBackground) {
        if (isBackgoundMode != isBackground) {
            isBackgoundMode = isBackground;
            doBackgroundModeChange();
        }
        isBackgoundMode = isBackground;
    }

    public boolean isBackgroundMode() {
        return isBackgoundMode;
    }

    protected abstract void doBackgroundModeChange();


}
