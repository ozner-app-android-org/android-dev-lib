
/*
 * AylaBatchResponse.java
 * Ayla Mobile Library
 * 
 * Created by Di Wang on 10/15/2015
 * Copyright (c) 2014 Ayla Networks. All Rights Reserved.
 * */

package com.aylanetworks.aaml;

import com.google.gson.annotations.Expose;

public class AylaBatchResponse extends AylaSystemUtils {

	@Expose
	public String dsn;
	
	@Expose
	public String name;
	
	@Expose
	public int status;
	
	@Expose
	public AylaDatapoint datapoint;
	
}// end of AylaBatchResponse class   






