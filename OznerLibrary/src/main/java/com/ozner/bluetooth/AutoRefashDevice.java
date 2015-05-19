package com.ozner.bluetooth;

import java.util.Timer;
import java.util.TimerTask;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

public abstract class AutoRefashDevice extends BaseBluetoothDevice {
	Timer mUdateTimer;
	public AutoRefashDevice(Context context, BluetoothDevice device,
			BluetoothCloseCallback callback) {
		super(context, device, callback);
	}
	
	protected abstract void onAutoUpdate();
	@Override
	protected void onReadly() {
		synchronized (this) {
			if (mUdateTimer!=null) return;
			mUdateTimer=new Timer();
			mUdateTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					if (isBackground()) return;
					if (isUpdateFirmware) return;
					onAutoUpdate();
					/*else
					{
						//如果是后台模式，且处于闲置状态，关闭连接
						if (!isBusy())
						{
							close();
						}
					}*/
				}
			}, 1000,5000);
		}
		super.onReadly();
	}
	@Override
	public void close() {
		onPause();
		super.close();
	}

	@Override
	protected void onPause() {
		super.onPause();
		synchronized (this) {
			if (mUdateTimer==null) return;
			mUdateTimer.cancel();
			mUdateTimer.purge();
			mUdateTimer=null;
		}
	}
}
