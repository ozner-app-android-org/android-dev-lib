package com.ozner.device;

import android.content.Context;

import com.ozner.XObject;
import com.ozner.util.Helper;

/**
 * @author zhiyongxu
 *         浩泽设备基类
 */
public abstract class OznerDevice extends XObject {
    public final static String Extra_Address = "Address";
    private String address;
    private BaseDeviceIO deviceIO;
    private DeviceSetting setting;
    private String Model;

    public OznerDevice(Context context, String Address, String Model, String Setting) {
        super(context);
        this.address = Address;
        this.Model = Model;
        this.setting = initSetting(Setting);

    }

    public abstract Class<?> getIOType();

    /**
     * 设备型号
     */
    public String Model() {
        return Model;
    }

    /**
     * 设置对象
     */
    public DeviceSetting Setting() {
        return setting;
    }


    /**
     * 地址
     */
    public String Address() {
        return address;
    }

    /**
     * 名称
     */
    public String getName() {
        return setting.name();
    }


    /**
     * 蓝牙控制对象
     *
     * @return NULL=没有蓝牙连接
     */
    public BaseDeviceIO IO() {
        return deviceIO;
    }


    /**
     * 判断设备是否连接
     */
    public BaseDeviceIO.ConnectStatus connectStatus() {
        if (deviceIO == null)
            return BaseDeviceIO.ConnectStatus.Disconnect;
        return deviceIO.connectStatus();
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
    }


    protected abstract void doSetDeviceIO(BaseDeviceIO oldIO, BaseDeviceIO newIO);

    public boolean Bind(BaseDeviceIO deviceIO) throws DeviceNotReadyException {
        if (this.deviceIO == deviceIO)
            return false;

        if (Helper.StringIsNullOrEmpty(setting.name())) {
            setting.name(deviceIO.getName());
        }

        doSetDeviceIO(this.deviceIO, deviceIO);

        if (this.deviceIO != null) {
            this.deviceIO.close();
            this.deviceIO = null;
        }

        this.deviceIO = deviceIO;
        if (deviceIO != null) {
            deviceIO.open();
        }

        return true;
    }


}
