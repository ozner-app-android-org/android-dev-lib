//
//  AylaDevice.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 8/15/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml; 


import android.os.Handler;
import android.text.TextUtils;

import com.aylanetworks.aaml.models.AylaCryptoEncapData;
import com.google.gson.annotations.Expose;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

//------------------------------------- AylaDevice --------------------------

class AylaLanModeConfigContainer {
	@Expose
	AylaLanModeConfig lanip;
}

class AylaLanModeConfig {
	@Expose
	Number lanipKeyId;
	@Expose
	String lanipKey;
	@Expose
	Number keepAlive;
	@Expose
	Number autoSync;
	@Expose
	String status  = "UnKnown";;
}

class AylaDeviceContainer {
	@Expose
	AylaDevice device;
}

public class AylaDevice extends AylaSystemUtils {

	
	private final static String tag = AylaDevice.class.getSimpleName();                        
	
	// device properties retrievable from the service
	@Expose
	public String connectedAt;	// last time the device connected to the service
	@Expose
	public String connectionStatus;	// near realtime indicator of device to service connectivity. Values are "Online" or "OffLine"
	@Expose
	public String dsn;			// Unique Device Serial Number
	@Expose
	public Boolean hasProperties;	// Does this device have properties
	// id
	@Expose
	public String ip;			// public external WAN IP Address
	@Expose
	private Number key;			// Unique Device Service DB identifier
	@Expose
	public boolean lanEnabled;	// is LAN Mode enabled on the service
	@Expose
	public String lanIp;		// local WLAN IP Address
	@Expose
	public String lat;			// latitude coordinate
	@Expose
	public String lng;			// longitude coordinate
	@Expose
	public String mac;			// optional
	@Expose
	public String model;		// device model
	@Expose
	public String moduleUpdatedAt; // when this device last completed OTA
	@Expose
	public String oem;
	@Expose
	public String oemModel;		// OEM model of the device. Typically assigned by the template

	
	@Expose
	public String productClass;	// device product class
	@Expose
	public String productName;	// device product name, user assignable

	@Expose
	public String ssid; 		// ssid of the AP the device is connected to	
	@Expose
	public String swVersion;	// software version running on the device
	@Expose
	public Number userID;		// User Id who has registered this device
	@Expose
	public Number templateId;	// template Id associated with this device
	@Expose
	public String registrationType; // how to register this device. One of "Same-LAN", "Button-Push", "AP-Mode", "Display", "None"(OEM)
	@Expose
	public String deviceName;	// name of this device
	@Expose
	public AylaGrant grant;		// If present, it indicates the device is registered to another user - it has been shared with this user


	// derived device properties
	@Expose
	AylaLanModeConfig lanModeConfig;
	@Expose
	public AylaTimezone timezone;

	@Expose
	public String setupToken;
	@Expose
	public String registrationToken;

	@Expose
	public AylaProperty property;
	@Expose
	public AylaProperty[] properties;

	@Expose
	public AylaSchedule schedule;
	@Expose
	public AylaSchedule[] schedules;

	@Expose
	public AylaDatum datum;
	@Expose
	public AylaDatum datums[];
	
	@Expose
	public AylaShare share;
	@Expose
	public AylaShare shares[];
	
	@Expose
	public AylaDeviceNotification deviceNotification;
	@Expose
	public AylaDeviceNotification[] deviceNotifications;

	private AylaLanModule _lanModule;



	// constructors
	public AylaDevice () {
		lanModeConfig = new AylaLanModeConfig();
		timezone = new AylaTimezone();
	}

	public AylaDevice(Number key, String dsn) {
		this();
		this.key = key;
		this.dsn = dsn;
	}

	// getters & setters
	public Number getKey() {
		return key;
	}

	void setKey(Number key) {
		this.key = key;
	}
	public String getProductName() {
		return productName;
	}

	public String getModel() 	{
		return model;
	}

	// Constants
	public final static String kAylaDeviceTypeWifi = "Wifi";
	public final static String kAylaDeviceTypeGateway = "Gateway";
	public final static String kAylaDeviceTypeNode = "Node";

	public final static String kAylaDeviceClassNameGateway = "AylaDeviceGateway";
	public final static String kAylaDeviceClassNameNode = "AylaDeviceNode";
	public final static String kAylaDeviceClassName = "AylaDevice";
	
	public final static String kAylaDeviceJoinWindowDuration = "duration";


	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");

		result.append(this.getClass().getSimpleName() + " Object {" + NEW_LINE);
		result.append(" productName: " + productName + NEW_LINE);
		result.append(" model: " + model + NEW_LINE);
		result.append(" dsn: " + dsn + NEW_LINE );
		result.append(" oemModel: " + oemModel + NEW_LINE);
		result.append(" connectedAt: " + connectedAt + NEW_LINE);
		result.append(" mac: " + mac + NEW_LINE);
		result.append(" lanIp: " + lanIp + NEW_LINE);
		result.append(" templateId: " + templateId + NEW_LINE);
		result.append(" registrationType" + registrationType + NEW_LINE);
		result.append(" setupToken" + setupToken + NEW_LINE );
		result.append(" registrationToken " + registrationToken + NEW_LINE);
		result.append("}");
		return result.toString();
	}


	// Inside library, if there is a copy inside device manager, return that copy, or insert/return this object
	protected AylaDevice getCopyInDeviceManager () {
		AylaDevice d = AylaDeviceManager.sharedManager().deviceWithDSN(this.dsn);
		if (d==null) {
			d = this;
			AylaDeviceManager.sharedManager().addDevice(this, false);
		}
		
		return d;
	}
	
	
	// -------------------------- Change Device -----------------------
	/**
	 * Same as {@link AylaDevice#update(Handler, Map, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService update(Map<String, String>callParams) {
		return update(null, callParams, true) ;
	}

	/**
	 * Same as {@link AylaDevice#update(Handler, Map, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService update(Handler mHandle, Map<String, String>callParams) {
		return update(mHandle, callParams, false) ;
	}

	/**
	 * This instance method supports to update module information.
	 *
	 * @param mHandle is where result would be returned.
	 * @param callParams allows for specifying module information required to be changed.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event
	 * @return AylaRestService object
	 */
	public AylaRestService update(Handler mHandle, Map<String, String>callParams, Boolean delayExecution) 
	{
		// PUT https://ads-dev.aylanetworks.com/apiv1/devices/<dev_key>.json
		int errCode = AML_USER_INVALID_PARAMETERS;
		Number devKey = this.getKey().intValue(); // Handle gson LazilyParsedNumber
		String url = String.format(Locale.getDefault(),"%s%s%d%s", deviceServiceBaseURL(), "devices/", devKey, ".json");
		AylaRestService rs = null;

		JSONObject deviceValues = new JSONObject();
		JSONObject errors = new JSONObject();
		String paramKey, paramValue;

		try {
			// test validity of objects
			paramKey = "productName";
			paramValue = (String)callParams.get(paramKey);
			if (TextUtils.isEmpty(paramValue)) {
				errors.put(paramKey, "can't be blank"); // use delete() & signUp()
			}
			deviceValues.put("product_name", paramValue);

			// return if errors in required fields
			if(errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.UPDATE_DEVICE);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaDevice", ERR_URL, errors.toString(), "update");
				returnToMainActivity(rs, errors.toString(), errCode, 0, false);
				return rs;
			}

			// All good, update the device
			rs = new AylaRestService(mHandle, url, AylaRestService.UPDATE_DEVICE);
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaDevice", "device.productName", this.productName, "update");
			String jsonDeviceContainer = new JSONObject().put("device", deviceValues).toString();		

			rs.setEntity(jsonDeviceContainer);
			if (delayExecution == false) {
				rs.execute(); // result returned through stripContainer
			}
		} catch (Exception e) {
			e.printStackTrace();
			rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.UPDATE_DEVICE);
			saveToLog("%s, %s, %s:%s, %s", "E", "AylaDevice", "exception", e.getCause(), "update");
			returnToMainActivity(rs, errors.toString(), AML_GENERAL_EXCEPTION, 0, false);
		}
		return rs;
	}
	/**
	 * 
	 * @param mHandle handler to return results
	 * @param delayExecution to be set to true to setup the call to execute later.
	 * @return AylaRestService object
	 */
	protected static AylaRestService getNewDeviceConnected(Handler mHandle, String dsn, String setupToken, Boolean delayExecution) {
		//String url = "http://ads-dev.aylanetworks.com/apiv1/devices/connected.json";
		String baseUrl = deviceServiceBaseURL();
		if (AylaSystemUtils.secureSetup == NO) {
			baseUrl = deviceServiceBaseURL().replace("https","http");
		}
		String url = String.format("%s%s%s", baseUrl, "devices/connected", ".json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.GET_NEW_DEVICE_CONNECTED);
		String delayedStr = (delayExecution == true) ? "true" : "false";
		saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaSetup", "url", url, "delayedExecution", delayedStr, "AylaDevice.getNewDeviceConnected");

		rs.addParam("dsn", dsn);
		rs.addParam("setup_token", setupToken);

		saveToLog("%s, %s, %s:%s, %s:%s ,%s", "I", "AylaSetup", "setupToken", "setupToken", "dsn", dsn, "AylaDevice.getNewDeviceConnected");
		if (delayExecution == false) {
			rs.execute(); 
		}
		return rs;
	}

	/**
	 * Same as {@link AylaDevice#getDevices(Handler, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public static AylaRestService getDevices() {
		return getDevices(null, true);
	}

	/**
	 * Same as {@link AylaDevice#getDevices(Handler, Boolean)} with no option to setup the call to execute later.
	 * **/
	public static AylaRestService getDevices(Handler mHandle) {
		return getDevices(mHandle, false);
	}

	/**
	 * These class methods get one or more registered devices from the Ayla Cloud Service. If the application has been LAN Mode enabled, the devices
	 * are read from cache, rather than the Ayla field service.
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event
	 * @return AylaRestService object
	 */
	public static AylaRestService getDevices(Handler mHandle, Boolean delayExecution) {
		AylaRestService rs = null;
		String savedJsonDeviceContainers = "";

		// read the devices from storage, returns "" if no cached values
		savedJsonDeviceContainers = AylaCache.get(AML_CACHE_DEVICE);


		// get devices from the service
		if ( AylaReachability.isCloudServiceAvailable()) {
			if (AylaSystemUtils.slowConnection == AylaNetworks.YES) {
				if (!TextUtils.isEmpty(savedJsonDeviceContainers)) {
					// Use cached values
					try {
						String jsonDevices = stripContainers(savedJsonDeviceContainers, AylaRestService.GET_DEVICES);
						rs = new AylaRestService(mHandle, "GetDevicesStorageLanMode", AylaRestService.GET_DEVICES_LANMODE);
						AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaDevices", "fromStorage", "true", "getDevicesStorage");
						returnToMainActivity(rs, jsonDevices, 203, 0, delayExecution);
					} catch (Exception e) {
						AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", "AylaDevices", "exception", e.getCause(), "getDevicesStorage_lanMode");
						e.printStackTrace();
					}			
				} else {
					// query field service
					// String url = "http://ads-dev.aylanetworks.com/apiv1/devices.json";
					String url = String.format("%s%s%s", AylaSystemUtils.deviceServiceBaseURL(), "devices", ".json");
					rs = new AylaRestService(mHandle, url, AylaRestService.GET_DEVICES);
					String delayedStr = (delayExecution == true) ? "true" : "false";
					saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaDevices", "url", url, "delayedExecution", delayedStr, "getDevicesService");
					if (delayExecution == false) {
						rs.execute(); 
					}
				}
			} else {
				// query field service
				// String url = "http://ads-dev.aylanetworks.com/apiv1/devices.json";
				String url = String.format("%s%s%s", AylaSystemUtils.deviceServiceBaseURL(), "devices", ".json");
				rs = new AylaRestService(mHandle, url, AylaRestService.GET_DEVICES);
				String delayedStr = (delayExecution == true) ? "true" : "false";
				saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaDevices", "url", url, "delayedExecution", delayedStr, "getDevicesService");
				if (delayExecution == false) {
					rs.execute(); 
				}
			}
		}

		// service is not reachable
		else 
			if (AylaReachability.isWiFiConnected(null) && !TextUtils.isEmpty(savedJsonDeviceContainers))
			{
				// use cached values
				try {
					String jsonDevices = stripContainers(savedJsonDeviceContainers, AylaRestService.GET_DEVICES);

					rs = new AylaRestService(mHandle, "GetDevicesStorageLanMode", AylaRestService.GET_DEVICES_LANMODE);
					saveToLog("%s, %s, %s:%s, %s", "I", "AylaDevices", "fromStorage", "true", "getDevicesStorage");
					returnToMainActivity(rs, jsonDevices, 203, 0, delayExecution);
				} catch (Exception e) {
					AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", "AylaDevices", "exception", e.getCause(), "getDevicesStorage_lanMode");
					e.printStackTrace();
				}
			}

		// no devices
			else { 
				if (AylaCache.cacheEnabled(AML_CACHE_DEVICE) == true && !TextUtils.isEmpty(savedJsonDeviceContainers)) {
					// use cached values
					try {
						String jsonDevices = stripContainers(savedJsonDeviceContainers, AylaRestService.GET_DEVICES);

						rs = new AylaRestService(mHandle, "GetDevicesStorageLanMode", AylaRestService.GET_DEVICES_LANMODE);
						saveToLog("%s, %s, %s:%s, %s", "I", "AylaDevices", "fromStorage", "true", "getDevicesStorage");
						returnToMainActivity(rs, jsonDevices, 203, 0, delayExecution);
					} catch (Exception e) {
						AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", "AylaDevices", "exception", e.getCause(), "getDevicesStorage_lanMode");
						e.printStackTrace();
					}
				} else {
					rs = new AylaRestService(mHandle, "GetDevicesStorageLanMode", AylaRestService.GET_DEVICES_LANMODE);
					saveToLog("%s, %s, %s:%s, %s", "I", "AylaDevices", "devices", "null", "getDevices");
					returnToMainActivity(rs, null, 404, 0, delayExecution);
				}
			}

		return rs;
	}
	
	
	/**
	 * Removes device container ("device:") from device service API response (see AylaExecuteRequest.commit())
	 * Used by device service calls returning information about a single device (vs an array of devices)
	 * 
	 * @param jsonDeviceContainer - AylaDevice object with key identifier
	 * @param method - AylaRestService requestId
	 * @return AylaDevice in JSON format
	 * @throws Exception
	 */
	protected static String stripContainer(String jsonDeviceContainer, int method ) throws Exception {
		String jsonDevice = "";
		AylaDevice device = null;
		
		try {
			AylaDeviceContainer deviceContainer = AylaSystemUtils.gson.fromJson(jsonDeviceContainer, AylaDeviceContainer.class);
			device = deviceContainer.device;
			AylaDeviceManager.sharedManager().addDevice(device, false);
			if (device.isNode()) { 
				// TODO: This is not necessary as it does not cache into AylaDeviceManager, just in case apps use the response in unexpected ways.
				device.initPropertiesOwner();
			}
			jsonDevice = AylaSystemUtils.gson.toJson(device,AylaDevice.class);
			AylaSystemUtils.saveToLog("%s %s %s", "I", tag, "stripContainer");
			return jsonDevice;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", tag, "jsonDeviceContainer", jsonDeviceContainer, "stripContainer");
			e.printStackTrace();
			throw e;
		}
	}

	protected static String stripContainers(final String jsonDeviceContainers, final int method) throws Exception {
		return stripContainers(jsonDeviceContainers, method, true);
	}
	protected static String stripContainers(final String jsonDeviceContainers, final int method, final boolean shouldCached ) throws Exception {
		int count = 0;
		String jsonDevices = "";
		AylaDevice[] devices = null;
		AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "D", tag, "stripContainers", "jsonDeviceContainers:" + jsonDeviceContainers);
		try {
			AylaDeviceContainer[] deviceContainers = AylaSystemUtils.gson.fromJson(jsonDeviceContainers, AylaDeviceContainer[].class);
			devices = new AylaDevice[deviceContainers.length];
			for (AylaDeviceContainer deviceContainer : deviceContainers) {
				devices[count]= deviceContainer.device;
				if (devices[count].isNode()) {
					devices[count].initPropertiesOwner();
				}
				count++;
			}

			if (shouldCached) {
				if ( count > 0 ) { 
					// TODO: would rather to init product_name here than every time resume from cache.
					AylaCache.save(AML_CACHE_DEVICE, jsonDeviceContainers);
				}
				// Let the device manager know we got a new device list in
				AylaDeviceManager.sharedManager().updateDevices(devices);
			}

			AylaSystemUtils.saveToLog("%s, %s, %s:%s %s.", "I", tag, "count", count, "stripContainers");
			jsonDevices = AylaSystemUtils.gson.toJson(devices, AylaDevice[].class);
			return jsonDevices;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s %s", "E", tag, "count", count, "jsonDeviceContainers:" + jsonDeviceContainers, "stripContainers");
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Same as {@link AylaDevice#getDeviceDetail(Handler, boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService getDeviceDetail() {
		return getDeviceDetail(null, true);
	}

	/**
	 * Same as {@link AylaDevice#getDeviceDetail(Handler, boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getDeviceDetail(Handler mHandle) {
		return getDeviceDetail(mHandle, false);
	}

	/**
	 * This instance method will fetch one single device from the Ayla Cloud Service, as specified by device.key.<br/>
	 *
	 * Note that there is NO properties for the returned device, please call {@link AylaDevice#getProperties()}
	 * to initiate properties if it has never been done before.
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event
	 * @return AylaRestService object
	 */
	public AylaRestService getDeviceDetail(Handler mHandle, final boolean delayExecution) {
		AylaRestService rs = null;
		if (this.getKey() == null) {
			saveToLog("%s, %s, %s, %s.", "E", tag, "getDeviceDetail", "key is missing");
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.GET_DEVICE_DETAIL);
			returnToMainActivity(rs, "Missing key.", AylaNetworks.AML_USER_INVALID_PARAMETERS, AylaRestService.GET_DEVICE_DETAIL, null);
			return rs;
		}
		
		Number devKey = this.getKey().intValue(); // Handle gson LazilyParsedNumber;
		// String url = "http://ads-dev.aylanetworks.com/apiv1/devices/###.json";
		String url = String.format(Locale.getDefault(),"%s%s%d%s", deviceServiceBaseURL(), "devices/", devKey, ".json");
		rs = new AylaRestService(mHandle, url, AylaRestService.GET_DEVICE_DETAIL);

		if ( !delayExecution ) {
			rs.execute(); 
		}
		return rs;
	}// end of getDeviceDetail            


	/**
	 * Same as {@link AylaDevice#getDeviceDetailByDSN(Handler, String, boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public static AylaRestService getDeviceDetailByDSN(final String dsn){
		return getDeviceDetailByDSN(null, dsn, true);
	}

	/**
	 * Same as {@link AylaDevice#getDeviceDetailByDSN(Handler, String, boolean)} with no option to setup the call to execute later.
	 * **/
	public static AylaRestService getDeviceDetailByDSN(Handler mHandle, final String dsn){
		return getDeviceDetailByDSN(mHandle, dsn, false);
	}

	/**
	 * This instance method will fetch one single device from the Ayla Cloud Service, as specified by device.dsn.<br/>
	 *
	 * NOTE: This method is not normally used for end user scoped applications: use getDeviceDetail() after
	 * calling getDevices() if more device information is required (which is rare in itself). This method is
	 * provided ONLY to enable an RBAC based OEM/Distributor/Dealer model. Use this method ONLY for that use
	 * case. If you are implementing a RBAC based OEM/Distributor/Dealer model, then use this static method to
	 * retrieve a device based on it's Device Serial Number (DSN).
	 *
	 * @param mHandle app call back
	 * @param dsn DSN of device
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event
	 * @return an executable object
	 * */
	public static AylaRestService getDeviceDetailByDSN(final Handler mHandle, final String dsn, final boolean delayExecution) {
		AylaRestService rs = null;
		if (TextUtils.isEmpty(dsn)) {
			saveToLog("%s, %s, %s, %s.", "E", tag, "getDeviceDetailByDSN", "dsn is invalid");
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.GET_DEVICE_DETAIL_BY_DSN);
			returnToMainActivity(rs, "Invalid Parameter.", AylaNetworks.AML_USER_INVALID_PARAMETERS, AylaRestService.GET_DEVICE_DETAIL_BY_DSN, null);
			return rs;
		}
		
		String url = String.format(Locale.getDefault(), "%s%s", deviceServiceBaseURL(),"dsns/" + dsn + ".json");
		rs = new AylaRestService(mHandle, url, AylaRestService.GET_DEVICE_DETAIL_BY_DSN);
		if (!delayExecution) {
			rs.execute();
		}
		return rs;
	}// end of getDeviceDetialByDSN
	
	// Expose to zigbee package so need to be public.
	public void initPropertiesFromCache() {
		String jsonProperties = AylaCache.get(AML_CACHE_PROPERTY, this.dsn);
		this.properties = AylaSystemUtils.gson.fromJson(jsonProperties, AylaProperty[].class);		
	}


	/**
	 * Same as {@link AylaDevice#factoryReset(Handler, Map, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService factoryReset(Map<String, Object> callParams) {
		return factoryReset(null, callParams, true);
	}

	/**
	 * Same as {@link AylaDevice#factoryReset(Handler, Map, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService factoryReset(Handler mHandle, Map<String, Object> callParams) {
		return factoryReset(mHandle, callParams, false);
	}

	/**
	 * This method will do factory reset for the current device.
	 * There are no call parameters required for this method at this time, so supply null for now.
	 * @param callParams not required: null
	 * @param mHandle handler to return results
	 * @param delayExecution to be set to true if you want setup this call but have it execute on an external event
	 *
	 */
	public AylaRestService factoryReset(Handler mHandle, Map<String, Object> callParams, Boolean delayExecution) {
			
		Number devKey = this.getKey().intValue(); // Handle gson LazilyParsedNumber;
		// String url = "http://ads-dev.aylanetworks.com/apiv1/devices/<devKey>/cmds/factory_reset.json";
		String url = String.format(Locale.getDefault(),"%s%s%d%s", deviceServiceBaseURL(), "devices/", devKey, "/cmds/factory_reset.json");
		AylaRestService restService = new AylaRestService(mHandle, url, AylaRestService.PUT_DEVICE_FACTORY_RESET);
		String delayedStr = (delayExecution == true) ? "true" : "false";
		saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "Devices", "url", url, "delayedExecution", delayedStr, "factoryReset");

		if (delayExecution == false) {
			restService.execute(); 
		}
		return restService;
	}

	
	// TODO: Move to a common utils class
	protected static void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId, Boolean delayExecution) {
		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		if (delayExecution == false) {
			rs.execute();
		}
	}

	// --------------------- Pass through to device helper classes -----------------------------------
	
	
	
	// --------------------- Properties pass-through methods -----------------------------------------
	/**
	 * Same as {@link AylaDevice#getProperties(Handler, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService getProperties() {	// sync, get all properties
		return AylaProperty.getProperties(null, this, null, true);
	}

	/**
	 * Same as {@link AylaDevice#getProperties(Handler, Map, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService getProperties(Map<String, String> callParams) {	// sync, get some properties
		return AylaProperty.getProperties(null, this, callParams, true);
	}

	/**
	 * Same as {@link AylaDevice#getProperties(Handler, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getProperties(Handler mHandle) {	// async, get all properties
		AylaRestService rs = AylaProperty.getProperties(mHandle, this, null, false); 
		return rs;
	}

	/**
	 * Same as {@link AylaDevice#getProperties(Handler, Map, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getProperties(Handler mHandle, Map<String, String> callParams) {	// async get some properties async
		AylaRestService rs = AylaProperty.getProperties(mHandle, this, callParams, false); 
		return rs;
	}

	/**
	 * Gets all properties summary objects associated with the device from Ayla device Service. Use getProperties when ordering is not important.
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event
	 * @return AylaRestService object
	 */
	public AylaRestService getProperties(Handler mHandle, Boolean delayExecution) {	// async get all properties
		AylaRestService rs = AylaProperty.getProperties(mHandle, this, null, delayExecution); 
		return rs;
	}

	/**
	 * Gets properties summary objects associated with the device from Ayla device Service for properties specified in callparams. Use getProperties when ordering is not important.
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param callParams call parameters
	 * @param delayExecution to be set to true if you want setup this call but have it execute on an external event
	 * @return AylaRestService object
	 */
	public AylaRestService getProperties(Handler mHandle, Map<String, String> callParams, Boolean delayExecution) {	// async get some properties
		AylaRestService rs = AylaProperty.getProperties(mHandle, this, callParams, delayExecution); 
		return rs;
	}


	/**
	 * Same as {@link AylaDevice#createBatchDatapoint(Handler, AylaBatchRequest[], boolean)} with no option to setup the call to execute later.
	 * **/
	// TODO: For now we support different device different property,  but to access cloud only, regardless of if the related device has an active lan mode session.
	// Implement sort and merge requests based on device connectivity and build batch datapoint request accordingly if necessary(personally it is not, or it should not.),  
	// Well, the behavior of this API is going to be complicated, mHandler might be called multiple times, and expectations are not specified for now.
	public static AylaRestService createBatchDatapoint(Handler mHandle, AylaBatchRequest[] requests) {
		return createBatchDatapoint(mHandle, requests, false);
	}

	/**
	 * Create multiple datapoints in different devices and different properties respectively in one API call.<br/>
	 * App is responsible for input validation. Note that we do not have partial validation for now, so if there is<br/>
	 * anything in AylaBatchRequest array that is not valid, lib will fail the whole http request.
	 *
	 * For validation metrics, please refer {@link AylaBatchRequest#isValidRequest()}.
	 *
	 *  Support only service mode for now. Lan mode support TBD.
	 *
	 * @param mHandle app callback
	 * @param requests All datapoint requests
	 * @param delayExecution to be set to true if you want setup this call but have it execute on an external event
	 * @return an executable unit representing this request.
	 * */
	public static AylaRestService createBatchDatapoint(Handler mHandle, AylaBatchRequest[] requests, final boolean delayExecution) {
		AylaRestService rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.CREATE_BATCH_DATAPOINT);    
		if (requests == null || requests.length<1) {
			saveToLog("%s, %s, %s, %s.", "W", tag, "createBatchDatapoint", "reqeusts array empty");           
			returnToMainActivity(rs, "Parameter invalid or missing.", AylaNetworks.AML_ERROR_PARAM_MISSING, AylaRestService.CREATE_BATCH_DATAPOINT, false);    
			return rs;
		}
		
		// For now, app responsible for requests validation, if any one of them invalid, fail the http request.
		for (AylaBatchRequest abr : requests) {
			if (!abr.isValidRequest()) {
				saveToLog("%s, %s, %s, %s.", "W", tag, "createBatchDatapoint", "invalid request");
				returnToMainActivity(rs, "Parameter invalid or missing.", AylaNetworks.AML_ERROR_PARAM_MISSING, AylaRestService.CREATE_BATCH_DATAPOINT, false);    
				return rs;
			}
		}
		
		if (!AylaReachability.isCloudServiceAvailable()) {
			saveToLog("%s, %s, %s, %s.", "W", tag, "createBatchDatapoint", "cloud service not available at this time");
			returnToMainActivity(rs, "Service not reachable.", AylaNetworks.AML_ERROR_UNREACHABLE, AylaRestService.CREATE_BATCH_DATAPOINT, false);    
			return rs;
		}
		
		StringBuilder entity = new StringBuilder(32*(requests.length + 1));
		entity.append("{")
			.append("\"batch_datapoints\":[");  
		
		for (int i=0; i<requests.length; i++) {
			if (i!=0) {
				entity.append(",");
			}
			entity.append(requests[i].toRequestString());             
		}
		
		entity.append("]}");
		String url = String.format(Locale.getDefault(), "%s%s", deviceServiceBaseURL(), "batch_datapoints.json");
		rs = new AylaRestService(mHandle, url, AylaRestService.CREATE_BATCH_DATAPOINT);
		rs.setEntity(entity.toString()); 
		saveToLog("%s, %s, %s\n%s\n%s", "D", tag, "createBatchDatapoint", "entity:" + entity.toString(), "url:" + rs.url);
		if (!delayExecution) {
			rs.execute();
		}
		return rs;
	}// end of createBatchDatapoint        



	/**
	 * Same as {@link AylaDevice#registerNewDevice(Handler)} with no handler to return results.
	 * **/
	public AylaRestService registerNewDevice() {	// sync
		AylaRestService rs = AylaRegistration.registerNewDevice(null, this);
		return rs;
	}

	/**
	 * Device Registration provides a way to easily register a device once it has successfully completed the Setup process. Devices must be registered
	 * before they can be accessed by the Device Service methods. Optional parameters such as
	 * latitude and longitude can be set in this device object if they need to be sent to
	 * the service during registration.
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @return AylaRestService object
	 */
	public AylaRestService registerNewDevice(Handler mHandle) {	// async
		AylaRestService rs = AylaRegistration.registerNewDevice(mHandle, this);
		return rs;
	}

	/**
	 * Same as {@link AylaDevice#unregisterDevice(Handler)} with no handler to return results.
	 * **/
	public AylaRestService unregisterDevice() {
		return AylaRegistration.unregisterDevice(null, this);
	}

	/**
	 * This method will unregister a device from a user account.
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @return AylaRestService object
	 */
	public AylaRestService unregisterDevice(Handler mHandle) {
		AylaRestService rs = AylaRegistration.unregisterDevice(mHandle, this);
		return rs;
	}

	// ----------------------------- Schedule Pass Through Methods ----------------------------
	/**
	 * Same as {@link AylaDevice#updateSchedule(Handler, AylaSchedule, AylaScheduleAction[] )} with no handler to return results.
	 * **/
	public AylaRestService updateSchedule(AylaSchedule schedule, AylaScheduleAction[] actions) {
		return schedule.update(null, this, actions);
	}

	/**
	 * This updateSchedule method is used to update/change schedule object and associated Schedule Action properties. When using the Full Template Schedule
	 * Model,(schedules and Actions are pre-created in the OEM template), this method will PUT the data to existing schedule and action instances passed in
	 * as parameters. When using the Dynamic Template Schedule Model, (schedules are precreated in the OEM template, Schedule Actions are dynamically created
	 * and deleted), this method will create and delete the Actions as required if newly allocated scheduleAction object(s) are passed in as parameters.
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param schedule is the current schedule object set to desired values.
	 * @param actions is the array of new actions updated to current schedule
	 * @return AylaRestService object
	 */
	public AylaRestService updateSchedule(Handler mHandle, AylaSchedule schedule, AylaScheduleAction[] actions) {
		return schedule.update(mHandle, this, actions);
	}

	/**
	 * Same as {@link AylaDevice#getAllSchedules(Handler, Map, boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService getAllSchedules(Map<String, String> callParams) {
		return getAllSchedules(null, callParams, true);
	}

	/**
	 * Same as {@link AylaDevice#getAllSchedules(Handler, Map, boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getAllSchedules(Handler mHandle, Map<String, String> callParams) {
		return getAllSchedules(mHandle, callParams, false);
	}

	/**
	 * This method results in all schedules for a given device object being return to successBlock. Each AylaSchedule array member instance includes only
	 * the schedule properties and not the associated Schedule Actions. This method is typically used to provide a top-level listing of available schedules
	 * from which the end user selects.
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param callParams must contain required parameter(s) by this method. Please read the mobile library document for details.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event
	 * @return AylaRestService object
	 */
	public AylaRestService getAllSchedules(Handler mHandle, Map<String, String> callParams, boolean delayExecution) {
		return AylaSchedule.getAll(mHandle, this, callParams, delayExecution);
	}

	/**
	 * Same as {@link AylaDevice#getScheduleByName(Handler, Map, boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService getScheduleByName(Map<String, String> callParams) {
		return getScheduleByName(null, callParams, true);
	}

	/**
	 * Same as {@link AylaDevice#getScheduleByName(Handler, Map, boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getScheduleByName(Handler mHandle, Map<String, String> callParams) {
		return getScheduleByName(mHandle, callParams, false);
	}

	/**
	 * The method results in the schedule matching the given name being returned to the handler. The AylaSchedule instance includes the schedule properties
	 * and the associated Schedule Actions. This method is typically used to provide complete schedule information for a top-level schedule selected from a
	 * list populated by the getAllSchedules method.
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param callParams must contain required parameter(s) by this method. Please read the mobile library document for details.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event
	 * @return AylaRestService object
	 */
	public AylaRestService getScheduleByName(Handler mHandle, Map<String, String> callParams, boolean delayExecution) {
		return AylaSchedule.getByName(mHandle, this, callParams, delayExecution);
	}

	/**
	 * Same as {@link AylaDevice#clearSchedule(Handler, AylaSchedule, boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService clearSchedule(AylaSchedule schedule) {
		return schedule.clear(null, true);
	}

	/**
	 * Same as {@link AylaDevice#clearSchedule(Handler, AylaSchedule, boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService clearSchedule(Handler mHandle, AylaSchedule schedule) {
		return schedule.clear(mHandle, false);
	}

	/**
	 * The clearSchedule method will delete the Schedule Actions associated with the Schedule instance and also set the schedule.active value to false. Consider
	 * the clear method a virtual delete method for the Dynamic Action Schedule Model. DO NOT use clear when implementing the Full Template model as it will
	 * delete the Actions. Instead, simply set schedule.active (and optionally the associated scheduleAction[].active values) to false using the updateSchedule
	 * method.
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param schedule is the Schedule to be cleared.
	 * @param delayExecution set to true to setup this call but have it execute on an external event
	 * @return AylaRestService object
	 */
	public AylaRestService clearSchedule(Handler mHandle, AylaSchedule schedule, boolean delayExecution) {
		return schedule.clear(mHandle, delayExecution);
	}

	// ----------------------------- Timezone Pass Through Methods ----------------------------

	/**
	 * Same as {@link AylaDevice#createTimezone(Handler, Boolean)}with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService createTimezone() {
		return timezone.create(null, this, true);
	}

	/**
	 * Same as {@link AylaDevice#createTimezone(Handler, Boolean)}with no option to setup the call to execute later.
	 * **/
	public AylaRestService createTimezone(Handler mHandle) {
		return timezone.create(mHandle, this, false);
	}

	/**
	 * Posts the time zone associated with the device to the Ayla device Service.
	 * device.timeZone:
	 *     utc_offset (required): string which specifies utc offset. Format must be +HH:MM or -HH:MM. For example +05:00 or -03:00.
	 *     dst (optional): boolean which specifies if the location follows DST. Default: false.
	 *     dst_active (optional): booelan which specifies if DST is currently active. Default: false.
	 *     dst_next_change_date (optional): string which specifies next DST state change from active/inactive OR from inactive/active. Format must be yyyy-mm-dd
	 *     tz_id (optional): String identifier for the timezone. eg: "America/New_York".
	 *         NOTE: If tz_id is present, it must correlate with the utc_offset, else the POST will be rejected.
	 *
	 * @param mHandle is where result will be returned.
	 * @param delayExecution to be set to true if you want setup this call but have it execute on an external event
	 * @return AylaRestService object
	 */
	public AylaRestService createTimezone(Handler mHandle, Boolean delayExecution) {
		return timezone.create(mHandle, this, false);
	}


	/**
	 * Same as {@link AylaDevice#updateTimezone(Handler, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService updateTimezone() {
		return timezone.update(null, this, true);
	}

	/**
	 * Same as {@link AylaDevice#updateTimezone(Handler, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService updateTimezone(Handler mHandle) {
		return timezone.update(mHandle, this, false);
	}

	/**
	 * Update the timezone ID for this device
	 * @param mHandle handler to return results
	 * @param delayExecution to be set to true if you want setup this call but have it execute on an external event
	 *     tzId: (required): String identifier for the timezone. eg: "America/New_York".
	 *     NOTE: DST attributes are updated, based on whether the timezone has DST or not.
	 * @return AylaRestService object
	 */
	public AylaRestService updateTimezone(Handler mHandle, Boolean delayExecution) {
		return timezone.update(mHandle, this, false);
	}


	/**
	 * Same as {@link AylaDevice#getTimezone(Handler, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService getTimezone() {
		return timezone.get(null, this, true);
	}

	/**
	 * Same as {@link AylaDevice#getTimezone(Handler, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getTimezone(Handler mHandle) {
		return timezone.get(mHandle, this, false);
	}

	/**
	 * Get the timezone information for this device
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param delayExecution delayExecution set to true to setup this call but have it execute on an external event
	 * @return AylaRestService object
	 */
	public AylaRestService getTimezone(Handler mHandle, Boolean delayExecution) {
		return timezone.get(mHandle, this, delayExecution);
	}

	
	// ---------------------- Device Datum pass-through methods ------------------------
	public AylaRestService createDatum(AylaDatum deviceDatum) {
		return deviceDatum.create(this);
	}
	public AylaRestService createDatum(Handler mHandle, AylaDatum deviceDatum) {
		return deviceDatum.create(mHandle, this);
	}
	public AylaRestService updateDatum(AylaDatum deviceDatum) {
		return deviceDatum.update(this);
	}
	public AylaRestService updateDatum(Handler mHandle, AylaDatum deviceDatum) {
		return deviceDatum.update(mHandle, this);
	}
	public AylaRestService getDatumWithKey(String key) {
		return AylaDatum.getWithKey(this, key);
	}
	public AylaRestService getDatumWithKey(Handler mHandle, String key) {
		return AylaDatum.getWithKey(mHandle, this, key);
	}
	public AylaRestService getDatum(Map<String, ArrayList<String>> callParams) {
		return AylaDatum.get(this, callParams);
	}
	public AylaRestService getDatum(Handler mHandle, Map<String, ArrayList<String>> callParams) {
		return AylaDatum.get(mHandle, this, callParams);
	}
	public AylaRestService deleteDatum(AylaDatum deviceDatum) {
		return deviceDatum.delete(this);
	}
	public AylaRestService deleteDatum(Handler mHandle, AylaDatum deviceDatum) {
		return deviceDatum.delete(mHandle, this);
	}

	// ---------------------- Device Share pass-through methods ------------------------
	// Create an owned device share
	public AylaRestService createShare(AylaShare deviceShare) {
		return deviceShare.create(this);
	}
	public AylaRestService createShare(Handler mHandle, AylaShare deviceShare) {
		return deviceShare.create(mHandle, this);
	}
	// update an owned device share
	public AylaRestService updateShare(AylaShare deviceShare) {
		return deviceShare.update();
	}
	public AylaRestService updateShare(Handler mHandle, AylaShare deviceShare) {
		return deviceShare.update(mHandle);
	}
	// get an owned or received share for a given id
	public AylaRestService getShare(String id) {
		return AylaShare.getWithId(null, id);
	}
	public AylaRestService getShare(Handler mHandle, String id) {
		return AylaShare.getWithId(mHandle, id);
	}
	// get all owned shares for a given device
	public AylaRestService getShares() {
		return AylaShare.get(null, this, null);
	}
	public AylaRestService getShares(Map<String, String> callParams) {
		return AylaShare.get(null, this, callParams);
	}
	public AylaRestService getShares(Handler mHandle) {
		return AylaShare.get(mHandle, this, null);
	}
	public AylaRestService getShares(Handler mHandle, Map<String, String> callParams) {
		return AylaShare.get(mHandle, this, callParams);
	}
	// get all owned device shares for the current user
	static public AylaRestService getAllShares() {
		AylaDevice devObj = new AylaDevice();
		return AylaShare.getReceives(null, devObj, null);
	}
	static public AylaRestService getAllShares(Handler mHandle) {
		AylaDevice devObj = new AylaDevice();
		return AylaShare.getReceives(mHandle, devObj, null);
	}
	// delete an owned or received device share
	public AylaRestService deleteShare(AylaShare deviceShare) {
		return deviceShare.delete();
	}
	public AylaRestService deleteShare(Handler mHandle, AylaShare deviceShare) {
		return deviceShare.delete(mHandle);
	}

	// ---------------------- Device Received Share pass-through methods ------------------------
	// get received shares for this device.dsn
	public AylaRestService getReceivedShares() {
		return AylaShare.getReceives(null, this, null);
	}
	public AylaRestService getReceivedShares(Handler mHandle) {
		return AylaShare.getReceives(mHandle, this, null);
	}
	// get all received device shares for the current user
	static public AylaRestService getAllReceivedShares() {
		AylaDevice devObj = new AylaDevice();
		return AylaShare.getReceives(null, devObj, null);
	}
	static public AylaRestService getAllReceivedShares(Handler mHandle) {
		AylaDevice devObj = new AylaDevice();
		return AylaShare.getReceives(mHandle, devObj, null);
	}
	
	// ---------------------- Device Notification pass-through methods ------------------------
	/**
	 * Same as {@link AylaDevice#createNotification(Handler, AylaDeviceNotification, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService createNotification(AylaDeviceNotification deviceNotification) {
		return deviceNotification.createNotification(null, this, true);
	}

	/**
	 * Same as {@link AylaDevice#createNotification(Handler, AylaDeviceNotification, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService createNotification(Handler mHandle, AylaDeviceNotification deviceNotification) {
		return deviceNotification.createNotification(mHandle, this, false);
	}

	/**
	 * Post a new device notification associated with input param device. See section Device Service - Device Notifications in aAyla Mobile Library document for details.
	 * @param mHandle is where result would be returned.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService createNotification(Handler mHandle, AylaDeviceNotification deviceNotification, Boolean delayExecution) {
		return deviceNotification.createNotification(mHandle, this, delayExecution);
	}
	/**
	 * Same as {@link AylaDevice#updateNotification(Handler, AylaDeviceNotification, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService updateNotification(AylaDeviceNotification deviceNotification) {
		return deviceNotification.updateNotification(null, true);
	}

	/**
	 * Same as {@link AylaDevice#updateNotification(Handler, AylaDeviceNotification, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService updateNotification(Handler mHandle, AylaDeviceNotification deviceNotification) {
		return deviceNotification.updateNotification(mHandle, false);
	}

	/**
	 * Put a device notification associated with input param device. See section Device Service - Device Notifications in aAyla Mobile Library document for details.
	 * @param mHandle is where result would be returned.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService updateNotification(Handler mHandle, AylaDeviceNotification deviceNotification, Boolean delayExecution) {
		return deviceNotification.updateNotification(mHandle, delayExecution);
	}

	/**
	 * Same as {@link AylaDevice#getNotifications(Handler, Map, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService getNotifications(Map<String, String> callParams) {
		return AylaDeviceNotification.getNotifications(null, this, callParams, true);
	}

	/**
	 * Same as {@link AylaDevice#getNotifications(Handler, Map, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getNotifications(Handler mHandle, Map<String, String> callParams) {
		return AylaDeviceNotification.getNotifications(mHandle, this, callParams, false);
	}

	/**
	 * Get all the device notifications associated with the device.
	 * @param mHandle is where result would be returned.
	 * @param callParams is not used. Set to null for now.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService getNotifications(Handler mHandle, Map<String, String> callParams, Boolean delayExecution) {
		return AylaDeviceNotification.getNotifications(mHandle, this, callParams, delayExecution);
	}


	/**
	 * Same as {@link AylaDevice#destroyNotification(Handler, AylaDeviceNotification, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService destroyNotification(AylaDeviceNotification deviceNotification) {
		return deviceNotification.destroyNotification(null, true);
	}

	/**
	 * Same as {@link AylaDevice#destroyNotification(Handler, AylaDeviceNotification, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService destroyNotification(Handler mHandle, AylaDeviceNotification deviceNotification) {
		return deviceNotification.destroyNotification(mHandle, false);
	}

	/**
	 * Destroy a dedicated device notification.
	 * @param mHandle is where result would be returned.
	 * @param deviceNotification is the device notification to be destroyed.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService destroyNotification(Handler mHandle, AylaDeviceNotification deviceNotification, Boolean delayExecution) {
		return deviceNotification.destroyNotification(mHandle, delayExecution);
	}
	
	// ------------------------ Helper Methods ---------------------
	
	/**
	 * @return true if the registered/currentUser is the owner of this device, false otherwise, 
	 * like if this device has been shared with the current user.
	 * */
	public boolean amOwner() {
		boolean amOwner = false;
		if (this.grant == null) {
			amOwner = true;
		}
		return amOwner;
	}
	
	//TODO: this does not make any sense, remove it after verifying no app would be impacted.
	@Deprecated
	public boolean isDevice() {
		return true;
	}
	
	public boolean isWifi() {
		return (this instanceof AylaDeviceGateway || this instanceof AylaDeviceNode) ? false : true;
	}
	
	public boolean isGateway() {
		return false;
	}

	public boolean isNode() {
		return false;
	}
	
	// TODO: hardcode for zigbee is not a good idea. Remove this after refactoring.
	protected boolean isZigbee() {
		return false;
	}
	
	/**
	 * For everyone in AylaDevice.properties, initialize AylaProperty.product_name.
	 * */
	protected void initPropertiesOwner() {
		if (this.properties == null || this.properties.length <1 ) {
			saveToLog("%s, %s, %s, %s.", "W", tag, "initPropertiesOwner", "this.properties null");
			return;
		}
		
		for (int i=0; i<this.properties.length; i++){
			this.properties[i].product_name = this.dsn;
		}
	}// end of initPropertiesOwner
	
	// New LAN mode methods
	void setPropertiesFromCache() {
		String json = AylaCache.get(AML_CACHE_PROPERTY, dsn);
		if ( json != null ) {
			properties = AylaSystemUtils.gson.fromJson(json, AylaProperty[].class);
		}
	}

	//TODO: Now apps do not use it, zigbee uses it. Should not expose to app level, or it will cause device object inconsistency for now.   
	public AylaLanModule getLanModule() {
		return _lanModule;
	}

	void setLanModule(AylaLanModule lanModule) {
		_lanModule = lanModule;
	}


	// ----------------------- Lan Mode Methods --------------------

	private AylaDiscovery _discovery;

	/**
	 * This method enables direct communication with the device after the application/activity has been LAN enabled. Call this message before any other
	 * AylaDevice methods to leverage direct communication. If the direct communication with the device is determined, a standard SUCCESS/FAILURE message is
	 * sent to the AylaLanMode notification handler. Subsequent calls to get property values should wait until LAN Mode enablement has been determined. If
	 * successful direct communication with the device is established, the receipt of a SUCCESS message by the notification handler will signal property changes
	 * from the device. The notification is generic and does not specify the nature of the change. Therefore, the application should immediately perform
	 * getProperties to assess the impact of the changes. See section LAN Mode Support of aAyla Mobile Library document for details.
	 */
	public void lanModeEnable() {

		final AylaDevice d = this.getCopyInDeviceManager();

		if ( d.isLanModeActive() ) {
			// Notify that we're enabled
			String response = "{\"type\":\""  + AylaSystemUtils.AML_NOTIFY_TYPE_SESSION + "\",\"dsn\":\"" + dsn + "\"}";
			AylaNotify.returnToMainActivity(null, response, 200, 0, null);
			return;
		}

		if ( !AylaReachability.isWiFiConnected(null) ) {
			// Fail. No WiFi, no LAN.
			String response = "{\"type\":\""  + AylaSystemUtils.AML_NOTIFY_TYPE_SESSION + "\",\"dsn\":\"" + dsn + "\"}";
			AylaNotify.returnToMainActivity(null, response, 404, 0, null);
			return;
		}

		// Create a discovery object if we don't have one already to find the device on the LAN
		if ( d._discovery == null ) {
			d._discovery = new AylaDiscovery(d);
		}

		AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaDevice", "dsn", d.dsn, "lanModeEnable");
		if (AylaSystemUtils.lanModeState != lanMode.RUNNING) {
			AylaSystemUtils.saveToLog("%s, %s, %s, %s", "I", "AylaDevice", "lanModeState not running", "lanModeEnable");
			String response = "{\"type\":\""  + AylaSystemUtils.AML_NOTIFY_TYPE_SESSION + "\",\"dsn\":\"" + dsn + "\"}";
			AylaNotify.returnToMainActivity(null, response, 404, 0, null);
			return;
		}

		// Make sure we have a LAN module. We probably need to create one unless we were already in LAN mode.
		if ( d.getLanModule() == null ) {
			d.setLanModule(new AylaLanModule(d));
        }

        d.getLanModule().startLanModeSession(AylaRestService.POST_LOCAL_REGISTRATION, false);

        // did enable lan mode for current device
        d.didEnableLanMode();
		String localHostName = String.format("%s%s", d.dsn, ".local"); 
		
		if (d.lanEnabled &&
                AylaReachability.isWiFiConnected(null) &&
                AylaReachability.getDeviceReachability(d) == AML_REACHABILITY_REACHABLE) {
			d.lanModeEnableContinue(localHostName);
		} else {
			d.getLanModule().lanModeSessionFailed();	// device is not reachable, notif
		}
	}

	private void lanModeEnableContinue(String localHostName) {
		_discovery.discoveredLanIp = null;
		_discovery.waitForDiscovery = true;
		_discovery.queryDeviceIpAddress(localHostName);
	}

	/**
	 * This method is called when LAN mode connection to LME device is no longer required. Then library will stop responding any message from or to this device.
	 * All requests will be sent to service after this method is called. See section LAN Mode Support of aAyla Mobile Library document for details.
	 */
	public void lanModeDisable() {
		
		AylaDevice d = this.getCopyInDeviceManager();
		
		AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaDevice", "dsn", d.dsn, "lanModeDisable");
		if (AylaSystemUtils.lanModeState != lanMode.DISABLED ) {
			if (d.properties != null) {
				try {
					// write current property values to cache
					String jsonProperties = AylaSystemUtils.gson.toJson(d.properties, AylaProperty[].class);
					AylaCache.save(AML_CACHE_PROPERTY, d.dsn, jsonProperties);
                    AylaLanModule module = d.getLanModule();
                    if ( module != null ) {
                        module.lanModeDisable();
                        d.setLanModule(null);
                    }
				} catch (Exception e) {
					AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", "AylaDevice", "saveCacheProperties", e.getCause(), "lanModeDisable");
					e.printStackTrace();
				}
				//TODO: let device know session has ended, TBD
			}  else {
				AylaSystemUtils.saveToLog("%s, %s, %s, %s", "I", "AylaDevice", "saveCacheProperties:null", "lanModeDisable");
			}
		}  else {
			AylaSystemUtils.saveToLog("%s, %s, %s, %s", "I", "AylaDevice", "lanModeState:DISABLED", "lanModeDisable");
		}
	}

	/**
	 * pre-condition: newProperties is always latest, no validation on AylaProperty.updatedAt.
	 *
	 * Properties full set: {Pa, Pb, Pc, Pd, Pe}
	 * 
	 * Firstly retrieve {Pa, Pb, Pc}, and so is memory/cache at this time.
	 * And some time later retrieve {Pc`, Pd`, Pe`} ,both memory/cache 
	 * should be updated to {Pa, Pb, Pc`, Pd`, Pe`}
	 * 
	 * @param newProperties newly retrieved properties
	 * */
	public void mergeNewProperties(AylaProperty[] newProperties) {
		if (newProperties == null || newProperties.length <1) {
			return;
		}
		
		if (this.properties == null) {
			this.properties = newProperties;
			return;
		}
		
		Map<String, AylaProperty> tempPropertyMap = new HashMap<String, AylaProperty>();
		for (AylaProperty p : this.properties) {
			tempPropertyMap.put(p.name(), p);
		}
		
		for (AylaProperty p : newProperties) {
			if (tempPropertyMap.containsKey(p.name())) {
				AylaProperty ap = tempPropertyMap.get(p.name());
				if (!TextUtils.isEmpty(p.ackedAt)) {
					ap.ackedAt = p.ackedAt;
				}
				if (!TextUtils.isEmpty(p.dataUpdatedAt)) {
					ap.dataUpdatedAt = p.dataUpdatedAt;
				}
				if (!TextUtils.isEmpty(p.product_name)) {
					ap.product_name = p.product_name;
				}
				if (!TextUtils.isEmpty(p.value)) {
					ap.value = p.value;
				}
				if (p.datapoint != null) {
					ap.datapoint = p.datapoint;
				}
				if (p.datapoints != null) {
					ap.datapoints = p.datapoints;
				}
				tempPropertyMap.put(ap.name(), ap);
			} else {
				tempPropertyMap.put(p.name(), p);
			}
		}
		
		this.properties = tempPropertyMap.values().toArray(new AylaProperty[tempPropertyMap.size()]);
		String nps = AylaSystemUtils.gson.toJson(this.properties, AylaProperty[].class);
		AylaCache.save(AML_CACHE_PROPERTY, this.dsn, nps);
	}// end of mergeNewProperties   
	

	// New lan mode methods
	public boolean isLanModeActive() {
		AylaDevice d = this.getCopyInDeviceManager();
		
		AylaLanModule module = d.getLanModule();
		if ( module == null ) {
			return false;
		}

		return module.isLanModeEnabled();
	}

	protected Integer lanModeWillSendEntity(AylaLanCommandEntity entity) {
		return 200;
	}

	/**
	 * For now, this is serving lan mode internally.
	 * */
	protected Integer updateProperty(final AylaCryptoEncapData data, final boolean isNotified) {
		property = this.findProperty(data.name);
		saveToLog("%s, %s, %s. ", tag, "updateProperty", "device.dsn:" + this.dsn + " propertyName:" + data.name);
		int status = 200;
		if (property != null) {
			saveToLog("%s, %s, %s. ", tag, "updateProperty", "property:\n" + property.toString());
			
			if (TextUtils.isEmpty(property.product_name)) {
				property.product_name = this.dsn;
			}
			
			if (!TextUtils.isEmpty(data.value)) {
				property.value = data.value;
			}
			if ( !TextUtils.isEmpty(data.id) ) {
				property.ackMessage = data.ackMessage;
				property.ackStatus = data.ackStatus;
				property.ackedAt = AylaSystemUtils.gmtFmt.format(new Date());
			}
			if (property.datapoint != null) {
				property.updateDatapointFromProperty();
				property.datapoint.convertValueToType(property);
				if ( isNotified ) {
					if ( !TextUtils.isEmpty(data.devTimeMs) ) {
						property.datapoint.createdAtFromDevice = gmtFmt.format(new Date(Long.parseLong(data.devTimeMs)));
					} 
					property.datapoint.createdAt = AylaSystemUtils.gmtFmt.format(new Date());
					property.datapoint.updatedAt = AylaSystemUtils.gmtFmt.format(new Date());
					
					property.dataUpdatedAt = property.datapoint.createdAt;
				}
				
				if ( !TextUtils.isEmpty(data.id) ) {
					property.datapoint.ackedAt = property.ackedAt;
					property.datapoint.ackStatus = property.ackStatus;
					property.datapoint.ackMessage = property.ackMessage;
					property.datapoint.id = data.id;
				}
			} else {
				if (isNotified) {
					property.dataUpdatedAt = AylaSystemUtils.gmtFmt.format(new Date());
				}
			}
			
			property.lanModeEnable();
		}
		else {
			AylaSystemUtils.saveToLog("%s, %s, %s, %s", "E", tag,  "updateProperty"
					, "Property " + data.name + " not found on device " + this.dsn);
			status = 404; // 404 Not Found
		}
		return status;
	}// end of updateProperty        

	

	/**
	 * pre-condition: AylaDevice object is the one we get from AylaDeviceManager, with properties array initiated properly. 
	 * 
	 * Based on messages from module, either notifications or requests feedbacks, update corresponding attributes. 
	 * 
	 * @param rs null for notifications, callback for requests.
	 * @param status from module, -1 for notification, non-negative result code for request.
	 * @param data pay load in a decrypted result, requests in different types would have different valid fields.  
	 * 
	 * @return status code.  200 if everything is good; 400 indicates this is a failure response; 
	 * 404 if no property matches, 405 if request type not implemented. More TBD. 
	 * */ 
	protected int lanModeUpdateProperty(final AylaRestService rs, final int status, final AylaCryptoEncapData data) {
		AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "I", tag, "lanModeUpdateProperty", "status:" + status + "\nrs:" + rs + "\ndata:" + data);
		
		if (rs == null) { // for notifications          
			String response = "{\"type\":\"" + AML_NOTIFY_TYPE_PROPERTY + "\",\"dsn\":\"" + this.dsn +
					  "\",\"properties\":[\"" + data.name + "\"]}";
			int propertyUpdateStatus = this.updateProperty(data, true);
			AylaNotify.returnToMainActivity(null, response, propertyUpdateStatus, 0, this);
			return propertyUpdateStatus;
		} 
		
		// for request result
		int updateStatus = 200;
		if (status>=200 && status<300) {
			updateStatus = this.updateProperty(data, false);
		} else {
			updateStatus = 400;
		}
		
		List<String> result = new ArrayList<String>();
		if (!TextUtils.isEmpty(data.name)) {
			result.add(data.name);
		}
		
		if ( !TextUtils.isEmpty(data.name) && rs.collectResult(result) == 1) {
			// All cmds for this request are collected, get back to app.
			AylaProperty[] aps = this.getCollectiveResult( rs.getCollectiveResult() );
			if (rs.RequestType == AylaRestService.GET_PROPERTIES_LANMODE) {
				String jsonProperties = AylaSystemUtils.gson.toJson(aps, AylaProperty[].class);
				AylaProperty.returnToMainActivity(rs, jsonProperties, status, 0, false);
			} else if (rs.RequestType == AylaRestService.GET_DATAPOINT_LANMODE
					|| rs.RequestType == AylaRestService.CREATE_DATAPOINT_LANMODE) {
				String jsonDatapoint = AylaSystemUtils.gson.toJson(aps[0].datapoint, AylaDatapoint.class);
				if (rs.RequestType == AylaRestService.GET_DATAPOINT_LANMODE) {
					jsonDatapoint = "[" + jsonDatapoint + "]";
				}
				AylaDatapoint.returnToMainActivity(rs, jsonDatapoint, updateStatus, 0);
			} else {
				saveToLog("%s, %s, %s, %s.", "E", tag, "lanModeUpdateProperty", "requestType " + rs.RequestType + " not found");
				updateStatus = 405;
			}
		}
		
		return updateStatus;
	}// end of lanModeUpdateProperty  
	
	
	
	/**
	 *  pre-condition: this.properties are properly initialized and updated.
	 * 
	 *  Based on the collective property name, get the properties involved in this request.
	 *  @param pns property name that is collected during the process
	 *  @return properties among this.properties that matches the property names, null if invalid input.
	 * */
	protected AylaProperty[] getCollectiveResult(final List<String> pns) {

		if ( pns == null || pns.isEmpty() ) {
			return null;
		}

		AylaProperty[] results = new AylaProperty[pns.size()];        
		
		for (int i=0; i<pns.size(); i++) {
			results[i] = this.findProperty( pns.get(i) );
		}
		
		return results;           
	}// end of getCollectiveResult
	
	
	/**
	 * Return property value on Android side. return whole command string on iOS side
	 * */ 
	protected String lanModeToDeviceUpdate(AylaRestService rs, AylaProperty property, String value, int cmdId) {
		return value;
	}
	
	//TODO: replace implementation with StringBuilder. 
	protected String zCmdToLanModeDevice(String type, String propertyName, String data, String uri, int cmdId) {
		String jsonCommand = "";
		if (AylaLanMode.lanModeState != lanMode.DISABLED) {
			// build property for lan mode device
			jsonCommand = jsonCommand + "{\"cmds\":[";
			jsonCommand = jsonCommand + "{\"cmd\":";
			jsonCommand = jsonCommand + "{\"cmd_id\":" + cmdId + ",";
			jsonCommand = jsonCommand + "\"method\":" + "\""+ type +"\"" + ",";
			jsonCommand = jsonCommand + "\"resource\":" + "\"" + "property.json?name=" + propertyName + "\"" + ",";
			jsonCommand = jsonCommand + "\"data\":" + "\"" + data + "\"" + ",";
			jsonCommand = jsonCommand + "\"uri\":" + "\"" + uri + "\"";
			jsonCommand = jsonCommand + "}}";
			jsonCommand = jsonCommand + "]}";
		}
		return jsonCommand;
	}
	
	
	protected String lanModeToDeviceCmd(AylaRestService rs, String type, String uri, Object obj) {
		String cmd = "";
		if (TextUtils.equals(type, "GET") && TextUtils.equals(uri, "datapoint.json") ) {
			//GET property/datapoint.json
			AylaProperty property = (AylaProperty)obj;
			if(property.datapoint == null) {
				property.datapoint = new AylaDatapoint();
			}
			cmd = property.datapoint.getDatapointFromLanModeDevice( rs, property, this.isNode() ); 
		}
		return cmd;
	}

	
	protected String lanModePropertyNameFromEdptPropertyName(String name) {
		return name;
	}

	/**
	 * Based on the given property name, get the right property name as per the design,
	 * and find the right AylaProperty object based on the revised property name. 
	 * 
	 * @param propName property name in properties
	 * @return The property object the datapoint would be created on, null if there is anything wrong.
	 * */
	protected AylaProperty getPropertyForCreateDatapointOnEndPoints (final String propName) {
		if (TextUtils.isEmpty(propName)) {
			saveToLog("%s, %s, %s, %s.", "W", tag, "getPropertyForCreateDatapointOnEndPoints", "propName is null");
			return null;
		}
		
		return this.findProperty(propName);
	}// end of getPropertyForCreateDatapointOnEndPoints
	
	
	protected void didEnableLanMode()
	{
	    //Nothing to be set in AylaDevice
	}

	protected void didDisableLanMode()
	{
	    //Nothing to be set in AylaDevice
	}
	
	// Return the device property matching the product name
	public AylaProperty findProperty(String propertyName) {
		if ( TextUtils.isEmpty(propertyName) ) 
		{
			saveToLog("%s, %s, %s, %s.", "W", tag, "findProperty", "property name is empty");
			return null;
		}
		
		if ( this.properties == null) {
			saveToLog("%s, %s, %s, %s.", "W", tag, "findProperty", "properties array not initialized on device " + this.dsn);
			return null;
		}
		
		for (int i=0; i<this.properties.length; i++) {
			if (TextUtils.equals(propertyName, this.properties[i].name) ) {
				return this.properties[i];
			}
		}
		
		return null;
	}

	void updateDevicesCacheLanIp(String discoveredLanIp) {
		String savedJsonDeviceContainers;
		savedJsonDeviceContainers =  AylaCache.get(AML_CACHE_DEVICE);
		if (!TextUtils.isEmpty(savedJsonDeviceContainers)) {
			String newJsonDeviceContainers = savedJsonDeviceContainers.replace(this.lanIp, discoveredLanIp);
			this.lanIp = discoveredLanIp;
			AylaCache.save(AML_CACHE_DEVICE, newJsonDeviceContainers);
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "Devices", "discoveredLanIp" , discoveredLanIp, "updateDevicesCacheLanIp");
		}
	}

	// Return the device schedule matching the product name
	public AylaSchedule findSchedule(String scheduleName) {
		if (this.schedules != null) {
			for (AylaSchedule schedule: this.schedules) {
				if ( TextUtils.equals(schedule.name, scheduleName) ) {
					return schedule;
				}
			}
		}
		return null;
	}
}







