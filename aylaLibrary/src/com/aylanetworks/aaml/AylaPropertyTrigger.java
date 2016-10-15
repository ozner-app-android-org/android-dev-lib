//
//  AylaPropertyTrigger.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 8/25/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import java.util.Locale;
import java.util.Map;

import com.google.gson.annotations.Expose;

import android.os.Handler;
import android.text.TextUtils;

//----------------------------------------------- Property Triggers -------------------------------
class AylaPropertyTriggerContainer {
	@Expose
	public AylaPropertyTrigger trigger = null;
}

public class AylaPropertyTrigger extends AylaSystemUtils 
{
	//Properties for Trigger1
	@Expose
	public String triggerType;
	@Expose
	public String compareType;
	@Expose
	public String value;
	@Expose
	public String propertyNickname;
	@Expose
	public String deviceNickname;
	@Expose
	public String retrievedAt;
	@Expose
	public Boolean active;
	
	@Expose
	Number key;
	

	//Additional Properties for Trigger2
	@Expose
	public String period;
	@Expose
	public String baseType;
	@Expose
	public String triggeredAt;
	// private Number propertyKey;

	@Expose
	public AylaApplicationTrigger applicationTrigger = null;
	@Expose
	public AylaApplicationTrigger[] applicationTriggers = null;

	public AylaPropertyTrigger() {
		applicationTrigger = new AylaApplicationTrigger(); // initialize for pass through calls
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");

		result.append(this.getClass().getName() + " Object {" + NEW_LINE);
		result.append(" active:" + active + NEW_LINE);
		result.append(" triggerType: " + triggerType + NEW_LINE);
		result.append(" compareType: " + compareType + NEW_LINE);
		result.append(" value: " + value + NEW_LINE );
		result.append(" period: " + period + NEW_LINE );
		result.append(" baseType: " + baseType + NEW_LINE );
		result.append(" triggeredAt: " + triggeredAt + NEW_LINE );
		// result.append(" retrievedAt: " + retrievedAt + NEW_LINE);
		result.append("}");
		return result.toString();
	}

	/**
	 * Same as {@link AylaPropertyTrigger#createTrigger(Handler, AylaProperty, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService createTrigger(AylaProperty property) {
		return createTrigger(null,  property, true);
	}

	/**
	 * Same as {@link AylaPropertyTrigger#createTrigger(Handler, AylaProperty, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService createTrigger(Handler mHandle, AylaProperty property) {
		return createTrigger(mHandle,  property, false);
	}

	/**
	 * Post a new property trigger associated with input param property. See section Device Service - Property Triggers in aAyla Mobile Library document for details.
	 * @param mHandle is where result would be returned.
	 * @param property is the property associated with new created trigger.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService createTrigger(Handler mHandle, AylaProperty property, Boolean delayExecution) {
		Number propertyKey = property.getKey().intValue(); // Handle gson LazilyParsedNumber
		String url = String.format(Locale.getDefault(),"%s%s%d%s", deviceServiceBaseURL(), "properties/", propertyKey, "/triggers.json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.CREATE_PROPERTY_TRIGGER);

		String propertyTriggerContainerJson = "";
		try {
			AylaPropertyTriggerContainer propTriggerContainer = new AylaPropertyTriggerContainer();
			propTriggerContainer.trigger = this;
			propertyTriggerContainerJson = AylaSystemUtils.gson.toJson(propTriggerContainer, AylaPropertyTriggerContainer.class);
		} catch (Exception ex) {
			AylaSystemUtils.saveToLog("%s, %s, %s:%d", "E", "PropertyTrigger", "Ayla error", AylaNetworks.AML_USER_INVALID_PARAMETERS);
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.CREATE_PROPERTY_TRIGGER);
			returnToMainActivity(rs, "Invalid Parameters.", AylaNetworks.AML_USER_INVALID_PARAMETERS, AylaRestService.CREATE_PROPERTY_TRIGGER);
			return rs;
		}
		
		rs.setEntity(propertyTriggerContainerJson);

		saveToLog("%s, %s, %s:%s, %s", "I", "PropertyTrigger", "path", url, "createPropertyTrigger");
		if (delayExecution == false) {
			rs.execute();
		}
		return rs;
	}

	/**
	 * Same as {@link AylaPropertyTrigger#updateTrigger(Handler, AylaProperty, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService updateTrigger(AylaProperty property) {
		return updateTrigger(null,  property, true);
	}

	/**
	 * Same as {@link AylaPropertyTrigger#updateTrigger(Handler, AylaProperty, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService updateTrigger(Handler mHandle, AylaProperty property) {
		return updateTrigger(mHandle,  property, false);
	}

	/**
	 * Put a property trigger associated with input param property. See section Device Service - Property Triggers in aAyla Mobile Library document for details.
	 * @param mHandle is where result would be returned.
	 * @param property is the property associated with new created trigger.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService updateTrigger(Handler mHandle, AylaProperty property, Boolean delayExecution) {
		Number propertyTriggerKey = this.key.intValue(); // Handle gson LazilyParsedNumber
		String url = String.format(Locale.getDefault(), "%s%s%d%s", deviceServiceBaseURL(), "triggers/", propertyTriggerKey, ".json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.UPDATE_PROPERTY_TRIGGER);

		String propertyTriggerContainerJson = "";
		try {
			AylaPropertyTriggerContainer propTriggerContainer = new AylaPropertyTriggerContainer();
			propTriggerContainer.trigger = this;
			propertyTriggerContainerJson = AylaSystemUtils.gson.toJson(propTriggerContainer, AylaPropertyTriggerContainer.class);
		} catch (Exception ex) {
			AylaSystemUtils.saveToLog("%s, %s, %s:%d", "E", "PropertyTrigger", "Ayla error", AylaNetworks.AML_USER_INVALID_PARAMETERS);
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.UPDATE_PROPERTY_TRIGGER);
			returnToMainActivity(rs, "Invalid Parameters.", AylaNetworks.AML_USER_INVALID_PARAMETERS, AylaRestService.UPDATE_PROPERTY_TRIGGER);
			return rs;
		}
		rs.setEntity(propertyTriggerContainerJson);

		saveToLog("%s, %s, %s:%s, %s", "I", "PropertyTrigger", "path", url, "updatePropertyTrigger");
		if (delayExecution == false) {
			rs.execute();
		}
		return rs;
	}
	
	protected static String stripContainer(String jsonPropertyTriggerContainer, int method) throws Exception {
		String jsonPropertyTrigger = "";
		String requestIdStr = (method == AylaRestService.CREATE_PROPERTY_TRIGGER) ? "create" : "update";
		try {
			AylaPropertyTriggerContainer propertyTriggerContainer = AylaSystemUtils.gson.fromJson(jsonPropertyTriggerContainer,AylaPropertyTriggerContainer.class);
			AylaPropertyTrigger propertyTrigger = propertyTriggerContainer.trigger;
			jsonPropertyTrigger = AylaSystemUtils.gson.toJson(propertyTrigger,AylaPropertyTrigger.class);
			AylaSystemUtils.saveToLog("%s %s %s:%s %s.%s", "I", "PropertyTrigger", "propertyTrigger",
					                                       propertyTrigger.toString(), requestIdStr, "stripContainer");
			return jsonPropertyTrigger;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s.%s", "E", "PropertyTrigger", "jsonPropertyTriggerContainer",
					                                    jsonPropertyTriggerContainer, requestIdStr, "stripContainer");
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Same as {@link AylaPropertyTrigger#getTriggers(Handler, AylaProperty, Map, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService getTriggers(AylaProperty property, Map<String, String> callParams) {
		return getTriggers(null, property, callParams, true);
	}

	/**
	 * Same as {@link AylaPropertyTrigger#getTriggers(Handler, AylaProperty, Map, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getTriggers(Handler mHandle, AylaProperty property, Map<String, String> callParams) {
		return getTriggers(mHandle, property, callParams, false);
	}

	/**
	 * Get all the property triggers associated with the property.
	 * @param mHandle is where result would be returned.
	 * @param property is the property which retrieved property triggers bind to.
	 * @param callParams is not required (TBD);
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService getTriggers(Handler mHandle, AylaProperty property, Map<String, String> callParams, Boolean delayExecution) {
		Number propKey = property.getKey().intValue(); // Handle gson LazilyParsedNumber

		//properties/122/triggers.json
		String url = String.format(Locale.getDefault(), "%s%s%d%s", deviceServiceBaseURL(), "properties/", propKey, "/triggers.json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.GET_PROPERTY_TRIGGERS); 

		saveToLog("%s, %s, %s:%s, %s", "I", "PropertyTrigger", "url", url, "getTriggers");
		if (delayExecution == false) {
			rs.execute(); //Executes the request with the HTTP GET verb
		}
		return rs;
	}
	
	protected static String stripContainers(String jsonPropertyTriggerContainers) throws Exception {
		int count = 0;
		String jsonPropertyTriggers = "";
		try {
			AylaPropertyTriggerContainer[] propertyTriggerContainers = AylaSystemUtils.gson.fromJson(jsonPropertyTriggerContainers,AylaPropertyTriggerContainer[].class);
			AylaPropertyTrigger[] propertyTriggers = new AylaPropertyTrigger[propertyTriggerContainers.length];
			for (AylaPropertyTriggerContainer propertyTriggerContainer : propertyTriggerContainers) {
				propertyTriggers[count++]= propertyTriggerContainer.trigger;   			
			}
			jsonPropertyTriggers = AylaSystemUtils.gson.toJson(propertyTriggers,AylaPropertyTrigger[].class);
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "PropertyTrigger", "count", count, "stripContainers");
			return jsonPropertyTriggers;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s:%s %s", "E", "PropertyTrigger", "count", count, "jsonPropertyContainers", jsonPropertyTriggerContainers, "stripContainers");
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Same as {@link AylaPropertyTrigger#destroyTrigger(Handler, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService destroyTrigger() {
		return destroyTrigger(null, this, true);
	}

	/**
	 * Same as {@link AylaPropertyTrigger#destroyTrigger(Handler, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService destroyTrigger(Handler mHandle) {
		return destroyTrigger(mHandle, this, false);
	}

	/**
	 * Destroy a dedicated property trigger.
	 * @param mHandle is where result would be returned.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService destroyTrigger(Handler mHandle, Boolean delayExecution) {
		return destroyTrigger(mHandle, this, delayExecution);
	}

	/**
	 * Same as {@link AylaPropertyTrigger#destroyTrigger(Handler, AylaPropertyTrigger, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService destroyTrigger(AylaPropertyTrigger propertyTrigger) {
		return destroyTrigger(null, propertyTrigger, true);
	}

	/**
	 * Same as {@link AylaPropertyTrigger#destroyTrigger(Handler, AylaPropertyTrigger, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService destroyTrigger(Handler mHandle, AylaPropertyTrigger propertyTrigger) {
		return destroyTrigger(mHandle, propertyTrigger, false);
	}

	/**
	 * Destroy a dedicated property trigger.
	 * @param mHandle is where result would be returned.
	 * @param propertyTrigger is the property trigger to be destroyed.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService destroyTrigger(Handler mHandle, AylaPropertyTrigger propertyTrigger, Boolean delayExecution) {
		Number propertyTriggerKey = propertyTrigger.key.intValue(); // Handle gson LazilyParsedNumber
		String url = String.format(Locale.getDefault(), "%s%s%d%s", deviceServiceBaseURL(), "triggers/", propertyTriggerKey, ".json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.DESTROY_PROPERTY_TRIGGER);

		saveToLog("%s, %s, %s:%s, %s", "I", "PropertyTrigger", "path", url, "destroyPropertyTrigger");
		if (delayExecution == false) {
			rs.execute();
		}
		return rs;
	}

	// ----------------------- Pass through methods --------------------------
	/**
	 * Same as {@link AylaPropertyTrigger#createPushApplicationTrigger(Handler, AylaApplicationTrigger, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService createPushApplicationTrigger(AylaApplicationTrigger applicationTrigger) {
		return createPushApplicationTrigger(null,applicationTrigger, true);
	}

	/**
	 * Same as {@link AylaPropertyTrigger#createPushApplicationTrigger(Handler, AylaApplicationTrigger, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService createPushApplicationTrigger(Handler mHandle, AylaApplicationTrigger applicationTrigger) {
		return createPushApplicationTrigger(mHandle, applicationTrigger, false);
	}

	/**
	 * Used to post/put a new push notification message application trigger to the Ayla Cloud Service.
	 * See section Application Triggers for details on the AylaApplicationTrigger class in aAyla Mobile Library document.
	 * @param mHandle is where result would be returned.
	 * @param applicationTrigger is the trigger to be created.
	 * @param delayExecution could be set to true if you want to setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService createPushApplicationTrigger(Handler mHandle, AylaApplicationTrigger applicationTrigger, Boolean delayExecution) {
		
		if (TextUtils.isEmpty(applicationTrigger.name)) {
			applicationTrigger.name = AylaAppNotification.aylaAppNotificationTypePush;
		}
		
		AylaRestService rs = applicationTrigger.createTrigger(mHandle, this, applicationTrigger, delayExecution);
		return rs;
	}
	
	public AylaRestService updatePushApplicationTrigger(AylaApplicationTrigger applicationTrigger) {
		return updatePushApplicationTrigger(null,applicationTrigger, true);
	}
	public AylaRestService updatePushApplicationTrigger(Handler mHandle, AylaApplicationTrigger applicationTrigger) {
		return updatePushApplicationTrigger(mHandle, applicationTrigger, false);
	}
	public AylaRestService updatePushApplicationTrigger(Handler mHandle, AylaApplicationTrigger applicationTrigger, Boolean delayExecution) {
		
		if (TextUtils.isEmpty(applicationTrigger.name)) {
			applicationTrigger.name = AylaAppNotification.aylaAppNotificationTypePush;
		}
		
		AylaRestService rs = applicationTrigger.updateTrigger(mHandle, this, delayExecution);
		return rs;
	}

	/**
	 * Same as {@link AylaPropertyTrigger#createSMSApplicationTrigger(Handler, AylaApplicationTrigger, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService createSMSApplicationTrigger(AylaApplicationTrigger applicationTrigger) {
		return createSMSApplicationTrigger(null,applicationTrigger, true);
	}

	/**
	 * Same as {@link AylaPropertyTrigger#createSMSApplicationTrigger(Handler, AylaApplicationTrigger, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService createSMSApplicationTrigger(Handler mHandle, AylaApplicationTrigger applicationTrigger) {
		return createSMSApplicationTrigger(mHandle, applicationTrigger, false);
	}

	/**
	 * Used to post/put a new text message application trigger to the Ayla Cloud Service. See section Application Triggers for details on
	 the AylaApplicationTrigger class in aAyla Mobile Library document.
	 * @param mHandle is where result would be returned.
	 * @param applicationTrigger is the trigger to be created.
	 * @param delayExecution could be set to true if you want to setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService createSMSApplicationTrigger(Handler mHandle, AylaApplicationTrigger applicationTrigger, Boolean delayExecution) {
		applicationTrigger.name = AylaAppNotification.aylaAppNotificationTypeSms;
		AylaRestService rs = applicationTrigger.createTrigger(mHandle, this, applicationTrigger, delayExecution);
		return rs;
	}
	public AylaRestService updateSMSApplicationTrigger(AylaApplicationTrigger applicationTrigger) {
		return updateSMSApplicationTrigger(null,applicationTrigger, true);
	}
	public AylaRestService updateSMSApplicationTrigger(Handler mHandle, AylaApplicationTrigger applicationTrigger) {
		return updateSMSApplicationTrigger(mHandle, applicationTrigger, false);
	}
	public AylaRestService updateSMSApplicationTrigger(Handler mHandle, AylaApplicationTrigger applicationTrigger, Boolean delayExecution) {
		applicationTrigger.name = AylaAppNotification.aylaAppNotificationTypeSms;
		AylaRestService rs = applicationTrigger.updateTrigger(mHandle, this, delayExecution);
		return rs;
	}

	/**
	 * Same as {@link AylaPropertyTrigger#createEmailApplicationTrigger(Handler, AylaApplicationTrigger, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService createEmailApplicationTrigger(AylaApplicationTrigger applicationTrigger) {
		return createEmailApplicationTrigger(null, applicationTrigger, true);
	}

	/**
	 * Same as {@link AylaPropertyTrigger#createEmailApplicationTrigger(Handler, AylaApplicationTrigger, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService createEmailApplicationTrigger(Handler mHandle, AylaApplicationTrigger applicationTrigger) {
		return createEmailApplicationTrigger(mHandle, applicationTrigger, false);
	}

	/**
	 * Used to post/put a new email message application trigger to the Ayla Cloud Service. See section Application Triggers for details on
	 the AylaApplicationTrigger class in aAyla Mobile Library document.
	 * @param mHandle is where result would be returned.
	 * @param applicationTrigger is the trigger to be created.
	 * @param delayExecution could be set to true if you want to setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService createEmailApplicationTrigger(Handler mHandle, AylaApplicationTrigger applicationTrigger, Boolean delayExecution) {
		applicationTrigger.name = AylaAppNotification.aylaAppNotificationTypeEmail;
		AylaRestService rs = applicationTrigger.createTrigger(mHandle, this, applicationTrigger, delayExecution);
		return rs;
	}
	public AylaRestService updateEmailApplicationTrigger(AylaApplicationTrigger applicationTrigger) {
		return updateEmailApplicationTrigger(null, applicationTrigger, true);
	}
	public AylaRestService updateEmailApplicationTrigger(Handler mHandle, AylaApplicationTrigger applicationTrigger) {
		return updateEmailApplicationTrigger(mHandle, applicationTrigger, false);
	}
	public AylaRestService updateEmailApplicationTrigger(Handler mHandle, AylaApplicationTrigger applicationTrigger, Boolean delayExecution) {
		applicationTrigger.name = AylaAppNotification.aylaAppNotificationTypeEmail;
		AylaRestService rs = applicationTrigger.updateTrigger(mHandle, this, delayExecution);
		return rs;
	}

	/**
	 * Same as {@link AylaPropertyTrigger#getTriggers(Handler, Map, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService getTriggers(Map<String, String> callParams) {
		return getTriggers(null,  callParams, true);
	}

	/**
	 * Same as {@link AylaPropertyTrigger#getTriggers(Handler, Map, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getTriggers(Handler mHandle, Map<String, String> callParams) {
		return getTriggers(mHandle,  callParams, false);
	}

	/**
	 * Get all the application triggers for the given property.
	 * @param mHandle is where result would be returned.
	 * @param callParams is not required (TBD).
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService getTriggers(Handler mHandle,  Map<String, String> callParams, Boolean delayExecution) {
		AylaRestService rs = this.applicationTrigger.getTriggers(mHandle, this, callParams, delayExecution);
		return rs;
	}

	/**
	 * Same as {@link AylaPropertyTrigger#destroyTrigger(Handler, AylaApplicationTrigger, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService destroyTrigger(AylaApplicationTrigger applicationTrigger) {
		return destroyTrigger(null, applicationTrigger, true);
	}

	/**
	 * Same as {@link AylaPropertyTrigger#destroyTrigger(Handler, AylaApplicationTrigger, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService destroyTrigger(Handler mHandle, AylaApplicationTrigger applicationTrigger) {
		return destroyTrigger(mHandle, applicationTrigger, false);
	}

	/**
	 * Destroy a single application trigger
	 * @param mHandle is where result would be returned.
	 * @param applicationTrigger is the trigger to be destroyed
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService destroyTrigger(Handler mHandle, AylaApplicationTrigger applicationTrigger, Boolean delayExecution) {
		AylaRestService rs = applicationTrigger.destroyTrigger(mHandle, applicationTrigger, delayExecution);
		return rs;
	}
	
	// Serves internally, rs can never be null.
	static void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId) {
		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		rs.execute();
	}
}// end of AylaPropertyTrigger class                







