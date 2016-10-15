//
//  AylaExecuteResponse.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 10/1/13.
//  Copyright (c) 2013 Ayla Networks. All rights reserved.
//
package com.aylanetworks.aaml;

import com.google.gson.annotations.Expose;

public class AylaExecuteResponse {
	
	@Expose
	private int what;
	@Expose
	private int arg1;
	@Expose
	private int arg2;
	@Expose
	private Object obj;
	
	public AylaExecuteResponse(int what, int arg1, int arg2, Object obj) {
		this.what = what;
		this.arg1 = arg1;
		this.arg2 = arg2;
		this.obj = obj;
	}
	
	public int getWhat() {
		return this.what;
	}
	
	public int getArg1() {
		return this.arg1;
	}
	
	public int getArg2() {
		return this.arg2;
	}
	
	public Object getObject() {
		return this.obj;
	}
}