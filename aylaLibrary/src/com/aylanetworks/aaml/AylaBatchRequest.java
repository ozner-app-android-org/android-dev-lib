
/*
 * AylaBatchRequest.java
 * Ayla Mobile Library
 * 
 * Created by Di Wang on 10/15/2015
 * Copyright (c) 2014 Ayla Networks. All Rights Reserved.
 * */


package com.aylanetworks.aaml;

import android.text.TextUtils;

import com.google.gson.annotations.Expose;


/**
 * Represents a datapoint in a batch datapoint request. For app it is a data model, all functions are hide inside lib. 
 * */
public class AylaBatchRequest extends AylaSystemUtils {

	private static final String tag = AylaBatchRequest.class.getSimpleName();
	
	
	AylaDatapoint datapoint;
	
	String dsn;
	
	String name; // property name.
	
	public AylaBatchRequest() {
		this.datapoint = new AylaDatapoint();
		this.dsn = "";
		this.name = "";
	}
	
	public AylaBatchRequest(final AylaDatapoint adp, final String dsn, final String pName) {
		this.datapoint = adp;
		this.dsn = dsn;
		this.name = pName;
	}
	
	/**
	 * Validate the object, and see if it is ok to send.  Validation metrics include:<br/>
	 * 1. Device specified by dsn belongs to the account, or can be found in AylaDeviceManager;<br/>
	 * 2. Property specified by name belongs to the device;<br/>
	 * 3. The base type of the property specified by name is supported for batch datapoint;<br/>
	 * 4. The datapoint value is align with the property base type.<br/>
	 * 
	 * @return true if it is valid to send.
	 * */
	boolean isValidRequest() {
		AylaDevice dev = AylaDeviceManager.sharedManager().deviceWithDSN(dsn);
		if (dev == null) {
			saveToLog("%s, %s, %s, %s.", "W", tag, "isValidRequest", "device " + dsn + " not found in AylaDeviceManager");
			return false;
		}
		
		AylaProperty ap = dev.findProperty(name);
		if (ap == null) {
			saveToLog("%s, %s, %s, %s.", "W", tag, "isValidRequest", "property " + name + " not found in device " + dsn);
			return false;
		}
		
		if ( !isSupportedBaseType(ap.baseType()) ) {         
			saveToLog("%s, %s, %s, %s.", "W", tag, "isValidRequest", "property " + ap.name() + " baseType " + ap.baseType() + " not supported");
			return false;
		}
		
		if ( !isValueAlignBaseType(ap.baseType()) ) {
			saveToLog("%s, %s, %s, %s.", "W", tag, "isValidRequest"
					, "property " + ap.name() + " baseType " + ap.baseType() + " not aligned with value " + datapoint.value());
			return false;
		}
		return true;
	}// end of isValidRequest             
	
	
	
	/**
	 * Validate property base type and datapoint value. No "file" or "stream" here as it particularly serves AylaBatchRequest.
	 * 
	 *  @param bt
	 *  @return true if they are aligned.
	 * */
	private boolean isValueAlignBaseType(final String bt) {
		if (TextUtils.isEmpty(bt)) {
			saveToLog("%s, %s, %s, %s!", "E", tag, "isValueAlignBaseType", "bast type empty");
			return false;
		}
		
		if ( this.datapoint == null || TextUtils.isEmpty(this.datapoint.value()) ) {
			saveToLog("%s, %s, %s, %s.", "W", tag, "isValueAlignBaseType", "invalid datapoint");
			return false;
		}
		
		try {
			if ("integer".equalsIgnoreCase(bt) 
					|| "boolean".equalsIgnoreCase(bt)) {
				Integer.parseInt(this.datapoint.value());
			} else if ("decimal".equalsIgnoreCase(bt)
					|| "float".equalsIgnoreCase(bt)) {
				Float.parseFloat(this.datapoint.value());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}// end of isValueAlignBaseType               
	
	
	
	/**
	 * Check if the property base type is supported for batch datapoint.<br/> 
	 * NOTE that it is coincidence that the type not supported is the same as that of AylaBlob.
	 *
	 * @return true if the base type is supported
	 * */
	private boolean isSupportedBaseType(final String bt) {
		if (TextUtils.isEmpty(bt)) {
			saveToLog("%s, %s, %s, %s!", "E", tag, "isSupportedBaseType", "base type empty");
			return false;
		}
		
		if ( "file".equalsIgnoreCase(bt) || "stream".equalsIgnoreCase(bt) ) {
			return false;
		}
		return true;
	}// end of isSupportedBAseType        
	
	
	/**
	 * precondition: Everything is valid.
	 * 
	 * Reference the to device batch datapoint specification. <br/>
	 * The resulted json string should be ready to be appended in the entity directly.
	 * 
	 * @return json string that is aligned to the spec.
	 * */
	String toRequestString() {
		StringBuilder sb = new StringBuilder(128);         
		sb.append("{")
			.append("\"datapoint\":{")
			.append("\"value\":\"").append(this.datapoint.value()).append("\"")
			.append("},")
			.append("\"dsn\":\"").append(dsn).append("\",")
			.append("\"name\":\"").append(name).append("\"")
			.append("}");
		return sb.toString();
	}// end of toRequestString   
}// end of AylaBatchRequest class           






