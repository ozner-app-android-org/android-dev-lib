//
//  AylaDeviceNotification.java
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

//----------------------------------------------- Device Notifications -------------------------------
class AylaDeviceNotificationContainer {
	@Expose
	public AylaDeviceNotification notification = null;
}

public class AylaDeviceNotification extends AylaSystemUtils 
{

	@Expose
	public String notificationType;
	@Expose
	public String deviceNickname;
	@Expose
	public Integer threshold;
	@Expose
	public String url;
	@Expose
	public String userName;
	@Expose
	public String password;
	@Expose
	public String message;
	@Expose
	protected Integer id;
	
	public AylaAppNotification appNotification = null;
	public AylaAppNotification[] appNotifications = null;

	public AylaDeviceNotification() {
		appNotification = new AylaAppNotification(); // initialize for pass through calls
	}
	
	// global constants for this class
	public static final String aylaDeviceNotificationTypeOnConnect = "on_connect";
	public static final String aylaDeviceNotificationTypeIpChange = "ip_change";
	public static final String aylaDeviceNotificationTypeOnConnectionLost = "on_connection_lost";
	public static final String aylaDeviceNotificationTypeOnConnectionRestore = "on_connection_restore";
	
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");

		result.append(this.getClass().getName() + " Object {" + NEW_LINE);
		result.append(" notificationType: " + notificationType + NEW_LINE);
		result.append(" deviceNickname: " + deviceNickname + NEW_LINE);
		result.append(" threshold: " + threshold + NEW_LINE);
		result.append(" message: " + message + NEW_LINE );
		result.append(" id: " + id + NEW_LINE);
		result.append("}");
		return result.toString();
	}

	/**
	 * Same as {@link AylaDeviceNotification#createNotification(Handler, AylaDevice, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService createNotification(AylaDevice device) {
		return createNotification(null,  device, true);
	}

	/**
	 * Same as {@link AylaDeviceNotification#createNotification(Handler, AylaDevice, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService createNotification(Handler mHandle, AylaDevice device) {
		return createNotification(mHandle,  device, false);
	}

	/**
	 * Post a new device notification associated with input param device. See section Device Service - Device Notifications in aAyla Mobile Library document for details.
	 * @param mHandle is where result would be returned.
	 * @param device is the device associated with newly created device notification.
	 * notification_type(mandatory): One of the following strings:
	 "on_connect", "ip_change", "on_connection_lost", "on_connection_restore"
	 threshold(mandatory for on_connection_lost and on_connection_restore types):
	 number of seconds for which the condition must be active before notification
	 is sent. Minimum is 300 seconds.
	 message(optional): Custom message for this notification type along default message.
	 device_nickname(optional): A nickname for the associated device.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService createNotification(Handler mHandle, AylaDevice device, Boolean delayExecution) {
		// https://ads-dev.aylanetworks.com/apiv1/devices/<devKey>/notifications.json
		Number deviceKey = device.getKey().intValue(); // Handle gson LazilyParsedNumber
		String url = String.format(Locale.getDefault(),"%s%s%d%s", deviceServiceBaseURL(), "devices/", deviceKey, "/notifications.json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.CREATE_DEVICE_NOTIFICATION);

		// {"notification":{"notification_type":"on_connection_lost", "message":"Connection dropped", "threshold:"3600"} }'
		String deviceNotificationContainerJson = "";
		try {
			AylaDeviceNotificationContainer devNotificationContainer = new AylaDeviceNotificationContainer();
			devNotificationContainer.notification = this;
			deviceNotificationContainerJson = AylaSystemUtils.gson.toJson(devNotificationContainer, AylaDeviceNotificationContainer.class);
		} catch (Exception ex) {
			AylaSystemUtils.saveToLog("%s, %s, %s:%d", "E", "DeviceNotification", "Ayla error", AylaNetworks.AML_USER_INVALID_PARAMETERS);
			return null;
		}
		
		rs.setEntity(deviceNotificationContainerJson);

		saveToLog("%s, %s, %s:%s, %s", "I", "DeviceNotification", "path", url, "createDeviceNotification");
		if (delayExecution == false) {
			rs.execute();
		}
		return rs;
	}

	/**
	 * Same as {@link AylaDeviceNotification#updateNotification(Handler, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService updateNotification() {
		return updateNotification(null,  true);
	}

	/**
	 * Same as {@link AylaDeviceNotification#updateNotification(Handler, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService updateNotification(Handler mHandle) {
		return updateNotification(mHandle,  false);
	}

	/**
	 * Put a device notification associated with input param device. See section Device Service - Device Notifications in aAyla Mobile Library document for details.
	 * @param mHandle is where result would be returned.
	 * notification_type(mandatory): One of the following strings:
	 "on_connect", "ip_change", "on_connection_lost", "on_connection_restore"
	 threshold(mandatory for on_connection_lost and on_connection_restore types):
	 number of seconds for which the condition must be active before notification
	 is sent. Minimum is 300 seconds.
	 message(optional): Custom message for this notification type along default message.
	 device_nickname(optional): A nickname for the associated device.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService updateNotification(Handler mHandle, Boolean delayExecution) {
		// http://ads-dev.aylanetworks.com/apiv1/notifications/<deviceNotificationKey>.json
		Number deviceNotificationKey = this.id.intValue(); // Handle gson LazilyParsedNumber
		String url = String.format(Locale.getDefault(), "%s%s%d%s", deviceServiceBaseURL(), "notifications/", deviceNotificationKey, ".json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.UPDATE_DEVICE_NOTIFICATION);

		String deviceNotificationContainerJson = "";
		try {
			AylaDeviceNotificationContainer devNotificationContainer = new AylaDeviceNotificationContainer();
			devNotificationContainer.notification = this;
			deviceNotificationContainerJson = AylaSystemUtils.gson.toJson(devNotificationContainer, AylaDeviceNotificationContainer.class);
		} catch (Exception ex) {
			AylaSystemUtils.saveToLog("%s, %s, %s:%d", "E", "DeviceNotification", "Ayla error", AylaNetworks.AML_USER_INVALID_PARAMETERS);
			return null;
		}
		rs.setEntity(deviceNotificationContainerJson);

		saveToLog("%s, %s, %s:%s, %s", "I", "DeviceNotification", "path", url, "updateDeviceNotification");
		if (delayExecution == false) {
			rs.execute();
		}
		return rs;
	}
	
	protected static String stripContainer(String jsonDeviceNotificationContainer, int method) throws Exception {
		String jsonDeviceNotification = "";
		String requestIdStr = (method == AylaRestService.CREATE_DEVICE_NOTIFICATION) ? "create" : "update";
		try {
			AylaDeviceNotificationContainer deviceNotificationContainer = AylaSystemUtils.gson.fromJson(jsonDeviceNotificationContainer, AylaDeviceNotificationContainer.class);
			AylaDeviceNotification deviceNotification = deviceNotificationContainer.notification;
			jsonDeviceNotification = AylaSystemUtils.gson.toJson(deviceNotification, AylaDeviceNotification.class);
			AylaSystemUtils.saveToLog("%s %s %s:%s %s.%s", "I", "DeviceNotification", "deviceNotification",
					                                       deviceNotification.toString(), requestIdStr, "stripContainer");
			return jsonDeviceNotification;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s.%s", "E", "DeviceNotification", "jsonDeviceNotificationContainer",
					                                    jsonDeviceNotificationContainer, requestIdStr, "stripContainer");
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Same as {@link AylaDeviceNotification#getNotifications(Handler, AylaDevice, Map, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public static AylaRestService getNotifications(AylaDevice device, Map<String, String> callParams) {
		return getNotifications(null, device, callParams, true);
	}

	/**
	 * Same as {@link AylaDeviceNotification#getNotifications(Handler, AylaDevice, Map, Boolean)} with no option to setup the call to execute later.
	 * **/
	public static AylaRestService getNotifications(Handler mHandle, AylaDevice device, Map<String, String> callParams) {
		return getNotifications(mHandle, device, callParams, false);
	}

	/**
	 * Get all the device notifications associated with the device.
	 *
	 * @param mHandle is where result would be returned.
	 * @param device is the device which retrieved device notifications bind to.
	 * @param callParams is not currently used. Set to null for now.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public static AylaRestService getNotifications(Handler mHandle, AylaDevice device, Map<String, String> callParams, Boolean delayExecution) {
		Number deviceKey = device.getKey().intValue(); // Handle gson LazilyParsedNumber

		// https://ads-dev.aylanetworks.com/apiv1/devices/7/notifications.xm
		String url = String.format(Locale.getDefault(),"%s%s%d%s", deviceServiceBaseURL(), "devices/", deviceKey, "/notifications.json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.GET_DEVICE_NOTIFICATIONS); 

		saveToLog("%s, %s, %s:%s, %s", "I", "DeviceNotifications", "url", url, "getNotifications");
		if (delayExecution == false) {
			rs.execute(); //Executes the request with the HTTP GET verb
		}
		return rs;
	}
	
	protected static String stripContainers(String jsonDeviceNotificationContainers) throws Exception {
		int count = 0;
		String jsonDeviceNotifications = "";
		try {
			AylaDeviceNotificationContainer[] deviceNotificationContainers = AylaSystemUtils.gson.fromJson(jsonDeviceNotificationContainers,AylaDeviceNotificationContainer[].class);
			AylaDeviceNotification[] deviceNotifications = new AylaDeviceNotification[deviceNotificationContainers.length];
			for (AylaDeviceNotificationContainer deviceNotificationContainer : deviceNotificationContainers) {
				deviceNotifications[count++]= deviceNotificationContainer.notification;   			
			}
			jsonDeviceNotifications = AylaSystemUtils.gson.toJson(deviceNotifications,AylaDeviceNotification[].class);
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "DeviceNotifications", "count", count, "stripContainers");
			return jsonDeviceNotifications;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s:%s %s", "E", "DeviceNotifications", "count", count, "jsonDeviceContainers", jsonDeviceNotificationContainers, "stripContainers");
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Same as {@link AylaDeviceNotification#destroyNotification(Handler, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService destroyNotification() {
		return destroyNotification(null, true);
	}

	/**
	 * Same as {@link AylaDeviceNotification#destroyNotification(Handler, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService destroyNotification(Handler mHandle) {
		return destroyNotification(mHandle, false);
	}

	/**
	 * Destroy a dedicated device notification.
	 *
	 * @param mHandle is where result would be returned.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService destroyNotification(Handler mHandle, Boolean delayExecution) {
		// https://ads-dev.aylanetworks.com/apiv1/notifications/<deviceNotificationKey>.json
		Number deviceNotificationKey = this.id.intValue(); // Handle gson LazilyParsedNumber
		String url = String.format(Locale.getDefault(), "%s%s%d%s", deviceServiceBaseURL(), "notifications/", deviceNotificationKey, ".json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.DESTROY_DEVICE_NOTIFICATION);

		saveToLog("%s, %s, %s:%s, %s", "I", "DeviceNotification", "path", url, "destroyDeviceNotification");
		if (delayExecution == false) {
			rs.execute();
		}
		return rs;
	}

	// ----------------------- Pass through methods --------------------------
	/**
	 * Same as {@link AylaDeviceNotification#createApp(Handler, AylaAppNotification, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService createApp(AylaAppNotification appNotification) {
		return createApp(null,appNotification, true);
	}

	/**
	 * Same as {@link AylaDeviceNotification#createApp(Handler, AylaAppNotification, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService createApp(Handler mHandle, AylaAppNotification appNotification) {
		return createApp(mHandle, appNotification, false);
	}

	/**
	 * Used to post/put a new push notification message device application notification to the Ayla Cloud Service.
	 * See section Application Notifications for details on the AylaAppNotification class in aAyla Mobile Library document.
	 *
	 * @param mHandle is where result would be returned.
	 * @param appNotification is the device app notification to be created.
	 * @param delayExecution could be set to true if you want to setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService createApp(Handler mHandle, AylaAppNotification appNotification, Boolean delayExecution) {
		AylaRestService rs = appNotification.create(mHandle, this, appNotification, delayExecution);
		return rs;
	}


	public AylaRestService updateApp(AylaAppNotification appNotification) {
		return updateApp(null,appNotification, true);
	}
	public AylaRestService updateApp(Handler mHandle, AylaAppNotification appNotification) {
		return updateApp(mHandle, appNotification, false);
	}
	public AylaRestService updateApp(Handler mHandle, AylaAppNotification appNotification, Boolean delayExecution) {
		AylaRestService rs = appNotification.update(mHandle, this, delayExecution);
		return rs;
	}

	/**
	 * Same as {@link AylaDeviceNotification#getApps(Handler, Map)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService getApps(Map<String, String> callParams) {
		return getApps(null,  callParams, true);
	}

	/**
	 * Same as {@link AylaDeviceNotification#getApps(Handler, Map)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getApps(Handler mHandle, Map<String, String> callParams) {
		return getApps(mHandle,  callParams, false);
	}

	/**
	 * Get all the device application notifications for the given device.
	 *
	 * @param mHandle is where result would be returned.
	 * @param callParams is not required at this time. Set to null.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService getApps(Handler mHandle,  Map<String, String> callParams, Boolean delayExecution) {
		AylaRestService rs = this.appNotification.gets(mHandle, this, callParams, delayExecution);
		return rs;
	}

	/**
	 * Same as {@link AylaDeviceNotification#destroyApp(Handler, AylaAppNotification, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService destroyApp(AylaAppNotification appNotification) {
		return destroyApp(null, appNotification, true);
	}

	/**
	 * Same as {@link AylaDeviceNotification#destroyApp(Handler, AylaAppNotification, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService destroyApp(Handler mHandle, AylaAppNotification appNotification) {
		return destroyApp(mHandle, appNotification, false);
	}

	/**
	 * Destroy a single device application notification.
	 *
	 * @param mHandle is where result would be returned.
	 * @param appNotification is the notification to be destroyed
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService destroyApp(Handler mHandle, AylaAppNotification appNotification, Boolean delayExecution) {
		AylaRestService rs = appNotification.destroy(mHandle, appNotification, delayExecution);
		return rs;
	}
}





