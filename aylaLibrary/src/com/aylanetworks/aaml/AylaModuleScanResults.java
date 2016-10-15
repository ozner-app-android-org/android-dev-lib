//
//  AylaModuleScanResults.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 10/23/12
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import com.google.gson.annotations.Expose;

public class AylaModuleScanResults {
	@Expose
	public String ssid = null;
	@Expose
	public String type = null;
	@Expose
	public int chan = 0;
	@Expose
	public int signal = 0;
	@Expose
	public int bars = 0;
	@Expose
	public String security = null;
	@Expose
	public String bssid = null;

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");
		result.append(this.getClass().getName() + " Object {" + NEW_LINE);
		result.append(" ssid: " + ssid + NEW_LINE);
		result.append(" type: " + type + NEW_LINE);
		result.append(" chan: " + chan + NEW_LINE);
		result.append(" signal: " + signal + NEW_LINE);
		result.append(" bars: " + bars + NEW_LINE);
		result.append(" security: " + security + NEW_LINE);
		result.append(" bssid: " + bssid + NEW_LINE);
		result.append("}");
		System.out.println(result);
		return result.toString();
	}
}