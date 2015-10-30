package com.ozner.device;

import android.content.Context;
import android.content.Intent;

import com.ozner.bluetooth.BluetoothIOMgr;
import com.ozner.util.Helper;
import com.ozner.util.SQLiteDB;
import com.ozner.util.dbg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class OznerDeviceManager implements IOManager.IOManagerCallback {

    /**
     * 新增一个配对设备广播
     */
    public final static String ACTION_OZNER_MANAGER_DEVICE_ADD = "com.ozner.manager.device.add";
    /**
     * 删除设备广播
     */
    public final static String ACTION_OZNER_MANAGER_DEVICE_REMOVE = "com.ozner.manager.device.remove";
    /**
     * 修改设备广播
     */
    public final static String ACTION_OZNER_MANAGER_DEVICE_CHANGE = "com.ozner.manager.device.change";
    static OznerDeviceManager instance;
    final HashMap<String, OznerDevice> devices = new HashMap<>();
    final ArrayList<BaseDeviceManager> mManagers = new ArrayList<>();
    SQLiteDB sqLiteDB;
    String owner = "";
    boolean isBackground = false;
    //蓝牙管理器
    BluetoothIOMgr bluetoothIOMgr;
    Context context;


    public OznerDeviceManager(Context context) throws InstantiationException {
        if (instance != null) {
            throw new InstantiationException();
        }

        this.context = context;
        sqLiteDB = new SQLiteDB(context);
        //导入老表
        importOldDB();
        bluetoothIOMgr = new BluetoothIOMgr(context);
        bluetoothIOMgr.setIoManagerCallback(this);
        instance = this;
    }

    public void start() {
        bluetoothIOMgr.Start();
    }

    public void stop() {
        bluetoothIOMgr.Stop();
    }

    public static OznerDeviceManager Instance() {
        return instance;
    }

    public BluetoothIOMgr bluetoothIOMgr() {
        return bluetoothIOMgr;
    }

    /**
     * 获取用户对应的表名
     */
    private String getOwnerTableName() {
        if (Helper.StringIsNullOrEmpty(owner)) return null;
        return Helper.MD5(owner.trim());
    }

    private void importOldDB() {
        HashSet<String> ownerList = new HashSet<>();

        try {
            List<String[]> result = sqLiteDB.ExecSQL("SELECT DISTINCT name from OznerDevices", new String[0]);
            for (String[] list : result) {
                String owner = list[0];
                if (owner != null) {
                    owner = owner.trim();
                }

                if (Helper.StringIsNullOrEmpty(owner)) continue;

                if (!ownerList.contains(owner))
                    ownerList.add(owner);
            }
        } catch (Exception e) {
        }


        try {
            List<String[]> result = sqLiteDB.ExecSQL("SELECT DISTINCT name from CupSetting", new String[0]);
            for (String[] list : result) {
                String owner = list[0];
                if (owner != null)
                    owner = owner.trim();
                if (Helper.StringIsNullOrEmpty(owner)) continue;
                if (!ownerList.contains(owner))
                    ownerList.add(owner);
            }
        } catch (Exception e) {
        }

        for (String owner : ownerList) {
            String table = Helper.MD5(owner);
            String Sql = String.format("CREATE TABLE IF NOT EXISTS %s (Address VARCHAR PRIMARY KEY NOT NULL,getModel Text NOT NULL,JSON TEXT)", table);
            sqLiteDB.execSQLNonQuery(Sql, new String[]{});
            try {
                String sql = String.format("INSERT INTO %s (Address,getModel,JSON) SELECT Address,'CUP001',JSON from CupSetting where Owner=?", table);
                sqLiteDB.execSQLNonQuery(sql, new String[]{owner});
            } catch (Exception e) {

            }

            try {
                String sql = String.format("INSERT INTO %s (Address,getModel,JSON) SELECT Address,getModel,JSON from OznerDevices where Owner=?", table);
                sqLiteDB.execSQLNonQuery(sql, new String[]{owner});
            } catch (Exception e) {

            }
        }
        try {
            sqLiteDB.execSQLNonQuery("DROP TABLE CupSetting", new String[]{});
            sqLiteDB.execSQLNonQuery("DROP TABLE OznerDevices", new String[]{});
        } catch (Exception e) {

        }
    }

    protected String Owner() {
        return owner;
    }

    /**
     * 设置绑定的用户
     *
     * @param owner 用户ID
     */
    public void setOwner(String owner) {
        if (owner != null)
            owner = owner.trim();
        if (Helper.StringIsNullOrEmpty(owner)) return;
        if (this.owner != null) {
            if (this.owner.equals(owner)) return;
        }
        this.owner = owner;
        synchronized (this) {
            devices.clear();
        }

        String Sql = String.format("CREATE TABLE IF NOT EXISTS %s (Address VARCHAR PRIMARY KEY NOT NULL,getModel Text NOT NULL,JSON TEXT)", getOwnerTableName());
        sqLiteDB.execSQLNonQuery(Sql, new String[]{});


        CloseAll();
        LoadDevices();
        dbg.i("Set Owner:%s", owner);
    }

    protected void CloseAll() {
        bluetoothIOMgr.closeAll();
    }

    private void LoadDevices() {
        String sql = String.format("select Address,getModel,JSON from %s", getOwnerTableName());
        List<String[]> list = sqLiteDB.ExecSQL(sql, new String[]{});
        synchronized (this) {
            for (String[] v : list) {
                String Address = v[0];
                String Model = v[1];
                String Json = v[2];
                if (!devices.containsKey(Address)) {
                    for (BaseDeviceManager mgr : getManagers()) {
                        OznerDevice device = mgr.loadDevice(Address, Model, Json);
                        device.setBackground(isBackground());

                        if (device != null) {
                            devices.put(Address, device);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 删除一个已经配对的设备
     */
    public void remove(OznerDevice device) {
        String sql = String.format("delete from %s where Address=?", getOwnerTableName());
        sqLiteDB.execSQLNonQuery(sql, new String[]{device.Address()});

        String address = device.Address();
        synchronized (this) {
            if (devices.containsKey(address)) {
                devices.remove(address);
            }
            Intent intent = new Intent(ACTION_OZNER_MANAGER_DEVICE_REMOVE);
            intent.putExtra("Address", address);
            context.sendBroadcast(intent);

            ArrayList<BaseDeviceManager> list = getManagers();
            for (BaseDeviceManager mgr : list) {
                mgr.remove(device);
            }

            if (device.IO() != null) {
                device.IO().close();
            }
            try {
                device.Bind(null);
            } catch (DeviceNotReadyException e) {
                e.printStackTrace();
            }
        }
    }

//	/**
//	 * 通过蓝牙设备获取一个设备控制对象
//	 *
//	 */
//	public OznerDevice getDevice(OznerBluetoothDevice bluetooth)
//			throws NotSupportDeviceException {
//		String address = bluetooth.getAddress();
//		OznerDevice device = getDevice(address);
//		if (device == null) {
//			ArrayList<BaseDeviceManager> list = getManagers();
//			for (BaseDeviceManager mgr : list) {
//				device = mgr.getDevice(bluetooth);
//				if (device != null)
//					return device;
//			}
//		}
//		return device;
//	}

    /**
     * 获取所有设备集合
     */
    public OznerDevice[] getDevices() {
        synchronized (this) {
            return devices.values().toArray(new OznerDevice[devices.size()]);
        }
    }

    /**
     * 通过MAC地址获取已经保存的设备
     */
    public OznerDevice getDevice(String address) {
        synchronized (this) {
            if (devices.containsKey(address))
                return devices.get(address);
            else
                return null;
        }
    }

    /**
     * 通过一个IO接口来构造或者查找一个对应的设备
     *
     * @param io 接口实例
     * @return 返回NULL无对应的设备
     */
    public OznerDevice getDevice(BaseDeviceIO io) throws NotSupportDeviceException {
        synchronized (mManagers) {
            for (BaseDeviceManager mgr : mManagers) {
                if (mgr.isMyDevice(io)) {
                    try {
                        return mgr.getDevice(io);
                    } catch (DeviceNotReadyException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }
            throw new NotSupportDeviceException();
        }
    }

    public BaseDeviceIO[] getNotBindDevices() {
        ArrayList<BaseDeviceIO> list = new ArrayList<>();
        ArrayList<BaseDeviceIO> result = new ArrayList<>();
        Collections.addAll(list, bluetoothIOMgr.getAvailableDevices());

        synchronized (this) {
            for (BaseDeviceIO io : list) {
                if (!devices.containsKey(io.getAddress()))
                    result.add(io);
            }
        }
        return result.toArray(new BaseDeviceIO[result.size()]);
    }


    /**
     * 保存并绑定设备设置
     */
    public void save(OznerDevice device) {
        synchronized (this) {
            String Address = device.Address();
            boolean isNew;
            if (Helper.StringIsNullOrEmpty(Owner())) return;

            if (!devices.containsKey(Address)) {
                devices.put(Address, device);
                isNew = false;
            } else
                isNew = true;
            device.setBackground(isBackground());

            String sql = String.format("INSERT OR REPLACE INTO %s (Address,getModel,JSON) VALUES (?,?,?);", getOwnerTableName());

            sqLiteDB.execSQLNonQuery(sql,
                    new String[]{device.Address(), device.Model(),
                            device.Setting().toString()});

            Intent intent = new Intent();
            intent.putExtra("Address", Address);
            intent.setAction(isNew ? ACTION_OZNER_MANAGER_DEVICE_ADD
                    : ACTION_OZNER_MANAGER_DEVICE_CHANGE);
            context.sendBroadcast(intent);
            device.resetSettingUpdate(); //刷新设置变更

            ArrayList<BaseDeviceManager> list = getManagers();
            if (isNew) {
                for (BaseDeviceManager mgr : list) {
                    mgr.add(device);
                }
            } else {
                for (BaseDeviceManager mgr : list) {
                    mgr.update(device);
                }
            }
        }
    }

    private ArrayList<BaseDeviceManager> getManagers() {
        ArrayList<BaseDeviceManager> list = new ArrayList<>();
        synchronized (this) {
            list.addAll(mManagers);
        }
        return list;
    }

    /**
     * 注册一个设备管理器
     */
    public void registerManager(BaseDeviceManager manager) {
        synchronized (mManagers) {
            if (!mManagers.contains(manager)) {
                mManagers.add(manager);
            }
        }
    }

    /**
     * 注销设备管理器
     */
    public void unregisterManager(BaseDeviceManager manager) {
        synchronized (mManagers) {
            if (mManagers.contains(manager))
                mManagers.remove(manager);
        }
    }


    public boolean isBackground() {
        return isBackground;
    }

    /**
     * 设置前后台模式
     */
    public void setBackgroundMode(boolean isBackground) {
        if (this.isBackground == isBackground) return;
        this.isBackground = isBackground;
        bluetoothIOMgr.setBackgroundMode(isBackground);
        synchronized (devices) {
            for (OznerDevice device : devices.values()) {
                device.setBackground(isBackground);
            }
        }
    }

    @Override
    public void onDeviceAvailable(IOManager manager, BaseDeviceIO io) {
        if (io != null) {
            OznerDevice device = getDevice(io.getAddress());
            if (device != null) {
                try {
                    device.Bind(io);
                } catch (DeviceNotReadyException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDeviceUnavailable(IOManager manager, BaseDeviceIO io) {
        if (io != null) {
            OznerDevice device = getDevice(io.getAddress());
            if (device != null) {
                try {
                    device.Bind(null);
                } catch (DeviceNotReadyException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
