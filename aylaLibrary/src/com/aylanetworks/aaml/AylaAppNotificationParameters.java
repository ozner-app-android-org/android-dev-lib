//
//  AppNotificationParameters.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 06/12/2014.
//  Copyright (c) 2014 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import com.google.gson.annotations.Expose;

/**
* For each notification app type the parameters are added to the notificationAppParameters class. 
* The appTypes and the accepted notificaitonAppParameters for each are:
* 
* "email" -  "username", "email", "message"
* "sms" -  "username", "country_code", "phone_number", "message"
* "push_ios" : "registration_id", "application_id", "message", "push_sound", "push_mdata" (for completeness only in Android)
* "push_android" : "registration_id", "message", "push_sound", "push_mdata"
* "push_baidu" : "app_id", "channel_id", "message", "message_title", "push_sound", "push_mdata"
*
* Param details:
*     message: for iOS and Android push apps max length is 100 chars
*     push_sound: Used for iOS and Android push apps (max length 50 chars)
*         Contains the sound file name present on mobile app eg: my_sound_file.mp3
*     push_mdata: Used for iOS and Android push apps (max length 100 chars)
*         Contains comma separated data needed by app eg: {"key1"=>"value1", "key2"=>"value2"}
*         
*  See AylaAppNotificaion methods for usage
*/
@Deprecated
public class AylaAppNotificationParameters {
	
	// email & sms
	@Expose
	public String username;
	@Expose
	public String message;
	
	@Expose
	public String contactId;
	
	// email
	@Expose
	public String email;
	
	// SMS
	@Expose
	public String countryCode;
	@Expose
	public String phoneNumber;
	
	// Push Notification 
	@Expose
	public String registrationId;
	@Expose
	public String applicationId;
	@Expose
	public String pushSound;
	@Expose
	public String pushMdata;

    @Expose
    public String channelId;
	// Custom email Template
	@Expose
	public String emailTemplateId;
	@Expose
	public String emailSubject;
	@Expose
	public String emailBodyHtml;
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");

		result.append(this.getClass().getName() + " Object {" + NEW_LINE);
		result.append(" username: " + username + NEW_LINE);
		result.append(" message: " + message + NEW_LINE);
		result.append(" contactId: " + contactId + NEW_LINE);
		result.append(" email: " + email + NEW_LINE);
		result.append(" countryCode: " + countryCode + NEW_LINE);
		result.append(" phoneNumber: " + phoneNumber + NEW_LINE);
		result.append(" registrationId: " + registrationId + NEW_LINE);
		result.append(" applicationId: " + applicationId + NEW_LINE);
		result.append(" pushSound: " + pushSound + NEW_LINE);
		result.append(" pushMdata: " + pushMdata + NEW_LINE);
		result.append(" emailTemplateId: " + emailTemplateId + NEW_LINE);
		result.append(" emailSubject: " + emailSubject + NEW_LINE);
		result.append(" emailBodyHtml: " + emailBodyHtml + NEW_LINE);

		result.append("}");
		return result.toString();
	}
}






