//
//  AylaWiFiConnectHistory.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 10/23/12
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import com.google.gson.annotations.Expose;

/**
 * 
 * Modal class representing a WifiConnectHistory object.
 * 
 * */
public class AylaWiFiConnectHistory {
	@Expose
	public String ssidInfo;
	@Expose
	public String bssid;
	@Expose
	public int error;
	@Expose
	public String msg;
	@Expose
	public int mtime;
	@Expose
	public String ipAddr;
	@Expose
	public String netmask;
	@Expose
	public String defaultRoute;
	@Expose
	public String dnsServers[];
}





