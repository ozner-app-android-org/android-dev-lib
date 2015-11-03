package com.ozner.tap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import com.ozner.bluetooth.BaseBluetoothDeviceManager;
import com.ozner.bluetooth.BluetoothIO;
import com.ozner.cup.Cup;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.DeviceNotReadyException;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

@SuppressLint("NewApi")
/**
 * 水探头管理器
 * @category 水探头
 * @author zhiyongxu
 *
 */
public class TapManager extends BaseBluetoothDeviceManager {
    final static int AD_CustomType_BindStatus = 0x10;
    /**
     * 新增一个配对的水杯
     */
    public final static String ACTION_MANAGER_TAP_ADD = "com.ozner.Tap.Add";
    /**
     * 删除配对水杯
     */
    public final static String ACTION_MANAGER_TAP_REMOVE = "com.ozner.Tap.Remove";
    /**
     * 更新配对水杯
     */
    public final static String ACTION_MANAGER_TAP_CHANGE = "com.ozner.Tap.Change";

    public TapManager(Context context) {
        super(context);
    }

    public static boolean IsTap(String model) {
        if (model.equals("SC001")) {
            return true;
        } else
            return false;
    }

    /**
     * 通过指定地址获取水探头设备
     *
     * @param address 设备MAC地址
     * @return 水探头实例
     */
    public Tap getTap(String address) {
        return (Tap) OznerDeviceManager.Instance().getDevice(address);
    }


    /**
     * 在数据库中构造一个新的水探头
     *
     * @param address     水杯地址
     * @param SettingJson 配置 JSON
     * @param Name        名称
     * @return 水探头实例
     */
    public Tap newTap(String address, String Name, String SettingJson) {
        Tap c = new Tap(context(), address, "SC001", SettingJson);
        c.Setting().name(Name);
        OznerDeviceManager.Instance().save(c);
        return c;
    }

    @Override
    protected OznerDevice getDevice(BaseDeviceIO io) throws DeviceNotReadyException {
        if (io instanceof BluetoothIO) {
            String address = io.getAddress();
            OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
            if (device != null) {
                return device;
            } else {
                if (IsTap(io.getModel())) {
                    Tap c = new Tap(context(), address, io.getModel(), "");
                    c.Bind(io);
                    return c;
                }
            }
        }
        return null;
    }

    @Override
    protected OznerDevice loadDevice(String address, String Model,
                                     String Setting) {
        if (IsTap(Model)) {
            return new Tap(context(), address, Model, Setting);
        } else
            return null;
    }


    @Override
    protected void update(OznerDevice device) {
        if (device instanceof Cup) {
            Intent intent = new Intent(ACTION_MANAGER_TAP_CHANGE);
            intent.putExtra("Address", device.Address());
            context().sendBroadcast(intent);
        }
    }

    @Override
    protected void add(OznerDevice device) {
        if (device instanceof Cup) {
            Intent intent = new Intent(ACTION_MANAGER_TAP_ADD);
            intent.putExtra("Address", device.Address());
            context().sendBroadcast(intent);
        }
        super.add(device);
    }

    @Override
    protected void remove(OznerDevice device) {
        if (device instanceof Cup) {
            Intent intent = new Intent(ACTION_MANAGER_TAP_REMOVE);
            intent.putExtra("Address", device.Address());
            context().sendBroadcast(intent);
        }
        super.remove(device);
    }

    @Override
    protected boolean chekcBindMode(String Model, int CustomType, byte[] CustomData) {
        if (IsTap(Model)) {
            if ((CustomType == AD_CustomType_BindStatus) && (CustomData != null) && (CustomData.length > 0)) {
                return CustomData[0] == 1;
            }
        }
        return false;
    }

    @Override
    public boolean isMyDevice(BaseDeviceIO io) {
        if (io == null) return false;
        return IsTap(io.getModel());
    }
}
