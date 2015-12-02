package com.ozner.device;

import android.content.Context;
import android.content.Intent;

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
    private String Type;

    public OznerDevice(Context context, String Address, String Type, String Setting) {
        super(context);
        this.address = Address;
        this.Type = Type;
        this.setting = initSetting(Setting);
    }



    /**
     * 设备数值改变
     */
    public final static String ACTION_DEVICE_UPDATE = "com.ozner.device.update";

    protected void doUpdate()
    {
        Intent intent = new Intent(ACTION_DEVICE_UPDATE);
        intent.putExtra(Extra_Address, Address());
        context().sendBroadcast(intent);
    }

    protected abstract String getDefaultName();

    public abstract Class<?> getIOType();

    /**
     * 设备类型
     */
    public String Type() {
        return Type;
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

    /**
     * 在后台模式时判断接口是否包含有效数据,如果是则连接,否不进行连接
     * @param io 接口IO
     * @return true包含数据
     */
    protected boolean doCheckAvailable(BaseDeviceIO io)
    {
        return true;
    }
    public boolean Bind(BaseDeviceIO deviceIO) throws DeviceNotReadyException {

        if ((deviceIO!=null) && (!deviceIO.getClass().equals(getIOType())))
        {
            throw new ClassCastException();
        }

        if (this.deviceIO == deviceIO)
            return false;

        if ((getRunningMode()==RunningMode.Background) && (deviceIO!=null))
        {
            if (!doCheckAvailable(deviceIO)) return false;
        }

        if (Helper.StringIsNullOrEmpty(setting.name())) {
            setting.name(getDefaultName());
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
