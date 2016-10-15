//
//  AylaNetworks.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 10/08/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//
package com.aylanetworks.aaml;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import java.util.Map;

public class AylaSetup extends AylaHost
{
	
	private static final String tag = AylaSetup.class.getSimpleName();
	
	
	
	public static String connectedMode = AML_CONNECTION_UNKNOWN; // connection status of the new device
	private static Boolean inExit = false;		

	public static int lastMethodCompleted = AML_SETUP_TASK_NONE;	  // incremented as each Setup task is completed. Use in an application task progress indicator

	public static AylaModule newDevice = null;	// new device selected by user
	
	static boolean isApStaSupported = false;
	static boolean isRsaKeySupported = false;
	public static String newDeviceConnectionMessage = null;

	// Connect To new Device
	static int    hostOriginalNetId = AML_NETID_NOT_REMEMBERED; // phone/tablet netId it is originally connected to
	static String hostOriginalSsid = null; // phone/tablet ssid it is originally connected to
	static AylaHostNetworkConnection hostNetworkConnection = null; // connection info from host to new device
	static int    hostNewDeviceNetId = AML_NETID_NOT_REMEMBERED; // netId from host to new device
	static String hostNewDeviceSsid = null; // ssid of Ayla new device in AP mode selected by user
	static String hostNewDevicePassword = null; // null for Ayla AP mode
	static String hostNewDeviceSecurityType = AML_OPEN; // OPEN for Ayla new device in AP mode
	static String hostNewDeviceLanIp = GBL_MODULE_DEFAULT_WIFI_IPADDR; // 192.168.0.1 default ip addr in AP mode
	// static String newDeviceDsn = null; // dsn

	// Connect New Device To Service
	static String setupToken = null; // setup token sent to new device and service for secure association
	public static String lanSsid = null;    // WLAN AP with internet access ssid
	public static String lanPassword = null; // WLAN AP with internet access pswd
	public static String lanSecurityType = null; // WLAN AP with internet access security type

	// Confirm Device To Service Connection
	public static String lanIp = null;	// WLAN IP address of new device connected to device service
	static String registrationType = null;
	public static int newDeviceToServiceConnectionRetries = 0;	// number of retries to confirm successful setup. Use in an application progress indicator.
	
	public static Boolean haveSetupToken() {
		return setupToken != null;
	}
	
	public static Boolean inExit() {
		return inExit;
	}
	
	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");

		result.append(this.getClass().getName() + " Object {" + NEW_LINE);
		result.append(" connectedMode: " + connectedMode + NEW_LINE);
		result.append(" inExit: " + inExit + NEW_LINE);
		result.append(" lastTaskCompleted: " + lastMethodCompleted + NEW_LINE);
		result.append(" newDevice: " + newDevice + NEW_LINE);
		result.append(" hostOriginalNetId: " + hostOriginalNetId + NEW_LINE);
		result.append(" hostNewDeviceSsid: " + hostNewDeviceSsid + NEW_LINE);
		result.append(" hostNewDevicePassword: " + hostNewDevicePassword + NEW_LINE);
		result.append(" hostNewDeviceSecurityType: " + hostNewDeviceSecurityType + NEW_LINE);
		result.append(" hostNewDeviceLanIp: " + hostNewDeviceLanIp + NEW_LINE);
		result.append(" setupToken: " + setupToken + NEW_LINE);
		result.append(" lanSsid: " + lanSsid + NEW_LINE);
		result.append(" lanPassword: " + lanPassword + NEW_LINE);
		result.append(" lanIp: " + lanIp + NEW_LINE);
		result.append(" lanSecuirtyType: " + lanSecurityType + NEW_LINE);
		result.append(" newDeviceToServiceConnectionRetries: " + newDeviceToServiceConnectionRetries + NEW_LINE);
		result.append("}");
		return result.toString();
	}

// ------------------------------------ Initialization ------------------------------------
	/**
	 * Initializes all Setup variables
	 * Must be called before starting Setup Tasks HOWEVER...
	 * It is automatically called by the first setup call returnHostScanForNewDevices() so should be no need to explicitly call
	 */
	static void init(boolean async) {
		saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "entry", "OK", "init()");
		inExit = false;

		AylaLanMode.pause(false);

		newDevice = new AylaModule();
		isApStaSupported = false;
		isRsaKeySupported = false;
		
		newDeviceConnectionMessage = null;

		connectedMode = AML_CONNECTION_UNKNOWN;

		// Connect To New Device
		hostOriginalNetId = AML_NETID_NOT_REMEMBERED; 
		hostOriginalSsid = null;
		hostNetworkConnection = null;
		hostNewDeviceNetId = AML_NETID_NOT_REMEMBERED;
		hostNewDeviceSsid = null;
		hostNewDevicePassword = null;
		hostNewDeviceSecurityType = AML_OPEN;
		hostNewDeviceLanIp = GBL_MODULE_DEFAULT_WIFI_IPADDR;

		// Connect New Device To Service
		setupToken = null;
		lanSsid = null;
		lanPassword = null;
		lanSecurityType = null;

		// Confirm Device To Service Connection
		lanIp = null;
		newDeviceToServiceConnectionRetries = 0;

		deleteHostNetworkConnections(async); // delete new device wifi profiles
		
		lastMethodCompleted = AML_SETUP_TASK_INIT;
	}

// ------------------------------------ Begin Setup Task calls ------------------------------------
	
//	public static boolean isRsaKeySupported() {
//		return isRsaKeySupported;
//	}
	
	/**
	 * This compound static method returns an array of new Ayla enabled devices in AP mode via a host local WiFi scan. The results are 
	 * those APs matching the regular expression gblAmlDeviceSsidRegex, which must be declared as a String in the main activity and 
	 * instantiated via a single instance of AylaNetworks().
	 * @param mHandle is where result would be returned.
	 * @return AylaRestService object
	 */
	static public AylaRestService returnHostScanForNewDevices(Handler mHandle) {
		AylaSetup.init(mHandle == null); // Always the first call for setup
		return AylaHost.returnHostScanForNewDevices(mHandle); // pass through
	}

	
	/**
	 * Exit secure setup session. 
	 * */
	//TODO: if no apps use this, needs to hide inside lib, and use AylaSetup.exit() instead
	public static void exitSecureSetupSession() {
		saveToLog("%s, %s.", "D", "AylaSetup.exitSecureSetupSession().");

		AylaLanMode.closeSecureSetup();

		// Restore the original LAN mode and reachability handlers that we stole earlier
		AylaHost.restoreOriginalHandlersIfNeeded();

		isRsaKeySupported = false;

		if (AylaSetup.lastMethodCompleted >= AML_SETUP_TASK_RETURN_HOST_SCAN_FOR_NEW_DEVICES) {
			deleteHostNetworkConnections(false);
		}

		if (AylaSetup.lastMethodCompleted >= AML_SETUP_TASK_GET_DEVICE_SCAN_FOR_APS) {
			// regenerate key pair for secure session everytime a session gets shut down,
			// to make sure key pair is session wise.
			Thread keyRefresh = new Thread(new Runnable(){
				@Override
				public void run() {
					AylaEncryptionHelper.getInstance().refreshKeyPair();
				}
			});
			keyRefresh.start();
		}

		AylaSetup.lastMethodCompleted = AML_SETUP_TASK_NONE;

        // Undo any process binding to networks we did during setup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager)AylaNetworks.appContext.getSystemService(Context
                    .CONNECTIVITY_SERVICE);
            connectivityManager.bindProcessToNetwork(null);
        }

        // Kick LAN mode- we've been mucking with the webserver
        AylaLanMode.pause(false);
        AylaLanMode.resume();
    }// end of exitSecureSetupSession
	
	
	/**
	 * This compound static method creates an HTTP connection to the new device selected by the customer and returns detailed information about it.
	 * @param mHandle is where result would be returned.
	 * @return AylaRestService object
	 */
	static public AylaRestService connectToNewDevice(Handler mHandle) {
		// Init a new setup
//		AylaSetup.init(false);
		return AylaHost.connectToNewDevice(mHandle); // pass through
	}

	/**
	 * This is a compound static method that returns an array of WLAN APs via a remote device WiFi scan. 
	 * @param mHandle is where result would be returned.
	 * @return AylaRestService object
	 */
	public static AylaRestService getNewDeviceScanForAPs(Handler mHandle) {
		return AylaModule.getNewDeviceScanForAPs(mHandle, false); // pass through
	}

	/**
	 * This compound static method directs the selected new device to connect to the Ayla Device Service via the customer selected WLAN AP. 
	 * @param mHandle is where result would be returned.
	 * @return AylaRestService object
	 */
	public static AylaRestService connectNewDeviceToService(final Handler mHandle) {
		return AylaModule.connectNewDeviceToService(mHandle, null); // pass through
	}
	public static AylaRestService connectNewDeviceToService(final Handler mHandle, Map<String, Object> callParams) {
		return AylaModule.connectNewDeviceToService(mHandle, callParams); // pass through
	}
	
	/**
	 * This compound static method confirms that the new device has successfully connected to the Ayla Device Service. It does this by repeatedly 
	 * checking with the Ayla Device Service and can take many tens of seconds to complete. By monitoring newDeviceToServiceConnectionRetries a progress 
	 * indicator can be provided for customer feedback and assurance while this method completes.
	 * @param mHandle is where result would be returned.
	 * @return AylaRestService object
	 */
	public static AylaRestService confirmNewDeviceToServiceConnection(Handler mHandle) {
		return AylaModule.confirmNewDeviceToServiceConnection(mHandle); // pass through
	}

	/**
	 * this method returns an array of past connection attempts from the new device. These are then used to determine and correct the issue. Method only 
	 * works when android device is connected to module.
	 * @param mHandle is where result would be returned.
	 * @return AylaRestService object
	 */
	public static AylaRestService getNewDeviceWiFiStatus(Handler mHandle) {
		return AylaModule.getNewDeviceWifiStatus(mHandle); // pass through
	}

    /**
     * This method is used to Shut down AP Mode on the module. This method is typically not
     * used by the Client Apps as the AP Mode will be shut down automatically in 30 seconds
     * when the device is connected to service. Method only works when android device is
     * connected to module. On success, an HTTP status of “204 No Content” is returned.
     * Otherwise, an HTTP status of “403 Forbidden” is returned. AP mode disconnect command is
     * only accepted when module is still in AP mode, and successfully connected to
     * service in STA mode (i.e STA mode and AP mode are active at the same time). This command
     * turns off the AP mode, leaving STA mode active only.
     * @param mHandle is where result would be returned.
     * @return AylaRestService object
     */
    public static AylaRestService disconnectAPMode(Handler mHandle) {
        return AylaModule.disconnectAPMode(mHandle,AylaSetup.lanIp); // pass through
    }

// ------------------------------------ Begin Exit ------------------------------------
	/**
	 * If the Setup Task is abandon before successful completion, the application should call exit(). This method attempts to remove the host-to-device 
	 * connection & tries to reestablish the customer"s original WiFi connection. This is a best effort attempt to leave the host environment in its 
	 * original state. There is no need to call exit() ifsetup completes successfully.
	 */
	static public void exit() {
		if (inExit) {
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "entry", "reEntry", "exit()-------------------------");	
			return; // already executing
		}
		if (!AylaReachability.isWiFiEnabled(null)) {
			saveToLog("%s, %s, %s:%s, %s", "W", "AylaSetup", "wifi", "disabled", "exit()-------------------------");	
			return; // wifi is not enabled
		}
		saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "entry", "OK", "exit()");

//		newDevice = null;
		AylaSetup.exitSecureSetupSession();
	}

	//------------------------------------------ Delete Host Network Connections -------------------------
	// remove remembered connection to the new device in AP mode
	private static void deleteHostNetworkConnections(boolean async) {	
		saveToLog("%s, %s, %s", "I", "AylaSetup", "exit:deleteHostNetworkConnections");
		if (async) {
			AylaHost.deleteHostNetworkConnections(deleteHostNetworkConnections, AylaNetworks.deviceSsidRegex);
		} else {
			AylaRestService aylaRestService = AylaHost.deleteHostNetworkConnections(AylaNetworks.deviceSsidRegex);
			Message callResponse = aylaRestService.execute();
			doDeleteHostNetworkConnections(callResponse, false);
		}
	}	

	private final static Handler deleteHostNetworkConnections = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			doDeleteHostNetworkConnections(msg, true);
		}
	};

	static void doDeleteHostNetworkConnections(Message msg, boolean async) {
		String jsonResults = (String)msg.obj; // overload AylaHostNetworkConnections: netId == number of remembered networks removed
		if (msg.what == AylaNetworks.AML_ERROR_OK) {
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "profilesRemoved", jsonResults, "exit:deleteHostNetworkConnections_handler");
		} else {
			int responseCode = (int)msg.arg1;
			saveToLog("%s, %s, %s:%d, %s, %s", "E", "AylaSetup", "error", responseCode, msg.obj,
					"exit:deleteHostNetworkConnections_handler");
		}

		checkHostNetworkConnection(async); // check for reconnect to WLAN AP
	}

	// --------------------------------- Check Host Network Connection -----------------------
	private static void checkHostNetworkConnection(boolean async) {
		if (AylaSetup.hostOriginalNetId == AML_NETID_NOT_REMEMBERED) {
			inExit = false;
			return;
		}

		if (async) {
			AylaHost.returnHostNetworkConnection(returnHostNetworkConnection); // get the currently connected wifi network info
		} else {
			AylaRestService aylaRestService = AylaHost.returnHostNetworkConnection(); // get the currently connected wifi network info, sync
			Message callResponse = aylaRestService.execute();
			doReturnHostNetworkConnection(callResponse, false);
		}
	}

	private final static Handler returnHostNetworkConnection = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			doReturnHostNetworkConnection(msg, true);
		}
	};
	
	static void doReturnHostNetworkConnection(Message msg, boolean async) {
		String jsonResults = (String)msg.obj;
		saveToLog("%s, %s, %s, %s, %s.", "D", tag, "doReturnHostNetworkConnection"
			, "msg.arg1:" + msg.arg1
			, "msg.obj:" + msg.obj);
		if (msg.what == AylaNetworks.AML_ERROR_OK) {
			AylaHostNetworkConnection currentConnection = AylaSystemUtils.gson.fromJson(jsonResults,  AylaHostNetworkConnection.class);

			String ssid = currentConnection.ssid;
			int netId = currentConnection.netId;

			if (netId != AylaSetup.hostOriginalNetId) {
				saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaSetup", "netId", netId, "ssid", ssid, "checkHostNetworkConnection");
				setHostNetworkConnection(async); // reconnect to original network
			} else {
				inExit = false;
				return;
			}
		} else {
			inExit = false;
			return;
		}
	}

	// --------------------------------- Set Host Network Connection -----------------------
	// reconnect to original wlan 
	private static void setHostNetworkConnection(boolean async) {
		if (AylaSetup.hostOriginalNetId == AML_NETID_NOT_REMEMBERED) {
			inExit = false;
			return;
		}

		int netId = AylaSetup.hostOriginalNetId; // reconnect
		String ssid = "";
		String SecurityType = "";
		String password = "";
		int retries = AylaSystemUtils.wifiRetries/2;
		saveToLog("%s, %s, %s:%s, %s:%d, %s", "I", "AylaSetup", "host original ssid", AylaSetup.hostOriginalSsid, "host original netId", netId, "exit:setHostNetworkConnection");

		if (async) {
			AylaHost.setHostNetworkConnection(setHostNetworkReconnect, netId, ssid, SecurityType, password, retries);
		} else {
			AylaRestService rs = AylaHost.setHostNetworkConnection(netId, ssid, SecurityType, password, retries);
			Message callResponse = rs.execute();
			doSetHostNetworkConnection(callResponse, false);
		}
	}

	private static final Handler setHostNetworkReconnect= new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			doSetHostNetworkConnection(msg, true);
		}
	};
	
	static void doSetHostNetworkConnection(Message msg, boolean async) {
		String jsonResults = (String)msg.obj;
		int responseCode = (int)msg.arg1;
		saveToLog("%s, %s, %s, %s, %s.", "D", tag, "doSetHostNetworkConnection"
			, "msg.arg1:" + msg.arg1
			, "msg.obj:" + jsonResults);
		if (msg.what == AylaNetworks.AML_ERROR_OK) {
			try {
                // We've changed networks. Check for cloud connectivity again.
                AylaReachability.determineReachability(true);

				// Enable LAN mode again
				AylaLanMode.resume();
                saveToLog("I, AylaSetup, doSetHostNetworkConnection: Reachability determined. Cloud reachable? " + AylaReachability.isCloudServiceAvailable());

				AylaHostNetworkConnection networkConnection = AylaSystemUtils.gson.fromJson(jsonResults,  AylaHostNetworkConnection.class);
				saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "netId", networkConnection.netId, "exit:setHostNetworkReconnect_handler");
			} catch (Exception e) { // v3.00c
				String cause = (String) ((e.getCause() == null) ? "Unknown parse error" : e.getCause());
				saveToLog("%s, %s, %s:%s, %s:%s, %s", "E", "AylaSetup", "parseError", cause, "msg.obj", msg.obj, "exit:setHostNetworkReconnect_handler");
			}
		} else {
			saveToLog("%s, %s, %s:%d, %s, %s", "E", "AylaSetup", "error", responseCode, msg.obj, "exit:setHostNetworkReconnect_handler");
		}
		inExit = false;
	}		

// ------------------------------------ End Exit ------------------------------------
	
	/** save new device connected info to flash
	 * After a successful new device setup, the registration type and dsn are saved to flash.
	 *  This information can be used by the application to facilitate the registration process.
	 * @param value
	 * @return
	 */
	static int save(String value) { 
		AylaCache.save(AML_CACHE_SETUP, value);
		return AylaNetworks.SUCCESS;
	}
	
	/**
	 * If setup completes successfully, this new device would be buffered by library. This method is used to load that stored device. Returned AylaDevice object 
	 * can only be used to do registration, please check method registerNewDevice in AylaDevice class.
	 */
	public static String get() {
		return AylaCache.get(AML_CACHE_SETUP);	// return cached new device connected info
	}
	
	/**
	 * After a new device successfully completes registration, the application should remove the information stored by the save() method.
	 */
	public static void clear() {
		AylaCache.clear(AML_CACHE_SETUP);
	}
}






