/*
 * AylaContacts.java
 * Ayla Mobile Library
 * 
 * Created by Di Wang on 12/29/2014
 * Copyright (c) 2014 Ayla Networks. All Rights Reserved.
 * */

package com.aylanetworks.aaml;

import java.util.Map;

import org.json.JSONObject;

import android.os.Handler;
import android.text.TextUtils;

import com.google.gson.annotations.Expose;


class AylaContactContainer {
	@Expose
	AylaContact contact;
}


/**
 * The AylaContacts Class is used to provide all contact related 
 * operations, basically CRUD. 
 * */
public class AylaContact extends AylaSystemUtils {

	@Expose
	public Integer id;
	@Expose
	public String displayName;
	@Expose
	public String firstname;
	@Expose
	public String lastname;
	@Expose
	public String email;
	@Expose
	public String phoneCountryCode;
	@Expose
	public String phoneNumber;
	@Expose
	public String streetAddress;
	@Expose
	public String zipCode;
	@Expose
	public String country;
	@Expose
	public String emailAccept;
	@Expose
	public String smsAccept;
	@Expose
	public boolean emailNotification;
	@Expose
	public boolean smsNotification;
	@Expose
	public boolean pushNotification;
	@Expose
	public String metadata;
	@Expose
	public String notes;
	@Expose
	public String updatedAt;
	@Expose
	public String[] oemModels;
	
	public static final int INVALID_ID = Integer.MIN_VALUE;
	public static final String kAylaContactContactId = "contact_id";
	public static final String kAylaContactUserEmail = "user_email";
	
	public static final String kAylaContactDisplayName = "display_name";           
	
	public static final String kAylaContactOEMModel = "oem_model";
	public static final String kAylaContactOEMModels = "oem_models%5B%5D";     // url encoding for []            
	
	public static final String kAylaContactNotes = "notes";

	/**
	 * Same as {@link AylaContact#create(Handler, Map, boolean)} with no option to setup the call to execute later and no handler to return results.
	 * */
	public AylaRestService create(Map<String, String> callParams) {
		return create(null, callParams, true);
	}

	/**
	 * Same as {@link AylaContact#create(Handler, Map, boolean)} with no option to setup the call to execute later.
	 * */
	public AylaRestService create(Handler mHandle, Map<String, String> callParams) {
		return create(mHandle, callParams, false);
	}
	/**
	 * This instance method is used to create contact object on the Ayla Service.
	 *
	 * @param mHandle is where the results will be returned. Successful results will include the newly created contact.
	 * @param callParams is the call paramaters
	 * @param delayExecution to be set to true if you want to setup this call but have it execute later
	 * @return AylaRestService instance
	 * */
	public AylaRestService create(Handler mHandle, Map<String, String> callParams, boolean delayExecution) {
		int errCode = AML_USER_INVALID_PARAMETERS;
		AylaRestService rs = null;
		String url = null;
		int requestId = 0;
		
		try {
			url = String.format("%s%s", 
					userServiceBaseURL(), 
					"api/v1/users/contacts.json"
					);
			requestId = AylaRestService.CREATE_USER_CONTACT;
			
			if (TextUtils.isEmpty(this.displayName)) {
				StringBuilder sb = new StringBuilder();
				if (!TextUtils.isEmpty(firstname)) {
					sb.append(firstname);
				}
				
				sb.append(" ");
				if (!TextUtils.isEmpty(lastname)) {
					sb.append(lastname);
				}
				
				displayName = sb.toString();
			}

			saveToLog("%s, %s, %s:%s, %s", "I", "Contacts", "path", url, "create");
			
			// Create the Contacts object
			AylaContactContainer contactsContainer = new AylaContactContainer();
			contactsContainer.contact = this;
			String jsonContactsContainer = AylaSystemUtils.gson.toJson(contactsContainer, AylaContactContainer.class);        
			//saveToLog("%s, %s, %s:%s, %s", "I", "Contacts", "jsonContactsContainer", jsonContactsContainer, "create");
			
			rs = new AylaRestService(mHandle, url, requestId);
			rs.setEntity(jsonContactsContainer);
			
			if (mHandle != null || !delayExecution) {
				rs.execute();
			}
		} catch (Exception e) {
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, requestId);
			saveToLog("%s, %s, %s:%s, %s", "E", "AylaContacts", "exception", e.getMessage(), "create");
			returnToMainActivity(rs, e.getMessage(), AML_GENERAL_EXCEPTION, 0);
		}
		return rs;
	}// end of create                 



	/**
	 * Same as {@link AylaContact#update(Handler, boolean)} with no option to setup the call to execute later and no handler to return the results.
	 * */
	public AylaRestService update() {
		return update(null,  true);
	}

	/**
	 * Same as {@link AylaContact#update(Handler, boolean)} with no option to setup the call to execute later.
	 * */
	public AylaRestService update(Handler mHandle) {
		return update(mHandle,  false);
	}

	/**
	 * This instance method is used to update contact information on the Ayla Service.
	 * @param mHandle is where the results will be returned. Successful results will include the updated contact.
	 * @param delayExecution to be set to true if you want to setup this call but have it execute later
	 * @return AylaRestService instance.
	 * */
	public AylaRestService update(Handler mHandle,  boolean delayExecution) {
		String url = null;
		AylaRestService rs = null;
		int requestId = 0;
		
		try {
			url = String.format("%s%s%s", userServiceBaseURL(), "api/v1/users/contacts/", id+".json");
			requestId = AylaRestService.UPDATE_USER_CONTACT;     
			
			if (TextUtils.isEmpty(this.displayName)) {
				StringBuilder sb = new StringBuilder();
				if (!TextUtils.isEmpty(firstname)) {
					sb.append(firstname);
				}
				
				sb.append(" ");
				if (!TextUtils.isEmpty(lastname)) {
					sb.append(lastname);
				}
				
				displayName = sb.toString();
			}
			
			saveToLog("%s, %s, %s:%s, %s", "I", "Contacts", "path", url, "update");
			
			// update object
			AylaContactContainer contactContainer = new AylaContactContainer();
			contactContainer.contact = this;
			String jsonContactContainer = AylaSystemUtils.gson.toJson(contactContainer, AylaContactContainer.class);

			rs = new AylaRestService(mHandle, url, requestId);
			rs.setEntity(jsonContactContainer);
			
			if (mHandle !=null || !delayExecution) {
				rs.execute();
			}
		} catch (Exception e) {
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, requestId);
			saveToLog("%s, %s, %s:%s, %s", "E", "AylaContacts", "exception", e.getMessage(), "update");
			returnToMainActivity(rs, e.getMessage(), AML_GENERAL_EXCEPTION, 0);
		}
		return rs;
	}// end of update           




	/**
	 * Same as {@link AylaContact#get(Handler, Map, boolean)} with no option to setup the call to execute later and no handler to return results.
	 * */

	// TODO: 12/1/15 : Need to talk to service team what this is about.
	public static AylaRestService get(Map<String, String> callParams) {
		return get(null, callParams, true);
	}


	/**
	 * Same as {@link AylaContact#get(Handler, Map, boolean)} with no option to setup the call to execute later.
	 * */

	// TODO: 12/1/15 : Need to talk to service team what this is about.
	public static AylaRestService get(Handler mHandle, Map<String, String> callParams) {
		return get(mHandle, callParams, false);
	}

	/**
	 * This class method is used to retrieve contact information.
	 * If user is not null, use GET /api/v1/users/contacts?user_email=<email> and return a list of contacts;
	 * use GET  /api/v1/users/contacts/:id and return a specified contact.
	 *
	 * @param mHandle is where the results will be returned. Successful results will include the matching contact
	 * @param callParams is the call parameters
	 * @param delayExecution to be set to true to set up this call to execute later.
	 * @return AylaRestService instance.
	 * */

	// TODO: 12/1/15 Need to talk to service team what this is about.
	public static AylaRestService get(Handler mHandle, Map<String, String> callParams, boolean delayExecution) {
		String url = null;
		AylaRestService rs = null;
		int requestId = 0;
		
		try {
			
			url = String.format("%s%s", 
					userServiceBaseURL(), 
					"api/v1/users/contacts"
					);
			
			saveToLog("%s, %s, %s:%s, %s", "I", "Contacts", "path", url, "get");
			
			if (callParams != null && callParams.get(AylaContact.kAylaContactContactId)!=null ) {
				// explicitly specify contact_id.
				url += "/" + callParams.get(AylaContact.kAylaContactContactId) + ".json";
				requestId = AylaRestService.GET_USER_CONTACT;
			} else {
				url += ".json";
				requestId = AylaRestService.GET_USER_CONTACT_LIST;
			}
			rs = new AylaRestService(mHandle, url, requestId);
			
			if ( callParams != null ) {
				String dn = callParams.get(AylaContact.kAylaContactDisplayName);
				if ( !TextUtils.isEmpty(dn) ) {
					rs.addParam(AylaContact.kAylaContactDisplayName, dn);
				}
				
				String oemModel = callParams.get(AylaContact.kAylaContactOEMModels);
				if ( !TextUtils.isEmpty(oemModel) ) {
					rs.addParam(AylaContact.kAylaContactOEMModels, oemModel);
				}
			}/////////////
			
			if (mHandle != null || !delayExecution) {
				rs.execute();
			}
		} catch (Exception e) {
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, requestId);
			saveToLog("%s, %s, %s:%s, %s", "E", "AylaContacts", "exception", e.getMessage(), "get");
			returnToMainActivity(rs, e.getMessage(), AML_GENERAL_EXCEPTION, 0);
		}
		return rs;
	}// end of get        


	/**
	 * Same as {@link AylaContact#delete(Handler, boolean)} with no option to setup the call to execute later and no handler to return results.
	 * */
	public AylaRestService delete() {
		return delete(null, true);
	}

	/**
	 * Same as {@link AylaContact#delete(Handler, boolean)} with no option to setup the call to execute later.
	 * */
	public AylaRestService delete(Handler mHandle) {
		return delete(mHandle, false);
	}

	/**
	 * This instance method is used to delete contact on the Ayla service.
	 *
	 * @param mHandle is where the results will be returned. Successful results will include a success code with no data.
	 * @param delayExecution to be set to true to setup this call to be executed later.
	 * @return AylaRestService instance
	 * */
	public AylaRestService delete(Handler mHandle, boolean delayExecution) {
		int errCode = AML_USER_INVALID_PARAMETERS;
		int requestId = 0;
		String url = null;
		AylaRestService rs = null;
		JSONObject errors = new JSONObject();
		
		try {
			requestId = AylaRestService.DELETE_USER_CONTACT;
			url = String.format("%s%s%s", 
					userServiceBaseURL(), 
					"api/v1/users/contacts/", 
					id + ".json"
					);
			
			if (errors.length() != 0) {
				rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, requestId);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaContacts", "errors", errors.toString(), "delete");     
				returnToMainActivity(rs, errors.toString(), errCode, 0);
				return rs;
			}
			
			rs = new AylaRestService(mHandle, url, requestId);
			
			saveToLog("%s, %s, %s:%s, %s", "I", "Contacts", "path", url, "delete");
			if (mHandle != null || !delayExecution) {
				rs.execute();            
			}
		} catch (Exception e) {
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, requestId);
			saveToLog("%s, %s, %s:%s, %s", "E", "AylaContacts", "exception", e.getMessage(), "delete");
			returnToMainActivity(rs, e.getMessage(), AML_GENERAL_EXCEPTION, 0);
		}
		return rs;
	}// end of delete             
	
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		final String NEW_LINE = System.getProperty("line.separator");
		
		result.append(this.getClass().getName()).append("Object {").append(NEW_LINE)
			.append(" id: " + id).append(NEW_LINE)
			.append(" display_name: " + displayName).append(NEW_LINE)
			.append(" first_name: " + firstname).append(NEW_LINE)
			.append(" last_name: " + lastname).append(NEW_LINE)
			.append(" email: " + email).append(NEW_LINE)
			.append(" phone_country_code: " + phoneCountryCode).append(NEW_LINE)
			.append(" phone_number: " + phoneNumber).append(NEW_LINE)
			.append(" street_address: " + streetAddress).append(NEW_LINE)
			.append(" zip_code: " + zipCode).append(NEW_LINE)
			.append(" country: " + country).append(NEW_LINE)
			.append(" emailAccept: " + emailAccept).append(NEW_LINE)
			.append(" smsAccept: " + smsAccept).append(NEW_LINE)
			.append(" emailNotification: " + emailNotification).append(NEW_LINE)
			.append(" smsNotification: " + smsNotification).append(NEW_LINE)
			.append(" pushNotification: " + pushNotification).append(NEW_LINE)
			.append(" metadata: " + metadata).append(NEW_LINE)
			.append(" notes: " + notes).append(NEW_LINE)
			.append(" updated_at: " + updatedAt).append(NEW_LINE)
			.append(" oem_models: " + AylaSystemUtils.arrayToJsonString(oemModels)).append(NEW_LINE)
			.append("}");
		return result.toString();
	}// end of toString    
	
	
	
	
	protected static String stripContainer(String jsonContactsContainer, int requestId) throws Exception {
		String jsonContacts = "";
		//AylaSystemUtils.saveToLog("%s, jsonResponse:%s", "Contacts", jsonContactsContainer);
		try {
			// array of contacts
			if (requestId == AylaRestService.GET_USER_CONTACT_LIST) {
				AylaContactContainer[] contactsContainers = AylaSystemUtils.gson.fromJson(jsonContactsContainer, AylaContactContainer[].class);     
				int count = 0;
				if (contactsContainers != null) {
					AylaContact[] contacts = new AylaContact[contactsContainers.length];
					for (AylaContactContainer contactContainer : contactsContainers) {
						contacts[count++] = contactContainer.contact;
					}
					jsonContacts = AylaSystemUtils.gson.toJson(contacts, AylaContact[].class);
				}
				AylaSystemUtils.saveToLog("%s %s %s:%d %s", "I", "AylaContacts", "count", count, "stripContainer");
			} else {// one single contact
				AylaContactContainer contactContainer = AylaSystemUtils.gson.fromJson(jsonContactsContainer, AylaContactContainer.class);
				if (contactContainer != null) {
					jsonContacts = AylaSystemUtils.gson.toJson(contactContainer.contact, AylaContact.class);
					AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "AylaContact", "Contact", jsonContacts, "stripContainer");
				}
			}
			return jsonContacts;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", "AylaContacts", "jsonContactContainer", jsonContactsContainer, "stripContainer");
			e.printStackTrace();
			throw e;
		}
	}
	
	//TODO: Move to a common utils class        
	static private void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId) {
		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		
		rs.execute();
		return;
	}
}// end of AylaContacts class 





