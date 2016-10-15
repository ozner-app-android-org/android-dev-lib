//
//  AylaWiFiStatus.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 10/23/12
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import com.google.gson.annotations.Expose;


/**
 * 
 * Modal class representing a wifiStatus object.
 * 
 * */
public class AylaWiFiStatus {
	@Expose
	public AylaWiFiConnectHistory [] connectHistory;
	@Expose
	public String dns;
	@Expose
	public String deviceService;
	@Expose
	public String mac;
	@Expose
	public int ant;
	@Expose
	public int rssi;
	@Expose
	public int bars;
}







