package com.ozner.cup;

import java.io.InvalidClassException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.ozner.device.DeviceSetting;
import com.ozner.device.OznerBluetoothDevice;
import com.ozner.device.OznerContext;
import com.ozner.device.OznerDevice;
import com.ozner.util.SQLiteDB;
import com.ozner.util.dbg;

/**
 * 智能杯对象
 *
 * @author zhiyongxu
 * @category 智能杯
 */
public class Cup extends OznerDevice {
    CupVolume mVolumes;
    /**
     * 饮水记录传输完成
     */
    public final static String ACTION_BLUETOOTHCUP_RECORD_COMPLETE = "com.ozner.cup.bluetooth.record.complete";

    public Cup(OznerContext context, String Address, String Serial, String Model, String Setting,
               SQLiteDB db) {
        super(context, Address, Serial, Model, Setting, db);
        mVolumes = new CupVolume(Address, db);
    }

    @Override
    protected DeviceSetting initSetting(String Setting) {
        DeviceSetting setting = new CupSetting();
        setting.load(Setting);
        return setting;
    }

    ;

    /**
     * 获取水杯设置
     */
    public CupSetting Setting() {
        return (CupSetting) super.Setting();
    }

    /**
     * 获取饮水记录
     *
     * @return
     */
    public CupVolume Volume() {
        return mVolumes;
    }

    /**
     * 获取蓝牙操作对象
     *
     * @return 返回NULL说明没有蓝牙连接
     */
    public BluetoothCup GetBluetooth() {
        return (BluetoothCup) this.Bluetooth();
    }

    CupMonitor mCupMonitor = new CupMonitor();

    @Override
    public boolean Bind(OznerBluetoothDevice bluetooth) {
        if (bluetooth == GetBluetooth()) return false;
        if ((!(bluetooth instanceof BluetoothCup)) && (bluetooth != null)) {
            throw new ClassCastException("错误的类型");
        }
        if (bluetooth != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothCup.ACTION_BLUETOOTHCUP_RECORD_RECV_COMPLETE);
            getContext().registerReceiver(mCupMonitor, filter);
        } else {
            getContext().unregisterReceiver(mCupMonitor);
        }

        return super.Bind(bluetooth);
    }

    class CupMonitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!intent.getStringExtra("Address").equals(Address()))
                return;
            if (action.equals(BluetoothCup.ACTION_BLUETOOTHCUP_RECORD_RECV_COMPLETE)) {
                if (GetBluetooth() != null) {
                    mVolumes.addRecord(GetBluetooth().GetReocrds());
                    Intent comp_intent = new Intent(
                            ACTION_BLUETOOTHCUP_RECORD_COMPLETE);
                    dbg.i("send ACTION_BLUETOOTHCUP_RECORD_COMPLETE:" + GetBluetooth().GetReocrds().length);

                    comp_intent.putExtra("Address", Cup.this.Address());
                    getContext().sendBroadcast(comp_intent);
                }
            }
        }
    }

}
