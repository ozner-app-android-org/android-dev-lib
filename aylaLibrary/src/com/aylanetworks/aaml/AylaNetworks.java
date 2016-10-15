//
//  AylaNetworks.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 8/15/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//
package com.aylanetworks.aaml;

import android.content.Context;
import android.os.Message;
import android.util.Log;

import com.aylanetworks.aaml.enums.IAML_SECURITY_KEY_SIZE;


public class AylaNetworks {

	final static String aamlVersion = "4.4.06";


	public static String getAamlVersion() {
        return aamlVersion;
    }

	/**
	 * All Library init work done in onCreate() here.
	 * */
	public static synchronized void init(final Context context, String deviceSsidRegexStr, String appIdStr) {

		appContext = context;
		deviceSsidRegex = deviceSsidRegexStr;
		appId = appIdStr;
		if (appContext == null) {
			Log.e("AylaNetworks", "E, AylaNetworks, appContext:null, frameworkInit");
			assert(true);
		}
		if (deviceSsidRegex == null) {
			Log.e("AylaNetworks", "E, AylaNetworks, deviceSsidRegx:null, frameworkInit");
			assert(true);
		}
		if (appId == null) {
			Log.e("AylaNetworks", "E, AylaNetworks, appId:null, frameworkInit");
			assert(true);
		}

		AylaSystemUtils.AylaSystemUtilsInit(context); // initialize system utilities
		AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "SystemUtils", "userServiceBaseURL", AylaSystemUtils.userServiceBaseURL(), "settingsInitialize");
		AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "SystemUtils", "deviceServiceBaseURL", AylaSystemUtils.deviceServiceBaseURL(), "settingsInitialize");

		// Generate key pair every time it starts. 
		new Thread(new Runnable(){
			@Override
			public void run() {
				AylaEncryptionHelper.getInstance().init(IAML_SECURITY_KEY_SIZE.IAML_SECURITY_KEY_SIZE_1024);
			}
		}).start();
		// init AylaReachability state
		AylaReachability.determineReachability(false);

		AylaConnectivityListener.registerConnectivityListener(appContext);
	}
	
	/**
	 * All library work done in onResume() claimed here
	 * */
	public static synchronized void onResume() {
		AylaConnectivityListener.enableConnectivityListener(appContext);

		AylaLanMode.resume();
	}
	
	/**
	 * All library work done in onPause() claimed here
	 * */
	public static synchronized void onPause(final boolean pausedByUser) {
		AylaConnectivityListener.disableConnectivityListener(appContext);

		if (AylaSetup.lastMethodCompleted > AylaSetup.AML_SETUP_TASK_NONE) {
			AylaSetup.exit();
		}

		AylaLanMode.pause(pausedByUser);
	}
	
	/**
	 * All library work done in onDestroy() claimed here. 
	 * */
	public static synchronized void onDestroy() {
		//TODO: nothing for now, but app needs to call this in order to avoid future modifications.
	}
	
	public static Context appContext = null;
	static String deviceSsidRegex = null;
	protected static String appId = null;

	// Constants
	public final static int SUCCESS = 0;
	public final static int OK = 0;
	public final static int NO = 0;
	public final static int FAIL = 1;
	public final static int YES = 1;

	// Service Types & locations
	public final static int AML_DEVICE_SERVICE = 0;
	public final static int AML_FIELD_SERVICE = 1;
	public final static int AML_DEVELOPMENT_SERVICE = 2;
    public final static int AML_PRODUCTION_SERVICE = 2;
    public final static int AML_STAGING_SERVICE = 3;
	public final static int AML_DEMO_SERVICE = 4;
	
	public static final String AML_SERVICE_LOCATION_USA = "USA";
	public static final String AML_SERVICE_LOCATION_CHINA = "CN";
	public static final String AML_SERVICE_LOCATION_EUROPE = "EU";
	public final static String[] EUROPE_COUNTRY_CODES = {"AL", "AD", "AT", "BA", "BE", "BG", "BY", "CH", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FO", "FR", "GB",
			"GI", "GR","HR", "HU", "IE", "IM", "IS",  "IT", "LI",  "LT", "LV",  "LU", "MC", "MD", "ME", "MK", "MT",   "NL", "NO", "PL", "PT",  "RO",  "RS", "RU", "SE", "SI", "SM", "SK",
			 "UA", "VA" };
	// logging levels
	public final static int AML_LOGGING_LEVEL_NONE = 0;
	public final static int AML_LOGGING_LEVEL_ERROR = 0x01;
	public final static int AML_LOGGING_LEVEL_WARNING = 0x3;
	public final static int AML_LOGGING_LEVEL_DEBUG = 0x04;
	public final static int AML_LOGGING_LEVEL_INFO = 0x07;
	public final static int AML_LOGGING_LEVEL_ALL = 0xff;
		
	// Define configuration settings defaults. See Settings in AylaSystemUtils
	public final static int DEFAULT_REFRESH_INTERVAL = 4;
	public final static int DEFAULT_WIFI_TIMEOUT = 10;
	public final static int DEFAULT_MAX_COUNT = 100;
	public final static int DEFAULT_SERVICE = AML_DEVICE_SERVICE; // v3.16 - use DNS to determine DEVELOPMENT_SERVICE or FIELD_SERVICE
	public final static int DEFAULT_WIFI_RETRIES = 10; // v1.22
	public final static int DEFAULT_SLOW_CONNECTION = NO; // v2.19a
	public final static int DEFAULT_NEW_DEVICE_TO_SERVICE_CONNECTION_RETRIES = 16;
	public final static int DEFAULT_SECURE_SETUP = YES;
	public final static lanMode DEFAULT_LAN_MODE = lanMode.ENABLED; // v1.63_ENG enabled by default
	public final static int DEFAULT_SERVER_PORT_NUMBER = 10275;
	public final static String DEFAULT_SERVER_BASE_PATH = "/local_lan";
	public final static String DEFAULT_LOGFILE_NAME = "aml_log";
	public final static String DEFAULT_SUPPORT_EMAIL_ADDRESS = "mobile-libraries@aylanetworks.com";
	public final static int DEFAULT_LOGGING_ENABLED = NO;
	public final static int DEFAULT_LOGGING_LEVEL = AML_LOGGING_LEVEL_ERROR;
	public final static int DEFAULT_CLEAR_ALL_CACHES = NO;
	public final static int DEFAULT_MAX_SCHEDULES = 1;
	public final static int DEFAULT_MAX_SCHEDULE_ACTIONS = 10;
	public final static int DEFAULT_SMART_PLUG_SIMULATION = NO;
	public final static int DEFAULT_SETUP_STATUS_POLLING_INTERVAL = 3000;
	public final static int DEFAULT_ACCESS_TOKEN_REFRESH_THRRESHOLD = 21600; // 6 hours
	
	static final int DEFAULT_LAN_MODE_TIME_OUT_FACTOR_PER_CMD = 250;   // in milliseconds.   
	public static int DEFAULT_LAN_MODE_TIME_OUT = 20; // in seconds
	// Service Base URLs
	
	// User Service 
	// Shared servers in the USA for development/production & field
	// Separate servers for China and Staging
	static final String GBL_USER_FIELD_URL = "https://user.aylanetworks.com/";   		 	// field user service
	//static final String GBL_USER_FIELD_CN_URL = "https://user-cn.aylanetworks.com/";     	// China field user service
	static final String GBL_USER_FIELD_CN_URL = "https://user.ayla.com.cn/";     			// China field user service
	static final String GBL_USER_FIELD_EU_URL = "https://user-field-eu.aylanetworks.com/";     	// Europe field user service
	static final String GBL_USER_DEVELOP_URL = "https://user.aylanetworks.com/";   		 	// development/production user service
	//static final String GBL_USER_DEVELOP_CN_URL = "https://user-cn.aylanetworks.com/";  	// China development/production user service
	static final String GBL_USER_DEVELOP_CN_URL = "https://user.ayla.com.cn/";   			// China development/production user service
	static final String GBL_USER_DEVELOP_EU_URL = "https://user.aylanetworks.com/"; 			// Europe development/production user service TBD
	static final String GBL_USER_STAGING_URL = "https://staging-user.ayladev.com/"; 		// staged user service
	static final String GBL_USER_STAGING_CN_URL = "https://staging-user.ayladev.com.cn/"; 	// staged user service TBD
	static final String GBL_USER_STAGING_EU_URL = "https://staging-user.ayladev.com/";; 	// staged user service TBD
	static final String GBL_USER_DEMO_URL = "https://ayla-user.aylanetworks.com/";			// Demo user service
	static final String GBL_USER_DEMO_CN_URL = "https://ayla-user.aylanetworks.com.cn/";	// Demo url in cn pattern
	static final String GBL_USER_DEMO_EU_URL = "https://ayla-user.aylanetworks.com/";	// Demo url in Europe pattern TBD

	// Device Service
	// Default device service is determined by the appId via DNS
	// Overrides are provided for developers only via settings
	static final String GBL_DEVICE_SUFFIX_URL = "-device.aylanetworks.com/apiv1/";            	  		// device service determined by DNS
	static final String GBL_DEVICE_FIELD_URL = "https://ads-field.aylanetworks.com/apiv1/"; 	  		// field device service
	//static final String GBL_DEVICE_FIELD_CN_URL = "https://ads-field-cn.aylanetworks.com/apiv1/"; 	// China field device service
	static final String GBL_DEVICE_FIELD_CN_URL = "https://ads-field.ayla.com.cn/apiv1/"; 				// China field device service
	static final String GBL_DEVICE_FIELD_EU_URL = "https://ads-field-eu.aylanetworks.com/apiv1/";
					// Europe field device service
	static final String GBL_DEVICE_DEVELOP_URL = "https://ads-dev.aylanetworks.com/apiv1/";   	  		// development/production device service
	//static final String GBL_DEVICE_DEVELOP_CN_URL = "https://ads-dev-cn.aylanetworks.com/apiv1/"; 	// China development/production device service
	static final String GBL_DEVICE_DEVELOP_CN_URL = "https://ads-dev.ayla.com.cn/apiv1/"; 				// China development/production device service
	static final String GBL_DEVICE_DEVELOP_EU_URL = "https://ads-dev.aylanetworks.com/apiv1/";  		// Europe development/production device service
	static final String GBL_DEVICE_STAGING_URL = "https://staging-ads.ayladev.com/apiv1/";   			// staging device service
	static final String GBL_DEVICE_STAGING_CN_URL = "https://staging-ads.ayladev.com.cn/apiv1/";		// China staging device service TBD
	static final String GBL_DEVICE_STAGING_EU_URL = "https://staging-ads.ayladev.com/apiv1/"; 			// Europe staging device service TBD
	static final String GBL_DEVICE_DEMO_URL = "https://ayla-ads.aylanetworks.com/apiv1/";				// Demo device service
	static final String GBL_DEVICE_DEMO_CN_URL = "https://ayla-ads.aylanetworks.com.cn/apiv1/";			// Demo url in cn pattern
	static final String GBL_DEVICE_DEMO_EU_URL = "https://ayla-ads.aylanetworks.com/apiv1/";			// Demo url in EU pattern
	
	// Application Trigger/Rules Service
	// Default Application Trigger/Rules service is determined by the appId via DNS
	// Overrides are provided for developers only via settings
	static final String GBL_APPTRIGGER_SUFFIX_URL = "-device.aylanetworks.com/apiv1/";            		// device service determined by DNS
	static final String GBL_APPTRIGGER_FIELD_URL = "https://ads-field.aylanetworks.com/apiv1/"; 		// field application trigger service
	//static final String GBL_APPTRIGGER_FIELD_CN_URL = "https://ads-field-cn.aylanetworks.com/apiv1/"; // China field application trigger service
	static final String GBL_APPTRIGGER_FIELD_CN_URL = "https://ads-field.ayla.com.cn/apiv1/"; 			// China field application trigger service
	static final String GBL_APPTRIGGER_DEVELOP_URL = "https://ads-dev.aylanetworks.com/apiv1/";			// development/production application trigger service
	//static final String GBL_APPTRIGGER_DEVELOP_CN_URL = "https://ads-dev-cn.aylanetworks.com/apiv1/";	// China development/production application trigger service
	static final String GBL_APPTRIGGER_DEVELOP_CN_URL = "https://ads-dev.ayla.com.cn/apiv1/"; 			// China development/production application trigger service
	static final String GBL_APPTRIGGER_STAGING_URL = "https://staging-ads.ayladev.com/apiv1/"; 			// staging application trigger service
	static final String GBL_APPTRIGGER_STAGING_CN_URL = "https://staging-ads.ayladev.com.cn/apiv1/"; 	// China staging application trigger service TBD
	static final String GBL_APPTRIGGER_DEMO_URL = "https://ayla-ads.aylanetworks.com/apiv1/";			// demo device service
	static final String GBL_APPTRIGGER_CN_DEMO_URL = "https://ayla-ads.aylanetworks.com.cn/apiv1/";		// same demo url for now.

	// Logging Service
	// Shared servers in the USA for development/production & field
	// Separate servers for China and Staging
	static final String GBL_LOG_FIELD_URL = "https://log.aylanetworks.com/api/v1/"; 			// field sign-up
	//static final String GBL_LOG_FIELD_CN_URL = "https://log-cn.aylanetworks.com/api/v1/"; 	// China field sign-up
	static final String GBL_LOG_FIELD_CN_URL = "https://log.ayla.com.cn/api/v1/"; 				// China field sign-up
	static final String GBL_LOG_FIELD_EU_URL = "https://log-field-eu.aylanetworks.com/api/v1/";
			// Europe log service
	static final String GBL_LOG_DEVELOP_URL = "https://log.aylanetworks.com/api/v1/"; 			// development sign-up
	//static final String GBL_LOG_DEVELOP_CN_URL = "https://log-cn.aylanetworks.com/api/v1/"; 	// China development sign-up
	static final String GBL_LOG_DEVELOP_CN_URL = "https://log.ayla.com.cn/api/v1/"; 			// China development sign-up
	static final String GBL_LOG_DEVELOP_EU_URL = "https://log-eu.aylanetworks.com/"; 			// Europe development sign-up TBD
	static final String GBL_LOG_STAGING_URL = "https://staging-log.ayladev.com/api/v1/"; 		// staging sign-up service
	static final String GBL_LOG_STAGING_CN_URL = "https://staging-log.ayladev.com.cn/api/v1/"; 	// China staging sign-up service TBD
	static final String GBL_LOG_STAGING_EU_URL = "https://staging-log-eu.ayladev.com/api/v1/"; 	// Europe staging sign-up service TBD
	static final String GBL_LOG_DEMO_URL = "https://log.aylanetworks.com/api/v1/";				// demo log service
	static final String GBL_LOG_DEMO_CN_URL = "https://log.aylanetworks.com.cn/api/v1/";			// same demo url for now
	
	//static final String GBL_DSN_PREFIX = "Ayla-AC"; // early SSID prefix
	//static final String GBL_MAC_PREFIX = "Ayla-60"; // current SSID prefix
	static final String GBL_FILE_URL = "./ayla";  // local test file data
	static final String GBL_DEVICES_DIRECTORY = "devices";
	static final String GBL_PROPERTIES_DIRECTORY = "properties";
	static final String GBL_TRIGGERS_DIRECTORY = "triggers";
	static final String GBL_AJAX_TIMEOUT = "6000"; // 6 seconds 
	static final String GBL_MODULE_DEFAULT_WIFI_IPADDR = "192.168.0.1";

    /** Helper method to see if a returned Message succeeded */
    public static boolean succeeded(Message msg) {
        if ( msg.what != AML_ERROR_OK ) {
            return false;
        }

        switch ( msg.arg1 ) {
            case AML_ERROR_OK:
            case AML_ERROR_ASYNC_OK:
            case AML_ERROR_OK_CREATED:
            case AML_ERROR_ASYNC_OK_CACHED:
            case AML_ERROR_OK_NO_CONTENT:
                return true;

            default:
                return false;
        }
    }

	public final static int AML_ERROR_OK = 0; // No error
	public final static int AML_ERROR_FAIL = 1; // The operation did not succeed. Check the nativeErrorInfo object for details
	public final static int AML_ERROR_ASYNC_OK = 200; // Good results for asynchronous rest service calls
	public final static int AML_ERROR_SYNC_OK = 200;
    public final static int AML_ERROR_OK_CREATED = 201;     // Success code for creation of an object
	public final static int AML_ERROR_ASYNC_OK_CACHED = 203; // Good results for asynchronous rest service calls, using cached values
    public final static int AML_ERROR_OK_NO_CONTENT = 204;  // Good result but nothing coming back from the server
	public final static int AML_ERROR_TOKEN_EXPIRE = 403;
    public final static int AML_ERROR_NOT_FOUND = 404; // No results were found
    public final static int AML_ERROR_TIMEOUT = 408; // Request timeout
	public final static int AML_ALLOCATION_FAILURE = 1000;
	public final static int AML_NO_ITEMS_FOUND = 1001;
	public final static int AML_JSON_PARSE_ERROR = 1002;
	public final static int AML_USER_NO_AUTH_TOKEN = 1003;
	public final static int AML_USER_INVALID_PARAMETERS = 1004;
	public final static int AML_USER_OAUTH_DENY = 1005;
	public final static int	AML_USER_OAUTH_ERROR = 1006;
	public final static int AML_ERROR_BUSY = 1011;
	public final static int AML_IO_EXCEPTION = 1105;
	public final static int AML_CLIENT_PROTOCOL_EXCEPTION = 1106;
	public final static int AML_GENERAL_EXCEPTION = 1107;
	public final static int AML_WIFI_ERROR = 1108;
	public final static int AML_TASK_ORDER_ERROR = 1109;
	public final static int AML_ERROR_UNREACHABLE = 1110;
	public final static int AML_ERROR_PARAM_MISSING = 1111;
    public final static int AML_ERROR_LOCATION_SERVICE_DISABLED = 1112;

    // error codes for compound method registerNewDevice - indicates in which sub-method an error occurred
	public final static int AML_REGISTER_NEW_DEVICE = 1500;
	public final static int AML_GET_REGISTRATION_CANDIDATE = 1501;
	public final static int AML_GET_MODULE_REGISTRATION_TOKEN = 1502;
	public final static int AML_REGISTER_DEVICE = 1503;
	
	// registration types
	public final static String AML_REGISTRATION_TYPE_SAME_LAN ="Same-LAN";
	public final static String AML_REGISTRATION_TYPE_BUTTON_PUSH = "Button-Push";
	public final static String AML_REGISTRATION_TYPE_AP_MODE = "AP-Mode";
	public final static String AML_REGISTRATION_TYPE_DISPLAY = "Display";
	public final static String AML_REGISTRATION_TYPE_DSN = "Dsn";
	public final static String AML_REGISTRATION_TYPE_NODE = "Node";
	public final static String AML_REGISTRATION_TYPE_NONE = "None";

	// Connect Host To New Device
	public final static int AML_CONNECT_HOST_TO_NEW_DEVICE = 1510;
	public final static int AML_RETURN_HOST_NETWORK_CONNECTION = 1511;
	public final static int AML_SET_HOST_NETWORK_CONNECTION = 1512;
	public final static int AML_GET_NEW_DEVICE_DETAIL = 1513;
	

	public final static int AML_CONNECT_NEW_DEVICE_TO_SERVICE = 1520;
	public final static int AML_SET_NEW_DEVICE_CONNECT_TO_NETWORK = 1521;
	public final static int AML_DELETE_HOST_NETWORK_CONNECTION = 1522;

	// Confirm New Device To Service Connection
	public final static int AML_SET_HOST_NETWORK_RECONNECT = 1530;
	public final static int AML_GET_NEW_DEVICE_CONNECTED = 1531;

    //Disconnect AP Mode
    public final static int AML_DISCONNECT_AP_MODE = 1532;

	// Get New Device WiFi Status
	public final static int AML_SET_HOST_NETWORK_CONNECTION2 = 1540;
	public final static int AML_GET_NEW_DEVICE_WIFI_STATUS = 1542;
	
	// Get New Device WiFi Scan
	public final static int AML_GET_NEW_DEVICE_SCAN_FOR_APS = 1550;
	
	// longitude & latitude
	public static final String AML_SETUP_LOCATION_LONGITUDE = "longitude";
	public static final String AML_SETUP_LOCATION_LATITUDE = "latitude";
	
	// Cache selection
	public final static int AML_CACHE_DEVICE = 0x01;
	public final static int AML_CACHE_PROPERTY = 0x02;
	public final static int AML_CACHE_LAN_CONFIG = 0x04;
	public final static int AML_CACHE_SETUP = 0x08;
	public final static int AML_CACHE_GROUP = 0x10;
	public final static int AML_CACHE_NODE = 0x20;
	public final static int AML_CACHE_ALL = 0xFF;
	
	public final static String AML_CACHE_DEVICE_PREFIX = "allDevices";
	public final static String AML_CACHE_PROPERTY_PREFIX = "properties_";
	public final static String AML_CACHE_LAN_CONFIG_PREFIX = "lanConfig_";
	public final static String AML_CACHE_SETUP_PREFIX = "newDeviceConnected";
	public final static String AML_CACHE_NODE_PREFIX = "nodes_";
	public final static String AML_CACHE_GROUP_PREFIX =  "allGroups";
	
	// Email Customization Support
	public final static String AML_EMAIL_TEMPLATE_ID = "emailTemplateId";
	public final static String AML_EMAIL_SUBJECT = "emailSubject";
	public final static String AML_EMAIL_BODY_HTML = "emailBodyHtml";
	
	// error codes for compound method schedule
	public final static int AML_UPDATE_SCHEDULE = 1550;
	public final static int AML_CREATE_SCHEDULE_ACTIONS = 1551;
	public final static int AML_DELETE_SCHEDULE_ACTIONS = 1552;
	public final static int AML_SCHEDULE_UPDATE_CONTINUE = 1553;
	
	// error codes for compound method Get Picture/BLOB
	public final static int AML_GET_DATAPOINT_BLOBS = 1560;
	public final static int AML_GET_DATAPOINT_BLOB_URLS = 1561;
	//public final static int AML_GET_SAVE_DATAPOINT_BLOB = 1562;
	public final static int AML_MARK_DATAPOINT_BLOB_FETCHED = 1563;
	public final static int AML_GET_DATAPOINT_BLOBS_FILES = 1564;
	public final static int AML_CREATE_DATAPOINT_BLOBS = 1565;
	public final static int AML_CREATE_DATAPOINT_BLOBS_FILES = 1566;
	public final static int AML_MARK_DATAPOINT_BLOB_FINISHED = 1567;
	
//	public final static String AML_DEFAULT_BLOB_FILENAME = "aylaBlob";
	
	// error codes for compound Oauth login
	public final static int AML_POST_OAUTH_LOGIN = 1570;
	public final static int AML_POST_OAUTH_PROVIDER_URL = 1571;
	public final static int AML_POST_OAUTH_AUTHENTICATE_TO_SERVICE = 1572;
	
	// Reachability/Discovery
	public final static int AML_REACHABILITY_REACHABLE = 0;
	public final static int AML_REACHABILITY_UNREACHABLE = -1;
	public final static int AML_REACHABILITY_LAN_MODE_DISABLED = -2;
	public final static int AML_REACHABILITY_UNKNOWN = -3;
	public final static int AML_REACHABILITY_DETERMINING = -4;
	
	public final static int AML_DEVICE_REACHABLE_TIMEOUT = 3000; // ms to wait for WLAN device Discovery
	public final static int AML_SERVICE_REACHABLE_TIMEOUT = 4000; // ms to wait for service Discovery
	public final static int AML_LAN_MODE_TIMEOUT_SAFETY = 5000; // ms subtracted from secure session keep-alive timer value
	public final static int AML_COMMAND_ID_NOT_USED = -1;
	
	// device connected modes
	public final static String AML_CONNECTION_UNKNOWN = "Unknown";
	public final static String AML_IN_AP_MODE = "AP Mode";
	public final static String AML_CONNECTED_TO_HOST = "Host";
	public final static String AML_CONNECTED_TO_SERVICE = "Service";

	// Wifi helpful values
	public final static int AML_NETID_NOT_REMEMBERED = -2;
	public final static String AML_LANMODE_IGNORE_BASETYPES = "stream float file"; // list of unnecessary property base types for lan mode

	// Setup task states
	public final static int AML_SETUP_TASK_NONE = 0;
	public final static int AML_SETUP_TASK_INIT = 1;
	public final static int AML_SETUP_TASK_RETURN_HOST_SCAN_FOR_NEW_DEVICES = 2;
	public final static int AML_SETUP_TASK_CONNECT_TO_NEW_DEVICE = 3;
	public final static int AML_SETUP_TASK_GET_DEVICE_SCAN_FOR_APS = 4;
	public final static int AML_SETUP_TASK_CONNECT_NEW_DEVICE_TO_SERVICE = 5;
	public final static int AML_SETUP_TASK_CONFIRM_NEW_DEVICE_TO_SERVICE_CONNECTION = 6;
	public final static int AML_SETUP_TASK_GET_NEW_DEVICE_WIFI_STATUS = 7; // only called on connection error
	public final static int AML_SETUP_TASK_EXIT = 8;

	// Wifi Security Types
	public static final String AML_WPA2 = "WPA2";
	public static final String AML_WPA = "WPA";
	public static final String AML_WEP = "WEP";
	public static final String AML_OPEN = "OPEN";
	public static final String AML_WPA_EAP = "WPA_EAP";  // EAP Enterprise fields
	public static final String AML_IEEE8021X = "IEEE8021X";
	public static final String AML_EAP_METHOD[] = {"PEAP", "TLS", "TTLS"};

	public static final String AML_wifiErrorMsg[] = { 
		"No Error.", // 0
		"Resource problem, out of memory or buffers, perhaps temporary.", 
		"Connection timed out.", 
		"Bad password. Please check and try again.", // "Invalid key.", 
		"SSID not found.", 
		"Not authenticated via 802.11 or failed to associate with the AP.", // 5
		"Bad password. Please check and try again.", // "Incorrect key.", 
		"Failed to get IP address from DHCP.", 
		"Failed to get default gateway from DHCP.", 
		"Failed to get DNS server from DHCP.", 
		"Disconnected by AP.", // 10
		"Signal lost from AP (beacon miss).", 
		"Name server look-up failed. Please check your wireless router connection.", // "Device service host lookup failed.", 
		"Device service GET was redirected.", 
		"Device service connection timed out.", 
		"No empty Wifi profile slots.", // 15
		"Security methond used by AP not supported.",
		"Network type (e.g. ad-hoc) is not supported.", 
		"The server responded in an incompatible way. The AP may be a Wi-Fi hotspot.",
		"Device Service authentication failed.",
		"Connection attempt is still in progress.", // 20
		"Unknown error." 
	};
	
	
	// lan Mode 
	public enum lanMode {ENABLED, DISABLED, STARTING, RUNNING, STOPPING, STOPPED, FAILED} // states
	public static final String lanModeMsg[] = {
		"ENABLED",
		"DISABLED",
		"STARTING",
		"RUNNING",
		"STOPPING",
		"STOPPED",
		"FAILED"
	};
	
	public static final String AML_NOTIFY_TYPE_SESSION = "session";
	public static final String AML_NOTIFY_TYPE_PROPERTY = "property";
	public static final String AML_NOTIFY_TYPE_NODE = "node";
	
	public static final String sessionStateMsg[] = {
		"DOWN",
		"LOCAL_REGISTRATION",
		"KEY_EXCHANGE",
		"UP",
		"UNKNOWN"
	};
	
	public static final String TAG = "com.aylanetworks.aaml.AylaNetworks";

}
