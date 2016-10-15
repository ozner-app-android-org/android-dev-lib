//
//  AylaConnectivityListner.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 12/2/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * This is a broadcast receiver that is listening broadcasts from android system, when 
 * detecting connection status changes, service/LAN, and initialize connection status 
 * scan process. 
 * 
 * */
public class AylaConnectivityListener extends BroadcastReceiver {
	private final static String LOG_TAG = AylaConnectivityListener.class.getSimpleName();
    private static Handler __debounceHandler;
    private static final int DEBOUNCE_TIME = 500; // Time for debouncing connectivity changes
	private static AylaConnectivityListener receiver;
	
	private static final String tag = "AylaConnectivityListener";
	static Intent intent = null;

    private static AylaConnectivityListener mInstance = null;

    static synchronized void registerConnectivityListener(final Context c) {
        if (c == null) {
            AylaSystemUtils.saveToLog("%s, %s, %s."
                    , "E"
                    , LOG_TAG
                    , "context null in registerConnectivityListner");
            return;
        }

        if (mInstance!=null) {
            AylaSystemUtils.saveToLog("%s, %s, %s."
                    , "W"
                    , LOG_TAG
                    , "duplicate connectivitylistener registration");
            return;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");

        mInstance = new AylaConnectivityListener();
        c.registerReceiver(mInstance, filter);
    }// end of registerConnectivityListener

    static void enableConnectivityListener(final Context c) {
        if (mInstance == null) {
            return;
        }
        setConnectivityListenerState(c, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }

    static void disableConnectivityListener(final Context c) {
        if (mInstance == null) {
            return;
        }
        setConnectivityListenerState(c, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

	private static void setConnectivityListenerState(final Context c, final int newState)
	{
		if (c == null) {
            AylaSystemUtils.saveToLog("%s, %s, %s."
                    , "W", LOG_TAG
                    , "context null, newState:" + newState + " fails");
			return;
		}
		ComponentName receiver = new ComponentName(c, AylaConnectivityListener.class);
		PackageManager pm = c.getPackageManager();
		try {
			pm.setComponentEnabledSetting(receiver, newState, PackageManager.DONT_KILL_APP);
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s, %s, error message:%s.", "D", tag, e.getMessage());
		}
	}
	
	private AylaConnectivityListener() {
		super();
	}

    private static Handler getDebounceHandler() {
        if (__debounceHandler == null) {
            __debounceHandler = new Handler(Looper.getMainLooper());
        }
        return __debounceHandler;
    }
	/** 
	 * This would be called when it gets registered, and every time a broadcast message comes in.
	 * 
	 * Reference <a href="https://developer.android.com/reference/android/content/BroadcastReceiv	\n
	 * er.html">Android Broadcast Receiver</a> for details.
	 * */  
	@Override
	public void onReceive(Context c, Intent thisIntent) {

        if (AylaNetworks.appContext == null) {
            Log.d(LOG_TAG, "Bounce!: app is NOT active");
            return;
        }
        Log.d(LOG_TAG, "Bounce!: app IS active");

        getDebounceHandler().removeCallbacksAndMessages(null);
        getDebounceHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "Debounce!");
                AylaSystemUtils.saveToLog("%s, %s, %s.", "I", tag + " onReceive() called",  "onReceive");
                AylaSystemUtils.serviceReachableTimeout = AylaNetworks.AML_SERVICE_REACHABLE_TIMEOUT; // always test if connectivity changes
                AylaReachability.setDeviceReachability(null, AylaNetworks.AML_REACHABILITY_UNKNOWN);
                AylaReachability.determineReachability(false);

                // Pause LAN mode if we're not connected to a WiFi network. Resume it otherwise.
                boolean isConnected = AylaReachability.isWiFiConnected(null);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ConnectivityManager cm = (ConnectivityManager) AylaNetworks.appContext.getSystemService(Context
                            .CONNECTIVITY_SERVICE);
                    Network[] networks = cm.getAllNetworks();
                    for (Network n : networks) {
                        NetworkInfo info = cm.getNetworkInfo(n);
                        if (info.getType() == ConnectivityManager.TYPE_WIFI &&
                                info.isConnectedOrConnecting()) {
                            isConnected = true;
                            break;
                        }
                    }
                }
                if (!isConnected) {
                    AylaLanMode.pause(false);
                    Log.d(LOG_TAG, "Debounce: WiFi is NOT connected");
                } else {
                    AylaLanMode.resume();
                    Log.d(LOG_TAG, "Debounce: WiFi IS connected");
                }
            }
        }, DEBOUNCE_TIME);
	}

}// end of AylaConnectivityListener class     



