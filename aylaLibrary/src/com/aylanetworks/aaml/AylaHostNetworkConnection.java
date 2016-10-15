//
//  AylaHostNetworkConnection.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 10/23/12
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import com.google.gson.annotations.Expose;

public class AylaHostNetworkConnection {
	@Expose
	public String state;
	@Expose
	public String ssid;
	@Expose
	public int netId;
	@Expose
	public Boolean hidden;
	@Expose
	public int rssi;
	@Expose
	public Boolean success;

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");
		result.append(this.getClass().getName() + " Object {" + NEW_LINE);
		result.append(" state: " + state + NEW_LINE);
		result.append(" ssid: " + ssid + NEW_LINE);
		result.append(" netId: " + netId + NEW_LINE);
		result.append(" hidden: " + hidden + NEW_LINE );
		result.append(" rssi: " + rssi + NEW_LINE);
		result.append(" success: " + success + NEW_LINE);
		result.append("}");
		System.out.println(result);
		return result.toString();
	}
}