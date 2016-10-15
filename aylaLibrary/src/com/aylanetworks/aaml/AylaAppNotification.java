//
//  AylaAppNotification.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 06/12/2014.
//  Copyright (c) 2014 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import java.util.Locale;
import java.util.Map;

import com.google.gson.annotations.Expose;

import android.os.Handler;


//---------------------------------------------- Application Notifications -------------------------------
class AylaAppNotificationContainer {
	@Expose
	public AylaAppNotification notification_app = null;
}

/**
* The AylaAppNotification class is designed to CRUD actions executed when the owner device notification is triggered. 
*
* Types and Parameters:
*
* For each notification app type the parameters are added to the notificationAppParameters class. 
* The appTypes and the accepted notificaitonAppParameters for each are:
* 
* "email" -  "username", "email", "message"
* "sms" -  "username", "country_code", "phone_number", "message"
* "push_ios" : "registration_id", "application_id", "message", "push_sound", "push_mdata" (only for iOS devices)
* "push_android" : "registration_id", "message", "push_sound", "push_mdata" (only for Android devices)
* "push_baidu" : "app_id", "channel_id", "message", "message_title", "push_sound", "push_mdata"
*
* Param details:
*     message: for iOS and Android push apps max length is 100 chars
*     push_sound: Used for iOS and Android push apps (max length 50 chars)
*         Contains the sound file name present on mobile app eg: my_sound_file.mp3
*     push_mdata: Used for iOS and Android push apps (max length 100 chars)
*         Contains comma separated data needed by app eg: {"key1":"value1", "key2":"value2"}
* 
*/
public class AylaAppNotification extends AylaSystemUtils {
	@Expose
	public String appType;			// Required - "email", "sms", "push_android", "push_baidu". "push_ios" may be read but not set in Android
	@Expose
	public AylaAppNotificationParameters notificationAppParameters = new AylaAppNotificationParameters(); // All the appType specific parameters
	
	@Expose
	public String nickname;			// User assigned name for this notificatiion app
	@Expose
	private Number notificationId;	// The ID of the owner device notification
	@Expose
	private Number id;				// The ID of this notification application
	
	// global constants for this class
	public static final String aylaAppNotificationTypeEmail = "email";                     
	public static final String aylaAppNotificationTypeSms = "sms";                          
	public static final String aylaAppNotificationTypePushGoogle = "push_android"; // backward compatible          
	public static final String aylaAppNotificationTypePushBaidu = "push_baidu";             
	public static String aylaAppNotificationTypePush = aylaAppNotificationTypePushGoogle;

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");

		result.append(this.getClass().getName() + " Object {" + NEW_LINE);
		result.append(" appType: " + appType + NEW_LINE);
		result.append(" nickname: " + nickname + NEW_LINE);
		result.append(" notificationId: " + notificationId + NEW_LINE);
		result.append(" id: " + id + NEW_LINE);
		result.append("}");
		return result.toString();
	}


	/**
	 * Same as {@link AylaAppNotification#create(Handler, AylaDeviceNotification, AylaAppNotification, Boolean)} with no option to setup the call to execute later and no handler to return results.
	 * */
	public AylaRestService  create(AylaDeviceNotification deviceNotification,  AylaAppNotification applicationNotification) {
		return  create(null,  deviceNotification,  applicationNotification, true);
	}

	/**
	 * Same as {@link AylaAppNotification#create(Handler, AylaDeviceNotification, AylaAppNotification, Boolean)} with no option to setup the call to execute later.
	 * */
	public AylaRestService  create(Handler mHandle, AylaDeviceNotification deviceNotification,  AylaAppNotification applicationNotification) {
		return  create(mHandle,  deviceNotification,  applicationNotification, false);
	}

	/**
	 * Used to create a new device application notification to the Ayla Cloud Service.
	 * See section Application Notifications for details on the AylaAppNotification class in aAyla Mobile Library document.
	 * @param mHandle is where result would be returned.
	 * @param deviceNotification is the device notification created application notification binds to
	 * @param applicationNotification is the notification to be created.
	 * @param delayExecution could be set to true if you want to setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService  create(Handler mHandle, AylaDeviceNotification deviceNotification,  AylaAppNotification applicationNotification, Boolean delayExecution) {
		String applicationNotificationContainerJson = null;

		try {
			// {"notification_app":{"app_type":"email","nickname":"test","notification_app_parameters":{"username":"abc","email":"abc@aylanetworks.com"}}}
			AylaAppNotificationContainer appNotificationContainer = new AylaAppNotificationContainer();
			appNotificationContainer.notification_app = applicationNotification;
			applicationNotificationContainerJson = AylaSystemUtils.gson.toJson(appNotificationContainer, AylaAppNotificationContainer.class);
			
			// https://ads-dev.aylanetworks.com/apiv1/notifications/<deviceNotificationKey>/notification_apps.json
			Number devNotifyKey = deviceNotification.id.intValue(); // Handle gson LazilyParsedNumber
			String url = String.format(Locale.getDefault(), "%s%s%d%s", deviceServiceBaseURL(), "notifications/", devNotifyKey, "/notification_apps.json");
			AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.CREATE_APP_NOTIFICATION);
			rs.setEntity(applicationNotificationContainerJson);

			saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AppNotification", "path", url, "appType", applicationNotification.appType, "create");
			if (delayExecution == false) {
				rs.execute();
			}
			
			return rs;
		} catch (Exception ex) {
			AylaSystemUtils.saveToLog("%s, %s, %s:%s", "E", "AppNotification", "error", ex.getCause());
			return null;
		}
	}

	/**
	 * Same as {@link AylaAppNotification#update(Handler, AylaDeviceNotification, Boolean)} with no option to setup the call to execute later and no handler to return results.
	 * */
	public AylaRestService  update(AylaDeviceNotification deviceNotification) {
		return  update(null,  deviceNotification, true);
	}

	/**
	 * Same as {@link AylaAppNotification#update(Handler, AylaDeviceNotification, Boolean)} with no option to setup the call to execute later.
	 * */
	public AylaRestService  update(Handler mHandle, AylaDeviceNotification deviceNotification) {
		return  update(mHandle,  deviceNotification,  false);
	}

	/**
	 * Used to update a new device application notification in the Ayla Cloud Service. See section Application Notifications for details on the AylaAppNotification class in aAyla Mobile Library document.
	 * @param mHandle is where result would be returned.
	 * @param deviceNotification is the device notification updated application notification binds to
	 * @param delayExecution could be set to true if you want to setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService  update(Handler mHandle, AylaDeviceNotification deviceNotification,  Boolean delayExecution) {
		String applicationNotificationContainerJson = null;

		try {
			AylaAppNotificationContainer appNotificationContainer = new AylaAppNotificationContainer();
			appNotificationContainer.notification_app = this;
			applicationNotificationContainerJson = AylaSystemUtils.gson.toJson(appNotificationContainer, AylaAppNotificationContainer.class);
			
			// https://ads-dev.aylanetworks.com/apiv1/notifications/<deviceNotificationKey>/notification_apps/<appNotificaitonKey>.json
			Number _notificationId = this.notificationId.intValue(); // Handle gson LazilyParsedNumber
			Number _id = this.id.intValue(); // Handle gson LazilyParsedNumber
			String url = String.format(Locale.getDefault(), "%s%s%d%s%d%s", deviceServiceBaseURL(), "notifications/", _notificationId, "/notification_apps/", _id, ".json");
			AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.UPDATE_APP_NOTIFICATION);
			rs.setEntity(applicationNotificationContainerJson);

			saveToLog("%s, %s, %s:%s, %s", "I", "AppNotification", "path", url, "update");
			if (delayExecution == false) {
				rs.execute();
			}
			return rs;
		} catch (Exception ex) {
			AylaSystemUtils.saveToLog("%s, %s, %s:%s", "E", "AppNotification", "error", ex.getCause());
			return null;
		}
	}
	
	protected static String stripContainer(String jsonAppNotificationContainer, int method) throws Exception {
		String jsonAppNotification = "";
		String requestIdStr = (method == AylaRestService.CREATE_APP_NOTIFICATION) ? "create" : "update";
		try {
			AylaAppNotificationContainer applicationNotificationContainer = AylaSystemUtils.gson.fromJson(jsonAppNotificationContainer,AylaAppNotificationContainer.class);
			AylaAppNotification applicationNotification = applicationNotificationContainer.notification_app;
			
			jsonAppNotification = AylaSystemUtils.gson.toJson(applicationNotification,AylaAppNotification.class);
			AylaSystemUtils.saveToLog("%s %s %s:%s %s.%s", "I", "AppNotification", "applicationNotification",
					                                       applicationNotification.toString(), requestIdStr, "stripContainer");
			return jsonAppNotification;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s.%s", "E", "AppNotification", "jsonAppNotificationContainer",
					                                       jsonAppNotificationContainer, requestIdStr, "stripContainer");
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Same as {@link AylaAppNotification#gets(Handler, AylaDeviceNotification, Map, Boolean)} with no option to setup the call to execute later and no handler to return results.
	 * */
	public AylaRestService gets(AylaDeviceNotification deviceNotification, Map<String, String> callParams) {
		return gets(null,deviceNotification, callParams, true);
	}

	/**
	 * Same as {@link AylaAppNotification#gets(Handler, AylaDeviceNotification, Map, Boolean)} with no option to setup the call to execute later.
	 * */
	public AylaRestService gets(Handler mHandle, AylaDeviceNotification deviceNotification, Map<String, String> callParams) {
		return gets(mHandle, deviceNotification, callParams, false);
	}


	/**
	 * Get all the application notifications for the given device.
	 * @param mHandle is where result would be returned.
	 * @param deviceNotification is the device notification that retrieved application notifications might bind to
	 * @param callParams is not required (TBD).
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService gets(Handler mHandle, AylaDeviceNotification deviceNotification, Map<String, String> callParams, Boolean delayExecution) {
		
		try {
			// https://ads-dev.aylanetworks.com/apiv1/notifications/<deviceNotificationId>/notification_apps.json
			Number _id = deviceNotification.id.intValue(); // Handle gson LazilyParsedNumber
			String url = String.format(Locale.getDefault(), "%s%s%d%s", deviceServiceBaseURL(), "notifications/", _id, "/notification_apps.json");
			AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.GET_APP_NOTIFICATIONS); 
	
			saveToLog("%s, %s, %s:%s, %s", "I", "AppNotification", "url", url, "gets");
			if (delayExecution == false) {
				rs.execute();
			}
			return rs;
		} catch (Exception ex) {
			AylaSystemUtils.saveToLog("%s, %s, %s:%s", "E", "AppNotification", "error", ex.getCause());
			return null;
		}
	}
	protected static String stripContainers(String jsonAppNotificationContainers) throws Exception {
		int count = 0;
		String jsonAppNotifications = "";
		try {
			AylaAppNotificationContainer[] applicationNotificationContainers = AylaSystemUtils.gson.fromJson(jsonAppNotificationContainers,AylaAppNotificationContainer[].class);
			AylaAppNotification[] applicationNotifications = new AylaAppNotification[applicationNotificationContainers.length];
			for (AylaAppNotificationContainer applicationNotificationContainer : applicationNotificationContainers) {
				applicationNotifications[count]= applicationNotificationContainer.notification_app;
				count++; 			
			}
			jsonAppNotifications = AylaSystemUtils.gson.toJson(applicationNotifications,AylaAppNotification[].class);
			AylaSystemUtils.saveToLog("%s %s %s:%d %s", "I", "AppNotification", "count", count, "stripContainers");
			return jsonAppNotifications;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%d %s:%s %s", "E", "AppNotification", "count", count, "jsonDeviceContainers", jsonAppNotificationContainers, "stripContainers");
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Same as {@link AylaAppNotification#destroy(Handler, Boolean)} with no option to setup the call to execute later and no handler to return results.
	 * */
	public AylaRestService destroy() {
		return destroy(null, this, true);
	}

	/**
	 * Same as {@link AylaAppNotification#destroy(Handler, Boolean)} with no option to setup the call to execute later.
	 * */
	public AylaRestService destroy(Handler mHandle) {
		return destroy(mHandle, this, false);
	}

	/**
	 * Destroy a single application notification
	 * @param mHandle is where result would be returned.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService destroy(Handler mHandle, Boolean delayExecution) {
		return destroy(mHandle, this, false);
	}

	/**
	 * Destroy a single application notification
	 * @param applicationNotification is the notification to be destroyed
	 * @return AylaRestService object
	 */
	public AylaRestService destroy(AylaAppNotification applicationNotification) {
		return destroy(null, applicationNotification, true);
	}

	/**
	 * Destroy a single application notification
	 * @param mHandle is where result would be returned.
	 * @param applicationNotification is the notification to be destroyed
	 * @return AylaRestService object
	 */
	public AylaRestService destroy(Handler mHandle, AylaAppNotification applicationNotification) {
		return destroy(mHandle, applicationNotification, false);
	}

	/**
	 * Destroy a single application notification
	 * @param mHandle is where result would be returned.
	 * @param applicationNotification is the notification to be destroyed
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService destroy(Handler mHandle, AylaAppNotification applicationNotification, Boolean delayExecution) {
		
		try {
			// https://ads-dev.aylanetworks.com/apiv1/notifications/<deviceNotifiationId>/notification_apps/<notificationAppId>.json
			Number _notificationId = this.notificationId.intValue(); // Handle gson LazilyParsedNumber
			Number _id = this.id.intValue(); // Handle gson LazilyParsedNumber
			String url = String.format(Locale.getDefault(), "%s%s%d%s%d%s", deviceServiceBaseURL(), "notifications/", _notificationId, "/notification_apps/", _id, ".json");
			AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.DESTROY_APP_NOTIFICATION);
	
			saveToLog("%s, %s, %s:%s, %s", "I", "AppNotification", "path", url, "destroy");
			if (delayExecution == false) {
				rs.execute();
			}
			return rs;
		} catch (Exception ex) {
			AylaSystemUtils.saveToLog("%s, %s, %s:%s", "E", "AppNotification", "error", ex.getCause());
			return null;
		}
	}
}
