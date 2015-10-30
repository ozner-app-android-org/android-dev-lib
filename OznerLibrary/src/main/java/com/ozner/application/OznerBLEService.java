package com.ozner.application;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.ozner.cup.CupManager;
import com.ozner.device.OznerDeviceManager;
import com.ozner.tap.TapManager;
import com.ozner.util.dbg;

import java.util.List;

public class OznerBLEService extends Service implements ActivityLifecycleCallbacks {
    static OznerDeviceManager mManager;
    static CupManager mCups;
    static TapManager mTaps;
    OznerBLEBinder binder = new OznerBLEBinder();
    public static final String ACTION_ServiceInit = "ozner.service.init";

    public OznerBLEService() {

    }

    public void checkBackgroundMode(boolean isClose) {
        ActivityManager activityManager = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE));
        List<ActivityManager.RunningTaskInfo> tasksInfo = activityManager.getRunningTasks(1);
        if (tasksInfo.size() > 0) {
            dbg.i("top Activity = "
                    + tasksInfo.get(0).topActivity.getPackageName());
            String packet = getPackageName();
            // 应用程序位于堆栈的顶层
            if (packet.equals(tasksInfo.get(0).topActivity.getPackageName())) {
                mManager.setBackgroundMode(isClose);
                return;
            }
        }
        mManager.setBackgroundMode(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            mManager = new OznerDeviceManager(getApplicationContext());
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        mCups = new CupManager(getApplicationContext());
        mTaps = new TapManager(getApplicationContext());
        mManager.start();
    }

    @Override
    public void onDestroy() {
        mManager.stop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        this.getApplication().registerActivityLifecycleCallbacks(this);
        BluetoothManager bluetoothManager = (BluetoothManager) this
                .getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        if (adapter.getState() == BluetoothAdapter.STATE_OFF) {
            adapter.enable();
        }

        //BluetoothWorkThread work=new BluetoothWorkThread(getApplicationContext());
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        this.getApplication().unregisterActivityLifecycleCallbacks(this);
        return super.onUnbind(intent);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        checkBackgroundMode(false);

    }

    @Override
    public void onActivityPaused(Activity activity) {
        checkBackgroundMode(true);
    }

    @Override
    public void onActivityStopped(Activity activity) {

        checkBackgroundMode(true);

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        checkBackgroundMode(false);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        checkBackgroundMode(false);
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        checkBackgroundMode(true);
    }

    public class OznerBLEBinder extends Binder {
        /**
         * 获取水杯管理器
         */
        public CupManager getCupManager() {
            return mCups;
        }

        /**
         * 获取水龙头管理器
         *
         * @return
         */
        public TapManager getTapManager() {
            return mTaps;
        }

        /**
         * 获取设备管理器
         *
         * @return
         */
        public OznerDeviceManager getDeviceManager() {
            return mManager;
        }



    }

}
