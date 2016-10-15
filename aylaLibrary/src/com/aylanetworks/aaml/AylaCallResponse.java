//
//  AylaCallResponse.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 10/1/13.
//  Copyright (c) 2013 Ayla Networks. All rights reserved.
//
package com.aylanetworks.aaml;

import android.os.Bundle;


public class AylaCallResponse {
	
	private int mResultCode;
	private Bundle mBundle;
	
	public AylaCallResponse(int resultCode) {
		this.mResultCode = resultCode;
		this.mBundle = null;
	}
	
	public AylaCallResponse(int resultCode, Bundle bundle) {
		this.mResultCode = resultCode;
		this.mBundle = bundle;
	}
	
	public int getResultCode() {
		return this.mResultCode;
	}
	
	public Bundle getBundle() {
		return this.mBundle;
	}
	
	public void setBundle(Bundle b) {
		mBundle = b;
	}
	
	public void setResultCode(int rc) {
		mResultCode = rc;
	}
}





