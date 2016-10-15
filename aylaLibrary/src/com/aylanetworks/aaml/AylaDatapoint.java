//
//  AylaDatapoint.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 8/15/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.json.JSONObject;
import com.aylanetworks.aaml.enums.CommandEntityBaseType;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

class AylaDatapointContainer {
	@Expose
	public AylaDatapoint datapoint = null;
}

public class AylaDatapoint extends AylaSystemUtils {

	private final static String tag = AylaDatapoint.class.getSimpleName();
	
	public static final String kAylaDataPointCount = "count";
	public static final String kAylaDataPointValue = "value";
	public static final String kAylaDataPointUpdatedAt = "updated_at";
	public static final String kAylaDataPointCreatedAt = "created_at";
	public static final String kAylaDataPointOwnerDSN = "owner_dsn";
	public static final String kAylaDataPointSinceDate = "created_at_since_date";
	public static final String kAylaDataPointEndDate = "created_at_end_date";

	/* properties from cloud service. */
	@Expose
	protected String createdAt;
	@Expose
	protected String updatedAt;
	@Expose 
	protected String value;
	@Expose
	private Number nValue;
	@Expose
	private String sValue;

	/* For datapoint ack. */
	@Expose
	public String id;
//	public int id;
//	@Expose
//	public int ack_enabled;
	@Expose
	public int ackStatus;
	@Expose
	public int ackMessage;
	@Expose
	public String ackedAt;
	
	@Expose
	public String createdAtFromDevice;
	
	
	// Exopose in public interface.
	public static final String kAylaDatapointDeviceKey = "device_key";
	public static final String kAylaDatapointDatapointID = "datapoint_id";
	public static final String kAylaDatapointPropertyName = "property_name";
	
	static int cmdId = 0;

	// getters and setters
	public String createdAt() {
		return createdAt;
	}
	public void createdAt(String _createdAt) {
		this.createdAt = _createdAt;
	}

	public String updatedAt() {
		return updatedAt;
	}
	public void updatedAt(String _updatedAt) {
		this.createdAt = _updatedAt;
	}
	
	public String value() {
		return value;
	}
	public void value(String _value) {
		this.value = _value;
	}

	public Number nValue() {
		return nValue;
	}
	public void nValue(Number _nValue) {
		this.nValue = _nValue;
	}

	public String sValue() {
		return sValue;
	}
	public void sValue(String _sValue) {
		this.sValue = _sValue;
	}


	@Override
	public String toString()
	{
		return AylaSystemUtils.gson.toJson(this, this.getClass());
	}

	public Number nValueFormatted(String baseType) {
		Number num = 0;
		try {
			if (this.value == null) {
				num = 0;
			} else if (TextUtils.equals(baseType, "integer")) {
				num = Integer.parseInt(this.value);
			} else if (TextUtils.equals(baseType, "string")) {
				num = 0;
			} else if (TextUtils.equals(baseType, "boolean")) { // checks TBD
				num = Integer.parseInt(this.value);
			} else if (TextUtils.equals(baseType, "decimal")) {
				num = Double.parseDouble(this.value);  // formating TBD
			} else if (TextUtils.equals(baseType, "float")) {
				num = Double.parseDouble(this.value);
			} else if (TextUtils.equals(baseType, "stream")||TextUtils.equals(baseType, "file")) {
				// Not Applicable.
			} else {
				if (!AML_LANMODE_IGNORE_BASETYPES.contains(baseType)) {// skip unnecessary base types
					AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s:%s, %s", "E", "Datapoints", "baseType", baseType, "error", "unsupported basetype", "getnValue");
				}
			}
		} catch (NumberFormatException nx) {
			nx.printStackTrace();
			AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s:%s, %s", "E", "Datapoints", kAylaDataPointValue, this.value, "exception", nx, "getnValue - Number format exception");
		}
		return num;
	}

	public String sValueFormatted(String baseType) {
		String valueStr = "";
		try {
			if (TextUtils.equals(baseType, "integer")) {
				valueStr = this.value;
			} else if (TextUtils.equals(baseType, "string")) {
				valueStr = this.value;
			} else if (TextUtils.equals(baseType, "boolean")) {               
				valueStr = this.value;
			} else if (TextUtils.equals(baseType, "decimal")) {
				valueStr = this.value; 					
			} else if (TextUtils.equals(baseType, "float")) {
				valueStr = this.value;
			} else if (TextUtils.equals(baseType, "stream")||TextUtils.equals(baseType, "file")) {
				valueStr = this.value;
			} else {
				AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "Datapoints", "baseType", baseType, "getsValue - unsupported base type");
			}
		} catch (NumberFormatException nx) {
			nx.printStackTrace();
			AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s:%s, %s", "E", "Datapoints", kAylaDataPointValue, this.value, "exception", nx, "getsValue - Number format exception");
		}	
		return valueStr;
	}

	
	/**
	 * Same as {@link AylaDatapoint#createDatapoint(Handler, AylaProperty, boolean)} with no handler to return resiults and no option to setup the call to execute later.
	 */
	/*
	 * Complementary comment not exposed to app developers:
	 * For zigbee node property update,  it actually returns the node datapoint if it is a node operation, and gateway datapoint if the operation
	 * is on a property on the gateway.        
	 * */
	public AylaRestService createDatapoint(AylaProperty property) {
		return createDatapoint(null, property, true);
	}

	/**
	 * Same as {@link AylaDatapoint#createDatapoint(Handler, AylaProperty, boolean)} with no option to setup the call to execute later
	 */
	public AylaRestService createDatapoint(Handler mHandle, AylaProperty property) {
		return createDatapoint(mHandle, property, false);
	}

	/**
	 * Upon successful completion this instance method will post the value to the Ayla device service and instantiate a new datapoint object.
	 *
	 * @param mHandle is where result would be returned.
	 * @param property is the property that the created datapoint would bind to.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService createDatapoint(Handler mHandle, AylaProperty property, boolean delayExecution) {
		// By default 5*2s for ack time out.
		return createDatapoint(mHandle, property, 5, delayExecution);
	}

	/**
	 * Upon successful completion this instance method will post the value to the Ayla device service and instantiate a new datapoint object.
	 *
	 * @param mHandle is where result would be returned.
	 * @param property is the property that the created datapoint would bind to.
	 * @param retry positive number to poll ack status if datapoint has ack capability and is enabled at the server side. If retry is zero or a negative number, datapoint status will not be polled. Default value of retry is 5
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService createDatapoint(Handler mHandle, AylaProperty property, int retry, boolean delayExecution) {
		Number propKey = property.getKey().intValue(); // Handle gson LazilyParsedNumber
		AylaRestService rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.CREATE_DATAPOINT);
		String jsonDatapointContainer = "";
		String jsonDatapoint = "";
		this.convertValueToType(property);

		AylaDevice endpoint = AylaDeviceManager.sharedManager().deviceWithDSN(property.product_name);
		if (endpoint == null) {
			saveToLog("%s, %s, %s, %s.", "E", tag, "createDatapoint", "device " + property.product_name + " cannot be found in AylaDeviceManager");
			returnToMainActivity(rs, "Device missing.", 400, AylaRestService.CREATE_DATAPOINT);
			return rs;
		}
		
		if (endpoint.properties == null) {
			endpoint.initPropertiesFromCache();
		}

		// create a datapoint on the device
		if ( endpoint != null && endpoint.properties!=null && endpoint.isLanModeActive() ) {
			rs.RequestType = AylaRestService.CREATE_DATAPOINT_LANMODE;
			if (endpoint.isNode()) {
				rs.RequestType = AylaRestService.CREATE_NODE_DATAPOINT_LANMODE;
			}
			
			this.createdAt = gmtFmt.format(new Date());
			this.updatedAt = this.createdAt;
			
			endpoint.property = endpoint.findProperty(property.name);
			
			if (endpoint.property != null) {
				this.lanModeEnable(property);
								
				cmdId = endpoint.getLanModule().getSession().nextCommandOutstandingId();

				String toDeviceValue = endpoint.lanModeToDeviceUpdate(rs, endpoint.property, this.value, cmdId);
				if(toDeviceValue == null) {
					saveToLog("%s, %s, %s:%s, %s", "E", "Datapoints", "LanMode", "toDeviceValue null", "createDatapoint_lanmode");
					returnToMainActivity(rs, jsonDatapoint, 404, 0);
					return rs;
				}
				
				AylaProperty lanProperty = endpoint.getPropertyForCreateDatapointOnEndPoints(property.name);
				
				if(lanProperty != null) {
					jsonDatapointContainer = this.sendToLanModeDevice(rs, lanProperty, toDeviceValue, cmdId, endpoint.isNode()&&!endpoint.isZigbee());                 

					// BSK: Nodes will not want this JSON response back- it's the JSON for the gateway's datapoint,
					// not the node's datapoint.
                    if ( endpoint.isNode() &&  !lanProperty.ackEnabled ) {
                        jsonDatapointContainer = 			"{\"datapoint\":{";
                        jsonDatapointContainer = jsonDatapointContainer + "\"" + kAylaDataPointCreatedAt + "\":" + "\"" + createdAt + "\",";
                        jsonDatapointContainer = jsonDatapointContainer + "\"" + kAylaDataPointUpdatedAt + "\":" + "\"" + updatedAt + "\",";
                        jsonDatapointContainer = jsonDatapointContainer + "\"" + kAylaDataPointValue + "\":" + value ;
                        jsonDatapointContainer = jsonDatapointContainer + "}}";
					}

					if (!TextUtils.isEmpty(jsonDatapointContainer)) {
						try {
							jsonDatapoint = stripContainer(jsonDatapointContainer);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					if ( !lanProperty.ackEnabled ) {
						saveToLog("%s, %s, %s:%s, %s", "I", "Datapoints", "mode", "LanMode", "createDatapoint");
						returnToMainActivity(rs, jsonDatapoint, 200, 0);
					}
				}
				else {
					saveToLog("%s, %s, %s:%s, %s", "E", "Datapoints", "LanModeProperty", "PropertyNotFound", "createDatapoint_lanmode");
					returnToMainActivity(rs, jsonDatapoint, 404, 0);
				}
			} else {
				saveToLog("%s, %s, %s:%s, %s", "E", "Datapoints", "LanModeEdpt", "PropertyNotFound", "createDatapoint_lanmode");
				returnToMainActivity(rs, jsonDatapoint, 404, 0);
			}
		}
		// create the datapoint on the service
		else if (AylaReachability.isCloudServiceAvailable()) {
			//  params = {"datapoint":{"value":1}};
			
			Handler handler = null;
			if ( property.ackEnabled ) 
			{
				handler = new DatapointAckHandler(mHandle, property, retry);
			} else { // datapoint ack not enabled
				handler = mHandle;
			}
			
			final JsonObject datapoint = new JsonObject();
			jsonDatapoint = jsonDatapoint + "{\"datapoint\":";
			datapoint.addProperty(kAylaDataPointValue, this.value);
			jsonDatapoint = jsonDatapoint + datapoint.toString();
			jsonDatapoint = jsonDatapoint + "}";
			
			String url = String.format(Locale.getDefault(), "%s%s%d%s", deviceServiceBaseURL(), "properties/", propKey, "/datapoints.json");
			rs = new AylaRestService(handler, url, AylaRestService.CREATE_DATAPOINT);
			rs.setEntity(jsonDatapoint);
			saveToLog("%s, %s, %s:%s, %s%s, %s", "I", "Datapoints", "url", url, "datapointJson", jsonDatapoint, "createDatapoint");
			if (!delayExecution) {
				rs.execute(); 
			}
		}
		// device is unreachable v2.02_ENG
		else {
			saveToLog("%s, %s, %s:%s, %s", "E", "Datapoints", "LanMode", "PropertyNotFound", "createDatapoint_lanmode");
			rs = new AylaRestService(mHandle, "createDatapointLanmode", AylaRestService.CREATE_DATAPOINT_LANMODE);
			jsonDatapoint = AylaSystemUtils.gson.toJson(property.datapoint,AylaDatapoint.class); // return original value
			returnToMainActivity(rs, jsonDatapoint, AML_ERROR_UNREACHABLE, 0);
		}
		return rs;
	}// end of createDatapoint                             
	
	

	
	private static class DatapointAckHandler extends Handler {
		private int numRetries;
		private AylaProperty mProperty;
		// Cache caller customized handler, for multi-level call
		private Handler mAppHandler;
		private AylaDatapoint mCurDatapoint;
		
		public DatapointAckHandler(final Handler h, AylaProperty ap, int retry) {
			numRetries = retry;
			mProperty = ap;
			mAppHandler = h;
			
		}
		
		public void handleMessage(Message msg) {
			String jsonResults = (String)msg.obj;
			AylaSystemUtils.saveToLog("%s, %s, %s, jsonResults:%s."
					, "D", "AylaDatapoint", "DatapointAckHandler", jsonResults);
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
				
				AylaRestService rs = new AylaRestService(mAppHandler, AylaSystemUtils.ERR_URL, AylaRestService.CREATE_DATAPOINT); 
				
				AylaDevice d = AylaDeviceManager.sharedManager().deviceWithDSN(mProperty.product_name);
				if ( d!= null && d.isLanModeActive() ) {
					// In Lan Mode, we do not poll as module will push latest changes.
					AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "D", tag, "DatapointAckHandler", "no polling in lan mode");   
					returnToMainActivity(rs, jsonResults, AylaNetworks.AML_ERROR_OK, AylaRestService.CREATE_DATAPOINT);
					return ;
				}
				
				if (numRetries <= 0) { 
					AylaSystemUtils.saveToLog("%s, %s, %s, %s%s.", "D", tag
							, "DatapointAckHandler", "no polling by user, ackRetry:", numRetries + "");
					returnToMainActivity(rs, jsonResults, AylaNetworks.AML_ERROR_OK, AylaRestService.CREATE_DATAPOINT);
					return ;
				}
				
				try {
					mCurDatapoint = AylaSystemUtils.gson.fromJson(jsonResults, AylaDatapoint.class);
				} catch ( Exception e ) {
					e.printStackTrace();
				}
				DatapointAck ack = null;
				
				ack = new DatapointAck(mAppHandler, mProperty, mCurDatapoint, numRetries);
				ack.waitForDatapointAck();
			} else {
				AylaSystemUtils.saveToLog("%s, %s, %s, %s:%d, %s."
						, "F", "AylaDatapoint", "DatapointAckHandler"
						, "error", msg.arg1, msg.obj);    
				AylaRestService rs = new AylaRestService(mAppHandler, AylaSystemUtils.ERR_URL, AylaRestService.CREATE_DATAPOINT); 
				returnToMainActivity(rs, jsonResults, AML_ERROR_FAIL, AylaRestService.CREATE_DATAPOINT);
			}
		}
	}// end of DatapointAckHandler inner static class     
	
	
	@SuppressLint("HandlerLeak")
	private static class DatapointAck {
		
		private static int ackRetries = 5; // By default 5
//		private AylaProperty mProperty = null;
		private String mDeviceKey = null;
		private String mPropertyName = null;
		private Handler mAppHandler = null;
		private AylaDatapoint mCurDatapoint;

		
		public DatapointAck (final Handler handle, final AylaProperty property, final AylaDatapoint curDP, final int retry) {
			ackRetries = retry;
			mAppHandler = handle;
			mCurDatapoint = curDP;
			
			if (property != null) {
				mPropertyName = property.name();
				AylaDevice device = AylaDeviceManager.sharedManager().deviceWithDSN(property.product_name);
				if ( device != null ) {
					mDeviceKey = device.getKey() + "";
				}
			} else {
				// should not get here. 
				saveToLog("%s, %s, %s, %s.", "E", "AylaDatapoint", "DatapointAck", "property should not be null!");
			}
		}
		
		public void waitForDatapointAck() {
			if (mCurDatapoint == null) {
				AylaRestService rs = new AylaRestService(mAppHandler, AylaSystemUtils.ERR_URL, AylaRestService.GET_DATAPOINT_BY_ID);       
				returnToMainActivity(rs, "datapoint not prepared properly.", AML_ERROR_FAIL, AylaRestService.GET_DATAPOINT_BY_ID);
				return;
			}
			if ( ackRetries < 0 ) {
				AylaRestService rs = new AylaRestService(mAppHandler, AylaSystemUtils.ERR_URL, AylaRestService.GET_DATAPOINT_BY_ID);
				returnToMainActivity(rs, "Polling timeout.", AML_ERROR_TIMEOUT, AylaRestService.GET_DATAPOINT_BY_ID);
				return;
			}
			
			Map<String, String> param = new HashMap<String, String>(); 
			param.put(kAylaDatapointDeviceKey, mDeviceKey);
			param.put(kAylaDatapointPropertyName, mPropertyName);
			mCurDatapoint.getDatapointByID(waitForDatapointAck, param);
		}// end of waitForDatapointAck    
		
		
		private final Handler waitForDatapointAck = new Handler() {
			public void handleMessage(Message msg) {
				String jsonResults = (String)msg.obj;
				AylaSystemUtils.saveToLog("%s, %s, %s, %s:%s, iteration:%s.", "D"
						, "DatapointAck", "waitForDatapointAck", "jsonResult", jsonResults, ackRetries + "");                       
				
				
				if (msg.what != AylaNetworks.AML_ERROR_OK) {
					ackRetries --;
					this.postDelayed(new Runnable(){
						@Override
						public void run() {
							waitForDatapointAck();
						}
					}, 2000);
					return;  
				}
				
				AylaDatapoint dp = AylaSystemUtils.gson.fromJson(jsonResults, AylaDatapoint.class);
				if (dp == null || TextUtils.isEmpty(dp.ackedAt)) {
					ackRetries --;
					this.postDelayed(new Runnable(){
						@Override
						public void run() {
							waitForDatapointAck();
						}
					}, 2000);
					return;
				}
				
				// Success, return to app
				AylaRestService rs = new AylaRestService(mAppHandler, AylaSystemUtils.ERR_URL, AylaRestService.GET_DATAPOINT_BY_ID );
				returnToMainActivity(rs, jsonResults, AylaNetworks.AML_ERROR_ASYNC_OK, AylaRestService.GET_DATAPOINT_BY_ID );
			}// end of handleMessage  
		};
		
	}// end of DatapointAck inner static class 
	
	
	
	
	
	protected void convertValueToType(AylaProperty property) {
		// convert datapoint sValue and nValue to value
		String baseType = property.baseType();
		if (TextUtils.equals(baseType, "integer")) {
			this.value = this.nValue().toString();
		} else if (TextUtils.equals(baseType, "string")) {
			this.value = this.sValue();
		} else if (TextUtils.equals(baseType, "boolean")) {	// checks TBD
			this.value = this.nValue().toString();
		} else if (TextUtils.equals(baseType, "decimal")) {
			DecimalFormat myFormatter = new DecimalFormat("##0.00");
			String decString = myFormatter.format(this.nValue().doubleValue());
			this.value = decString;
		} else if (TextUtils.equals(baseType, "float")) {
			this.value = this.nValue().toString();
		} else if (TextUtils.equals(baseType, "stream") || TextUtils.equals(baseType, "file"))  {
			this.value = this.sValue();
		}
		else {
			saveToLog("%s, %s, %s:%s, %s", "E", "Datapoints", "baseType", baseType, "createDatapoint:unsupported base type");
		}
	}

	protected static String stripContainer(String jsonDatapointContainer) throws Exception {
		String jsonDatapoint = "";
		AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "D", tag, "stripContainer", "jsonDatapointContainer:" + jsonDatapointContainer);
		try {
			AylaDatapointContainer datapointContainer = AylaSystemUtils.gson.fromJson(jsonDatapointContainer,AylaDatapointContainer.class);
			AylaDatapoint datapoint = datapointContainer.datapoint;
			jsonDatapoint = AylaSystemUtils.gson.toJson(datapoint,AylaDatapoint.class);
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", tag, kAylaDataPointValue, datapoint.value, "stripContainer");
			return jsonDatapoint;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", tag, "jsonDatapointContainer", jsonDatapointContainer, "stripContainer");
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Same as {@link AylaDatapoint#getDatapointByID(Handler, Map)} with no handler to return results.
	 * */ 		//TODO: Add unit test for this API.
	public AylaRestService getDatapointByID(Map<String, String> callParams) {
		return getDatapointByID(null, callParams, true);
	}

	/**
	 * This instance method returns datapoint with the specified datapoint id. Since id is globally unique, should return only one datapoint.
	 * This API only has service mode, because in lan mode we do not need to poll the status. One can specify datapoint id, by default the id
	 * of current datapoint.
	 *
	 * @param mHandle is where the results would be returned.
	 * @param callParams call parameters.
	 *
	 * @return the executable AylaRestService unit.
	 * */
	public AylaRestService getDatapointByID(final Handler mHandle, Map<String, String> callParams) {
		return getDatapointByID(mHandle, callParams, false);
	}

	/**
	 * {@link AylaDatapoint#getDatapointByID(Handler, Map)} with option to setup the call to execute later
	 *
	 * @param mHandle is where the results would be returned.
	 * @param callParams call parameters.
	 * @param delayExecution to be set to true to setup this call but have it execute later.
	 * @return the executable AylaRestService unit.
	 * */
	public AylaRestService getDatapointByID(final Handler mHandle, Map<String, String> callParams, boolean delayExecution) {
		String datapointID = this.id;
		String propertyName = "";
		String deviceKey = ""; 
		AylaRestService rs = null;
		
		StringBuilder errors = new StringBuilder();
		
		if ( callParams != null ) {
			String s = callParams.get(kAylaDatapointDeviceKey);
			if (s != null) {
				deviceKey = s;
			} else {
				errors.append("deviceKey missing! ");
			}
			
			s = callParams.get(kAylaDatapointPropertyName);
			if (s!=null) {
				propertyName = s;
			} else {
				errors.append("property name missing! ");
			}
			
			s = callParams.get(kAylaDatapointDatapointID);
			if (s != null) {
				datapointID = s;
			} else {
//				errors.append("Using default datapoint id! ");
			}
		}
		
		if ( !TextUtils.isEmpty(errors.toString()) ) {
			saveToLog("%s, %s, %s, %s", "E", "AylaDatapoints", errors.toString(), "getDatapointByID");
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.GET_DATAPOINT_BY_ID);
			returnToMainActivity(rs, errors.toString(), AylaNetworks.AML_ERROR_PARAM_MISSING, AylaRestService.GET_DATAPOINT_BY_ID);
			return rs;
		}
		
		if ( AylaReachability.isCloudServiceAvailable() ) {
			String url = String.format( Locale.getDefault(), "%s%s%s%s%s", deviceServiceBaseURL(), "devices/"+deviceKey, "/properties/" + propertyName, "/datapoints/"+datapointID, ".json");
			rs = new AylaRestService(mHandle, url, AylaRestService.GET_DATAPOINT_BY_ID);
			saveToLog("%s, %s, %s:%s, %s.", "D", "AylaDatapoints", "url", url, "getDatapointByID");
			if ( !delayExecution ) {
				rs.execute();
			}
		} else {
			// Service not reachable. 
			saveToLog("%s, %s, %s, %s.", "E", "AylaDatapoints", "Service not reachable", "getDatapointByID");
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.GET_DATAPOINT_BY_ID);
			returnToMainActivity(rs, "Service not Reachable", AylaNetworks.AML_ERROR_UNREACHABLE, AylaRestService.GET_DATAPOINT_BY_ID);
		}
		
		return rs;
	}// end of getDatapointByID.  
	
	
	/**
	 * Same as{@link AylaDatapoint#getDatapointsByActivity(Handler, AylaProperty, Map, boolean)} with no handler to return results and no option to execute the call on an external event.
	 */
	/*
	 * Complementary comment not exposed to app developers:
	 * For zigbee node property update,  it actually returns the node datapoint if it is a node operation, and gateway datapoint if the operation is on a 
	 * property on the gateway. 
	 * */
	public AylaRestService getDatapointsByActivity(AylaProperty property, Map<String, String> callParams) {
		return getDatapointsByActivity(null, property, callParams, true);
	}

	/**
	 * Same as {@link AylaDatapoint#getDatapointsByActivity(Handler, AylaProperty, Map, boolean)} with no option to execute the call on an external event.
	 */
	public AylaRestService getDatapointsByActivity(final Handler mHandle, AylaProperty property, Map<String, String> callParams) {
		return getDatapointsByActivity(mHandle, property, callParams, false);
	}

	/**
	 * This instance method returns datapoints for a given property. getDatapointsByActivity returns datapoints in the order they were created.
	 *
	 * @param mHandle is where result would be returned.
	 * @param property is the property that retrieved datapoints should bind to.
	 * @param callParams is applied to qualify the datapoints returned and the maximum number of datapoints returned per query will be limited to maxCount in AylaSystemUtils.
	 *                   Time range filter are provided by setting the following parameters: <br/>
	 *                   kAylaDataPointSinceDate - since created time of datapionts with format "YYYY-MM-DD HH:mm:ss" <br/>
	 *                   kAylaDataPointEndDate - end created time of datapionts with format "YYYY-MM-DD HH:mm:ss"
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 *
	 * @return AylaRestService object
	 */
	public AylaRestService getDatapointsByActivity(final Handler mHandle, AylaProperty property, Map<String, String> callParams, boolean delayExecution) {
		AylaRestService rs = null;
		if ( property == null ) {
			saveToLog("%s, %s, %s, %s.", "W", tag, "getDatapointsByActivity", "null property input");           
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.GET_DATAPOINTS);
			returnToMainActivity(rs, "Parameter invalid", AylaNetworks.AML_ERROR_PARAM_MISSING, AylaRestService.GET_DATAPOINTS);
			return rs;
		}
		Number propKey = property.getKey().intValue(); // Handle gson LazilyParsedNumber
		int count = 1;
		String sinceDate = null;
		String endDate = null;
		if (callParams != null) {
			Object obj = callParams.get(kAylaDataPointCount); //HashMap hm = (HashMap) obj; System.out.println(hm);
            try {
                count = Integer.parseInt( (String)obj );
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

			// limit count to maxCount
			int maxCount = AylaSystemUtils.maxCount;
			count = (count <= maxCount) ? count : maxCount;
			count = (count >= 1) ? count : 1; // get at least one datapoint

			sinceDate = callParams.get(kAylaDataPointSinceDate);
			endDate = callParams.get(kAylaDataPointEndDate);
            if (sinceDate != null && endDate != null) {
                count = maxCount;
            }
		}
		
		// device is reachable, have a device, not the first time, need one datapoint
		AylaDevice device = AylaDeviceManager.sharedManager().deviceWithDSN(property.product_name);
		if (device == null) {
			returnToMainActivity(rs, "Device " + property.product_name + " missing.", 400, AylaRestService.GET_DATAPOINT_LANMODE);
			return rs;
		}
		
		if ( (device.isLanModeActive()) 
				&& (count == 1) 
				&& (!AML_LANMODE_IGNORE_BASETYPES.contains(property.baseType))
		    )
		{
			// GET a datapoint from the device for this property
			try {
				int requestType = AylaRestService.GET_DATAPOINT_LANMODE;
				if (device.isNode()) {
					requestType = AylaRestService.GET_NODE_DATAPOINT_LANMODE;
				}
				rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, requestType);
				
				String ownerDsn = callParams.get(kAylaDataPointOwnerDSN);
				AylaDevice dev = AylaDeviceManager.sharedManager().endpointForDSN(ownerDsn);
				String cmdRequest = null;
				if (dev != null) {
					if (dev.isNode()) {
						
						cmdRequest = dev.lanModeToDeviceCmd(rs, "GET", "datapoint.json", property);
					} else { 
						
						cmdRequest = this.getDatapointFromLanModeDevice(rs, property);
					}
				}
				saveToLog("%s, %s, %s:%s, %s", "I", tag, "cmdRequest", cmdRequest, "getDatapointByActivity_lanMode");
			} catch (Exception e) {
				AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "E", tag, "exception", e.getCause(), "getDatapointsByActivity_lanMode");
				e.printStackTrace();
			}
		} else { // get datapoints from the device service
			String url = String.format(Locale.getDefault(), "%s%s%d%s%s%s", deviceServiceBaseURL(), "properties/", propKey, "/datapoints.json", "?limit=", count);
			if (sinceDate != null && endDate != null) {
                try {
                    url = String.format(Locale.getDefault(), "%s%s%s%s%s", url, "&filter[created_at_since_date]=", URLEncoder.encode(sinceDate, "utf-8"), "&filter[created_at_end_date]=", URLEncoder.encode(endDate, "utf-8"));
                } catch (Exception e) {
                    AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "E", "Datapoints", "exception", e.getCause(), "getDatapointsByActivity_with_filter");
                    e.printStackTrace();
                }

			}
			rs = new AylaRestService(mHandle, url, AylaRestService.GET_DATAPOINTS); 

			saveToLog("%s, %s, %s:%s, %s%s, %s", "I", "Datapoints", "url", url, kAylaDataPointCount, count, "getDatapointsByActivity");
			if (delayExecution == false) {
				rs.execute(); // execute request, datapoints returned to stripContainers
			}
		}
		return rs;
	}

	
	protected static String stripContainers(String jsonDatapointContainers) throws Exception {
		int count = 0;
		String jsonDatapoints = "";
		saveToLog("%s, %s, %s, %s.", "D", tag, "stripContainers", "jsonDatapointContainers:" + jsonDatapointContainers);
		try {
			AylaDatapointContainer[] datapointContainers = AylaSystemUtils.gson.fromJson(jsonDatapointContainers,AylaDatapointContainer[].class);
			AylaDatapoint[] datapoints = new AylaDatapoint[datapointContainers.length];
			for (AylaDatapointContainer datapointContainer : datapointContainers) {
				datapoints[count++]= datapointContainer.datapoint;   			
			}
			jsonDatapoints = AylaSystemUtils.gson.toJson(datapoints,AylaDatapoint[].class);
			AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "Datapoints", kAylaDataPointCount, count, "stripContainers");
			return jsonDatapoints;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s:%s, %s", "E", "Datapoints", kAylaDataPointCount, count, "jsonDatapointContainers", jsonDatapointContainers, "stripContainers");
			e.printStackTrace();
			throw e;
		}
	}
	
	// ----------------------------- Lan Mode Helper methods -------------------
	
	/**
	 * Get datapoint from a property on a generic node. 
	 * pre-condition: rs and property object should be valid, getLanModule()._session should be valid.
	 * @param rs
	 * @param property
	 * 
	 * @return The command that is sent to lan module.         
	 * */      
	// TODO: move it to AylaProperty 
    // TODO: If it is not related to this object, then it should be class function(static)
	// TODO: change it from protected to public for zigbee, need to think of maintaining encapsulation here. 
	public String getDatapointFromLanModeDevice(final AylaRestService rs, final AylaProperty property) {
		return getDatapointFromLanModeDevice(rs, property, false);
	}

	/**
	 * Get datapoint from a property on a generic node.
	 * * pre-condition: rs and property object should be valid, getLanModule()._session should be valid.
	 * @param rs
	 * @param property
	 * @param isNode
	 *
	 * @return The command that is sent to lan module.
	 * */      // TODO: move it to AylaProperty
	protected String getDatapointFromLanModeDevice(final AylaRestService rs, final AylaProperty property, final boolean isNode) {
		AylaDevice device = AylaDeviceManager.sharedManager().deviceWithDSN(property.product_name);
		if ( device == null ) {
			saveToLog("%s, %s, %s, %s.", "D", tag, "getDatapointFromLanModeDevice", "device null");
			return null;
		}

		String uri = null;
		String resource = null;
		if (isNode) {
			uri = "local_lan/node";
			resource = "node_property.json";
		} else {
			uri = serverPath;
			resource = "property.json";
		}
		AylaLanModule module = device.getLanModule();
		String jsonCommand = null;
		StringBuilder sb = new StringBuilder(128);
		if (module != null) {
			int cmdId = module.getSession().nextCommandOutstandingId(); // match up async response with request
			// build property for lan mode device
			
			sb.append("{\"cmds\":[" )
			.append("{\"cmd\":" )
			.append("{\"cmd_id\":" ).append(cmdId).append(",")
			.append("\"method\":\"GET\",")
			.append("\"resource\":\"").append(resource).append("?name=").append(property.name()).append("\",")
			.append("\"data\":\"{\\\"dsn\\\":\\\"" + property.product_name + "\\\"}\",")
			.append("\"uri\":\"").append(uri).append("/property/datapoint.json\"")
			.append("}}]}");
			jsonCommand = sb.toString();
			saveToLog("%s, %s, %s, jsonCommand:%s.", "D", "AylaDatapoint", "getDatapointFromLanModeDevice", jsonCommand.toString());
			
			AylaLanCommandEntity entity = new AylaLanCommandEntity(jsonCommand, cmdId, CommandEntityBaseType.AYLA_LAN_COMMAND);
			entity.dsn = property.product_name;        
			entity.propertyName = property.name();     
			module.getSession().sendToLanModeDevice(entity, rs);
		}
		return jsonCommand;
	}
	
	private void lanModeEnable(AylaProperty property) {
		if (lanModeState != lanMode.DISABLED) {
			String dateStr = gmtFmt.format(new Date());
			// update the lan mode device with the datapoint info
			AylaDevice endpoint = AylaDeviceManager.sharedManager().deviceWithDSN(property.product_name);

			if (endpoint != null) {
				endpoint.property.value = this.value;
				endpoint.property.dataUpdatedAt = dateStr;
				endpoint.property.updateDatapointFromProperty();

				endpoint.property.datapoint = this; // v1.61
				if (endpoint.property.datapoints == null) {
					endpoint.property.datapoints = new AylaDatapoint[1];
				}
				endpoint.property.datapoints[0]= this;
			}
		}
	}
	
	private String sendToLanModeDevice(AylaRestService rs, AylaProperty property, String value, int cmdId) {
		return sendToLanModeDevice(rs, property, value, cmdId, false);
	}
	
	private String sendToLanModeDevice(AylaRestService rs, AylaProperty property, String value, int cmdId, boolean isNode) {
		
		AylaDevice device = AylaDeviceManager.sharedManager().deviceWithDSN(property.product_name);
		if ( device == null || property == null) {
			saveToLog("%s, %s, %s, %s.", "E", tag, "sendToLanModeDevice", "device or property is null");
			return null;
		}

		String datapointJson = "file or stream type not supported";
		if (TextUtils.equals("file", property.baseType()) 
				|| TextUtils.equals("stream", property.baseType())) {
			// baseType not supported in lan Mode.
			return datapointJson;
		}
		
		String sProperties = "properties";
		if (isNode) {
			sProperties = "node_properties";
		}
		AylaLanModule module = device.getLanModule();
		if ( module != null ) {
			// build property for lan mode device
			String jsonProperty = null;
			AylaLanCommandEntity entity = new AylaLanCommandEntity(jsonProperty, cmdId, CommandEntityBaseType.AYLA_LAN_PROPERTY);
			
			String thisValue = value;
			if (TextUtils.equals(property.baseType, "string")) {
				try {
					thisValue = JSONObject.quote(value);
				} catch(Exception e) {
					e.printStackTrace();
					saveToLog("%s, %s, %s:%s, %s", "E", "AylaDatapoints", "JSON_Exception", e.getCause(), "sendToLanModeDevice");
				}
			} 
			
			jsonProperty = "{\"" + sProperties +"\":["
				 + "{\"property\":{";
			if (isNode) { // here isNode only means isGenericNode.
				jsonProperty = jsonProperty + "\"dsn\":\"" + property.product_name + "\",";
			}
			
			jsonProperty = jsonProperty + "\"name\":" + "\"" + property.name + "\","
				 + "\"" + kAylaDataPointValue + "\":" + thisValue + ","
				 + "\"base_type\":\"" + property.baseType + "\"";
			
			if (property.ackEnabled) { // this is going to be what we receive from the module.
				jsonProperty += ",\"id\":\"" + cmdId + "\"";
				entity.dsn = property.product_name;
				entity.propertyName = property.name();
			}
			
			jsonProperty = jsonProperty + "}}";
			jsonProperty = jsonProperty + "]}";

			saveToLog("%s, %s, %s, %s.", "D", tag, "sendToLanModeDevice", "jsonProperty:" + jsonProperty);
			entity.jsonStr = jsonProperty;
			//Push to queue by calling AylaLanMode.sendToLanModeDevice
			module.getSession().sendToLanModeDevice(entity, rs);

			//{
			//	"datapoint":{
			//	"created_at":"2011-11-15T06:22:44Z",
			//	"updated_at":"2011-11-15T06:22:44Z",
			//	"value":0
			//	}
			//}
			datapointJson = 			"{\"datapoint\":{";
			datapointJson = datapointJson + "\"" + kAylaDataPointCreatedAt + "\":" + "\"" + this.createdAt + "\",";
			datapointJson = datapointJson + "\"" + kAylaDataPointUpdatedAt + "\":" + "\"" + this.updatedAt + "\",";
			datapointJson = datapointJson + "\"" + kAylaDataPointValue + "\":" + thisValue ;
			datapointJson = datapointJson + "}}";
		}
		return datapointJson;
	}
	
	// TODO: Move to some common utils class    
	public static void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId) {
		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		
		rs.execute();
	}
}





