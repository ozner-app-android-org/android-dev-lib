/*
 * AylaCommand.java
 * Ayla Mobile Library
 * 
 * Created by Di Wang on 03/03/2015
 * Copyright (c) 2015 Ayla Networks. All Rights Reserved.
 * */


package com.aylanetworks.aaml;

import org.json.JSONObject;

/**
 * Generic command for Gateway/node, place holder for now.
 * */
public class AylaCommand {

	//TODO: elaborate visibility later. 
	public String id;
	public String addr;
	public String value;
	public String valueString;
	public String valueType;
	public boolean isValid;
	
	public boolean isValid() {
		return isValid;
	}
	
	public static AylaCommand[] fromContainerToCommands(final String jsonCmdContainers) {
		//TODO: not implemented
		return null;
	}
	
	public static AylaCommand fromContainerToCommand(final String jsonCmdContainers) {
		//TODO: not implemented
		return null;
	}
	
	protected String toGatewayGetPropertyCmdData() {
		//TODO: not implemented.
		return "";
	}
	
	public static AylaCommand getAylaCommand(JSONObject object){
		//TODO: not implemented.
		return null;
	}
	
	public static AylaCommand getAylaCommand(AylaDeviceNode node
			, AylaProperty property
			, String valueString
			, String zId){
		
		return null;
	}
}// end of AylaCommand class 





