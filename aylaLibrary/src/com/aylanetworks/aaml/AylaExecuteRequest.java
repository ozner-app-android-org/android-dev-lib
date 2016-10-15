//
//  AylaExecuteRequests.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 8/15/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//
package com.aylanetworks.aaml;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import org.apache.http.NoHttpResponseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

public class AylaExecuteRequest extends IntentService {
	
	private static final String tag = AylaExecuteRequest.class.getSimpleName();
	
	public static int NETWORK_TIMEOUT_CLOUD 			= 15000;			// 15 sec cloud timeout
	public static int NETWORK_TIMEOUT_LAN				= 2500;				// 2.5 sec LAN timeout

	private int responseCode;
	private int method;
	private byte[] entity;
	private ArrayList <AylaParcelableNVPair> params;
	private ArrayList <AylaParcelableNVPair> headers;
	private ResultReceiver receiver;
	private String url;
	private boolean async;
	private int requestRetryCounter = 0;


	private HttpURLConnection urlConnection = null;

	/* Useful debugging method
	String getMethodString(int method) {
		if (method == AylaRestService.GET_DEVICES)
			return "GET_DEVICES";
		if (method == AylaRestService.POST_USER_LOGIN)
			return "POST_USER_LOGIN";
		if (method == AylaRestService.GET_USER_INFO)
			return "GET_USER_INFO";
		if (method == AylaRestService.GET_USER_METADATA_BY_KEY)
			return "GET_USER_METADATA_BY_KEY";
		if (method == AylaRestService.GET_GROUPS_ZIGBEE)
			return "GET_GROUPS_ZIGBEE";
		if (method == AylaRestService.GET_BINDINGS_ZIGBEE)
			return "GET_BINDINGS_ZIGBEE";
		if (method == AylaRestService.GET_SCENES_ZIGBEE)
			return "GET_SCENES_ZIGBEE";
		if (method == AylaRestService.GET_REGISTERED_NODES)
			return "GET_REGISTERED_NODES";
		if (method == AylaRestService.GET_PROPERTIES)
			return "GET_PROPERTIES";
		if (method == AylaRestService.GET_USER_CONTACT_LIST)
			return "GET_USER_CONTACT_LIST";
		if (method == AylaRestService.PROPERTY_CHANGE_NOTIFIER)
			return "PROPERTY_CHANGE_NOTIFIER";
		if (method == AylaRestService.REACHABILITY)
			return "REACHABILITY";
		if (method == AylaRestService.POST_LOCAL_REGISTRATION)
			return "POST_LOCAL_REGISTRATION";
		if (method == AylaRestService.GET_NODE_PROPERTIES_LANMODE)
			return "GET_NODE_PROPERTIES_LANMODE";
		if (method == AylaRestService.GET_PROPERTIES_LANMODE)
			return "GET_PROPERTIES_LANMODE";
		if (method == AylaRestService.PUT_LOCAL_REGISTRATION)
			return "PUT_LOCAL_REGISTRATION";
		if (method == AylaRestService.GET_DEVICES_LANMODE)
			return "GET_DEVICES_LANMODE";
		if (method == AylaRestService.GET_REGISTRATION_CANDIDATE)
			return "GET_REGISTRATION_CANDIDATE";
		if (method == AylaRestService.GET_MODULE_REGISTRATION_TOKEN)
			return "GET_MODULE_REGISTRATION_TOKEN";
		if (method == AylaRestService.REGISTER_DEVICE)
			return "REGISTER_DEVICE";
		if (method == AylaRestService.REGISTER_NEW_DEVICE)
			return "REGISTER_NEW_DEVICE";
		if (method == AylaRestService.CREATE_DEVICE_NOTIFICATION)
			return "CREATE_DEVICE_NOTIFICATION";
		if (method == AylaRestService.CREATE_APP_NOTIFICATION)
			return "CREATE_APP_NOTIFICATION";
		if (method == AylaRestService.GET_NEW_DEVICE_SCAN_RESULTS_FOR_APS)
			return "GET_NEW_DEVICE_SCAN_RESULTS_FOR_APS";
		if (method == AylaRestService.START_NEW_DEVICE_SCAN_FOR_APS)
			return "START_NEW_DEVICE_SCAN_FOR_APS";
		if (method == AylaRestService.CONNECT_TO_NEW_DEVICE)
			return "CONNECT_TO_NEW_DEVICE";
		if (method == AylaRestService.START_NEW_DEVICE_SCAN_FOR_APS)
			return "START_NEW_DEVICE_SCAN_FOR_APS";
		if (method == AylaRestService.RETURN_HOST_NETWORK_CONNECTION)
			return "RETURN_HOST_NETWORK_CONNECTION";
		if (method == AylaRestService.DELETE_HOST_NETWORK_CONNECTIONS)
			return "DELETE_HOST_NETWORK_CONNECTIONS";
		if (method == AylaRestService.SET_HOST_NETWORK_CONNECTION)
			return "SET_HOST_NETWORK_CONNECTION";
		if (method == AylaRestService.PUT_NEW_DEVICE_TIME)
			return "PUT_NEW_DEVICE_TIME";
		if (method == AylaRestService.GET_NEW_DEVICE_STATUS)
			return "GET_NEW_DEVICE_STATUS";
		if (method == AylaRestService.RETURN_HOST_SCAN)
			return "RETURN_HOST_SCAN";
		if (method == AylaRestService.UNREGISTER_DEVICE)
			return "UNREGISTER_DEVICE";
		if (method == AylaRestService.SEND_NETWORK_PROFILE_LANMODE)
			return "SEND_NETWORK_PROFILE_LANMODE";
		if (method == AylaRestService.DELETE_HOST_NETWORK_CONNECTION)
			return "DELETE_HOST_NETWORK_CONNECTION";
		if (method == AylaRestService.CONNECT_NEW_DEVICE_TO_SERVICE)
			return "CONNECT_NEW_DEVICE_TO_SERVICE";
		if (method == AylaRestService.GET_NEW_DEVICE_CONNECTED)
			return "GET_NEW_DEVICE_CONNECTED";
		if (method == AylaRestService.CONFIRM_NEW_DEVICE_TO_SERVICE_CONNECTION)
			return "CONFIRM_NEW_DEVICE_TO_SERVICE_CONNECTION";

		return ""+method;
	}
	*/

	public AylaExecuteRequest() {
		super("executeRestRequest");
	}

	private final List<Intent> outstandingIntents = new ArrayList<>();

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		synchronized (outstandingIntents) {
			outstandingIntents.add(intent);
		}
		return super.onStartCommand(intent, flags, startId);
	}

	// asynchronous service interface
	@Override
	protected void onHandleIntent(Intent intent) {
		synchronized (outstandingIntents) {
			if(outstandingIntents.isEmpty()){
				return;
			}
			outstandingIntents.remove(0);
			if (outstandingIntents.size() > 0) {
				AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "W", "ExecuteRequest", "This intent URL", intent.getStringExtra("url"), "onHandleIntent");
				for (Intent i : outstandingIntents) {
					AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "W", "ExecuteRequest", "Outstanding intent URL", i.getStringExtra("url"), "onHandleIntent");
				}
			}
		}
		params = intent.getParcelableArrayListExtra("params");
		headers = intent.getParcelableArrayListExtra("headers");
		url = intent.getStringExtra("url");
		receiver = (ResultReceiver) intent.getParcelableExtra("receiver");
		method = (int) intent.getIntExtra("method", 1);
		entity = intent.getByteArrayExtra("entity");
		async = intent.getBooleanExtra("async", true);
		
		// complete for Host based calls
		AylaRestService rs = new AylaRestService();
		rs.jsonResults = intent.getStringExtra("result");
		rs.subTaskFailed = intent.getIntExtra("subTask", 1);
		rs.responseCode = intent.getIntExtra("responseCode", 1);
		rs.info = intent.getStringExtra("info");
		rs.url = url;
		
		try {
			execute(method, rs);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// sync interface
	protected AylaCallResponse handleIntent(Intent intent, AylaRestService rs) throws Exception{
		params = intent.getParcelableArrayListExtra("params");
		headers = intent.getParcelableArrayListExtra("headers");
		url = intent.getStringExtra("url");
		receiver = (ResultReceiver) intent.getParcelableExtra("receiver");
		method = (int) intent.getIntExtra("method", 1);
		entity = intent.getByteArrayExtra("entity");
		async = intent.getBooleanExtra("async", false);
		try {
			return execute(method, rs);
		} catch (Exception e) {
			e.printStackTrace();
			Bundle bundle = new Bundle();
			bundle.putString("result", e.getLocalizedMessage()); //  response
			responseCode = AylaNetworks.AML_GENERAL_EXCEPTION; // djm check response code
			return new AylaCallResponse(responseCode, bundle);
		}
	}

    /**
     * Returns true if the supplied method supports offline results.
     * @param method ID of the method to check
     * @return True if the method supports an offline response, false otherwise
     */
    private static boolean supportsOfflineResponse(int method) {
        switch (method) {
            case AylaRestService.GET_DEVICES_LANMODE:
            case AylaRestService.GET_NODES_LOCAL_CACHE:
            case AylaRestService.CREATE_DATAPOINT_LANMODE:
            case AylaRestService.CREATE_NODE_DATAPOINT_LANMODE:
            	
            case AylaRestService.GET_DATAPOINT_LANMODE:
            case AylaRestService.GET_DATAPOINTS_LANMODE:
            	
            case AylaRestService.GET_PROPERTIES_LANMODE:
            case AylaRestService.GET_NODE_PROPERTIES_LANMODE:
            case AylaRestService.GET_NODE_DATAPOINT_LANMODE:
            	
            case AylaRestService.GET_NODES_CONNECTION_STATUS_ZIGBEE_LANMODE:
            	
            case AylaRestService.GET_PROPERTY_DETAIL_LANMODE:
            case AylaRestService.GET_NODES_CONNECTION_STATUS_LANMODE:
            case AylaRestService.POST_USER_LOGIN:

			// These APIs are used during wifi setup / registration where we may not be
			// connected to the cloud, but rather to the device's wifi AP.
            case AylaRestService.SET_DEVICE_CONNECT_TO_NETWORK:
            case AylaRestService.GET_MODULE_WIFI_STATUS:
            case AylaRestService.START_NEW_DEVICE_SCAN_FOR_APS:
            case AylaRestService.GET_NEW_DEVICE_STATUS:
            case AylaRestService.PUT_NEW_DEVICE_TIME:
            case AylaRestService.DELETE_DEVICE_WIFI_PROFILE:
            case AylaRestService.CONNECT_NEW_DEVICE_TO_SERVICE:
            case AylaRestService.SET_HOST_NETWORK_CONNECTION:
            case AylaRestService.DELETE_HOST_NETWORK_CONNECTION:
            case AylaRestService.DELETE_HOST_NETWORK_CONNECTIONS:
            case AylaRestService.CONFIRM_NEW_DEVICE_TO_SERVICE_CONNECTION:
            	
            case AylaRestService.RETURN_HOST_SCAN:
            case AylaRestService.RETURN_HOST_NETWORK_CONNECTION:
            case AylaRestService.CONNECT_TO_NEW_DEVICE:
            case AylaRestService.GET_NEW_DEVICE_SCAN_RESULTS_FOR_APS:
            case AylaRestService.PUT_DISCONNECT_AP_MODE:
                
            // While this call should require network access, it is made right after re-connecting
			// to the original wifi network during wifi setup, and on certain devices the
			// check for reachability fails before this call is made. Including this API
			// in the list here allows these devices to complete setup.
            case AylaRestService.GET_NEW_DEVICE_CONNECTED:
            case AylaRestService.PROPERTY_CHANGE_NOTIFIER:
            case AylaRestService.PUT_LOCAL_REGISTRATION:
            
            // for same-lan wifi setup
            case AylaRestService.POST_LOCAL_REGISTRATION:
            case AylaRestService.DELETE_NETWORK_PROFILE_LANMODE:
            case AylaRestService.SEND_NETWORK_PROFILE_LANMODE:
            case AylaRestService.GET_NEW_DEVICE_WIFI_STATUS:

            // Confirm reachability state is finalized locally, either service or device, or both. 
            case AylaRestService.REACHABILITY:
                return true;
        }
        return false;
    }
    
    /**
     * Returns true if the supplied method supports LAN mode results.
     * @param method ID of the method to check
     * @return True if the method supports an LAN mode response, false otherwise
     */
    private static boolean supportsLanModeResponse(int method) {
        switch (method) {
            case AylaRestService.PUT_LOCAL_REGISTRATION:
            case AylaRestService.POST_LOCAL_REGISTRATION:
            case AylaRestService.PROPERTY_CHANGE_NOTIFIER:
            case AylaRestService.REACHABILITY:
            case AylaRestService.GET_DEVICES_LANMODE:
            case AylaRestService.GET_NODES_LOCAL_CACHE:
            case AylaRestService.GET_PROPERTIES_LANMODE:
            	
            case AylaRestService.GET_NODE_PROPERTIES_LANMODE:
            case AylaRestService.GET_NODE_DATAPOINT_LANMODE:
            case AylaRestService.CREATE_NODE_DATAPOINT_LANMODE:
            	
            case AylaRestService.GET_PROPERTY_DETAIL_LANMODE:
            case AylaRestService.GET_DATAPOINT_LANMODE:
            case AylaRestService.GET_DATAPOINTS_LANMODE:
            case AylaRestService.CREATE_DATAPOINT_LANMODE:
            case AylaRestService.GET_NODES_CONNECTION_STATUS_LANMODE:
            	
            case AylaRestService.GET_NODES_CONNECTION_STATUS_ZIGBEE_LANMODE:
            	
            // For secure wifi setup
            case AylaRestService.SEND_NETWORK_PROFILE_LANMODE:
            // For delete wifi profile secure session
            case AylaRestService.DELETE_NETWORK_PROFILE_LANMODE:
            	
                return true;
        }
        return false;
    }

	/**
	 * This method checks the method and request to see if it can be processed. If the request
	 * cannot be processed, we return a failure response. Otherwise we return null.
	 * @param method Method to check
	 * @param rs AylaRestService request to check
	 * @return null if the request should be processed, or an error response if the request should
	 * not be processed
	 */
	private AylaCallResponse checkRequest(int method, AylaRestService rs) {
		Bundle responseBundle = null;
		AylaCallResponse commitResponse = null;

		// If this is a LAN mode request other than a notification, and we are not on the LAN, then fail immediately
		if ( (method != AylaRestService.PROPERTY_CHANGE_NOTIFIER) &&
                (method != AylaRestService.SEND_NETWORK_PROFILE_LANMODE) &&
				supportsLanModeResponse(method) &&
				!AylaReachability.isWiFiConnected(null) &&
                // While we are in secure setup mode, wifi connectivity can report strange things
                AylaLanMode.getSecureSetupDevice() == null) {
			AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "E", "ExecuteRequest", "!LAN && supportsLanModeResponse", method, "execute");
			responseBundle = new Bundle();
			responseBundle.putString("result", rs.jsonResults); //  response v3.07
			responseBundle.putInt("subTask", rs.subTaskFailed);
			responseCode = AylaNetworks.AML_ERROR_UNREACHABLE;

			if (async) {
				receiver.send(responseCode, responseBundle);
			} else {
				commitResponse =  new AylaCallResponse(responseCode, responseBundle);
			}
			return commitResponse;
		}

		// If this is request is going to a device directly, make sure the device we want to
		// contact is reachable. First get the URL from the rest service and see if it's a real
		// URL. If so, we'll see if it's addressed to one of our local devices.
		URL destinationURL;
		try {
			destinationURL = new URL(this.url);
		} catch (MalformedURLException ex) {
			// Ignore
			destinationURL = null;
		}

		// If we got a valid URL for the destination, see if it's destined for a local device.
		// We will know that if DeviceManager finds it by the URL host, which will be the
		// device's IP address if this is in fact destined for a device.
		AylaDevice targetDevice = null;
		if ( destinationURL != null ) {
			targetDevice = AylaDeviceManager.sharedManager().deviceWithLanIP(destinationURL.getHost());
			if ( targetDevice != null
			     // Special case for getting the new device registration token during same lan registration flow
			     && method != AylaRestService.GET_MODULE_REGISTRATION_TOKEN
			) {
				// Make sure the target device is reachable via the LAN
				int canReach = AylaReachability.getDeviceReachability(targetDevice);
				if ( canReach != AylaNetworks.AML_REACHABILITY_REACHABLE ) {
					// Throw out this request- we know now that it will fail, and don't want to
					// take the time to wait for it to do so.
					AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s, %s.", "E", "ExecuteRequest",
							"LM device not reachable", destinationURL.toString(),
							"canReach:" + canReach, "execute");
					responseBundle = new Bundle();
					responseBundle.putString("result", rs.jsonResults); //  response v3.07
					responseBundle.putInt("subTask", rs.subTaskFailed);
					responseCode = AylaNetworks.AML_ERROR_UNREACHABLE;

					if (async) {
						receiver.send(responseCode, responseBundle);
					} else {
						commitResponse =  new AylaCallResponse(responseCode, responseBundle);
					}
				}
			}
		}

		// If this is a cloud request, and we can't reach the cloud, fail immediately
		if (!AylaReachability.isCloudServiceAvailable()) {
			if (AylaReachability.isDeviceLanModeAvailable(null) && supportsLanModeResponse(method)) {
				// We aren't connected to the cloud, but we are connected to the LAN mode device
				AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "V", "ExecuteRequest", "lanMode", method, "execute");
			} else if (!supportsOfflineResponse(method)) {
				// Make sure the method supports cached results if we cannot reach the service.\
				// return failure here
				AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "E", "ExecuteRequest", "!cloud && !supportOffline", method, "execute");
				responseBundle = new Bundle();
				responseBundle.putString("result", rs.jsonResults); //  response v3.07
				responseBundle.putInt("subTask", rs.subTaskFailed);
				responseCode = AylaNetworks.AML_ERROR_UNREACHABLE;

				if (async) {
					receiver.send(responseCode, responseBundle);
				} else {
					commitResponse =  new AylaCallResponse(responseCode, responseBundle);
				}
				return commitResponse;
			} else {
				AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "V", "ExecuteRequest", "cached", method, "execute");
			}
		}

		return commitResponse;
	}

	private AylaCallResponse execute(int method, AylaRestService rs)
	{
		// Log.v("AylaExecuteRequest", "dev: execute " + getMethodString(method));

		requestRetryCounter = 3;
		AylaCallResponse commitResponse = null;
		Bundle responseBundle = null;

        if (!AylaReachability.isCloudServiceAvailable()) {
            if (AylaReachability.isDeviceLanModeAvailable(null) && supportsLanModeResponse(method)) {
                // We aren't connected to the cloud, but we are connected to the LAN mode device
                AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "V", "ExecuteRequest", "lanMode", method, "execute");
            } else if (!supportsOfflineResponse(method)) {
                // Make sure the method supports cached results if we cannot reach the service.\
                // return failure here
                AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "E", "ExecuteRequest", "!cloud && !supportOffline", method, "execute");
                responseBundle = new Bundle();
                responseBundle.putString("result", rs.jsonResults); //  response v3.07
                responseBundle.putInt("subTask", rs.subTaskFailed);
                responseCode = AylaNetworks.AML_ERROR_UNREACHABLE;
    
                if (async) {
                    receiver.send(responseCode, responseBundle);
                } else {
                    commitResponse =  new AylaCallResponse(responseCode, responseBundle);
                }
                return commitResponse;
            } else {
                AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "V", "ExecuteRequest", "cached", method, "execute");
            }
        }

		AylaCallResponse failureResponse = checkRequest(method, rs);
		if ( failureResponse != null ) {
			return failureResponse;
		}

		try{
			switch(method)
			{
				case AylaRestService.CREATE_GROUP_ZIGBEE:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("POST", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);
					}

					break;
				case AylaRestService.UPDATE_GROUP_ZIGBEE:
				case AylaRestService.TRIGGER_GROUP_ZIGBEE:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("PUT", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);
					} else {
						sendToReceiver(rs);
					}
					break;
				case AylaRestService.DELETE_GROUP_ZIGBEE:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("DELETE", rs.url, NETWORK_TIMEOUT_CLOUD);
						commitResponse = commit(rs);
					} else {
						sendToReceiver(rs);
					}
					break;
				case AylaRestService.CREATE_BINDING_ZIGBEE:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("POST", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs );

					}
					break;
				case AylaRestService.UPDATE_BINDING_ZIGBEE:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("PUT", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);


					}
					break;
				case AylaRestService.DELETE_BINDING_ZIGBEE:

					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("DELETE", rs.url, NETWORK_TIMEOUT_CLOUD);
						commitResponse = commit(rs );
					} else {
						sendToReceiver(rs);
					}
					break;
				case AylaRestService.CREATE_SCENE_ZIGBEE:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("POST", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);

					}
					break;
				case AylaRestService.UPDATE_SCENE_ZIGBEE:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("PUT", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);
					}
					break;
				case AylaRestService.RECALL_SCENE_ZIGBEE:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("PUT", rs.url, NETWORK_TIMEOUT_CLOUD);
						commitResponse = commit(rs);
					}
					break;
				case AylaRestService.DELETE_SCENE_ZIGBEE:

					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("DELETE", rs.url, NETWORK_TIMEOUT_CLOUD);
						commitResponse = commit(rs);
					} else {
						sendToReceiver(rs);
					}
					break;
				case AylaRestService.CREATE_USER_SHARE:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("POST", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);
					} else { // return errors to handler
						sendToReceiver(rs);
					}
					break;
				case AylaRestService.UPDATE_USER_SHARE:
				{
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("PUT", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);
						break;
					} else { // return errors to handler
						sendToReceiver(rs);
						break;
					}
				}

				case AylaRestService.GET_USER_SHARE:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("GET", rs.url, NETWORK_TIMEOUT_CLOUD);
						commitResponse = commit(rs);
					} else { // return errors to handler
						sendToReceiver(rs);

					}
					break;
				case AylaRestService.DELETE_USER_SHARE:

					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("DELETE", rs.url, NETWORK_TIMEOUT_CLOUD);
						commitResponse = commit(rs);
						break;
					} else { // return errors to handler
						sendToReceiver(rs);
						break;
					}

				case AylaRestService.LOGIN_THROUGH_OAUTH :	// Compound objects, no service API, just return to handler
				{
					sendToReceiver(rs);
					break;
				}
				case AylaRestService.CREATE_USER_CONTACT:
					if (!AylaSystemUtils.ERR_URL.equals(url)) {
						setUrlConnection("POST", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);

					} else {
						sendToReceiver(rs);
					}
					break;
				case AylaRestService.UPDATE_USER_CONTACT:
					if (!AylaSystemUtils.ERR_URL.equals(url)) {
						setUrlConnection("PUT", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);
					} else {
						sendToReceiver(rs);
					}// end of if error_url.equals(url)

				break;
				case AylaRestService.DELETE_USER_CONTACT:
					if (!AylaSystemUtils.ERR_URL.equals(url)) {
						setUrlConnection("DELETE", rs.url, NETWORK_TIMEOUT_CLOUD);
						commitResponse = commit(rs);
					} else {
						sendToReceiver(rs);
					}

					break;
				case AylaRestService.CREATE_USER_METADATA:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("POST", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);
					} else{
						responseBundle = new Bundle();
						sendToReceiver(rs);
					}
					break;
				case AylaRestService.UPDATE_USER_METADATA:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("PUT", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);
					}else{
						sendToReceiver(rs);
					}

					break;
				case AylaRestService.GET_USER_METADATA_BY_KEY:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("GET", rs.url, NETWORK_TIMEOUT_CLOUD);
						commitResponse = commit(rs);
					} else { // return errors to handler
						sendToReceiver(rs);
					}
					break;
				case AylaRestService.DELETE_USER_METADATA:
				{
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("DELETE", rs.url, NETWORK_TIMEOUT_CLOUD);
						commitResponse = commit(rs);
					} else { // return errors to handler
						sendToReceiver(rs);
					}
					break;
				}
				case AylaRestService.CREATE_DEVICE_METADATA:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("POST", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);

					} else { // return errors to handler
						sendToReceiver(rs);
					}
					break;
				case AylaRestService.UPDATE_DEVICE_METADATA:
				{
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("PUT", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);
					} else { // return errors to handler
						sendToReceiver(rs);
					}
					break;
				}
				case AylaRestService.GET_DEVICE_METADATA_BY_KEY:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("GET", rs.url, NETWORK_TIMEOUT_CLOUD);
						commitResponse = commit(rs);
					}  else { // return errors to handler
						sendToReceiver(rs);

					}
					break;
				case AylaRestService.DELETE_DEVICE_METADATA:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("DELETE", rs.url, NETWORK_TIMEOUT_CLOUD);
						commitResponse = commit(rs);
					} else { // return errors to handler
						sendToReceiver(rs);

					}

					break;
				case AylaRestService.CREATE_LOG_IN_SERVICE:
				{
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("POST", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);

					} else { // return errors to handler
						sendToReceiver(rs);
					}
					break;
				}
				case AylaRestService.CREATE_SCHEDULE_ACTION:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("POST", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);
					} else { // return errors to handler
						sendToReceiver(rs);
					}
					break;
				case AylaRestService.UPDATE_SCHEDULE_ACTION:
				{
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("PUT", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);
					} else { // return errors to handler
						sendToReceiver(rs);
					}
				}
				break;
				case AylaRestService.DELETE_SCHEDULE_ACTION:
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("DELETE", rs.url, NETWORK_TIMEOUT_CLOUD);
						commitResponse = commit(rs);
					}
					else { // return errors to handler
						sendToReceiver(rs);
					}
					break;
				//case AylaRestService.CREATE_SCHEDULE:
				case AylaRestService.UPDATE_SCHEDULE:
				case AylaRestService.CLEAR_SCHEDULE:
					//case AylaRestService.DELETE_SCHEDULE:

				{
					if (!url.equals(AylaSystemUtils.ERR_URL)) {

						setUrlConnection("PUT", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);

					} else { // return errors to handler
						sendToReceiver(rs);

					}
				}
				break;
				case AylaRestService.CREATE_SCHEDULE_ACTIONS: // only called on (url.equals(AylaSystemUtils.ERR_URL)
				case AylaRestService.DELETE_SCHEDULE_ACTIONS: // only called on (url.equals(AylaSystemUtils.ERR_URL)

					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("DELETE", rs.url, NETWORK_TIMEOUT_CLOUD);
						commitResponse = commit(rs);

					}  else { // return errors to handler
						sendToReceiver(rs);

					}
					break;

				case AylaRestService.UPDATE_DEVICE:
					if(!url.equals(AylaSystemUtils.ERR_URL)){
						setUrlConnection("PUT", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);

					}  else { // return errors to handler
						sendToReceiver(rs);

					}
					break;

				case AylaRestService.SEND_NETWORK_PROFILE_LANMODE:
				case AylaRestService.DELETE_NETWORK_PROFILE_LANMODE:

				case AylaRestService.GET_DEVICES_LANMODE:
				case AylaRestService.GET_NODES_LOCAL_CACHE:
				case AylaRestService.CREATE_DATAPOINT_LANMODE:
				case AylaRestService.CREATE_NODE_DATAPOINT_LANMODE:

				case AylaRestService.GET_DATAPOINT_LANMODE:
				case AylaRestService.GET_DATAPOINTS_LANMODE:
				case AylaRestService.GET_PROPERTIES_LANMODE:
				case AylaRestService.GET_NODES_CONNECTION_STATUS_ZIGBEE_LANMODE:

				case AylaRestService.GET_NODE_PROPERTIES_LANMODE:
				case AylaRestService.GET_NODE_DATAPOINT_LANMODE:

				case AylaRestService.GET_PROPERTY_DETAIL_LANMODE:
				{
					responseBundle = new Bundle();
					responseBundle.putString("result", rs.jsonResults); //  response v3.07
					responseBundle.putInt("subTask", rs.subTaskFailed);
					responseCode = rs.responseCode;

					if (async) {
						receiver.send(responseCode, responseBundle);
					} else {
						commitResponse =  new AylaCallResponse(responseCode, responseBundle);
					}
					break;
				}
				case AylaRestService.GET_DATAPOINT_BLOB_SAVE_TO_FILE:
				{
					if (!url.equals(AylaSystemUtils.ERR_URL)) {

						setUrlConnection("GET", url, NETWORK_TIMEOUT_CLOUD, false);
						// For debug, reserved for future.
//						AylaSystemUtils.saveToLog("%s, %s, %s, %s, %s, %s.", "D", tag,
//								"getDatapointBlobSaveToFile"
//								, "url:" + urlConnection.getURL()
//								, "method:" + urlConnection.getRequestMethod()
//								, "headers:" + headers);
						commitResponse = commit(rs);
					} else { // return errors to handler
						sendToReceiver(rs);
					}
					break;
				}
				case AylaRestService.GET_DEVICES:
				case AylaRestService.GET_DEVICE_DETAIL:
				case AylaRestService.GET_DEVICE_DETAIL_BY_DSN:
				case AylaRestService.GET_NEW_DEVICE_CONNECTED:
				case AylaRestService.GET_REGISTERED_NODES:
				case AylaRestService.GET_DEVICE_NOTIFICATIONS:
				case AylaRestService.GET_APP_NOTIFICATIONS:
				case AylaRestService.GET_PROPERTIES:
				case AylaRestService.GET_PROPERTY_DETAIL:
				case AylaRestService.GET_DATAPOINTS:
				case AylaRestService.GET_DATAPOINT_BY_ID:

				case AylaRestService.GET_DATAPOINT_BLOB:
				case AylaRestService.GET_PROPERTY_TRIGGERS:
				case AylaRestService.GET_APPLICATION_TRIGGERS:
				case AylaRestService.GET_REGISTRATION_CANDIDATE:
				case AylaRestService.GET_GATEWAY_REGISTRATION_CANDIDATES:

				case AylaRestService.GET_NEW_DEVICE_STATUS:
				case AylaRestService.GET_NEW_DEVICE_SCAN_RESULTS_FOR_APS:
				case AylaRestService.GET_MODULE_WIFI_STATUS:
				case AylaRestService.GET_NEW_DEVICE_PROFILES:
				case AylaRestService.GET_DEVICE_LANMODE_CONFIG:
				case AylaRestService.GET_USER_INFO:
				case AylaRestService.GET_SCHEDULES:
				case AylaRestService.GET_SCHEDULE:
				case AylaRestService.GET_SCHEDULE_ACTIONS:
				case AylaRestService.GET_SCHEDULE_ACTIONS_BY_NAME:
				case AylaRestService.GET_TIMEZONE:
				case AylaRestService.GET_USER_METADATA:
				case AylaRestService.GET_DEVICE_METADATA:
				case AylaRestService.GET_USER_SHARES:
				case AylaRestService.GET_USER_RECEIVED_SHARES:
				case AylaRestService.GET_GROUP_ZIGBEE:
				case AylaRestService.GET_GROUPS_ZIGBEE:
				case AylaRestService.GET_BINDING_ZIGBEE:
				case AylaRestService.GET_BINDINGS_ZIGBEE:
				case AylaRestService.GET_SCENE_ZIGBEE:
				case AylaRestService.GET_SCENES_ZIGBEE:
				case AylaRestService.GET_NODES_CONNECTION_STATUS_ZIGBEE:
				case AylaRestService.GET_USER_CONTACT:
				case AylaRestService.GET_USER_CONTACT_LIST:
				{
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						String combinedParams = "";

						if(params!= null && !params.isEmpty()) {
							combinedParams += "?";
							for(AylaParcelableNVPair p : params) {
								if ( p.getName() != null && p.getValue() != null ) {
									String paramString = p.getName() + "=" + URLEncoder.encode(p.getValue(), "UTF-8");
									if (combinedParams.length() > 1) {
										combinedParams += "&" + paramString;
									} else {
										combinedParams += paramString;
									}
								}
							}
						}
						url+=combinedParams;
						setUrlConnection("GET", url, NETWORK_TIMEOUT_CLOUD);
						commitResponse = commit(rs);

					} else { // return errors to handler
						sendToReceiver(rs);
					}
					break;
				}
				case AylaRestService.GET_MODULE_REGISTRATION_TOKEN:
				{
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						String combinedParams = "";

						if(params!= null && !params.isEmpty()) {
							combinedParams += "?";
							for(AylaParcelableNVPair p : params) {
								if ( p.getName() != null && p.getValue() != null ) {
									String paramString = p.getName() + "=" + URLEncoder.encode(p.getValue(), "UTF-8");
									if (combinedParams.length() > 1) {
										combinedParams += "&" + paramString;
									} else {
										combinedParams += paramString;
									}
								}
							}
						}
						url+=combinedParams;
						setUrlConnection("GET", url, NETWORK_TIMEOUT_LAN);
						commitResponse = commit(rs);

					} else { // return errors to handler
						sendToReceiver(rs);
					}
					break;
				}
				case AylaRestService.POST_USER_LOGIN:
				case AylaRestService.POST_USER_LOGOUT:
				case AylaRestService.POST_USER_SIGNUP:
				case AylaRestService.POST_USER_RESEND_CONFIRMATION:
				case AylaRestService.POST_USER_RESET_PASSWORD:
				case AylaRestService.POST_USER_REFRESH_ACCESS_TOKEN:
				case AylaRestService.POST_USER_OAUTH_LOGIN:
				case AylaRestService.POST_USER_OAUTH_AUTHENTICATE_TO_SERVICE:
				{
					if (!url.equals(AylaSystemUtils.ERR_URL)) {

						setUrlConnection("POST", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);

						break;
					} else { // return user sign-up errors to handler
						sendToReceiver(rs);
						break;
					}
				}
				case AylaRestService.CREATE_DATAPOINT_BLOB_POST_TO_FILE:
				{
			/* Interact with amazon S3
			 * authorization mechanism and param already in url
			 * do not need header.*/
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("PUT", rs.url, NETWORK_TIMEOUT_CLOUD, false);
						writeData();
						commitResponse = commit(rs);
					} else { // return user sign-up errors to handler
						sendToReceiver(rs);
					}
					break;
				}
				case AylaRestService.PUT_USER_CHANGE_PASSWORD:
				case AylaRestService.PUT_RESET_PASSWORD_WITH_TOKEN:
				case AylaRestService.PUT_USER_CHANGE_INFO:
				case AylaRestService.PUT_USER_SIGNUP_CONFIRMATION:
				case AylaRestService.UPDATE_PROPERTY_TRIGGER:
				case AylaRestService.UPDATE_APPLICATION_TRIGGER:
				case AylaRestService.UPDATE_DEVICE_NOTIFICATION:
				case AylaRestService.UPDATE_APP_NOTIFICATION:
				case AylaRestService.PUT_DEVICE_FACTORY_RESET:
				case AylaRestService.BLOB_MARK_FETCHED:
				case AylaRestService.BLOB_MARK_FINISHED:
				case AylaRestService.IDENTIFY_NODE:
				case AylaRestService.UPDATE_USER_EMAIL:
                case AylaRestService.PUT_DISCONNECT_AP_MODE:
				{
					if (!url.equals(AylaSystemUtils.ERR_URL)) {
						setUrlConnection("PUT", rs.url, NETWORK_TIMEOUT_CLOUD);
						writeData();
						commitResponse = commit(rs);
					} else { // return user sign-up errors to handler
						sendToReceiver(rs);
					}
					break;
				}
				case AylaRestService.CREATE_DATAPOINT:
				case AylaRestService.CREATE_BATCH_DATAPOINT:
				case AylaRestService.CREATE_DATAPOINT_BLOB:
				case AylaRestService.CREATE_PROPERTY_TRIGGER:
				case AylaRestService.CREATE_APPLICATION_TRIGGER:
				case AylaRestService.START_NEW_DEVICE_SCAN_FOR_APS:
				case AylaRestService.REGISTER_DEVICE:
					//	case AylaRestService.SET_DEVICE_CONNECT_TO_NETWORK:
				case AylaRestService.POST_LOCAL_REGISTRATION:
				case AylaRestService.OPEN_REGISTRATION_WINDOW:
				case AylaRestService.CREATE_TIMEZONE:
				case AylaRestService.CREATE_DEVICE_NOTIFICATION:
				case AylaRestService.CREATE_APP_NOTIFICATION:
				{
					setUrlConnection("POST", rs.url, NETWORK_TIMEOUT_CLOUD);
					writeData();
					commitResponse = commit(rs);
					break;
				}

				case AylaRestService.SET_DEVICE_CONNECT_TO_NETWORK:
				{
					// request = new HttpPost(url);

					String urlQueryParams = "";
					if (!params.isEmpty()) {
						urlQueryParams += "?";
						for (AylaParcelableNVPair p : params) {
							String paramString = p.getName() + "=" + URLEncoder.encode(p.getValue(), "UTF-8");
							if (urlQueryParams.length() > 1) {
								urlQueryParams += "&" + paramString;
							} else {
								urlQueryParams += paramString;
							}
						}

					}
					url = rs.url + urlQueryParams;
					//request = new HttpPost(url);

					setUrlConnection("POST", url, NETWORK_TIMEOUT_CLOUD);
					writeData();
					commitResponse = commit(rs);
					break;
				}

				case AylaRestService.PUT_LOCAL_REGISTRATION:
				case AylaRestService.PUT_NEW_DEVICE_TIME:
				case AylaRestService.PUT_DATAPOINT: // used to mark blob fetched
				case AylaRestService.UPDATE_TIMEZONE:
				{
					setUrlConnection("PUT", rs.url, NETWORK_TIMEOUT_CLOUD);
					writeData();
					commitResponse = commit(rs);
					break;
				}

				case AylaRestService.DELETE:
				case AylaRestService.DESTROY_DEVICE_NOTIFICATION:
				case AylaRestService.DESTROY_APP_NOTIFICATION:
				case AylaRestService.DESTROY_PROPERTY_TRIGGER:
				case AylaRestService.DESTROY_APPLICATION_TRIGGER:
				case AylaRestService.UNREGISTER_DEVICE:
				case AylaRestService.DELETE_DEVICE_WIFI_PROFILE:
				case AylaRestService.DELETE_USER:
				{
					setUrlConnection("DELETE", rs.url, NETWORK_TIMEOUT_CLOUD);
					commitResponse = commit(rs);
					break;
				}
		//		case AylaRestService.SECURE_SETUP_SESSION_COMPLETED:
				case AylaRestService.PROPERTY_CHANGE_NOTIFIER:
				{
					sendToReceiver(rs);
					break;
				}
				case AylaRestService.REACHABILITY:
				{
					sendToReceiver(rs);
					break;
				}
				case AylaRestService.REGISTER_NEW_DEVICE:			// Compound objects, no service API, just return to handler
				{
					// wait for completion if it's a synchronous call
					if (async == false) {
						while (rs.jsonResults == null) {
							try { //NOP
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							continue;
						}
					}

					responseBundle = new Bundle();
					responseBundle.putString("result", rs.jsonResults); //  response v3.07
					responseBundle.putInt("subTask", rs.subTaskFailed);
					responseCode = rs.responseCode;
					if (async) {
						receiver.send(responseCode, responseBundle);
					} else {
						commitResponse = new AylaCallResponse(responseCode, responseBundle);
					}
					break;
				}
				case AylaRestService.CONNECT_TO_NEW_DEVICE: // Compound objects no service API, just return to handler
				case AylaRestService.RETURN_HOST_WIFI_STATE: // Using host local calls, no service API, just return to handler
				case AylaRestService.RETURN_HOST_SCAN:
				case AylaRestService.RETURN_HOST_NETWORK_CONNECTION:
				case AylaRestService.SET_HOST_NETWORK_CONNECTION:
				case AylaRestService.DELETE_HOST_NETWORK_CONNECTION:
				case AylaRestService.DELETE_HOST_NETWORK_CONNECTIONS:
				case AylaRestService.RETURN_HOST_DNS_CHECK:
				case AylaRestService.GROUP_ACK_ZIGBEE:
				case AylaRestService.BINDING_ACK_ZIGBEE:
				case AylaRestService.SCENE_ACK_ZIGBEE:
				case AylaRestService.GET_NODES_CONNECTION_STATUS:
				{
					// wait for completion if it's a synchronous call
					if (async == false) {
						while (rs.jsonResults == null) {
							try { //NOP
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							continue;
						}
					}

					responseBundle = new Bundle();
					responseBundle.putString("result", rs.jsonResults); //  response v3.07
					responseBundle.putInt("subTask", rs.subTaskFailed);
					responseCode = rs.responseCode;
					if (async) {

						receiver.send(responseCode, responseBundle);
					} else {

						commitResponse = new AylaCallResponse(responseCode, responseBundle);
					}
					break;
				}
				case AylaRestService.CONNECT_NEW_DEVICE_TO_SERVICE:
				case AylaRestService.CONFIRM_NEW_DEVICE_TO_SERVICE_CONNECTION: // Compound object
				case AylaRestService.GET_NEW_DEVICE_WIFI_STATUS: // compound object
				case AylaRestService.GET_NODES_CONNECTION_STATUS_LANMODE:
				{
					responseBundle = new Bundle();
					responseBundle.putString("result", rs.jsonResults); //  response v3.07
					responseBundle.putInt("subTask", rs.subTaskFailed);
					responseCode = rs.responseCode;

					receiver.send(responseCode, responseBundle);
					break;
				}
				default:
					AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "E", tag, "execute", "method " + method + " unknown");
					break;
	}

		}catch(Exception e){
			e.printStackTrace();
			closeResources();
			responseBundle = new Bundle();
			responseBundle.putString("result", rs.jsonResults); //  response v3.07
			responseBundle.putInt("subTask", rs.subTaskFailed);
			responseCode = rs.responseCode;
			if (async) {
				receiver.send(responseCode, responseBundle);
			} else {
				commitResponse = new AylaCallResponse(responseCode, responseBundle);
			}
		} finally{
			closeResources();
		}
		return commitResponse;
	}


	/**
	 * @param rs
	 * @return
	 */
	private AylaCallResponse commit(AylaRestService rs) {
		//Log.v("AylaExecuteRequest", "dev: commit " + getMethodString(method));

        // Get the HTTP client for either cloud or LAN requests (they have different timeouts,
        // thus different clients).

		String initialResponse = null;
		String response = null;
		int responseCode = -1;
		Bundle responseBundle = new Bundle();

		try {
			responseCode = urlConnection.getResponseCode();
			if(responseCode >= 200 && responseCode <300){
				InputStream iStream = urlConnection.getInputStream();
				initialResponse = convertStreamToString(iStream);
				iStream.close();
			// BSK: Uncomment these saveToLog lines to see the time it takes to execute a network request.
			// Failures are currently printed out in the exception handler.
//			AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s:%s %s", "E", "BSK", "url", request.getURI().toString(), "About to execute", "", "commit.NoContent");
//			long timeToExecute = new Date().getTime() - start.getTime();
//			AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s:%d %s", "E", "BSK", "url", request.getURI().toString(), "Time", timeToExecute, "commit.NoContent");

				if(initialResponse != null){

					switch (method) { // remove containers from response
						case AylaRestService.GET_DEVICES:
						case AylaRestService.GET_GATEWAY_REGISTRATION_CANDIDATES:
							response = AylaDevice.stripContainers(initialResponse, method);
							break;
						case AylaRestService.GET_REGISTERED_NODES:
							response = AylaDeviceNode.stripContainers(initialResponse, rs);
							break;
						// case AylaRestService.UPDATE_DEVICE:
						case AylaRestService.GET_DEVICE_DETAIL:
						case AylaRestService.GET_DEVICE_DETAIL_BY_DSN:
						case AylaRestService.GET_REGISTRATION_CANDIDATE:
						case AylaRestService.REGISTER_DEVICE:
						case AylaRestService.GET_NEW_DEVICE_CONNECTED:
							response = AylaDevice.stripContainer(initialResponse, method);
							break;
						case AylaRestService.CREATE_DEVICE_NOTIFICATION:
						case AylaRestService.UPDATE_DEVICE_NOTIFICATION:
							response = AylaDeviceNotification.stripContainer(initialResponse, method);
							break;
						case AylaRestService.GET_DEVICE_NOTIFICATIONS:
							response = AylaDeviceNotification.stripContainers(initialResponse);
							break;
						case AylaRestService.CREATE_APP_NOTIFICATION:
						case AylaRestService.UPDATE_APP_NOTIFICATION:
							response = AylaAppNotification.stripContainer(initialResponse, method);
							break;
						case AylaRestService.GET_APP_NOTIFICATIONS:
							response = AylaAppNotification.stripContainers(initialResponse);
							break;
						case AylaRestService.GET_PROPERTIES:
							response = AylaProperty.stripContainers(initialResponse, rs.info);
							break;
						case AylaRestService.GET_PROPERTY_DETAIL:
							response = AylaProperty.stripContainer(initialResponse);
							break;

						case AylaRestService.GET_DATAPOINTS:
							response = AylaDatapoint.stripContainers(initialResponse);
							break;
						case AylaRestService.CREATE_DATAPOINT:
						case AylaRestService.GET_DATAPOINT_BY_ID:
							response = AylaDatapoint.stripContainer(initialResponse);
							break;
						case AylaRestService.GET_DATAPOINT_BLOB:
						case AylaRestService.CREATE_DATAPOINT_BLOB:
						case AylaRestService.CREATE_DATAPOINT_BLOB_POST_TO_FILE:
						case AylaRestService.BLOB_MARK_FETCHED:
						case AylaRestService.BLOB_MARK_FINISHED:
							response = AylaBlob.stripContainer(initialResponse, method);
							break;

						case AylaRestService.CREATE_PROPERTY_TRIGGER:
						case AylaRestService.UPDATE_PROPERTY_TRIGGER:
							response = AylaPropertyTrigger.stripContainer(initialResponse, method);
							break;
						case AylaRestService.GET_PROPERTY_TRIGGERS:
							response = AylaPropertyTrigger.stripContainers(initialResponse);
							break;
						case AylaRestService.CREATE_APPLICATION_TRIGGER:
						case AylaRestService.UPDATE_APPLICATION_TRIGGER:
							response = AylaApplicationTrigger.stripContainer(initialResponse, method);
							break;
						case AylaRestService.GET_APPLICATION_TRIGGERS:
							response = AylaApplicationTrigger.stripContainers(initialResponse);
							break;
						case AylaRestService.GET_NEW_DEVICE_SCAN_RESULTS_FOR_APS:
							response = AylaModule.stripScanContainerAndReturnAPs(initialResponse);
							break;
						case AylaRestService.GET_MODULE_WIFI_STATUS:
							response = AylaModule.stripDeviceWiFiStatusContainers(initialResponse);
							break;
						case AylaRestService.POST_USER_LOGIN:{

							AylaUser.user.updatedAt = System.currentTimeMillis(); // set timestamp used to calc access token expiry
							response = initialResponse;
						}
						break;
						case AylaRestService.POST_USER_REFRESH_ACCESS_TOKEN:
							AylaUser.user.updatedAt = System.currentTimeMillis(); // set timestamp used to calc access token expiry
							response = initialResponse;
							break;
						case AylaRestService.POST_USER_LOGOUT:
							AylaUser.user.setauthHeaderValue("none"); // nullify the current access_id
							response = initialResponse;
							outstandingIntents.clear();
							stopSelf();
							break;
						case AylaRestService.GET_SCHEDULE:
							response = AylaSchedule.stripContainer(initialResponse, rs.info);
							break;
						case AylaRestService.GET_SCHEDULES:
							response = AylaSchedule.stripContainers(initialResponse);
							break;
						case AylaRestService.GET_SCHEDULE_ACTIONS:
						case AylaRestService.GET_SCHEDULE_ACTIONS_BY_NAME:
							response = AylaScheduleAction.stripContainers(initialResponse);
							break;
						case AylaRestService.UPDATE_SCHEDULE_ACTION:
							response = AylaScheduleAction.stripContainer(initialResponse);
							break;
						case AylaRestService.CREATE_TIMEZONE:
						case AylaRestService.UPDATE_TIMEZONE:
						case AylaRestService.GET_TIMEZONE:
							response = AylaTimezone.stripContainer(initialResponse);
							break;
						case AylaRestService.CREATE_DEVICE_METADATA:
						case AylaRestService.UPDATE_DEVICE_METADATA:
						case AylaRestService.GET_DEVICE_METADATA_BY_KEY:
						case AylaRestService.GET_DEVICE_METADATA:
						case AylaRestService.CREATE_USER_METADATA:
						case AylaRestService.UPDATE_USER_METADATA:
						case AylaRestService.GET_USER_METADATA_BY_KEY:
						case AylaRestService.GET_USER_METADATA:
							Log.d("HTTPCLIENT", "CRAETE?UPDATE datum initialResponse " + initialResponse);
							response = AylaDatum.stripContainer(initialResponse, method);
							break;
						case AylaRestService.CREATE_USER_SHARE:
						case AylaRestService.UPDATE_USER_SHARE:
						case AylaRestService.GET_USER_SHARE:
						case AylaRestService.GET_USER_SHARES:
						case AylaRestService.GET_USER_RECEIVED_SHARES:
							response = AylaShare.stripContainer(initialResponse, method);
							break;
						case AylaRestService.GET_USER_CONTACT:
						case AylaRestService.GET_USER_CONTACT_LIST:
						case AylaRestService.CREATE_USER_CONTACT:
						case AylaRestService.UPDATE_USER_CONTACT:
						case AylaRestService.DELETE_USER_CONTACT:
							response = AylaContact.stripContainer(initialResponse, method);
							break;
						case AylaRestService.CREATE_GROUP_ZIGBEE:
						case AylaRestService.UPDATE_GROUP_ZIGBEE:
						case AylaRestService.GET_GROUP_ZIGBEE:
						case AylaRestService.DELETE_GROUP_ZIGBEE:
							response = AylaCommProxy.stripGroupContainer(initialResponse, method);
							break;
						case AylaRestService.GET_GROUPS_ZIGBEE:
							response = AylaCommProxy.stripGroupContainers(initialResponse);
							break;
						case AylaRestService.GET_NODES_CONNECTION_STATUS_ZIGBEE:
							// TODO: inherit AylaExecuteRequestZigbee and maitain encapsulation and zigbee operation code 
							response = AylaCommProxy.extractNodeConnectionStatusResponse(initialResponse, rs.info, responseCode);
							break;
						case AylaRestService.CREATE_BINDING_ZIGBEE:
						case AylaRestService.UPDATE_BINDING_ZIGBEE:
						case AylaRestService.GET_BINDING_ZIGBEE:
						case AylaRestService.DELETE_BINDING_ZIGBEE:
							response = AylaCommProxy.stripBindingContainers(initialResponse, method);
							break;
						case AylaRestService.GET_BINDINGS_ZIGBEE:
							response = AylaCommProxy.stripBindingContainers(initialResponse, method);
							break;
						case AylaRestService.CREATE_SCENE_ZIGBEE:
						case AylaRestService.UPDATE_SCENE_ZIGBEE:
					//	case AylaRestService.RECALL_SCENE_ZIGBEE:
						case AylaRestService.GET_SCENE_ZIGBEE:
						case AylaRestService.DELETE_SCENE_ZIGBEE:
							response = AylaCommProxy.stripSceneContainer(initialResponse, method);
							break;
						case AylaRestService.GET_SCENES_ZIGBEE:
							response = AylaCommProxy.stripSceneContainers(initialResponse);
							break;
						default:
							response = initialResponse; // no container
					}
				}
			}
			else{
				InputStream iStream = urlConnection.getErrorStream();
				response = convertStreamToString(iStream);
				iStream.close();

			}
			responseBundle.putString("result", response);
			if (async) {
				receiver.send(responseCode, responseBundle);
			}
			return  new AylaCallResponse(responseCode, responseBundle);

		} catch (IOException e) {
			// *********************check how to handle retry (httpContext) in HttpUrlConnection*******************
			String eMsg = (e.getLocalizedMessage() == null) ? e.toString() : e.getLocalizedMessage();
			if (retryRequest(e, eMsg, requestRetryCounter, responseCode)) {
				requestRetryCounter--;
				AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%s %s", "E", "ExecuteRequest", "Error", AylaNetworks.AML_IO_EXCEPTION, "eMsg", eMsg, "commit.retry");
				AylaSystemUtils.sleep(1000);

				return commit(rs);
			} else {
				AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%s %s", "E", "ExecuteRequest", "Error", AylaNetworks.AML_IO_EXCEPTION, "eMsg", eMsg, "commit.IoException");

				responseBundle.putString("result", eMsg);
				if (async) {
					receiver.send(AylaNetworks.AML_IO_EXCEPTION, responseBundle);
				}

				closeResources();
				e.printStackTrace();
				return new AylaCallResponse(AylaNetworks.AML_IO_EXCEPTION, responseBundle);
			}

		} catch(Exception e){
				String eMsg = (e.getLocalizedMessage() == null) ? e.toString() : e.getLocalizedMessage();
				AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%s", "E", "ExecuteRequest", "Error", AylaNetworks.AML_GENERAL_EXCEPTION, "eMsg", eMsg, "commit.GeneralException");

				responseBundle.putString("result", eMsg);
				if (async) {
					receiver.send(AylaNetworks.AML_GENERAL_EXCEPTION, responseBundle);
				}
				closeResources();
				return  new AylaCallResponse(AylaNetworks.AML_GENERAL_EXCEPTION, responseBundle);
			}finally{
				closeResources();
			}
	}

	// Retry I/O exception errors
    private static HashSet<Class<?>> exceptionWhitelist = new HashSet<Class<?>>();
    private static HashSet<Class<?>> exceptionBlacklist = new HashSet<Class<?>>();

    static {
        // Retry if the server dropped connection on us
        exceptionWhitelist.add(NoHttpResponseException.class);
        // retry-this, since it may happens as part of a Wi-Fi to 3G failover
        exceptionWhitelist.add(UnknownHostException.class);
        // retry-this, since it may happens as part of a Wi-Fi to 3G failover
        exceptionWhitelist.add(SocketException.class);

        // never retry timeouts
        exceptionBlacklist.add(InterruptedIOException.class);
        // never retry SSL handshake failures
        exceptionBlacklist.add(SSLHandshakeException.class);
    }


	private Boolean retryRequest(IOException exception, String eMsg, int requestRetryCounter, int responseCode) {
		Boolean retry = true;

      /* Boolean b = (Boolean) context.getAttribute(ExecutionContext.
               HTTP_REQ_SENT);
       Boolean sent = (b != null && b.booleanValue());*/

		Boolean sent = true;

		//modify responseCode check
		//default value
		if(responseCode == -1){
			sent = false;
		}

		if(requestRetryCounter <= 0) {
			// Do not retry if over max retry count
			retry = false;
		} else if (exceptionBlacklist.contains(exception.getClass())) {
			// immediately cancel retry if the error is blacklisted
			retry = false;
		} else if (eMsg.contains("timed out") ||
				eMsg.contains("refused"))
		{
			// catch all time out cases
			retry = false;
		} else if ( responseCode == 104 ) {  // Connection reset by peer
			retry = true;
		} else if (exceptionWhitelist.contains(exception.getClass())) {
			// immediately retry if error is white listed
			retry = true;
		}  else if (!sent) {
			// for most other errors, retry
			retry = true;
		}

		return retry;
	}
    
    // Stop the Intent Service when called by garbage collection
    protected void finalize() { 
    	stopSelf(); 
    } 
    
    // ----------------- Blob Support -------------------------------
	private static String convertStreamToString(InputStream is) {

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				String eMsg = (e.getLocalizedMessage() == null) ? e.toString() : e.getLocalizedMessage();
				AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%s %s", "E", "ExecuteRequest", "Error", AylaNetworks.AML_GENERAL_EXCEPTION, "eMsg", eMsg, "commit.convertStreamToString");
			}
		}
		return sb.toString();
	}

	private void setUrlConnection(String methodName
			, String url
			, int timeout) throws IOException {
		setUrlConnection(methodName, url, timeout, true);
	}

	private void setUrlConnection(String methodName
			, String url
			, int timeout
			, boolean addAylaHeader)
			throws
			IOException {
		closeResources();
		URL requestUrl = new URL(url);
		urlConnection = (HttpURLConnection) requestUrl.openConnection();
		urlConnection.setConnectTimeout(timeout);
        urlConnection.setReadTimeout(5000);
		System.setProperty("http.keepAlive", "false");
		urlConnection.setRequestMethod(methodName);
		switch(methodName){
			case "POST":
				urlConnection.setDoOutput(true);
				urlConnection.setDoInput(true);
				break;
			case "PUT":
				urlConnection.setDoOutput(true);
				urlConnection.setDoInput(true);
				break;
			case "GET":
				urlConnection.setDoInput(true);
				break;
			case "DELETE":
				urlConnection.setDoInput(true);
				break;
		}
		for (AylaParcelableNVPair h : headers) {
			if ("Authorization".equalsIgnoreCase(h.getName())) {
				if (addAylaHeader) {
					urlConnection.setRequestProperty(h.getName(), h.getValue());
				}
			} else {
				urlConnection.setRequestProperty(h.getName(), h.getValue());
			}

		}
	}

	private void writeData() throws IOException {
		if (!params.isEmpty() || entity!=null) {
			OutputStream output = null;
			try {
				output = urlConnection.getOutputStream();
				if (!params.isEmpty()) {
					output.write(params.toString().getBytes("UTF-8"));
				}
				if ( entity != null ) {
					output.write(entity);
				}
			} finally {
				try {
					if (output != null) {
						output.close();
						output = null;
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		} else {
			// nothing to do here...
			Log.d("AyalExecuteRequest", "writeData: nothing to do.");
		}
	}

	private void closeResources(){
		if(urlConnection != null){
			urlConnection.disconnect();
			urlConnection = null;
		}
	}

	private void sendToReceiver(AylaRestService rs){
		Bundle responseBundle = new Bundle();
		responseBundle = new Bundle();
		responseBundle.putString("result", rs.jsonResults); //  response v3.07
		responseBundle.putInt("subTask", rs.subTaskFailed);
		responseCode = rs.responseCode;

		receiver.send(responseCode, responseBundle);
	}
}

