//
//  AylaDeviceNode.java
//  Android_AylaLibrary
//
//  Created by Dan Myers on 7/28/14.
//  Copyright (c) 2014 AylaNetworks. All rights reserved.
//

package com.aylanetworks.aaml; 

import com.google.gson.annotations.Expose;

//------------------------------------- AylaGateway --------------------------
class AylaDeviceWifiContainer {
	@Expose
	AylaDeviceWifi device;
}

public class AylaDeviceWifi extends AylaDevice  {

	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");

		result.append(this.getClass().getName() + " Object {" + NEW_LINE);
		result.append(" productName: " + productName + NEW_LINE);
		result.append(" model: " + model + NEW_LINE);
		result.append(" dsn: " + dsn + NEW_LINE );
		result.append(" oemModel: " + oemModel + NEW_LINE);
		result.append(" connectedAt: " + connectedAt + NEW_LINE);
		result.append(" mac: " + mac + NEW_LINE);
		result.append(" lanIp: " + lanIp + NEW_LINE);
		result.append(" templateId: " + templateId + NEW_LINE);
		result.append(" registrationType" + registrationType + NEW_LINE);
		result.append(" setupToken" + setupToken + NEW_LINE );
		result.append(" registrationToken " + registrationToken + NEW_LINE);
		result.append("}");
		return result.toString();
	}
}
	
