//
//  AylaModule.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 9/24/12
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;

import com.aylanetworks.aaml.AylaLanMode.lanModeSession;
import com.aylanetworks.aaml.enums.CommandEntityBaseType;
import com.google.gson.annotations.Expose;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Map;

class AylaModuleScanResultsTopContainer {
	@Expose
	 AylaModuleScanResultsContainer wifiScan;
}

class AylaModuleScanResultsContainer {
	@Expose
	 long mtime;
	@Expose
	 AylaModuleScanResults[] results;

	 @Override
	 public String toString() {
		 StringBuilder result = new StringBuilder();
		 String NEW_LINE = System.getProperty("line.separator");
		 result.append(this.getClass().getName() + " Object {" + NEW_LINE);
		 result.append(" mtime: " + mtime + NEW_LINE);
		 result.append(" AylaModuleScanResults: " + Arrays.toString(results) + NEW_LINE);
		 result.append("}");
		 return result.toString();
	 }
}

// error response from module if connection cannot be attempted
class AylaModuleError { 
	@Expose
	int error;
	@Expose
	String msg;
}

class AylaWiFiStatusContainer {
	@Expose
	AylaWiFiStatus wifiStatus;
}

public class AylaModule extends AylaSystemUtils
//public class AylaModule extends AylaDevice
{
	private final static String tag = AylaModule.class.getSimpleName();
	
	// device properties retrievable from a new device
	@Expose
	public String dsn = ""; // v3.00
	@Expose
	public String device_service = "";
	@Expose
	public long last_connect_mtime = 0L;
	@Expose
	public long mtime = 0L;
	@Expose
	public String version = "";
	@Expose
	public String api_version = "1.0";
	@Expose
	public String build = "";
	@Expose
	public String features[];

	@Expose
	public AylaHostScanResults hostScanResults;
	@Expose
	public AylaHostNetworkConnection hostNetworkConnection;
	
	static int waitOnConnectionCounter = 0;

	// compound rest service objects
	static AylaRestService rsGetNewDeviceScanResultsForAPs;
	static AylaRestService rsConnectNewDeviceToService;
	static AylaRestService rsConfirmNewDeviceToServiceConnection;
	static AylaRestService rsGetNewDeviceWiFiStatus;

	static String jsonResults;

	// ----------------------------- Begin Get New Device Scan For APs ------------------------------------------------
	// First initiate a scan on the device
	// Second, retrieve the scan and return it
	protected static AylaRestService getNewDeviceScanForAPs(Handler mHandle) {
		return getNewDeviceScanForAPs(mHandle, false);
	}
	protected static AylaRestService getNewDeviceScanForAPs(Handler mHandle, Boolean delayExecution) {
		// http://192.168.0.1/wifi_scan_results.json
		String lanIp = AylaSetup.hostNewDeviceLanIp;
		String url = String.format("%s%s", lanIpServiceBaseURL(lanIp), "wifi_scan_results.json");
		saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "lanIpAddress", lanIp, "AylaModule.getDeviceScanResultsForAPs");
		
		// save rs for later, contains original handle
		rsGetNewDeviceScanResultsForAPs = new AylaRestService(mHandle, url, AylaRestService.GET_NEW_DEVICE_SCAN_RESULTS_FOR_APS); 

		startNewDeviceScanForAPs(); 
		return rsGetNewDeviceScanResultsForAPs;
	}

	static private AylaRestService startNewDeviceScanForAPs() {
		// http://192.168.0.1/wifi_scan.json
		String lanIp = AylaSetup.hostNewDeviceLanIp;
		String url = String.format("%s%s", lanIpServiceBaseURL(lanIp), "wifi_scan.json");
		saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "lanIpAddress", lanIp, "AylaModule.startNewDeviceScanForAPs");
		AylaRestService rsStartScan = new AylaRestService(startNewDeviceScanForAPs, url, AylaRestService.START_NEW_DEVICE_SCAN_FOR_APS);
		rsStartScan.execute();
		return null;
	}
	private final static Handler startNewDeviceScanForAPs = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			String jsonResults = (String)msg.obj;
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
				if (msg.arg1 == 204) {
					SystemClock.sleep(1600); // wait for scan to complete 1 to 2 seconds
					String lanIp = AylaSetup.hostNewDeviceLanIp;
					String url = String.format("%s%s", lanIpServiceBaseURL(lanIp), "wifi_scan_results.json");
					saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "lanIpAddress", lanIp, "AylaModule.startNewDeviceScanForAPs_handler");
					
					// Retrieve wifi scan from New Device
					rsGetNewDeviceScanResultsForAPs = new AylaRestService(rsGetNewDeviceScanResultsForAPs.mHandler, url, AylaRestService.GET_NEW_DEVICE_SCAN_RESULTS_FOR_APS);
					rsGetNewDeviceScanResultsForAPs.execute(); 
				} else {
					
					if (AylaSetup.isRsaKeySupported && AylaLanMode.hasSecureSetupDevice() && AylaLanMode.lanModeState == lanMode.RUNNING) {
						AylaSetup.exitSecureSetupSession();
					}
					
					// {"error":3, "msg": "Invalid Key" }
					AylaModuleError modErr = AylaSystemUtils.gson.fromJson(jsonResults, AylaModuleError.class);
					saveToLog("%s, %s, %s:%d, %s:%s, %s", "E", "AylaSetup", "error", modErr.error, "connectMsg", modErr.msg, "AylaModule.startNewDeviceScanForAPs_handler");
					msg.what = AylaNetworks.AML_GENERAL_EXCEPTION;
					msg.arg1 = 424; // Method Failure
					returnToMainActivity(rsGetNewDeviceScanResultsForAPs, jsonResults, msg.arg1, AML_GET_NEW_DEVICE_SCAN_FOR_APS);
				}
			} else {
				
				if (AylaSetup.isRsaKeySupported && AylaLanMode.hasSecureSetupDevice() && AylaLanMode.lanModeState == lanMode.RUNNING) {
					AylaSetup.exitSecureSetupSession();
				}
				
				saveToLog("%s, %s, %s:%d, %s, %s", "E", "AylaSetup", "error", msg.arg1, msg.obj, "AylaModule.startNewDeviceScanForAPs_handler");
				returnToMainActivity(rsGetNewDeviceScanResultsForAPs, jsonResults, msg.arg1, AML_GET_NEW_DEVICE_SCAN_FOR_APS);
			}
		}
	};

	protected static String stripScanContainerAndReturnAPs(String jsonScanResultsTopContainer)  {
		int count = 0;
		String jsonScanResults = "";
		try {
			AylaModuleScanResultsTopContainer scanResultsTopContainer = AylaSystemUtils.gson.fromJson(jsonScanResultsTopContainer,AylaModuleScanResultsTopContainer.class);
			AylaModuleScanResults[] scanResults = new AylaModuleScanResults[scanResultsTopContainer.wifiScan.results.length];

			// Only interested in Ayla Devices
			int numberOfAPs = 0;
			for (AylaModuleScanResults scanResult : scanResultsTopContainer.wifiScan.results) {
				if (scanResult.ssid.matches(AylaNetworks.deviceSsidRegex) == false ) {
					numberOfAPs++;
				}
				scanResults[count++] = scanResult;
			}
			if (numberOfAPs > 0) {
				AylaModuleScanResults[] filteredScanResults = returnAPsFromScanResults(scanResults, numberOfAPs);
				if (filteredScanResults != null) {
					// return json string
					jsonScanResults = AylaSystemUtils.gson.toJson(filteredScanResults,AylaModuleScanResults[].class);
				} else {
					jsonResults = null;
				}
			} else {
				jsonResults = null;
			}

			AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%d, %s", "I", "AylaSetup", "count", count, "numberOfAPs", numberOfAPs, "AylaModulestripScanContanier");
			return jsonScanResults;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%s, %s", "E", "AylaSetup", "count", count, "jsonScanResultsContainer", jsonScanResults, "AylaModule.stripScanContainer");
			e.printStackTrace();
			return null;
		}
	}

	private static AylaModuleScanResults[] returnAPsFromScanResults(AylaModuleScanResults[] scanResults, int numberOfAPs) {
		AylaModuleScanResults[] filteredScanResults = new AylaModuleScanResults[numberOfAPs]; // max size possible

		for(int i= 0,j = 0; i < scanResults.length; i++) {
			String ssid = scanResults[i].ssid;
			if (ssid.matches(AylaNetworks.deviceSsidRegex) == false ) {
				filteredScanResults[j++] = scanResults[i];
			}
		}
		return filteredScanResults;
	}
	
	
	
//----------------------------- End Get New Device Scan For APs ------------------------------------------------
	
	
	
// ------------------------------------------ Begin Connect New Device To Service ------------------------------
	
	
	/**
	 * Hidden network is supported by the module, no need to do anything on the lib side.
	 * */
	protected static AylaRestService connectNewDeviceToService(Handler mHandle) { // no location coordinates
		return connectNewDeviceToService(mHandle, null);
	}
	protected static AylaRestService connectNewDeviceToService(Handler mHandle, Map<String, Object> callParams) {
	
		String url = ""; // not used in main of compound calls
		rsConnectNewDeviceToService = new AylaRestService(mHandle, url, AylaRestService.CONNECT_NEW_DEVICE_TO_SERVICE);

		String setupToken = AylaSetup.setupToken  = AylaEncryption.randomToken(8);
		String lanIp = AylaSetup.hostNewDeviceLanIp;
		String lanSsid = AylaSetup.lanSsid;
		String ssidPassword = AylaSetup.lanPassword;
		
		String locationCoordinates = "";

		// validate params
		if (lanSsid.matches(AylaNetworks.deviceSsidRegex) == true ) {
			saveToLog("%s, %s, %s:%s, %s", "E", "AylaSetup", "lanSsid", lanSsid, "AylaModule.connectNewDeviceToService - invalid new device ssid");
			if (AylaSetup.isRsaKeySupported && AylaLanMode.hasSecureSetupDevice() && AylaLanMode.lanModeState == lanMode.RUNNING) {
				AylaSetup.exitSecureSetupSession();
			}
			returnToMainActivity(rsConnectNewDeviceToService, "invalid new device ssid", AML_ERROR_FAIL, AML_CONNECT_NEW_DEVICE_TO_SERVICE);
			return null;
		}

		// check optional params
		if( callParams != null ) {
			// set location
			Object longitudeObj = callParams.get(AylaNetworks.AML_SETUP_LOCATION_LONGITUDE);
			Object latitudeObj = callParams.get(AML_SETUP_LOCATION_LATITUDE);
			if (longitudeObj != null && latitudeObj != null) {
				if ( (longitudeObj instanceof Double) && (latitudeObj instanceof Double) ) {
					double longitude = (Double)longitudeObj;
					double latitude = (Double)latitudeObj;
					if ( (longitude != 0.0) || (latitude != 0.0) ) {
						locationCoordinates = String.format("%f,%f", latitude, longitude);
					}
				} else {
					// {"error":3, "msg": "Invalid Key" }
					Message msg = new Message();
					msg.what = AylaNetworks.AML_USER_INVALID_PARAMETERS;
					msg.arg1 = 417; // Expectation failed
					msg.obj =  "Invalid location information.";
					
					if (AylaSetup.isRsaKeySupported && AylaLanMode.hasSecureSetupDevice() && AylaLanMode.lanModeState == lanMode.RUNNING) {
						AylaSetup.exitSecureSetupSession();
					}
					
					saveToLog("%s, %s, %s:%d, %s:%s, %s", "E", "AylaSetup", "errCode", msg.arg1, "errMsg", msg.obj, "AylaModule.connectNewDeviceToService");
					returnToMainActivity(rsConnectNewDeviceToService, (String)msg.obj, msg.what, AML_CONNECT_NEW_DEVICE_TO_SERVICE);
				}
			}
		}

		// For Debugging. 
//		saveToLog("%s, %s, %s:%s, %s:%s, %s:%s, %s.", "I", "AylaSetup", "lanSsid", lanSsid, "ssidPassword", ssidPassword, "setupToken", setupToken, "AylaModule.connectNewDeviceToService");
		if ( AylaSetup.isRsaKeySupported && AylaLanMode.hasSecureSetupDevice()
				&& (AylaLanMode.lanModeState == lanMode.RUNNING || AylaLanMode.lanModeState == lanMode.ENABLED)
				&& AylaLanMode.getSecureSetupDevice().getLanModule().getSessionState() == lanModeSession.UP )
//		if (false)
		{
			setNewDeviceConnectToNetworkInSecureSession(setNewDeviceConnectToNetwork, lanSsid, ssidPassword, setupToken, locationCoordinates);
		} else {
			setNewDeviceConnectToNetwork(setNewDeviceConnectToNetwork, lanIp, lanSsid, ssidPassword, setupToken,  locationCoordinates);
		}
		return rsConnectNewDeviceToService ;
	}
	
	
	private static void setNewDeviceConnectToNetworkInSecureSession(
			final Handler mHandle
			, String lanSsid
			, String ssidPassword
			, String setupToken
			, String locationCoordinates) {
		AylaDevice setupDevice = AylaLanMode.getSecureSetupDevice();
		int cmdId = setupDevice.getLanModule().getSession().nextCommandOutstandingId();
		
		String path = "wifi_connect.json?ssid=" + lanSsid + "&setup_token=" + setupToken; 
		if ( !TextUtils.isEmpty(ssidPassword)) {
			try {
				path = path + "&key=" + URLEncoder.encode(ssidPassword, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

		}
		
		if ( !TextUtils.isEmpty(locationCoordinates)) {
			path = path + "&location=" + locationCoordinates;
		}

		JSONObject jsonEntity = new JSONObject();
		JSONArray jsonCmdArr = new JSONArray();
		JSONObject jsonCmd = new JSONObject();
		JSONObject jsonCmdContainer = new JSONObject();
		try {
			jsonCmd.put("cmd_id", cmdId);
			jsonCmd.put("method", "POST");
			jsonCmd.put("resource", path);
			jsonCmd.put("data", "none");
			jsonCmd.put("uri", URLEncoder.encode("local_lan/connect_status", "UTF-8"));

			jsonCmdContainer.put("cmd", jsonCmd);
			jsonCmdArr.put(0, jsonCmdContainer);
			jsonEntity.put("cmds", jsonCmdArr);
		} catch (Exception e) {
			e.printStackTrace();
		}

		String cmd = jsonEntity.toString();
		AylaLanCommandEntity entity = new AylaLanCommandEntity(cmd, cmdId, CommandEntityBaseType.AYLA_LAN_COMMAND); 
		String url = ""; // Not a http request.
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.SEND_NETWORK_PROFILE_LANMODE);
		AylaLanMode.sendToSecureSetupDevice(entity, rs);
	}// end of setNewDeviceConnectToNetworkInSecureSession
	

	private static AylaRestService setNewDeviceConnectToNetwork(final Handler mHandle, String lanIp, String lanSsid, String ssidPassword, String setupToken, String locationCoordinates) {
		// http://192.168.0.1//wifi_connect.json?ssid=example1&key=top%20secret%20key&setup_token=01a932&hidden=1
		String url = String.format("%s%s", lanIpServiceBaseURL(lanIp), "wifi_connect.json");
		saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "lanSsid", lanSsid, "AylaModule.setNewDeviceConnectToNetwork");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.SET_DEVICE_CONNECT_TO_NETWORK);
		rs.addParam("ssid", lanSsid); // module can only parse params from the url
		rs.addParam("setup_token", setupToken);
		
		// optional location coordinates
		if ( !TextUtils.isEmpty(ssidPassword) )
		{
			rs.addParam("key", ssidPassword);
		}
		
		if ( !TextUtils.isEmpty(locationCoordinates) )
		{
			rs.addParam("location", locationCoordinates);
		}
		
		saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaSetup", "url", url, "locationCoordinates", locationCoordinates, "AylaModule.setNewDeviceConnectToNetwork");
		rs.execute(); 
		return rs;
	}

	private final static Handler setNewDeviceConnectToNetwork = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			String jsonResults = (String)msg.obj;
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
				if (msg.arg1 == 204) {
					AylaSetup.connectedMode = AML_CONNECTION_UNKNOWN;
					saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "success", "OK",  "AylaModule.setNewDeviceConnectToNetwork_handler");

					int netId = AylaSetup.hostNewDeviceNetId; // delete from configured list
					AylaHost.deleteHostNetworkConnection(deleteHostNetworkConnection, netId);
				} else {
					// {"error":3, "msg": "Invalid Key" }
					AylaModuleError moduleError = AylaSystemUtils.gson.fromJson(jsonResults,AylaModuleError.class);
					int error =  moduleError.error; 
					String connectMsg =  moduleError.msg;
					saveToLog("%s, %s, %s:%d, %s:%s, %s", "E", "AylaSetup", "error", error, "connectMsg", connectMsg, "AylaModule.setDeviceConnectToNetwork_handler");
					msg.what = AylaNetworks.AML_USER_INVALID_PARAMETERS;
					msg.arg1 = 417; // Expectation failed
					returnToMainActivity(rsConnectNewDeviceToService, jsonResults, msg.arg1, AML_SET_NEW_DEVICE_CONNECT_TO_NETWORK);
				}
			} else {
				saveToLog("%s, %s, %s:%d, %s, %s", "E", "AylaSetup", "error", msg.arg1, msg.obj, "AylaModule.setDeviceConnectToNetwork");
				returnToMainActivity(rsConnectNewDeviceToService, jsonResults, msg.arg1, AML_SET_NEW_DEVICE_CONNECT_TO_NETWORK);
			}
		}
	};

	//------------------------------------------ Delete Host Network Connection -------------------------
	private final static Handler deleteHostNetworkConnection = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			String jsonResults = (String)msg.obj;
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
				AylaHostNetworkConnection networkConnection = AylaSystemUtils.gson.fromJson(jsonResults,  AylaHostNetworkConnection.class);
				saveToLog("%s, %s, %s:%d, %s", "I", "AylaSetup", "netId", networkConnection.netId, "AylaModule.deleteHostNetworkConnection_handler");

				AylaSetup.hostNetworkConnection.netId = AML_NETID_NOT_REMEMBERED ; // not connected
				AylaSetup.hostNewDeviceNetId = AML_NETID_NOT_REMEMBERED ; // deleted from remembered list

				int responseCode = (int)msg.arg1;
				AylaSetup.lastMethodCompleted = AML_SETUP_TASK_CONNECT_NEW_DEVICE_TO_SERVICE;
				returnToMainActivity(rsConnectNewDeviceToService, jsonResults, responseCode, 0);
			} else {
				saveToLog("%s, %s, %s:%d, %s, %s", "E", "AylaSetup", "error", msg.arg1, msg.obj, "AylaModule.deleteHostNetworkConnection_handler");
				returnToMainActivity(rsConnectNewDeviceToService, jsonResults, msg.arg1, AML_DELETE_HOST_NETWORK_CONNECTION);
			}
		}		
	};	

	// TODO: This is not related to object or class inner state, only parameter related. Would like to put all returnToMainActivity in a utility class. 
	public static void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId) {
		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		
		rs.execute(); 
	}
// ------------------------------------------ End Connect New Device To Service ---------------------

// ------------------------------------------ Begin Confirm New Device Connected To Service -----------------------
	static Boolean tryWirelessData = true; 
	protected static AylaRestService confirmNewDeviceToServiceConnection(Handler mHandle) {
		String url = ""; // not used in main compound call
		rsConfirmNewDeviceToServiceConnection = new AylaRestService(mHandle, url, AylaRestService.CONFIRM_NEW_DEVICE_TO_SERVICE_CONNECTION);
		
		int netId;
		String ssid = "";
		String hostSecurityType = "";
		String password = "";
		int retries = AylaSystemUtils.newDeviceToServiceConnectionRetries;
		
		// try to connect/reconnect to wifi
		if (AylaSetup.hostOriginalNetId < 0) { // not connected to Wifi, or connected to new device at the beginning
			netId = -1; // add to known list
			ssid = AylaSetup.lanSsid;
			password = AylaSetup.lanPassword;
			hostSecurityType = mapNewDeviceToHostSecurityType(AylaSetup.lanSecurityType);
		} else { // connected to Wifi at the beginning
			netId = AylaSetup.hostOriginalNetId; // reconnect
			ssid = AylaSetup.hostOriginalSsid;
			hostSecurityType = "";
			password = "";
		}
		saveToLog("%s, %s, %s:%s, %s:%s, %s:%s, %s", "I", "AylaSetup", "netId", netId, "ssid", ssid, "hostSecurityType", hostSecurityType, 
				 "AylaModule.confirmNewDeviceToServiceConnection");

		tryWirelessData = true; // will try wireless data connection one time if no WLAN connection
		AylaHost.setHostNetworkConnection(setHostNetworkReconnect, netId, ssid, hostSecurityType, password, retries);
		return null;
	}

	// --------------------------------- Set Host Network Reconnect -----------------------
	private static final Handler setHostNetworkReconnect= new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			String jsonResults = (String)msg.obj;
			saveToLog("%s, %s, %s:%s, %s", "D", "AylaModule", "jsonResults", jsonResults, "AylaModule.setHostNetworkReconnect_handler");
			int responseCode = (int)msg.arg1;
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
                // Our network topology has changed. Check reachability.
                AylaReachability.determineReachability(true);
				AylaHostNetworkConnection networkConnection = AylaSystemUtils.gson.fromJson(jsonResults,  AylaHostNetworkConnection.class);
				if (networkConnection.netId >= 0) {
					saveToLog("%s, %s, %s:%s, %s", "I", "AylaModule", "netId", networkConnection.netId, "AylaModule.setHostNetworkReconnect_handler");

					// check if device has connected to the service
					String setupToken = AylaSetup.setupToken;
					String dsn = AylaSetup.newDevice.dsn;
					Boolean delayExecution = false;
					AylaSystemUtils.sleep(2000); // v1.22
					AylaDevice.getNewDeviceConnected(getNewDeviceConnected, dsn, setupToken, delayExecution);
				} else {
					saveToLog("%s, %s, %s:%s, %s", "E", "AylaModule", "netId", networkConnection.netId, "AylaModule.setHostNetworkReconnect_handler");
					returnToMainActivity(rsConfirmNewDeviceToServiceConnection, jsonResults, AML_WIFI_ERROR, AML_SET_HOST_NETWORK_RECONNECT);
				}
			} else {
				saveToLog("%s, %s, %s:%d, %s, %s", "E", "AylaModule", "error", responseCode, msg.obj, "AylaSetup.setHostNetworkReconnect_handler");
				//if ( (AylaSetup.hostOriginalNetId < 0) &&  (tryWirelessData = true) ) { // try wireless data connection one time if not connected to WLAN or connected to new device at the beginning
				if ( tryWirelessData ) { // try wireless data connection one time. v1.22: always try
					tryWirelessData = false;
					// check if device has connected to the service
					String setupToken = AylaSetup.setupToken;
					String dsn = AylaSetup.newDevice.dsn;
					Boolean delayExecution = false;
					AylaSystemUtils.sleep(2000); // v1.22: 1500 to 2000 ms
					AylaDevice.getNewDeviceConnected(getNewDeviceConnected, dsn, setupToken, delayExecution);
				} else {
					returnToMainActivity(rsConfirmNewDeviceToServiceConnection, jsonResults, responseCode, AML_SET_HOST_NETWORK_RECONNECT);
				}
			}
		}		
	};

	private final static Handler getNewDeviceConnected = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			String jsonResults = (String)msg.obj;
			int responseCode = (int)msg.arg1;
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
				AylaDevice device = AylaSystemUtils.gson.fromJson(jsonResults,  AylaDevice.class);
				AylaSetup.lanIp = device.lanIp;
				AylaSetup.registrationType = device.registrationType;
				AylaSetup.connectedMode = AML_CONNECTED_TO_SERVICE;
				
//				saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaSetup", "registrationType", device.registrationType, "lanIp", device.lanIp, "AylaModule.getNewDeviceConnected_handler");
				
				// save new device info for registration
				device.dsn = AylaSetup.newDevice.dsn;
				device.setupToken = AylaSetup.setupToken;
				String jsonNewDevice = AylaSystemUtils.gson.toJson(device,AylaDevice.class);
				saveToLog("%s, %s, %s:%s.", "I", "AylaSetup", "New Registered Device", jsonNewDevice);
				AylaSetup.save(jsonNewDevice);

				// String jsonSuccess = "{" + "\"success\":" + "\"success\"" + "}";
				returnToMainActivity(rsConfirmNewDeviceToServiceConnection, jsonNewDevice, responseCode, 0);
				AylaSetup.lastMethodCompleted = AML_SETUP_TASK_CONFIRM_NEW_DEVICE_TO_SERVICE_CONNECTION;

				// end Confirm New Device To Service Connection success path
			} else {
				saveToLog("%s, %s, %s:%d, %s", "I", "AylaSetup", "error", responseCode, "AylaModule.getNewDeviceConnected_handler");
				if ( (AylaSetup.newDeviceToServiceConnectionRetries++ < AylaSystemUtils.newDeviceToServiceConnectionRetries) &&
					 (responseCode == 404) )
				{
					String setupToken =  AylaSetup.setupToken;
					String dsn = AylaSetup.newDevice.dsn; //gblAmlNewDevice.DSN;
					Boolean delayExecution = false;
					AylaSystemUtils.sleep(2000);
					AylaDevice.getNewDeviceConnected(getNewDeviceConnected, dsn, setupToken, delayExecution);
					
					//AylaSetup.exit(); // don't call for a failed new device to service connection
					return;
				} else {
					saveToLog("%s, %s, %s:%d, %s", "E", "AylaSetup", "error", responseCode, "AylaModule.getNewDeviceConnected_handler");
					returnToMainActivity(rsConfirmNewDeviceToServiceConnection, jsonResults, responseCode, AML_GET_NEW_DEVICE_CONNECTED);
				}
			}
		}		
	};
// ------------------------------------------ End Confirm New Device Connected To Service -----------------------

// ------------------------------------------ Begin Get New Device WiFi Status -----------------------
		protected static AylaRestService getNewDeviceWifiStatus(Handler mHandle) {
			String url = ""; // not used in main compound call
			rsGetNewDeviceWiFiStatus = new AylaRestService(mHandle, url, AylaRestService.GET_NEW_DEVICE_WIFI_STATUS);
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "entry", "OK", "AylaModule.getNewDeviceWiFiStatus");

			// set up host to new device connection
			int newNetId = -1; // add to host configured list
			String newSsid =  AylaSetup.hostNewDeviceSsid;// ssid selected by user
			String newSecurityType = AylaSetup.hostNewDeviceSecurityType; // OPEN by default
			String newPassword = AylaSetup.hostNewDevicePassword; // null/OPEN by default
			int retries = AylaSystemUtils.wifiRetries;
			AylaHost.setHostNetworkConnection(setHostNetworkConnection, newNetId, newSsid, newSecurityType, newPassword, retries);
			return null;
		}

		private final static Handler setHostNetworkConnection = new Handler(AylaNetworks.appContext.getMainLooper()) {
			public void handleMessage(Message msg) {
				String jsonResults = (String)msg.obj;
				if (msg.what == AylaNetworks.AML_ERROR_OK) {
					AylaHostNetworkConnection networkConnection = AylaSystemUtils.gson.fromJson(jsonResults,  AylaHostNetworkConnection.class);
					if (networkConnection.netId >= 0) {
						AylaSetup.hostNetworkConnection = networkConnection;
						AylaSetup.hostNewDeviceNetId = networkConnection.netId;
						AylaSetup.connectedMode = AML_CONNECTED_TO_HOST;
						saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "netId", networkConnection.netId, "AylaModule.setHostNetworkConnection_handler");	
	
						waitOnConnectionCounter = 0;
						String lanIp = AylaSetup.hostNewDeviceLanIp;
						Boolean delayExecution = false;
						getDeviceWiFiStatus(getNewDeviceWiFiStatus, lanIp, delayExecution);
					} else {
						saveToLog("%s, %s, %s:%d, %s, %s", "E", "AylaSetup", "error", AML_WIFI_ERROR, msg.obj, "AylaModule.setHostNetworkConnection_handler");
						returnToMainActivity(rsGetNewDeviceWiFiStatus, jsonResults, AML_WIFI_ERROR, AML_SET_HOST_NETWORK_CONNECTION2);
					}
				} else {
					saveToLog("%s, %s, %s:%d, %s, %s", "E", "AylaSetup", "error", msg.arg1, msg.obj, "AylaModule.setHostNetworkConnection_handler");
					returnToMainActivity(rsGetNewDeviceWiFiStatus, jsonResults, msg.arg1, AML_SET_HOST_NETWORK_CONNECTION2);
					AylaSetup.exit();
				}
			}		
		};

		private final static Handler getNewDeviceWiFiStatus = new Handler(AylaNetworks.appContext.getMainLooper()) {
			public void handleMessage(Message msg) {
				String jsonResults = (String)msg.obj;
				int responseCode = (int)msg.arg1;
				String serviceResponse;

				if (msg.what == AylaNetworks.AML_ERROR_OK) {
					AylaWiFiStatus wifiStatus = AylaSystemUtils.gson.fromJson(jsonResults,AylaWiFiStatus.class);
					String mac = wifiStatus.mac;

					int count = 0;
					AylaWiFiConnectHistory[] wifiConnectHistories = null;
					if (wifiStatus.connectHistory != null) {
						wifiConnectHistories = new AylaWiFiConnectHistory[wifiStatus.connectHistory.length];
						for (AylaWiFiConnectHistory connectHistory : wifiStatus.connectHistory) {
							wifiConnectHistories[count++] = connectHistory;
						}
					}
					saveToLog("%s, %s, %s:%s, %s:%d, %s", "I", "AylaSetup", "mac", mac, "wifiConnectHistories", count, "AylaModule.getNewDeviceWiFiStatus_handler");

					// If dual AP/STA mode enabled, wait for completion
					if (wifiConnectHistories != null) {
						if (wifiConnectHistories[0].error == 20) { // Connection attempt is still in progress
							if (waitOnConnectionCounter++ < 8) {	// can take up to 30 seconds
								SystemClock.sleep(4000); // wait for connection attempt to complete
								String lanIp = AylaSetup.hostNewDeviceLanIp;
								Boolean delayExecution = false;
								getDeviceWiFiStatus(getNewDeviceWiFiStatus, lanIp, delayExecution);
							}
						}
					}

					returnToMainActivity(rsGetNewDeviceWiFiStatus, jsonResults, responseCode, 0);

					AylaSetup.lastMethodCompleted = AML_SETUP_TASK_GET_NEW_DEVICE_WIFI_STATUS;
					if(AylaSetup.newDevice != null){
						sendToLogService(AylaSetup.newDevice.dsn,  jsonResults, "warning", "AylaSetup.AylaModule.getNewDeviceWiFiStatus_handler", null, true); // delay execution until we have service connectivity
					}
					// End Get New Device WiFi Status
				} else {
					saveToLog("%s, %s, %s:%d, %s, %s", "E", "AylaSetup", "error", responseCode, (String)msg.obj, "AylaSetup.AylaModule.getNewDeviceWiFiStatus_handler");
					returnToMainActivity(rsGetNewDeviceWiFiStatus, jsonResults, responseCode, AML_GET_NEW_DEVICE_WIFI_STATUS);
					
					serviceResponse = String.format("%s:%d", "responseCode", responseCode);
					sendToLogService(AylaSetup.newDevice.dsn,  serviceResponse, "error", "AylaSetup.AylaModule.getNewDeviceWiFiStatus_handler", null, true); // delay execution until we have service connectivity
				}
				
				AylaSetup.exit(); // need to reconnect to wifi on success
			}
		};

		protected static AylaRestService getDeviceWiFiStatus(String lanIp) {
			return getDeviceWiFiStatus(null, lanIp, true);
		}
		private static AylaRestService getDeviceWiFiStatus(Handler mHandle, String lanIp, boolean delayExecution) {
			// http://192.168.0.1/wifi_status.json
			String url = String.format("%s%s", lanIpServiceBaseURL(lanIp), "wifi_status.json");
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaSetup", "lanIpAddress", lanIp, "AylaModule.getDeviceWiFiStatus_handler");
			AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.GET_MODULE_WIFI_STATUS);
			String delayedStr = (delayExecution) ? "true" : "false";
			saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaSetup", "url", url, "delayedExecution", delayedStr, "AylaModule.getDeviceWiFiStatus_handler");
			if (delayExecution == false) {
				rs.execute(); 
			}
			return rs;
		}
		protected static String stripDeviceWiFiStatusContainers(String jsonWiFiStatusTopContainer)  {
			int count = 0;
			String jsonResults = "";
			try {
				AylaWiFiStatusContainer wiFiStatusTopContainer = AylaSystemUtils.gson.fromJson(jsonWiFiStatusTopContainer,AylaWiFiStatusContainer.class);
				AylaWiFiStatus wiFiStatusContainer = wiFiStatusTopContainer.wifiStatus;

				jsonResults = AylaSystemUtils.gson.toJson(wiFiStatusContainer,AylaWiFiStatus.class);
				AylaSystemUtils.saveToLog("%s, %s, %s", "I", "AylaSetup", "AylaModule.stripDeviceWiFiStatusContainer");
				return jsonResults;

			} catch (Exception e) {
				AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%s, %s", "E", "AylaSetup", "count", count, "jsonStatusResultsContainer", jsonResults, "AylaModule.stripDeviceWiFiStatusContainer");
				e.printStackTrace();
				return null;
			}
		}
// ------------------------------- End Get New Device Wifi Status ---------------------------------------------
		
// -------------------------------- Begin Module JSON API support methods --------------------------------------
		
		// Get detailed information and status about the new device
		protected AylaRestService getNewDeviceDetail(Handler mHandle, String lanIp) {
			return getNewDeviceDetail(mHandle, lanIp, true);
		}
		protected AylaRestService getNewDeviceDetail(Handler mHandle, String lanIp, boolean delayExecution) {
			// http://192.168.0.1/status.json
			String url = String.format("%s%s", lanIpServiceBaseURL(lanIp), "status.json");
			AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.GET_NEW_DEVICE_STATUS);
			String delayedStr = (delayExecution) ? "true" : "false";
			saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaSetup", "url", url, "delayedExecution", delayedStr, "AylaModule.getNewDeviceStatus");
			if (delayExecution == false) {
				rs.execute(); 
			}
			return rs;
		}

		// Sets the new device time
		protected AylaRestService setNewDeviceTime(Handler mHandle, String lanIp, long newTime, boolean delayExecution) {
			String url = String.format("%s%s", lanIpServiceBaseURL(lanIp), "time.json");
			AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.PUT_NEW_DEVICE_TIME);
			String delayedStr = (delayExecution) ? "true" : "false";
			saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaSetup", "url", url, "delayedExecution", delayedStr, "AylaModule.setNewDeviceTime");

			// {"time":87654321}
			String jsonTime = String.format("%s%d%s","{\"time\":", newTime, "}");
			rs.setEntity(jsonTime);

			if (delayExecution == false) {
				rs.execute(); 
			}
			return rs;
		}
		
		
		// Deletes the profile and disconnects, if currently connected. Profile names are SSIDs
		public static AylaRestService deleteDeviceWifiProfile(
				final AylaDevice device
//				final String lanIP
//				final String dsn
				, String profileName) {
//			return deleteDeviceWifiProfile(null, dsn, profileName, true);
			return deleteDeviceWifiProfile(null, device, profileName, true);
		}
		public static AylaRestService deleteDeviceWifiProfile(
				final Handler mHandle
				, final AylaDevice device
//				, final String lanIP
//				, final String dsn
				, String profileName) {
//			return deleteDeviceWifiProfile(mHandle, dsn, profileName, false);
			return deleteDeviceWifiProfile(mHandle, device, profileName, false);
		}
		// Now module only accepts encrypted form of this request, need to send via secure session! 
		/** 
		 * Assuming all are valid. 
		 * No null, and all necessary fields are initiated.
		 * */
		public static AylaRestService deleteDeviceWifiProfile(
				final Handler mHandle
				, final AylaDevice device
				, String profileName
				, boolean delayExecution) {
			
			String sIpAddr = AylaSystemUtils.getLocalIpv4Address();
			AylaLanMode.serverIpAddress = (sIpAddr == null)?"null":sIpAddr;      
			
			AylaDevice.serverPortNumber = AylaNetworks.DEFAULT_SERVER_PORT_NUMBER;
			AylaDevice.serverPath = "local_lan";

			
			if (device == null) {
				saveToLog("%s, %s, %s.", "D", "AylaModule.deleteDeviceWifiProfile", "Device not valid");
				return null;
			}
			
			// Prepare lan mode.
			AylaLanMode.setSecureSetupDevice(null);
			device.lanEnabled = true;
			device.lanModeEnable();

			AylaLanMode.enable(new DeleteNotifier(profileName, mHandle), reachabilityHandler);

			// Prepare lan mode config.
			String jsonLanModeConfig
				= AylaCache.get(AML_CACHE_LAN_CONFIG, device.dsn);         
			saveToLog("%s, %s, %s:%s.", "D", "AylaModule.deleteDeviceWifiProfile", "jsonLanModeConfig", jsonLanModeConfig+"");
			try {
				device.lanModeConfig = AylaSystemUtils.gson.fromJson(jsonLanModeConfig, AylaLanModeConfig.class);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (device.lanModeConfig == null) {
				saveToLog("%s, %s, %s.", "D", "AylaModule.deleteDeviceWifiProfile", "Device LanModeConfig not cached");
				if ( device.getLanModule() == null ) {
					new AylaLanModule(device);
				}
				device.getLanModule().getLanModeConfig();
			} else {
				// TODO: Assuming lanModeConfig has cached properly at this time.
				if ( device.getLanModule() == null ) {
					new AylaLanModule(device);
				}
				device.getLanModule().startLanModeSession(AylaRestService.POST_LOCAL_REGISTRATION, false);
			}
			return null;
		}// end of deleteDeviceWifiProfile   
		
		private final static Handler reachabilityHandler = new Handler(AylaNetworks.appContext.getMainLooper()) {
			public void handleMessage(Message msg) {
				String jsonResults = (String)msg.obj;
				if (msg.what == AylaNetworks.AML_ERROR_OK) {
					AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "D", tag, "reachability", jsonResults, "reachabilityHandler");
				}
			}// end of handleMessage         
		};
		

		static class DeleteNotifier extends Handler {
			private String mSSID = null;             
			private Handler mHandler = null;         
			
			public DeleteNotifier(final String ssid, final Handler handler) {
				mSSID = ssid;
				mHandler = handler;
			}
			
			@Override 
			public void handleMessage(Message msg) {
				String jsonResults = (String)msg.obj;
				AylaSystemUtils.saveToLog("%s, %s, jsonResults:%s.", "D", tag + ".notifierHandle", jsonResults);       
				if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
					if ( msg.arg1 == 204) {
						AylaRestService rs = new AylaRestService(mHandler, "", AylaRestService.DELETE_NETWORK_PROFILE_LANMODE);
						AylaModule.returnToMainActivity(rs, "", 200, AylaRestService.DELETE_NETWORK_PROFILE_LANMODE);
					} else { // Not sure what subTaskId is back. 
						// Secure session is done. 
						sendDeleteCommand(mHandler, mSSID);
					}
				}
			}// end of handleMessage
		}// end of DeleteNotifier class       
		
		
		/* Actually send delete command via secure session. Assuming lan mode is up.*/
		private static void sendDeleteCommand(
				final Handler mHandle
				, String lanSsid) {
			// http://192.168.0.1//wifi_profile.json?ssid=aWlanSSID		DELETE
			AylaDevice secureSetupDevice = AylaLanMode.getSecureSetupDevice();
			AylaLanModule module = secureSetupDevice.getLanModule();
			if ( module == null ) {
				module = new AylaLanModule(secureSetupDevice);
			}

			int cmdId = module.getSession().nextCommandOutstandingId();
			// Assuming ssid is valid here. 
			String path = "wifi_profile.json?ssid=" + lanSsid;
			
			String cmd = "{\"cmd\":{\"cmd_id\":" + cmdId 
					+ ",\"method\":\"DELETE\""
					+ ",\"resource\":\"" + path
					+ "\",\"data\":\"none\""
					+ ",\"uri\":\"delete_wifi_profile\"}}";
			
			cmd = "{\"cmds\":[" + cmd + "]}";
//			AylaSystemUtils.saveToLog("%s, %s, LanModeCommand:%s.", "D", tag + ".sendDeleteCommand", cmd);
			AylaLanCommandEntity entity = new AylaLanCommandEntity(cmd, cmdId, CommandEntityBaseType.AYLA_LAN_COMMAND);
			String url = ""; // not a http request.
			AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.DELETE_NETWORK_PROFILE_LANMODE);
			module.getSession().sendToLanModeDevice(entity, rs);
		}// end of sendDeleteCommand

    /**
     * disconnectAPMode method is used to Shut down AP Mode on the module. This method is typically not
     * used by the Client Apps as the AP Mode will be shut down automatically in 30 seconds
     * when the device is connected to service. Method only works when android device is
     * connected to module. On success, an HTTP status of “204 No Content” is returned.
     * Otherwise, an HTTP status of “403 Forbidden” is returned. AP mode disconnect command is
     * only accepted when module is still in AP mode, and successfully connected to
     * service in STA mode (i.e STA mode and AP mode are active at the same time). This command
     * turns off the AP mode, leaving STA mode active only.
     * */
    public static AylaRestService disconnectAPMode(
            final Handler mHandle
            , final String lanIp) {
        return disconnectAPMode(mHandle, lanIp, false);
    }

    public static AylaRestService disconnectAPMode(final String lanIp) {
        return disconnectAPMode(null, lanIp, true);
    }


    private static AylaRestService disconnectAPMode(Handler mHandle, String lanIp, boolean delayExecution) {
        // http://192.168.0.1/wifi_stop_ap.json
        String url = String.format("%s%s", lanIpServiceBaseURL(lanIp), "wifi_stop_ap.json");
        AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.PUT_DISCONNECT_AP_MODE);
        if(lanIp == null ) {
            returnToMainActivity(rs, "Make sure device is in AP Mode and connected to service", AML_ERROR_FAIL, AML_DISCONNECT_AP_MODE);
            return null;
        }
        String delayedStr = (delayExecution) ? "true" : "false";
        saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaSetup", "url", url, "disconnectAPMode", delayedStr, "AylaModule.disconnectAPMode");
        if (delayExecution == false) {
            rs.execute();
        }
        return rs;
    }

		
// -------------------------------- End Module JSON API support methods --------------------------------------
		
		//----- map module security type to host security type		
		private static String mapNewDeviceToHostSecurityType (String newDeviceSecurityType) {
			String hostSecurityType = "";
			
			if (newDeviceSecurityType == null) {
				hostSecurityType = AylaHostWifiApi.OPEN;
			} else {
				if (newDeviceSecurityType.contains(AylaHostWifiApi.WPA_AES)) {
					hostSecurityType = AylaHostWifiApi.WPA2;
				} else if (newDeviceSecurityType.contains(AylaHostWifiApi.WPA_MIX)) {
					hostSecurityType = AylaHostWifiApi.WPA;
				}  else if (newDeviceSecurityType.contains(AylaHostWifiApi.WPA)) {
					hostSecurityType = AylaHostWifiApi.WPA;
				} else if (newDeviceSecurityType.contains(AylaHostWifiApi.WEP)) {
					hostSecurityType = AylaHostWifiApi.WEP;
				}else if (newDeviceSecurityType.contains(AylaHostWifiApi.NONE)) {
					hostSecurityType = AylaHostWifiApi.OPEN;
				}else if (newDeviceSecurityType.contains(AylaHostWifiApi.UNKNOWN)) {
					hostSecurityType = AylaHostWifiApi.UNKNOWN;
					saveToLog("%s, %s, %s:%s, %s, %s", "E", "AylaSetup", "securityType", newDeviceSecurityType, "unknown security type", "AylaModule.confirmNewDeviceToServiceConnection");
				}  else {
					saveToLog("%s, %s, %s:%s, %s, %s", "E", "AylaSetup", "securityType", newDeviceSecurityType, "unrecognized security type", "AylaModule.confirmNewDeviceToServiceConnection");
				}
			}
			return hostSecurityType;
		}
		
		
		@Override 
		public String toString() {
			StringBuilder sb = new StringBuilder();
			String NEW_LINE = System.getProperty("line.separator");   
			
			sb.append(this.getClass().getSimpleName() + " Object {" + NEW_LINE)
				.append("dsn: " + dsn + NEW_LINE)
				.append("device_service: " + device_service + NEW_LINE)
				.append("last_connect_mtime: " + last_connect_mtime + NEW_LINE)
				.append("mtime: " + mtime + NEW_LINE)
				.append("version: " + version + NEW_LINE)
				.append("api_version: " + api_version + NEW_LINE)
				.append("build: " + build + NEW_LINE)
				.append("scanResult: " + hostScanResults + NEW_LINE)
				;
				sb.append("features: ");
				if (features != null) {
					for (String s:features) {
						sb.append(s + " ");
					}
				} else {
					sb.append("null");
				}
				sb.append(NEW_LINE);
			
			return sb.toString();
		}
 }// end of AylaModule class       

