//
//  AylaShare.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 4/20/14.
//  Copyright (c) 2014 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import android.os.Handler;
import android.text.TextUtils;

import com.google.gson.annotations.Expose;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

class AylaShareContainer {
	@Expose
	AylaShare share;
}

// For parsing
class Role{
	@Expose
	String name;
}

/**
 * Share a given resource between registered users. 
 * By specifying a resource class and a unique resource identifier, these CRUD APIs support sharing the resource. 
 * When a resource is shared by the owner, the resource for the target user will contain updated grant information. 
 * See Device Service Grants for more information.
 *
 * Currently, only devices may be shared.
 * Only the owner to whom the device has been registered may share a device.
 * A resource may be shared to one or more registered user.
 * Share access controls access rights: read and write are supported.
 * Shares may include a start and end time-stamp.
 * Sharing supports custom email templates for share notification on creation.
 * A user can"t have more than one share for the same resource_name and  resource_id.
**/
public class AylaShare extends AylaSystemUtils {

	@Expose
	public String id;			// The unique share id, Required except for create.
	@Expose
	public Boolean accepted;	// If the share has been accepted by the recipient
	@Expose
	public String acceptedAt;	// When the share has been accepted by the recipient
	@Expose
	public String grantId;		// The unique grant id associated with this share
	@Expose
	public String resourceName;	// Name of the resource class being shared. Ex: 'device', Required for create.
	@Expose
	public String resourceId;	// Unique identifier for the resource name being shared. Ex: 'AC000W0000001234', Required for create.

	@Expose
	public String operation;	// Access permissions allowed: either read or write. Used with create/POST & update/PUT operations. Ex: 'write', Optional
								// If omitted, the default access permitted is read only
	@Expose
	public String startDateAt;	// When this named resource will be shared. Used with create/POST & update/PUT operations. Ex: '2014-03-17 12:00:00', Optional
								// If omitted, the resource will be shared immediately. UTC DateTime value.
	@Expose
	public String endDateAt;	// When this named resource will stop being shared. Used with create/POST & update/PUT operations. Ex: '2020-03-17 12:00:00', Optional
								// If omitted, the resource will be shared until the share or named resource is deleted. UTC DateTime value
	@Expose
	public String ownerId; 			// The owner user id that created this share. Returned with create/POST & update/PUT operations

	@Expose
	public AylaShareOwnerProfile ownerProfile;	// The owner of a shared resource info
	
	@Expose
	public String userId;				// The target user id that created this share. Returned with create/POST & update/PUT operations
	@Expose
	public String userEmail;	// Unique email address of the Ayla registered target user to share the named resource with, Required
	@Expose
	public AylaShareUserProfile userProfile;	// The recipient of a shared resource info

	@Expose
	public String createdAt;	// When this object was created. Returned with create/POST & update/PUT operations
	@Expose
	public String updatedAt;	// When this object was last updated. Returned with update/PUT operations

	@Expose
	public String roleName;     // To cloud param.
	// If this is not null, need to set operation an empty string to make sure the role based
	// sharing works properly.
	
	@Expose
	public Role role;			// From cloud param
	
	@Override
	public String toString() {
		
		return AylaSystemUtils.gson.toJson(this, AylaShare.class);
	}


	/**
	 * Same as {@link AylaShare#create(Handler, Object)} with no handler to return results.
	 * **/

	public AylaRestService create(Object object) {
		return create(null, object);
	}

	/**
	 * create
	 *
	 * Share a given resource between registered users.
	 * By specifying a resource class and a unique resource identifier, these CRUD APIs support sharing the resource.
	 * When a resource is shared by the owner, the resource for the target user will contain updated grant information.
	 * See Device Service Grants for more information.<br/>
	 *
	 * Only AylaDevices of instance type WiFi may be shared.
	 * Only the owner to whom the device has been registered may share a device.
	 * A resource may be shared to one or more registered user.
	 * Share access controls access rights: read and write are supported.
	 * Shares may include a start and end time-stamp.
	 * Sharing supports custom email templates for share notification on creation.
	 * A user can"t have more than one share for the same resource_name and  resource_id.<br/>
	 *
	 * Note that to make sure role based sharing feature works, operation attribute need to be
	 * set an empty string as operation takes higher priority when sent to server at the same
	 * time. <br/>
	 *
	 * Typical usage is to call this method from the owner pass-through methods in AylaDevice or AylaUser<br/>
	 *
	 * @param mHandle is where the results will be returned.
	 *        Successful results will include the newly created share
	 * @param object may be an AylaDevice object instance to retrieve it's shares
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
			// get required params
			if (object instanceof AylaUser) {
				// this instance contains all required share values
			} else
			if (object instanceof AylaShare) {
				// this instance contains all required share values
			} else 
			if (object instanceof AylaDevice) {
				// https://ads-dev.aylanetworks.com/apiv1/dsns/<key>/data.json
				AylaDevice device = (AylaDevice)object;
				if (!device.isWifi()) {
					errors.put("error", "Gateway and node devices may not be shared");
				}
				resourceName = "device";
				resourceId = device.dsn;
			} else {
				errors.put("error", "This class does not support share");
			}
			
			// test validity of required parameters
			if (TextUtils.isEmpty(resourceId)) {
				errors.put("resourceId", "is empty");
			}
			
			if (TextUtils.isEmpty(resourceName)) {
				errors.put("resoruceName", "is empty");
			}
			
			if (TextUtils.isEmpty(userEmail)) {
				errors.put("emailUser", "is empty");
			}

			// return if errors in required fields
			if (errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, requestId);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaShare", ERR_URL, errors.toString(), "create");
				returnToMainActivity(rs, errors.toString(), errCode, 0);
				return rs;
			}
			// all good
			
			//create the url
			if (id != null) {			// it's an update
				// https://ads-dev.aylanetworks.com/apiv1/users/shares/<resourceId>.json
				url = String.format("%s%s%s%s", userServiceBaseURL(), "api/v1/users/shares/", id, ".json");
				requestId = AylaRestService.UPDATE_USER_SHARE;
			} else {					// it's a create
				url = String.format("%s%s", userServiceBaseURL(), "api/v1/users/shares.json");
				requestId = AylaRestService.CREATE_USER_SHARE;
			}
			rs = new AylaRestService(mHandle, url, requestId);
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaShare", "path", url, "create");
			
			// create the Share object
			AylaShareContainer shareContainer = new AylaShareContainer();
			shareContainer.share = this;
			String jsonShareContainer = AylaSystemUtils.gson.toJson(shareContainer, AylaShareContainer.class);
		
			rs.setEntity(jsonShareContainer);
			
			if (mHandle != null) { 
				rs.execute();	// is asynch call
			}
		} catch (Exception e) {
			rs = new AylaRestService(mHandle, ERR_URL, requestId);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaShare", "exception", e.getMessage(), "create");
			returnToMainActivity(rs, e.getMessage(), AML_GENERAL_EXCEPTION, 0);
    	}
    	
		return rs;
	}

	/**
	 * Same as {@link AylaShare#update(Handler)} with no handler to return results.
	 * **/
	public AylaRestService update() {
		return update(null);
	}

	/**
	 *
	 * This instance method is used to update a share on the Ayla Service.
	 * Typical usage is to call this method from the owner pass-through methods in AylaDevice or AylaUser.
	 *
	 * @param mHandle is where the results will be returned.
	 *        Successful results will include the updated share
	 * @return AylaRestService instance
	 */
	public AylaRestService update(Handler mHandle) {
		int errCode = AML_USER_INVALID_PARAMETERS;
		AylaRestService rs = null;
		int requestId = 0;
		JSONObject errors = new JSONObject();
		
		try {
			// test validity of required parameters
			if (TextUtils.isEmpty(id)) {
				errors.put("id", "is empty");
			}

			// return if errors in required fields
			if (errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, requestId);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", ERR_URL, errors.toString(), "update");
				returnToMainActivity(rs, errors.toString(), errCode, 0);
				return rs;
			}
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaShare", "id", id, "update");
			
			// use create to check remainder of params
			return create(mHandle, this);
			
		} catch (Exception e) {
			rs = new AylaRestService(mHandle, ERR_URL, requestId);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaShare", "exception", e.getMessage(), "update");
			returnToMainActivity(rs, e.getMessage(), AML_GENERAL_EXCEPTION, 0);
    	}
    	
		return rs;
	}

	/**
	 * Same as {@link AylaShare#getWithId(Handler, String)} with no handler to return results.
	 * **/
	public static AylaRestService getWithId(String id) {
		return getOwnedOrReceivedWithId(null, id);
	}

	/**
	 *
	 * This instance method is used to retrieve an existing share the Ayla Service based on a given id.
	 * Typical usage is to call this method from the owner pass-through methods.
	 *
	 * @param mHandle is where the results will be returned.
	 *        Successful results will include the matching share
	 * @param id the id whose value will be retrieved
	 * @return AylaRestService instance
	 */
	public static AylaRestService getWithId(Handler mHandle, String id)  {
		return getOwnedOrReceivedWithId(mHandle, id);
	}
	private static AylaRestService getOwnedOrReceivedWithId (Handler mHandle, String id)
	{
		int errCode = AML_USER_INVALID_PARAMETERS;
		String url = null;
		AylaRestService rs = null;
		int requestId = 0;
		JSONObject errors = new JSONObject();
		
		try {
			// test validity of required parameters
			if (TextUtils.isEmpty(id)) {
				errors.put("id", "is empty");
			}

			// return if errors in required fields
			if (errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, requestId);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaShare", ERR_URL, errors.toString(), "getById");
				returnToMainActivity(rs, errors.toString(), errCode, 0);
				return rs;
			}

			// https://user.aylanetworks.com/api/v1/users/shares/1.json
			url = String.format("%s%s%s%s%s", userServiceBaseURL(), "api/v1/", "users/shares/", id, ".json");
			requestId = AylaRestService.GET_USER_SHARE;

			saveToLog("%s, %s, %s:%s, %s", "I", "AylaShare", "path", url, "getById");
						
			rs = new AylaRestService(mHandle, url, requestId); 
	
			if (mHandle != null) {
				rs.execute(); //GET user shareq with key, stripContainer to deserialize
			}
			
		} catch (Exception e) {
			rs = new AylaRestService(mHandle, ERR_URL, requestId);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaShare", "exception", e.getMessage(), "getById");
			returnToMainActivity(rs, e.getMessage(), AML_GENERAL_EXCEPTION, 0);
    	}
		
		return rs;
	}

	// Strip the class container
	/**
	 *  
	 * Internal method used to remove root name from JSON object.
	 *  
	 * @param jsonShareContainer
	 * @param requestId
	 * @return
	 * @throws Exception
	 */
	protected static String stripContainer(String jsonShareContainer, int requestId) throws Exception {
		int count = 0;
		String jsonShare = "";
		
		try {
			// array of shares
			if (requestId == AylaRestService.GET_USER_SHARES || requestId == AylaRestService.GET_USER_RECEIVED_SHARES) {

				// [{"share":{"created_at":"2014-03-27T21:25:35Z","key":"aKey","updated_at":"2014-03-27T21:25:35Z","user_id":17,"value":"aValue2"}}]
				AylaShareContainer[] shareContainers = AylaSystemUtils.gson.fromJson(jsonShareContainer,AylaShareContainer[].class);
				if (shareContainers != null) {
					AylaShare[] shares = new AylaShare[shareContainers.length];
					for (AylaShareContainer shareContainer : shareContainers) {
						shares[count++]= shareContainer.share;   			
					}
					jsonShare = AylaSystemUtils.gson.toJson(shares, AylaShare[].class);
				}

				AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "AylaShare", "count", count, "stripContainer");
			} else {
				// a share
				AylaShareContainer shareContainer = AylaSystemUtils.gson.fromJson(jsonShareContainer, AylaShareContainer.class);
				if (shareContainer != null) {
					jsonShare = AylaSystemUtils.gson.toJson(shareContainer.share, AylaShare.class);
				}
				AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "AylaShare", "share", jsonShare, "stripContainer");
			}
			return jsonShare;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", "AylaShare", "jsonShareContainer", jsonShareContainer, "stripContainer");
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Same as {@link AylaShare#get(Handler, Object, Map)} with no handler to return results.
	 * **/
	
	public static AylaRestService get(Object object, Map<String, String> callParams) {
		return getOwnsOrReceives(null, null, false, callParams);
	}
	public static AylaRestService getReceives(Object object, Map<String, String> callParams) {
		return getOwnsOrReceives(null, null, true, callParams);
	}

	/**
	 *
	 * This instance method is used to retrieve existing share objects from the Ayla Cloud Service
	 * May be called from an owner pass-through method in AylaDevice to auto filter by class/type & DSN.
	 *
	 * @param mHandle is where the results will be returned for async invocation. Omit for sync usage
	 *        Successful results will include the matching share objects
	 * @param callParams one of the following filters:
	 *            null: retrieve all share objects
	 *            a resourceName: the resource class/type to retrieve. Currently only "device" is supported
	 *            a resourceId: the specific resource id to be retrieved. Currently only a device.dsn is supported
	 *              If resourceId is specified, resourceName is required.
	 * @return AylaRestService instance
	 */
	public static AylaRestService get(Handler mHandle, Object object, Map<String, String> callParams) {
		return getOwnsOrReceives(mHandle, object, false, callParams);
	}
	public static AylaRestService getReceives(Handler mHandle, Object object, Map<String, String> callParams) {
		return getOwnsOrReceives(mHandle, object, true, callParams);
	}
	public static AylaRestService getOwnsOrReceives(Handler mHandle, Object object, boolean received, Map<String, String> callParams)
	{
		int errCode = AML_USER_INVALID_PARAMETERS;
		String url = null;
		AylaRestService rs = null;
		int requestId = 0;
		JSONObject errors = new JSONObject();
		String paramValue;
		String rsrcName = null;
		String rsrcId = null;
		
		try {
			if (object instanceof AylaUser) {
				// https://user.aylanetworks.com/api/v1/users/shares.json
				if (received) {
					url = String.format("%s%s%s", userServiceBaseURL(), "api/v1/", "users/shares/received.json");
					requestId = AylaRestService.GET_USER_RECEIVED_SHARES;
				} else {
					url = String.format("%s%s%s", userServiceBaseURL(), "api/v1/", "users/shares.json");
					requestId = AylaRestService.GET_USER_SHARES;
				}
			}
			else if (object instanceof AylaDevice) {
				// Limit shares to this device
				
				if (received) {
					url = String.format("%s%s%s", userServiceBaseURL(), "api/v1/", "users/shares/received.json");
					requestId = AylaRestService.GET_USER_RECEIVED_SHARES;
				} else {
					url = String.format("%s%s%s", userServiceBaseURL(), "api/v1/", "users/shares.json");
					requestId = AylaRestService.GET_USER_SHARES;
				}
				
				// assign defaults
				rsrcName = "device"; 											// default class type
				if ( (object != null) &&  ((AylaDevice) object).dsn != null) {	
					rsrcId = ((AylaDevice) object).dsn;							// use dsn if provided
				}
				if (callParams == null) {
					callParams = new HashMap<String, String>();
					callParams.put("resourceName", rsrcName);	// assign default resourceName
					if (rsrcId != null) {
						callParams.put("resourceId", rsrcId);	// assign default resourceId
					}
				}
			} else {
					errors.put("error", "This class does not support sharing");
			}
			
			// assign the rest service request
			rs = new AylaRestService(mHandle, url, requestId);
			
			// add filter parameters, if any to rest service object
			if (callParams != null) {
				
				paramValue = (String)callParams.get("resourceId");
				if (paramValue != null) {
					rsrcId = paramValue;
					rs.addParam("resource_id", paramValue);
				}
				
				paramValue = (String)callParams.get("resourceName");
				if (rsrcId != null && (paramValue == null && rsrcName == null)) {
					// resourceId must include resourceName
					errors.put("error", "Must include resouce class name type with resource Id");
				} else {
					if (paramValue != null) {
						rs.addParam("resource_name", paramValue);	// specified param takes precedence
					} else
					if (rsrcName != null) {
						rs.addParam("resource_name", rsrcName);		// use resource class name/type
					}
				}
			}
		
			// return if errors in required fields
			if (errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, requestId);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaShare", ERR_URL, errors.toString(), "get");
				returnToMainActivity(rs, errors.toString(), errCode, 0);
				return rs;
			}

			// good request
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaShare", "url", url, "get");
			
			if (mHandle != null) {
				rs.execute(); //GET user shares, stripContainers to deserialize
			}
			
		} catch (Exception e) {
			rs = new AylaRestService(mHandle, ERR_URL, requestId);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaShare", "exception", e.getMessage(), "get");
			returnToMainActivity(rs, e.getMessage(), AML_GENERAL_EXCEPTION, 0);
    	}
		
		return rs;
	}


	/**
	 * Same as {@link AylaShare#delete(Handler)} with no handler to return results.
	 * **/
	public AylaRestService delete() {
		return delete(null);
	}

	/**
	 * This instance method is used to delete an existing share on the Ayla Service.
	 * Typical usage is to call this method from the owner pass-through methods AylaDevice or AylaUser.
	 *
	 * @param mHandle is where the results will be returned.
	 *        Successful results will include a success code with no data
	 * @return AylaRestService instance
	 */
    public AylaRestService delete(Handler mHandle) {
    	int errCode = AML_USER_INVALID_PARAMETERS;
    	int requestId = 0;
    	String url = null;
    	AylaRestService rs = null;
    	JSONObject errors = new JSONObject();
    	
    	try {
			// test validity of required parameters
			if (TextUtils.isEmpty(id)) {
				errors.put("id", "is empty");
			}	

	    	// return if errors in required fields
			if (errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, requestId);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaShare", ERR_URL, errors.toString(), "delete");
				returnToMainActivity(rs, errors.toString(), errCode, 0);
				return rs;
			}
			
			// https://user.aylanetworks.com/api/v1/users/shares/:id
			url = String.format("%s%s%s%s%s", userServiceBaseURL(), "api/v1/", "users/shares/", id, ".json");
			requestId = AylaRestService.DELETE_USER_SHARE;
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaShare", "url", url, "delete");
	    	
	    	// all good
			rs = new AylaRestService(mHandle, url, requestId); 
	
			if (mHandle != null) {
				rs.execute(); //Delete object share based on key match
			}
			
    	} catch (Exception e) {
			rs = new AylaRestService(mHandle, ERR_URL, requestId);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaShare", "exception", e.getMessage(), "delete");
			returnToMainActivity(rs, e.getMessage(), AML_GENERAL_EXCEPTION, 0);
    	}
    	
    	return rs;
	}
    
    /**
     *  return to callback handler.
     *  
     * @param rs: Ayla Rest Service class object
     * @param thisJsonResults
     * @param thisResponseCode
     * @param thisSubTaskId
     */ //TODO: Move to a common utils class. 
	static private void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId) {
		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		
		rs.execute(); // return in main activity
	}
}





