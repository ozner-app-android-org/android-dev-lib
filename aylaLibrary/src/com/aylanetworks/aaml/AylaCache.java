//
//  AylaCache.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 8/30/13.
//  Copyright (c) 2013 Ayla Networks. All rights reserved.
//
package com.aylanetworks.aaml;

import java.util.HashMap;
import java.util.zip.CRC32;

import android.text.TextUtils;

/**
 Overview
 
 This class controls library caching. One important difference from traditional
 caching is that some caching is required for LAN Mode Enablement (LME) to function.
 Therefore, if caching is totally disabled, and there is no service reachability,
 direct communication with Ayla devices will not be possible. Therefore, disabling
 caching is not recommended, but is included for completeness. Caching is fully
 enabled by default.
 <p>
Uses
<p>
Caching allows for devices and properties to be saved so they are available when
service connectivity is not present. The typical use case here is WLAN sign-in
(device access w/o service authentication). 
<p>
If present, cached values are also used when AylaSystemUtils.slowConnection equals 
AylaNetworks.YES. If no cached values have been saved, then the values will be
retrieved from the service. Think of slowConnection as a preference, but not a
requirement, to used cached values.
 <p>
Indications
<p>
Creating, updating, and reading of caches is handled transparently by the
Ayla Mobile Library. When cached values are returned in getDevices() or
getProperties() the return code is set to 203. This provide the opportunity
for the application designer to handle cached values differently than
service/device values.
<p>
If caching is disabled and neither the service or device is reachable, the
public method will return 404.
<p>
Saving, Getting & Testing
<p>
See cache selection in AylaNetworks.java for the most current values:
<p>
	AML_CACHE_DEVICE 	 0x01<p>
	AML_CACHE_PROPERTY 	 0x02<p>
	AML_CACHE_LAN_CONFIG 0x04<p>
	AML_CACHE_SETUP 	 0x08<p>
	AML_CACHE_GROUP 	 0x10<p>
	AML_CACHE_NODE		 0x20<p>
	AML_CACHE_ALL 		 0xFF<p>
 */
public class AylaCache extends AylaSystemUtils {

	private static int caches = 0xFF; // all caches enabled by default
	private static HashMap<String, Integer> cacheCRCs = new HashMap<String, Integer>();	// unique cache name, crc32
	
	// --------------------------- Application scoped methods ------------------------
	/** 
	 * Clear all caches
	 */
	public static void clearAll() {
		clearCaches(AML_CACHE_ALL);
	}

	/**
	 * Clear caches based on AML_CACHE_XXXXX
	 * @param cachesToClear
	 */
	public static void clear(int cachesToClear) {
		clearCaches(cachesToClear);
	}

	/**
	 * Determine if a particular cache(s) is/are enabled
	 * @param cacheToCheck - bit mask based on AML_CACHE_XXXXX
	 * @return true if the caches to check are enabled
	 */
	public static boolean cacheEnabled(int cacheToCheck) {
		return ((caches & cacheToCheck) == cacheToCheck);
	}
	
	/**
	 * WARNING - enabling/disabling individual caches may lead to unexpected behavior, consider clearCache() instead<p>
	 * Enable cache(s) based on AM_CACHE_XXXXX
	 * @param cachestoSet
	 */
	public static void enable(int cachestoSet) {
		caches |= cachestoSet;
		saveToLog("%s, %s, %s:0x%02X, %s", "I", "AylaCache", "caches", caches, "enable");
	}

	/**
	 * WARNING - disabling/enabling individual caches may lead to unexpected behavior, consider clearCache() instead<p>
	 * Disable cache(s) based on AM_CACHE_XXXXX
	 * @param cachesToDisable
	 */
	public static void disable(int cachesToDisable) {
		caches &= ~cachesToDisable;
		saveToLog("%s, %s, %s:0x%02X, %s", "I", "AylaCache", "caches", caches, "disable");
	}

	/**
	 * @return caches bit mask based on AML_CACHE_XXXXX
	 */
	public static int caches() {
		return caches;
	}


// --------------- Library scope methods ---------------------
	/**
	 * get a cache from storage w/o a unique identifier<p>
	 * used to retrieve top level caches like devices and groups
	 *
	 * @param cacheType - cache type based on AM_CACHE_XXXXX
	 * @return cache value from storage
	 */
	static String get(int cacheType) {
		return getCache(cacheType, null);
	}
	
	/**
	 * get a cache from storage<p>
	 * used to retrieve device specific caches like properties and LAN config Info
	 * 
	 * @param cacheType - cache type based on AM_CACHE_XXXXX
	 * @param uniqueId - unique identifier appended to cache type prefix
	 * @return cache value from storage
	 */
	static String get(int cacheType, String uniqueId) {
		return getCache(cacheType, uniqueId);
	}
	
	/**
	 * save a cache from storage w/o unique name identifier<p>
	 * used to save top level caches like devices and groups
	 * 
	 * @param type - one of AM_CACHE_XXXXX
	 * @param valueToCache - string data written to storage
	 */
	static void save(int type, String valueToCache) {
		if (valueToCache == null) { //v3.07
			valueToCache = "";
		}
		saveCache(type, null, valueToCache);
	}
	
	/**
	 * save a cache from storage w/o unique name identifier<p>
	 * used to save device specific level caches like properties and LAN config info
	 * 
	 * @param type - on of AML_CACHE_XXXXX
	 * @param uniqueId - appended to type base identifier, typically the device dsn
	 * @param valueToCache - string data written to storage
	 */
	static void save(int type, String uniqueId, String valueToCache) {
		if (valueToCache == null) { // v3.07
			valueToCache = "";
		}
		saveCache(type, uniqueId, valueToCache);
	}
	
	// ------------------------- Class scoped methods -----------------------------------
	/**
	 * get a cache from storage<p>
	 * used to retrieve device specific caches like properties and LAN config Info
	 * 
	 * @param type - cache type based on AM_CACHE_XXXXX
	 * @param uniqueId - unique identifier appended to cache type prefix, typically the device dsn.
	 * @return cached value. Null if no data is stored under the cache name.
	 */
	private static String getCache(int type, String uniqueId) {
		String cachedValue;
		String cacheName;
		
		if ((type & caches) != type) {
			saveToLog("%s, %s, %s:0x%02X, %s", "I", "AylaCache", "cacheDisabled", type, "getCache");
			return "";	// cache is disabled
		} 
		
		switch (type) {
			case AML_CACHE_DEVICE:
				cacheName = AML_CACHE_DEVICE_PREFIX ;
				break;
			case AML_CACHE_PROPERTY:
				cacheName = AML_CACHE_PROPERTY_PREFIX + uniqueId;
				break;
			case AML_CACHE_LAN_CONFIG:
				cacheName = AML_CACHE_LAN_CONFIG_PREFIX + uniqueId;
				break;
			case AML_CACHE_SETUP:
				cacheName = AML_CACHE_SETUP_PREFIX;
				break;
			case AML_CACHE_GROUP:
				cacheName = AML_CACHE_GROUP_PREFIX;
				break;
			case AML_CACHE_NODE:
				cacheName = AML_CACHE_NODE_PREFIX + uniqueId;
				break;
			default:
				saveToLog("%s, %s, %s:0x%02X, %s", "E", "AylaCache", "type", type, "getCache");
				cachedValue = "";
				return cachedValue;
		}
		
		cachedValue = loadSavedSetting(cacheName, "");
		saveToLog("%s, %s, %s:%s %s:%d, %s", "I", "AylaCache", "cacheName", cacheName, "cachedValue.length", cachedValue.length(), "getCache");
		return cachedValue;
	}
	
	/**
	 * save a cache from storage w/o unique name identifier<p>
	 * used to save device specific level caches like properties and LAN config info
	 * 
	 * @param type - one of AML_CACHE_XXXXX
	 * @param uniqueId - unique identifier appended to cache type prefix, typically the device dsn
	 * @param valueToCache - string value to write to storage
	 */
	private static void saveCache(int type, String uniqueId, String valueToCache) {
		String cacheName;
		
		if ((type & caches) != type) {
			saveToLog("%s, %s, %s:0x%02X, %s", "I", "AylaCache", "cacheDisabled", type, "saveCache");
			return;	// cache is disabled
		}
		
		switch (type) {
			case AML_CACHE_DEVICE:
				cacheName = AML_CACHE_DEVICE_PREFIX ;
				break;
			case AML_CACHE_PROPERTY:
				cacheName = AML_CACHE_PROPERTY_PREFIX + uniqueId;
				break;
			case AML_CACHE_LAN_CONFIG:
				cacheName = AML_CACHE_LAN_CONFIG_PREFIX + uniqueId;
				break;
			case AML_CACHE_SETUP:
				cacheName = AML_CACHE_SETUP_PREFIX;
				break;
			case AML_CACHE_GROUP:
				cacheName = AML_CACHE_GROUP_PREFIX;
				break;
			case AML_CACHE_NODE:
				cacheName = AML_CACHE_NODE_PREFIX + uniqueId;
				break;
			default:
				saveToLog("%s, %s, %s:0x%02X, %s", "E", "AylaCache", "type", type, "saveCache");
				return;
		}
		
		// calculate crc
		CRC32 thisCRC = new CRC32();
		thisCRC.update(valueToCache.getBytes());
		Integer thisCrcValue = (int)thisCRC.getValue();
		
		// get cacheddValue crc
		Integer cachedCrcValue = cacheCRCs.get(cacheName);
	
		// Don't write if no changes
		if (cachedCrcValue != null && cachedCrcValue.intValue() == thisCrcValue.intValue()) {
			saveToLog("%s, %s, %s:%s %s:%d, %s", "I", "AylaCache", "cacheName", cacheName,"valueToCache.length", valueToCache.length(), "saveCache_noChange");
		} else {
			// save to hash
			cacheCRCs.put(cacheName, thisCrcValue);
			
			// save to storage
			saveSetting(cacheName, valueToCache);
			saveToLog("%s, %s, %s:%s %s:%d, %s", "I", "AylaCache", "cacheName", cacheName,"valueToCache.length", valueToCache.length(), "saveCache");
		}
	}
	
	/**
	 * This method will clear cached data from storage.<p>
	 * Caches cleared depends on bits matching mask based on AM_CACHE_XXXXX
	 * @param type - one of AML_CACHE_XXXXX
	 */
	private static void clearCaches(int type) {
		// remove devices, lan config, & properties caches
		int count = 0;
		String savedJsonDeviceContainers;
		
		savedJsonDeviceContainers = loadSavedSetting(AML_CACHE_DEVICE_PREFIX , "");
		//savedJsonDeviceContainers = "[{\"node\": { \"dsn\": \"VR00ZN000000022\",\"device_type\": \"Node\",\"node_type\": \"Zigbee\"}}]"; // djm testing only
		if (!TextUtils.isEmpty(savedJsonDeviceContainers)) {
			AylaDeviceContainer[] deviceContainers = AylaSystemUtils.gson.fromJson(savedJsonDeviceContainers,AylaDeviceContainer[].class);
			AylaDevice[] devices = new AylaDevice[deviceContainers.length];
			for (AylaDeviceContainer deviceContainer : deviceContainers) {
				
				devices[count]= deviceContainer.device;
				
				if ((type & AML_CACHE_LAN_CONFIG) != 0x00) {
					String lanModeConfigName = AML_CACHE_LAN_CONFIG_PREFIX + devices[count].dsn; // remove saved lan config cache 
					saveSetting(lanModeConfigName, "");
					cacheCRCs.remove(lanModeConfigName);
				}
					
				if ((type & AML_CACHE_PROPERTY) != 0x00) {
					String lanModePropertiesName  =AML_CACHE_PROPERTY_PREFIX + devices[count].dsn; // remove saved properties caches 
					saveSetting(lanModePropertiesName, "");
					cacheCRCs.remove(lanModePropertiesName);
				}
				
				if ((type & AML_CACHE_NODE) != 0x00) {
					String lanModeNodesName = AML_CACHE_NODE_PREFIX + devices[count].dsn; // remove saved device node caches
					saveSetting(lanModeNodesName, "");
					cacheCRCs.remove(lanModeNodesName);
				}
				
				count++;
			}
			
			if ((type & AML_CACHE_DEVICE) != 0x00) {
				saveSetting(AML_CACHE_DEVICE_PREFIX , "");// remove saved devices cache
				cacheCRCs.remove(AML_CACHE_DEVICE_PREFIX);
			}
			
			if ((type & AML_CACHE_SETUP) != 0x00) {
				saveSetting(AML_CACHE_SETUP_PREFIX, ""); // remove new devices that have completed setup
				cacheCRCs.remove(AML_CACHE_SETUP_PREFIX);
			}
			
			if ((type & AML_CACHE_GROUP) != 0x00) {
				saveSetting(AML_CACHE_GROUP_PREFIX, "");// remove all groups from cache
				cacheCRCs.remove(AML_CACHE_GROUP_PREFIX);
			}

			saveToLog("%s, %s, %s:0x%02X, %s", "I", "AylaCache", "cachesCleared", type, "clearCaches");
		} else {
			return;
		}
	}
}



