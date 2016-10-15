/*
 * AylaTranslate.java
 * Ayla Mobile Library
 * 
 * Created by Di Wang on 03/03/2015
 * Copyright (c) 2015 Ayla Networks. All Rights Reserved.
 * */


package com.aylanetworks.aaml;

/**
 * Translate utility for generic gateway/node. 
 * */
public class AylaTranslate {

	
	/**
	 * place holder for now, until I know the expected behavior for a generic gateway/node.
	 * */
	public static String zCmdToNode(
			AylaDeviceNode node
			, AylaProperty property
			, String valueString
			, String zCmdId) {
		// for generic solution, there are duplicate places to do the same thing.
		return valueString;
	}
	
	
	/**
	 * place holder for now, until I know the expected behavior for a generic gateway/node.
	 * */
	public static boolean isGatewayAttributeProperty(final String proeprtyName) {
		//TODO: not implemented yet.
		return false;
	}
	
	
	/**
	 * place holder for now, until I know the expected behavior for a generic gateway/node.
	 * */
	public static String[] updateNodeWithGWAttr(
			final AylaDeviceGateway gw
			, final String propertyName
			, final String value
			, Boolean toBeNotified) {
		return null;
	}
}// end of AylaTranslate class     





