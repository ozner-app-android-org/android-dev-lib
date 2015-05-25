package com.ozner.application;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import com.ozner.bluetooth.BluetoothScan;
import com.ozner.bluetooth.BluetoothWorkThread;
import com.ozner.cup.CupManager;
import com.ozner.device.OznerContext;
import com.ozner.device.OznerDeviceManager;
import com.ozner.tap.TapManager;
import com.ozner.util.dbg;

public class OznerBLEService extends Service implements ActivityLifecycleCallbacks {
	static OznerContext mContext;
	static OznerDeviceManager mManager;
	static CupManager mCups;
	static BluetoothScan mScaner;
	static TapManager mTaps;
	OznerBLEBinder binder = new OznerBLEBinder();

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


	
		/**
		 * 判断是否处于激活状态
		 * 
		 * @return
		 */
		public boolean isRuning() {
			return mScaner.isRuning();
		}

	}

	Timer checkTimer=new Timer();
	ArrayList<Activity> activitys=new ArrayList<Activity>();
	Date lastTime;
	@Override
	public void onCreate() {
		super.onCreate();
		if (mScaner == null) {
			
			checkTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					Date date=new Date();
					long t=lastTime!=null?date.getTime()-lastTime.getTime():0;
					if (t>5000)
						checkBackMode();
				}
			}, 1000,5000);
			mScaner = new BluetoothScan(getApplicationContext());
			mContext = new OznerContext(getApplicationContext());
			mManager = new OznerDeviceManager(mContext, mScaner);
			mCups = new CupManager(mContext, mManager);
			mTaps = new TapManager(mContext, mManager);
		}
	}

	public OznerBLEService() {
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		this.getApplication().registerActivityLifecycleCallbacks(this);
		if (!mScaner.isRuning())
		{
			mManager.Start();
			mScaner.Start();
		}
		//BluetoothWorkThread work=new BluetoothWorkThread(getApplicationContext());
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		this.getApplication().unregisterActivityLifecycleCallbacks(this);
		return super.onUnbind(intent);
	}

	
	
	private void checkBackMode()
	{
		boolean back=mManager.isBackground();
		if (activitys.size()>0)
			back=false;
		else
			back=true;
		if (mManager.isBackground()!=back)
		{
			mManager.setBackgroundMode(back);
			mScaner.setBackgroundMode(back);
			dbg.i("setBackgroundMode:%s",back?"yes":"no");
		}
	}
	
	@Override
	public void onActivityResumed(Activity activity) {
		if (!activitys.contains(activity))
			activitys.add(activity);
		checkBackMode();
		
	}

	@Override
	public void onActivityPaused(Activity activity) {
	}

	@Override
	public void onActivityStopped(Activity activity) {
		if (activitys.contains(activity))
			activitys.remove(activity);
		lastTime=new Date();
		
	}

	@Override
	public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
		
	}
	@Override
	public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
		
	}

	@Override
	public void onActivityStarted(Activity activity) {
	}
	@Override
	public void onActivityDestroyed(Activity activity) {
		if (activitys.contains(activity))
			activitys.remove(activity);
	}

}
