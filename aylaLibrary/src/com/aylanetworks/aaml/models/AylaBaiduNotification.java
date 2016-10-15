
/*
 * MCABaiduNotification.java
 * Ayla android Mobile Control Application
 *
 * Created by Di Wang on 06/02/2015
 * Copyright (c) 2015 Ayla Networks. All Rights Reserved.
 * */



package com.aylanetworks.aaml.models;

import com.google.gson.annotations.Expose;




/**
 * Model class for Baidu Notification object.
 * Data model exposed to app level, for Gson parsing. 
 **/
public class AylaBaiduNotification {

	@Expose
	public String title;
	
	@Expose
	public String description; // message
}// end of MCABaiduNotification class         






