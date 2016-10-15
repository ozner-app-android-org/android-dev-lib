//
//  AylaRegistration.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 9/06/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//
package com.aylanetworks.aaml;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.annotations.Expose;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 *
 * Get registration candidate device from the Ayla device service. Save the LAN IP address & DSN.
 *
 */
public class AylaRegistration extends AylaSystemUtils {
	@Expose
	private String regtoken;
	@Expose
	private String lanIpAddress;

	private static AylaDevice regCandidate;
	private static AylaDevice[] regCandidates;

	private static boolean isGateway;

	private static AylaRestService registerNewDeviceRS;

	static final String kAylaRegistrationTargetDsn = "dsn";
	static final String kAylaRegistrationRegistrationType = "regtype";
	static final String kAylaRegistrationWindowLength = "time";

	static final String kAylaRegistrationLongitude = "lng";
	static final String kAylaRegistrationLatitude = "lat";

	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");

		result.append(this.getClass().getName() + " Object {" + NEW_LINE);
		result.append(" regToken: " + regtoken + NEW_LINE);
		result.append(" regCandidate: " + regCandidate + NEW_LINE);
		result.append(" lanIpAddress: " + lanIpAddress + NEW_LINE);
		result.append("}");
		return result.toString();
	}

	/**
	 * ---------------------- Register a New Device ---------------------------
	 *
	 * Prerequisites:
	 *   a) The local Ayla device has completed setup and connected to the Ayla device service within the last hour
	 *   b) The local Ayla device and the phone/pad/pod/tablet running this code are connected to the same WLAN
	 *
	 * Steps
	 *   a) Get registration candidate device from the Ayla device service. Save the LAN IP address & DSN.
	 *   b) Get registration token from the local device using the LAN IP address
	 *   c) Register the local device with the Ayla device service using the Ayla local device registration token and DSN
	 *
	 * Returns
	 *   Success
	 *      A newly registered Ayla device
	 *   Failure
	 *     Ayla error code indicating which step failed
	 *
	 * ---------------------------
	 *
	 * @param mHandle is where result would be returned.
	 * @param device target device.
	 * @return AylaRestService object
	 */
	public static AylaRestService registerNewDevice(final Handler mHandle, AylaDevice device) {
		return registerNewDevice(mHandle, device, null);
	}
	public static AylaRestService registerNewDevice(final Handler mHandle, AylaDevice device, Map<String, String> callParams) {
		saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "Registration", "targetDsn", device.dsn, "regMode", device.registrationType, "registerNewDevice");
		String url = "";
		boolean async = (mHandle == null) ? false : true;

		registerNewDeviceRS = new AylaRestService(mHandle, url, AylaRestService.REGISTER_NEW_DEVICE);

		registerNewDeviceBegin(mHandle, device, callParams, async);
		return registerNewDeviceRS;
	}


	protected static AylaRestService registerNewDeviceBegin(final Handler mHandle, AylaDevice device, boolean async) {
		return registerNewDeviceBegin(mHandle, device, null, async);
	}
	protected static AylaRestService registerNewDeviceBegin(final Handler mHandle, AylaDevice device, Map<String, String> callParams, boolean async) {

		isGateway = device.isGateway();
		Map<String, String> params = null;
		if (callParams != null) {
			params = callParams;
		} else {
			params = new HashMap<String, String>();
		}
		params.put(kAylaRegistrationTargetDsn, device.dsn);
		params.put(kAylaRegistrationRegistrationType, device.registrationType);
		if ( !TextUtils.isEmpty(device.lng)) {
			params.put(kAylaRegistrationLongitude, device.lng);
		}
		if ( !TextUtils.isEmpty(device.lat)) {
			params.put(kAylaRegistrationLatitude, device.lat);
		}

		if (device.registrationType.equals(AML_REGISTRATION_TYPE_SAME_LAN) || device.registrationType.equals(AML_REGISTRATION_TYPE_BUTTON_PUSH)) {
			getRegistrationCandidate(null, params, async);
		} else if (device.registrationType.equals(AML_REGISTRATION_TYPE_AP_MODE)) {
			if (device.setupToken != null) {
				registerDevice(device.dsn, null, device.setupToken, params ,async);
			} else {
				saveToLog("%s, %s:%s, %s", "E", "Registration", "error", "Setup token not found", "registerNewDevice");
				returnToMainActivity(registerNewDeviceRS, "Setup token not found", AML_NO_ITEMS_FOUND , AML_REGISTER_NEW_DEVICE);
			}
		} else if (device.registrationType.equals(AML_REGISTRATION_TYPE_DISPLAY)) {
			if (device.registrationToken != null && !device.registrationToken.equals("")) {
				registerDevice(device.dsn, device.registrationToken, null, params, async);
			} else {
				saveToLog("%s, %s:%s, %s", "E", "Registration", "error", "Invalid registration token", "registerNewDevice");
				returnToMainActivity(registerNewDeviceRS, "Invalid registration token", AML_NO_ITEMS_FOUND , AML_REGISTER_NEW_DEVICE);
			}
		} else if (device.registrationType.equals(AML_REGISTRATION_TYPE_DSN)) {
			if (device.dsn != null && !device.dsn.equals("")) {
				registerDevice(device.dsn, null, null, params, async);
			} else {
				saveToLog("%s, %s:%s, %s", "E", "Registration", "error", "Device DSN is missing", "registerNewDevice");
				returnToMainActivity(registerNewDeviceRS, "Device DSN is missing", AML_NO_ITEMS_FOUND , AML_REGISTER_NEW_DEVICE);
			}
		} else if (device.registrationType.equals(AML_REGISTRATION_TYPE_NONE)) {
				saveToLog("%s, %s:%s, %s", "I", "Registration", "registrationType", "AML_REGISTRATION_TYPE_NONE", "registerNewDevice");
				returnToMainActivity(registerNewDeviceRS, "{}", AML_ERROR_OK , AML_REGISTER_NEW_DEVICE);
		} else {
			saveToLog("%s, %s:%s, %s", "E", "Registration", "error", "Registration type not found", "registerNewDevice"); 
			
			AylaSetup.clear(); // delete cached unsupported registration type
			returnToMainActivity(registerNewDeviceRS, "Not Found", AML_ERROR_NOT_FOUND, AML_REGISTER_NEW_DEVICE);
		}
		
		return registerNewDeviceRS;
	}

	
	
	protected static AylaRestService getCandidates(Handler mHandle, AylaDeviceGateway gateway) {
		return getCandidates(mHandle, gateway, null);
	}
	protected static AylaRestService getCandidates(Handler mHandle, AylaDeviceGateway gateway, Map<String, String> params) {
		String url = "";
		boolean async = (mHandle == null) ? false : true;
		
		registerNewDeviceRS = new AylaRestService(mHandle, url, AylaRestService.GET_GATEWAY_REGISTRATION_CANDIDATES); // save handle
		saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "Registration", "targetDsn", gateway.dsn, "regMode", AML_REGISTRATION_TYPE_NODE, "getCandidates");
		
		isGateway = true;
		Map<String, String> p = null;
		if (params == null) {
			p = new HashMap<String, String>();
		} else {
			p = params;
		}
		p.put(kAylaRegistrationTargetDsn, gateway.dsn);
		p.put(kAylaRegistrationRegistrationType, AML_REGISTRATION_TYPE_NODE);
		getRegistrationCandidate(mHandle, p, async);

		return registerNewDeviceRS;
	}
	
	// -------------------------------- getRegistrationCandidate ---------------------
	/**
	 * Use AylaDevice.stripContainer to remove "device:" wrapper from API JSON response
	 * 
	 * @param targetDsn: Device Serial Number of the new device
	 * @param targetRegType: registration type of the new device
	 * @return AylaRestService object
	 */
	private static AylaRestService getRegistrationCandidate(final Handler mHandle, String
			targetDsn, String targetRegType,  boolean async){
		Map<String, String> params = new HashMap<String, String>();
		if ( !TextUtils.isEmpty(targetDsn) ) {
			params.put(kAylaRegistrationTargetDsn, targetDsn);
		}
		if ( !TextUtils.isEmpty(targetRegType) ) {
			params.put(kAylaRegistrationRegistrationType, targetRegType);
		}
		return getRegistrationCandidate(mHandle, params, async);
	}
	private static AylaRestService getRegistrationCandidate(final Handler mHandle, Map<String, String> params, boolean async){
		// String url = "http://ads-dev.aylanetworks.com/apiv1/devices/register.json";
		String url = String.format("%s%s%s", deviceServiceBaseURL(), "devices/register", ".json");

		if (params != null) {
			StringBuilder param = new StringBuilder();
			String s = params.get(kAylaRegistrationTargetDsn);
			if (s!=null) {
				param.append("?").append(kAylaRegistrationTargetDsn).append("=").append(s);
			}
			
			s = params.get(kAylaRegistrationRegistrationType);
			if (s!=null) {
				if (param.length()>0) {
					param.append("&");
				} else {
					param.append("?");
				}
				param.append(kAylaRegistrationRegistrationType).append("=").append(s);
			}
			
			s = params.get(kAylaRegistrationWindowLength);
			if (s!=null) {
				if (param.length()>0) {
					param.append("&");
				} else {
					param.append("?");
				}
				param.append(kAylaRegistrationWindowLength).append("=").append(s);
			}
			url = url + param.toString();
		}
		
		int method = AylaRestService.GET_REGISTRATION_CANDIDATE;	// assume wifi device registration
		if (isGateway) {
			method = AylaRestService.GET_GATEWAY_REGISTRATION_CANDIDATES;
		}
		
		AylaRestService rs = null;
		if (async) {
			if (isGateway) {
				rs = new AylaRestService(mHandle, url, method);						// return node candidates to app
			} else {
				rs = new AylaRestService(new RegistrationHandler(AylaNetworks.appContext
						.getMainLooper(), params), url, method);
				
			}
			rs.execute();
		} else {
			rs = new AylaRestService(null, url, method);
			Message callResponse = rs.execute();
			doGetRegistrationCandidate(callResponse, params, false);
		}
		saveToLog("%s, %s, %s:%s, %s", "I", "Registration", "url", url, "getRegistrationCandidate");
		return rs;
	}

	/*private static final Handler getRegistrationCandidate = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			doGetRegistrationCandidate(msg, true);
		}		
	};*/

	private static class RegistrationHandler extends Handler{
		Map<String, String> callparams;

		public RegistrationHandler(Looper looper, Map<String, String> callparams) {
			super(looper);
			this.callparams = callparams;
		}

		@Override
		public void handleMessage(Message msg) {
			doGetRegistrationCandidate(msg, callparams, true);
		}
	}
	
	private static void doGetRegistrationCandidate(Message msg, Map<String, String> callparams,
												   boolean async) {
		String jsonResults = (String)msg.obj;
		if (msg.what == AylaNetworks.AML_ERROR_OK) {
			if (isGateway == false) {
				regCandidate = AylaSystemUtils.gson.fromJson(jsonResults,  AylaDevice.class);
				saveToLog("%s, %s, %s:%s, %s", "I", "Registration", "productName", regCandidate.productName, "getRegistrationCandidate.Handler");
				if (TextUtils.equals(regCandidate.registrationType,AML_REGISTRATION_TYPE_BUTTON_PUSH) || regCandidate.isGateway()) {
					registerDevice(regCandidate.dsn, null, null, callparams, async);
				} else {
					String lanIp = regCandidate.lanIp;
					getModuleRegistrationToken(lanIp, callparams, async);
				}
			} else {
				regCandidates = AylaSystemUtils.gson.fromJson(jsonResults,  AylaDevice[].class);
				if (regCandidates != null) {
					saveToLog("%s, %s, %s:%s, %s", "I", "Registration", "dsn", regCandidates[0].dsn, "getRegistrationCandidates.Handler");
					saveToLog("%s, %s, %s:%d, %s", "I", "Registration", "count", regCandidates.length, "getRegistrationCandidates.Handler");
				}
				
				returnToMainActivity(registerNewDeviceRS, jsonResults, msg.arg1, 0); // return to main with reg candidates
			}
		} else {
			saveToLog("%s, %s, %s:%d, %s, %s", "E", "Registration", "error", msg.arg1, msg.obj, "getRegistrationCandidate.Handler");
			returnToMainActivity(registerNewDeviceRS, jsonResults, msg.arg1, AML_GET_REGISTRATION_CANDIDATE);
		}
	}


	//----------------------------------- getModuleRegistrationToken ------------------------------
	/**
	 * Returns the new device registration token to the handler
	 * Required for AML_REGISTRATION_TYPE_SAME_LAN registration
	 * Nice to have for AML_REGISTRATION_TYPE_BUTTON_PUSH (DSN is required)
	 * Not required for AML_REGISTRATION_TYPE_AP_MODE
	 * Not required for AML_REGISTRATION_TYPE_DISPLAY
	 * Not required for AML_REGISTRATION_TYPE_DSN
	 * 
	 * @param lanIp - WLAN IP address
	 * @return registration token
	 */
	private static AylaRestService getModuleRegistrationToken(String lanIp,
															  Map<String, String> callparams,
															  boolean async) {
		// http://192.168.0.1/regtoken.json
		String url = String.format("%s%s", lanIpServiceBaseURL(lanIp), "regtoken.json");
		saveToLog("%s, %s, %s:%s, %s", "I", "Registration", "lanIpAddress", lanIp, "getModuleRegistrationToken");
		
		AylaRestService rs = null;
		if (async) {
			rs = new AylaRestService(new GetModuleRegTokenHandler(AylaNetworks.appContext
					.getMainLooper(), callparams), url,
					AylaRestService.GET_MODULE_REGISTRATION_TOKEN);
			rs.execute(); 
		} else {
			rs = new AylaRestService(null, url, AylaRestService.GET_MODULE_REGISTRATION_TOKEN);
			Message callResponse = rs.execute();
			doGetModuleRegistrationToken(callResponse, callparams, false);
		}
		saveToLog("%s, %s, %s:%s, %s", "I", "Registration", "url", url, "getModuleRegistrationToken");
		return rs;
	}
	
	/*private static final Handler getModuleRegistrationToken = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			doGetModuleRegistrationToken(msg, true);
		}		
	};*/

	private static class GetModuleRegTokenHandler extends Handler{
		Map<String, String> params;

		public GetModuleRegTokenHandler(Looper looper, Map<String, String> params) {
			super(looper);
			this.params = params;
		}

		@Override
		public void handleMessage(Message msg) {
			doGetModuleRegistrationToken(msg, params, true);
		}
	}


	private static void doGetModuleRegistrationToken(Message msg, Map<String, String>
			callparams, boolean async) {
		String jsonResults = (String)msg.obj;
		if (msg.what == AylaNetworks.AML_ERROR_OK) {
			AylaRegistration regToken = AylaSystemUtils.gson.fromJson(jsonResults,  AylaRegistration.class);
			saveToLog("%s, %s, %s:%s, %s", "I", "Registration", "regToken", "regToken.regtoken", "getModuleRegistrationToken.Handler");

			registerDevice(regCandidate.dsn, regToken.regtoken, null, callparams, async);
		} else {
			saveToLog("%s, %s, %s:%d, %s, %s", "E", "Registration", "error", msg.arg1, msg.obj, "getModuleRegistrationToken.Handler");
			returnToMainActivity(registerNewDeviceRS, jsonResults, msg.arg1, AML_GET_MODULE_REGISTRATION_TOKEN);
		}
	}

	protected static AylaRestService registerCandidate(Handler mHandle, AylaDeviceNode node) {
		saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "Registration", "targetDsn", node.dsn, "regMode", node.registrationType, "registerCandidate");
		String url = "registerCandidate";
		boolean async = (mHandle == null) ? false : true;
		
		registerNewDeviceRS = new AylaRestService(mHandle, url, AylaRestService.REGISTER_NEW_DEVICE); // save handle

		registerDevice(node.dsn, null, null, null, async);

		return registerNewDeviceRS;
	}

	//-------------------------------- registerDevice ---------------------------------------------
	private static AylaRestService registerDevice(String dsn, String regToken, String setupToken,
												  Map<String, String> callParams, boolean async) {
		String url = String.format("%s%s%s", deviceServiceBaseURL(), "devices", ".json");
		if (callParams != null) {
			StringBuilder param = new StringBuilder();
			String s = callParams.get(kAylaRegistrationLongitude);
			if (s != null) {
				if (param.length() > 0) {
					param.append("&");
				} else {
					param.append("?");
				}
				param.append(kAylaRegistrationLongitude).append("=").append(s);
			}

			s = callParams.get(kAylaRegistrationLatitude);
			if (s != null) {
				if (param.length() > 0) {
					param.append("&");
				} else {
					param.append("?");
				}
				param.append(kAylaRegistrationLatitude).append("=").append(s);
			}

			url = url + param.toString();
		}

		String tokenType = null, token = null;
		if (setupToken != null) {
			tokenType =  "\"setup_token\":";
			token = setupToken;
		} else
		if (regToken != null) {
			tokenType = "\"regtoken\":";
			token = regToken;
		}

		//{"device":{"dsn":"AC000WT00000999","regtoken":"4d54f2"}}
		String regDeviceJson = 			"{\"device\":{";
		if (dsn != null) { 		// not used in AP setup mode registration type
			regDeviceJson = regDeviceJson + "\"dsn\":" + "\"" + dsn + "\"";
			if (token != null) {
				regDeviceJson = regDeviceJson + ",";
			}
		}
		if (token != null) {	// no token with Dsn registration type
			regDeviceJson = regDeviceJson + tokenType + "\"" + token + "\"";
		}
		regDeviceJson = regDeviceJson + "}}";
		
		AylaRestService rs = null;
		if (async) {
			rs = new AylaRestService(registerDevice, url, AylaRestService.REGISTER_DEVICE);
			rs.setEntity(regDeviceJson);
			rs.execute(); 
		} else {
			rs = new AylaRestService(null, url, AylaRestService.REGISTER_DEVICE);
			rs.setEntity(regDeviceJson);
			Message callResponse = rs.execute();
			doRegisterDevice(callResponse, false);
		}
		
		saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "Registration", "url", url, "regParams", "regDeviceJson", "registerDevice");
		
		return rs;
	}

	private static final Handler registerDevice = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			doRegisterDevice(msg, true);
		}		
	};

	private static void doRegisterDevice(Message msg, boolean async) {
		String jsonResults = (String)msg.obj;
		if (msg.what == AylaNetworks.AML_ERROR_OK) {
			
			AylaDevice newDevice = AylaSystemUtils.gson.fromJson(jsonResults,  AylaDevice.class);
			saveToLog("%s, %s, %s:%s, %s", "I", "Registration", "productName", newDevice.productName, "registerDevice.Handler");
			
			returnToMainActivity(registerNewDeviceRS, jsonResults, msg.arg1, 0);
		} else {
			saveToLog("%s, %s, %s:%d, %s, %s", "E", "Registration", "error", msg.arg1, msg.obj, "registerDevice.Handler");
			returnToMainActivity(registerNewDeviceRS, jsonResults, msg.arg1, AML_REGISTER_DEVICE);
		}
	}
	
	/**
	 * Used to open, close and return the current value of the gateway join registration window.
	 * When open, nodes are recognized by the gateway as registerable
	 * If callParms are null, the remaining seconds of the join window are returned
	 * 
	 * @param mHandle
	 * @param gateway
	 * @param callParams
	 *     "duration" : The number of seconds to open the join window, optional
	 *                  Max: 255 seconds, Min: 0 which will close the join window
	 * @return AylaRestService object
	*/
	public static AylaRestService openRegistrationWindow(Handler mHandle, AylaDeviceGateway gateway, Map<String, String> callParams) {
		
		String paramKey = null;
		String paramValue = null;
		String durationStr = null;
		Integer duration = 200;		// gateway default
		
		// get optional fields
		
		// Join window time out, defaults to 200
		if (callParams != null) {
			
	    	paramKey = AylaDevice.kAylaDeviceJoinWindowDuration;	// "duration"
			paramValue = (String)callParams.get(paramKey);
			if (paramValue != null) {
				duration = Integer.valueOf(paramValue);
				durationStr = "?" + paramKey + "=";
			
				if (duration > 255) {
					durationStr = durationStr + "255";
				} else {
					durationStr = durationStr + duration.toString();
				}
			}
		}
		
		//String url = "http://ads-dev.aylanetworks.com/apiv1/devices/<deviceId>/registration_window.json";
		Number gwKey = gateway.getKey().intValue(); // Handle gson LazilyParsedNumber;
		String url = String.format("%s%s%d%s%s", deviceServiceBaseURL(), "devices/", gwKey, "/registration_window", ".json");
		if (durationStr != null) {
			url = url + durationStr;
		}
		
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.OPEN_REGISTRATION_WINDOW);
		saveToLog("%s, %s, %s:%s, %s", "I", "Registration", "url", url, "openRegistrationWindow");

		rs.execute(); 

		return rs;
	}
	
	/**
	 * Convenience method to close the gateway join registration window
	 * Simply calls openRegistrationWindow with an open time of 0 seconds
	 * 
	 * @param mHandle handler to return results
	 * @param gateway
	 * @return AylaRestService object
	 */
	public static AylaRestService closeRegistrationWindow(Handler mHandle, AylaDeviceGateway gateway) {
		final String joinWindowOpenTime = "0";								// 0 will close the join registration window
		Map<String, String> callParams = new HashMap<String, String>();
		
		callParams.put(AylaDevice.kAylaDeviceJoinWindowDuration, joinWindowOpenTime);
		return openRegistrationWindow(mHandle, gateway, callParams);
	}
	


	/**
	 * return to main Activity
	 * @param thisJsonResults JSON response from Ayla device service
	 * @param thisResponseCode HTTP response code
	 * @param thisSubTaskId AML specific code identifying where an error occurred. Set responseCode to <200...299> & this value to zero on no error
	 */
	private static void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId) {
		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		
		rs.execute();
	}

	/**
	 * Same as {@link AylaRegistration#unregisterDevice(Handler, AylaDevice)} with no handler to return results.
	 * **/
	public static AylaRestService unregisterDevice(AylaDevice device) {
		return unregisterDevice(null, device);
	}

	/**
	 * Unregister Device from Ayla Device Service
	 * @param mHandle is where result would be returned.
	 * @param device is the device to be unregistered
	 * @return AylaRestService object
	 */
	public static AylaRestService unregisterDevice(Handler mHandle, AylaDevice device) {
		Number devKey = device.getKey().intValue(); // Handle gson LazilyParsedNumber;
		String url = String.format(Locale.getDefault(), "%s%s%d%s", deviceServiceBaseURL(), "devices/", devKey, ".json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.UNREGISTER_DEVICE);
		saveToLog("%s, %s, %s:%s, %s", "I", "Registration", "url", url, "unregisterDevice");

		if (mHandle != null) { // is an async request
			rs.execute(); 
		}
		return rs;
	}
}


