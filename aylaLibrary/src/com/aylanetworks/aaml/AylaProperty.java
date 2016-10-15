//
//  AylaProperty.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 8/15/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.annotations.Expose;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

// ------------------------------ Property ----------------------------------
class AylaPropertyContainer {
	@Expose
	public AylaProperty property = null;
}

public class AylaProperty extends AylaSystemUtils {

	private final static String tag = AylaProperty.class.getSimpleName();
	
	@Expose
	public String baseType;
	@Expose
	public String value;
	@Expose
	public String dataUpdatedAt;
	@Expose
	public String name;
	@Expose
	public String displayName;
	@Expose
	public String direction;
	@Expose
	boolean readOnly;
	@Expose
	Map<String, String> metadata;
	
	@Expose
	Number key;

	@Expose
	public String type;
	
	@Expose
	public String product_name; 
	
	/* For datapoint ack. */
	@Expose
	public boolean ackEnabled;
	@Expose
	public int ackStatus;
	@Expose
	public int ackMessage;
	@Expose
	public String ackedAt;
	
	// derived
	@Expose
	public AylaDatapoint datapoint;
	@Expose
	public AylaDatapoint[] datapoints;

	@Expose
	public AylaPropertyTrigger propertyTrigger;
	@Expose
	public AylaPropertyTrigger[] propertyTriggers;
	
	@Expose
	public AylaBlob blob;
	@Expose
	public AylaBlob[] blobs;
	
	// constructors
	public AylaProperty() {
		datapoint = new AylaDatapoint(); // initialize for pass through calls
		propertyTrigger = new AylaPropertyTrigger(); // initialize for pass through calls
	}

	public AylaProperty(Number key) {
    	this();
		this.key = key;
	}
	
	static Boolean readPropertiesCacheOnce = true;
	
	public String baseType() {
		return baseType;
	}

	public String name() {
		return name;
	}

	public String direction() {
		return direction;
	}

	Number getKey() {
		return key;
	}
 	
	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");

		result.append(this.getClass().getName() + " Object {" + NEW_LINE);
		result.append(" baseType: " + baseType + NEW_LINE);
		result.append(" name: " + name + NEW_LINE);
		result.append(" value: " + value + NEW_LINE);
		result.append(" direction: " + direction + NEW_LINE );
		result.append(" data_updated_at: " + dataUpdatedAt + NEW_LINE);
		result.append(" display_name: " + displayName + NEW_LINE);
		result.append(" product_name: " + product_name + NEW_LINE);
		result.append(" ack_enabled: " + ackEnabled + NEW_LINE);
		result.append(" ack_status: " + ackStatus + NEW_LINE);
		result.append(" ack_message: " + ackMessage + NEW_LINE);
		result.append(" acked_at: " + ackedAt + NEW_LINE);
		result.append("}");
		return result.toString();
	}

	@Override
	public boolean equals(Object other) {
		if ( this == other ) {
			return true;
		}
		if ( other == null ) {
			return false;
		}
		if ( other.getClass() != this.getClass() ) {
			return false;
		}
		AylaProperty otherProperty = (AylaProperty)other;
		return TextUtils.equals(name, otherProperty.name) && TextUtils.equals(value, otherProperty.value);
	}

	/**
	 * Same as {@link AylaProperty#getProperties(Handler, AylaDevice, Map, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public static AylaRestService getProperties(AylaDevice device, Map<String, String> callParams) {
		return getProperties(null, device, callParams, true);
	}

	/**
	 * Same as {@link AylaProperty#getProperties(Handler, AylaDevice, Map, Boolean)} with no option to setup the call to execute later.
	 * **/
	public static AylaRestService getProperties(Handler mHandle, AylaDevice device, Map<String, String> callParams) {
		return getProperties(mHandle, device, callParams, false);
	}

	/**
	 * Gets all properties summary objects associated with the device from Ayla device Service. Use getProperties when ordering is not important.
	 * @param mHandle is where result would be returned.
	 * @param device is the device requested properties belong to.
	 * @param callParams allows for specifying the property names of a subset of properties to retrieve. These callParams are ignored for calls
	made to the Ayla field service and all properties are retrieved.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event
	 * @return AylaRestService object
	 */    //TODO; clean up logic for this function. Should not be static, and package visibility.
	public static AylaRestService getProperties(Handler mHandle, AylaDevice device, Map<String, String> callParams, Boolean delayExecution) {
		Number devKey = device.getKey().intValue();
		AylaRestService rs = null;

		if (device.properties == null) {
			device.initPropertiesFromCache();
		}
		// Return properties sent directly from a LAN Mode device to the caller
		// TODO: This is not valid.  Add valid active lan mode check. 
		if ( device.getLanModule() != null &&
				device.getLanModule().getSession() != null &&
				device.getLanModule().getSession().isNotifyOutstanding() &&
				AylaSystemUtils.isNotifyOutstandingEnabled() )
		{
			AylaLanModule module = device.getLanModule();
			AylaLanModule.AylaLanSession session = module.getSession();
            session.setNotifyOutstanding(false);

			try {
				AylaDevice lanDevice = AylaDeviceManager.sharedManager().deviceWithDSN(device.dsn);
				if(lanDevice != null &&
				   lanDevice.properties != null) {
					String jsonProperties = AylaSystemUtils.gson.toJson(lanDevice.properties,AylaProperty[].class);
					int requestType = AylaRestService.GET_PROPERTIES_LANMODE;
					if ( lanDevice.isNode() ) {
						requestType = AylaRestService.GET_NODE_PROPERTIES_LANMODE;
					}
					rs = new AylaRestService(mHandle, "getPropertiesNotifyLanMode", requestType);
					saveToLog("%s, %s, %s:%s, %s", "I", "AylaProperties", "notifyOutStanding", "true", "getPropertiesNotify_lanmode");
					returnToMainActivity(rs, jsonProperties, 200, 0, delayExecution);
					return rs;
				} else {
					Log.e("BSK", "Notify: lanDevice: " + lanDevice );
					rs = new AylaRestService(mHandle, "AylaProperties", AylaRestService.GET_PROPERTIES_LANMODE);
					saveToLog("%s, %s, %s:%s, %s", "I", "AylaProperties", "getProperties", "null", "prop");
					returnToMainActivity(rs, null, 404, 0, delayExecution);
				}
				
			} catch (Exception e) {
				AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", "AylaProperties", "exception", e.getCause(), "getPropertiesNotify_lanMode");
				e.printStackTrace();
				rs = new AylaRestService(mHandle, "AylaProperties", AylaRestService.GET_PROPERTIES_LANMODE);
				returnToMainActivity(rs, null, 404, 0, delayExecution);
			}
		} else {
			AylaDeviceManager.sharedManager().addDevice(device, false);

		    // version 3.12, check if all requested properties have been retrieved, required for lan-mode retrieval
	        boolean isPropertiesCached = true;
	        if (device.isLanModeActive()) {
	        	String names = (callParams == null) ? null : callParams.get("names");
	        	// if they specify fetch, then ignore cached values, and fetch new ones
	        	boolean fetch = (((callParams == null) ? null : callParams.get("fetch")) != null);
	        	if (fetch) {
	        	    isPropertiesCached = false;
	        	} else if (names != null) {
					String propertyNames[] = names.split(" ");
					if(propertyNames.length > 0) {
						int counter = 0;
						if ( device.properties != null ) {
							for (String name : propertyNames) {
								for (AylaProperty property : device.properties) {
									if (!AML_LANMODE_IGNORE_BASETYPES.contains(property.baseType) && TextUtils.equals(name, property.name)) {
										counter++;
										break;
									}
								}
							}
						}

						if(counter != propertyNames.length)  {
							isPropertiesCached = false;
							saveToLog("%s, %s, %s:%s, %s", "I", "AylaProperties", "propertyNames", "NotCached", "getProperties_lanMode");
						}
					}
				}	
	        }
	        
	        // For GG Get Node Properties Lan Mode
	        if (device.isNode()
	        		&& device.isLanModeActive()
	        		&& device.properties != null && device.properties.length > 0) {
	        	saveToLog("%s, %s, %s, %s.", "D", tag, "getProperties", "GG Properties getProperties lan mode branch");
	        	
	        	int requestType = AylaRestService.GET_NODE_PROPERTIES_LANMODE;
	        	rs = new AylaRestService( mHandle, AylaSystemUtils.ERR_URL, requestType );            
	        	rs.info = device.dsn;
	        	
	        	int count = 0;
				String namesWithSpace = (callParams == null) ? null : callParams.get("names");
				String[] pns = null;
				if (!TextUtils.isEmpty(namesWithSpace)) {
					pns = namesWithSpace.split(" ");
				}
				for (AylaProperty property : device.properties) {
					if (!AML_LANMODE_IGNORE_BASETYPES.contains(property.baseType)) { // skip unnecessary base types
						if (TextUtils.isEmpty(namesWithSpace)) { // get all properties from the device
							String cmdRequest = device.lanModeToDeviceCmd(rs, "GET", "datapoint.json", property);
							saveToLog("%s, %s, %s:%s, %s", "I", "AylaProperties", "cmdRequest", cmdRequest, "getProperties_lanMode");
							count++;
						} 
						else {
							for (String pn : pns) {
								if (TextUtils.equals(pn, property.name)) {
									String cmdRequest = device.lanModeToDeviceCmd(rs, "GET", "datapoint.json", property);;
									saveToLog("%s, %s, %s:%s, %s", "I", "AylaProperties", "cmdRequest", cmdRequest, "getProperties_lanMode");
									count++;
									break;
								}
							}// end of pn loop    
						}// end of else 
					}
				}
				AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "AylaProperties", "count", count, "getProperties_lanMode");
				
				return rs;
	        }
	        
	        
			// Retrieve properties directly from the device in LAN Mode
			if (!device.isNode() && device.isLanModeActive() && device.properties!=null && isPropertiesCached) {
			    
				/*
				 * TODO: this is due to a historical bug, cannot change it at this time. 
				 * Investigate further and resolve this ticket: https://aylanetworks.atlassian.net/browse/AML-96
				 * */
			    int requestType = AylaRestService.GET_PROPERTIES_LANMODE;
			    if ((mHandle == null) && (device instanceof AylaDeviceNode)) {
	                // we actually want to use GET_PROPERTIES_LANMODE, 
	                // but as a work around we're going to just do GET_PROPERTIES
			        requestType = AylaRestService.GET_PROPERTIES;
			    }
				rs = new AylaRestService(mHandle, "getpropertiesLanmode_"+device.dsn, requestType, device.dsn);
				
				int count = 0;
				String names = (callParams == null) ? null : callParams.get("names");
				ArrayList<String> propertyNames = null;
				if (names != null) {
					propertyNames = new ArrayList<String>(Arrays.asList(names.split(" ")));
				}
				for (AylaProperty property : device.properties) {
					if (!AML_LANMODE_IGNORE_BASETYPES.contains(property.baseType)) { // skip unnecessary base types
						if (names == null) { // get all properties from the device
							String cmdRequest = device.lanModeToDeviceCmd(rs, "GET", "datapoint.json", property);
							saveToLog("%s, %s, %s:%s, %s", "I", "AylaProperties", "cmdRequest", cmdRequest, "getProperties_lanMode");
							count++;
						} else if (propertyNames.contains(property.name)) { // get some properties from the device
							String cmdRequest = device.lanModeToDeviceCmd(rs, "GET", "datapoint.json", property);
							saveToLog("%s, %s, %s:%s, %s", "I", "AylaProperties", "cmdRequest", cmdRequest, "getProperties_lanMode");
							count++;
						}
					}
				}
				AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "AylaProperties", "count", count, "getProperties_lanMode");
				
                if ((requestType == AylaRestService.GET_PROPERTIES) && (delayExecution == false)) {
                    rs.execute();
                }
				return rs;
			}
			
			// Retrieve properties from the Cloud Service
			else if (AylaReachability.isCloudServiceAvailable()) {
				saveToLog("%s, %s, %s:%s, %s", "I", "AylaProperties", "connectivity", AylaReachability.isCloudServiceAvailable()+"", "getPropertiesService");
				
				// Retrieve a subset of properties from the service 
				String names = (callParams == null) ? null : callParams.get("names");
				String url = String.format(Locale.getDefault(), "%s%s%d%s", deviceServiceBaseURL(), "devices/", devKey, "/properties.json");
				if (names != null) {
					String propertyNames[] = names.split(" ");
					if (propertyNames.length > 0) {
						String urlArray = AylaRestService.createUrlArray("names", propertyNames);
						url += urlArray;
					}
				}
					
				// String url = "http://ads-dev.aylanetworks.com/apiv1/devices/####/properties.json";
				rs = new AylaRestService(mHandle, url, AylaRestService.GET_PROPERTIES, device.dsn);
				saveToLog("%s, %s, %s:%s, %s", "I", "AylaProperties", "url", url, "getPropertiesService");
				if (delayExecution == false) {
					rs.execute();
				}
				return rs;
				
			// Can't retrieve properties
			} else {
				if (AylaCache.cacheEnabled(AML_CACHE_PROPERTY) && device.properties != null) {
					// use cached values
					rs = new AylaRestService(mHandle, "getPropertiesStorageLanMode", AylaRestService.GET_PROPERTIES_LANMODE);
					saveToLog("%s, %s, %s:%s, %s", "I", "AylaProperties", "getPropertiesReadCache", "true", "getPropertiesReadCache");
					String jsonProperties = AylaSystemUtils.gson.toJson(device.properties,AylaProperty[].class);
					returnToMainActivity(rs, jsonProperties, 203, 0, delayExecution);
				} else {
					// properties are not cached or is caching disabled, and the service is not reachable
					rs = new AylaRestService(mHandle, "AylaProperties", AylaRestService.GET_PROPERTIES_LANMODE);
					saveToLog("%s, %s, %s:%s, %s", "I", "AylaProperties", "getProperties", "null", "prop");
					returnToMainActivity(rs, null, 404, 0, delayExecution);
				}
			}
		} 
		return rs;
	}

	static String stripContainers(String jsonPropertyContainers, String dsn) throws Exception {
		int count = 0;
		String jsonProperties = "";
		AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "D", tag, "stripContainers", "dsn:" + dsn + "\njsonPropertyContainers:" + jsonPropertyContainers);
		try {
			AylaPropertyContainer[] propertyContainers = AylaSystemUtils.gson.fromJson(jsonPropertyContainers,AylaPropertyContainer[].class);
			AylaProperty[] properties = new AylaProperty[propertyContainers.length];
			for (AylaPropertyContainer propertyContainer : propertyContainers) {
				properties[count] = propertyContainer.property;
				properties[count].product_name = dsn;
				properties[count].updateDatapointFromProperty();
				properties[count].lanModeEnable();
				count++;
			}
			
			// Update DeviceManager   
			AylaDevice d = AylaDeviceManager.sharedManager().deviceWithDSN(dsn);
			if (d!=null) {
				d.mergeNewProperties(properties);
			}

			jsonProperties = AylaSystemUtils.gson.toJson(properties, AylaProperty[].class);
			
			if (count > 0) {
				lanModeEnable(properties, jsonProperties, dsn);
				AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "Properties", "count", count, "stripContainers");
			} else {
				AylaSystemUtils.saveToLog("%s %s %s:%s %s:%s %s", "I", "Properties", "count", count, "jsonPropertyContainers", jsonPropertyContainers, "stripContainers");
			}
			
			return jsonProperties;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s:%s %s", "E", "Properties", "count", count, "jsonPropertyContainers", jsonPropertyContainers, "stripContainers");
			e.printStackTrace();
			throw e;
		}
	}// end of stripContainers    
	
	protected static String stripContainer(String jsonPropertyContainer) throws Exception {
		String jsonProperty = "";
		try {
			AylaPropertyContainer propertyContainer = AylaSystemUtils.gson.fromJson(jsonPropertyContainer,AylaPropertyContainer.class);
			AylaProperty property = propertyContainer.property; 
			property.updateDatapointFromProperty();
			property.lanModeEnable();

			// Update AylaDeviceManager device copy by merging 
			AylaProperty[] ps = new AylaProperty[1];
			ps[0] = property;
			AylaDevice d = AylaDeviceManager.sharedManager().deviceWithDSN(property.product_name);
			if (d!=null) {
				d.mergeNewProperties(ps);
			}
			
			
			jsonProperty = AylaSystemUtils.gson.toJson(property,AylaProperty.class);
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "Properties", "property", property.toString(), "stripContainer");
			return jsonProperty;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", "Properties", "jsonPropertyContainer", jsonPropertyContainer, "stripContainer");
			e.printStackTrace();
			throw e;
		}
	}// end of stripContainer       



	/**
	 * Same as {@link AylaProperty#getPropertyDetail(Handler, Map, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService getPropertyDetail(Map<String, String> callParams) {
		return getPropertyDetail(null, callParams, true);
	}

	/**
	 * Same as {@link AylaProperty#getPropertyDetail(Handler, Map, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getPropertyDetail(final Handler mHandle, Map<String, String> callParams) {
		return getPropertyDetail(mHandle, callParams, false);
	}

	/**
	 * This instance method will instantiate a new property detail object from the Ayla device service and retrieve its associated triggers.  Use this call only if
	 * additional property detail is required. In almost all cases, using the getDatapoints or getTriggers method with properties summary object are the preferred
	 * and more efficient calls.
	 * @param mHandle is where result would be returned.
	 * @param callParams will be applied to qualify the property triggers associated with this property.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService getPropertyDetail(final Handler mHandle, Map<String, String> callParams, Boolean delayExecution) {
		Number propKey = this.getKey().intValue(); // Handle gson LazilyParsedNumber
		AylaRestService rs = null;

		//String url = "http://ads-dev.aylanetworks.com/apiv1/devices/##/properties.json";
		String url = String.format(Locale.getDefault(), "%s%s%d%s", deviceServiceBaseURL(), "properties/", propKey, ".json");
		rs = new AylaRestService(mHandle, url, AylaRestService.GET_PROPERTY_DETAIL);
		saveToLog("%s, %s, %s:%s, %s", "I", "Properties", "url", url, "getPropertyDetail");
		if (delayExecution == false) {
			rs.execute();
		}
		return rs;
	}
	

	// ----------------------- Datapoint Helper methods --------------------------
	protected void updateDatapointFromProperty() {
		this.datapoint.value(this.value);
		String sValue = this.datapoint.sValueFormatted(this.baseType);
		Number nValue = this.datapoint.nValueFormatted(this.baseType);
		this.datapoint.nValue(nValue);
		this.datapoint.sValue(sValue);
		this.datapoint.createdAt(this.dataUpdatedAt);
	}
	
	// ----------------------------- Lan Mode Helper methods
	private static void lanModeEnable(AylaProperty[] properties, String jsonProperties, String dsn) {
		if (lanModeState != lanMode.DISABLED) {

			AylaDevice lanDevice = AylaDeviceManager.sharedManager().deviceWithDSN(dsn);
			// write properties to cache
			if (lanDevice == null) {
				saveToLog("%s, %s, %s:%s, %s", "I", "AylaProperty", "AylaLanMode.device.dsn", "!= savedLanModeDevice.dsn", "lanModeEnable_properties");
			} else {
				// version 3.12, check current lan mode enabled properties
				AylaProperty bufferedProperties[] = lanDevice.properties != null? lanDevice.properties: new AylaProperty[0];
				HashMap<String, AylaProperty> tempPropertyMap = new HashMap<String, AylaProperty>();

				if(properties != null)
					for(AylaProperty property : properties) {
						tempPropertyMap.put(property.name, property);
					}

				AylaProperty knownProperties[] = tempPropertyMap.values().toArray(new AylaProperty[tempPropertyMap.size()]);
				lanDevice.mergeNewProperties(knownProperties);
			}
			//}
		} else {
			saveToLog("%s, %s, %s:%s, %s", "E", "AylaProperty", "AylaLanMode.device", "null", "lanModeEnable_properties");
		}
	}

	protected void lanModeEnable() {
		if (lanModeState != lanMode.DISABLED) {
			AylaDevice device = AylaDeviceManager.sharedManager().deviceWithDSN(product_name);
			if (device != null ) {
				AylaProperty prop = device.findProperty(name);
				if ( prop != null ) {
					prop.value = this.value;
				}
				device.property = this;
			} else {
				saveToLog("%s, %s, %s, %s.", "E", tag, "lanModEnable", "device " + product_name + " cannot be found inside AylaDeviceManager");
			}
		}
	}

	// TODO: put in a common utils class
	public static void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId, Boolean delayExecution) {
		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		if (delayExecution == false) {
			rs.execute();
		}
	}

	// ---------------------- Pass through methods -------------------------

	/**
	 * Same as {@link AylaProperty#createDatapoint(Handler, AylaDatapoint, boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService createDatapoint(AylaDatapoint datapoint) {
		if (datapoint == null) {
			saveToLog("%s, %s, %s.", "E", tag, "Null datapoint for AylaProperty.createDatapoint()");
			return null;
		}
		this.datapoint = datapoint;
		return datapoint.createDatapoint(null, this, true);
	}

	/**
	 * Same as {@link AylaProperty#createDatapoint(Handler, AylaDatapoint, boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService createDatapoint(Handler mHandle, AylaDatapoint datapoint) {
		if (datapoint == null) {
			saveToLog("%s, %s, %s.", "E", tag, "Null datapoint for AylaProperty.createDatapoint()");
			return null;
		}
		this.datapoint = datapoint;
		return datapoint.createDatapoint(mHandle, this, false);
	}

	/**
	 * Upon successful completion this instance method will post the value to the Ayla device service and instantiate a new datapoint object.
	 * @param mHandle is where result would be returned.
	 * @param datapoint is the datapoint to be created.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService createDatapoint(Handler mHandle, AylaDatapoint datapoint, boolean delayExecution) {
		if (datapoint == null) {
			saveToLog("%s, %s, %s.", "E", tag, "Null datapoint for AylaProperty.createDatapoint()");
			return null;
		}
		this.datapoint = datapoint;
		AylaRestService rs = datapoint.createDatapoint(mHandle, this, delayExecution);
		return rs;
	}



	/**
	 * Same as {@link AylaProperty#getDatapointsByActivity(Handler, Map, boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService getDatapointsByActivity(Map<String, String> callParams) {
		if (callParams == null) {
			callParams = new HashMap<String, String>();
		}
		callParams.put(AylaDatapoint.kAylaDataPointOwnerDSN, this.product_name);
		
		if (this.datapoint == null) {
			this.datapoint = new AylaDatapoint();
		}
		return this.datapoint.getDatapointsByActivity(null, this, callParams, true);
	}

	/**
	 * Same as {@link AylaProperty#getDatapointsByActivity(Handler, Map, boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getDatapointsByActivity(final Handler mHandle, Map<String, String> callParams) {
		if (callParams == null) {
			callParams = new HashMap<String, String>();
		}
		callParams.put(AylaDatapoint.kAylaDataPointOwnerDSN, this.product_name);
		
		if (this.datapoint == null) {
			this.datapoint = new AylaDatapoint();
		}
		return this.datapoint.getDatapointsByActivity(mHandle, this, callParams, false);
	}

	/**
	 * This instance method returns datapoints for a given property. getDatapointsByActivity returns datapoints in the order they were created.
	 * @param mHandle is where result would be returned.
	 * @param callParams is applied to qualify the datapoints returned and the maximum number of datapoints returned per query will be limited to maxCount in AylaSystemUtils.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService getDatapointsByActivity(final Handler mHandle, Map<String, String> callParams, boolean delayExecution) {
		if (callParams == null) {
			callParams = new HashMap<String, String>();
		}
		callParams.put(AylaDatapoint.kAylaDataPointOwnerDSN, this.product_name);
		
		if (this.datapoint == null) {
			this.datapoint = new AylaDatapoint();
			saveToLog("%s, %s, %s.", tag, "getDatapointsByActivity", "AylaProperty.datapoint null");
		} else {
			saveToLog("%s, %s\n%s.", tag, "getDatapointsByActivity", "AylaProperty.datapoint:" + this.datapoint.toString());
		}
		AylaRestService rs = this.datapoint.getDatapointsByActivity(mHandle, this, callParams, delayExecution);
		return rs;
	}



	/**
	 * Same as {@link AylaProperty#getBlobs(Handler, Map, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getBlobs(Handler mHandle, Map<String, String> callParams) {
		if (blob == null) {
			blob = new AylaBlob();
		}
		return this.blob.getBlobs(mHandle, this, callParams, false);
	}

	/**
	 * getBlobs is a pass-through to datapoint.getBlobs
	 *   checks baseType and adds blobType:picture to callParams
	 *
	 * @param mHandle - Intent handle callback with results
	 * @param callParams - call parameters
	 * @param delayExecution - execute now or later
	 * @return AylaRestService object
	 */
	public AylaRestService getBlobs(Handler mHandle, Map<String, String> callParams, Boolean delayExecution) {
		if (blob == null) {
			blob = new AylaBlob();
		}
		return this.blob.getBlobs(mHandle, this, callParams, delayExecution);
	}



	/**
	 * Same as {@link AylaProperty#createTrigger(Handler, AylaPropertyTrigger, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService createTrigger(AylaPropertyTrigger propertyTrigger) {
		return propertyTrigger.createTrigger(null, this, true);
	}

	/**
	 * Same as {@link AylaProperty#createTrigger(Handler, AylaPropertyTrigger, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService createTrigger(Handler mHandle, AylaPropertyTrigger propertyTrigger) {
		return propertyTrigger.createTrigger(mHandle, this, false);
	}

	/**
	 * Post/Put a new property trigger associated with this property. See section Device Service : Property Triggers in aAyla Mobile Library document for details.
	 * @param mHandle is where result would be returned.
	 * @param propertyTrigger is the property trigger to be created.
	 * @param delayExecution could be set to true if you want to setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService createTrigger(Handler mHandle, AylaPropertyTrigger propertyTrigger, Boolean delayExecution) {
		AylaRestService rs = propertyTrigger.createTrigger(mHandle, this, delayExecution);
		return rs;
	}
	
	public AylaRestService updateTrigger(AylaPropertyTrigger propertyTrigger) {
		return propertyTrigger.updateTrigger(null, this, true);
	}
	public AylaRestService updateTrigger(Handler mHandle, AylaPropertyTrigger propertyTrigger) {
		return propertyTrigger.updateTrigger(mHandle, this, false);
	}
	public AylaRestService updateTrigger(Handler mHandle, AylaPropertyTrigger propertyTrigger, Boolean delayExecution) {
		AylaRestService rs = propertyTrigger.updateTrigger(mHandle, this, delayExecution);
		return rs;
	}



	/**
	 * Same as {@link AylaProperty#getTriggers(Handler, Map, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService getTriggers(Map<String, String> callParams) {
		return this.propertyTrigger.getTriggers(null, this, callParams, true);
	}

	/**
	 * Same as {@link AylaProperty#getTriggers(Handler, Map, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getTriggers(Handler mHandle, Map<String, String> callParams) {
		return this.propertyTrigger.getTriggers(mHandle, this, callParams, false);
	}

	/**
	 * Get all the property triggers associated with the property.
	 * @param mHandle is where result would be returned.
	 * @param callParams is unused at this time. Set to null
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService getTriggers(Handler mHandle, Map<String, String> callParams, Boolean delayExecution) {
		AylaRestService rs = this.propertyTrigger.getTriggers(mHandle, this, callParams, delayExecution);
		return rs;
	}

	/**
	 * Same as {@link AylaProperty#destroyTrigger(Handler, AylaPropertyTrigger, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService destroyTrigger(AylaPropertyTrigger propertyTrigger) {
		return propertyTrigger.destroyTrigger(null, propertyTrigger, true);
	}

	/**
	 * Same as {@link AylaProperty#destroyTrigger(Handler, AylaPropertyTrigger, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService destroyTrigger(Handler mHandle, AylaPropertyTrigger propertyTrigger) {
		return propertyTrigger.destroyTrigger(mHandle, propertyTrigger, false);
	}

	/**
	 * Call this method to destroy a dedicated property trigger.
	 * @param mHandle is where result would be returned.
	 * @param propertyTrigger is the property trigger to be destroyed.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService destroyTrigger(Handler mHandle, AylaPropertyTrigger propertyTrigger, Boolean delayExecution) {
		AylaRestService rs = propertyTrigger.destroyTrigger(mHandle, propertyTrigger, delayExecution);
		return rs;
	}
}



