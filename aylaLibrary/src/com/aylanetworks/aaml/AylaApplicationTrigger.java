//
//  AylaApplicationTrigger.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 8/25/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import java.util.Locale;
import java.util.Map;

import com.aylanetworks.aaml.models.AylaBaiduMessage;
import com.google.gson.annotations.Expose;

import android.os.Handler;
import android.text.TextUtils;


//---------------------------------------------- Application Triggers -------------------------------
class AylaApplicationTriggerContainer {
	@Expose
	public AylaApplicationTrigger trigger_app = null;
}

/**
 * Class for creating application triggers
 */
public class AylaApplicationTrigger extends AylaSystemUtils {
	// sms
	@Expose
	public String name;         // name of this app trigger
	@Expose
	public String nickname;     // user friendly display name of this app trigger
	@Expose
	public String countryCode;  // phone country country code for SMS notification
	@Expose
	public String phoneNumber;  // phone number for SMS notification

	@Expose
	private String messageTitle; // Optional for now.
	
	@Expose
	public String message;      // message to send in the notification

	//email
	@Expose
	public String username;     // familiar name for notification greeting
	
	@Expose
	public String contactId;   // used for notification by contact Id reference
	
	@Expose
	public String emailAddress; // email address for email notification
	@Expose
	public String emailTemplateId;  // associated template Id as defined in the dashboard
	@Expose
    public String emailBodyHtml;    // fully encoded HTML message displayed to the user
	@Expose
	public String emailSubject; // email subject for email notifications
	
	
	
	// push Notification
	@Expose
	public String registrationId;   // required for google push
	@Expose
	public String channelId;    // requuired for baidu push
	
	@Expose
	public String pushSound;    // "none", "default", "alert", or sound file in the Music directory
	@Expose
	public String pushMdata;    // custom app data

	// mapped for sms, email, or push notification
	@Expose
	private String param1;
	@Expose
	private String param2;
	@Expose
	private String param3;
	@Expose
	private String param4;
	@Expose
	private String param5;
	
	// General
	@Expose
	public String retrievedAt;	// returned with new object success
	
	@Expose
	private Number key;

	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");

		result.append(this.getClass().getName() + " Object {" + NEW_LINE);
		result.append(" name: " + name + NEW_LINE);
		result.append(" contactId: " + contactId + NEW_LINE);
		result.append(" channel_id: " + channelId + NEW_LINE);
		result.append(" countryCode: " + countryCode + NEW_LINE);
		result.append(" phoneNumber: " + phoneNumber + NEW_LINE );
		result.append(" messageTitle: " + messageTitle + NEW_LINE );
		result.append(" message: " + message + NEW_LINE );
		result.append(" emailTemplateId: " + emailTemplateId + NEW_LINE );
		result.append(" emailBodyHtml: " + emailBodyHtml + NEW_LINE );
		result.append(" emailSubject: " + emailSubject + NEW_LINE );
		result.append(" username: " + username + NEW_LINE );
		result.append(" emailAddress: " + emailAddress + NEW_LINE );
		result.append(" pushSound: " + pushSound + NEW_LINE );
		result.append(" pushMdata: " + pushMdata + NEW_LINE );
		result.append("}");
		return result.toString();
	}

	/**
	 * Same as {@link AylaApplicationTrigger#createTrigger(Handler, AylaPropertyTrigger, AylaApplicationTrigger, Boolean)} with no option to set to execute later and no handler to return results.
	 * This method is used to make synchronous calls.
	 */
	public AylaRestService createTrigger(AylaPropertyTrigger propertyTrigger,  AylaApplicationTrigger applicationTrigger) {
		return createTrigger(null,  propertyTrigger,  applicationTrigger, true);
	}

	/**
	 * Same as {@link AylaApplicationTrigger#createTrigger(Handler, AylaPropertyTrigger, AylaApplicationTrigger, Boolean)} with no option to set to execute later.
	 * See section Application Triggers for details on the AylaApplicationTrigger class in aAyla Mobile Library document.
	 */
	public AylaRestService createTrigger(Handler mHandle, AylaPropertyTrigger propertyTrigger,  AylaApplicationTrigger applicationTrigger) {
		return createTrigger(mHandle,  propertyTrigger,  applicationTrigger, false);
	}

	/**
	 * Used to create a new application trigger in the Ayla cloud service.
	 * See section Application Triggers for details on the AylaApplicationTrigger class in aAyla Mobile Library document.
	 * @param mHandle is where result would be returned.
	 * @param propertyTrigger is the property trigger created application trigger binds to
	 * @param applicationTrigger is the trigger to be created.
	 * @param delayExecution could be set to true if you want to setup this call but have it execute on an external event.
	 * @return {@link AylaRestService} object
	 */
	public AylaRestService createTrigger(Handler mHandle, AylaPropertyTrigger propertyTrigger,  AylaApplicationTrigger applicationTrigger, Boolean delayExecution) {
		String applicationTriggerContainerJson = null;
		
		if ( TextUtils.equals(AylaAppNotification.aylaAppNotificationTypeSms, applicationTrigger.name) ) {
			// {"trigger_app": {"name":"sms", "param1":"1", "param2":"4085551111", "param3":"Hi. Pushbutton event"}}
			if (TextUtils.isEmpty(applicationTrigger.countryCode)) {
				applicationTrigger.param1 = "1";// country code is required by service, or 422 returned. By default US.
			} else {
				applicationTrigger.param1 = applicationTrigger.countryCode.replaceFirst("^0*", "");
			}
			applicationTrigger.param2 = applicationTrigger.phoneNumber;
			applicationTrigger.param3 = applicationTrigger.message;
		} else if ( TextUtils.equals(AylaAppNotification.aylaAppNotificationTypeEmail, applicationTrigger.name) ) {
			// {"trigger_app":{"name":"myEmail","username":"myName","param1":"myEmailAddress"}}
			applicationTrigger.param1 = applicationTrigger.emailAddress;
			applicationTrigger.param3 = applicationTrigger.message;
		} else if ( TextUtils.equals(AylaAppNotification.aylaAppNotificationTypePushGoogle, applicationTrigger.name) ) {
			// {"trigger_app":{"name":"myEmail","username":"myName","param1":"myEmailAddress"}}
			applicationTrigger.param1 = applicationTrigger.registrationId;
			applicationTrigger.param3 = applicationTrigger.message;
		} else if ( TextUtils.equals(AylaAppNotification.aylaAppNotificationTypePushBaidu, applicationTrigger.name)) {
//			applicationTrigger.param1 = applicationTrigger.userId;
			applicationTrigger.param1 = AylaSystemUtils.appId;
			applicationTrigger.param2 = applicationTrigger.channelId;
//			applicationTrigger.param3 = applicationTrigger.message;
			AylaBaiduMessage msg = new AylaBaiduMessage();
			msg.msg = applicationTrigger.message;
			msg.sound = applicationTrigger.pushSound;
			msg.data = applicationTrigger.pushMdata;
			msg.msgType = 0; // for normal message. 
			String jsonMsg = AylaSystemUtils.gson.toJson(msg, AylaBaiduMessage.class);
			applicationTrigger.param3 = jsonMsg;
			
			applicationTrigger.param4 = applicationTrigger.messageTitle;
		} else {
			AylaSystemUtils.saveToLog("%s, %s, %s:%d", "E", "ApplicationTrigger", "Ayla error", AylaNetworks.AML_USER_INVALID_PARAMETERS);
			return null;
		}
		try {
			AylaApplicationTriggerContainer appTriggerContainer = new AylaApplicationTriggerContainer();
			appTriggerContainer.trigger_app = applicationTrigger;
			applicationTriggerContainerJson = AylaSystemUtils.gson.toJson(appTriggerContainer, AylaApplicationTriggerContainer.class);
		} catch (Exception ex) {
			AylaSystemUtils.saveToLog("%s, %s, %s:%d", "E", "ApplicationTrigger", "Ayla error", AylaNetworks.AML_USER_INVALID_PARAMETERS);
			return null;
		}

		Number propertyKey = propertyTrigger.key.intValue(); // Handle gson LazilyParsedNumber
		String url = String.format(Locale.getDefault(), "%s%s%d%s", deviceServiceBaseURL(), "triggers/", propertyKey, "/trigger_apps.json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.CREATE_APPLICATION_TRIGGER);
		rs.setEntity(applicationTriggerContainerJson);

		saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "ApplicationTrigger", "path", url, "name", applicationTrigger.name, "createApplicationTrigger");
		if (delayExecution == false) {
			rs.execute();
		}
		return rs;
	}
	
	/**
	 * Same as {@link AylaApplicationTrigger#updateTrigger(Handler, AylaPropertyTrigger, Boolean)} with no option to set to execute later and no handler to return results.
	 * This method is used to update triggers synchronously. See section Application Triggers for details on the AylaApplicationTrigger class in aAyla Mobile Library document.
	 */
	public AylaRestService updateTrigger(AylaPropertyTrigger propertyTrigger) {
		return updateTrigger(null,  propertyTrigger, true);
	}

	/**
	 * Same as {@link AylaApplicationTrigger#updateTrigger(Handler, AylaPropertyTrigger, Boolean)} with no option to set to execute later.
	 * See section Application Triggers for details on the AylaApplicationTrigger class in aAyla Mobile Library document.
	 */
	public AylaRestService updateTrigger(Handler mHandle, AylaPropertyTrigger propertyTrigger) {
		return updateTrigger(mHandle,  propertyTrigger,  false);
	}

	/**
	 * Used to update an application trigger in the Ayla Cloud Service and return results to a handler.
	 * See section Application Triggers for details on the AylaApplicationTrigger class in aAyla Mobile Library document.
	 * @param mHandle is where result would be returned.
	 * @param propertyTrigger is the property trigger created application trigger binds to
	 * @param delayExecution could be set to true if you want to setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService updateTrigger(Handler mHandle, AylaPropertyTrigger propertyTrigger,  Boolean delayExecution) {
		String applicationTriggerContainerJson = null;
		
		if ( TextUtils.equals(this.name, AylaAppNotification.aylaAppNotificationTypeSms) ) {
			// {"trigger_app": {"name":"sms", "param1":"1", "param2":"4085551111", "param3":"Hi. Pushbutton event"}}
			this.param1 = this.countryCode.replaceFirst("^0*", "");
			this.param2 = this.phoneNumber;
			this.param3 = this.message;
		} else if ( TextUtils.equals(this.name, AylaAppNotification.aylaAppNotificationTypeEmail) ) {
			// {"trigger_app":{"name":"myEmail","username":"myName","param1":"myEmailAddress"}}
			this.param1 = this.emailAddress;
			this.param3 = this.message;
		} else if ( TextUtils.equals(this.name, AylaAppNotification.aylaAppNotificationTypePushGoogle) ) {
			// {"trigger_app":{"name":"myEmail","username":"myName","param1":"myEmailAddress"}}
			this.param1 = this.registrationId;
			this.param3 = this.message;
		} else if ( TextUtils.equals(this.name, AylaAppNotification.aylaAppNotificationTypePushBaidu) ) {
//			this.param1 = this.userId;   
			this.param1 = AylaSystemUtils.appId;
			this.param2 = this.channelId;          
//			this.param3 = this.message;  
			AylaBaiduMessage msg = new AylaBaiduMessage();
			msg.msg = this.message;
			msg.sound = this.pushSound;
			msg.data = this.pushMdata;
			msg.msgType = 0; // for normal message. 
			String jsonMsg = AylaSystemUtils.gson.toJson(msg, AylaBaiduMessage.class);
			this.param3 = jsonMsg;
			
			this.param4 = this.messageTitle;             
		} else {
			AylaSystemUtils.saveToLog("%s, %s, %s:%d", "E", "ApplicationTrigger", "Ayla error", AylaNetworks.AML_USER_INVALID_PARAMETERS);
			return null;
		}
		try {
			AylaApplicationTriggerContainer appTriggerContainer = new AylaApplicationTriggerContainer();
			appTriggerContainer.trigger_app = this;
			applicationTriggerContainerJson = AylaSystemUtils.gson.toJson(appTriggerContainer, AylaApplicationTriggerContainer.class);
		} catch (Exception ex) {
			AylaSystemUtils.saveToLog("%s, %s, %s:%d", "E", "ApplicationTrigger", "Ayla error", AylaNetworks.AML_USER_INVALID_PARAMETERS);
			return null;
		}

		Number applicationTriggerKey = this.key.intValue(); // Handle gson LazilyParsedNumber
		String url = String.format(Locale.getDefault(), "%s%s%d%s", deviceServiceBaseURL(), "trigger_apps/", applicationTriggerKey, ".json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.UPDATE_APPLICATION_TRIGGER);
		rs.setEntity(applicationTriggerContainerJson);

		saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "ApplicationTrigger", "path", url, "name", this.name, "updateApplicationTrigger");
		if (delayExecution == false) {
			rs.execute();
		}
		return rs;
	}
	
	protected static String stripContainer(String jsonApplicationTriggerContainer, int method) throws Exception {
		String jsonApplicationTrigger = "";
		String requestIdStr = (method == AylaRestService.CREATE_APPLICATION_TRIGGER) ? "create" : "update";
		try {
			AylaApplicationTriggerContainer applicationTriggerContainer = AylaSystemUtils.gson.fromJson(jsonApplicationTriggerContainer,AylaApplicationTriggerContainer.class);
			AylaApplicationTrigger applicationTrigger = applicationTriggerContainer.trigger_app;

			applicationTrigger.convertParamsToAppNames();
			
			jsonApplicationTrigger = AylaSystemUtils.gson.toJson(applicationTrigger,AylaApplicationTrigger.class);
			AylaSystemUtils.saveToLog("%s %s %s:%s %s.%s", "I", "ApplicationTrigger", "applicationTrigger",
					                                       applicationTrigger.toString(), requestIdStr, "stripContainer");
			return jsonApplicationTrigger;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s.%s", "E", "ApplicationTrigger", "jsonApplicationTriggerContainer",
					                                       jsonApplicationTriggerContainer, requestIdStr, "stripContainer");
			e.printStackTrace();
			throw e;
		}
	}

	void convertParamsToAppNames() {
		
		if (TextUtils.equals(this.name, AylaAppNotification.aylaAppNotificationTypeSms)) {
			// {"trigger_app": {"name":"sms", "param1":"1", "param2":"4085551111", "param3":"Hi. Pushbutton event"}}
			this.countryCode = this.param1;
			this.phoneNumber = this.param2;
			this.message = this.param3;
		} else if (TextUtils.equals(this.name, AylaAppNotification.aylaAppNotificationTypeEmail)) {
			// {"trigger_app":{"name":"email","username":"Dave","param1":"emailAddress"}}
			// userName implicit
			this.emailAddress = this.param1;
			this.message = this.param3;
		} else if (TextUtils.equals(this.name, AylaAppNotification.aylaAppNotificationTypePushGoogle)) {
			this.registrationId = this.param1;
			this.message = this.param3;
		} else if (TextUtils.equals(this.name, AylaAppNotification.aylaAppNotificationTypePushBaidu)) {
//			this.userId = this.param1;
			if (TextUtils.equals(AylaSystemUtils.appId , this.param1)) {
				this.channelId = this.param2;
				this.message = this.param3;
				this.messageTitle = this.param4;
			}
		} else {
			String appName = (this.name != null) ? this.name : "null";
			saveToLog("%s, %s, %s:%s %s", "E", "ApplicationTrigger", "appName", appName, "stripContainer: Unsupported application");
		}
	}

	/**
	 * Same as {@link AylaApplicationTrigger#getTriggers(Handler, AylaPropertyTrigger, Map, Boolean)} with no option to execute later and no handler to return results.
	 */
	public AylaRestService getTriggers(AylaPropertyTrigger propertyTrigger, Map<String, String> callParams) {
		return getTriggers(null,propertyTrigger, callParams, true);
	}

	/**
	 * Same as {@link AylaApplicationTrigger#getTriggers(Handler, AylaPropertyTrigger, Map, Boolean)} with no option to execute later.
	 */
	public AylaRestService getTriggers(Handler mHandle, AylaPropertyTrigger propertyTrigger, Map<String, String> callParams) {
		return getTriggers(mHandle, propertyTrigger, callParams, false);
	}

	/**
	 * Get all the application triggers for the given property.
	 * @param mHandle is where result would be returned.
	 * @param propertyTrigger is the property trigger that retrieved application triggers might bind to
	 * @param callParams is not required (TBD).
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService getTriggers(Handler mHandle, AylaPropertyTrigger propertyTrigger, Map<String, String> callParams, Boolean delayExecution) {
		Number propKey = propertyTrigger.key.intValue(); // Handle gson LazilyParsedNumber

		//triggers/122/trigger_app.json
		String url = String.format(Locale.getDefault(), "%s%s%d%s", deviceServiceBaseURL(), "triggers/", propKey, "/trigger_apps.json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.GET_APPLICATION_TRIGGERS); 

		saveToLog("%s, %s, %s:%s, %s", "I", "ApplicationTrigger", "url", url, "getTriggers");
		if (delayExecution == false) {
			rs.execute();
		}
		return rs;
	}
	
	
	protected static String stripContainers(String jsonApplicationTriggerContainers) throws Exception {
		int count = 0;
		String jsonApplicationTriggers = "";
		try {
			AylaApplicationTriggerContainer[] applicationTriggerContainers = AylaSystemUtils.gson.fromJson(jsonApplicationTriggerContainers,AylaApplicationTriggerContainer[].class);
			AylaApplicationTrigger[] applicationTriggers = new AylaApplicationTrigger[applicationTriggerContainers.length];
			for (AylaApplicationTriggerContainer applicationTriggerContainer : applicationTriggerContainers) {
				applicationTriggers[count]= applicationTriggerContainer.trigger_app;
				applicationTriggers[count].convertParamsToAppNames();
				count++; 			
			}
			jsonApplicationTriggers = AylaSystemUtils.gson.toJson(applicationTriggers,AylaApplicationTrigger[].class);
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "ApplicationTrigger", "count", count, "stripContainers");
			return jsonApplicationTriggers;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s:%s %s", "E", "ApplicationTrigger", "count", count, "jsonPropertyContainers", jsonApplicationTriggerContainers, "stripContainers");
			e.printStackTrace();
			throw e;
		}
	}


	/**
	 * Same as {@link AylaApplicationTrigger#destroyTrigger(Handler, Boolean)} with no option to setup call to execute later and no handler to return results.
	 */
	public AylaRestService destroyTrigger() {
		return destroyTrigger(null, this, true);
	}

	/**
	 * Same as {@link AylaApplicationTrigger#destroyTrigger(Handler, Boolean)} with no option to setup call to execute later.
	 */
	public AylaRestService destroyTrigger(Handler mHandle) {
		return destroyTrigger(mHandle, this, false);
	}

	/**
	 * Destroy a single application trigger
	 * @param mHandle is where result would be returned.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService destroyTrigger(Handler mHandle, Boolean delayExecution) {
		return destroyTrigger(mHandle, this, false);
	}

	/**
	 * Destroy a single application trigger
	 * @param applicationTrigger is the trigger to be destroyed
	 * @return AylaRestService object
	 */
	public AylaRestService destroyTrigger(AylaApplicationTrigger applicationTrigger) {
		return destroyTrigger(null, applicationTrigger, true);
	}

	/**
	 * Destroy a single application trigger
	 * @param mHandle is where result would be returned.
	 * @param applicationTrigger is the trigger to be destroyed
	 * @return AylaRestService object
	 */
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
		Number applicationTriggerKey = applicationTrigger.key.intValue(); // Handle gson LazilyParsedNumber
		String url = String.format(Locale.getDefault(), "%s%s%d%s", deviceServiceBaseURL(), "trigger_apps/", applicationTriggerKey, ".json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.DESTROY_APPLICATION_TRIGGER);

		saveToLog("%s, %s, %s:%s, %s", "I", "ApplicationTrigger", "path", url, "destroyApplicationTrigger");
		if (delayExecution == false) {
			rs.execute();
		}
		return rs;
	}
}
