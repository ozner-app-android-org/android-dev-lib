//
//  AppShareOwnerProfile.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 07/18/2014.
//  Copyright (c) 2014 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import com.google.gson.annotations.Expose;

/**
 * 
 * Model class representing an owner of a shared resource. 
 * 
 * */
public class AylaShareOwnerProfile {
	@Expose
	public String firstname;
	@Expose
	public String lastname;
	@Expose
	public String email;

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");

		result.append(this.getClass().getName() + " Object {" + NEW_LINE);
		result.append(" firstname: " + firstname + NEW_LINE);
		result.append(" lastname: " + lastname + NEW_LINE);
		result.append(" email: " + email + NEW_LINE);
		result.append("}");
		return result.toString();
	}
}



