//
//  AylaNotify.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 11/25/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import com.google.gson.annotations.Expose;

import android.os.Handler;

public class AylaNotify {

	@Expose
	public String type; 	// Notification is about a "session" or a "property" change
	@Expose
	public int statusCode;  
	@Expose
	public String dsn; 		// The Device Serial Number of the device the property change notification references
	@Expose
	public String description; // The response description for a property change notification
	@Expose
	public String properties[]; 	// Array of property names for a property change notification
	@Expose 
	public AylaDeviceGateway.AylaConnectionStatus connStatus[];
	
	static public Handler notifierHandle = null;
	static AylaRestService rsNotifier = null;

	/**
	 * register callback handle from main activity
	 */
	public static void register(Handler handle) {
		if (handle != null) {
			notifierHandle = handle;
			rsNotifier = new AylaRestService(notifierHandle, "notify_register", AylaRestService.PROPERTY_CHANGE_NOTIFIER);
		}
	}

	public static void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId, AylaDevice fromDevice) {
		if (notifierHandle == null) {
			return;
		}
		if (rs == null) {
			rs = rsNotifier;
		}

		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		if ( fromDevice != null && fromDevice.getLanModule() != null ) {
			// Only set notifyOutstanding if this is not an error.
			// We will accept 2xx or 0 as "success" response codes.
			if ( thisResponseCode / 100 == 2 || thisResponseCode == 0 ) {
				fromDevice.getLanModule().getSession().setNotifyOutstanding(true);
			}
		}
		rs.execute(); // call notifier handler in main activity
	}
}
