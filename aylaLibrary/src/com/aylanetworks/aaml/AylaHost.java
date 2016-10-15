//
//  AylaHost.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 10/08/12
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import java.net.URLDecoder;

import org.json.JSONObject;

import com.aylanetworks.aaml.AylaNetworks.lanMode;
import com.aylanetworks.aaml.enums.IAML_SECURITY_KEY_SIZE;
import com.google.gson.annotations.Expose;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;

class AylaHostScanResultsContainer {
	@Expose
	AylaHostScanResults scanResults[];
}

public class AylaHost extends AylaSystemUtils
{
	
	private static final String tag = AylaHost.class.getSimpleName();
	
	
	public static final String FEATURE_AP_STA = "ap-sta";
	public static final String FEATURE_WPS = "wps";
	public static final String FEATURE_RSA_KE = "rsa-ke";
	
	
	// connectToNewDevice params and values
	static AylaRestService rsConnectToNewDevice;
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");
		result.append(this.getClass().getName() + " Object {" + NEW_LINE);
		result.append("}");
		return result.toString();
	}

// ---------------------------------- Return Host WiFi State -----------------------------------
	/**
	 * Same as {@link AylaHost#returnHostWiFiState(Handler)} with no handler to return results.
	 * **/
	public static AylaRestService returnHostWiFiState() {
		return returnHostWiFiState(null);
	}

	/**
	 * Returns the current wifi state of the host
	 * States: WIFI_STATE_DISABLED, WIFI_STATE_DISABLING, WIFI_STATE_ENABLED, WIFI_STATE_ENABLING, WIFI_STATE_UNKNOWN
	 * @param mHandle is where result would be returned.
	 * @return AylaRestService object
	 */
	public static AylaRestService returnHostWiFiState(Handler mHandle)
	{
		final AylaRestService rs = new AylaRestService(mHandle, "retunHostWifiState", AylaRestService.RETURN_HOST_WIFI_STATE);
		saveToLog("%s, %s, %s", "I", "AylaHost", "returnHostWiFiState");

		final AylaHostWifiApi wiFiHostApi = new AylaHostWifiApi();
		final String action = AylaHostWifiApi.ACTION_WIFI_STATE;
		final JSONObject args = null;
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					WifiApiResult wifiApiResult = wiFiHostApi.execute(action, args, AylaNetworks.appContext);
					int status = wifiApiToAmlError(wifiApiResult.getStatus());
					String stateMessage = wifiApiResult.getMessage();
					saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaHost", "status", status, "stateMessage", stateMessage, "returnHostWiFiState");

					returnToMainActivity(rs, stateMessage, status, 0);

				} catch (Exception e) {
					saveToLog("%s, %s, %s:%s, %s", "E", "AylaHost", "status", AML_ERROR_FAIL, "returnHostWiFiState");

					returnToMainActivity(rs, e.getMessage(), AML_JSON_PARSE_ERROR, 0);
				}
			}
		});
		thread.start();
		return rs;
	}

// --------------------------------- Begin Host Scan For New Devices ---------------------------
		/**
		 * Performs a WiFi scan on the host (phone/tablet/etc) and filters out Access Points that are not Ayla devices.
		 * @param mHandle is where result would be returned.
		 * @return AylaRestService object
		 * Asynchronously returns AylaScanResults[] as a JSON string to the message handler for mHandle.
		 * Immediately returns AylaRestService instance.
		 */
		static AylaRestService returnHostScanForNewDevices(Handler mHandle)
		{
//			AylaSetup.init(mHandle == null); // Always the first call for setup
            final AylaRestService rs = new AylaRestService(mHandle, "returnHostScan", AylaRestService.RETURN_HOST_SCAN);

            //check if location service is enabled for Android M and Higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean isEnabled=isLocationServiceEnabled();
                if (!isEnabled) {
                    saveToLog("%s, %s, %s", "E", "AylaSetup","Location Service is disabled in the Android device");
                    returnToMainActivity(rs, "Please enable Location Service in the Android device", AML_ERROR_FAIL, AML_ERROR_LOCATION_SERVICE_DISABLED);
                    return null;
                }
            }
            saveToLog("%s, %s, %s", "I", "AylaSetup", "AylaHost.returnHostScanForNewDevices");
			final AylaHostWifiApi wiFiHostApi = new AylaHostWifiApi();
			final String action = AylaHostWifiApi.ACTION_SCAN_RESULTS;
			final JSONObject args = null;
			Thread thread = new Thread(new Runnable() {
				public void run() {
					try {
						String jsonResults = "";
						WifiApiResult wifiApiResult = wiFiHostApi.execute(action, args, AylaNetworks.appContext);
						if (wifiApiResult.getStatus() == WifiApiResult.Status.WIFI_STATE_ERROR.ordinal()) {
							int status = wifiApiToAmlError(wifiApiResult.getStatus());
							String errorMessage = wifiApiResult.getMessage();
							saveToLog("%s, %s, %s:%s, %s:%s, %s", "E", "AylaSetup", "status", status, "errorMessage", errorMessage, "AylaHost.returnHostScanForNewDevices");
							returnToMainActivity(rs, errorMessage, status, 0);
						} else {
							String jsonDecoded = URLDecoder.decode(wifiApiResult.getMessage(), "UTF-8"); //revert URL encoding
							jsonResults = stripScanContainerAndReturnModules(jsonDecoded);

							int status = wifiApiToAmlError(wifiApiResult.getStatus());
							saveToLog("%s, %s, %s:%s, %s%s, %s", "I", "AylaSetup", "status", status, "jsonResults", jsonResults, "AylaHost.returnHostScanForNewDevices");
							returnToMainActivity(rs, jsonResults, status, 0);
							AylaSetup.lastMethodCompleted = AML_SETUP_TASK_RETURN_HOST_SCAN_FOR_NEW_DEVICES;
						}
					} catch (Exception e) {
						saveToLog("%s, %s, %s:%s %s", "E", "AylaSetup", "status", AML_JSON_PARSE_ERROR, "AylaHost.returnHostScanForNewDevices");
						returnToMainActivity(rs, e.getMessage(), AML_JSON_PARSE_ERROR, 0);
					}
				}
			});
			thread.start();
			return rs;
		}

        static public boolean isLocationServiceEnabled() {
            int locationMode = 0;
            try {
                locationMode = Settings.Secure.getInt(AylaNetworks.appContext.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                saveToLog("%s, %s, %s:%s, %s", "E", "AylaSetup", "error", e.getMessage(), "AylaHost.isLocationServiceEnabled");
                return false;
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }

		static private String stripScanContainerAndReturnModules(String jsonScanResultsContainer)  {
			int count = 0;
			String jsonScanResults = "";
			try {
				AylaHostScanResultsContainer scanResultsContainer = AylaSystemUtils.gson.fromJson(jsonScanResultsContainer,AylaHostScanResultsContainer.class);
				AylaHostScanResults[] scanResults = new AylaHostScanResults[scanResultsContainer.scanResults.length];

				// Only interested in Ayla Devices
				int numberOfModules = 0;
				for (AylaHostScanResults scanResult : scanResultsContainer.scanResults) {
					if (scanResult.ssid.matches(AylaNetworks.deviceSsidRegex)) {
						numberOfModules++;
					}
					scanResults[count++] = scanResult;
				}
				AylaHostScanResults[] filteredScanResults = returnModulesFromScan(scanResults, numberOfModules);

				// return json string
				jsonScanResults = AylaSystemUtils.gson.toJson(filteredScanResults,AylaHostScanResults[].class);
				AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%d, %s", "I", "AylaSetup", "count", count, "numberOfModules", numberOfModules, "AylaHost.stripScanContanier");
				return jsonScanResults;
			} catch (Exception e) {
				AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%s, %s", "E", "AylaSetup", "count", count, "jsonScanResultsContainer", jsonScanResults, "AylaHost.stripScanContanier");
				e.printStackTrace();
				return null;
			}
		}

		private static AylaHostScanResults[] returnModulesFromScan (AylaHostScanResults[] scanResults, int numberOfModules) {
			AylaHostScanResults[] filteredScanResults = new AylaHostScanResults[numberOfModules]; // max size possible

			for(int i= 0,j = 0; i < scanResults.length; i++) {
				String ssid = scanResults[i].ssid;
				if (ssid.matches(AylaNetworks.deviceSsidRegex)) {
					filteredScanResults[j++] = scanResults[i];
				}
			}
			return filteredScanResults;
		}
//----------------------------------- End return Host Scan For Modules -------------------------

// ------------------------------- Begin Connect To New Device -----------------------------------
	static AylaRestService connectToNewDevice(Handler mHandle) {
		String url = ""; // not used in main compound call
		rsConnectToNewDevice = new AylaRestService(mHandle, url, AylaRestService.CONNECT_TO_NEW_DEVICE);

		String ssid =  AylaSetup.newDevice.hostScanResults.ssid;
		String securityType = AylaNetworks.AML_OPEN; // default for module api v1
		String password = null; // default for module api v1

		// validate params
		if (ssid.matches(AylaNetworks.deviceSsidRegex) == false ) {
			saveToLog("%s, %s, %s:%s, %s", "E", "AylaSetup", "ssid", ssid, "AylaHost.connectToNewDevice - invalid new device ssid");
			returnToMainActivity(rsConnectToNewDevice, "invalid new device ssid", AML_ERROR_FAIL, AML_CONNECT_HOST_TO_NEW_DEVICE);
			return null;
		}

		// save input params for later task sub-calls
		AylaSetup.hostNewDeviceSsid =ssid;
		AylaSetup.hostNewDeviceSecurityType = securityType;
		AylaSetup.hostNewDevicePassword = password;
		saveToLog("%s, %s, %s:%s, %s:%s,%s", "I", "AylaSetup", "ssid", ssid, "securityType", securityType, "AylaHost.connectToNewDevice");

		returnHostNetworkConnection(returnHostNetworkConnection);
		return null;
	}

	// ---------------------------- Return Host Network Connection -------------------------
	static AylaRestService returnHostNetworkConnection() {
		return returnHostNetworkConnection(null);
	}
	static AylaRestService returnHostNetworkConnection(Handler returnHostNetworkConnection)
	{
		final AylaRestService rs = new AylaRestService(returnHostNetworkConnection, "returnHostNetworkConnection", AylaRestService.RETURN_HOST_NETWORK_CONNECTION);
		saveToLog("%s, %s, %s", "I", "AylaSetup", "AylaHost.returnHostCurrentConnection");

		final AylaHostWifiApi wiFiHostApi = new AylaHostWifiApi();
		final String action = AylaHostWifiApi.ACTION_CURRENT_CONNECTION;
		final JSONObject args = null;
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					WifiApiResult wifiApiResult = wiFiHostApi.execute(action, args, AylaNetworks.appContext);
					int status = wifiApiToAmlError(wifiApiResult.getStatus());
					String jsonCurrentConnection = URLDecoder.decode(wifiApiResult.getMessage(), "UTF-8"); //revert URL encoding
					saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "jsonCurrentConnection", jsonCurrentConnection, "AylaHost.returnHostCurrentConnection");
					returnToMainActivity(rs, jsonCurrentConnection, status, 0);
				} catch (Exception e) {
					saveToLog("%s, %s, %s:%s, %s:%s, %s", "E", "AylaSetup", "status", AML_ERROR_FAIL, "error", e.getMessage(), "AylaHost.returnHostCurrentConnection");
					returnToMainActivity(rs, e.getMessage(), AML_ERROR_FAIL, 0);
				}
			}
		});
		thread.start();
		return rs;
	}
	
	private final static Handler returnHostNetworkConnection = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			String jsonResults = (String)msg.obj;
			saveToLog("%s, %s, %s, %s, %s.", "D", tag, "returnHostNetworkConnection"
					, "msg.arg1:" + msg.arg1
					, "msg.obj:" + msg.obj);
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
				AylaHostNetworkConnection currentConnection = AylaSystemUtils.gson.fromJson(jsonResults,  AylaHostNetworkConnection.class);
				
				// Don't save hostOriginal info if connected to a new device
				if (currentConnection.ssid.matches(AylaNetworks.deviceSsidRegex)) { 
					saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaSetup", "currentConnection.ssid", currentConnection.ssid,
							  "wlan connected to module", "yes",
							  "AylaHost.connectNewDeviceToService_handler");
					// delete too
					currentConnection.ssid = null;
					currentConnection.netId = AML_NETID_NOT_REMEMBERED;
				}

				String ssid = currentConnection.ssid;
				AylaSetup.hostOriginalSsid = currentConnection.ssid;
				int netId = currentConnection.netId;
				AylaSetup.hostOriginalNetId = currentConnection.netId;
				

				String statusMsg = null;
				statusMsg = String.format("%s, %s, %s:%s, %s:%s, %s", "I", "AylaSetup", "netId", netId, "ssid", ssid, "AylaHost.returnHostNetworkConnection_handler");
				saveToLog(statusMsg);
					
				// initiate next task
				int newNetId = -1; // add to host configured list
				String newSsid =  AylaSetup.newDevice.hostScanResults.ssid;// ssid selected by user
				String newSecurityType = AylaSetup.hostNewDeviceSecurityType; // OPEN by default
				String newPassword = AylaSetup.hostNewDevicePassword; // null/OPEN by default
				int retries = AylaSystemUtils.wifiRetries;
				AylaHost.setHostNetworkConnection(setHostNetworkConnection, newNetId, newSsid, newSecurityType, newPassword, retries);
			} else {
				if (AylaSetup.isRsaKeySupported && AylaLanMode.hasSecureSetupDevice() && AylaLanMode.lanModeState == lanMode.RUNNING) {
					AylaSetup.exitSecureSetupSession();
				}
				saveToLog("%s, %s, %s:%d, %s, %s", "E", "AylaSetup", "error", msg.arg1, msg.obj, "AylaHost.returnHostNetworkConnection_handler");
				returnToMainActivity(rsConnectToNewDevice, jsonResults, msg.arg1, AML_RETURN_HOST_NETWORK_CONNECTION);
			}
		}		
	};

	// --------------------------------- Set Host Network Connection ----------------------------
	/**
	 * 
	 * @param mHandle
	 * @return
	 */
	static AylaRestService setHostNetworkConnection(final int netId, final String ssid, final String securityType, final String password, final int retries) {
		return setHostNetworkConnection(null, netId, ssid, securityType, password, retries);
	}
	static AylaRestService setHostNetworkConnection(Handler mHandle, final int netId, final String ssid, final String securityType, final String password, final int retries)
	{
		final AylaRestService rs = new AylaRestService(mHandle, "setHostNetworkConnection", AylaRestService.SET_HOST_NETWORK_CONNECTION);
		saveToLog("%s, %s, %s", "I", "AylaSetup", "AylaHost.setHostNetworkConnection");


		final AylaHostWifiApi wiFiHostApi = new AylaHostWifiApi();
		final String action = AylaHostWifiApi.ACTION_NETWORK_CONNECT;

		// if not connected to WLAN 
		if ( (netId < 0) && 
			 ( TextUtils.isEmpty(ssid) ) ) 
		{
			String jsonSetConnection = "{\"net_id\":-1}";
			int status = AML_ERROR_ASYNC_OK;
			
			if (AylaSetup.isRsaKeySupported && AylaLanMode.hasSecureSetupDevice() &&AylaLanMode.lanModeState == lanMode.RUNNING) {
				AylaSetup.exitSecureSetupSession();
			}
			
			saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaSetup", "host WLAN", "not connected", "jsonCurrentConnection", jsonSetConnection, "AylaHost.setHostCurrentConnection");
			returnToMainActivity(rs, jsonSetConnection, status, 0);
			return rs;
		}

		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					JSONObject args = new JSONObject();
					args.put("net_id", netId);
					args.put("ssid", ssid);
					args.put("security_type", securityType);
					args.put("password", password);
					args.put("retries", retries);
					sleep(1000); // v2.19a_ENG
					WifiApiResult wifiApiResult = wiFiHostApi.execute(action, args, AylaNetworks.appContext);
					if ( (wifiApiResult.getMessage().contains("-2"))  // DHCP failed
						  && (ssid.matches(deviceSsidRegex)) )// on a new device
					{
						saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "eMsg", "{netID:-2}", "AylaHost.setHostCurrentConnection.retry");
						sleep(1000);
						wifiApiResult = wiFiHostApi.execute(action, args, AylaNetworks.appContext); // retry
						if ( (wifiApiResult.getMessage().contains("-2"))  // DHCP failed again 
							  && (ssid.matches(deviceSsidRegex)) ) // on a new device
						{
							if (AylaSetup.newDevice.dsn != null) {
								sendToLogService(AylaSetup.newDevice.dsn, wifiApiResult.getMessage(), "error", "AylaSetup.AylaHost.setHostCurrentConnection.retry", null, true); // delay execution until we have service connectivity
							}
						}
					}
					int status = wifiApiToAmlError(wifiApiResult.getStatus());
					String jsonSetConnection = URLDecoder.decode(wifiApiResult.getMessage(), "UTF-8"); //revert URL encoding
//					if ( AylaSetup.isRsaKeySupported && AylaLanMode.hasSecureSetupDevice() && AylaLanMode.lanModeState == lanMode.RUNNING ) {
//						AylaSetup.exitSecureSetupSession();  
//					}
					saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "jsonCurrentConnection", jsonSetConnection, "AylaHost.setHostCurrentConnection");
					returnToMainActivity(rs, jsonSetConnection, status, 0);
				} catch (Exception e) {
//					if ( AylaSetup.isRsaKeySupported && AylaLanMode.hasSecureSetupDevice() &&AylaLanMode.lanModeState == lanMode.RUNNING ) {
//						AylaSetup.exitSecureSetupSession();
//					}
					saveToLog("%s, %s, %s:%s, %s:%s, %s", "E", "AylaSetup", "status", AML_ERROR_FAIL, "error", e.getMessage(), "AylaHost.setHostCurrentConnection");
					returnToMainActivity(rsConnectToNewDevice, e.getMessage(), AML_ERROR_FAIL,AML_SET_HOST_NETWORK_CONNECTION );
				}
			}
		});
		thread.start();
		return rs;
	}

	private final static Handler setHostNetworkConnection = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			String jsonResults = (String)msg.obj;
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
				AylaHostNetworkConnection networkConnection = AylaSystemUtils.gson.fromJson(jsonResults,  AylaHostNetworkConnection.class);
				if (networkConnection.netId >= 0) {
					AylaSetup.hostNetworkConnection = networkConnection;
					AylaSetup.connectedMode = AML_CONNECTED_TO_HOST;
					AylaSetup.hostNewDeviceNetId = networkConnection.netId;
					saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "netId", networkConnection.netId, "AylaHost.setHostNetworkConnection_handler");
				
					String lanIp = AylaSetup.hostNewDeviceLanIp; 
					Boolean delayExecution = false;
					Long newTime = System.currentTimeMillis()/1000;
					
					AylaSetup.newDevice.setNewDeviceTime(setNewDeviceTime, lanIp, newTime, delayExecution);
					         
				} else {
					saveToLog("%s, %s, %s:%d, %s, %s", "E", "AylaSetup", "error", AML_WIFI_ERROR, msg.obj, "AylaHost.setHostNetworkConnection_handler");
					returnToMainActivity(rsConnectToNewDevice, jsonResults, AML_WIFI_ERROR, AML_SET_HOST_NETWORK_CONNECTION2);
				}
			} else {
				saveToLog("%s, %s, %s:%d, %s, %s", "E", "AylaSetup", "error", msg.arg1, msg.obj, "AylaHost.setHostNetworkConnection_handler");
				returnToMainActivity(rsConnectToNewDevice, jsonResults, msg.arg1, AML_SET_HOST_NETWORK_CONNECTION);
			}
		}		
	};

	// --------------------------------- Get New Device Detail --------------------------------------------
	private final static Handler setNewDeviceTime = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			//String jsonResults = (String)msg.obj;
			String lanIp = AylaSetup.hostNewDeviceLanIp; 
			
			boolean delayExecution = false;
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
				saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "http status code", msg.arg1, "AylaHost.setNewDeviceTime_handler");

				AylaSetup.newDevice.getNewDeviceDetail(getNewDeviceDetail, lanIp, delayExecution);
			} else {
				saveToLog("%s, %s, %s:%d, %s, %s", "E", "AylaSetup", "error", msg.arg1, msg.obj, "AylaHost.setNewDeviceTime_handler");
				
				AylaSetup.newDevice.getNewDeviceDetail(getNewDeviceDetail, lanIp, delayExecution); // best effort
			}
		}
	};
	
	private final static Handler getNewDeviceDetail = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			String jsonResults = (String)msg.obj;
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
				// AylaModule newDevice = AylaSystemUtils.gson2.fromJson(jsonResults,AylaModule.class); // special case parsing for "DSN" in api v1.0
				AylaModule module = AylaSystemUtils.gson.fromJson(jsonResults,AylaModule.class); // special case parsing for "DSN" in api v1.0

				// Module info is complete here, it is empty before.  
				AylaSetup.newDevice = module; 
				
				// See if module supports app staying connected while it joins the WLAN
				AylaSetup.isApStaSupported = matchStringInArray(module.features, AylaHost.FEATURE_AP_STA);
				
				saveToLog("%s, %s, %s:%s, %s:%s, %s"
						, "I", "AylaSetup", "dsn", AylaSetup.newDevice.dsn
						, "build", AylaSetup.newDevice.build
						, "AylaHost.getNewDeviceDetail_handler");
				
				AylaSetup.isRsaKeySupported = matchStringInArray(module.features, AylaHost.FEATURE_RSA_KE);
//				AylaSetup.isRsaKeySupported = false;  
				if ( AylaSetup.isRsaKeySupported ) {
//					establishSecureWifiSetup (newDevice);
					establishSecureWifiSetup (module.dsn);
				} else { // Stop http routine if start secure wifi setup. 
					int responseCode = (int)msg.arg1;
					returnToMainActivity(rsConnectToNewDevice, jsonResults, responseCode, 0);
					AylaSetup.lastMethodCompleted = AML_SETUP_TASK_CONNECT_TO_NEW_DEVICE;  
				}
			} else {
				saveToLog("%s, %s, %s:%d, %s, %s", "E", "AylaSetup", "error", msg.arg1, msg.obj, "AylaHost.getNewDeviceDetail_handler");
				returnToMainActivity(rsConnectToNewDevice, jsonResults, msg.arg1, AML_GET_NEW_DEVICE_DETAIL);
			}
		}
	};

	private static boolean _stoleOriginalHandlers = false;
	private static Handler _originalNotifierHandler;
	private static Handler _originalReachabilityHandler;
	/** Prepare secure setup by reusing lan mode session, send local_reg request.*/
//	private static void establishSecureWifiSetup (final AylaModule module)
	static void establishSecureWifiSetup (final String moduleDSN) 
	{
		AylaDevice device = new AylaDevice();
		device.dsn = moduleDSN;
		// TODO: This accesses the global static variable, pay attention when multiple setup processes are allowed at the same time.  
		device.lanIp = AylaSetup.hostNewDeviceLanIp;
		
		String sIpAddr = AylaSystemUtils.getLocalIpv4Address();
		AylaLanMode.serverIpAddress = (sIpAddr == null)?"null":sIpAddr;    
		// TODO: instance-static access! 
		AylaDevice.serverPortNumber = AylaNetworks.DEFAULT_SERVER_PORT_NUMBER;
		AylaDevice.serverPath = "local_lan";
		AylaLanMode.setSecureSetupDevice(device);
		
		device.lanModeEnable();
		device.lanEnabled = true;

		if (!_stoleOriginalHandlers) {
			// Save the current notifier / reachability handlers so we can restore them later
			_originalNotifierHandler = AylaNotify.notifierHandle;
			_originalReachabilityHandler = AylaReachability.reachabilityHandle;
			_stoleOriginalHandlers = true;
		}
	
		AylaLanMode.enable(notifierHandler, reachabilityHandler);

		if ( isLanModeConfigCached(device) ) {
			device.getLanModule().startLanModeSession(AylaRestService.POST_LOCAL_REGISTRATION, false);
		} else { // no cache. 
			AylaEncryptionHelper helper = AylaEncryptionHelper.getInstance();
			helper.init(IAML_SECURITY_KEY_SIZE.IAML_SECURITY_KEY_SIZE_1024); 
			byte[] pub = helper.getPublicKeyPKCS1V21Encoded();
			
			// pass in public key raw bytes.
			device.getLanModule().startLanModeSession(
					AylaRestService.POST_LOCAL_REGISTRATION
					, true
					, pub);
		}
	}// end of establishSecureWifiSetup

	/**
	 * Restores the original handlers we co-opted in establishSecureWifiSetup
	 */
	static void restoreOriginalHandlersIfNeeded() {
		if ( _stoleOriginalHandlers ) {
			AylaLanMode.enable(_originalNotifierHandler, _originalReachabilityHandler);
			_stoleOriginalHandlers = false;
			_originalReachabilityHandler = null;
			_originalNotifierHandler = null;
		}
	}

	/**
	 * Precondition: AylaLanMode.device is setup, with dsn info; AylaLanMode.sessionTimer is initiated; 
	 * 
	 * Will let the AylaLanMode as it is and return false if cache misses, 
	 * prepare AylalanMode.device.lanModeConfig and sessionTimer other wise and return true
	 * 
	 *  @return false if cache misses, true otherwise.
	 * */
	private static boolean isLanModeConfigCached(AylaDevice device) {
		String jsonLanModeConfig = AylaCache.get(AML_CACHE_LAN_CONFIG, device.dsn);
		try {
			if ( TextUtils.isEmpty(jsonLanModeConfig) ) {
				return false;
			}
			
			AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaLanModeConfig", "lanModeConfigStorage", jsonLanModeConfig, "getLanModeConfig");
			device.lanModeConfig = AylaSystemUtils.gson.fromJson(jsonLanModeConfig,  AylaLanModeConfig.class);
			int interval = ( device.lanModeConfig.keepAlive.intValue()*1000 ) - AML_LAN_MODE_TIMEOUT_SAFETY;
			device.getLanModule().getSession().setTimerInterval(interval);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}// end of isLanModeConfigCached   
	
	
	private final static Handler reachabilityHandler = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			String jsonResults = (String)msg.obj;
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
				AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "D", tag, "reachability", jsonResults, "reachabilityHandler");
			}
		}// end of handleMessage         
	};
	
	
	private final static Handler notifierHandler = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			String jsonResults = (String)msg.obj;
			AylaSystemUtils.saveToLog("%s, %s, jsonResults:%s.", "D", tag + ".notifierHandle", jsonResults);
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
//				if ( msg.arg1 == 200 && msg.arg2 == AylaRestService.LAN_MODE_SESSION_ESTABLISHED )  
//				if ( msg.arg1 == 200 && msg.arg2 == AylaRestService.PROPERTY_CHANGE_NOTIFIER )         
				if ( msg.arg1 == 200 )
				{
//					AylaSetup.isRsaKeySupported = true; 
					int responseCode = (int)msg.arg1;
					returnToMainActivity(rsConnectToNewDevice, jsonResults, responseCode, 0);           
					AylaSetup.lastMethodCompleted = AML_SETUP_TASK_CONNECT_TO_NEW_DEVICE;
				} else {
					AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "E", tag, "notifier", jsonResults, "notifier");
				}
			}
		}// end of handleMessage                
	};
	
	
	// determine features in list
	protected static boolean matchStringInArray(String[] strArray, String matchString) {
		if (TextUtils.isEmpty(matchString)) {
			return false;
		}
		
		int count = strArray == null ? 0 : strArray.length;
		while (--count >= 0) {
			if (TextUtils.equals(strArray[count], matchString)) {
				return true;
			}
		}
		return false;
	}
// --------------------------------- End Connect To New Device -----------------------------------

// ---------------------------- Delete Host Network Connection -----------------------------------
	/**
	 * Same as {@link AylaHost#deleteHostNetworkConnection(Handler, int)} with no handler to return results.
	 * **/
	static AylaRestService deleteHostNetworkConnection(final int netId) {
		return deleteHostNetworkConnection(null, netId);
	}

	/**
	 * Remove a remembered wifi connection netId
	 * @param mHandle handler to return results
	 * @param netId the integer that identifies the network configuration to the supplicant
	 * @return AylaRestService object
	 */
	static AylaRestService deleteHostNetworkConnection(Handler mHandle, final int netId)
	{
		final AylaRestService rs = new AylaRestService(mHandle, "deleteHostNetworkConnection", AylaRestService.DELETE_HOST_NETWORK_CONNECTION);
		saveToLog("%s, %s, %s", "I", "AylaSetup", "AylaHost.deleteHostNetworkConnection");

		if (mHandle == null) {
			doDeleteHostNetworkConnection(rs, netId);
		} else {
			Thread thread = new Thread(new Runnable() {
				public void run() {
					doDeleteHostNetworkConnection(rs, netId);
				}
			});
			thread.start();
		}
		return rs;
	}
	
	private static void doDeleteHostNetworkConnection(AylaRestService rs, int netId) {
		final AylaHostWifiApi wiFiHostApi = new AylaHostWifiApi();
		final String action = AylaHostWifiApi.ACTION_REMOVE_CONFIGURED_CONNECTION;
		
		try {
			JSONObject args = new JSONObject();
			args.put("net_id", netId);
			WifiApiResult wifiApiResult = wiFiHostApi.execute(action, args, AylaNetworks.appContext);
			int status = wifiApiToAmlError(wifiApiResult.getStatus());
			String jsonDeleteConnection = wifiApiResult.getMessage();
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "jsonDeleteConnection", jsonDeleteConnection, "AylaHost.deleteHostNetworkConnection");
			returnToMainActivity(rs, jsonDeleteConnection, status, 0);
		} catch (Exception e) {
			saveToLog("%s, %s, %s:%s, %s:%s, %s", "E", "AylaSetup", "status", AML_ERROR_FAIL, "error", e.getMessage(), "AylaHost.deleteHostNetworkConnection");
			returnToMainActivity(rs, e.getMessage(), AML_ERROR_FAIL, 0);
		}
	}
	
	
	// ---------------------------- Delete Host Network Connections -----------------------------------
		/**
		 * Same as {@link AylaHost#deleteHostNetworkConnections(Handler, String)} with no handler to return results and no option to setup the call to execute later.
		 * **/
		static AylaRestService deleteHostNetworkConnections(final String ssidRegEx) {
			return deleteHostNetworkConnections(null, ssidRegEx);
		}

		/**
		 * Remove all wifi connections matching the SSID regEx
		 * @param mHandle handler to return results
		 * @param ssidRegEx ssid regex
		 * @return AylaRestService object
		 */
		static AylaRestService deleteHostNetworkConnections(Handler mHandle, final String ssidRegEx)
		{
			final AylaRestService rs = new AylaRestService(mHandle, "deleteHostNetworkConnection", AylaRestService.DELETE_HOST_NETWORK_CONNECTIONS);
			saveToLog("%s, %s, %s", "I", "AylaSetup", "AylaHost.deleteHostNetworkConnections");
			
			if (mHandle == null) {
				doDeleteHostNetworkConnections(rs, ssidRegEx);
			} else {
				Thread thread = new Thread(new Runnable() {
					public void run() {
						doDeleteHostNetworkConnections(rs, ssidRegEx);
					}
				});
				thread.start();
			}

			return rs;
		}
		
		static void doDeleteHostNetworkConnections(AylaRestService rs, final String ssidRegEx) {
			final AylaHostWifiApi wiFiHostApi = new AylaHostWifiApi();
			final String action = AylaHostWifiApi.ACTION_REMOVE_CONFIGURED_CONNECTIONS;
			try {
				JSONObject args = new JSONObject();
				args.put("ssidRegEx", ssidRegEx);
				WifiApiResult wifiApiResult = wiFiHostApi.execute(action, args, AylaNetworks.appContext);
				int status = wifiApiToAmlError(wifiApiResult.getStatus());
				String jsonDeleteConnections = wifiApiResult.getMessage();
				saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "jsonDeleteConnections", jsonDeleteConnections, "AylaHost.deleteHostNetworkConnections");
				returnToMainActivity(rs, jsonDeleteConnections, status, 0);
			} catch (Exception e) {
				saveToLog("%s, %s, %s:%s, %s:%s, %s", "E", "AylaSetup", "status", AML_ERROR_FAIL, "error", e.getMessage(), "AylaHost.deleteHostNetworkConnections");
				returnToMainActivity(rs, e.getMessage(), AML_ERROR_FAIL, 0);
			}
		}
		
	// ------------------------------------ Wifi Api Support methods ---------------------------------------
	private static int wifiApiToAmlError(int wifiApiStatus) {
		int amlError = wifiApiStatus;
		if ((wifiApiStatus == WifiApiResult.Status.OK.ordinal()) || (wifiApiStatus == WifiApiResult.Status.NO_RESULT.ordinal())) {
			amlError = AML_ERROR_ASYNC_OK;
		} else
		if (wifiApiStatus == WifiApiResult.Status.INVALID_ACTION.ordinal()) {
			amlError = AML_USER_INVALID_PARAMETERS;
		} else
		if (wifiApiStatus == WifiApiResult.Status.ERROR.ordinal()) {
			amlError = AML_ERROR_FAIL;
		} else
			if (wifiApiStatus == WifiApiResult.Status.WIFI_STATE_ERROR.ordinal()) {
				amlError = AML_WIFI_ERROR;
			}
		return amlError;
	}

	private static void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId) {
		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		
		rs.execute();
	}
}

class WifiApiResult {
    private final int status;
    private final String message;

    public WifiApiResult(Status status) { 
        this.status = status.ordinal();
        this.message = "\"" + WifiApiResult.StatusMessages[this.status] + "\"";
    }

    public WifiApiResult(Status status, String message) { 
        this.status = status.ordinal();
        this.message = message;
    }

    public WifiApiResult(Status status, JSONObject message) { 
        this.status = status.ordinal();
        this.message = message.toString();
    }

    public WifiApiResult(Status status, Boolean b) { 
        this.status = status.ordinal();
        this.message = ""+b;
    }
    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    private final static String[] StatusMessages = new String[] {
        "No result",
        "OK",
        "Class not found",
        "Illegal access",
        "Instantiation error",
        "Malformed url",
        "IO error",
        "Invalid action",
        "JSON error",
        "WiFi state error",
        "Error"
    };

    enum Status {
        NO_RESULT,
        OK,
        CLASS_NOT_FOUND_EXCEPTION,
        ILLEGAL_ACCESS_EXCEPTION,
        INSTANTIATION_EXCEPTION,
        MALFORMED_URL_EXCEPTION,
        IO_EXCEPTION,
        INVALID_ACTION,
        JSON_EXCEPTION,
        WIFI_STATE_ERROR,
        ERROR
    }
}
