package com.ozner.cup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import com.ozner.bluetooth.BaseBluetoothDeviceManager;
import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.DeviceNotReadyException;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

import java.util.ArrayList;

@SuppressLint("NewApi")
/**
 * 智能杯管理对象
 * @category 智能杯
 * @author zhiyongxu
 *
 */
public class CupManager extends BaseBluetoothDeviceManager {

    /**
     * 新增一个配对的水杯
     */
    public final static String ACTION_MANAGER_CUP_ADD = "com.ozner.cup.CupManager.Cup.Add";
    /**
     * 删除配对水杯
     */
    public final static String ACTION_MANAGER_CUP_REMOVE = "com.ozner.cup.CupManager.Cup.Remove";
    /**
     * 更新配对水杯
     */
    public final static String ACTION_MANAGER_CUP_CHANGE = "com.ozner.cup.CupManager.Cup.Change";
    public CupManager(Context context) {
        super(context);
    }

    public static boolean IsCup(String Model) {
        if (Model == null) return false;
        return Model.trim().equals("CP001");
    }

    /**
     * 获取为配对的水杯蓝牙控制对象集合
     */
    public BaseDeviceIO[] getNotBindCups() {
        ArrayList<BaseDeviceIO> list = new ArrayList<>();
        OznerDeviceManager mgr = OznerDeviceManager.Instance();
        for (BaseDeviceIO device : mgr.bluetoothIOMgr().getAvailableDevices()) {
            if (IsCup(device.getModel())) {
                if (mgr.getDevice(device.getAddress()) == null) {
                    list.add(device);
                }
            }
        }
        return list.toArray(new BaseDeviceIO[list.size()]);
    }

    /**
     * 通过MAC地址获取指定的智能杯
     *
     * @param address 水杯MAC地址
     * @return 返回NULL，说明没有杯子
     */
    public Cup getCup(String address) {
        return (Cup) OznerDeviceManager.Instance().getDevice(address);
    }

    /**
     * 在数据库中构造一个新的水杯
     *
     * @param address     水杯地址
     * @param SettingJson 配置 JSON
     * @return 水杯实例
     */
    public Cup newCup(String address, String Name, String SettingJson) {
        Cup c = new Cup(context(), address, "CP001", SettingJson);
        c.Setting().name(Name);
        OznerDeviceManager.Instance().save(c);
        return c;
    }

    /**
     * 获取所有者是其他人的杯子
     *
     * @return
     */
    public Cup[] getOtherCupList() {
        ArrayList<Cup> list = new ArrayList<>();
        for (OznerDevice cup : OznerDeviceManager.Instance().getDevices()) {
            if (cup instanceof Cup) {
                if (!((CupSetting) cup.Setting()).isMe())
                    list.add((Cup) cup);
            }
        }
        return list.toArray(new Cup[list.size()]);

    }

    /**
     * 获取所有配对过的杯子
     */
    public Cup[] getCupList() {
        ArrayList<Cup> list = new ArrayList<>();
        for (OznerDevice cup : OznerDeviceManager.Instance().getDevices()) {
            if (cup instanceof Cup) {
                list.add((Cup) cup);
            }
        }
        return list.toArray(new Cup[list.size()]);

    }

    /**
     * 获取所有人是我的智能杯
     */
    public Cup[] getMyCupList() {
        ArrayList<Cup> list = new ArrayList<>();
        for (OznerDevice cup : OznerDeviceManager.Instance().getDevices()) {
            if (cup instanceof Cup) {
                if (((CupSetting) cup.Setting()).isMe())
                    list.add((Cup) cup);
            }
        }
        return list.toArray(new Cup[list.size()]);

    }

    @Override
    protected void update(OznerDevice device) {
        if (device instanceof Cup) {
            Intent intent = new Intent(ACTION_MANAGER_CUP_CHANGE);
            intent.putExtra("Address", device.Address());
            context().sendBroadcast(intent);
        }
    }

    @Override
    protected void add(OznerDevice device) {
        if (device instanceof Cup) {
            Intent intent = new Intent(ACTION_MANAGER_CUP_ADD);
            intent.putExtra("Address", device.Address());
            context().sendBroadcast(intent);
        }
        super.add(device);
    }

    @Override
    protected void remove(OznerDevice device) {
        if (device instanceof Cup) {
            Intent intent = new Intent(ACTION_MANAGER_CUP_REMOVE);
            intent.putExtra("Address", device.Address());
            context().sendBroadcast(intent);
        }
        super.remove(device);
    }


    @Override
    protected OznerDevice loadDevice(String address,
                                     String Model, String Setting) {
        if (IsCup(Model)) {
            return new Cup(context(), address, Model, Setting);
        } else
            return null;
    }

    @Override
    protected OznerDevice getDevice(BaseDeviceIO io) throws DeviceNotReadyException {
        if (io instanceof BluetoothIO) {
            String address = io.getAddress();
            OznerDevice device = OznerDeviceManager.Instance().getDevice(address);
            if (device != null) {
                return device;
            } else {
                if (IsCup(io.getModel())) {
                    Cup c = new Cup(context(), address, io.getModel(), "");
                    c.Setting().name(io.getName());
                    c.Bind(io);
                    return c;
                }
            }
        }
        return null;
    }

    @Override
    protected boolean chekcBindMode(String Model, int CustomType, byte[] CustomData) {
        if (IsCup(Model)) {
            if ((CustomType == Cup.AD_CustomType_Gravity) && (CustomData != null)) {
                CupGravity gravity = new CupGravity();
                gravity.FromBytes(CustomData, 0);
                return gravity.IsHandstand();
            } else
                return false;
        } else
            return false;
    }

    @Override
    public boolean isMyDevice(BaseDeviceIO io) {
        if (io == null) return false;
        return IsCup(io.getModel());
    }
}
