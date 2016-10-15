//
//  AylaSystemUtils.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 8/15/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//
package com.aylanetworks.aaml;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

@SuppressLint("NewApi")
public class AylaSystemUtils extends AylaNetworks {
	
	private final static String tag = AylaSystemUtils.class.getSimpleName();
	
	public static Gson gson = null;
	public static final String ERR_URL = "ERRORS";
	
//	protected static Logger log = null;
	private static SharedPreferences prefs = null;
	private static SharedPreferences.Editor prefsEd = null;
	private static String amlDeviceService = null;
	private static String amlAppTriggerService = null;
	protected static String serviceLocation = null;
    private static AylaLogManager logManager;

	private static boolean notifyOutstandingEnabled = true;
	
	/* called once from AylaNetworks.init(). */
	protected static void AylaSystemUtilsInit(Context appContext) {

		if (loggingEnabled == YES) {
			//loggingInit();
			//adds file and console logger
			initLogger();
		}

		// set default/automatic service location based on appId
		// can be overridden by user settings in the next call
		setAppId(appId);

		if (prefs == null) {
			settingsInit(appContext);
		}

		systemInfo(); // get system info once

		@SuppressWarnings("unused") // OK
		AylaDevice device = new AylaDevice();
		@SuppressWarnings("unused") // OK
		AylaProperty property = new AylaProperty();
		@SuppressWarnings("unused")
		AylaSchedule schedule = new AylaSchedule();
		@SuppressWarnings("unused")
		AylaBlob blob = new AylaBlob();

		if (gson == null) {
			gson = AylaCommProxy.getParser();
		}

		if (AylaUser.user == null) {
			AylaUser.setCurrent(new AylaUser()); // instantiate a new user v3.20_ENG
		}
		
		if (clearAllCaches == YES) {
			AylaCache.clearAll();
		}
		
		AylaLogService.clearLogsQueue();
	}


    private static void initLogger(){
        logManager = AylaLogManager.getInstance();
        logManager.init();
		Log.d("LOGLEVEL", "initLogger "+ String.valueOf(AylaSystemUtils.loggingLevel));

        logManager.setLogLevel(AylaSystemUtils.loggingLevel);
    }

	//--------------------------------- persistence methods -----------------------------
	private static String settingsFilePath = null;
	private static String settingsFileName = "AylaSettings";

	// Define configuration settings defaults
	public static int refreshInterval = AylaNetworks.DEFAULT_REFRESH_INTERVAL;
	public static int wifiTimeout = AylaNetworks.DEFAULT_WIFI_TIMEOUT;
	public static int maxCount = AylaNetworks.DEFAULT_MAX_COUNT;
	public static int serviceType = AylaNetworks.DEFAULT_SERVICE;
	public static int wifiRetries = AylaNetworks.DEFAULT_WIFI_TIMEOUT;
	public static int newDeviceToServiceConnectionRetries = AylaNetworks.DEFAULT_NEW_DEVICE_TO_SERVICE_CONNECTION_RETRIES;
	public static int slowConnection = AylaNetworks.DEFAULT_SLOW_CONNECTION;
	public static int secureSetup = AylaNetworks.DEFAULT_SECURE_SETUP;
	public static lanMode lanModeState = AylaNetworks.DEFAULT_LAN_MODE;  // must ENABLE for LAN Mode operation
	public static int serverPortNumber = AylaNetworks.DEFAULT_SERVER_PORT_NUMBER;
	public static String serverPath = AylaNetworks.DEFAULT_SERVER_BASE_PATH;
	public static String logfileName = AylaNetworks.DEFAULT_LOGFILE_NAME;
	public static int loggingEnabled = AylaNetworks.DEFAULT_LOGGING_ENABLED;
	public static int loggingLevel = AylaNetworks.DEFAULT_LOGGING_LEVEL;
	public static int clearAllCaches = AylaNetworks.DEFAULT_CLEAR_ALL_CACHES;
	public static int serviceReachableTimeout = AML_SERVICE_REACHABLE_TIMEOUT;
	public static String supportEmailAddress = AylaNetworks.DEFAULT_SUPPORT_EMAIL_ADDRESS;


	// Settings key names
    public static String SETTING_SAVED_USER = "currentUser";

	/**
	 * Used to set a new AppId. Required only for testing service location changes
	 * In released apps, the appId is set in Aylanetworks.init() and never changed.
	 * 
	 * @param _appId
	 */
	public static void setAppId(String _appId) {
		// Set the app Id
		if (_appId == null) {
			saveToLog("%s, %s, %s:%s %s", "E", "SystemUtils", "appId", "null", "setAppId()");
			return;
		}
		appId = _appId; // assign new appId
		
		// Set service location
		if (_appId.contains("-cn")) {
			serviceLocation = AML_SERVICE_LOCATION_CHINA;
		} else if(_appId.contains("-eu")){
			serviceLocation = AML_SERVICE_LOCATION_EUROPE;
		} else{
			serviceLocation = AML_SERVICE_LOCATION_USA;
		}
		amlDeviceService = null; // for resetting of Device service based on new appId
		
	}

    /*
    *Used to set service location based on country code
    * @param countryCode
    * @return serviceLocation
    */
    public static String setServicelocationWithCountryCode(String countryCode){

		final List<String> countryListEurope = Arrays.asList(EUROPE_COUNTRY_CODES);
		if(countryListEurope.contains(countryCode)){
			countryCode = "EU";
		}
		countryCode = countryCode.toUpperCase();
		switch(countryCode){
			case "CN":
				serviceLocation = AML_SERVICE_LOCATION_CHINA;
				break;
			case "US":
				serviceLocation = AML_SERVICE_LOCATION_USA;
				break;
			case "EU":
				serviceLocation = AML_SERVICE_LOCATION_EUROPE;
				break;
			default:
				serviceLocation = AML_SERVICE_LOCATION_USA;
				break;

		}
		return serviceLocation;

    }

	
	private static int settingsInit(Context appContext) {
		saveToLog("%s, %s, %s", "I", "SystemUtils", "settingsInitialize()");
		try {
			prefs = appContext.getSharedPreferences(settingsFileName, Context.MODE_PRIVATE);
			prefsEd = prefs.edit();
			// See: http://stackoverflow.com/questions/5531289/copy-the-shared-preferences-xml-file-from-data-on-samsung-device-failed
			// for more info on why we do this instead of use /data/data/...
						
			String sharedPrefsPath = new File(appContext.getFilesDir(), "../shared_prefs").getPath();
			settingsFilePath = sharedPrefsPath + "/" + settingsFileName + ".xml";
			
			File f = new File(settingsFilePath);
			if (f.exists()) {
				saveToLog("%s, %s, %s:%s, %s", "I", "SystemUtils", "settingsFilePath", settingsFilePath, "settingsInitialize");
				loadSavedSettings();
			} else {
				saveToLog("%s, %s, %s:%s, %s", "I", "SystemUtils", "settingsFilePath", "doesNotExist", "settingsInitialize");
				saveDefaultSettings(); // create and save default settings
			}
			saveToLog("%s, %s, %s:%s, %s", "I", "SystemUtils", "userServiceBaseURL", userServiceBaseURL(), "settingsInitialize");
		}
		catch (Exception e) {
			saveToLog("%s, %s, %s:%s, %s", "E", "SystemUtils", "Exception_thrown", "getSharedPreferences", "settingsInitialize");
			e.printStackTrace();
		}
		if (prefs == null) {
			Log.e("AylaSystemUtils", "E, SystemUtils, prefs:null, settingsInitialize");
			assert(true);
		}
		return AylaNetworks.SUCCESS;
	}

	/**
	 * 
	 * Save current library configuration parameters locally, which are all 
	 * encapsulated inside AylaSystemUtils, no parameter from the outside.  
	 * 
	 * @return AylaNetworks.SUCCESS upon operation successfully committed. 
	 * */ //TODO: figure out the failure case.
	public static int saveCurrentSettings() {
		saveToLog("%s, %s, %s", "I", "SystemUtils", "saveCurrentSettings()");

		prefsEd.putString("version", aamlVersion);
		prefsEd.putInt("refreshInterval", refreshInterval);
		prefsEd.putInt("wifiTimeout", wifiTimeout);
		prefsEd.putInt("maxCount", maxCount);
		prefsEd.putInt("serviceType", serviceType);
		prefsEd.putInt("loggingEnabled", loggingEnabled);
		prefsEd.putInt("loggingLevel", loggingLevel);
		prefsEd.putInt("newDeviceToServiceConnectionRetries", newDeviceToServiceConnectionRetries);
		prefsEd.putInt("slowConnection", slowConnection);
		prefsEd.putInt("clearAllCaches", clearAllCaches);
		if (AylaSystemUtils.lanModeState == lanMode.DISABLED) {
			prefsEd.putString("lanMode", lanModeMsg[lanMode.DISABLED.ordinal()]);
		} else {
			prefsEd.putString("lanMode", lanModeMsg[lanMode.ENABLED.ordinal()]);
		}
		prefsEd.commit();

		return AylaNetworks.SUCCESS;
	}

	/**
	 * 
	 * Load library configurations from local storage, and initialize AylaSystemUtils. 
	 * 
	 * @return AylaNetworks.SUCCESS upon operation successfully committed. 
	 * */ //TODO: figure out the failure case.
	public static int loadSavedSettings() {
		saveToLog("%s, %s, %s", "I", "SystemUtils", "loadSavedSettings()");

		String version = prefs.getString("version", "1.0");
		if ("1.0".equals(version)) {										// is version 1.x
			serviceType = (serviceType == 0) ? AML_STAGING_SERVICE : DEFAULT_SERVICE; // map old service type to new service type
			saveSetting("serviceType", serviceType);				// save the new service type
			saveSetting("version", aamlVersion); 					// save the current version
			saveToLog("%s, %s, %s:%s, %s", "I", "SystemUtils", "version", version, "loadSavedSettings()");
		}

		refreshInterval = prefs.getInt("refreeshInteval", AylaNetworks.DEFAULT_REFRESH_INTERVAL);
		wifiTimeout = prefs.getInt("wifiTimeout", AylaNetworks.DEFAULT_WIFI_TIMEOUT);
		maxCount = prefs.getInt("maxCount", AylaNetworks.DEFAULT_MAX_COUNT);
		serviceType = prefs.getInt("serviceType", AylaNetworks.DEFAULT_SERVICE);
		loggingEnabled = prefs.getInt("loggingEnabled", AylaNetworks.DEFAULT_LOGGING_ENABLED);
		loggingLevel = prefs.getInt("loggingLevel", AylaNetworks.DEFAULT_LOGGING_LEVEL);
		newDeviceToServiceConnectionRetries = prefs.getInt("newDeviceToServiceConnectionRetries", AylaNetworks.DEFAULT_NEW_DEVICE_TO_SERVICE_CONNECTION_RETRIES);
		slowConnection = prefs.getInt("slowConnection", AylaNetworks.DEFAULT_SLOW_CONNECTION);
		clearAllCaches = prefs.getInt("clearAllCaches", AylaNetworks.DEFAULT_CLEAR_ALL_CACHES);
		String lanModeStr = prefs.getString("lanMode", lanModeMsg[AylaNetworks.DEFAULT_LAN_MODE.ordinal()]);
		if (TextUtils.equals(lanModeStr, lanModeMsg[lanMode.DISABLED.ordinal()])) {
			lanModeState = lanMode.DISABLED;
		}

		return AylaNetworks.SUCCESS;
	}

	
	/**
	 * 
	 *  Save default configuration parameters to local storage.
	 * 
	 * @return AylaNetworks.SUCCESS upon operation successfully committed. 
	 * */ //TODO: figure out the failure case.
	public static int saveDefaultSettings() {
		saveToLog("%s, %s, %s", "I", "SystemUtils", "saveDefaultSettings()");

		prefsEd.clear();
		prefsEd.putString("version", aamlVersion);
		prefsEd.putInt("refreshInterval", AylaNetworks.DEFAULT_REFRESH_INTERVAL);
		prefsEd.putInt("wifiTimeout", AylaNetworks.DEFAULT_WIFI_TIMEOUT);
		prefsEd.putInt("maxCount", AylaNetworks.DEFAULT_MAX_COUNT);
		prefsEd.putInt("serviceType", AylaNetworks.DEFAULT_SERVICE);
		prefsEd.putInt("loggingEnabled", AylaNetworks.DEFAULT_LOGGING_ENABLED);
		prefsEd.putInt("loggingLevel", AylaNetworks.DEFAULT_LOGGING_LEVEL);
		prefsEd.putInt("newDeviceToServiceConnectionRetries", AylaNetworks.DEFAULT_NEW_DEVICE_TO_SERVICE_CONNECTION_RETRIES);
		prefsEd.putInt("slowConnection", AylaNetworks.DEFAULT_SLOW_CONNECTION);
		prefsEd.putInt("clearAllCaches", AylaNetworks.DEFAULT_CLEAR_ALL_CACHES);
		prefsEd.putString("lanMode", lanModeMsg[AylaNetworks.DEFAULT_LAN_MODE.ordinal()]);
		
		prefsEd.commit();

		return AylaNetworks.SUCCESS;
	}

	/**
	 * Save string value.
	 * 
	 * @return AylaNetworks.SUCCESS upon operation successfully committed. 
	 * */
	public static int saveSetting(String name, String value) {
		prefsEd.putString(name, ""); // clear existing value
		prefsEd.putString(name, value); // save info
		prefsEd.commit();
		return AylaNetworks.SUCCESS;
	}
	
	/**
	 * Save integer value.
	 * 
	 * @return AylaNetworks.SUCCESS upon operation successfully committed. 
	 * */
	public static int saveSetting(String name, int value) {
		prefsEd.putString(name, ""); // clear existing value
		prefsEd.putInt(name, value); // save info
		prefsEd.commit();
		return AylaNetworks.SUCCESS;
	}
	
	public static String loadSavedSetting(String name, String defaultValue) {
		if (prefs == null) {
			saveToLog("%s, %s, %s, %s.", "W", tag, "loadSavedSettings", "prefs is collected");
			return "";
		}
		return prefs.getString(name, defaultValue);
	}
	
	public static Integer loadSavedSetting(String name, Integer defaultValue) {
		return prefs.getInt(name, defaultValue);
	}
	
	//--------------------------- Service base URL getters --------------------------
	// assume field and development/production user service use the same servers
	public static String userServiceBaseURL() {
		// set device service base url now
		if (serviceType == AML_DEVICE_SERVICE) {
			if (amlDeviceService == null) {
				amlDeviceService = "https://" + appId + GBL_DEVICE_SUFFIX_URL; 
			}
			// return the base user service url
			String url;
			switch (serviceLocation){
				case AML_SERVICE_LOCATION_USA:
					url =  GBL_USER_FIELD_URL;
					break;
				case AML_SERVICE_LOCATION_CHINA:
					url =  GBL_USER_FIELD_CN_URL;
					break;
				case AML_SERVICE_LOCATION_EUROPE:
					url = GBL_USER_FIELD_EU_URL;
					break;
				default:
					url = GBL_USER_FIELD_URL;
					break;

			}
			return url;
		}
		if (serviceType == AML_FIELD_SERVICE) {

			String url;
			switch (serviceLocation){
				case AML_SERVICE_LOCATION_USA:
					url = GBL_USER_FIELD_URL;
					break;
				case AML_SERVICE_LOCATION_CHINA:
					url = GBL_USER_FIELD_CN_URL;
					break;
				case AML_SERVICE_LOCATION_EUROPE:
					url = GBL_USER_FIELD_EU_URL;
					break;
				default:
					url = GBL_USER_FIELD_URL;
					break;
			}
			return url;
		}
		if (serviceType == AML_DEVELOPMENT_SERVICE) {

			String url;
			switch(serviceLocation){
				case AML_SERVICE_LOCATION_USA:
					url = GBL_USER_DEVELOP_URL;
					break;
				case AML_SERVICE_LOCATION_CHINA:
					url = GBL_USER_DEVELOP_CN_URL;
					break;
				case AML_SERVICE_LOCATION_EUROPE:
					url = GBL_USER_DEVELOP_EU_URL;
					break;
				default:
					url = GBL_USER_DEVELOP_URL;
					break;

			}
			return url;
		}

		if (serviceType == AML_STAGING_SERVICE) {
			//return GBL_USER_STAGING_URL;

			String url;
			switch(serviceLocation){
				case AML_SERVICE_LOCATION_USA:
					url = GBL_USER_STAGING_URL;
					break;
				case AML_SERVICE_LOCATION_CHINA:
					url = GBL_USER_STAGING_CN_URL;
					break;
				case AML_SERVICE_LOCATION_EUROPE:
					url = GBL_USER_STAGING_EU_URL;
					break;
				default:
					url = GBL_USER_STAGING_URL;
					break;

			}
			return url;
		}
		if (serviceType == AML_DEMO_SERVICE) {
			// return GBL_USER_DEMO_URL;

			String url;
			switch(serviceLocation){
				case AML_SERVICE_LOCATION_USA:
					url = GBL_USER_DEMO_URL;
					break;
				case AML_SERVICE_LOCATION_CHINA:
					url = GBL_USER_DEMO_CN_URL;
					break;
				case AML_SERVICE_LOCATION_EUROPE:
					url = GBL_USER_DEMO_EU_URL;
					break;
				default:
					url = GBL_USER_DEMO_URL;
					break;

			}
			return url;
		}
		saveToLog("%s, %s, %s:%s, %s", "E", "SystemUtils", "serviceType", "null", "userServiceBaseURL");
		return null;
	}
	public static String deviceServiceBaseURL() {
		if (serviceType == AML_DEVICE_SERVICE) {
			if (amlDeviceService == null) {
				amlDeviceService = "https://" + appId + GBL_DEVICE_SUFFIX_URL;
			}
			return amlDeviceService;
		}
		if (serviceType == AML_FIELD_SERVICE) {

			String url;
			switch(serviceLocation){
				case AML_SERVICE_LOCATION_USA:
					url = GBL_DEVICE_FIELD_URL;
					break;
				case AML_SERVICE_LOCATION_CHINA:
					url = GBL_DEVICE_FIELD_CN_URL;
					break;
				case AML_SERVICE_LOCATION_EUROPE:
					url = GBL_DEVICE_FIELD_EU_URL;
					break;
				default:
					url = GBL_DEVICE_FIELD_URL;
					break;

			}
			return url;

		}
		if (serviceType == AML_DEVELOPMENT_SERVICE) {
			//return GBL_DEVICE_DEVELOP_URL;
			String url;
			switch(serviceLocation){
				case AML_SERVICE_LOCATION_USA:
					url = GBL_DEVICE_DEVELOP_URL;
					break;
				case AML_SERVICE_LOCATION_CHINA:
					url = GBL_DEVICE_DEVELOP_CN_URL;
					break;
				case AML_SERVICE_LOCATION_EUROPE:
					url = GBL_DEVICE_DEVELOP_EU_URL;
					break;
				default:
					url = GBL_DEVICE_DEVELOP_URL;
					break;
			}

			return url;
		}
		if (serviceType == AML_STAGING_SERVICE) {

			String url;
			switch(serviceLocation){
				case AML_SERVICE_LOCATION_USA:
					url = GBL_DEVICE_STAGING_URL;
					break;
				case AML_SERVICE_LOCATION_CHINA:
					url = GBL_DEVICE_STAGING_CN_URL;
					break;
				case AML_SERVICE_LOCATION_EUROPE:
					url = GBL_DEVICE_STAGING_EU_URL;
					break;
				default:
					url = GBL_DEVICE_STAGING_URL;
					break;

			}
			return url;
		}
		if (serviceType == AML_DEMO_SERVICE) {

			String url;
			switch(serviceLocation){
				case AML_SERVICE_LOCATION_USA:
					url = GBL_DEVICE_DEMO_URL;
					break;
				case AML_SERVICE_LOCATION_CHINA:
					url = GBL_DEVICE_DEMO_CN_URL;
					break;
				case AML_SERVICE_LOCATION_EUROPE:
					url = GBL_DEVICE_DEMO_EU_URL;
					break;
				default:
					url = GBL_DEVICE_DEMO_URL;
					break;

			}
			return url;

		}
		saveToLog("%s, %s, %s:%s, %s", "E", "SystemUtils", "serviceType", "null", "deviceServiceBaseURL");
		return null;
	}
	static String appTriggerServiceBaseURL() {
		if (serviceType == AML_DEVICE_SERVICE) {
			if (amlAppTriggerService == null) {
				amlAppTriggerService = "https://" + appId + GBL_APPTRIGGER_SUFFIX_URL;
			}
			return amlAppTriggerService;
		}
		if (serviceType == AML_FIELD_SERVICE) {
			//return GBL_APPTRIGGER_FIELD_URL;
			return (AML_SERVICE_LOCATION_USA.equals(serviceLocation)) ? GBL_APPTRIGGER_FIELD_URL : GBL_APPTRIGGER_FIELD_CN_URL;
		}
		if (serviceType == AML_DEVELOPMENT_SERVICE) {
			//return GBL_APPTRIGGER_DEVELOP_URL;
			return (AML_SERVICE_LOCATION_USA.equals(serviceLocation)) ? GBL_APPTRIGGER_DEVELOP_URL : GBL_APPTRIGGER_DEVELOP_CN_URL;
		}
		if (serviceType == AML_STAGING_SERVICE) {
			//return GBL_APPTRIGGER_STAGING_URL;
			return (AML_SERVICE_LOCATION_USA.equals(serviceLocation)) ? GBL_APPTRIGGER_STAGING_URL : GBL_APPTRIGGER_STAGING_CN_URL;
		}
		if (serviceType == AML_DEMO_SERVICE) {
			//return GBL_APPTRIGGER_DEMO_URL;
			return (AML_SERVICE_LOCATION_USA.equals(serviceLocation)) ? GBL_APPTRIGGER_DEMO_URL : GBL_APPTRIGGER_CN_DEMO_URL;
		}
		saveToLog("%s, %s, %s:%s, %s", "E", "SystemUtils", "serviceType", "null", "deviceServiceBaseURL");
		return null;
	}

	
	// assume field and development/production user service use the same servers
	static String logServiceBaseURL() {
		if (serviceType == AML_DEVICE_SERVICE) {
			//return GBL_LOG_FIELD_URL;
			return (AML_SERVICE_LOCATION_USA.equals(serviceLocation)) ? GBL_LOG_FIELD_URL : GBL_LOG_FIELD_CN_URL;
		}
		if (serviceType == AML_FIELD_SERVICE) {

			String url;

			switch(serviceLocation){
				case AML_SERVICE_LOCATION_USA:
					url = GBL_LOG_FIELD_URL;
					break;
				case AML_SERVICE_LOCATION_CHINA:
					url = GBL_LOG_FIELD_CN_URL;
					break;
				case AML_SERVICE_LOCATION_EUROPE:
					url = GBL_LOG_FIELD_EU_URL;
					break;
				default:
					url = GBL_LOG_FIELD_URL;
					break;

			}
			return url;
		}
		if (serviceType == AML_DEVELOPMENT_SERVICE) {
			//return GBL_LOG_DEVELOP_URL;
			return (AML_SERVICE_LOCATION_USA.equals(serviceLocation)) ? GBL_LOG_DEVELOP_URL : GBL_LOG_DEVELOP_CN_URL;
		}
		if (serviceType == AML_STAGING_SERVICE) {
			//return GBL_LOG_STAGING_URL;
			return (AML_SERVICE_LOCATION_USA.equals(serviceLocation)) ? GBL_LOG_STAGING_URL : GBL_LOG_STAGING_CN_URL;
		}
		if (serviceType == AML_DEMO_SERVICE) {
			//return GBL_LOG_DEMO_URL;
			return (AML_SERVICE_LOCATION_USA.equals(serviceLocation)) ? GBL_LOG_DEMO_URL : GBL_LOG_DEMO_CN_URL;
		}
		saveToLog("%s, %s, %s:%s, %s", "E", "SystemUtils", "serviceType", "null", "logServiceBaseURL");
		return null;
	}
	
	static String lanIpServiceBaseURL(String lanIp) {
		String url = String.format("http://%s/", lanIp);
		return url;
	}


	//---------------------------------- Logging: File & Console ----------------------
	public static void saveToLog(String format, Object...args) {
		List<Object> arrayOfArgs = new ArrayList<Object>();
		
		for (Object arg : args) {
			arrayOfArgs.add(arg);
		}
		/*String msg = String.format(format, arrayOfArgs.toArray());
		
		if (loggingEnabled == NO || log == null) { 	// logging disabled or error during init
			consoleMsg(msg, loggingLevel);
			//Log.d("", msg);	// log to console
		} else {
			log.info(msg);	// log to file and console
		}*/
		if(!arrayOfArgs.isEmpty()){
			String level = "I";
			String tag = "aAML";
			if(arrayOfArgs.size() >= 1){
				level = (String) arrayOfArgs.get(0);
			}
			if(arrayOfArgs.size() >=2){
				tag = (String) arrayOfArgs.get(1);
			}

			switch (level){
				case "E":
					logError(tag, 0, System.currentTimeMillis(), format, args);
					break;
				case "W":
					logWarning(tag, 0, System.currentTimeMillis(), format, args);
					break;
				case "I":
					logInfo(tag, 0, System.currentTimeMillis(), format, args);
					break;
				case "D":
					logDebug(tag, 0, System.currentTimeMillis(), format, args);
					break;
				case "V":
					logVerbose(tag, 0, System.currentTimeMillis(), format, args);
					break;
				default:
					logInfo(tag, 0, System.currentTimeMillis(), format, args);
			}
		}

	}

    // methods to log using AylaLogManager
    public static void logDebug(String tag, int flag, long time, String format, Object...args){
        if(logManager == null){
            initLogger();
        }
        logManager.log(tag, AylaLogManager.LOG_LEVEL.debug, format, args);
    }
    public static void logInfo(String tag, int flag, long time, String format, Object...args){
        if(logManager == null){
            initLogger();
        }
        logManager.log(tag, AylaLogManager.LOG_LEVEL.info, format, args);
    }

    public static void logError(String tag, int flag, long time, String format, Object...args){
        if(logManager == null){
            initLogger();
        }
        logManager.log(tag, AylaLogManager.LOG_LEVEL.error, format, args);
    }
    public static void logWarning(String tag, int flag, long time, String format, Object...args){
        if(logManager == null){
            initLogger();
        }
        logManager.log(tag, AylaLogManager.LOG_LEVEL.warning, format, args);
    }

    public static void logVerbose(String tag, int flag, long time, String format, Object...args){
        if(logManager == null){
            initLogger();
        }
        logManager.log(tag, AylaLogManager.LOG_LEVEL.verbose, format, args);
    }

    public static AylaLogger createAppLogger(String regex){
        if(logManager == null){
            initLogger();
        }
        return logManager.addLogger(regex);
    }

    /**
	 * Display (or not) console messages based on logging level
	 * 
	 * @param msg - message to display. First character must be E, W, or I
	 * @param loggingLevel current logging level for the library
	 */
	public static void consoleMsg(String msg, int loggingLevel) {
		
		// switch on message logging level
		switch(msg.charAt(0)) {
		case 'E':
			if ((0x01 & loggingLevel) != 0) Log.e("", msg);	// log to console
			break;

		case 'W':
			if ((0x02 & loggingLevel) != 0) Log.w("", msg);	// log to console
			break;

		case 'I':
			if ((0x04 & loggingLevel) != 0) Log.i("", msg);	// log to console
			break;

		default:
			Log.v("", msg);	// log to console
		}
	}
	
	public static void loggingInit() {
		if(loggingEnabled == YES){
			initLogger();
		}

	}

	public static String getLogFilePath() {
		return AylaLogManager.getLogFilePath();
	}
	public static String getSupportEmailAddress() {
		// mobile-libraries@aylanetworks.com
		return supportEmailAddress;
	}
	public static void setSupportEmailAddress(String newSupportEmailAddress) {
		// mobile-libraries@aylanetworks.com
		supportEmailAddress= newSupportEmailAddress;
	}
	public static String getLogMailSubject(String appId) {
		// AppId:aAML_id,LibVer:3.15_ENG,OS:Android_3.15_ENG
		String logMailSubject = String.format("AppId:%s,LibVer:%s,OS:Android_%s", appId, aamlVersion, aamlVersion);
		return logMailSubject;
	}
	
	// Send library logs to Ayla Support
	public static Intent emailLogsToSupport(String appId) {
		// Get the content
    	String[] supportEmailAddress = {AylaSystemUtils.getSupportEmailAddress()};
        String emailSubject = AylaSystemUtils.getLogMailSubject(appId);
        String emailMessage = "Ayla Library log file attached.";
        String libraryLogSrcFilePath = AylaLogManager.getLogFilePath();
        String libraryLogDstFilePath = AylaLogManager.getLogFilePath() + ".log";

        // copy of aml_log --> aml_log.log
        try {
        	File file = new java.io.File(libraryLogSrcFilePath);
			if (file.exists()) {
				AylaSystemUtils.copyFile(libraryLogSrcFilePath,libraryLogDstFilePath);
			} else {
				saveToLog("%s, %s, %s:%s, %s", "I", "SystemUtils", "notFound:", libraryLogSrcFilePath, "emailLogsToSupport");
				String infoMsg = "Library log file " + libraryLogSrcFilePath + "not found";
				Toast.makeText(appContext, infoMsg, Toast.LENGTH_LONG).show();
				return null;
			}
        } catch (IOException e) {
        	saveToLog("%s, %s, %s:%s, %s", "I", "SystemUtils", "IOException:", e.getCause(), "emailLogsToSupport");
        	String infoMsg = "Error copying log file " + libraryLogSrcFilePath + " to " + libraryLogDstFilePath;
			Toast.makeText(appContext, infoMsg, Toast.LENGTH_LONG).show();
        	e.printStackTrace();
        	return null;
        }
        
        // create the email
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, supportEmailAddress);
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, emailSubject);
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, emailMessage);
        libraryLogDstFilePath = "file://" + libraryLogDstFilePath;
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(libraryLogDstFilePath));
        
        return emailIntent;
    }
		
	public static boolean copyFile(String from, String to) throws IOException {
		try {
		    //int bytesum = 0;
		    int byteread = 0;
		    File oldfile = new File(from);
		    if (oldfile.exists()) {
		        InputStream inStream = new FileInputStream(from);
		        FileOutputStream fs = new FileOutputStream(to);
		        byte[] buffer = new byte[1444];
		        while ((byteread = inStream.read(buffer)) != -1) {
		            //bytesum += byteread;
		            fs.write(buffer, 0, byteread);
		        }
		        inStream.close();
		        fs.close();
		    }
		    return true;
		} catch (Exception e) {
		    return false;
		}
	}
	
	// Write json formated data to the log service, best effort
	public static void sendToLogService(String dsn,  String text, String level, String module, String time, boolean delayExecution) {
		Map<String, String> callParams = new HashMap<String, String>();
		callParams.put("dsn", dsn);
		callParams.put("text", text);
		callParams.put("level", level);
		callParams.put("mod", module);
		callParams.put("time", time);
		AylaLogService.sendLogServiceMessage(callParams, delayExecution);
		saveToLog("%s, %s, %s:%s, %s", "I", "SystemUtils", "text", text, "sendToLogService");
	}
	
	public static void sendQueuedLogServiceMessages() {
		AylaLogService.sendLogServiceMessage(null, false);
	}
	
	//------------------- General Helper Methods ------------------------
	static Boolean writeToFile(byte[] blob, String directory, String fileName) {
		try {
            File file = new File(directory, fileName);
            if (file.exists()) {
				file.delete();
			}
            file.createNewFile();
            
            FileOutputStream outStream = new FileOutputStream(file);
            
            outStream.write(blob);
            outStream.flush();
            outStream.close();
            
	    	return true;	
	    } catch (IOException e) {
	    	String eMsg = (e.getLocalizedMessage() == null) ? e.toString() : e.getLocalizedMessage();
			AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%s", "E", "AylaSystemUtils", "Error", AylaNetworks.AML_GENERAL_EXCEPTION, "eMsg", eMsg, "writeToFile");
			return false;
	    }
	}




	/**
	 * @param path
	 * */
	static byte[] readFromFile(String path) {
		File file = new File(path);
		return readFromFile(file, file.length());
	}

	static byte[] readFromFile(File file, long count) {
		if ( count<=0 || file == null || !file.exists()) {
			return null;
		}

		// TODO: The boundary of int in java, is the ceiling of a Aylablob size. Improve here.
		byte[] result = new byte[(int)count];
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			in.read(result);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (in!=null) {
				try {
					in.close();
					in = null;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}
	
	
	
	/**
	 * {"Hello", "World"} => "["Hello", "World"]"
	 * */
	public static String arrayToJsonString(final String[] src) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		
		if (src == null || src.length<1) {
			sb.append("]");
			return sb.toString();
		}
		
		for (int i=0; i <src.length; i++) {
			if (i!=0) {
				sb.append(",");
			}
			sb.append("\"").append(src[i]).append("\"");
		}
			
		sb.append("]");
		return sb.toString();
	}// end of arrayToJsonString       
	
	
	/**
	 * Check if a string is a valid json string.
	 * 
	 *  @param json the json string
	 *  @return true if it is valid, false otherwise.
	 * */
	static boolean isValidJson (final String json) {
		try {
			JSONObject obj = new JSONObject(json);
		} catch (JSONException je) {
			saveToLog("%s, %s, %s, %s.", "D", tag, "isValidJson", json + " is not a valid json string");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}// end of isValidJson
	
	
	public static byte[] stringToBytes(String aString, String errMsg) {
		byte[] someBytes = null;

		try {
			someBytes = aString.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			if (errMsg != null) {
				AylaSystemUtils.saveToLog("%s", errMsg);
			}
			e.printStackTrace();
		}
		return someBytes;
	}

	public static String bytesToString(byte[] someBytes, String errMsg) {
		String aString = null;

		try {
			aString = new String(someBytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			if (errMsg != null) {
				AylaSystemUtils.saveToLog("%s", errMsg);
			}
			e.printStackTrace();
		}
		return aString;
	}
	
	static //----------------------- System Info -------------------------
	void systemInfo() {
		saveToLog("%s, %s, %s:%s, %s", "I", "SystemUtils", "Log file path", AylaLogManager.logFilePath, "systemInfo");
		saveToLog("%s, %s, %s:%s, %s", "I", "SystemUtils", "AML_Version", AylaNetworks.aamlVersion, "systemInfo");
		saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "SystemUtils", "Android_Version", Build.VERSION.RELEASE, "Codename", Build.VERSION.CODENAME,"systemInfo");
		saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "SystemUtils", "Product", Build.PRODUCT, "Type", Build.TYPE,"systemInfo");
		saveToLog("%s, %s, %s:%sMB, %s:%sMB, %s:%sMB,%s", "I", "SystemUtils",
				"freeMemeory", freeMemory(), "freeInternalStorage", freeInternalStorage(), "freeExternalStorage", freeExternalStorage(), "systemInfo");
		String ipAddr = getLocalIpv4Address();
		if (ipAddr != null) {
			saveToLog("%s, %s, %s:%s, %s", "I", "SystemUtils", "IP_Address", ipAddr, "systemInfo");
		}
	}

	private static long freeMemory() {
		MemoryInfo mi = new MemoryInfo();
		ActivityManager activityManager = (ActivityManager)appContext.getSystemService(Activity.ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(mi);
		long freeMemory = mi.availMem / 1048576L; // in megaByes
		return freeMemory;
	}

	private static long freeInternalStorage()  
	{
		StatFs statFs = new StatFs(Environment.getDataDirectory().getPath()); 
		long free;
		if ( Build.VERSION.SDK_INT >= 18) { // Available starting from API 18
			free  = ( statFs.getAvailableBlocksLong() *  statFs.getBlockSizeLong()) / 1048576L;
		} else { // Deprecated after 18
			free  = ((long)statFs.getAvailableBlocks() *  (long)statFs.getBlockSize()) / 1048576L;
		}
		return free;
	}

	private static long freeExternalStorage() 
	{
		StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath()); 
		long free;
		if ( Build.VERSION.SDK_INT >= 18) { // Available starting from API 18
			free = ( statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong()) / 1048576L;
		} else { // Deprecated after 18
			free = ((long)statFs.getAvailableBlocks() *  (long)statFs.getBlockSize()) / 1048576L;
		}
		return free;
	}

	// General helper routines
	static void sleep(int milliSeconds) {
		SystemClock.sleep(milliSeconds);
	}

	
	/**
	 * Return the local IPV4 address.
	 * 
	 * @return local ipv4 address.
	 * */
	public static String getLocalIpv4Address() {
        if (appContext == null) {
            return null;
        }

        WifiManager wifiManager = (WifiManager)appContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        if (ip == 0) {
            // We're not connected
            return null;
        }
        return Formatter.formatIpAddress(ip);
	}

	public static java.text.SimpleDateFormat gmtFmt;
	static {
		//2012-04-30T22:57:15Z
		gmtFmt = new java.text.SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
		gmtFmt.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	
	/**
	 * Truncat a string to a specified length.
	 * 
	 * @param value the string
	 * @param length we wish to truncat in.
	 * @return the new string
	 * */
	public static String truncate(String value, int length)	{
	  if (value != null && value.length() > length)
	    value = value.substring(0, length);
	  return value;
	}

	public static boolean isNotifyOutstandingEnabled(){
		return notifyOutstandingEnabled;
	}

	public static void setNotifyOutstandingEnabled(boolean isEnabled){
		notifyOutstandingEnabled = isEnabled;
	}
}






