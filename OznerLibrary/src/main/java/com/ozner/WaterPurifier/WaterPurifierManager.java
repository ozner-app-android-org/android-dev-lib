package com.ozner.WaterPurifier;

import android.content.Context;
import android.content.Intent;

import com.ozner.device.BaseDeviceIO;
import com.ozner.device.BaseDeviceManager;
import com.ozner.device.DeviceNotReadyException;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;
import com.ozner.wifi.mxchip.MXChipIO;

/**
 * Created by xzyxd on 2015/11/2.
 */
public class WaterPurifierManager extends BaseDeviceManager {

    /**
     * 新增一个配对的水机
     */
    public final static String ACTION_MANAGER_WATER_PURIFIER_ADD = "com.ozner.Water.Purifier.Add";
    /**
     * 删除配对水机
     */
    public final static String ACTION_MANAGER_WATER_PURIFIER_REMOVE = "com.ozner.Water.Purifier.Remove";
    /**
     * 更新配对水机
     */
    public final static String ACTION_MANAGER_WATER_PURIFIER_CHANGE = "com.ozner.Water.Purifier.Change";

    public WaterPurifierManager(Context context) {
        super(context);
    }

    public static boolean IsWaterPurifier(String Model) {
        if (Model == null) return false;
        return Model.trim().equals("MXCHIP_HAOZE_Water");
    }

    @Override
    protected void update(OznerDevice device) {
        if (device instanceof WaterPurifier) {
            Intent intent = new Intent(ACTION_MANAGER_WATER_PURIFIER_CHANGE);
            intent.putExtra("Address", device.Address());
            context().sendBroadcast(intent);
        }
    }

    @Override
    protected void add(OznerDevice device) {
        if (device instanceof WaterPurifier) {
            Intent intent = new Intent(ACTION_MANAGER_WATER_PURIFIER_ADD);
            intent.putExtra("Address", device.Address());
            context().sendBroadcast(intent);
        }
        super.add(device);
    }

    @Override
    protected void remove(OznerDevice device) {
        if (device instanceof WaterPurifier) {
            Intent intent = new Intent(ACTION_MANAGER_WATER_PURIFIER_REMOVE);
            intent.putExtra("Address", device.Address());
            context().sendBroadcast(intent);
        }
        super.remove(device);
    }

    @Override
    protected OznerDevice getDevice(BaseDeviceIO io) throws DeviceNotReadyException {
        if (io instanceof MXChipIO) {
            String address = io.getAddress();
            OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
            if (device != null) {
                return device;
            } else {
                if (IsWaterPurifier(io.getType())) {
                    WaterPurifier c = new WaterPurifier(context(), address, io.getType(), "");
                    c.Bind(io);
                    return c;
                }
            }
        }
        return null;
    }

    public OznerDevice newWaterPurifier(Context context, String address) {
        OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
        if (device != null) {
            return device;
        } else {
            WaterPurifier waterPurifier = new WaterPurifier(context(), address, "MXCHIP_HAOZE_Water", "");
            MXChipIO io = OznerDeviceManager.Instance().ioManagerList().mxChipIOManager()
                    .createNewIO(waterPurifier.Setting().name(), waterPurifier.Address(), waterPurifier.Type());
            try {
                waterPurifier.Bind(io);
            } catch (DeviceNotReadyException e) {
                e.printStackTrace();
            }

            return waterPurifier;
        }
    }


    @Override
    protected OznerDevice loadDevice(String address, String Type, String Setting) {
        if (IsWaterPurifier(Type)) {
            WaterPurifier waterPurifier = new WaterPurifier(context(), address, Type, Setting);
            OznerDeviceManager.Instance().ioManagerList().mxChipIOManager()
                    .createNewIO(waterPurifier.Setting().name(), waterPurifier.Address(), waterPurifier.Type());
            return waterPurifier;
        } else
            return null;
    }

    @Override
    public boolean isMyDevice(BaseDeviceIO io) {
        if (io instanceof MXChipIO) {
            return IsWaterPurifier(io.getType());
        } else return false;
    }
}
