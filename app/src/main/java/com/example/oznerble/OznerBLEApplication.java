package com.example.oznerble;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.ozner.application.OznerBLEService;
import com.ozner.application.OznerBLEService.OznerBLEBinder;

public abstract class OznerBLEApplication extends Application {
    static int ActivityCount = 0;
    OznerBLEBinder localService = null;
    ServiceConnection mServiceConnection = null;

    public OznerBLEApplication() {
        this.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                ActivityCount++;
                checkActivity();
            }

            @Override
            public void onActivityPaused(Activity activity) {
                ActivityCount--;
                checkActivity();
            }


            @Override
            public void onActivityDestroyed(Activity activity) {

            }

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }
        });
    }

    public OznerBLEBinder getService() {
        return localService;
    }

    /**
     * 服务初始化完成时调用的方法
     */
    protected abstract void onBindService();

    @Override
    public void onCreate() {
        super.onCreate();
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                localService = (OznerBLEBinder) service;
                onBindService();
                checkActivity();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                localService = null;
            }
        };

        Intent intent = new Intent(this, OznerBLEService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void checkActivity() {
        return;
        /*
        if (ActivityCount<=0)
		{
			if (localService!=null)
			{
				localService.setBackgroundMode(true);
			}
		}else
		{
			if (localService!=null)
			{
				localService.setBackgroundMode(false);
			}
		}*/
    }

    @Override
    public void onTerminate() {
        unbindService(mServiceConnection);
        super.onTerminate();
    }
}
