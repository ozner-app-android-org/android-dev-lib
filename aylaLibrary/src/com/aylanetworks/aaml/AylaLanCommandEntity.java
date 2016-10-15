
/*
 * AylaLanCommandEntity.java
 * Ayla Mobile Library
 * 
 * Created by Di Wang on 02/27/2015
 * Copyright (c) 2015 Ayla Networks. All Rights Reserved.
 * */




package com.aylanetworks.aaml;

import com.aylanetworks.aaml.enums.CommandEntityBaseType;


/**
 * Model class representing a command sent in lan mode, which maps to a command ID.
 * */
/*
 * Note: public due to access in zigbee package. 
 * To inherit a new one AylaZigbeeLanCommandEntity in 
 * zigbee package and hide this inside core package.
 * */
public class AylaLanCommandEntity {
	public String jsonStr;
	public int cmdId;
	public CommandEntityBaseType baseType;

	/* For zigbee package */
	public String dsn;
	String propertyName;
	
	public AylaLanCommandEntity(){
		jsonStr = "";
		cmdId = 0;
		baseType = CommandEntityBaseType.AYLA_LAN_COMMAND;
		
		dsn = "";
		propertyName = "";
	}
	
	public AylaLanCommandEntity(String json, int id, CommandEntityBaseType type) {
		jsonStr = json;
		cmdId = id;
		baseType = type;
	}
}// end of AylaLanCommandentity class   




