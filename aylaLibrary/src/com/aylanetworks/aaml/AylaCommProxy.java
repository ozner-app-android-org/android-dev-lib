/*
 * AylaCommProxy.java
 * Ayla Mobile Library
 * 
 * Created by Di Wang on 03/03/2015
 * Copyright (c) 2015 Ayla Networks. All Rights Reserved.
 * */


package com.aylanetworks.aaml;

import java.lang.Override;
import java.lang.reflect.Method;

import android.text.TextUtils;

import com.aylanetworks.aaml.AylaDeviceGateway.AylaGenericNodeConnectionStatus;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;

import java.io.IOException;

import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * So far, this class is used for implementation transition.
 * 
 * If there is zigbee package, use zigbee, or simply use generic, which is on track for now.
 * 
 * Will be deprecated once Generic Gateaway feature is done. 
 * */
public class AylaCommProxy {

	private final static String tag = AylaCommProxy.class.getSimpleName();
	
	protected static final String kAylaCommProxyZigbeeGW = "com.aylanetworks.aaml.zigbee.AylaDeviceZigbeeGateway";
	protected static final String kAylaCommProxyZigbeeNode = "com.aylanetworks.aaml.zigbee.AylaDeviceZigbeeNode";

	private static final String kAylaCommProxySceneZigbee = "com.aylanetworks.aaml.zigbee.AylaSceneZigbee";
	private static final String kAylaCommProxyGroupZigbee = "com.aylanetworks.aaml.zigbee.AylaGroupZigbee";
	private static final String kAylaCommProxyBindingZigbee = "com.aylanetworks.aaml.zigbee.AylaBindingZigbee";

	/**
	 * Check if zigbee support is included in library
	 * @return true is zigbee gateway/node support is available
	 */
	public static Boolean isZigBeeAvailable() {
		return AylaDeviceTypeAdapterFactory.isZigBeeAvailable();
	}

	/**
	 * Initialize the global parser based on capabilities.
	 * */
	public static Gson getParser() {
		final GsonBuilder builder = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
									.excludeFieldsWithoutExposeAnnotation()
									.registerTypeAdapterFactory(new AylaDeviceTypeAdapterFactory<AylaDevice>());
		return builder.create();
	}// end of getParser    
	
	
	/**
	 * This serves particularly zigbee gateway, transforming the response from 
	 * cloud to what spec defines as expected response in app handler. <br/>
	 * 
	 * Cloud reponse is like:<br/>
	 * {"datapoint":<br/>
	 * 		{<br/>
	 * 			"name":"conn_status"<br/>
	 * 			, "type":"string"<br/>
	 * 			, "value":"{"mac":["0x00158D000060F863"],"value":[0]}"<br/>
	 * .....<br/>
	 * }<br/>   
	 * 
	 * Expected response is like:<br/>
	 * {<br/>
	 * 	"conn_status"=[{
	 * 	"dsn"=${node+dsn}
	 * 	, "status"=${Online/Offline}
	 * 	, {${node_connection_status_entity}}], <br/>
	 * 	"dsn"=${gateway_dsn}, <br/>
	 * 	"statusCode"=${statusCode}, <br/>
	 * 	"type"="node", <br/>
	 * }
	 * 
	 * @param response  from cloud
	 * @param GWDsn  gateway dsn
	 * 
	 * @return response string in expected json format, or partial result in a best effort manner if anything wrong in the middle, not null.
	 * */
	// TODO: need to refine the structure here.     
	static String extractNodeConnectionStatusResponse(
			final String response
			, final String GWDsn
			, final int resCode) {
		if (TextUtils.isEmpty(response) || TextUtils.isEmpty(GWDsn)) {
			AylaSystemUtils.saveToLog("%s, %s, %s, %s."
					, "E"
					, tag
					, "extractNodeConnectionStatusResponse"
					, response + " or " + GWDsn + " not valid");
			return "";
		}
		
		AylaDatapointContainer adc = AylaSystemUtils.gson.fromJson(response, AylaDatapointContainer.class);
		if (adc == null || adc.datapoint == null) {
			AylaSystemUtils.saveToLog("%s, %s, %s, %s."
					, "E"
					, tag
					, "extractNodeConnectionStatusResponse"
					, "Parsing error response:" + response);
			return "";
		}
		
		AylaDeviceGateway gw = (AylaDeviceGateway)AylaDeviceManager.sharedManager().deviceWithDSN(GWDsn);
		if ( gw==null ) {
			AylaSystemUtils.saveToLog("%s, %s, %s, %s."
					, "E"
					, tag
					, "extractNodeConnectionStatusResponse"
					, "Cannot find gateway " + GWDsn + " inside AylaDeviceManager");
			return "";
		}
		
		AylaGenericNodeConnectionStatus[] nodeStatus = getNodeConnectionStatusFromString(adc.datapoint.value, gw);
		
		JSONObject result = new JSONObject();
		try {
			JSONArray array = new JSONArray();
			for (int i=0; i<nodeStatus.length; i++) {
				JSONObject obj = new JSONObject();
				
				obj.put("mac", nodeStatus[i].mac);
				if ( !nodeStatus[i].status ) {
					obj.put("status", "Offline");
				} else {
					obj.put("status", "Online");
				}
				obj.put("dsn", nodeStatus[i].dsn);
				array.put(obj);
			}
			result.put("conn_status", array);
			result.put("dsn", GWDsn);
			result.put("statusCode", resCode);
			result.put("type", "node");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result.toString();
	}// end of extractNodeConnectionStatusResponse
	
	
	// TODO: public to expose to and serve zigbee, refine structure here. 
	public static AylaGenericNodeConnectionStatus[] getNodeConnectionStatusFromString(final String jsonValue, AylaDeviceGateway gw) {
		AylaGenericNodeConnectionStatus[] results 
			= new AylaGenericNodeConnectionStatus[0];
		try {
			JSONObject src = new JSONObject(jsonValue);         
			JSONArray macArray = src.getJSONArray("mac");
			JSONArray valueArray = src.getJSONArray("value");
			
			// Assuming macArray and valueArray are always the same length   
			results = new AylaGenericNodeConnectionStatus[macArray.length()];
			for (int i=0; i<results.length; i++) {
				results[i] = new AylaGenericNodeConnectionStatus();
				results[i].mac = macArray.optString(i);
				int v = valueArray.optInt(i);
				if (v==1) {
					results[i].status = true;
				} else {
					results[i].status = false;
				}
				AylaDevice d = gw.findNodeWithMacAddress(results[i].mac);
				if (d!=null) {
					results[i].dsn = d.dsn;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return results;
		}
		return results;
	}// end of getNodeConnectionStatusFromString      
	
	
	/**
	 * Strip group container.
	 * 
	 * @param response the json string including container to be parsed.
	 * @param requestId the action ID defined in AylaRestService
	 * @return empty string "" if no zigbee package, or play the functions in AylaGroupZigbee.stripContainer().
	 * */
	static String stripGroupContainer(final String response, final int requestId) {
		String result = "";
		Class zGroup = null;
		try {
			ClassLoader loader = AylaCommProxy.class.getClassLoader();
			zGroup = loader.loadClass(kAylaCommProxyGroupZigbee);
			Method method = zGroup.getDeclaredMethod("stripContainer", String.class, Integer.class);     
			result = (String)method.invoke(null, response, requestId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Strip group container for an array of groups.
	 * 
	 * @param response the json string including container to be parsed.
	 * @return empty string "" if no zigbee package, or play the functions in AylaGroupZigbee.stripContainers().
	 * */
	static String stripGroupContainers(final String response) {
		String result = "";
		Class zGroup = null;
		try {
			ClassLoader loader = AylaCommProxy.class.getClassLoader();
			zGroup = loader.loadClass(kAylaCommProxyGroupZigbee);
			Method method = zGroup.getDeclaredMethod("stripContainers", String.class);   
			result = (String)method.invoke(null, response);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Strip scene container 
	 * 
	 * @param response the json string including container to be parsed.
	 * @param requestId the action ID defined in AylaRestService
	 * @return empty string "" if no zigbee package, or play the functions in AylaSceneZigbee.stripContainer().
	 * */
	static String stripSceneContainer(final String response, final int requestId) {
		String result = "";
		Class zScene = null;
		try {
			ClassLoader loader = AylaCommProxy.class.getClassLoader();
			zScene = loader.loadClass(kAylaCommProxySceneZigbee);
			Method method = zScene.getDeclaredMethod("stripContainer"
					, String.class, Integer.class);
			result = (String)method.invoke(null, response, requestId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Strip scene container for an array of scenes
	 * 
	 * @param response the json string including container to be parsed.
	 * @return empty string "" if no zigbee package, or play the functions in AylaSceneZigbee.stripContainers().
	 * */
	static String stripSceneContainers(final String response) {
		String result = "";
		Class zScene = null;
		try {
			ClassLoader loader = AylaCommProxy.class.getClassLoader();
			zScene = loader.loadClass(kAylaCommProxySceneZigbee);
			Method method = zScene.getDeclaredMethod("stripContainers", String.class);
			result = (String)method.invoke(null, response);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	
	/**
	 * Strip Binding container for one single or an array of bindings 
	 * 
	 * @param  response the json string including container to be parsed.
	 * @param requestId the action ID defined in AylaRestService
	 * @return empty string "" if no zigbee package, or play the functions in AylaBindingZigbee.stripContainers().
	 * */
	static String stripBindingContainers(final String response, final int requestId) {
		String result = "";
		Class zBinding = null;
		try {
			ClassLoader loader = AylaCommProxy.class.getClassLoader();
			zBinding = loader.loadClass(kAylaCommProxyBindingZigbee);
			Method method = zBinding.getDeclaredMethod("stripContainers", String.class, Integer.class);
			result = (String)method.invoke(null, response, requestId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;      
	}
}// end of AylaCommProxy class    


/**
 * Adapts for any device classes inherted from AylaDevice.
 * Current version supports: AylaDevice, AylaDeviceGateway, AylaDeviceNode,
 * AylaDeviceZigbeeGateway(when having Zigbee support), AylaDeviceZigbeeNode (when having Zigbee support).
 * @param <T> Must pass in AylaDevice
 */
class AylaDeviceTypeAdapterFactory<T> implements TypeAdapterFactory
{
	private static final String kAylaDeviceTypeAdapterDeviceType = "device_type";
	private static final String kAylaDeviceTypeAdapterGatewayType = "gateway_type";
	private static final String kAylaDeviceTypeAdapterNodeType = "node_type";

	private static final String AylaDeviceTypeWifi= "Wifi";
	private static final String AylaDeviceTypeGateway= "Gateway";
	private static final String AylaDeviceTypeNode = "Node";

	private static final String AylaDeviceSubtypeGeneric = "Generic";
	private static final String AylaDeviceSubtypeZigbee = "Zigbee";

	private final Map<Class<?>, TypeAdapter<?>> classToDelegate = new LinkedHashMap<>();
	private final Map<String, Class<?>> classNameToClass = new LinkedHashMap<>();

	private static Boolean isZigBeeAvailable = false;

	/**
	 * Check if zigbee support is included in library
	 * @return true if zigbee gateway/node support is available
	 */
	public static Boolean isZigBeeAvailable() {
		return isZigBeeAvailable;
	}

	@Override
	public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> typeToken) {

		if (typeToken.getRawType() != AylaDevice.class) {
			return null;
		}

		List<Class<?>> classList = new ArrayList<>();
		classList.add(AylaDevice.class);
		classList.add(AylaDeviceGateway.class);
		classList.add(AylaDeviceNode.class);

		ClassLoader loader = AylaCommProxy.class.getClassLoader();
		try {
			Class classGW = loader.loadClass(AylaCommProxy.kAylaCommProxyZigbeeGW); //AylaDeviceZigbeeGateway
			if(classGW != null) classList.add(classGW);

			Class classNode = loader.loadClass(AylaCommProxy.kAylaCommProxyZigbeeNode); //AylaDeviceZigbeeNode
			if(classNode != null) classList.add(classNode);

			isZigBeeAvailable = true;
		} catch (Exception e) {
			isZigBeeAvailable = false;
		}

		for (Class<?> classType : classList) {
			this.classToDelegate.put(classType, gson.getDelegateAdapter(this, TypeToken.get(classType)));
			this.classNameToClass.put(classType.getName(), classType);
		}

		return new TypeAdapter<R>() {
			@Override
			public void write(JsonWriter jsonWriter, R device) throws IOException {
				Class<?> srcType = device.getClass();

				@SuppressWarnings("unchecked") // registration requires that subtype extends T
				TypeAdapter <R> delegate = (TypeAdapter <R>) classToDelegate.get(srcType);

				if(delegate == null) {
					throw new JsonParseException("cannot serialize " + srcType.getName()
					+ "; did you forget to register a subtype?");
				}

				JsonObject jsonObject;
				try {
					jsonObject = delegate.toJsonTree(device).getAsJsonObject();
				}
				catch (Exception e) {
					AylaSystemUtils.saveToLog("%s, %s, %s:%s.", "E", "AylaDeviceTypeAdapterFactory", "Exception", e.toString());
					throw new JsonParseException("cannot serialize" + srcType.getName() + "to Json Tree Exception:"
							+ e.toString());
				}

				if (jsonObject.has(kAylaDeviceTypeAdapterDeviceType)
						|| jsonObject.has(kAylaDeviceTypeAdapterGatewayType)
						|| jsonObject.has(kAylaDeviceTypeAdapterNodeType)) {
					throw new JsonParseException("cannot serialize " + srcType.getName()
							+ " because it already defines a field named "
							+ jsonObject.has(kAylaDeviceTypeAdapterDeviceType)
							+ jsonObject.has(kAylaDeviceTypeAdapterGatewayType)
							+ jsonObject.has(kAylaDeviceTypeAdapterNodeType));
				}

				JsonObject clone = new JsonObject();
				for (Map.Entry<String, JsonElement> e : jsonObject.entrySet()) {
					clone.add(e.getKey(), e.getValue());
				}

				if (device instanceof AylaDeviceGateway) {
					clone.add(kAylaDeviceTypeAdapterDeviceType, new JsonPrimitive(AylaDeviceTypeGateway));
					Class zigbeeClass = classNameToClass.get(AylaCommProxy.kAylaCommProxyZigbeeGW);
					if(zigbeeClass != null && zigbeeClass.isInstance(device)) {
						clone.add(kAylaDeviceTypeAdapterGatewayType, new JsonPrimitive(AylaDeviceSubtypeZigbee));
					}
					else {
						clone.add(kAylaDeviceTypeAdapterGatewayType, new JsonPrimitive(AylaDeviceSubtypeGeneric));
					}
				}
				else if (device instanceof AylaDeviceNode) {
					clone.add(kAylaDeviceTypeAdapterDeviceType, new JsonPrimitive(AylaDeviceTypeNode));
					Class zigbeeClass = classNameToClass.get(AylaCommProxy.kAylaCommProxyZigbeeNode);
					if(zigbeeClass != null && zigbeeClass.isInstance(device)) {
						clone.add(kAylaDeviceTypeAdapterNodeType, new JsonPrimitive(AylaDeviceSubtypeZigbee));
					}
					else {
						clone.add(kAylaDeviceTypeAdapterNodeType, new JsonPrimitive(AylaDeviceSubtypeGeneric));
					}
				}
				else {
					clone.add(kAylaDeviceTypeAdapterDeviceType, new JsonPrimitive(AylaDeviceTypeWifi));
				}

				Streams.write(clone, jsonWriter);
			}

			@Override
			public R read(JsonReader jsonReader) throws IOException {
				JsonElement jsonElement = Streams.parse(jsonReader);
				JsonElement labelJsonElement = jsonElement.getAsJsonObject().remove(kAylaDeviceTypeAdapterDeviceType);

				if (labelJsonElement == null) {
					throw new JsonParseException("cannot deserialize " + " because it does not define a field named "
													+ kAylaDeviceTypeAdapterDeviceType);
				}

				Class classType = AylaDevice.class;
				if(labelJsonElement.getAsString().equals(AylaDeviceTypeGateway)) {
					JsonElement gwTypeJsonElement = jsonElement.getAsJsonObject().remove(kAylaDeviceTypeAdapterGatewayType);
					if ( AylaDeviceTypeAdapterFactory.isZigBeeAvailable
							&& gwTypeJsonElement != null
							&& gwTypeJsonElement.getAsString().equals(AylaDeviceSubtypeZigbee)) {
						Class zigbeeClass = classNameToClass.get(AylaCommProxy.kAylaCommProxyZigbeeGW);
						classType = zigbeeClass != null ? zigbeeClass: AylaDeviceGateway.class;
					}
					else {
						classType = AylaDeviceGateway.class;
					}
				}
				else if(labelJsonElement.getAsString().equals(AylaDeviceTypeNode)) {
					JsonElement nodeTypeJsonElement = jsonElement.getAsJsonObject().remove(kAylaDeviceTypeAdapterNodeType);
					if ( AylaDeviceTypeAdapterFactory.isZigBeeAvailable
							&& nodeTypeJsonElement != null
							&& nodeTypeJsonElement.getAsString().equals(AylaDeviceSubtypeZigbee)) {
						Class zigbeeClass = classNameToClass.get(AylaCommProxy.kAylaCommProxyZigbeeNode);
						classType = zigbeeClass != null ? zigbeeClass: AylaDeviceNode.class;
					}
					else {
						classType = AylaDeviceNode.class;
					}
				}

				@SuppressWarnings("unchecked") // registration requires that subtype extends T
				TypeAdapter<R> delegate = (TypeAdapter<R>)classToDelegate.get(classType);
				return delegate.fromJsonTree(jsonElement);
			}
		};
	}
}// end of AylaDeviceTypeAdapterFactory class

