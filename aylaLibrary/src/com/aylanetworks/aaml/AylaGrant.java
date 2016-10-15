//
//  AylaGrant.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 6/20/14.
//  Copyright (c) 2014 Ayla Networks. All rights reserved.
//
package com.aylanetworks.aaml;

import com.google.gson.annotations.Expose;

public class AylaGrant {
	
	// "grant":{"user_id":1, "start_date_at":"2014-06-17T23:14:33Z", "end_date_at":null, "operation":"write", "role":"OEM::Ayla::User"}

	@Expose
	String userId;				// The target user id that created the original share. Returned with create/POST & update/PUT operations
	@Expose
	public String shareId;		// The unique share id associated with this grant
	
	@Expose
	public String operation;	// Access permissions allowed: either read or write. Used with create/POST & update/PUT operations. Ex: 'write', Optional
								// If omitted, the default access permitted is read only
	@Expose
	public String startDateAt;	// When this named resource will be shared. Used with create/POST & update/PUT operations. Ex: '2014-03-17 12:00:00', Optional
								// If omitted, the resource will be shared immediately. UTC DateTime value.
	@Expose
	public String endDateAt;	// When this named resource will stop being shared. Used with create/POST & update/PUT operations. Ex: '2020-03-17 12:00:00', Optional
								// If omitted, the resource will be shared until the share or named resource is deleted. UTC DateTime value
	
	@Expose
	public String role;
	
	// -------------------------- Support Methods ------------------------
	@Override
	public String toString() {
		return AylaSystemUtils.gson.toJson(this, AylaGrant.class);
	}
}
