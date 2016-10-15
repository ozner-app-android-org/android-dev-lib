/*
 * IAML_SECURITY_KEY_SIZE.java
 * Ayla Mobile Library
 * 
 * Created by Di Wang on 05/20/2015
 * Copyright (c) 2015 Ayla Networks. All Rights Reserved.
 * */

package com.aylanetworks.aaml.enums;

public enum IAML_SECURITY_KEY_SIZE {
	IAML_SECURITY_KEY_SIZE_1024(1024)
//	, IAML_SECURITY_KEY_SIZE_1536(1536)
	, IAML_SECURITY_KEY_SIZE_2048(2048)
	;
	
	private int value;
	private IAML_SECURITY_KEY_SIZE(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
}// end of IAML_SECURITY_KEY_SIZE           






