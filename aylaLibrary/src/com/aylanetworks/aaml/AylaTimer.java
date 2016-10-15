//
//  AylaTimer.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 11/16/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//
package com.aylanetworks.aaml;

import android.os.Handler;
import android.os.HandlerThread;

class AylaTimer {
    private static HandlerThread __timerThread;
    static {
        __timerThread = new HandlerThread("AylaTimer Thread");
        __timerThread.start();
    }
    
	private long _interval; 
	private Handler handler; 
	private Runnable _tickHandler; 
	private Runnable delegate; 
	private boolean ticking;
	
	long getInterval() { return _interval; }
	
	boolean getIsTicking() { return ticking; } 

	AylaTimer(final long interval) { 
		this(interval, null);
	} 

	AylaTimer(final long interval, Runnable onTickHandler) { 
		_interval = interval; 
		handler = new Handler(__timerThread.getLooper());
		setOnTickHandler(onTickHandler); 
	} 

	void start(int interval, Runnable onTickHandler) { 
		if (ticking) return; 
		_interval = interval; 
		setOnTickHandler(onTickHandler); 
		handler.postDelayed(delegate, _interval); 
		ticking = true; 
	} 

	void start() { 
		if (ticking) {
			return;
		}
		handler.postDelayed(delegate, _interval); 
		ticking = true; 
	} 

	void stop() {
		handler.removeCallbacksAndMessages(null);
		ticking = false; 
	} 

	void setInterval(final long delay) {
		_interval = delay;
		stop();
		start();
	} 
	
	void setOnTickHandler(Runnable onTickHandler) { 
		if (onTickHandler == null) {
			return;
		}
		
		_tickHandler = onTickHandler; 
		delegate = new Runnable() { 
			public void run() { 
				if (_tickHandler == null) {
					return;
				}
				_tickHandler.run();
				if (ticking) {
					handler.postDelayed(delegate, _interval);
				}
			} 
		}; 
	} 
}





