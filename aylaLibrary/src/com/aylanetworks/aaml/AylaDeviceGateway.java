//
//  AylaDeviceGateway.java
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

import org.json.JSONArray;
import org.json.JSONObject;

import com.aylanetworks.aaml.enums.CommandEntityBaseType;
import com.aylanetworks.aaml.models.AylaCryptoEncapData;
import com.google.gson.annotations.Expose;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

//------------------------------------- AylaGateway --------------------------
class AylaDeviceGatewayContainer {
	@Expose
	AylaDeviceGateway gateway;
}

public class AylaDeviceGateway extends AylaDevice  {

	private final static String tag = AylaDeviceGateway.class.getSimpleName();
	
	// gateway properties retrievable from the service
	@Deprecated
	public String gatewayType;		// @deprecated
	@Expose
	public AylaDeviceNode node;		// current node;
	@Expose
	public AylaDeviceNode[] nodes;	// nodes associated with this gateway

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");

		result.append(this.getClass().getName() + " Object {" + NEW_LINE);
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
		result.append("}");
		return result.toString();
	}

    @Override
    public boolean isGateway() {
        return true;
    }
    

	// Node registration pass-through methods
	public AylaRestService openRegistrationJoinWindow(final Handler mHandle, Map<String, String> callParams) {
		return AylaRegistration.openRegistrationWindow(mHandle, this, callParams);
	}

	public AylaRestService getRegistrationCandidates(final Handler mHandle, Map<String, String> callParams) {
		
		return AylaRegistration.getCandidates(mHandle, this, callParams);
	}

	public AylaRestService registerCandidate(final Handler mHandle, AylaDeviceNode node) {
		return AylaRegistration.registerCandidate(mHandle, node);
	}

	public AylaRestService closeRegistrationJoinWindow(final Handler mHandle, Map<String, String> callParams) {
		return AylaRegistration.closeRegistrationWindow(mHandle, this);
	}


	// ---------------------- Helper Methods --------------------------------------------------

	//TODO: make sure no apps use this APi, and removed it. Now we have isGateawy() and isZigbee() as alternatives.
	@Deprecated
	public boolean isZigbeeGateway() {
		ClassLoader loader = AylaCommProxy.class.getClassLoader();
		Class zigbeeClass = null;
		try {
			zigbeeClass = loader.loadClass(AylaCommProxy.kAylaCommProxyZigbeeGW);
		} catch (Exception e) {
		}

		return zigbeeClass != null && zigbeeClass.isInstance(this);
	}

	// --------------------- Node pass-through methods -----------------------------------------
	/**
	 * Same as {@link AylaDeviceGateway#getNodes(Handler, Map, boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService getNodes(Map<String, String> callParams) {
		return getNodes(null, callParams);
	}

	/**
	 * Same as {@link AylaDeviceGateway#getNodes(Handler, Map, boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getNodes(Handler mHandle, Map<String, String> callParams) {
		return getNodes(mHandle, callParams, false);
	}

	/**
	 * These class methods get one or more gateway nodes from the Ayla Cloud Service. For now getNodes API does not support
	 * lan mode due to device constraints. If cloud service is available, make http service request and return
	 * AylaNetworks.AML_ERROR_ASYNC_OK(200) upon success; if cloud service is not available, load local cache and return
	 * AylaNetworks.AML_ERROR_ASYNC_OK_CACHED(203) upon success; or otherwise return AylaNetworks.AML_ERROR_NOT_FOUND(404).
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param callParams call parameters
	 * @param delayExecution could be set to true if you want to setup this call but have it execute on an external event
	 * @return AylaRestService object
	 */
	public AylaRestService getNodes(Handler mHandle, Map<String, String> callParams, boolean delayExecution) {

		AylaDeviceGateway gw = (AylaDeviceGateway)this.getCopyInDeviceManager();
		
		if ( AylaReachability.isCloudServiceAvailable()) {
			return gw.getNodesFromService(mHandle, delayExecution);
		} else {
			String savedJsonNodeContainers = "";
			AylaRestService rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.GET_NODES_LOCAL_CACHE);
			savedJsonNodeContainers = AylaCache.get(AML_CACHE_NODE, this.dsn);
			if( !TextUtils.isEmpty(savedJsonNodeContainers) ) {
				try {
					String jsonNodes = AylaDeviceNode.stripContainers(savedJsonNodeContainers, null);
					AylaSystemUtils.saveToLog("%s, %s, %s, jsonNodes:%s.", "I", tag, "getNodes", jsonNodes);
					returnToMainActivity(rs, jsonNodes, AylaNetworks.AML_ERROR_ASYNC_OK_CACHED, AylaRestService.GET_NODES_LOCAL_CACHE, delayExecution);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				saveToLog("%s, %s, %s, %s.", "I", tag, "getNodes", "nodes not found in local cache!");
				returnToMainActivity(rs, "", AylaNetworks.AML_ERROR_NOT_FOUND, AylaRestService.GET_NODES_LOCAL_CACHE, delayExecution);
			}
			return rs;
		}
	}// end of getNodes 

	
	/**
	 * This helps retrieve Node info from service. 
	 * Will take AylaDevice.key as top priority, and dsn as secondary priority, 
	 * or fail if neither of them is valid.<br/>    
	 * 
	 * For now we cannot retrieve node info directly from device in lan mode, 
	 * put it private so that future changes do not impact interface.
	 * */
	protected AylaRestService getNodesFromService(final Handler h, final boolean delayExecution) {
		AylaRestService rs = null;
		String url = AylaSystemUtils.ERR_URL;
		if (this.getKey() != null) {
			Number devKey = this.getKey().intValue();
			// String url = "http://ads-dev.aylanetworks.com/apiv1/devices/<key>/nodes.json";
			url = String.format("%s%s%s%s", AylaSystemUtils.deviceServiceBaseURL(), "devices/", devKey, "/nodes.json");    
		} else if (!TextUtils.isEmpty(this.dsn)) {
			// String url = "http://ads-dev.aylanetworks.com/apiv1/dsns/<dsn>/nodes.json";
			url = String.format("%s%s%s%s", AylaSystemUtils.deviceServiceBaseURL(), "dsns/", this.dsn, "/nodes.json"); 
		} else { 
			saveToLog("%s, %s, %s, %s.", "W", tag, "getNodesFromService", "neither key nor dsn is valid");
		}
		
		rs = new AylaRestService(h, url, AylaRestService.GET_REGISTERED_NODES, this.dsn);
		saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaNodes", "url", url, "delayedExecution", delayExecution?"true":"false", "getNodesService");
		if (!delayExecution) {
			rs.execute();
		}
		return rs;
	}// end of getNodesFromService
	
	
	public AylaRestService getNodeProperties(Map<String, String> callParams, AylaDeviceNode requestNode) {
		return getNodeProperties(null, callParams, requestNode,  false);
	}
	public AylaRestService getNodeProperties(Handler mHandle, Map<String, String> callParams, AylaDeviceNode requestNode) {
		return getNodeProperties(mHandle, callParams, requestNode,  false);
	}
	public AylaRestService getNodeProperties(Handler mHandle, Map<String, String> callParams, AylaDeviceNode requestNode,  Boolean delayExecution) {

		AylaDeviceNode node = null;
		if (requestNode != null) {
			node = (AylaDeviceNode)requestNode.getCopyInDeviceManager();
		} else {
			saveToLog("%s, %s, %s, %s.", "W", tag, "getNodeProperties", "requestNode is null");
		}
		
		if (node == null) {
			saveToLog("%s, %s, %s, %s.", "E", tag, "getNodeProperties", "node not initialized properly");
		}
		AylaRestService rs = AylaDeviceNode.getProperties(mHandle, node, callParams, delayExecution);
		return rs;
	}
	
	public AylaRestService createNodeDatapoint(AylaDatapoint datapoint, AylaDeviceNode requestNode, AylaProperty requestProperty) {
		return createNodeDatapoint(null, datapoint, requestNode, requestProperty, false);
	}
	public AylaRestService createNodeDatapoint(Handler mHandle, AylaDatapoint datapoint, AylaDeviceNode requestNode, AylaProperty requestProperty) {
		return createNodeDatapoint(mHandle, datapoint, requestNode, requestProperty, false);
	}
	public AylaRestService createNodeDatapoint(Handler mHandle, AylaDatapoint datapoint, AylaDeviceNode requestNode, AylaProperty requestProperty, Boolean delayExecution) {

		AylaDevice node = null;
		if (requestNode != null) {
			node = (AylaDeviceNode)requestNode.getCopyInDeviceManager();
		} else {
			saveToLog("%s, %s, %s, %s.", "W", tag, "createNodeDatapoint", "requestNode is null");
		}

		if (node == null) {
			saveToLog("%s, %s, %s, %s.", "E", tag, "createNodeDatapoint", "node not initialized properly");
		}
		
		if ( node.properties == null ) {
			node.initPropertiesFromCache();
		}
		node.initPropertiesOwner();
		AylaProperty property = node.findProperty(requestProperty.name)!=null? node.findProperty(requestProperty.name): requestProperty;
		
		AylaRestService rs = property.createDatapoint(mHandle, datapoint, delayExecution);
		return rs;
	}
	
	public AylaRestService getNodeDatapointsByActivity(Map<String, String> callParams, AylaDeviceNode requestNode, AylaProperty requestProperty) {
		return getNodeDatapointsByActivity(null, callParams, requestNode, requestProperty, false);
	}
	public AylaRestService getNodeDatapointsByActivity(Handler mHandle, Map<String, String> callParams, AylaDeviceNode requestNode, AylaProperty requestProperty) {
		return getNodeDatapointsByActivity(mHandle, callParams, requestNode, requestProperty, false);
	}
	public AylaRestService getNodeDatapointsByActivity(Handler mHandle, Map<String, String> callParams, AylaDeviceNode requestNode, AylaProperty requestProperty, Boolean delayExecution) {

		AylaDeviceNode node = null;

		if (requestNode != null) {
			node = (AylaDeviceNode)requestNode.getCopyInDeviceManager();
		} else {
			saveToLog("%s, %s, %s, %s.", "W", tag, "createNodeDatapoint", "requestNode is null");
		}
		
		if (node == null) {
			saveToLog("%s, %s, %s, %s.", "E", tag, "createNodeDatapoint", "node not initialized properly");
		}
		
		AylaProperty property = node.findProperty(requestProperty.name)!=null? node.findProperty(requestProperty.name): requestProperty;
		
		AylaRestService rs = property.getDatapointsByActivity(mHandle, callParams, delayExecution);
		return rs;
	}
	
	
	public static class AylaConnectionStatus {
		@Expose
		public String dsn;
		@Expose
		public String mac;
		@Expose
		public String status;
	}// end of AylaConnectionStatus class     
	
	
	public static class AylaGenericNodeConnectionStatus {
		@Expose
		public String dsn;
		@Expose
		public String mac;
		@Expose
		public boolean status; 
	}// end of AylaGenericNodeConnectionStatus class    
	

	/**
	 * Same as {@link AylaDeviceGateway#getNodesConnectionStatus(Handler, Map, boolean, boolean)} with no call parameters and isforceFetch set to true.
	 * */

	public AylaRestService getNodesConnectionStatus(Handler mHandle) {
		return getNodesConnectionStatus(mHandle, false);
	}
	public AylaRestService getNodesConnectionStatus(Handler mHandle, final boolean delayExecution) {
		return getNodesConnectionStatus(mHandle, null, true, delayExecution);
	}
	/**
	 * These instance methods get connection status for one or more gateway nodes. 
	 * If the gateway is lan mode active, it is reliable that the latest connection status have been cached;<br/> 
	 * If the gateway is not lan mode active, get the latest connection status from service and update local cache. 
	 * <br/>
	 * <br/>
	 * Note that AylaDevice.connectionStatus is String, and so it is in the response from cloud, but it is boolean 
	 * in the response from module. The library deals with this difference underneath to make consistent behavior. And in 
	 * app level, one would always need to use AylaDeviceGateway.AylaConnectionStatus to parse results in the user handler.<br/> 
	 * 
	 * For node connection status notification, the format is like below:
	 * {<br/>
	 * &#09;"conn_status"=[{
	 * &#09;&#09;"dsn"=${node+dsn}
	 * &#09;&#09;, "status"=${Online/Offline}
	 * &#09;&#09;}, {${node_connection_status_entity}}], <br/>
	 * &#09;"dsn"=${gateway_dsn}, <br/>
	 * &#09;"statusCode"=${statusCode}, <br/>
	 * &#09;"type"="node", <br/>
	 * }
	 * 
	 * @param mHandle handler object to receive the result.
	 * @param callParams parameters fo the API call, by default null.
	 * 
	 * @param isForceFetch once set to true, it will either access service or lan mode with local cached node dsn info, 
	 * 	regardless of local cache availability. In case things change when lan mode is down. By default true.  This param 
	 * is deprecated, only when there is no lan mode no cloud we will use cache, and return AylaNetworks.AML_ERROR_ASYNC_OK_CACHED(203). 
	 * 
	 * @return an executable AylaRestService object.
	 * */

	private AylaRestService getNodesConnectionStatus(Handler mHandle, Map<String, String> callParams, final boolean isForceFetch, final boolean delayExecution) {
		
		AylaDeviceGateway gw = (AylaDeviceGateway)this.getCopyInDeviceManager();
		final AylaRestService rsConnectionStatus = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.GET_NODES_CONNECTION_STATUS);
		try {
			if ( gw.isLanModeActive() ) {
				// If currently in an active lan mode session.
				if (gw.nodes == null || gw.nodes.length < 1) {
					gw.nodes = AylaDeviceManager.getCachedGatewayNodeArray(gw.dsn);
				}
				
				if (gw.nodes == null || gw.nodes.length < 1) {
					// TODO: No local cache, get latest node info from service.
					
				}// nodes null ends here 
				else {
					if ( isForceFetch ) {
						// Assuming local cache has dsn info for all nodes. This assumes the hardware registration are all done by the same app. 
						final int batch = gw.nodes.length / MAX_NODE_IN_LAN_MODE_QUERY;
						final int residue = gw.nodes.length % MAX_NODE_IN_LAN_MODE_QUERY;
						saveToLog("%s, %s, %s, nodes.length:%s, batch:%s, residue:%s.", 
								"D", tag, "getNodesConnectionStatus", gw.nodes.length+"", batch+"", residue+"");
						AylaRestService rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.GET_NODES_CONNECTION_STATUS_LANMODE);
						
						for (int i=0; i<batch; i++) {
							List<AylaDeviceNode> list = new  ArrayList<AylaDeviceNode>();     
							for (int temp=0; temp<MAX_NODE_IN_LAN_MODE_QUERY ; temp++) {
								list.add(gw.nodes[ i*MAX_NODE_IN_LAN_MODE_QUERY + temp]);
							}
							gw.getNodesConnectionStatusLanMode(mHandle, list, rs);         
						}
						
						if (residue>0) {
							List<AylaDeviceNode> list = new ArrayList<AylaDeviceNode>();
							for (int temp=0; temp < residue ; temp++) {
								list.add(gw.nodes[ batch*MAX_NODE_IN_LAN_MODE_QUERY + temp ]);
							}
							gw.getNodesConnectionStatusLanMode(mHandle, list, rs);              
						}
						
						return rsConnectionStatus;
					} else {
						// extract connection status from cached nodes and get back to caller
						AylaConnectionStatus[] connectionStatus = extractConnectionStatusFromNodes(gw.nodes);
						
						String jsonConnectionStatus = AylaSystemUtils.gson.toJson(connectionStatus, AylaConnectionStatus[].class);
						returnToMainActivity(rsConnectionStatus, jsonConnectionStatus
								, AylaNetworks.AML_ERROR_ASYNC_OK_CACHED, AylaRestService.GET_NODES_CONNECTION_STATUS, false);
						return rsConnectionStatus;
					}
				}// nodes not null ends here                   
			}
			
			if ( AylaReachability.isCloudServiceAvailable() ) {
				// Access service for the latest connection info
				gw.getNodesFromService(new mGetNodeConnectionStatusHandler(mHandle, gw), delayExecution);
				return rsConnectionStatus;
			} 
			
		} catch (Exception ex) {
			ex.printStackTrace();
			saveToLog("%s, %s, %s:%s, %s", "E", tag, "error", ex.getLocalizedMessage(), "getNodesConnectionStatus");
			returnToMainActivity(rsConnectionStatus, "error", AML_GENERAL_EXCEPTION , AylaRestService.GET_NODES_CONNECTION_STATUS, false);
			return rsConnectionStatus;
		} 
		
		saveToLog("%s, %s, %s, %s.", "E", tag, "getNodesConnectionStatus", "Cannot retrieve the latest connection status");
		returnToMainActivity(rsConnectionStatus, "error", AML_ERROR_NOT_FOUND, AylaRestService.GET_NODES_CONNECTION_STATUS, false);
		return rsConnectionStatus;
	}// end of getNodesConnectionStatus 
	
	
	
	private final int MAX_NODE_IN_LAN_MODE_QUERY = 5;
	/*
	 * Generic gateway has constraints that we can only process at most 5 nodes per API calls for now. 
	 * It also assumes the gateway is in an active lan mode session, which means a getLanModule() would not return null.
	 * 
	 * As this function only serves internally, need to make sure it satisfies the preconditions.
	 * */
	private void getNodesConnectionStatusLanMode(final Handler mHandle, List<AylaDeviceNode> src, final AylaRestService rs) {
		if (src == null || src.size() > MAX_NODE_IN_LAN_MODE_QUERY) {
			saveToLog("%s, %s, %s, %s!", "E", tag, "getNodesConnectionStatusLanMode", "node size exceeds MAX(5)");
			return;
		}
		
		AylaLanModule module = this.getLanModule();
		if (module == null) {
			saveToLog("%s, %s, %s, %s!", "E", tag, "getNodesConnectionStatusLanMode", "Gateway not in active lan mode session");
			return; 
		}
		
		int cmdId = module.getSession().nextCommandOutstandingId();
		// For debugging.   
		String jsonCommand = null;
		StringBuilder sb = new StringBuilder(64);
		sb.append("{\"cmds\":[{\"cmd\":{")
			.append("\"cmd_id\":").append(cmdId).append(",")
			.append("\"method\":\"GET\",")
			.append("\"resource\":\"conn_status.json\",")
			.append("\"data\":\"{\\\"dsns\\\":[");
		for (int i=0; i < src.size(); i++) {
			if (i != 0) {
				sb.append(", ");
			}
			sb.append("\\\"").append(src.get(i).dsn).append("\\\"");
		}
		sb.append("]}\",")
			.append("\"uri\":\"node/conn_status.json\"")
			.append("}}]}");
		
		jsonCommand = sb.toString();
		saveToLog("%s, %s, %s, jsonCommand:%s.", "D", tag, "getNodesConnectionStatusLanMode", jsonCommand);
		AylaLanCommandEntity entity = new AylaLanCommandEntity(jsonCommand, cmdId, CommandEntityBaseType.AYLA_LAN_COMMAND);
		this.getLanModule().getSession().sendToLanModeDevice(entity, rs);
	}// end of getNodesConnectionStatusLanMode    
	
	
	/**
	 * Given a node array, extract AylaConnectionStatus struct from each of the nodes.
	 * 
	 * @param nodes latest nodes info to be extracted.
	 * @return AylaConnectionStatus array  
	 * */ //TODO: figure out the updateNode() and getNodeConnectionStatusHandler logic and change this to work on this.nodes array, the flow now is hardcode to retrieve nodes from cloud and not update gw.nodes[].
	private static AylaConnectionStatus[] extractConnectionStatusFromNodes(final AylaDeviceNode[] nodes) {
		AylaConnectionStatus[] status = null;
		if (nodes == null || nodes.length <1) {
			return status;
		}
		status = new AylaConnectionStatus[nodes.length];
		for (int i=0; i<nodes.length; i++) {
			if (nodes[i]!=null 
					&& !TextUtils.isEmpty(nodes[i].dsn) 
					&& !TextUtils.isEmpty(nodes[i].connectionStatus)) 
			{
				status[i] = new AylaConnectionStatus();
				status[i].dsn = nodes[i].dsn;
				status[i].mac = nodes[i].mac;
				status[i].status = nodes[i].connectionStatus;
			} else {
				saveToLog("%s, %s, %s, %sth out of %s nodes is %s."
						, "W", tag, "extractConnectionStatusFromNodes"
						, i+"", nodes.length + "", nodes[i] + "");
			}
		}
		return status;
	}// end of extractConnectionStatusFromNodes
	
	/**
	 * pre-condition: node dsn does not have "," char. Will change collective result implementation to list, and pass in a list here.<br/>
	 * 
	 * Given a list of node dsns, find nodes in this.nodes[] array and extract AylaConnectionStatus struct respectively. 
	 * This is a best effort manner, which means if for any dsn, we can not find the node, or we can find the node but 
	 * somehow status attribute is missing, then we simply ignore the node, rather than fail the whole request <br/>
	 * 
	 * @param dsns node dsns separated by ",".
	 * @return AylaConnectionStatus array, null if input is invalid. 
	 * */ //TODO: node connection status is for generic gateway for now, change these two APIs to protected once zigbee solution implement it.
	private AylaConnectionStatus[] extractConnectionStatusFromDSN(final List<String> dsns) {
		if ( dsns == null || dsns.isEmpty() ) {
			saveToLog("%s, %s, %s, %s.", "W", tag, "extractConnectionStatusFromDSN", "dsn string empty");
			return null;
		}
		
		List<AylaConnectionStatus> list = new ArrayList<AylaConnectionStatus>();       
		for (String dsn : dsns) {
			AylaDevice d = this.findNode(dsn);
			if (d==null) {
				continue;
			}
			AylaConnectionStatus status = new AylaConnectionStatus();
			status.dsn = dsn;
			status.mac = d.mac;
			status.status = d.connectionStatus;
			list.add(status);
		}
		
		AylaConnectionStatus[] statusArray = new AylaConnectionStatus[list.size()];
		list.toArray(statusArray);
		return statusArray;
	}// end of extractConnectStatusFromDSN    

	
	protected static class mGetNodeConnectionStatusHandler extends Handler {
		
		private Handler mPostHandler = null;
		private AylaDevice mDevice = null;
		
		public mGetNodeConnectionStatusHandler(final Handler h, final AylaDevice d) {
			mPostHandler = h;
			mDevice = d;
		}
		@Override
		public void handleMessage(Message msg) {
			String jsonResults = (String)msg.obj;
			saveToLog("%s, %s, %s, jsonResults:%s.", "I", tag, "mGetNodeConnectionStatusHandler", jsonResults);
			final AylaRestService rs = new AylaRestService(mPostHandler, AylaSystemUtils.ERR_URL, AylaRestService.GET_NODES_CONNECTION_STATUS);
			if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
				//TODO: Do we need to update nodes here? Or we do not care? 
				AylaDeviceNode[] nodes = AylaSystemUtils.gson.fromJson(jsonResults,  AylaDeviceNode[].class);
				AylaConnectionStatus[] connectionStatus = extractConnectionStatusFromNodes(nodes);
				
				String jsonConnectionStatus = AylaSystemUtils.gson.toJson(connectionStatus, AylaConnectionStatus[].class);
				saveToLog("%s, %s, %s, jsonConnectionStatus:%s.", "I", tag, "mGetNodeConnectionStatusHandler", jsonConnectionStatus);
				returnToMainActivity(rs, jsonConnectionStatus, AML_ERROR_SYNC_OK, AylaRestService.GET_NODES_CONNECTION_STATUS, false);
			} else {
				saveToLog("%s, %s, %s, %s:%s, %s.", "E", tag, "mGetNodeConnectionStatusHandler", "error", msg.arg1, jsonResults);
				returnToMainActivity(rs, jsonResults, msg.arg1, AylaRestService.GET_NODES_CONNECTION_STATUS, false);
			}
		}
	}// end of mGetNodeConnectionStatusHandler class    
	
	/*
	 * Based on given connection status array and status code, build the response json string as per the spec.
	 * return empty string if anything wrong in the middle. 
	 * */
	protected static String buildConnectionStatusResponseString(final AylaConnectionStatus[] status, final AylaDevice gw, final int statusCode) {
		if (status == null || status.length <1) {
			return "{}";
		}
		
		try {
			JSONObject jsonResponse = new JSONObject();
			JSONArray jsonConnStatus = new JSONArray();       
			for (AylaConnectionStatus acs: status) {
				JSONObject connStatus = new JSONObject();
				connStatus.put("dsn", acs.dsn);
				connStatus.put("mac", acs.mac);
				connStatus.put("status", acs.status);
				jsonConnStatus.put(connStatus);
			}
			jsonResponse.put("conn_status", jsonConnStatus);
			jsonResponse.put("dsn", gw.dsn);
			jsonResponse.put("statusCode", statusCode);
			jsonResponse.put("type", "node");
			return jsonResponse.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	protected static String buildConnectionStatusResponseString(AylaGenericNodeConnectionStatus[] status, final AylaDevice gw, final int statusCode) {
		if (status == null || status.length <1) {
			return "{}";
		}
		
		try {
			JSONObject jsonResponse = new JSONObject();
			JSONArray jsonConnStatus = new JSONArray(); 
			for (AylaGenericNodeConnectionStatus agncs: status) {
				JSONObject connStatus = new JSONObject();
				connStatus.put("dsn", agncs.dsn);
				if (agncs.status) {
					connStatus.put("status", "Online");
				} else {
					connStatus.put("status", "Offline");
				}
				jsonConnStatus.put(connStatus);
			}
			jsonResponse.put("conn_status", jsonConnStatus);
			jsonResponse.put("dsn", gw.dsn);
			jsonResponse.put("statusCode", statusCode);
			jsonResponse.put("type", AML_NOTIFY_TYPE_NODE);
			return jsonResponse.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}// end of buildConnectionStatusResponseString
	

	//TODO: update nodes list from input Device. Now it only handles lan mode buffers
	protected void updateNodesFromDeviceList(AylaDevice[] devices, Boolean saveToCache) {
		HashMap<String, AylaDeviceNode> nodeMap = new HashMap<String, AylaDeviceNode>();
		for(AylaDevice device : devices) {
			if(device instanceof AylaDeviceNode) {
				AylaDeviceNode node = (AylaDeviceNode)device;
				if (TextUtils.equals(node.gatewayDsn, this.dsn)) {
					nodeMap.put(node.dsn, node);
				} 
			}
		}
		this.updateNodes(nodeMap, saveToCache);
	}// end of updateNodesFromDeviceList
	
	protected void updateNodes(HashMap<String, AylaDeviceNode> nodeMap, Boolean saveToCache) {
		AylaDeviceNode[] nodes = nodeMap.values().toArray(new AylaDeviceNode[nodeMap.size()]);
		if(this.nodes == null) {
			this.nodes = nodes;
		}
	
		if(lanModeState != lanMode.DISABLED) {
			AylaDeviceNodeContainer[] nodeContainers = new AylaDeviceNodeContainer[nodes.length];
			for (int i=0; i<nodes.length; i++) {
				AylaDeviceNodeContainer container = new AylaDeviceNodeContainer();
				container.device = nodes[i];
				nodeContainers[i] = container;
			}
			String jsonNodeContainers = AylaSystemUtils.gson.toJson(nodeContainers, AylaDeviceNodeContainer[].class);
            updateNodes(nodes, jsonNodeContainers);
		}
	}// end of updateNodes
	
    // ----------------------------- Formerly AylaDeviceNode.lanModeEnable  --------------------------------
	
	//TODO: should not pass in jsonNodeContainers, generate jsonStrig based on merged nodes array inside.
    void updateNodes(AylaDeviceNode[] newNodes, String jsonNodeContainers) {

    	if (newNodes==null || newNodes.length<1) {
    		saveToLog("%s, %s, %s, %s.", "W", tag, "updateNodes", "newNodes array is empty");
    		return ;
    	}
        // Make sure we're only dealing with nodes that belong to us
        HashMap<String, AylaDeviceNode> nodeMap = new HashMap<String, AylaDeviceNode>();
        for (AylaDeviceNode device : newNodes) {
            if (TextUtils.equals(device.gatewayDsn, dsn)) {
                nodeMap.put(device.dsn, device);
            }
        }

        if (nodes == null) {
            // Just take the new node array for ourselves
            nodes = newNodes;
        } else {
            HashMap<String, AylaDeviceNode> lanNodeMap = new HashMap<String, AylaDeviceNode>();

            //current captured nodes
            for (AylaDeviceNode node : nodes) {
                if (nodeMap.containsKey(node.dsn)) {
                    AylaDeviceNode nNode = nodeMap.get(node.dsn);
                    updateNode(node, nNode);
                    lanNodeMap.put(node.dsn, node);
                } else {
                    //TODO: test only
                    AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "W", "AylaLanMode", node.dsn, "removedFromLanList", "updateProperty");
		        }
	        }
	
            //new nodes
            for (AylaDeviceNode node : newNodes) {
                if (!lanNodeMap.containsKey(node.dsn)) {
                    lanNodeMap.put(node.dsn, node);
                }
            }
            nodes = lanNodeMap.values().toArray(new AylaDeviceNode[lanNodeMap.size()]);
        }
        saveToLog("%s, %s, %s, %s.", "D", tag, "updateNodes", "verify nodes initialization, jsonNodeContainers:" + jsonNodeContainers);
        for (int i=0; i<nodes.length; i++) {
        	saveToLog("", "D", tag, "updateNodes", "nodes[" + i + "] object:" + nodes[i] + " dsn:" + nodes[i].dsn + " gatewayDSN:" + nodes[i].gatewayDsn);
        }

        AylaCache.save(AML_CACHE_NODE, dsn, jsonNodeContainers);
    }

	//lan mode methods
	@Override
	protected Integer lanModeWillSendEntity(AylaLanCommandEntity entity) {
		return 200;
	}

	
	/**
	 * Based on node connection status info from module, either notifications or request feedbacks, update corresponding attributes
	 * 
	 * @param rs null for notifications, callback for requests.
	 * @param status from module, -1 for notification, non-negative result code for request.
	 * @param connStatus latest node connection status info.
	 * 
	 * @return status code. 200 if everything is good; 206 if the whole request is not done; 400 indicates this is a failure response 
	 * or no node matches for some dsn. More TBD. 
	 * */   //TODO:  Now node connection status expands to zigbee, 
	// need to change the param name AylaGenericNodeConnectionStatus to something more appropriate.          
	protected int lanModeUpdateNodeConnectionStatus(
			final AylaRestService rs, final int status
			, final AylaDeviceGateway.AylaGenericNodeConnectionStatus[] connStatus) {
		AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "I", tag, "lanModeUpdateNodeConnectionStatus"
				, "status:" + status + "\nrs:" + rs + "\nstatus:" + connStatus);                       
		
		int requestStatus = 200;
		
		if (rs == null) {
			// For notifications        
			updateNodeConnectionStatus(connStatus);
			
			String response = AylaDeviceGateway.buildConnectionStatusResponseString(connStatus, this, status);    
			requestStatus = 200;
			AylaNotify.returnToMainActivity(null, response, requestStatus, 0, this);
			return requestStatus;
		}
		
		List<String> dsns = null;
		if ( connStatus!=null && connStatus.length >0) {
			dsns = new ArrayList<String>();
			for (AylaDeviceGateway.AylaGenericNodeConnectionStatus s : connStatus) {
				if ( !TextUtils.isEmpty(s.dsn) ) {
					dsns.add(s.dsn);
				}
			}
		}
		
		if ( (status>=400 && status<500) || status==503 ) {
			requestStatus = 400;
			// connStatus invalid   
			
			if (rs.collectResult(dsns) == -3) {
				AylaConnectionStatus[] s = this.extractConnectionStatusFromDSN( rs.getCollectiveResult() );
				String jsonStatus = "{\"error\":\"Bad Request\"}";
				if ( s==null || s.length<1 ) {
					requestStatus = 400;
				} else {
					if (!rs.isRequestComplete()) {
						requestStatus = 206;
					}
					
					jsonStatus = AylaSystemUtils.gson.toJson(s, AylaConnectionStatus[].class);
					jsonStatus = jsonStatus.replaceAll("\"status\":true", "\"status\":\"Online\"");
					jsonStatus = jsonStatus.replaceAll("\"status\":false", "\"status\":\"Offline\"");
				}
				
				AylaDeviceGateway.returnToMainActivity(rs, jsonStatus, requestStatus, 0, false);
			} else {
				requestStatus = status;
			}
			return requestStatus;
		}
		
		
		if (rs.collectResult(dsns)==1) {
			if (!rs.isRequestComplete()) {
				requestStatus = 206;
			}
			AylaConnectionStatus[] s = this.extractConnectionStatusFromDSN( rs.getCollectiveResult() );  
			String jsonStatus = AylaSystemUtils.gson.toJson(s, AylaConnectionStatus[].class);
			jsonStatus = jsonStatus.replaceAll("\"status\":true", "\"status\":\"Online\"");
			jsonStatus = jsonStatus.replaceAll("\"status\":false", "\"status\":\"Offline\"");
			
			AylaDeviceGateway.returnToMainActivity(rs, jsonStatus, requestStatus, 0, false);
		}
		return requestStatus;
	}// end of lanModeUpdateNodeConnectionStatus             
	
	
	
	
	

	/**
	 * Based on messages from module, either notifications or request feedbacks, update corresponding attributes. 
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
		
		if ( !TextUtils.isEmpty(data.dsn) && !TextUtils.equals(data.dsn, this.dsn)) { 
			// For node processing 
			AylaDevice d = AylaDeviceManager.sharedManager().deviceWithDSN(data.dsn);
			return d.lanModeUpdateProperty(rs, status, data);
		}
		
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
			 * Based on observation it could be 400, 404, 408, pretty tricky here
			 * It might cover other cases that should not be dealt with in this way.
			 * */
			if ( ( rs.RequestType == AylaRestService.GET_NODE_PROPERTIES_LANMODE) 
					||(rs.RequestType == AylaRestService.GET_NODE_DATAPOINT_LANMODE)
					||(rs.RequestType == AylaRestService.CREATE_NODE_DATAPOINT_LANMODE) ) {
				/*
				 * Handling for AylaRestService.GET_NODE_PROPERTIES_LANMODE 
				 * AylaRestService.GET_NODE_DATAPOINT_LANMODE
				 * AylaRestService.CREATE_NODE_DATAPOINT_LANMODE
				 * */
				String dsn = rs.getDSN();
				saveToLog("%s, %s, %s, %s.", "D", tag, "lanModeUpdateProperty", "dsn:" + dsn);
				AylaDevice d = AylaDeviceManager.sharedManager().deviceWithDSN(dsn);
				updateStatus = d.lanModeUpdateProperty(rs, status, data);
			} else if ( ( rs.RequestType == AylaRestService.GET_PROPERTIES_LANMODE) 
					|| (rs.RequestType == AylaRestService.GET_DATAPOINT_LANMODE) ) {
				if (rs.collectResult(result) == -3) {
					// All cmds for this request are collected, get back to app.
					AylaProperty[] aps = this.getCollectiveResult( rs.getCollectiveResult() );
					if (aps == null || aps.length<1 ) {
						updateStatus = 400;
					} else {
						if (!rs.isRequestComplete()) {
							updateStatus = 206;
						}
						
						else if (rs.RequestType == AylaRestService.GET_PROPERTIES_LANMODE) {
							String jsonProperties = "empty response";
							if (aps!=null && aps.length>=1) {
								jsonProperties = AylaSystemUtils.gson.toJson(aps, AylaProperty[].class);
							}
							AylaProperty.returnToMainActivity(rs, jsonProperties, updateStatus, 0, false);
						} else // if (rs.RequestType == AylaRestService.GET_DATAPOINT_LANMODE) 
						{
							String jsonDatapoint = "empty response";
							if (aps!=null && aps.length>=1) {
								jsonDatapoint = "[" + AylaSystemUtils.gson.toJson(aps[0].datapoint, AylaDatapoint.class) + "]";
							}
							AylaDatapoint.returnToMainActivity(rs, jsonDatapoint, updateStatus, 0);
						} 
					}
				}
			} else {
				saveToLog("%s, %s, %s, %s.", "E", tag, "lanModeUpdateProperty", "requestType " + rs.RequestType + " not found");
				updateStatus = 405;
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
			if (rs.RequestType == AylaRestService.GET_PROPERTIES_LANMODE) {
				String jsonProperties = AylaSystemUtils.gson.toJson(aps, AylaProperty[].class);
				AylaProperty.returnToMainActivity(rs, jsonProperties, updateStatus, 0, false);
			} else if (rs.RequestType == AylaRestService.GET_DATAPOINT_LANMODE) {
				String jsonDatapoint = "[" + AylaSystemUtils.gson.toJson(aps[0].datapoint, AylaDatapoint.class) + "]";
				AylaDatapoint.returnToMainActivity(rs, jsonDatapoint, updateStatus, 0);
			} else {
				saveToLog("%s, %s, %s, %s.", "E", tag, "lanModeUpdateProperty", "requestType " + rs.RequestType + " not found");
				updateStatus = 405;
			}
		}
		return updateStatus;
	}// end of lanModeUpdateProperty  
	
	
	
	//TODO: complete update of non-null node values, assuming all attributes on newNode is latest, except the consistent ones like dsn or macAddr.
	protected void updateNode(AylaDeviceNode node, AylaDeviceNode newNode) {
		if (TextUtils.isEmpty(node.gatewayDsn)) {
			node.gatewayDsn = this.dsn;
		}
		node.connectionStatus = newNode.connectionStatus;
		node.swVersion = newNode.swVersion;
	}
	
	
	/**
	 * Mainly serves getNodeConnectionStatus lan mode. 
	 * 
	 * @param connStatus connection status collection.   
	 * @return true if updated successfully; false if can not find any one node, or input is not valid.
	 * */
	protected boolean updateNodeConnectionStatus(final AylaGenericNodeConnectionStatus[] connStatus) {
		if (connStatus == null || connStatus.length <1 ) {
			return false;
		}
		
		for (AylaGenericNodeConnectionStatus status : connStatus) {
			AylaDeviceNode d = this.findNode(status.dsn);
			if (d == null) {
				return false;
			}
			d.connectionStatus = status.status? "Online":"Offline";
		}
		return true;
	}// end of updateNodeConnectionStatus
	
	
	public AylaDeviceNode findNode(String dsn) {
		AylaDeviceNode node = null;
		if(this.nodes == null ||
			this.nodes.length == 0 ||
			TextUtils.isEmpty(dsn)) {
			//no nodes found
		}
		else {
			
			for (int i=0; i<nodes.length; i++) {
				if (TextUtils.equals(nodes[i].dsn, dsn)) {
					return nodes[i];
				}
			}
		}
		return node;
	}
	
	public AylaDeviceNode findNodeWithMacAddress(String mac) {
		AylaDeviceNode node = null;
		
		if(TextUtils.isEmpty(mac) || this.nodes == null) {
			return node;
		}
		
		for (int i=0; i<this.nodes.length; i++) {
			if (TextUtils.equals(mac, this.nodes[i].mac)) {
				node = this.nodes[i];
				break;
			}
		}
		return node;
	}// end of findNodeWithMacAddress            
	
}





