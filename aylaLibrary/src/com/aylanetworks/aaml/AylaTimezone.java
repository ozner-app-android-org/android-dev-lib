//
//  AylaTimezone.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 11/27/13.
//  Copyright (c) 2013 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import com.google.gson.annotations.Expose;

import android.os.Handler;

// ------------------------------ Timezone ----------------------------------

class AylaTimezoneContainer {
	@Expose
	public AylaTimezone timeZone = null;
}

public class AylaTimezone extends AylaSystemUtils {
	
	//Timezone object properties
	@Expose
	public String utcOffset;			// Required.
	@Expose
	public Boolean dst;					// Optional. BOOL value which specifies if the location follows DS T
	@Expose
	public Boolean dstActive;			// Optional. BOOL value which specifies if DST is currenlty active
	@Expose
	public String dstNextChangeDate;	// Optional. Next DST state change from active/inactive OR from inactive/active. Forma:yyyy-mm-dd
	@Expose
	public String dstNextChangeTime;
	@Expose
	public String tzId;				// Optional. String identifier for the timezone. eg "America/New_York"
	@Expose
	Number key;

	public String toString()
	{
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");

		result.append(this.getClass().getName() + " Object {" + NEW_LINE);
		result.append(" utcOffset: " + utcOffset + NEW_LINE);
		result.append(" dst: " + dst + NEW_LINE);
		result.append(" dstActive: " + dstActive + NEW_LINE );
		result.append(" dstNextChangeDate: " + dstNextChangeDate + NEW_LINE );
		result.append(" dstNextChangeTime: " + dstNextChangeTime + NEW_LINE );
		result.append(" tzId: " + tzId + NEW_LINE );
		//result.append(" key: " + key + NEW_LINE);
		result.append("}");
		return result.toString();
	}

	/**
	 * Same as {@link AylaTimezone#create(Handler, AylaDevice, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService create(AylaDevice device) {
		return create(null, device, true);
	}

	/**
	 * Same as {@link AylaTimezone#create(Handler, AylaDevice, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService create(Handler mHandle, AylaDevice device) {
		return create(mHandle, device, false);
	}

	/**
	 * Posts the time zone associated with the device to the Ayla device Service.
	 * device.timeZone:
	 *     utc_offset (required): string which specifies utc offset. Format must be +HH:MM or -HH:MM. For example +05:00 or -03:00.
	 *     dst (optional): boolean which specifies if the location follows DST. Default: false.
	 *     dst_active (optional): booelan which specifies if DST is currently active. Default: false.
	 *     dst_next_change_date (optional): string which specifies next DST state change from active/inactive OR from inactive/active. Format must be yyyy-mm-dd
	 *     tz_id (optional): String identifier for the timezone. eg: "America/New_York".
	 *         NOTE: If tz_id is present, it must correlate with the utc_offset, else the POST will be rejected.
	 *
	 * @param mHandle is where result will be returned.
	 * @param device is the target object
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event
	 * @return AylaRestService object
	 */
	public AylaRestService create(Handler mHandle, AylaDevice device, Boolean delayExecution) {
		// POST apiv1/devices/<key>/time_zones.json
		Number devKey = device.getKey().intValue(); // Handle gson LazilyParsedNumber
		String url = String.format("%s%s%d%s", deviceServiceBaseURL(), "devices/", devKey, "/time_zones.json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.CREATE_TIMEZONE); 
		
		// {"time_zone": {"utc_offset": "-03:00",  "dst": true, "dst_active": true,  "dst_next_change_date": "2014-01-01"}}
		String jsonTimezone = AylaSystemUtils.gson.toJson(this, AylaTimezone.class);
		rs.setEntity(jsonTimezone);
		
		saveToLog("%s, %s, %s:%s, %s", "I", "Timezone", "url", url, "createTimezone");
		if (delayExecution == false) {
			rs.execute(); //Executes the request with the HTTP GET verb
		}
		return rs;
	}
	
	/**
	 * Update the timezone ID for this device.
	 * 
	 * @param device is the target object
	 *     tzId: (required): String identifier for the timezone. eg: "America/New_York".
	 *     NOTE: DST attributes are updated, based on whether the timezone has DST or not. 
	 * @return AylaRestService object
	 */
	public AylaRestService update(AylaDevice device) {
		return update(null, device, true);
	}
	public AylaRestService update(Handler mHandle, AylaDevice device) {
		return update(mHandle, device, false);
	}
	public AylaRestService update(Handler mHandle, AylaDevice device, Boolean delayExecution) {
		
		// apiv1/devices/<key>/time_zones.json
		Number devKey = device.getKey().intValue(); // Handle gson LazilyParsedNumber
		String url = String.format("%s%s%d%s", deviceServiceBaseURL(), "devices/", devKey, "/time_zones.json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.UPDATE_TIMEZONE); 
		
		// {"tz_id":"America/Los_Angeles"}
		String jsonTimezone = AylaSystemUtils.gson.toJson(this, AylaTimezone.class);
		rs.setEntity(jsonTimezone);

		saveToLog("%s, %s, %s:%s, %s", "I", "Timezone", "url", url, "updateTimezone");
		if (delayExecution == false) {
			rs.execute(); //Executes the request with the HTTP GET verb
		}
		
		return rs;
	}

	/**
	 * Same as {@link AylaTimezone#get(Handler, AylaDevice, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService get(AylaDevice device) {
		return get(null, device, true);
	}

	/**
	 * Same as {@link AylaTimezone#get(Handler, AylaDevice, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService get(Handler mHandle, AylaDevice device) {
		return get(mHandle, device, false);
	}

	/**
	 * Get the timezone information for this device.
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param device is the target object
	 * @param delayExecution delayExecution set to true to setup this call but have it execute on an external event
	 * @return AylaRestService object
	 */
	public AylaRestService get(Handler mHandle, AylaDevice device, Boolean delayExecution) {
		Number devKey = device.getKey().intValue(); // Handle gson LazilyParsedNumber

		// apiv1/devices/<key>/time_zones.json
		String url = String.format("%s%s%d%s", deviceServiceBaseURL(), "devices/", devKey, "/time_zones.json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.GET_TIMEZONE); 

		saveToLog("%s, %s, %s:%s, %s", "I", "Timezone", "url", url, "getTimezone");
		if (delayExecution == false) {
			rs.execute(); //Executes the request with the HTTP GET verb
		}
		return rs;
	}
	
	/**
	 * Removes timezone container ("time_zone:") from device service timezone API response (see AylaExecuteRequest.commit()).
	 * 
	 * @param jsonTimezoneContainer
	 * @return AylaTimezone in JSON format
	 * @throws Exception
	 */
	protected static String stripContainer(String jsonTimezoneContainer) throws Exception {
		String jsonTimezone = "";
		try {
			AylaTimezoneContainer timezoneContainer = AylaSystemUtils.gson.fromJson(jsonTimezoneContainer, AylaTimezoneContainer.class);
			AylaTimezone timezone = timezoneContainer.timeZone;

			jsonTimezone = AylaSystemUtils.gson.toJson(timezone, AylaTimezone.class);
			AylaSystemUtils.saveToLog("%s %s %s", "I", "Timezone", "stripContainer");
			return jsonTimezone;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", "Timezone", "jsonTimezoneContainer", jsonTimezoneContainer, "stripContainer");
			e.printStackTrace();
			throw e;
		}
	}
}

