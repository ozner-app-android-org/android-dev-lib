//
//  AylaReachability.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 11/25/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

/**
 * 
 * AylaReachability is responsible for detecting reachability to the service and/or the device in the same lan.
 * 
 * */
public class AylaReachability {

    // Timer for re-checking device reachability for devices that have found unreachable
    public static int DEFAULT_DEVICE_RECHECK_INTERVAL = 30000;

	// Only check for service connectivity this often at most
	public static int SERVICE_REACHABILITY_MAX_INTERVAL_SECONDS = 10;


    private static final String REACHABILITY_REGISTER = "reachability_register";
	
	static Handler reachabilityHandle = null;
	private static AylaRestService rsReachability = null;

	private static int connectivity = AylaNetworks.AML_REACHABILITY_UNKNOWN;
	private static int isServiceReachable = AylaNetworks.AML_REACHABILITY_UNKNOWN;

	private static AylaTimer _reachabilityCheckTimer;
	private static int _reachabilityCheckInterval = DEFAULT_DEVICE_RECHECK_INTERVAL;
    private static Runnable _reachabilityCheckRunnable;

    static {
        _reachabilityCheckRunnable = new Runnable() {
            @Override
            public void run() {
                AylaReachability.retryUnreachableDevices();
            }
        };

		setDeviceRecheckInterval(DEFAULT_DEVICE_RECHECK_INTERVAL);
	}

	/**
	 * Sets all devices that have been found as "unreachable" to "unknown". This will cause
	 * us to re-check reachability the next time a LAN mode attempt is made on any previously
	 * unreachable devices.
	 */
	public static synchronized void retryUnreachableDevices() {
		for ( String dsn : _deviceReachabilityMap.keySet() ) {
			if ( _deviceReachabilityMap.get(dsn) == AylaNetworks.AML_REACHABILITY_UNREACHABLE ) {
				_deviceReachabilityMap.put(dsn, AylaNetworks.AML_REACHABILITY_UNKNOWN);
			}
		}
	}

	/**
	 * Returns the interval, in milliseconds, we will re-check reachability on devices previously
	 * found to be unreachable.
	 *
	 * @return Time in ms between re-checks for device reachability
	 */
	public static int getDeviceRecheckInterval() {
		return _reachabilityCheckInterval;
	}

	/**
	 * Sets the time in milliseconds we will allow a device to be considered "unreachable" before
	 * checking again
	 *
	 * @param timeInMs Interval for timer that sets unreachable devices to "unknown"
	 */
	public static void setDeviceRecheckInterval(int timeInMs) {
		if ( _reachabilityCheckTimer != null ) {
			_reachabilityCheckTimer.stop();
		}
		_reachabilityCheckInterval = timeInMs;
		_reachabilityCheckTimer = new AylaTimer(getDeviceRecheckInterval(), _reachabilityCheckRunnable);
		_reachabilityCheckTimer.start();
	}

	/**
	 * Register message handlers from main activity
	 */
	public static synchronized void register(Handler handler) {
		if (handler != null) {
			reachabilityHandle = handler;
			rsReachability = new AylaRestService(reachabilityHandle, REACHABILITY_REGISTER, AylaRestService.REACHABILITY);
			
			lastServiceReachablityTest = 0L; // reset to frequent timerlastDeviceReachablityTest = 0L; // reset to frequent timer
		}
	}

	
	/**
	 * Internet reachability access methods
	 */
	public static int getConnectivity() {
		return connectivity;
	}

	public static boolean isCloudServiceAvailable() {
		return (connectivity == AylaNetworks.AML_REACHABILITY_REACHABLE);               
	}

	private static Map<String, Integer> _deviceReachabilityMap = new HashMap<>();

	/**
	 * Device reachability access methods
	 */ // TODO: has issues here. hardcode it for 0, Reachable to proceed for now. Will deprecate this API gradually. 
	public static synchronized int getDeviceReachability(AylaDevice device) {
        // First check to see if we're setting up a device. If so, we'll consider it always
        // reachable.
        AylaDevice setupDevice = AylaLanMode.getSecureSetupDevice();
        if (setupDevice != null && device != null && device.dsn.equals(setupDevice.dsn)) {
            return AylaNetworks.AML_REACHABILITY_REACHABLE;
        }

        // Normal reachability check
        Integer reachabilty = _deviceReachabilityMap.get(device.dsn);
		if ( reachabilty == null ) {
            AylaSystemUtils.saveToLog("Reachability UNKNOWN: " + device.dsn);
			return AylaNetworks.AML_REACHABILITY_UNKNOWN;
		}
        AylaSystemUtils.saveToLog("Reachability: " + reachabilty + ": " + device.dsn);
		return reachabilty;
	}
	
	public static synchronized void setDeviceReachability(final AylaDevice device, int reachable) {
		if ( device == null ) {
			// Set reachability for everything
			for ( String dsn : _deviceReachabilityMap.keySet() ) {
				_deviceReachabilityMap.put(dsn, reachable);
			}
			returnReachability();
		} else {
			// Set reachability for the specific device
			Integer oldReachability = _deviceReachabilityMap.get(device.dsn);

			_deviceReachabilityMap.put(device.dsn, reachable);

			if (oldReachability == null || oldReachability != reachable) {
				/* Disable for now, will come back in 5.0
				// If the device has become reachable, and there is a LAN mode handler set up,
				// put this device into LAN mode
				if ( reachable == AylaNetworks.AML_REACHABILITY_REACHABLE &&
						AylaNotify.notifierHandle != null &&
						AylaLanMode.isLanModeRunning() ) {
					AylaSystemUtils.saveToLog("Device " + device + " has become reachable, putting into LAN mode");

					// Put the device into LAN mode on the main thread- we don't want to deadlock here.
					new Handler(AylaNetworks.appContext.getMainLooper()).post(new Runnable() {
						@Override
						public void run() {
							AylaDevice dmDevice = AylaDeviceManager.sharedManager().deviceWithDSN(device.dsn);
							if (dmDevice != null) {
								dmDevice.lanModeEnable();
							}
						}
					});
				}
				*/

				returnReachability(); // notify main activity reachability has changed
			}
		}
	}
	
	public static boolean isDeviceLanModeAvailable(AylaDevice device) {
		if (device == null) {
			// See if anybody is reachable
			return _deviceReachabilityMap.values().contains(AylaNetworks.AML_REACHABILITY_REACHABLE);
		}
		int reachability = getDeviceReachability(device);
		if (reachability == AylaNetworks.AML_REACHABILITY_UNKNOWN) {
			// Check the device in the background
			determineDeviceReachability(device, false);
			// Don't Assume that it's reachable, assume that it is NOT reachable
			return false;
		}
		// Only return that it is reachable if it IS.
		return  (reachability == AylaNetworks.AML_REACHABILITY_REACHABLE);
	}

	/**
	 * This real-time method returns a combined reachability status. If either LME device or Ayla Device Service were reachable, this method would return reachability
	 * status AML_REACHABILITY_REACHABLE.
	 */	
	public static int getReachability() {
		if ( (isDeviceLanModeAvailable(null)) || (connectivity >= 0) ) {
			return AylaNetworks.AML_REACHABILITY_REACHABLE;
		} else  {
			return AylaNetworks.AML_REACHABILITY_UNREACHABLE;
		}
	}

	/**
	 * Called mostly by AylaConnectivityListener on network state changes.
	 * This method will takes up to 1 second to resolve and set current values connectivity & device reachability
	 * 
	 * Make it public for Unit Test Program, should not be called in app level.
	 * 
	 * @param waitForResults true to block until results are ready, false to return immediately
	 */

	// Use the form that checks a specific device. Pass null if no device reachability check is desired.
	@Deprecated
	public static synchronized void determineReachability(final boolean waitForResults) {
		determineReachability(waitForResults, null);
	}
	public static synchronized void determineReachability(final boolean waitForResults, final AylaDevice deviceToCheck) {
		boolean isConnected = false;
		
		isConnected = isConnected(null);
		if (!isConnected) {
			connectivity = AylaNetworks.AML_REACHABILITY_UNREACHABLE;
			setDeviceReachability(null, AylaNetworks.AML_REACHABILITY_UNREACHABLE);
			returnReachability();
		} else {
			// check device reachability. We need to ignore waitForResults if deviceToCheck
			// is null so we don't try every single device right here.
			determineDeviceReachability(deviceToCheck, (deviceToCheck == null) ? false : waitForResults);

			// check service reachablity
			determineServiceReachability(waitForResults);
		}
	}

	private static long lastServiceReachablityTest = 0L;// ?
	/**
	 * Determine Servicer reachability, set connectivity boolean
	 * 
	 * @param waitForResults
	 * */
	static synchronized void determineServiceReachability(final boolean waitForResults) {
		
		if (AylaSystemUtils.serviceReachableTimeout == -1) {
			connectivity = AylaNetworks.AML_REACHABILITY_UNREACHABLE;
			return;
		}
/*
		Date now = new Date();
		if ( now.getTime() - lastServiceReachablityTest < SERVICE_REACHABILITY_MAX_INTERVAL_SECONDS ) {
			AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "I", "Reachability", "service", connectivity,  "determineServiceReachability: too soon");
			return;
		}

		lastServiceReachablityTest = now.getTime();
*/
		// optimize
		//long delta = System.currentTimeMillis() - lastServiceReachablityTest;
		//AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "I", "Reachability", "delta", delta,  "determineServiceReachability");
		//if (delta <= AylaSystemUtils.serviceReachableTimeout*2) {
		//	AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "I", "Reachability", "service", connectivity,  "determineServiceReachability_optimized");
		//	return;
		//}
		
		isServiceReachable = AylaNetworks.AML_REACHABILITY_UNKNOWN;
		Thread thread = new Thread(new Runnable() {
			public void run() {
				
				try {
					String serviceURL = AylaSystemUtils.deviceServiceBaseURL();
					int lastForwardSlash = serviceURL.indexOf('/', 8);
					String hostName = serviceURL.substring(8,lastForwardSlash);
					
					InetAddress address = InetAddress.getByName(hostName); // lookup the ip address or throw UnknownHostException
					
					if (AylaSystemUtils.slowConnection == AylaNetworks.NO) {
						// Test host server is availability
						String serviceIpAddress = address.getHostAddress();
						SocketAddress sockaddr = new InetSocketAddress(serviceIpAddress, 80);
						Socket sock = new Socket();
						int timeoutMs = AylaSystemUtils.serviceReachableTimeout; // If times-out, SocketTimeoutException is thrown.
						sock.connect(sockaddr, timeoutMs);
						sock.close();
					}
					isServiceReachable = AylaNetworks.AML_REACHABILITY_REACHABLE;
				}
				catch(Exception e) {
					isServiceReachable = AylaNetworks.AML_REACHABILITY_UNREACHABLE;
				}
			}
		});
		thread.start();
		
		if (waitForResults) { // blocking manner
            try {
                thread.join(AylaNetworks.AML_SERVICE_REACHABLE_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            connectivity = isServiceReachable;
			returnReachability();
		} else {
            // Check every 100ms for reachability, and return if it changes or after the
            // total timeout period has elapsed
            // Because this uses a countdown timer, we need to run on a thread with a looper.
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    new CountDownTimer(AylaNetworks.AML_SERVICE_REACHABLE_TIMEOUT ,100) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            if ( isServiceReachable != AylaNetworks.AML_REACHABILITY_UNKNOWN ) {
                                connectivity = isServiceReachable;
                                returnReachability();
                                this.cancel();
                            }
                        }

                        @Override
                        public void onFinish() {
                            connectivity = isServiceReachable;
                            returnReachability();
                        }
                    }.start();
                }
            });
		}
		
		AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "I", "Reachability", "service", connectivity,  "determineServiceReachability");
		lastServiceReachablityTest = System.currentTimeMillis();
	}
	

	private static boolean isConnected(Context context) { 
		
		if (context == null) {
			context = AylaNetworks.appContext;
		}
		
		if (context == null) { // AylaNetworks.appContext gets unloaded by OS. 
			return false;
		}
		
	    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    
	    return (netInfo != null && netInfo.isConnected());
	}
	
	public static boolean isWiFiEnabled(Context context) { 
		if (context == null) {
			context = AylaNetworks.appContext;
		}
		
		if (context == null) { // AylaNetworks.appContext gets unloaded by OS.
			return false;
		}
		WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
	    return (wifi != null && wifi.isWifiEnabled());
	}
	
	public static boolean isWiFiConnected(Context context) {
		
		if (context == null) {
			context = AylaNetworks.appContext;
		}
		
		if (context == null) { // AylaNetworks.appContext gets unloaded by OS.
			return false;
		}
		NetworkInfo.DetailedState rcDetailedState;
		rcDetailedState = AylaHostWifiApi.NetworkState(context);
		
		return (NetworkInfo.DetailedState.CONNECTED == rcDetailedState);
	}
	

	/**
	 * Determine Device reachability, set device boolean
	 * */
	static synchronized void determineDeviceReachability(final AylaDevice aylaDevice, final boolean waitForResults) {
		if (!isWiFiConnected(null)) {
			AylaSystemUtils.saveToLog("%s, %s, %s, %s", "I", "Reachability", "No WiFi, setting all devices to unreachable", "determineDeviceReachability");
			setDeviceReachability(null, AylaNetworks.AML_REACHABILITY_UNREACHABLE);
			return;
		}

		if ( aylaDevice == null ) {
			AylaSystemUtils.saveToLog("%s, %s, %s, %s", "I", "Reachability", "null device", "determineDeviceReachability");
			return;
		}

        if ( getDeviceReachability(aylaDevice) == AylaNetworks.AML_REACHABILITY_DETERMINING ) {
            // We are already checking- don't check again!
            return;
        }

		// optimize
		//long delta = System.currentTimeMillis() - lastDeviceReachablityTest;
		//AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "I", "Reachability", "delta", delta,  "determineDeviceReachability_optimized");
		//if (delta <= AylaNetworks.AML_DEVICE_REACHABLE_TIMEOUT) {
			//AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "I", "Reachability", "device:", device,  "determineDeviceReachability_optimized");
		//	return;
		//}

        setDeviceReachability(aylaDevice, AylaNetworks.AML_REACHABILITY_DETERMINING);
		Thread thread = new Thread(new Runnable() {
			public void run() {
                int discoveredReachability = AylaNetworks.AML_REACHABILITY_UNKNOWN;
				try {
                    SocketAddress sockaddr = null;
                    sockaddr = new InetSocketAddress(aylaDevice.lanIp, 80);
                    Socket sock = new Socket();
                    int timeoutMs = AylaNetworks.AML_DEVICE_REACHABLE_TIMEOUT; // If times-out, SocketTimeoutException is thrown.
                    sock.connect(sockaddr, timeoutMs);
                    sock.close();
                    AylaSystemUtils.saveToLog("Reachability: " + aylaDevice.dsn + " is reachable.");
                    discoveredReachability = AylaNetworks.AML_REACHABILITY_REACHABLE;

				}
				catch(Exception e) {
                    AylaSystemUtils.saveToLog("Reachability: " + aylaDevice.dsn + " unreachable");
					discoveredReachability = AylaNetworks.AML_REACHABILITY_UNREACHABLE;
				}

                setDeviceReachability(aylaDevice, discoveredReachability);
			}
		});
		thread.start();
		
		if (waitForResults) {
			try {
                thread.join(AylaNetworks.AML_DEVICE_REACHABLE_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
                AylaSystemUtils.saveToLog("Reachability: " + aylaDevice.dsn + " timed out");
                setDeviceReachability(aylaDevice, AylaNetworks.AML_REACHABILITY_UNREACHABLE);
            }
			
			returnReachability();
		}
		
		AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "I", "Reachability", "device", getDeviceReachability(aylaDevice),  "determineDeviceReachability");
	}

	// return reachability status to the registered message handler
	private static String logMsg = "";
	private static int savedConnectivity = -100;
	protected static void returnReachability() {
		if (reachabilityHandle == null) {
			return;
		}

		// no need to notifiy reachability handler if no change in reachability
		if ( (savedConnectivity == connectivity)) {
			return;
		}
		
		savedConnectivity = connectivity;

		
		String jsonText = "";
		jsonText = "{";
		jsonText = jsonText + "\"device\":" + AylaNetworks.AML_REACHABILITY_UNKNOWN + ",";
		jsonText = jsonText + "\"connectivity\":" + connectivity ;
		jsonText= jsonText + "}";
		String msg = String.format("%s %s %s:%s %s", "I", "AylaReachability", "jsonText", jsonText, "returnReachability");
		if (TextUtils.equals(msg, logMsg)) {
			AylaSystemUtils.consoleMsg(logMsg, AylaSystemUtils.loggingLevel);
		} else { AylaSystemUtils.saveToLog("%s", msg); logMsg = msg; }
		
		returnToMainActivity(rsReachability, jsonText, 200, 0);
	}

	// TODO: put in a common utils class.
	private static void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId) {
		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		
		rs.execute(); 
	}
}// end of AylaReachability class     




