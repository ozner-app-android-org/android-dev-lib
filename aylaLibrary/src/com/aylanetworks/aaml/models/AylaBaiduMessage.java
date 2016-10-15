/*
 * MCABaiduMessage.java
 * Ayla android Mobile Control Application
 *
 * Created by Di Wang on 06/02/2015
 * Copyright (c) 2015 Ayla Networks. All Rights Reserved.
 * */


package com.aylanetworks.aaml.models;

import com.google.gson.annotations.Expose;


/**
 * Model class for Baidu Message object.
 * Data model exposed to app level, for Gson parsing. 
 **/
public class AylaBaiduMessage {

	
	/* User facing plain text*/
	@Expose
	public String msg;
	
	/* Audio file name, same as that in push_android/sound */
	@Expose
	public String sound;
	
	/* Meta data in json, for future scalability*/
	@Expose
	public String data;
	
	/* Same as messageType in GCM, for future scalability.   
	 * Ranging from 0 to 99, 0 for message, others for some 
	 * other cmds
	 * */
	@Expose
	public int msgType;
	
	
}// end of MCABaidyMessage class      






