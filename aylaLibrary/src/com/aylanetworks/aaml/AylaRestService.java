//
//  AylaRestService.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 8/15/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//
package com.aylanetworks.aaml;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.ResultReceiver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 
 * Asynchronous Rest Service using Intent
 *
 */
public class AylaRestService {
	
	private static final String tag = AylaRestService.class.getSimpleName();
	
	private ArrayList <AylaParcelableNVPair> params;
	private ArrayList <AylaParcelableNVPair> headers;

	String url;
	Handler mHandler;
	private Context mContext;
	public int RequestType;
	private byte[] entity;
	private List<String> mCollectiveResult;

	// For now, this is for lan mode only. 
	private boolean isExpired; 
	private AylaTimer mTimer = null;
	
	/*
	 * One request might map multiple commands, if there is at least one fails, set this to false.
	 * */
	private boolean mIsRequestComplete = true; 
	
	/* A simplified alternative for the RequestID, +1 when inserting one lan mode command, -1 when receiving one lan mode command */
	private int mCommandCounter;
	/* This will only increase, and used for calculating lan mode time out.*/
	private int mMaxCommand;
	
	/* <cmdID, cmdEntity> */
	private Map<String, AylaLanCommandEntity> mCmdEntityMap; 
	
	public String jsonResults;
	public int responseCode;
	public int subTaskFailed;
	public String info;
	
	/**
	 * method/request identifiers: must be unique
	 */
	final static int GET_DEVICES = 100;
	final static int GET_DEVICE_DETAIL = 101;
	final static int GET_NEW_DEVICE_CONNECTED = 102;
	final static int GET_REGISTERED_NODES = 103;
	final static int GET_DEVICE_DETAIL_BY_DSN = 104;
	final static int GET_DEVICE_METADATA_BY_KEY = 105;
	final static int GET_DEVICE_METADATA = 106;
	final static int GET_PROPERTIES = 110;
	final static int GET_PROPERTY_DETAIL = 111;
	final static int GET_DATAPOINTS = 120;
	final static int GET_DATAPOINT_BLOB = 121;
	final static int GET_DATAPOINT_BLOB_SAVE_TO_FILE = 123;
	final static int GET_PROPERTY_TRIGGERS = 150;
	final static int GET_DEVICE_NOTIFICATIONS = 153;
	final static int GET_APPLICATION_TRIGGERS = 170;
	final static int GET_APP_NOTIFICATIONS = 173;
	final static int GET_REGISTRATION_CANDIDATE = 180;
	final static int GET_MODULE_REGISTRATION_TOKEN = 181;
	final static int GET_GATEWAY_REGISTRATION_CANDIDATES = 182;
	final static int GET_NEW_DEVICE_STATUS = 200; // module
	final static int GET_NEW_DEVICE_SCAN_RESULTS_FOR_APS = 201;
	final static int GET_MODULE_WIFI_STATUS = 202;
	final static int GET_NEW_DEVICE_PROFILES = 203;
	final static int GET_DEVICE_LANMODE_CONFIG = 220;
	final static int GET_USER_INFO = 230;
	final static int GET_USER_METADATA_BY_KEY = 231;
	final static int GET_USER_METADATA = 232;
	final static int GET_SCHEDULES = 240;
	final static int GET_SCHEDULE = 241;
	final static int GET_SCHEDULE_ACTIONS = 245; 
	final static int GET_SCHEDULE_ACTIONS_BY_NAME = 246;
	final static int GET_TIMEZONE = 247;
	final static int GET_USER_SHARE = 255;
	final static int GET_USER_SHARES = 256;
	final static int GET_USER_RECEIVED_SHARES = 258;
	final static int GET_USER_CONTACT = 260;
	final static int GET_USER_CONTACT_LIST = 261;

	final static int POST_USER_LOGIN = 500;
	final static int POST_USER_LOGOUT = 501;
	final static int POST_USER_SIGNUP = 502;
	final static int POST_USER_RESEND_CONFIRMATION = 503;
	final static int POST_USER_RESET_PASSWORD = 504;
	final static int POST_USER_REFRESH_ACCESS_TOKEN = 505;
	final static int PUT_RESET_PASSWORD_WITH_TOKEN = 506;
	final static int POST_USER_OAUTH_LOGIN = 507;
	final static int POST_USER_OAUTH_AUTHENTICATE_TO_SERVICE = 508;
	final static int CREATE_DATAPOINT = 520;
	final static int CREATE_DATAPOINT_BLOB = 521;
	final static int CREATE_DATAPOINT_BLOB_POST_TO_FILE = 523;
	final static int CREATE_BATCH_DATAPOINT = 524;
	
	final static int CREATE_PROPERTY_TRIGGER = 530;
	final static int CREATE_DEVICE_NOTIFICATION = 532;
	final static int CREATE_APPLICATION_TRIGGER = 540;
	final static int CREATE_APP_NOTIFICATION = 542;
	final static int START_NEW_DEVICE_SCAN_FOR_APS = 550;
	final static int REGISTER_DEVICE = 555;
	final static int SET_DEVICE_CONNECT_TO_NETWORK = 560; 			// connect module to service
	final static int POST_LOCAL_REGISTRATION = 570; 				// initiate a session with a new device
	final static int OPEN_REGISTRATION_WINDOW = 575;
	//final static int CREATE_SCHEDULE = 580; 						//temporary, not supported
	final static int CREATE_SCHEDULE_ACTION = 585;
	final static int CREATE_SCHEDULE_ACTIONS = 586;
	final static int CREATE_LOG_IN_SERVICE = 590;
	final static int CREATE_TIMEZONE = 595;
	final static int CREATE_DEVICE_METADATA = 600;
	final static int CREATE_USER_METADATA = 601;
	final static int CREATE_USER_SHARE = 605;
	final static int CREATE_USER_CONTACT = 606;

	final static int PUT_LOCAL_REGISTRATION = 700;
	final static int PUT_NEW_DEVICE_TIME = 701;
	final static int PUT_USER_CHANGE_PASSWORD = 710;
	final static int PUT_USER_CHANGE_INFO = 711;
	final static int PUT_DATAPOINT = 712;
	final static int PUT_USER_SIGNUP_CONFIRMATION = 713;
	final static int UPDATE_PROPERTY_TRIGGER = 714;
	final static int UPDATE_APPLICATION_TRIGGER = 715;
	final static int UPDATE_DEVICE_NOTIFICATION = 716;
	final static int UPDATE_APP_NOTIFICATION = 717; 
	final static int UPDATE_DEVICE = 720;
	final static int PUT_DEVICE_FACTORY_RESET = 722;
	final static int UPDATE_SCHEDULE = 730;
	final static int CLEAR_SCHEDULE = 731;
	final static int UPDATE_SCHEDULE_ACTION = 732;
	final static int UPDATE_TIMEZONE = 735;
	final static int UPDATE_DEVICE_METADATA = 740;
	final static int UPDATE_USER_METADATA = 741;
	final static int UPDATE_USER_SHARE = 745;
	final static int IDENTIFY_NODE = 750;
	public final static int IDENTIFY_ZIGBEE_NODE = 750;
	final static int UPDATE_USER_CONTACT = 754;
	final static int UPDATE_USER_EMAIL = 755;
  final static int PUT_DISCONNECT_AP_MODE=756;

	final static int DELETE = 800;
	final static int DESTROY_PROPERTY_TRIGGER = 810;
	final static int DESTROY_DEVICE_NOTIFICATION = 812;
	final static int DESTROY_APPLICATION_TRIGGER = 815;
	final static int DESTROY_APP_NOTIFICATION = 817;
	final static int UNREGISTER_DEVICE= 830;
	final static int DELETE_DEVICE_WIFI_PROFILE = 835;
	final static int DELETE_USER = 840;
	//final static int DELETE_SCHEDULE = 841; 							// temporary, not supported
	final static int DELETE_SCHEDULE_ACTION = 845;
	final static int DELETE_SCHEDULE_ACTIONS = 846;
	final static int DELETE_DEVICE_METADATA = 850;
	final static int DELETE_USER_METADATA = 851;
	final static int DELETE_USER_SHARE = 855;
	final static int DELETE_USER_CONTACT = 857;
	final static int BLOB_MARK_FETCHED = 858;							// Once marked fetched, one would never get the blob, equals to delete.
	final static int BLOB_MARK_FINISHED = 859;

	final static int REGISTER_NEW_DEVICE = 900; 						// compound call for GET_REGISTRATION_CANDIDATE, GET_MODULE_REGISTRATION_TOKEN, & REGISTER_DEVICE
	final static int CONNECT_TO_NEW_DEVICE = 905; 						// compound call
	final static int CONNECT_NEW_DEVICE_TO_SERVICE = 910; 				// compound call
	final static int CONFIRM_NEW_DEVICE_TO_SERVICE_CONNECTION = 915; 	// Don't send auth_token in the clear if secureSetup is false
	final static int GET_NEW_DEVICE_WIFI_STATUS = 920;
	final static int LOGIN_THROUGH_OAUTH = 930;							// compound call for AML_POST_OAUTH_PROVIDER_URL

	final static int NO_AUTH_TOKEN_ABOVE_THIS_ID = 1500;
	final static int RETURN_HOST_WIFI_STATE = 1500;
	final static int RETURN_HOST_SCAN = 1501; 							// return a wifi scan from the local device: phone/tablet/etc
	final static int RETURN_HOST_NETWORK_CONNECTION = 1502;
	final static int SET_HOST_NETWORK_CONNECTION = 1503;
	final static int DELETE_HOST_NETWORK_CONNECTION = 1504;
	final static int DELETE_HOST_NETWORK_CONNECTIONS = 1505;
	final static int RETURN_HOST_DNS_CHECK = 1506;
	
	// General callbacks for LAN Mode, device & service reachability, and push notifications
	final static int PROPERTY_CHANGE_NOTIFIER = 2000; 					// Don't send auth_token in the clear on id's > 2000
	final static int REACHABILITY = 2001;
	final static int PUSH_NOTIFICATION = 2002;
	
	 
//	final static int LAN_MODE_SESSION_ESTABLISHED = 2003;

	final static int DELETE_NETWORK_PROFILE_LANMODE = 2098;				// 
	final static int SEND_NETWORK_PROFILE_LANMODE = 2099;				// for secure wifi setup. 
	final static int GET_DEVICES_LANMODE = 2100;						// lan mode values. 
	final static int GET_DEVICE_DETAIL_LANMODE = 2101;
	final static int GET_NEW_DEVICE_CONNECTED_LANMODE = 2102;
	final static int GET_NODES_LOCAL_CACHE = 2103;
	public final static int GET_PROPERTIES_LANMODE = 2110;
	final static int GET_PROPERTY_DETAIL_LANMODE = 2111;
	public static final int GET_NODE_PROPERTIES_LANMODE = 2112;
	public final static int GET_DATAPOINT_LANMODE = 2120;
	public final static int GET_NODE_DATAPOINT_LANMODE = 2123;
	final static int GET_DATAPOINTS_LANMODE = 2121;
	final static int GET_DATAPOINT_BY_ID = 2122;      
	
	final static int GET_PROPERTY_TRIGGERS_LANMODE = 2150;
	final static int GET_APPLICATION_TRIGGERS_LANMODE = 2170;
	final static int GET_REGISTRATION_CANDIDATE_LANMODE = 2180;
	final static int GET_MODULE_REGISTRATION_TOKEN_LANMODE = 2181;
	final static int GET_NEW_DEVICE_STATUS_LANMODE = 2200; // module
	final static int GET_NEW_DEVICE_SCAN_RESULTS_FOR_APS_LANMODE = 2201;
	final static int GET_MODULE_WIFI_STATUS_LANMODE = 2202;
	final static int GET_NEW_DEVICE_PROFILES_LANMODE = 2203;
	

	final static int CREATE_DATAPOINT_LANMODE = 2520;
	final static int CREATE_NODE_DATAPOINT_LANMODE = 2521;
	
	// zigbee request ids
	public final static int CREATE_GROUP_ZIGBEE = 3000;
	public final static int CREATE_BINDING_ZIGBEE = 3005;
	public final static int CREATE_SCENE_ZIGBEE = 3010;
	
	public final static int UPDATE_GROUP_ZIGBEE = 3100;
	public final static int UPDATE_BINDING_ZIGBEE = 3105;
	public final static int UPDATE_SCENE_ZIGBEE = 3110;
	public final static int RECALL_SCENE_ZIGBEE = 3115;
	public final static int TRIGGER_GROUP_ZIGBEE = 3120;
	
	public final static int GET_GROUP_ZIGBEE = 3200;
	public final static int GET_BINDING_ZIGBEE = 3205;
	public final static int GET_SCENE_ZIGBEE = 3210;
	
	public final static int GET_GROUPS_ZIGBEE = 3300;
	public final static int GET_BINDINGS_ZIGBEE = 3305;
	public final static int GET_SCENES_ZIGBEE = 3310;
	
	public final static int DELETE_GROUP_ZIGBEE = 3400;
	public final static int DELETE_BINDING_ZIGBEE = 3405;
	public final static int DELETE_SCENE_ZIGBEE = 3410;
	
	public final static int GROUP_ACK_ZIGBEE = 3500;			// compound call
	public final static int BINDING_ACK_ZIGBEE = 3505;			// compound call
	public final static int SCENE_ACK_ZIGBEE = 3510;			// compound call
	
	public final static int GET_NODES_CONNECTION_STATUS = 3600;	// compound call
	public final static int GET_NODES_CONNECTION_STATUS_LANMODE = 3601;
	
	//TODO: Why do we need these ints to be public? except that required in zigbee space.
	public final static int GET_NODES_CONNECTION_STATUS_ZIGBEE = 3602;
	//TODO: For some cases, cloud and lan mode goes different handling flow, we do not need to setup separate operation code for lan mode. 
	public final static int GET_NODES_CONNECTION_STATUS_ZIGBEE_LANMODE = 3603;
	
	
	// end method/request identifiers

	public AylaRestService() {
		this(null, AylaSystemUtils.ERR_URL, 0, "");
	}
	
	public AylaRestService(Handler mHandler, String url,int requestType, String info) {
    	this(mHandler, url, requestType);
		this.info = info;
	}
	
	public AylaRestService(Handler mHandler, String url,int requestType) {
		mCmdEntityMap = new HashMap<String, AylaLanCommandEntity>();        
		
		this.isExpired = false;
		this.mIsRequestComplete = true;
		this.mCommandCounter = 0;
		this.mMaxCommand = 0;
		
		this.mCollectiveResult = null;
		
		this.mHandler = mHandler;
		this.mContext = AylaNetworks.appContext;
		this.url = url;
		this.RequestType = requestType;
		params = new ArrayList<AylaParcelableNVPair>();
		headers = new ArrayList<AylaParcelableNVPair>();
		// assume JSON
		if ( this.RequestType == AylaRestService.CREATE_DATAPOINT_BLOB_POST_TO_FILE ) {
			this.addHeader("Content-Type", "application/octet-stream");
			this.addHeader("Accept", "application/octet-stream");
		} else if (this.RequestType == AylaRestService.GET_DATAPOINT_BLOB_SAVE_TO_FILE) {
			// Intentionally nothing for GET_DATAPOINT_BLOB_SAVE_TO_FILE
		} else {
			this.addHeader("Accept", "application/json");
			this.addHeader("Content-Type","application/json");
		}

		this.addHeader("Connection", "Keep-Alive");
		if (url.startsWith("https")) {
			this.addHeader("Authorization", AylaUser.user.getauthHeaderValue());
		}else {
			this.addHeader("Authorization", "none");
		}
	}
	
	
	void updateCmdEntity(final String cmdId, AylaLanCommandEntity entity) {
		mCmdEntityMap.put(cmdId, entity);
	}
	
	AylaLanCommandEntity getCmdEntity(final String cmdId) {
		return mCmdEntityMap.get(cmdId);
	}
	
	/**
	 * This now becomes a highlight indicating where we use it for lan mode communication.
	 * Initiates all data structures necessary for some lan mode functionalities, service 
	 * call would not initiate as they are for lan mode only. 
	 * 
	 * Note that ALL lan mode commands must call this to setup the <cmdID, rs> mapping!
	 * <br/>
	 * 
	 * @return the object itself when setting the lan mode cmd queue
	 * */  
	synchronized AylaRestService lanModeInit() {
		if (this.mCollectiveResult == null) {
			this.mCollectiveResult = new ArrayList<String>();
		}
		
		this.mCommandCounter ++;
		this.mMaxCommand ++;
		
		if (mTimer == null) {
			mTimer = new AylaTimer(getSuggestedTimeOutInterval()
					, new Runnable(){
						@Override
						public void run() {
							timeOutReturnToMainActivity();
							isExpired = true;
						}
			});
			mTimer.start();
		} else {
			mTimer.setInterval(getSuggestedTimeOutInterval());
		}
		
		return this;
	}// end of lanModeInit           
	
	private Runnable onLanModeTimeOut = new Runnable() {
		@Override
		public void run() {
			
		}
	};
	
	// Match iOS function name.
	private long getSuggestedTimeOutInterval() {
		return AylaNetworks.DEFAULT_LAN_MODE_TIME_OUT*1000 + AylaNetworks.DEFAULT_LAN_MODE_TIME_OUT_FACTOR_PER_CMD*mMaxCommand;
	}
	
	// NOTE: Now it is not required in zigbee package, but it might in the near future, so put it public.
	public boolean isRequestComplete() {
		return this.mIsRequestComplete;
	}
	
	/**
	 * pre-condition: AylaDevice.properties are properly initialized and updated, cmd results are separated by "," 
	 * so no "," in param string under any circumstance.  
	 * 
	 * Collect result for lan mode commands.  
	 * 
	 * @param results property name for getProperties, dsn for getNodeConnectionStatus
	 * @return -2 if result invalid but not last cmd; -3 if result invalid and last cmd; -1 if counter is wrong; 1 if all cmds belonging to this request are processed, 0 otherwise.
	 * */   // NOTE: Zigbee package needs this to be public
	public synchronized int collectResult(final List<String> results) {
		if (this.mCommandCounter <= 0) {
			AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "E", tag, "collectResult", "counter not initialized properly, or mismatches cmds");
			return -1;
		}
		if ( results == null || results.isEmpty() ) {
			AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "W", tag, "collectResult", "cmd result invalid");
			this.mIsRequestComplete = false;
			this.mCommandCounter--;
			if ( this.mCommandCounter == 0) {
				return -3;
			}
			return -2;
		}

		if (this.mCollectiveResult == null) {
			AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "E", tag, "collectResult"
					, "Please use AylaRestService.lanModeInit() for lan mode communication for rs " + this.RequestType + ".\nCan never merge as long as this log appears");
		}
		this.mCollectiveResult.addAll(results);
		this.mCommandCounter --;
		if ( this.mCommandCounter == 0) {
			return 1;
		} 
		return 0;
	}// end of collectResult    
	
	/**
	 * pre-condition:  mCmdEntityMap is properly initialized, one rs is for one device.
	 * 
	 * Get dsn of the device for the AylaRestService object.
	 * 
	 * @return dsn
	 * */   //NOTE: zigbee package needs this so keep it public.
	public String getDSN() {
		Collection<AylaLanCommandEntity> c= mCmdEntityMap.values();
		Iterator<AylaLanCommandEntity> i = c.iterator();
		AylaLanCommandEntity entity = (AylaLanCommandEntity)i.next();
		if (entity!=null) {
			return entity.dsn;
		}
		return "";
	}
	
	/**
	 * Return results collected so far, for this request. 
	 * 
	 * @return a list of strings, which are all the results collected so far.
	 * */ // NOTE: Zigbee package needs this to be public
	public List<String> getCollectiveResult() {
		return this.mCollectiveResult;
	}

	
	// NOTE: Zigbee package needs this to be public
	public void addParam(String name, String value){
		params.add(new AylaParcelableNVPair(name, value));
	}

	
	void addHeader(String name, String value){
		headers.add(new AylaParcelableNVPair(name,value));
	}

	
	/**
	 * Sets the HTTPEntity. Override all params set for the request.
	 */   // NOTE: Zigbee package needs this to be public
	public void setEntity(String entity) {
		try {
			this.entity = entity.getBytes("UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
			this.entity = null;
		}
	}

	void setEntity(byte[] entity){
		this.entity = entity;
	}

	/**
	 * createUrlArray - create a string of restful array params for appending to a given array
	 * @param arrayName - array variable name
	 * @param array - array values
	 * @return a string containing restful array parameters 
	 */  //TODO: Need to inherit a AylaRestServiceZigbee in zigbee space, and maintain encapsulation. For now just change protected to public. 
	public static String createUrlArray(String arrayName, String[] array) {
		ArrayList<String> arrayList = new ArrayList<String>(Arrays.asList(array));
		return createUrlArray(arrayName, arrayList);
	}
	protected static String createUrlArray(String arrayName, ArrayList<String> arrayList) {
		String urlArray = "";
		Boolean first = true;

		String firstFormat = String.format("?%s[]=", arrayName);
		String nextFormat  = String.format("&%s[]=", arrayName);

		for(String element : arrayList) {
			urlArray += (first?firstFormat:nextFormat) + element;
			first = false;
		}

		return urlArray;
	}
	
	/**
	 * Executes the REST request in an IntentService. 
	 * 
	 * The return string is provided in the HandleMessage method of the handler provided by
	 * the constructor. It will be present as the object field of the incoming message. 
	 */
	public Message execute()
	{
		
		if (mTimer!=null) {
			mTimer.stop();
			mTimer = null;
		}
		
		if (isExpired) {
			AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "W", tag, "execute", "Request " + this.RequestType + " Expired");
			return null;
		}
		Message message = null;

		ResultReceiver receiver;
		receiver = new ResultReceiver(mHandler){
			@Override
			protected void onReceiveResult(int resultCode, Bundle resultData) {
				if (resultCode >= 200 && resultCode < 300) {
					mHandler.obtainMessage(AylaNetworks.AML_ERROR_OK,resultCode,0,resultData.getString("result")).sendToTarget();
				} else {
					mHandler.obtainMessage(AylaNetworks.AML_ERROR_FAIL,resultCode,resultData.getInt("subTask"),resultData.getString("result")).sendToTarget();	
				}
			}
		};
		
		if (mContext != null) {
			final Intent intent = new Intent(mContext, AylaExecuteRequest.class);
			intent.putParcelableArrayListExtra("headers", (ArrayList<? extends Parcelable>) headers);
			intent.putExtra("params", params);
			intent.putExtra("url", url);
			intent.putExtra("receiver", receiver);
			intent.putExtra("method", RequestType);
			intent.putExtra("entity", entity);
			
			if (mHandler != null) {
				// asynchronous request returned to mHandler callback
				intent.putExtra("async", true);
				intent.putExtra("result", this.jsonResults);
				intent.putExtra("subTask", this.subTaskFailed);
				intent.putExtra("responseCode", this.responseCode);
				intent.putExtra("info", this.info);
				
				// Start service if not started, send intent params to service
				mContext.startService(intent);	// ==> AylaExecuteRequest.onHandleIntent()
			} else {
				// synchronous request
				intent.putExtra("async", false);
				AylaRestService rs = this;
				
				try {
					AylaCallResponse callResponse;
					AylaExecuteRequest aylaExecuteRequest = new AylaExecuteRequest();

					callResponse = aylaExecuteRequest.handleIntent(intent, rs); // call setup in AylaExecuteRequest.handleIntent()
					
					int resultCode = callResponse.getResultCode();
					String resultData = callResponse.getBundle().getString("result");
					int resultSubTask =  callResponse.getBundle().getInt("subTask");
					
					if (callResponse.getResultCode() >= 200 && callResponse.getResultCode() < 300) {
						message =  Message.obtain(null, AylaNetworks.AML_ERROR_OK, resultCode, 0, resultData);
					} else {
						message =  Message.obtain(null, AylaNetworks.AML_ERROR_FAIL, resultCode, resultSubTask, resultData);	
					}
				} catch (Exception e) {
					e.printStackTrace();
					AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "E", "AylaRestService", "status", AylaNetworks.AML_ERROR_FAIL, "execute");
					message =  Message.obtain(null, AylaNetworks.AML_ERROR_FAIL, AylaNetworks.AML_ERROR_FAIL, 0, e.getLocalizedMessage());	
				}
			}
		} else {
			AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "E", "AylaRestService", "mContext", "null", "execute");
		}
		return message;
	}// end of execute     
	
	
	private void timeOutReturnToMainActivity() {
		this.url = AylaSystemUtils.ERR_URL;
		this.jsonResults = "{\"error\":\"Lan Mode Request " + this.RequestType + " Time Out.\"}";
		this.responseCode = 408;
		this.execute();
	}// end of timeOutReturnToMainActivity
	
}// end of AylaRestService class         





