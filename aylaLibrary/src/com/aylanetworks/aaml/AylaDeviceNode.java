//
//  AylaDeviceNode.java
//  Android_AylaLibrary
//
//  Created by Dan Myers on 7/28/14.
//  Copyright (c) 2014 AylaNetworks. All rights reserved.
//

package com.aylanetworks.aaml;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

import com.aylanetworks.aaml.models.AylaCryptoEncapData;
import com.google.gson.annotations.Expose;

import android.os.Handler;
import android.text.TextUtils;


//------------------------------------- AylaGateway --------------------------
class AylaDeviceNodeContainer {
	@Expose
	AylaDeviceNode device;
}

public class AylaDeviceNode extends AylaDevice  {

	
	private static final String tag = AylaDeviceNode.class.getSimpleName();
	
	// node properties retrievable from the service
	@Deprecated
	public String nodeType;					// @Deprecated
	@Expose
	public String nodeDsn;					// The DSN for this node
	@Expose
	public String action;					//  The action last action attempted
	@Expose
	public String status;					// The status of the last action attempted
	@Expose
	public String errorCode;				// The error code of the last action attempted
	@Expose
	public String ackedAt;					// When the latest action was acknowledged  by an owner gateway
	@Expose
	public String gatewayDsn;				// Owner gateway DSN
	@Expose
	public String address;					// Node address

	@Expose
	public AylaDeviceGateway gateway;		// The current gateway that owns this node
	
	// derived property
	@Expose
	public AylaDeviceGateway[] gateways;	// The gateways that owns this node for developer usage
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");

		result.append(this.getClass().getSimpleName() + " Object {" + NEW_LINE);
		result.append(" productName: " + productName + NEW_LINE);
		result.append(" model: " + model + NEW_LINE);
		result.append(" dsn: " + dsn + NEW_LINE );
		result.append(" oemModel: " + oemModel + NEW_LINE);
		result.append(" connectedAt: " + connectedAt + NEW_LINE);
		result.append(" mac: " + mac + NEW_LINE);
		result.append(" lanIp: " + lanIp + NEW_LINE);
		result.append(" templateId: " + templateId + NEW_LINE);
		result.append(" registrationType: " + registrationType + NEW_LINE);
		result.append(" setupToken: " + setupToken + NEW_LINE );
		result.append(" registrationToken: " + registrationToken + NEW_LINE);
		result.append(" nodeType: " + nodeType + NEW_LINE);
		result.append(" nodeDsn: " + nodeDsn + NEW_LINE);
		result.append(" action: " + action + NEW_LINE);
		result.append(" status: " + status + NEW_LINE);
		result.append(" errorCode: " + errorCode + NEW_LINE);
		result.append(" ackedAt: " + ackedAt + NEW_LINE);
		result.append(" gatewayDsn: " + gatewayDsn + NEW_LINE);
		result.append("}");
		return result.toString();
	}

	@Override
	public boolean isNode() {
		return true;
	}

	public static String kAylaNodeParamIdentifyValue = "value";	
	public static String kAylaNodeParamIdentifyTime = "time";	
	public static String kAylaNodeParamIdentifyOn = "On";	
	public static String kAylaNodeParamIdentifyOff = "Off";
	public static String kAylaNodeParamIdentifyResult = "Result";
	
	// --------------------- Properties pass-through methods -----------------------------------------

		/**
		 * Same as {@link AylaDeviceNode#getProperties(Handler, Boolean)} with no handler to return results and no option to setup the call to execute later.
		 * **/
		public AylaRestService getProperties() {	// sync, get all properties
			return AylaDeviceNode.getProperties(null, this, null, true);
		}

		/**
		 * Same as {@link AylaDeviceNode#getProperties(Handler, Map, Boolean)} with no handler to return results and no option to setup the call to execute later.
		 * **/
		public AylaRestService getProperties(Map<String, String> callParams) {	// sync, get some properties
			return AylaDeviceNode.getProperties(null, this, callParams, true);
		}

		/**
		 * Same as {@link AylaDeviceNode#getProperties(Handler, Boolean)} with no option to setup the call to execute later.
		 * **/
		public AylaRestService getProperties(Handler mHandle) {	// async, get all properties
			AylaRestService rs = AylaDeviceNode.getProperties(mHandle, this, null, false); 
			return rs;
		}

		/**
		 * Same as {@link AylaDeviceNode#getProperties(Handler, Map, Boolean)} with no option to setup the call to execute later.
		 * **/
		public AylaRestService getProperties(Handler mHandle, Map<String, String> callParams) {	// async get some properties async
			AylaRestService rs = AylaDeviceNode.getProperties(mHandle, this, callParams, false); 
			return rs;
		}

		/**
		 * Get all properties summary objects associated with the device from Ayla device Service. Use getProperties when ordering is not important.
		 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
		 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event
		 * @return AylaRestService object
		 */
		public AylaRestService getProperties(Handler mHandle, Boolean delayExecution) {	// async get all properties
			AylaRestService rs = AylaDeviceNode.getProperties(mHandle, this, null, delayExecution); 
			return rs;
		}

		/**
		 * Get properties summary objects associated with the device from Ayla device Service for properties specified in callParams. Use getProperties when ordering is not important.
		 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
		 * @param callParams call parameters
		 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event
		 * @return AylaRestService object
		 */
		public AylaRestService getProperties(Handler mHandle, Map<String, String> callParams, Boolean delayExecution) {	// async get some properties
			AylaRestService rs = AylaDeviceNode.getProperties(mHandle, this, callParams, delayExecution); 
			return rs;
		}
		
		//TODO: should not have this static function, should not be public. 
		public static AylaRestService getProperties(Handler mHandle, AylaDeviceNode dev, Map<String, String> callParams, Boolean delayExecution) {	// async get some properties
			
			ArrayList<String> nPropertyNames = new ArrayList<String>();
			ArrayList<String> readFromCachePropertyNames = new ArrayList<String>();
			Boolean hasListChanges = true;
			
			// Assuming device is never null.    
			AylaDeviceNode lanNode = (AylaDeviceNode)dev.getCopyInDeviceManager();
			if(lanNode.properties == null) {
				lanNode.initPropertiesFromCache();
			} 
			
			lanNode.initPropertiesOwner();
			
			if(lanNode.isLanModeActive()) {
				
				if ( lanNode.properties==null || lanNode.properties.length == 0) {
					lanNode.mergeNewProperties(lanNode.properties);
					
					/*
					 * TODO: fix the hasListChanges logic.  
					 * false is default value, it always works but add unnecessary workload sometimes.  
					 * 
					 * To put it simply, for a set of properties, if there is anyone not get true from 
					 * the isLanModeEnabledProperty(pn), then this is true.
					 * */
					hasListChanges = false;
				}
				
				if (lanNode.properties != null) {
					String namesWithSpace = callParams!=null? callParams.get("names"): null;
					String[] propertyNames = null;
					if(namesWithSpace != null) {
						propertyNames = namesWithSpace.split(" ");
						for(String propertyName : propertyNames) {
							if(lanNode.findProperty(propertyName) == null){
								hasListChanges = false;
								break;
							}
							if(lanNode.isLanModeEnabledProperty(propertyName)){
								nPropertyNames.add(propertyName);
							}
							else {
								readFromCachePropertyNames.add(propertyName);
							}
						}
					}
					else {
						for(AylaProperty property : lanNode.properties) {
							if(lanNode.isLanModeEnabledProperty(property.name)){
								nPropertyNames.add(property.name);
							}
							else {
								readFromCachePropertyNames.add(property.name);
							}
						}
					}
				}				
			}
			else {
				hasListChanges = false;
			}
			     
			if(hasListChanges) {
				//lan mode is active, and could be fetched through lan mode
				String[] names = nPropertyNames.toArray(new String[nPropertyNames.size()]);
				Boolean isFirst = true;
				String namesInString = "";
				for(String propertyName : names) {
					if(isFirst) {
						namesInString = propertyName;
						isFirst = false;
					}
					else {
						namesInString += " ";
						namesInString += propertyName;
					}
				}
				if(callParams == null) {callParams = new HashMap<String, String>();}
				callParams.put("names", namesInString);
			}
			
			//TODO: in lan mode, only return lan mode enabled properties. Need change
			AylaRestService rs = AylaProperty.getProperties(mHandle, lanNode, callParams, delayExecution); 
			return rs;
		}

	/**
	 * Same as {@link AylaDeviceNode#getNodes(Handler, AylaDevice, Map, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	@Deprecated
	public static AylaRestService getNodes(AylaDevice device,
			Map<String, String> callParams) {
		return getNodes(null, device, callParams, true);
	}

	/**
	 * Same as {@link AylaDeviceNode#getNodes(Handler, AylaDevice, Map, Boolean)} with no option to setup the call to execute later.
	 * **/
	@Deprecated
	public static AylaRestService getNodes(Handler mHandle, AylaDevice device,
			Map<String, String> callParams) {
		return getNodes(mHandle, device, callParams, false);
	}

	/**
	 * These class methods get one or more gateway nodes from the Ayla Cloud
	 * Service. If the application has been LAN Mode enabled, the nodes are read
	 * from cache, rather than the Ayla field service.
	 *
	 * @deprecated from v4.3.00, replaced by
	 *             {@link AylaDeviceGateway#getNodes(Map)}
	 *
	 * @param mHandle is where async results are returned. If null,
	 *            restServiceObject.execute() provides synchronous results
	 * @param device is the owner gateway device of the nodes
	 * @param callParams call parameters
	 * @param delayExecution could be set to true if you want setup this call but have it
	 *            execute on an external event
	 * @return AylaRestService object
	 */

	@Deprecated
	public static AylaRestService getNodes(Handler mHandle, AylaDevice device, Map<String, String> callParams, Boolean delayExecution) {
		Number devKey = device.getKey().intValue();
		AylaRestService rs = null;
		String savedJsonNodeContainers = "";

		// read the nodes from storage, returns "" if no cached values
		savedJsonNodeContainers = AylaCache.get(AML_CACHE_NODE, device.dsn);
		

		// get nodes from the service
		if ( AylaReachability.isCloudServiceAvailable()) {
			if (AylaSystemUtils.slowConnection == AylaNetworks.YES) {
				if(!TextUtils.isEmpty(savedJsonNodeContainers)) {
					// Use cached values
					try {
						String jsonNodes = stripContainers(savedJsonNodeContainers, null);
						rs = new AylaRestService(mHandle, "GetNodesStorageLanMode", AylaRestService.GET_NODES_LOCAL_CACHE);
						AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaNodes", "fromStorage", "true", "getNodesStorage");
						returnToMainActivity(rs, jsonNodes, 203, 0, delayExecution);
					} catch (Exception e) {
						AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", "AylaNodes", "exception", e.getCause(), "getNodesStorage_lanMode");
						e.printStackTrace();
					}			
				} else {
					// query field service
					// String url = "http://ads-dev.aylanetworks.com/apiv1/devices/<key>/nodes.json";
					String url = String.format("%s%s%s%s", AylaSystemUtils.deviceServiceBaseURL(), "devices/", devKey, "/nodes.json");
					rs = new AylaRestService(mHandle, url, AylaRestService.GET_REGISTERED_NODES, device.dsn);
					String delayedStr = (delayExecution == true) ? "true" : "false";
					saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaNodes", "url", url, "delayedExecution", delayedStr, "getNodesService");
					if (delayExecution == false) {
						rs.execute(); 
					}
				}
			} else {
				// query field service
				// String url = "http://ads-dev.aylanetworks.com/apiv1/devices/<key>/nodes.json";
				String url = String.format("%s%s%s%s", AylaSystemUtils.deviceServiceBaseURL(), "devices/", devKey, "/nodes.json");
				rs = new AylaRestService(mHandle, url, AylaRestService.GET_REGISTERED_NODES, device.dsn);
				String delayedStr = (delayExecution == true) ? "true" : "false";
				saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaNodes", "url", url, "delayedExecution", delayedStr, "getNodesService");
				if (delayExecution == false) {
					rs.execute(); 
				}
			}
		}

		// service is not reachable
		else 
			if (AylaReachability.isWiFiConnected(null) && !TextUtils.isEmpty(savedJsonNodeContainers))
			{
				// use cached values
				try {
					String jsonNodes = stripContainers(savedJsonNodeContainers, null);

					rs = new AylaRestService(mHandle, "GetNodesStorageLanMode", AylaRestService.GET_NODES_LOCAL_CACHE);
					saveToLog("%s, %s, %s:%s, %s", "I", "AylaNodes", "fromStorage", "true", "getNodesStorage");
					returnToMainActivity(rs, jsonNodes, 203, 0, delayExecution);
				} catch (Exception e) {
					AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", "AylaNodes", "exception", e.getCause(), "getNodesStorage_lanMode");
					e.printStackTrace();
				}
			}

			// no nodes
			else { 
				if (AylaCache.cacheEnabled(AML_CACHE_NODE) == true && !TextUtils.isEmpty(savedJsonNodeContainers)) {
					// use cached values
					try {
						String jsonNodes = stripContainers(savedJsonNodeContainers, null);

						rs = new AylaRestService(mHandle, "GetNodesStorageLanMode", AylaRestService.GET_NODES_LOCAL_CACHE);
						saveToLog("%s, %s, %s:%s, %s", "I", "AylaNodes", "fromStorage", "true", "getNodesStorage");
						returnToMainActivity(rs, jsonNodes, 203, 0, delayExecution);
					} catch (Exception e) {
						AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", "AylaNodes", "exception", e.getCause(), "getNodesStorage_lanMode");
						e.printStackTrace();
					}
				} else {
					rs = new AylaRestService(mHandle, "GetNodesStorageLanMode", AylaRestService.GET_NODES_LOCAL_CACHE);
					saveToLog("%s, %s, %s:%s, %s", "I", "AylaNodes", "nodes", "null", "getNodes_notFound");
					returnToMainActivity(rs, null, 404, 0, delayExecution);
				}
			}
		return rs;
	}
	
	public static String stripContainers(String jsonNodeContainers, AylaRestService rs) throws Exception {
		int count = 0;
		String jsonNodes = "";
		AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "D", tag, "stripContainers", "jsonNodeContainers:" + jsonNodeContainers);
		try {
			AylaDeviceContainer[] deviceContainers = AylaSystemUtils.gson.fromJson(jsonNodeContainers, AylaDeviceContainer[].class);
			AylaDeviceNode[] nodes = new AylaDeviceNode[deviceContainers.length];
			for (AylaDeviceContainer deviceContainer : deviceContainers) {
				nodes[count] = (AylaDeviceNode)deviceContainer.device;
				nodes[count].initPropertiesOwner();
				count++;
			}

			jsonNodes = AylaSystemUtils.gson.toJson(nodes,AylaDevice[].class);
			
			// TODO: dangerous, a function should follow the specification or function name strictly, and nothing more. 
			//Use rs to check if this is from buffer or from cloud. If from buffer, skip udpate 
			if (rs != null && count > 0) {
                AylaDeviceGateway gateway = (AylaDeviceGateway)AylaDeviceManager.sharedManager().deviceWithDSN(rs.info);
                if ( gateway != null ) {
                    gateway.updateNodes(nodes, jsonNodeContainers);
                }
			}
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "Nodes", "count", count, "stripContainers");

			return jsonNodes;

		} catch (Exception e) {
			if (jsonNodeContainers == null) {
				jsonNodeContainers = "null";
			}
			AylaSystemUtils.saveToLog("%s %s %s:%s %s:%s %s", "E", "Nodes", "count", count, "jsonNodeContainers", jsonNodeContainers, "stripContainers");
			e.printStackTrace();
			throw e;
		}
	}
	
	/**
	 * Used to identify a node by blinking a light, making a sound, vibrating, etc
	 * 
	 * callParams
	 *   {"value":"On","time":"55"}
	 *       The key "value" may have a corresponding value of "On" or "Off"
	 *       The key "time" has a corresponding value from 0 to 255 in seconds, passed as a string
	 *   {"value":"Results"}
	 *   	The key "value" has a corresponding value of "Results"
	 *       
	 * Return
	 * 		AylaRestService for this call
	 * 
	 * Results
	 *     {"id":"12345"} for the "On" and "Off" option, or 
	 *     {"id":"on_0x123456789abc","status":"success"} for the "Result" option
	 *     
	 * Errors
	 * 		401 - Unauthorized
	 * 		404 - Node not found
	 * 		405 - Not supported for this node
	 */
	// The identify() method is not directly supported for generic nodes
	public AylaRestService identify(Map<String, String> callParams) {
		return identify(null, callParams);
	}
	public AylaRestService identify(Handler mHandle, Map<String, String> callParams) {
		AylaRestService rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.IDENTIFY_NODE);
		saveToLog("%s, %s, %s, %s.", "W", tag, "identify()", "method not allowed");
		returnToMainActivity(rs, "{\"error\":\"identify() not allowed for generic node.\"}", 405,
				AylaRestService.IDENTIFY_NODE, false);
		return rs;
	}

	@Override
	public AylaLanModule getLanModule() {
		AylaDevice gateway = AylaDeviceManager.sharedManager().deviceWithDSN(gatewayDsn);
		if ( gateway != null ) {
			return gateway.getLanModule();
		}
		return null;
	}

	@Override
	public boolean isLanModeActive() {
		AylaDevice gateway = AylaDeviceManager.sharedManager().deviceWithDSN(gatewayDsn);
		if ( gateway != null ) {
			return gateway.isLanModeActive();
		}
		return false;
	}

	@Override
	public void lanModeEnable() {
		AylaDevice gateway = AylaDeviceManager.sharedManager().deviceWithDSN(gatewayDsn);
		if ( gateway != null && !gateway.isLanModeActive() ) {
			gateway.lanModeEnable();
		}
	}

	@Override
	protected String lanModeToDeviceUpdate(AylaRestService rs, AylaProperty property, String value, int cmdId) {
		
		return AylaTranslate.zCmdToNode(this, property, value, cmdId+"");
	}
	
	@Override	
	protected String lanModeToDeviceCmd(final AylaRestService rs, final String type, final String uri, final Object obj) {
		String cmd = "";
		if ( TextUtils.equals(type, "GET") && TextUtils.equals(uri, "datapoint.json") ) 
		{ // get datapoint in lan mode.
			AylaProperty property = (AylaProperty)obj;
			if ( property.datapoint == null ) {
				property.datapoint = new AylaDatapoint();
			}
			cmd = property.datapoint.getDatapointFromLanModeDevice( rs, property, true );                    
		}
		return cmd;
	}
	
	
	
	/**
	 * Based on messages from module, either notifications or requests feedbacks, update corresponding attributes. 
	 * 
	 * @param rs null for notifications, callback for requests.
	 * @param status from module, -1 for notification, non-negative result code for request.
	 * @param data pay load in a decrypted result, requests in different types would have different valid fields.  
	 * 
	 * @return status code.  200 if everything is good; 400 indicates this is a failure response; 
	 * 404 if no property matches, 405 if request type not implemented. More TBD. 
	 * */  // NOTE: batch_datapoint is implemented on generic devices for now, which makes this function different from that in AylaDevice or AylaDeviceZigbeeX.
	@Override
	protected int lanModeUpdateProperty(final AylaRestService rs, final int status, final AylaCryptoEncapData data) {
		AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "I", tag, "lanModeUpdateProperty", "status:" + status + "\nrs:" + rs + "\ndata:" + data);
		
		if (rs == null) { 
			// for notifications 
			String response = "{\"type\":\"" + AML_NOTIFY_TYPE_PROPERTY + "\",\"dsn\":\"" + this.dsn +
					  "\",\"properties\":[\"" + data.name + "\"]}";
			int propertyUpdateStatus = this.updateProperty(data, true);
			AylaNotify.returnToMainActivity(null, response, propertyUpdateStatus, 0, this);
			return propertyUpdateStatus;
		} 
		
		List<String> result = new ArrayList<String>();
		if (!TextUtils.isEmpty(data.name)) {
			result.add(data.name);
		}
		

		// for request results
		int updateStatus = 200;
		if ( (status >=400 && status<500) 
				|| status == 503)  // Special case for insufficient resource on MCU.
		{
			/*
			 * Based on observation it could be 400, 404, 408 and 503. Pretty tricky here
			 * It might cover other cases that should not be coped with in this way.
			 * */
			if (rs.collectResult(result) == -3) {
				// All cmds for this request are collected, get back to app.
				AylaProperty[] aps = this.getCollectiveResult( rs.getCollectiveResult() );
				if (aps == null || aps.length<1) {
					updateStatus = 400;
				} else {
					if (!rs.isRequestComplete()) {
						updateStatus = 206;
					}
				}
				if (rs.RequestType == AylaRestService.GET_NODE_PROPERTIES_LANMODE) {
					String jsonProperties = "empty response";
					if (aps!=null && aps.length>=1) {
						jsonProperties = AylaSystemUtils.gson.toJson(aps, AylaProperty[].class);
					}
					AylaProperty.returnToMainActivity(rs, jsonProperties, updateStatus, 0, false);
				} else if (rs.RequestType == AylaRestService.GET_NODE_DATAPOINT_LANMODE 
						|| rs.RequestType == AylaRestService.CREATE_NODE_DATAPOINT_LANMODE) {
					String jsonDatapoint = "empty response";
					if (aps!=null && aps.length>=1) {
						jsonDatapoint = "[" + AylaSystemUtils.gson.toJson(aps[0].datapoint, AylaDatapoint.class) + "]";
					}
					if (rs.RequestType == AylaRestService.GET_NODE_DATAPOINT_LANMODE) {
						jsonDatapoint = "[" + jsonDatapoint + "]";
					}
					AylaDatapoint.returnToMainActivity(rs, jsonDatapoint, updateStatus, 0);
				} else {
					saveToLog("%s, %s, %s, %s.", "E", tag, "lanModeUpdateProperty", "requestType " + rs.RequestType + " not found");
					updateStatus = 405;
				}
			}
			return updateStatus;
		}
		
		// Assuming status only has 200+ and 400+ here.
		updateStatus = this.updateProperty(data, false);
		
		if ( !TextUtils.isEmpty(data.name) && rs.collectResult(result) == 1) {
			// All cmds for this request are collected, get back to app.
			if (!rs.isRequestComplete()) {
				updateStatus = 206;
			}
			AylaProperty[] aps = this.getCollectiveResult( rs.getCollectiveResult() );
			if (rs.RequestType == AylaRestService.GET_NODE_PROPERTIES_LANMODE) {
				String jsonProperties = AylaSystemUtils.gson.toJson(aps, AylaProperty[].class);
				AylaProperty.returnToMainActivity(rs, jsonProperties, updateStatus, 0, false);
			} else if (rs.RequestType == AylaRestService.GET_NODE_DATAPOINT_LANMODE 
					|| rs.RequestType == AylaRestService.CREATE_NODE_DATAPOINT_LANMODE) {
				String jsonDatapoint = AylaSystemUtils.gson.toJson(aps[0].datapoint, AylaDatapoint.class) ;
				if (rs.RequestType == AylaRestService.GET_NODE_DATAPOINT_LANMODE) {
					jsonDatapoint = "[" + jsonDatapoint + "]";
				}
				AylaDatapoint.returnToMainActivity(rs, jsonDatapoint, updateStatus, 0);
			} else {
				saveToLog("%s, %s, %s, %s.", "E", tag, "lanModeUpdateProperty", "requestType " + rs.RequestType + " not found");
				updateStatus = 405;
			}
		}
		return updateStatus;
	}// end of lanModeUpdateProperty  
	
	

// ------------------------------- Node Helper Methods ----------------------

	protected boolean isLanModeEnabledProperty(final String propertyName) {
		
		if (TextUtils.isEmpty(propertyName)) {
			return false;
		}
		return true; 
	}
	
	
}// end of AylaDeviceNode class     


	


