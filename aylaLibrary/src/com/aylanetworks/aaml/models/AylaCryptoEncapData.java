package com.aylanetworks.aaml.models;

import com.aylanetworks.aaml.AylaSystemUtils;
import com.google.gson.annotations.Expose;

/*
 * NOTE: make it public as zigbee package needs to access,
 * rework on new struct/model class and make necessary 
 * inheritance inside zigbee package if we need to remove 
 * this awkward visibility.  
 * */
public class AylaCryptoEncapData {
	
    @Expose
    public String name;
    @Expose
    public String value;
    @Expose
    public String baseType;
    @Expose
    public String dsn;
    
    // For ackEnabled Property
    @Expose
	public String id;
    @Expose
    public int ackStatus;
    @Expose
    public int ackMessage;
    
    // For Batch Datapoint
    @Expose
    public String devTimeMs;
    
    @Override
    public String toString() {
    	return AylaSystemUtils.gson.toJson(this, AylaCryptoEncapData.class);
    }
}







