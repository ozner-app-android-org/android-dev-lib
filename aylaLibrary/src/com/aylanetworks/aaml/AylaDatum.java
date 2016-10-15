//
//  AylaDatum.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 2/14/14.
//  Copyright (c) 2014 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import java.util.ArrayList;
import java.util.Map;

import org.json.JSONObject;

import com.google.gson.annotations.Expose;

import android.os.Handler;
import android.text.TextUtils;

class AylaDatumContainer {
	@Expose
	AylaDatum datum;
}


/**
 * 
 * The AylaDatum Class is used to store metadata on an Ayla Server
 * Metadata is associated wtih/owned by an AylaUser or AylaDevice instance per the UML
 * Each AylaDatum instance is composed of a simple key : value pair
 * Metadata associated with/owned by an AylaDevice instance will be deleted when the device is unregistered
 * Metadata associated with/owned by an AylaUser instance will be deleted when the user account is deleted
 * 
 */
public class AylaDatum extends AylaSystemUtils {

	@Expose
	public String key;			// Required field of key:value pair. Maximum length is 255 characters
	@Expose
	public String value;		// Required for create/POST, update/PUT operations, returned for GET operations. Maximum length is 2 MB.
	@Expose
	public String createdAt;	// When this object was created. Returned with create/POST & update/PUT operations
	@Expose
	public String updatedAt;	// When this object was last updated. Returned with update/PUT operations

	
	/**
	 * Same as {@link AylaDatum#create(Handler, Object)} with no handler to return results
	 */
	public AylaRestService create(Object object) {
		return create(null, object);
	}

	/**
	 * This instance method is used to create metadata key:value pair on the Ayla Service.
	 * Typical usage is to call this method from the owner pass-through methods in AylaUser or AylaDevice
	 * @param mHandle is where the results will be returned.
	 *        Successful results will include the newly created datum
	 * @param object must be an AylaUser or AylaDevice object instance
	 * @return AylaRestService instance
	 */
	public AylaRestService create(Handler mHandle, Object object)
	{
		int errCode = AML_USER_INVALID_PARAMETERS;
		AylaRestService rs = null;
		String url = null;
		int requestId = 0;
		JSONObject errors = new JSONObject();

		try {
			if (object instanceof AylaUser) {
				// https://user.aylanetworks.com/api/v1/users/1/data.json 
				url = String.format("%s%s", userServiceBaseURL(), "api/v1/users/data.json");
				requestId = AylaRestService.CREATE_USER_METADATA;
			} else
			if (object instanceof AylaDevice) {
				// https://ads-dev.aylanetworks.com/apiv1/dsns/<key>/data.json
				AylaDevice device = (AylaDevice)object;
				url = String.format("%s%s%s%s", deviceServiceBaseURL(), "dsns/", device.dsn, "/data.json");
				requestId = AylaRestService.CREATE_DEVICE_METADATA;
			} else {
				errors.put("error", "This class does not support metadata");
			}
			
			// test validity of required parameters
			if ( TextUtils.isEmpty(this.key) ) {
				errors.put("key", "is empty");
			}
			if ( TextUtils.isEmpty(this.value) ) {
				errors.put("value", "is empty");
			}

			// return if errors in required fields
			if (errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, requestId);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaDatum", ERR_URL, errors.toString(), "create");
				returnToMainActivity(rs, errors.toString(), errCode, 0, false);
				return rs;
			}
			saveToLog("%s, %s, %s:%s, %s", "I", "Datum", "path", url, "create");
			
			// create the Datum object
			AylaDatumContainer datumContainer = new AylaDatumContainer();
			datumContainer.datum = this;
			String jsonDatumContainer = AylaSystemUtils.gson.toJson(datumContainer, AylaDatumContainer.class);

			rs = new AylaRestService(mHandle, url, requestId);
			rs.setEntity(jsonDatumContainer);
			
			if (mHandle != null) { 
				rs.execute();	// is asynch call
			}
		} catch (Exception e) {
			rs = new AylaRestService(mHandle, ERR_URL, requestId);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaDatum", "exception", e.getMessage(), "create");
			returnToMainActivity(rs, e.getMessage(), AML_GENERAL_EXCEPTION, 0, false);
    	}
		return rs;
	}
	
	
	/**
	 * Same as {@link AylaDatum#update(Handler, Object)} with no handler to return results
	 */
	public AylaRestService update(Object object) {
		return update(null, object);
	}

	/**
	 * This instance method is used to update metadata key:value pair on the Ayla Service.
	 * Typical usage is to call this method from the owner pass-through methods.
	 *
	 * @param mHandle is where the results will be returned.
	 *        Successful results will include the updated datum
	 * @param object must be an AylaUser or AylaDevice object instance
	 * @return AylaRestService instance
	 */
	public AylaRestService update(Handler mHandle, Object object) {
		int errCode = AML_USER_INVALID_PARAMETERS;
		String url = null;
		AylaRestService rs = null;
		int requestId = 0;
		JSONObject errors = new JSONObject();
		
		try {
			if (object instanceof AylaUser) {
				// https://user.aylanetworks.com/api/v1/users/1/data.json 
				url = String.format("%s%s%s%s", userServiceBaseURL(), "api/v1/users/data/", this.key, ".json");
				requestId = AylaRestService.UPDATE_USER_METADATA;
			} else
			if (object instanceof AylaDevice) {
				// https://ads-dev.aylanetworks.com/apiv1/dsns/<dsn>/data/<key>.json
				url = String.format("%s%s%s%s%s%s", deviceServiceBaseURL(), "dsns/", ((AylaDevice)object).dsn, "/data/", this.key, ".json");
				requestId = AylaRestService.UPDATE_DEVICE_METADATA;
			} else {
				errors.put("error", "This class does not support metadata");
			}
			
			// test validity of required parameters
			if ( TextUtils.isEmpty(this.key) ) {
				errors.put("key", "is empty");
			}
			if ( TextUtils.isEmpty(this.value) ) {
				errors.put("value", "is empty");
			}

			// return if errors in required fields
			if (errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, requestId);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaDatum", ERR_URL, errors.toString(), "update");
				returnToMainActivity(rs, errors.toString(), errCode, 0, false);
				return rs;
			}
			saveToLog("%s, %s, %s:%s, %s", "I", "Datum", "path", url, "update");
			
			// update object
			AylaDatumContainer datumContainer = new AylaDatumContainer();
			datumContainer.datum = this;
			String jsonDatumContainer = AylaSystemUtils.gson.toJson(datumContainer, AylaDatumContainer.class);
			
			rs = new AylaRestService(mHandle, url, requestId);
			rs.setEntity(jsonDatumContainer);
			
			if (mHandle != null) { 
				rs.execute();	// is asynch call
			}
		} catch (Exception e) {
			rs = new AylaRestService(mHandle, ERR_URL, requestId);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaDatum", "exception", e.getMessage(), "update");
			returnToMainActivity(rs, e.getMessage(), AML_GENERAL_EXCEPTION, 0, false);
    	}
		return rs;
	}
	
	/**
	 * Same as {@link AylaDatum#getWithKey(Handler, Object, String)} with no handler to return results.
	 */
	public static AylaRestService getWithKey(Object object, String key) {
		return getWithKey(null, object, key);
	}

	/**
	 * This instance method is used to retrieve metadata key:value pair the Ayla Service based on a given key.
	 * Typical usage is to call this method from the owner pass-through methods.
	 *
	 * @param mHandle is where the results will be returned.
	 *        Successful results will include the matching datum
	 * @param key whose value will be retrieved
	 * @param object must be an AylaUser or AylaDevice object instance
	 * @return AylaRestService instance
	 */
	public static AylaRestService getWithKey(Handler mHandle, Object object, String key)
	{
		int errCode = AML_USER_INVALID_PARAMETERS;
		String url = null;
		AylaRestService rs = null;
		int requestId = 0;
		JSONObject errors = new JSONObject();
		
		try {
			String aKey = (key == null) ? "" : key;
			
			if (object instanceof AylaUser) {
				// https://user.aylanetworks.com/api/v1/users/data/aKey.json
				url = String.format("%s%s%s%s", userServiceBaseURL(), "api/v1/users/data/", aKey, ".json");
				requestId = AylaRestService.GET_USER_METADATA_BY_KEY;
			} else
			if (object instanceof AylaDevice) {
				// https://ads.aylanetworks.com/apiv1/dsns/<dsn>/data/<key>.json
				url = String.format("%s%s%s%s%s%s", deviceServiceBaseURL(), "dsns/", ((AylaDevice)object).dsn, "/data/", aKey, ".json");
				requestId = AylaRestService.GET_DEVICE_METADATA_BY_KEY;
			} else {
				errors.put("error", "This class does not support metadata");
			}
			
			// return if errors in required fields
			if (errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, requestId);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaDatum", ERR_URL, errors.toString(), "getByKey");
				returnToMainActivity(rs, errors.toString(), errCode, 0, false);
				return rs;
			}
			saveToLog("%s, %s, %s:%s, %s", "I", "Datum", "path", url, "getByKey");
						
			rs = new AylaRestService(mHandle, url, requestId); 
	
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaDatum", "url", url, "getByKey");
			if (mHandle != null) {
				rs.execute(); //GET user metadataq with key, stripContainer to deserialize
			}
			
		} catch (Exception e) {
			rs = new AylaRestService(mHandle, ERR_URL, requestId);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaDatum", "exception", e.getMessage(), "getByKey");
			returnToMainActivity(rs, e.getMessage(), AML_GENERAL_EXCEPTION, 0, false);
    	}
		
		return rs;
	}
	
	// Strip the class container
	protected static String stripContainer(String jsonDatumContainer, int requestId) throws Exception {
		int count = 0;
		String jsonDatum = "";
		
		try {
			// array of datums
			if (requestId == AylaRestService.GET_USER_METADATA || requestId == AylaRestService.GET_DEVICE_METADATA) {
				// [{"datum":{"created_at":"2014-03-27T21:25:35Z","key":"aKey","updated_at":"2014-03-27T21:25:35Z","user_id":17,"value":"aValue2"}}]
				AylaDatumContainer[] datumContainers = AylaSystemUtils.gson.fromJson(jsonDatumContainer,AylaDatumContainer[].class);
				if (datumContainers != null) {
					AylaDatum[] datums = new AylaDatum[datumContainers.length];
					for (AylaDatumContainer datumContainer : datumContainers) {
						datums[count++]= datumContainer.datum;   			
					}
					jsonDatum = AylaSystemUtils.gson.toJson(datums,AylaDatum[].class);
				}
				AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "AylaDatum", "count", count, "stripContainer");
			} else {
				// a datum
				AylaDatumContainer datumContainer = AylaSystemUtils.gson.fromJson(jsonDatumContainer, AylaDatumContainer.class);
				if (datumContainer != null) {
					jsonDatum = AylaSystemUtils.gson.toJson(datumContainer.datum, AylaDatum.class);
					AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "AylaDatum", "datum", jsonDatum, "stripContainer");
				}
			}
			return jsonDatum;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", "AylaDatum", "jsonDatumContainer", jsonDatumContainer, "stripContainer");
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Same as {@link AylaDatum#get(Handler, Object, Map)} with no handler to return results.
	 */
	public static AylaRestService get(Object owner, Map<String, ArrayList<String>> callParams) {
		return get(null, owner, callParams);
	}

	/**
	 * This instance method is used to retrieve metadata object key:value pairs from the Ayla Cloud Service
	 * Typical usage is to call this method from the owner pass-through method.
	 *
	 * @param mHandle is where the results will be returned for async invocation. Omit for sync usage.
	 *        Successful results will include the matching datum objects
	 * @param callParams one of the following filters:
	 *            null: retrieve all datum objects
	 *            a list of one or more key names to retrieve
	 *            a list of patterns where the "%" sign defines wild cards before or after the pattern
	 * @param owner The parent object must be an AylaUser or AylaDevice object instance
	 * @return AylaRestService instance
	 */
	public static AylaRestService get(Handler mHandle, Object owner, Map<String, ArrayList<String>> callParams)
	{
		int errCode = AML_USER_INVALID_PARAMETERS;
		String url = null;
		AylaRestService rs = null;
		int requestId = 0;
		JSONObject errors = new JSONObject();
		
		try {
			if (owner instanceof AylaUser) {
				// https://user.aylanetworks.com/api/v1/users/data.json 
				url = String.format("%s%s", userServiceBaseURL(), "api/v1/users/data.json");
				requestId = AylaRestService.GET_USER_METADATA;
			} else
			if (owner instanceof AylaDevice) {
				// https://ads.aylanetworks.com/apiv1/dsns/<dsn>/data.json
				url = String.format("%s%s%s%s", deviceServiceBaseURL(), "dsns/", ((AylaDevice)owner).dsn, "/data.json");
				requestId = AylaRestService.GET_DEVICE_METADATA;
			} else {
				errors.put("error", "This class does not support metadata");
			}
			
			// return if errors in required fields
			if (errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, requestId);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaDatum", ERR_URL, errors.toString(), "get");
				returnToMainActivity(rs, errors.toString(), errCode, 0, false);
				return rs;
			}
			saveToLog("%s, %s, %s:%s, %s", "I", "Datum", "path", url, "get");
						
			// good request
			rs = new AylaRestService(mHandle, url, requestId);
			
			// filter parameters
			// https://user.aylanetworks.com/api/v1/users/data.json?keys[]=contact1&keys[]=contact2
			// https://user.aylanetworks.com/api/v1/users/data.json?keys=contact% 
			if (callParams != null) {
				ArrayList<String> filters = (ArrayList<String>)callParams.get("filters");
				if (filters.size() == 1) {
					// get one key and/or match a filter
					rs.addParam("keys", filters.get(0));				
				} else {
					// match multiple key values, return 404 if any one is not found
					String urlArray = AylaRestService.createUrlArray("keys", filters);	
					rs.url += urlArray;
				}
			}
			
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaDatum", "url", url, "get");
			
			if (mHandle != null) {
				rs.execute(); //GET user metadataq with key, stripContainer to deserialize
			}
		} catch (Exception e) {
			rs = new AylaRestService(mHandle, ERR_URL, requestId);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaDatum", "exception", e.getMessage(), "get");
			returnToMainActivity(rs, e.getMessage(), AML_GENERAL_EXCEPTION, 0, false);
    	}
		return rs;
	}
	
 
	/**
	 * Same as {@link AylaDatum#delete(Handler, Object)} with no handler to return results.
	 */
	public AylaRestService delete(Object object) {
		return delete(null, object);
	}

	/**
	 * This instance method is used to delete metadata key:value pair on the Ayla Service.
	 * Typical usage is to call this method from the owner pass-through methods
	 * @param mHandle is where the results will be returned.
	 *        Successful results will include a success code with no data
	 * @param object must be an AylaUser or AylaDevice object instance
	 * @return AylaRestService instance
	 */
    public AylaRestService delete(Handler mHandle, Object object) {
    	int errCode = AML_USER_INVALID_PARAMETERS;
    	int requestId = 0;
    	String url = null;
    	AylaRestService rs = null;
    	JSONObject errors = new JSONObject();
    	
    	try { 
	    	if (object instanceof AylaUser) {
	    		// https://user.aylanetworks.com/api/v1/users/data/aKey.json
	    		url = String.format("%s%s%s%s", userServiceBaseURL(), "api/v1/users/data/", this.key, ".json");
				requestId = AylaRestService.DELETE_USER_METADATA;
			} else
			if (object instanceof AylaDevice) {
				// https://ads-dev.aylanetworks.com/apiv1/dsns/<key>/data.json
				url = String.format("%s%s%s%s%s%s", deviceServiceBaseURL(), "dsns/", ((AylaDevice)object).dsn, "/data/", this.key, ".json");
				requestId = AylaRestService.DELETE_DEVICE_METADATA;
			} else {
					errors.put("error", "This class does not support metadata");
			}
	    	
	    	// return if errors in required fields
			if (errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, requestId);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaDatum", ERR_URL, errors.toString(), "delete");
				returnToMainActivity(rs, errors.toString(), errCode, 0, false);
				return rs;
			}
			saveToLog("%s, %s, %s:%s, %s", "I", "Datum", "path", url, "delete");
	    	
	    	// all good
			rs = new AylaRestService(mHandle, url, requestId); 
	
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaDatum", "url", url, "delete");
			if (mHandle != null) {
				rs.execute(); //Delete object datum based on key match
			}
    	} catch (Exception e) {
			rs = new AylaRestService(mHandle, ERR_URL, requestId);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaDatum", "exception", e.getMessage(), "delete");
			returnToMainActivity(rs, e.getMessage(), AML_GENERAL_EXCEPTION, 0, false);
    	}
    	return rs;
	}
    
    
    //TODO: Move to a common Utils class.    
	static private void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId, Boolean fromDevice) {
		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		
		rs.execute(); // return in main activity
		return;
	}
}



