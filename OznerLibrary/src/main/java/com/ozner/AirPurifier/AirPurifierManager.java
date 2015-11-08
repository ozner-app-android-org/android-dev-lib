package com.ozner.AirPurifier;

import android.content.Context;
import android.content.Intent;

import com.ozner.device.BaseDeviceIO;
import com.ozner.device.BaseDeviceManager;
import com.ozner.device.DeviceNotReadyException;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;
import com.ozner.wifi.mxchip.MXChipIO;

/**
 * Created by xzyxd on 2015/11/7.
 */
public class AirPurifierManager extends BaseDeviceManager {

    /**
     * 新增一个配对的水机
     */
    public final static String ACTION_MANAGER_AIR_PURIFIER_ADD = "com.ozner.Air.Purifier.Add";
    /**
     * 删除配对水机
     */
    public final static String ACTION_MANAGER_AIR_PURIFIER_REMOVE = "com.ozner.Air.Purifier.Remove";
    /**
     * 更新配对水机
     */
    public final static String ACTION_MANAGER_AIR_PURIFIER_CHANGE = "com.ozner.Air.Purifier.Change";

    public AirPurifierManager(Context context) {
        super(context);
    }

    public static boolean IsAirPurifier(String Model) {
        if (Model == null) return false;
        return Model.trim().equals("FOG_HAOZE_AIR");
    }

    @Override
    protected void update(OznerDevice device) {
        if (device instanceof AirPurifier_MXChip) {
            Intent intent = new Intent(ACTION_MANAGER_AIR_PURIFIER_CHANGE);
            intent.putExtra("Address", device.Address());
            context().sendBroadcast(intent);
        }
    }

    @Override
    protected void add(OznerDevice device) {
        if (device instanceof AirPurifier_MXChip) {
            Intent intent = new Intent(ACTION_MANAGER_AIR_PURIFIER_ADD);
            intent.putExtra("Address", device.Address());
            context().sendBroadcast(intent);
        }
        super.add(device);
    }

    @Override
    protected void remove(OznerDevice device) {
        if (device instanceof AirPurifier_MXChip) {
            Intent intent = new Intent(ACTION_MANAGER_AIR_PURIFIER_REMOVE);
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
                if (IsAirPurifier(io.getModel())) {
                    AirPurifier_MXChip c = new AirPurifier_MXChip(context(), address, io.getModel(), "");
                    c.Bind(io);
                    return c;
                }
            }
        }
        return null;
    }

    public OznerDevice newAirPurifier(Context context, String address) {
        OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
        if (device != null) {
            return device;
        } else {
            AirPurifier_MXChip waterPurifier = new AirPurifier_MXChip(context(), address, "MXCHIP_HAOZE_Air", "");
            MXChipIO io = OznerDeviceManager.Instance().ioManagerList().mxChipIOManager()
                    .createNewIO(waterPurifier.Setting().name(), waterPurifier.Address(), waterPurifier.Model());
            try {
                waterPurifier.Bind(io);
            } catch (DeviceNotReadyException e) {
                e.printStackTrace();
            }

            return waterPurifier;
        }
    }


    @Override
    protected OznerDevice loadDevice(String address, String Model, String Setting) {
        if (IsAirPurifier(Model)) {
            AirPurifier_MXChip waterPurifier = new AirPurifier_MXChip(context(), address, Model, Setting);
            OznerDeviceManager.Instance().ioManagerList().mxChipIOManager()
                    .createNewIO(waterPurifier.Setting().name(), waterPurifier.Address(), waterPurifier.Model());
            return waterPurifier;
        } else
            return null;
    }

    @Override
    public boolean isMyDevice(BaseDeviceIO io) {
        if (io instanceof MXChipIO) {
            return IsAirPurifier(io.getModel());
        } else return false;
    }
}
