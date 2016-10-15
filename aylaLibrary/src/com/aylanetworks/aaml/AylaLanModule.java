/*
 * AylaLanModule.java
 * Ayla Android Mobile Library
 *
 * Created by Brian King on 6/5/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */


package com.aylanetworks.aaml;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.aylanetworks.aaml.enums.IAML_SECURITY_KEY_SIZE;
import com.aylanetworks.aaml.models.AylaCryptoEncapData;
import com.google.gson.annotations.Expose;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

import org.json.JSONObject;

import static com.aylanetworks.aaml.AylaHttpServer.MIME_HTML;
import static com.aylanetworks.aaml.AylaHttpServer.MIME_JSON;
import static com.aylanetworks.aaml.AylaNetworks.AML_REACHABILITY_REACHABLE;
import static com.aylanetworks.aaml.AylaNetworks.AML_REACHABILITY_UNKNOWN;
import static com.aylanetworks.aaml.AylaSystemUtils.bytesToString;
import static com.aylanetworks.aaml.AylaSystemUtils.consoleMsg;
import static com.aylanetworks.aaml.AylaSystemUtils.saveToLog;
import static com.aylanetworks.aaml.AylaSystemUtils.stringToBytes;


public class AylaLanModule {

	private static final String tag = AylaLanModule.class.getSimpleName();
	
    public enum SessionType {
        Normal,
        Setup
    }

    public enum Error {
        NoErr(0x00),
        RequireCloudReachability(0x01),
        LanNotEnabled(0x02),
        InvalidConfigFile(0x03),
        LanConfigEmptyOnCloud(0x04),
        LanConfigNotEnabled(0x05),
        UnmatchedKeyInfo(0x06),
        MobileSessionMsgTimeOut(0x12),
        DeviceNotSupport(0x13),
        DeviceDifferentLan(0x14),
        DeviceResponseError(0x15),
        LibraryNilDevice(0x80),
        LibraryInvalidParam(0x81),
        CloudInvalidResp(0x82);

        private int value;
        Error(int value) {
            this.value = value;
        }
    }

    private String _dsn;
    private AylaDevice _device;
    private AylaLanSession _session;
    private AylaLanModeConfig _lanConfig;
    private String _lanIP;
    private Error _lastError;
    private boolean inSetupMode;
    private AylaEncryption _encryption;
    private int saveLanipKeyId = -1;
    private int seq_no = 0;

    public AylaLanModule(AylaDevice device) {
        _device = device;
        _device.setLanModule(this);
        _session = new AylaLanSession(this);
        _encryption = new AylaEncryption(_device);
    }

    // TODO: zigbee uses this to clear shared queue. try to hide it inside lib. 
    public AylaLanSession getSession() {
        return _session;
    }


    public AylaDevice getDevice(){
        return _device;
    }

    public void lanModeEnable() {
        if ( isLanModeEnabled() ) {
            return;
        }

        int reachability = AylaReachability.getDeviceReachability(_device);

        // Attempt LAN mode if the device is reachable, or we don't know
        if ( reachability == AML_REACHABILITY_REACHABLE || reachability == AML_REACHABILITY_UNKNOWN ) {
            startLanModeSession(AylaRestService.POST_LOCAL_REGISTRATION, false);
        } else {
            saveToLog("%s, %s, %s, %s", "I", "AylaLanModule", _device.dsn, "startLanModeSession: Not reachable");
        }
    }

    public void lanModeDisable() {
        _session.stopSessionTimer();
        setSessionState(AylaLanMode.lanModeSession.DOWN);
    }

    boolean isLanModeEnabled() {
        return sessionState == AylaLanMode.lanModeSession.UP;
    }

    //{"key_exchange":{"ver":1,"random_1": "...","time_1": "...","proto": 1,"key_id": 1}}
    static class AylaKeyExchangeContainer {
        @Expose
        AylaKeyExchange keyExchange;
    }
    static class AylaKeyExchange {
        @Expose
        int ver;
        @Expose
        String random_1;
        @Expose
        long time_1;
        @Expose
        int proto;
        @Expose
        int key_id;
        @Expose
        String sec;
    }

    static class AylaBase64CryptoContainer {
        @Expose
        String enc;
        @Expose
        String sign;
    }
    static class AylaCryptoContainer {
        @Expose
        AylaCryptoEncap enc;
        @Expose
        String sign;
    }
    static class AylaCryptoEncap {
        @Expose
        int seqNo;
        @Expose
        AylaCryptoEncapData data;
    }
    
    static class AylaCryptoNodeConnectionContainer {
    	@Expose
    	AylaNodeConnectionCryptoEncap enc;
    	@Expose
    	String sign;
    }
    static class AylaNodeConnectionCryptoEncap {
    	@Expose 
    	int seqNo;
    	@Expose
    	AylaCryptoNodeConnectionEncapData data;
    }

    static class AylaCryptoNodeConnectionEncapData {
    	@Expose
    	AylaDeviceGateway.AylaGenericNodeConnectionStatus[] connection;
    }

    private boolean _gotLanConfigOnce;
    private AylaLanMode.lanModeSession sessionState = AylaLanMode.lanModeSession.DOWN;

    public AylaLanMode.lanModeSession getSessionState() {
        return sessionState;
    }

    public void setSessionState(AylaLanMode.lanModeSession sessionState) {
        this.sessionState = sessionState;
    }

    protected void lanModeSessionFailed() {
        setSessionState(AylaLanMode.lanModeSession.DOWN);

        String response = null;
        if (_device != null && _device.dsn != null) {
            response = "{\"type\":\""  + AylaSystemUtils.AML_NOTIFY_TYPE_SESSION + "\",\"dsn\":\"" + _device.dsn + "\"}";
        } else {
            response = "{\"type\":\""  + AylaSystemUtils.AML_NOTIFY_TYPE_SESSION + "\",\"dsn\":\"" + "unknown" + "\"}";
            AylaSystemUtils.saveToLog( "%s, %s, %s, device:%s.", "D", "AylaLanModule", "lanModeSessionFailed", "null" );
        }
        
        AylaNotify.returnToMainActivity(null, response, 404, 0, null); // notify app unable to establish lan mode
    }

    protected AylaRestService startLanModeSession(int method, boolean haveDataToSend) {
        return startLanModeSession(method, haveDataToSend, null);
    }

    protected AylaRestService startLanModeSession(int method, boolean haveDataToSend, final byte[] pubKey) {
        String logMsg1 = "", logMsg2 = "";
        saveToLog("%s, %s, %s, %s", "I", "AylaLanModule", "entry", "startLanModeSession");

        _session.sessionTimer();

        //TODO: should check AylaLanMode.isRunning().  This is not valid.      
        if (AylaLanMode.lanModeState != AylaNetworks.lanMode.RUNNING) {
            lanModeSessionFailed(); // v3.00a
            saveToLog("%s, %s, %s:%s, %s", "W", "AylaLanModule", "lanModeState", "!RUNNING", "localRegistration");
            return null;
        }

        if (!_device.lanEnabled) { 	// device is not lan mode enabled
            lanModeSessionFailed(); // v3.00a
            return null;
        }

        // _device is always not null here.
        int r = AylaReachability.getDeviceReachability(_device);
        if (r != AylaNetworks.AML_REACHABILITY_UNKNOWN && r != AylaNetworks.AML_REACHABILITY_REACHABLE ) 
        {
            lanModeSessionFailed(); // v3.00a
            return null;
        }

        if ( r == AylaNetworks.AML_REACHABILITY_UNKNOWN ) {
            AylaReachability.determineReachability(false, _device);
            lanModeSessionFailed();
            return null;
        }

        String url = String.format("%s%s%s", AylaSystemUtils.lanIpServiceBaseURL(_device.lanIp), "local_reg", ".json");
        AylaRestService rs = new AylaRestService(new LocalRegistrationHandler(_device), url, method);

        String msg1 = String.format("%s, %s, %s:%s, %s", "I", "AylaLanModule", "url", url, "localRegistration");
        if (TextUtils.equals(msg1, logMsg1)) {
            consoleMsg(logMsg1, AylaSystemUtils.loggingLevel);
        } else {
            saveToLog("%s", msg1); logMsg1 = msg1;
        }

        // {"local_reg":{"ip":"192.168.0.2","port":10275,"uri":"/local_reg","notify":1}} for normal lanmode.
        // {"local_reg":{"ip":"192.168.0.2","port":10275,"uri":"/local_reg","notify":1, "key":"..."}} for secure wifi setup.

        String jsonText = "";
        String notifyString;
        if (haveDataToSend) {
            notifyString = "\"notify\":1";
        } else {
            notifyString = "\"notify\":0";
        }

        if (pubKey!= null) { // Override haveDataToSend for secure wifi setup.
            notifyString = "\"notify\":1";
        }

        StringBuilder entity = new StringBuilder();

        entity.append("{\"local_reg\":{")
                .append("\"ip\":").append("\"").append(AylaLanMode.serverIpAddress).append("\",")
                .append("\"port\":").append(AylaLanMode.serverPortNumber).append(",")
                .append(notifyString).append(",")
                .append("\"uri\":").append("\"" + AylaLanMode.serverPath + "\"");

        if (pubKey != null) { // for secure wifi setup
            entity.append(",\"key\":\"")
                    .append(AylaEncryptionHelper.encode(pubKey))
                    .append("\"");
        }
        entity.append("}}");
        rs.setEntity(entity.toString());
        String msg2 = String.format(Locale.getDefault(),"%s, %s, entity:%s, %s", "I", tag, entity.toString(), "localRegistration");

        if (TextUtils.equals(msg2, logMsg2)) {
            consoleMsg(logMsg2, AylaSystemUtils.loggingLevel);
        } else {
            saveToLog("%s", msg2); logMsg2 = msg2;
        }
        rs.execute(); // send the session request/extension
        return rs;
    }

    // Called after successful key exchange
    protected void lanModeSessionInit() {
        String logMsg4 = null;
        seq_no = 0;
        setSessionState(AylaLanMode.lanModeSession.UP);
        AylaReachability.setDeviceReachability(_device, AylaNetworks.AML_REACHABILITY_REACHABLE);
        String logMsg= String.format(Locale.getDefault(),"%s, %s, %s:%s, %s", "I", "AylaLanModule", "sessionState", getSessionState(), "lanModeSessionInit");
        if (TextUtils.equals(logMsg, logMsg4)) {
            consoleMsg(logMsg4, AylaSystemUtils.loggingLevel);
        } else {
            saveToLog("%s", logMsg); logMsg4 = logMsg;
        }

        String response = "{\"type\":\""  + AylaSystemUtils.AML_NOTIFY_TYPE_SESSION + "\",\"dsn\":\"" + _device.dsn + "\"}";

        AylaNotify.returnToMainActivity(null, response, 200, 0, null); // notify app to GET current values from device
    }

    // Extend the Lan Mode session
    protected void extendLanModeSession() {
        saveToLog("%s, %s, %s.", "I", tag, "extendLanModeSession");
        startLanModeSession(AylaRestService.PUT_LOCAL_REGISTRATION, false);
    }

    public synchronized AylaHttpServer.Response handleRequest(String uri, AylaHttpServer.Method method, Map<String, String> header, Map<String, String> parms, Map<String, String> files) {

        // TODO: Extract all the uri as constant in proper place.
        if (uri.contains("key_exchange")) { // initiate session
            // parse out key exchange information
            String jsonString = parms.get("jsonString");
            AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaLanMode", "jsonString", jsonString, "Response_keyExchange1");

            AylaKeyExchangeContainer keyExchangeContainer = null;
            try {
                keyExchangeContainer = AylaSystemUtils.gson.fromJson(jsonString, AylaKeyExchangeContainer.class);
            }
            catch (Exception e) {
                AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "E", "AylaLanMode", "jsonString_parseError", jsonString, "Response_keyExchange2");
                e.printStackTrace();
                lanModeSessionFailed(); //v2.13_ENG
                return new AylaHttpServer.Response(AylaHttpServer.Response.Status.BAD_REQUEST, MIME_JSON, jsonString);
            }

            // jsonString = "{\"key_exchange\":{\"ver\":1,\"random_1\":\"abc\",\"time_1\":123,\"proto\":1,\"key_id\":1}}"; // for lanMode keyExchange
            // jsonString = "{\"key_exchange\":{\"ver\":1,\"random_1\":\"abc\",\"time_1\":123,\"proto\":1,\"sec\":"0EC7ade"}}"; // for secure setup.
            if (TextUtils.isEmpty(keyExchangeContainer.keyExchange.sec)) {
                return processLanModeKeyExchange(keyExchangeContainer, jsonString);
            } else {
                return processSecureSetupKeyExchange(keyExchangeContainer, jsonString);
            }
        }// uri.contains("key_exchange") ends here

        else if (uri.contains("commands")) {

            if ( !isLanModeEnabled() ) {
                saveToLog("%s, %s, %s:%s, %s:%s, %s", "W", "AylaLanModule", "serverIpAddress", AylaLanMode.serverIpAddress, "HTTP_FORBIDDEN_403 sessionState", AylaNetworks.sessionStateMsg[sessionState.ordinal()], "Response_commands");
                String html = "<html><head><head><body><h1>" + "LAN mode session is down" + "</h1></body></html>";
                return new AylaHttpServer.Response(AylaHttpServer.Response.Status.FORBIDDEN, MIME_HTML, html);
            }

            AylaLanCommandEntity cmdEntity = _session.nextInSendQueue();

            if (cmdEntity != null && _encryption.appSignKey != null) {
                if (!AylaHttpServer.Method.GET.equals(method)) {
                    saveToLog("%s, %s, %s:%s, %s:%s, %s", "E", "AylaLanModule", "serverIpAddress", AylaLanMode.serverIpAddress, "HTTP_METHOD_FAILURE_405", "error", "Response_commands");
                    String html = "<html><head><head><body><h1>" + "Unsupported command" + "</h1></body></html>";
                    return new AylaHttpServer.Response(AylaHttpServer.Response.Status.METHOD_FAILURE, MIME_HTML, html);
                }

                String jsonText = _encryption.encryptEncapsulateSign(seq_no++, cmdEntity.jsonStr, _encryption.appSignKey);

                AylaLanCommandEntity entity = _session.deQueueSend();

                AylaHttpServer.Response.Status statusCode = AylaHttpServer.Response.Status.OK;
                if (_session.nextInSendQueue() != null) {
                    statusCode = AylaHttpServer.Response.Status.PARTIAL_CONTENT; // let device know there are more properties enqueued
                }

                /*
				 * TODO: For both command and datapoints, need to be consistent like entity/rs pair. 
				 * now only hardcode for SEND_NETWORK_PROFILE_LANMODE 
				 * 
				 * Really need to think of a better exit strategy.
				 * */
                final AylaRestService rs = _session.getCommandsOutstanding(cmdEntity.cmdId + "");
                if ( rs != null && rs.RequestType == AylaRestService.SEND_NETWORK_PROFILE_LANMODE ) {
                	_session.removeCommandsOutstanding(cmdEntity.cmdId + "");

                	new Thread(new Runnable(){
                		public void run() {
                			try {
                				Thread.sleep(1000);
                                // Kill lan mode after the one time send efforts.
                                AylaSetup.exitSecureSetupSession();

                                // Assume the command is always sent to module successfully here.
                                AylaModule.returnToMainActivity(rs, "", 204, AylaRestService.SEND_NETWORK_PROFILE_LANMODE);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                		}
                	}).start();
                }
                
                _device.lanModeWillSendEntity(cmdEntity);
                saveToLog("%s, %s, %s:%s, %s", "I", "AylaLanModule", "commandsJson", jsonText, "Response_commands");
                return new AylaHttpServer.Response(statusCode, MIME_JSON, jsonText);
            } else {
                byte[] sign = {0,1,2,3,4};
                if (_encryption.appSignKey != null) {
                    sign = _encryption.appSignKey;
                } else {
                    saveToLog("%s, %s, %s:%s, %s", "E", "AylaLanModule", "signKey", "null", "Response_commands");
                }
                String jsonText = _encryption.encryptEncapsulateSign(seq_no++,  "{}", sign);
                return new AylaHttpServer.Response(AylaHttpServer.Response.Status.OK, MIME_JSON, jsonText);
            }
        }

        else if (uri.contains("datapoint")) { // receive property change from the device
            String jsonEncodedEncapsulated = parms.get("jsonString");
            AylaBase64CryptoContainer base64CryptoContainer = AylaSystemUtils.gson.fromJson(jsonEncodedEncapsulated,AylaBase64CryptoContainer.class);

            if (base64CryptoContainer == null) { // there is no packet
                saveToLog("%s, %s, %s:%s, %s", "E", "AylaLanModule", "base64Conatiner", "null", "Response_datapoint");
                return new AylaHttpServer.Response(AylaHttpServer.Response.Status.BAD_REQUEST, MIME_JSON, "");
            }

            // enc: base64.decode -> decrypt -> UTF8 -> {seq_no & data}
            String encapText = _encryption.unencodeDecrypt(base64CryptoContainer.enc);
            // String encapText = "{\"seq_no\":0,\"data\":{\"name\":\"Blue_button\",\"value\":1}}";
            if (encapText == null) { // there is no payload
                saveToLog("%s, %s, %s:%s, %s", "E", "AylaLanModule", "encapText", "null", "Response_datapoint");
                return new AylaHttpServer.Response(AylaHttpServer.Response.Status.BAD_REQUEST, MIME_JSON, "");
            }
            byte[] bCryptoEncapText = stringToBytes(encapText, null);

            // compare signatures: sign: base64.decode -> enc.signature
            byte[] bThisDevSignature = AylaEncryption.decode(base64CryptoContainer.sign);
            if (bThisDevSignature == null) { // there is no signature
                saveToLog("%s, %s, %s:%s, %s", "E", "AylaLanModule", "signature", "null", "Response_datapoint");
                return new AylaHttpServer.Response(AylaHttpServer.Response.Status.BAD_REQUEST, MIME_JSON, "");
            }
            String sThisDevSignature = bytesToString(bThisDevSignature,"error converting signature bytes to string");

            // calculate the signature for this packet
            byte[] sign = {0,1,2,3,4};
            if (_encryption.devSignKey != null) {
                sign = _encryption.devSignKey;
            } else {
                saveToLog("%s, %s, %s:%s, %s", "E", "AylaLanModule", "signKey", "null", "Response_datapoint");
            }
            byte[] bCalcDevSignature = AylaEncryption.hmacForKeyAndData(sign, bCryptoEncapText);
            String sCalcDevSignature = bytesToString(bCalcDevSignature,"error converting signature bytes to string");

            if (!sCalcDevSignature.equals(sThisDevSignature)) {
                saveToLog("%s, %s, %s:%s, %s", "E", "AylaLanModule", "signature", AylaHttpServer.Response.Status.UNAUTHORIZED, "Response_datapoint");
                return new AylaHttpServer.Response(AylaHttpServer.Response.Status.UNAUTHORIZED, MIME_JSON, "");
            }

            // --------- authenticated packet -----------------
            _session.stopSessionTimer(); 	// don't send keep-alive for another cycle

            // Get restful parameters (only for command responses)
            AylaRestService rs = null;
            String cmdIdStr = parms.get("cmd_id");
            String cmdStatusStr = parms.get("status");
            int cmdStatus = 200;
            if (!TextUtils.isEmpty(cmdStatusStr)) {
            	// For ack_enable feedback, it is ack_status, cmd_status would be null.
            	cmdStatus = Integer.parseInt(cmdStatusStr);
            }
            
            if (!TextUtils.isEmpty(cmdIdStr)) {
            	rs = _session.getCommandsOutstanding(cmdIdStr);
            	_session.removeCommandsOutstanding(cmdIdStr);
            }

            // create a json object and extract the property
            AylaCryptoEncap cryptoEncap = null;
            AylaCryptoEncapData cryptoEncapData = null;
            try {
                cryptoEncap = AylaSystemUtils.gson.fromJson(encapText, AylaCryptoEncap.class);
                cryptoEncapData = cryptoEncap.data;
            } catch (Exception e) {
            	e.printStackTrace();
                saveToLog("%s, %s, %s:%s, %s", "E", tag, "encapText", encapText, "Response_datapoint.gson");
                _session.oneStatus = 400; // 400 BAD_REQUEST
            }

            if(_device == null) {
                saveToLog("%s, %s, %s:%s, %s", "W", tag, "device", "null", "Response_datapoint.gson");
            }
            else {
            	if (cryptoEncapData == null) {
            		saveToLog("%s, %s, %s, %s.", "E", tag, "handleRequest uri:" + uri, "cryptoEncapData is null: " + encapText);
            		return new AylaHttpServer.Response(AylaHttpServer.Response.Status.NO_CONTENT, MIME_JSON, "");
            	}
            	
            	if ( !TextUtils.isEmpty(cryptoEncapData.id) ) {
            		cmdIdStr = cryptoEncapData.id +"";
            		if (rs == null) {
            			rs = _session.getCommandsOutstanding(cmdIdStr);
            		}
            		AylaLanCommandEntity entity = rs.getCmdEntity(cmdIdStr);
            		if (entity== null) {
            			saveToLog("%s, %s, %s, %s.", "W", tag, "handleRequest", "cannot fetch entity");
            		} else {
            			cryptoEncapData.dsn = entity.dsn;
            			cryptoEncapData.name = entity.propertyName;
            		}
            	}
            	_session.oneStatus = _device.lanModeUpdateProperty(rs, cmdStatus, cryptoEncapData);
            }

            AylaHttpServer.Response.Status finalReturnStatus;
            switch (_session.oneStatus) {
                case 200:
                    finalReturnStatus = AylaHttpServer.Response.Status.OK;
                    break;
                case 400:
                    finalReturnStatus = AylaHttpServer.Response.Status.BAD_REQUEST;
                    break;
                case 404:
                    finalReturnStatus = AylaHttpServer.Response.Status.NOT_FOUND;
                    break;
                case 405:
                	finalReturnStatus = AylaHttpServer.Response.Status.METHOD_NOT_ALLOWED;
                	break;
                default:
                    finalReturnStatus = AylaHttpServer.Response.Status.NOT_IMPLEMENTED;
            }

            if (_session.oneStatus != 200) {
                saveToLog("%s, %s, %s:%s, %s", "W", "AylaLanModule", "HTTP Status", finalReturnStatus.getDescription(), "Response_datapoint");
            } else {
                saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaLanModule", "cmdIdStr", cmdIdStr != null ? cmdIdStr : "null", "fromDeviceStatus", cmdStatusStr != null ? cmdStatusStr : "null", "Response_datapoint");
            }
            _session.sessionTimer();
            return new AylaHttpServer.Response(finalReturnStatus, MIME_JSON, "");
            
        } else if (uri.contains("local_lan/connect_status")) {
            // The uri is what we send to the module, in this case the secure setup session establish request.
            AylaSystemUtils.saveToLog("%s, %s, %s.", "D", "AylaLanMode.serve", "local_lan/connect_status branch");
            return new AylaHttpServer.Response(AylaHttpServer.Response.Status.OK, MIME_HTML, "");
            
        } else if (uri.contains("node/conn_status")) {
        	
        	// This is the response from getNodeConnnectionStatus lan mode command.
        	AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s:%s, %s:%s, %s:%s, %s", "D", tag, "uri", uri,
                    "method", method.toString(), "header", header, "parms", parms, "Response");
        	String jsonEncodedEncapsulated = parms.get("jsonString");
        	String status = parms.get("status");
        	String cmdID = parms.get("cmd_id");
        	if ( TextUtils.isEmpty(jsonEncodedEncapsulated)) {
        		//TODO: make error more detailed, like which param missing.
        		String error = "Parameters missing for node/conn_status";
        		saveToLog("%s, %s, %s, %s", "E", tag, "handleRequest", error);
        		
        		return new AylaHttpServer.Response(AylaHttpServer.Response.Status.PRECONDITION_FAILED, MIME_HTML, error);
        	}
        	
        	int statusCode = 200;
        	if (!TextUtils.isEmpty(status)) {
        		statusCode = Integer.parseInt(status);
        	}
        	AylaRestService rs = null; 
        	if (!TextUtils.isEmpty(cmdID)) {
        		rs = _session.getCommandsOutstanding(cmdID);
        	}
        	String encapText = decryptModuleResponse(jsonEncodedEncapsulated);
        	saveToLog("%s, %s, %s, %s, %s, %s.", "D", tag, "handleRequest node/conn_status branch", "statusCode:" + statusCode, "CmdID:" + cmdID, "encapText:" + encapText);
        	AylaNodeConnectionCryptoEncap cryptoencap = AylaSystemUtils.gson.fromJson(encapText, AylaNodeConnectionCryptoEncap.class);     
        	if ( cryptoencap.data == null ) {
        		saveToLog("%s, %s, %s, %s.", "E", tag, "handleRequest", "cryptoencap.data is null");
        		return new AylaHttpServer.Response(AylaHttpServer.Response.Status.BAD_REQUEST, MIME_HTML, "Parsing error.");
        	}
        	
        	if (! (_device instanceof AylaDeviceGateway) ) {
        		saveToLog("%s, %s, %s, %s.", "E", tag, "handleRequest node/conn_status branch", "_device is not a gateway");
        		return new AylaHttpServer.Response(AylaHttpServer.Response.Status.BAD_REQUEST, MIME_HTML, "Device is not a gateway.");
        	}
        	
        	AylaDeviceGateway gw = (AylaDeviceGateway)_device;
        	AylaDeviceGateway.AylaGenericNodeConnectionStatus[] connStatus = cryptoencap.data.connection;  
        	 
        	statusCode = gw.lanModeUpdateNodeConnectionStatus(rs, statusCode, connStatus);
        	
        	AylaHttpServer.Response.Status finalReturnStatus;
        	switch (statusCode) {
        	case 200:
        		finalReturnStatus = AylaHttpServer.Response.Status.OK;
        		break;
        	case 206:
        		finalReturnStatus = AylaHttpServer.Response.Status.PARTIAL_CONTENT;
        		break;
        	case 400:
        		finalReturnStatus = AylaHttpServer.Response.Status.BAD_REQUEST;
        		break;
        	case 404:
        		finalReturnStatus = AylaHttpServer.Response.Status.NOT_FOUND;
        		break;
        	default:
        		finalReturnStatus = AylaHttpServer.Response.Status.NOT_IMPLEMENTED;
        		saveToLog("%s, %s, %s, %s.", "E", tag, "handleRquest node/conn_status branch", "statusCode not handled");
        	}
        	return new AylaHttpServer.Response(finalReturnStatus, MIME_HTML, "");          
        	
        } else {
            saveToLog("%s, %s, %s:%s, %s:%s, %s", "E", tag, "serverIpAddress", AylaLanMode.serverIpAddress, "parmsBuf", "error", "Response");
            String html = "<html><head><head><body><h1>" + "Unknown URI" + "</h1></body></html>";
            return new AylaHttpServer.Response(AylaHttpServer.Response.Status.NOT_IMPLEMENTED, MIME_HTML, html);
        }
    }// end of handleRequest      
    
    
    /**
     * Decrypt the response from module, and compare the signature.
     * 
     * @param response the encrypted response from module.
     * @return encapText, the exact text from module, or "{"error":"{e.getMessage}"}" if anything wrong. 
     * */
    private String decryptModuleResponse(final String response) {
    	try {
    		AylaBase64CryptoContainer base64CryptoContainer 
    			= AylaSystemUtils.gson.fromJson(response, AylaBase64CryptoContainer.class);
    	
    	if (base64CryptoContainer == null) {
    		saveToLog("%s, %s, %s, %s.", "E", tag, "decryptConnectionStatusResponse", "base64CryptoContainer is null");
    		return AylaHttpServer.Response.Status.BAD_REQUEST.getRequestStatus() + "," + "base64CryptoContainer is null";
    	}
    	
    	// enc: base64.decode -> decrypt -> UTF8 -> {seq_no & data}
    	String encapText = _encryption.unencodeDecrypt(base64CryptoContainer.enc);
    	// encapText is the expected json string.
    	if (encapText == null) {
    		saveToLog("%s, %s, %s:%s, %s", "E", tag, "decryptConnectionStatusResponse", "encapText is null");
    		return AylaHttpServer.Response.Status.BAD_REQUEST.getRequestStatus() + "," + "encapText is null";
    	}
    	byte[] bCryptoEncapText = stringToBytes(encapText, null);
    	
    	// compare signatures: sign: base64.decode -> enc.signature
    	byte[] bThisDevSignature = AylaEncryption.decode(base64CryptoContainer.sign);
    	if (bThisDevSignature == null) { // there is no signature
            saveToLog("%s, %s, %s:%s, %s", "E", tag, "decryptConnectionStatusResponse", "bThisDevSignature is null");
            return AylaHttpServer.Response.Status.BAD_REQUEST.getRequestStatus() + "," + "bThisDevSignature is null";
        }
    	String sThisDevSignature = bytesToString(bThisDevSignature,"error converting signature bytes to string");  
    	
    	// calculate the signature for this packet
    	byte[] sign = {0,1,2,3,4};   
    	if (_encryption.devSignKey != null) {
            sign = _encryption.devSignKey;
        } else {
            saveToLog("%s, %s, %s, %s.", "E", tag, "decryptConnectionStatusResponse", "_encryption.devSignKey is null");
        }
    	byte[] bCalcDevSignature = AylaEncryption.hmacForKeyAndData(sign, bCryptoEncapText);
    	String sCalcDevSignature = bytesToString(bCalcDevSignature,"error converting signature bytes to string");
    	
    	if (!sCalcDevSignature.equals(sThisDevSignature)) {
            saveToLog("%s, %s, %s, %s.", "E", tag, "decryptConnectionStatusResponse", "wrong signature");
            return AylaHttpServer.Response.Status.UNAUTHORIZED.getRequestStatus() + "," + "sCalcDevSignature not equal to sThisDevSignature";
        }
    	
    	return encapText;
    	} catch (Exception e) {
    		e.printStackTrace();
    		return "{\"error\":\"" + JSONObject.quote(e.getMessage()) + "\"}";      
    	}
    }// end of decrypt

    
    

    AylaRestService getLanModeConfig() {
        saveToLog("%s, %s, %s, %s", "I", "AylaLanModeConfig", "entry", "getLanModeConfig");

        if (!_device.lanEnabled) { // v2.30
            lanModeSessionFailed();	// device is not lan mode enabled, notify app, v2.30
            return null; // v2.30
        } // v2.30

        // check storage, else retrieve from cloud service,
        String jsonLanModeConfig = AylaCache.get(AylaCache.AML_CACHE_LAN_CONFIG, _device.dsn); // AylaSystemUtils.loadSavedSetting(lanModeConfigName, "");
        if (AylaReachability.isWiFiConnected(null) && !TextUtils.isEmpty(jsonLanModeConfig) ) {
            // Use saved values
            try {
                AylaLanModeConfig lanModeConfig = AylaSystemUtils.gson.fromJson(jsonLanModeConfig,AylaLanModeConfig.class);
                _device.lanModeConfig = lanModeConfig;

                saveToLog("%s, %s, %s:%s, %s", "I", "AylaLanModeConfig", "lanModeConfigStorage", jsonLanModeConfig, "getLanModeConfig");

                //Set the secure session refresh timer
                int interval = (_device.lanModeConfig.keepAlive.intValue()*1000) - AylaNetworks.AML_LAN_MODE_TIMEOUT_SAFETY; //v2.30
                _session.setTimerInterval(interval); //v2.30

                Boolean haveDataToSend = false; // don't follow key exchange with a get commands
                startLanModeSession(AylaRestService.POST_LOCAL_REGISTRATION, haveDataToSend); // begin a new secure session with this device.
                return null; // v2.30
                //} v2.30
            } catch (Exception e) {
            	e.printStackTrace();
                saveToLog("%s, %s, %s:%s, %s", "E", "AylaLanModeConfig", "lanModeConfigStorage", e.getCause(), "getLanModeConfig");

            }
            lanModeSessionFailed();	// device is not reachable or lanEnabled, notify app, v1.67_ENG
            return null;
        }
        if (AylaReachability.isCloudServiceAvailable()) {
            _device.lanModeConfig = null; // set this in the handler
            if(_device.getKey() ==null) {
                saveToLog("%s, %s, %s:%s, %s", "E", "AylaLanModeConfig", "device key", "null", "getLanModeConfig");
                return null;
            }
            Number devKey = _device.getKey().intValue(); // Handle gson LazilyParsedNumber;
            // String url = "https://ads-dev.aylanetworks.com/apiv1/devices/###/lan.json";
            String url = String.format(Locale.getDefault(), "%s%s%d%s%s", AylaSystemUtils.deviceServiceBaseURL(), "devices/", devKey, "/lan", ".json");
            AylaRestService restService = new AylaRestService(new GetLanModeConfigHandler(_device, this), url, AylaRestService.GET_DEVICE_LANMODE_CONFIG);

            saveToLog("%s, %s, %s:%s, %s", "I", "AylaLanModeConfig", "url", url, "getLanModeConfig");
            restService.execute();

            return restService;
        } else {
            lanModeSessionFailed();	// device is not reachable or lanEnabled, notify app, v1.67_ENG
            return null;
        }
    }

    /* Key exchange for lanMode. */
    private AylaHttpServer.Response processLanModeKeyExchange(final AylaKeyExchangeContainer keyContainer, final String jsonString) {
        // Check preconditions/requirements
        _encryption.version = keyContainer.keyExchange.ver;
        _encryption.proto_1 = keyContainer.keyExchange.proto;
        _encryption.key_id_1 = keyContainer.keyExchange.key_id;

        if (_encryption.version != 1) {
            AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "E", "AylaLanMode", "version", _encryption.version, "Response_keyExchange3");
            lanModeSessionFailed(); //v2.13_ENG
            return new AylaHttpServer.Response(AylaHttpServer.Response.Status.UPGRADE_REQUIRED, MIME_JSON, jsonString);
        }

        if ( _device.lanModeConfig == null ||  _device.lanModeConfig.lanipKeyId == null) {
            AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "W", "AylaLanMode", "lanModeConfig", "null", "Response_keyExchange4");

            if (_gotLanConfigOnce == false) { // v3.00
                _gotLanConfigOnce = true;
                getLanModeConfig(); // try once to get a new lanKeyId
            }
            lanModeSessionFailed(); //v2.13_ENG
            return new AylaHttpServer.Response(AylaHttpServer.Response.Status.METHOD_FAILURE, MIME_JSON, jsonString);
        }
        _gotLanConfigOnce = false;

        int lanipKeyId = _device.lanModeConfig.lanipKeyId.intValue();
        // TODO: module send lanIpKey and random key at the same time?! It is observed. 
        
        // TODO: compare lanIpKey from wrong device/module with lanIpKey received from EVB.
        if (_encryption.key_id_1 != lanipKeyId) 
        {

            if ( AylaLanMode.hasSecureSetupDevice() ) { // secure setup session
                AylaEncryptionHelper helper = AylaEncryptionHelper.getInstance();
                helper.init(IAML_SECURITY_KEY_SIZE.IAML_SECURITY_KEY_SIZE_1024);
                byte[] pub = helper.getPublicKeyPKCS1V21Encoded();

                // pass in public key raw bytes.
                startLanModeSession(
                        AylaRestService.POST_LOCAL_REGISTRATION
                        , true
                        , pub);

            } else { // normal lan mode session
                String lanModeConfigName = AylaCache.AML_CACHE_LAN_CONFIG_PREFIX + _device.dsn;
                AylaCache.save(AylaCache.AML_CACHE_LAN_CONFIG, _device.dsn, "");	// clear existing lan mode config info

                if (saveLanipKeyId != lanipKeyId) {
                    saveLanipKeyId = lanipKeyId;
                    getLanModeConfig(); // try once to get a new lanKeyId
                }

                AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%d, %s:%s, %s", "W", "AylaLanMode",
                        "key_id_1", _encryption.key_id_1, "lanipKeyId", lanipKeyId,
                        "lanModeConfigName", lanModeConfigName, "Response_keyExchange5");
                lanModeSessionFailed(); //v2.13_ENG
            }
            return new AylaHttpServer.Response(AylaHttpServer.Response.Status.PRECONDITION_FAILED, MIME_JSON, jsonString);
        } // end of key_id matching.


        saveLanipKeyId = -1; // reset bad key flag

        if (_encryption.proto_1 != 1) {
            AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "E", "AylaLanMode", "proto", _encryption.proto_1, "Response_keyExchange5");
            lanModeSessionFailed(); //v2.13_ENG
            return new AylaHttpServer.Response(AylaHttpServer.Response.Status.UPGRADE_REQUIRED, MIME_JSON, jsonString);
        }

        // save crypto values from device
        _encryption.sRnd_1 = keyContainer.keyExchange.random_1;
        _encryption.nTime_1 = keyContainer.keyExchange.time_1;

        // generate crypto values for device
        _encryption.sRnd_2 = AylaEncryption.randomToken(16);
        _encryption.nTime_2 = System.nanoTime();

        // generate seed values & new session keys
        int rc = _encryption.generateSessionKeys(null);
        if (rc != AylaNetworks.AML_ERROR_OK) {
            AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%d, %s:%d, %s", "E", "AylaLanMode", "version", _encryption.version,
                    "proto", _encryption.proto_1, "key_id_1", _encryption.key_id_1, "Response_keyExchange");
            AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s:%s, %s", "E", "AylaLanMode", "random_1", _encryption.sRnd_1,
                    "time_1", _encryption.nTime_1, "Response_keyExchange");
            lanModeSessionFailed(); //v2.13_ENG
            return new AylaHttpServer.Response(AylaHttpServer.Response.Status.CERT_ERROR, MIME_JSON, jsonString);
        }

        // successful key generation

        // send app crypto seed values to device
        String jsonText = "{";
        jsonText = jsonText + "\"random_2\":" + "\"" + _encryption.sRnd_2 + "\"";
        jsonText = jsonText + ",\"time_2\":" + _encryption.nTime_2.longValue();
        jsonText = jsonText + "}";
        AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaLanMode", "jsonText", jsonText, "Response_keyExchange6");

        // Begin a new lan mode session
        lanModeSessionInit();

        //return keys
        return new AylaHttpServer.Response(AylaHttpServer.Response.Status.OK, MIME_JSON, jsonText);
    }// end of processLanModeKeyExchange



    /* key exchange for secure setup. */
    private AylaHttpServer.Response processSecureSetupKeyExchange(final AylaKeyExchangeContainer keyContainer, final String jsonString) {

        _encryption.version = keyContainer.keyExchange.ver;
        _encryption.proto_1 = keyContainer.keyExchange.proto;
        _encryption.sec = keyContainer.keyExchange.sec;

        // save crypto values from device
        _encryption.sRnd_1 = keyContainer.keyExchange.random_1;
        _encryption.nTime_1 = keyContainer.keyExchange.time_1;

        // generate crypto values for device
        _encryption.sRnd_2 = AylaEncryption.randomToken(16);
        _encryption.nTime_2 = System.nanoTime();

        // Generate seed values and new session keys.
        byte[] base64DecodedSec = AylaEncryptionHelper.decode(_encryption.sec.getBytes());
        byte[] decrypt = AylaEncryptionHelper.getInstance().decrypt(base64DecodedSec);
        String decryptbased64 = AylaEncryptionHelper.encode(decrypt);
//			String data = new String(decrypt);
        saveToLog("%s, %s, random key:%s.", "D", "AylaLanMode.processSecureSetupKeyExchange", decryptbased64);
        Map<String, String> param = new HashMap<String, String>();
        param.put(AylaEncryption.keyAylaEncryptionType, AylaEncryption.valueAylaEncryptionTypeWifiSetupRSA);
//			param.put(AylaEncryption.keyAylaEncryptionData, data);
        param.put(AylaEncryption.keyAylaEncryptionData, decryptbased64);

        int rc = _encryption.generateSessionKeys(param);
        if (rc != AylaNetworks.AML_ERROR_OK) {
//				AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%d, %s:%d, %s", "E", "AylaLanMode", "version", AylaEncryption.version,
//						"proto", AylaEncryption.proto_1, "key_id_1", AylaEncryption.key_id_1, "Response_keyExchange");
            AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%d, %s", "E", "AylaLanMode", "version", _encryption.version,
                    "key_id_1", _encryption.key_id_1, "Response_keyExchange");
            AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s:%s, %s", "E", "AylaLanMode", "random_1", _encryption.sRnd_1,
                    "time_1", _encryption.nTime_1, "Response_keyExchange");
            lanModeSessionFailed(); //v2.13_ENG
            AylaLanMode.setSecureSetupDevice(null);
            return new AylaHttpServer.Response(AylaHttpServer.Response.Status.CERT_ERROR, MIME_JSON, jsonString);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append("\"random_2\":" + "\"" + _encryption.sRnd_2 + "\"")
                .append(",\"time_2\":" + _encryption.nTime_2.longValue())
                .append("}");

        AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaLanMode", "entity", sb.toString(), "Response_keyExchange_secure_setup");

        // Begin a new Lan Mode session
        lanModeSessionInit();

        // return keys
        return new AylaHttpServer.Response(AylaHttpServer.Response.Status.OK, MIME_JSON, sb.toString());
    }// end of processSecureSetupKeyExchange


    static String logMsg3 = null;
    private final static class LocalRegistrationHandler extends Handler {
        private WeakReference<AylaDevice>_device;
        public LocalRegistrationHandler(AylaDevice device) {
            super(AylaNetworks.appContext.getMainLooper());
            _device = new WeakReference<AylaDevice>(device);
        }

        public void handleMessage(Message msg) {
            AylaDevice device = _device.get();
            if ( device == null ) {
                saveToLog("%s, %s, %s:%s, %s", "E", "AylaLanModule", "lanModeState", "Device gone", "LocalRegistrationHandler");
                return;
            }
            AylaLanModule module = device.getLanModule();
            if ( module == null ) {
                saveToLog("%s, %s, %s:%s, %s", "E", "AylaLanModule", "lanModeState", "No LAN module", "LocalRegistrationHandler");
                return;
            }

            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                if (msg.arg1 == 202) { //The Module will initiate a key exchange to establish or get commands to extend
                   // AylaSetup.newDevice = null;
                    AylaReachability.setDeviceReachability(_device.get(), AylaNetworks.AML_REACHABILITY_REACHABLE);

                    String msg3 = String.format(Locale.getDefault(),"%s, %s, %s:%d, %s", "I", "AylaLanModule", "success", msg.arg1, "localRegistration_handler");
                    if (TextUtils.equals(msg3, logMsg3)) {
                        consoleMsg(logMsg3, AylaSystemUtils.loggingLevel);
                    } else {
                        saveToLog("%s", msg3); logMsg3 = msg3;
                    }
                } else {
                    AylaReachability.setDeviceReachability(_device.get(), AylaNetworks.AML_REACHABILITY_REACHABLE);
                    saveToLog("%s, %s, %s:%d, %s, %s", "E", "AylaLanModule", "UnknownStatus", msg.arg1, msg.obj, "localRegistration_handler");
                }
            } else {
                switch (msg.arg1) {
                    case 400: //400: Forbidden - Bad Request (JSON parse failed)
                        module.setSessionState(AylaLanMode.lanModeSession.DOWN);
                        break;
                    case 403: //403: Forbidden - lan_ip on a different network
                        module.setSessionState(AylaLanMode.lanModeSession.DOWN);
                        break;
                    case 404: //404: Not Found - Lan Mode is not supported by this module
                        module.setSessionState(AylaLanMode.lanModeSession.DOWN);
                        break;
                    case 412: //412: Precondition failed - No lan_ip_key on module
                        if (AylaSetup.newDevice!= null &&  AylaSetup.isRsaKeySupported ) {
                            AylaCache.save(AylaCache.AML_CACHE_LAN_CONFIG, AylaSetup.newDevice.dsn, "");	// clear existing lan mode config info
                            AylaHost.establishSecureWifiSetup (AylaSetup.newDevice.dsn);
                        }
                        break;
                    case 503: //503: Service Unavailable - Insufficient resources or maximum number of sessions exceeded
                        module.setSessionState(AylaLanMode.lanModeSession.DOWN);
                        break;

                    default:
                        module.setSessionState(AylaLanMode.lanModeSession.DOWN);
                        break;
                }

                AylaReachability.setDeviceReachability(_device.get(), AylaNetworks.AML_REACHABILITY_UNREACHABLE);
                module.lanModeSessionFailed(); // v3.00

                saveToLog("%s, %s, %s:%d, %s, %s", "W", "AylaLanModule", "error", msg.arg1, msg.obj, "localRegistration_handler");
            }
        }
    };


    private static class GetLanModeConfigHandler extends Handler {
        AylaDevice _device;
        WeakReference<AylaLanModule> _module;

        GetLanModeConfigHandler(AylaDevice device, AylaLanModule module) {
            super(AylaNetworks.appContext.getMainLooper());
            _device = device;
            _module = new WeakReference<>(module);
        }

        @Override
        public void handleMessage(Message msg) {
            AylaLanModule module = _module.get();
            if ( module == null ) {
                return;
            }

            String jsonResults = (msg.obj != null) ? (String) msg.obj : "";
            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                try {
                    AylaLanModeConfigContainer lanModeConfigContainer = AylaSystemUtils.gson.fromJson(jsonResults, AylaLanModeConfigContainer.class);
                    AylaLanModeConfig lanModeConfig = lanModeConfigContainer.lanip;

                    if (_device != null) {
                        // Save lan mode config info for the session and in preferences
                        _device.lanModeConfig = lanModeConfig;
                        jsonResults = AylaSystemUtils.gson.toJson(_device.lanModeConfig, AylaLanModeConfig.class);
                        AylaCache.save(AylaCache.AML_CACHE_LAN_CONFIG, _device.dsn, jsonResults);

                        //Set the secure session refresh timer
                        int interval = (_device.lanModeConfig.keepAlive.intValue() * 1000) - AylaNetworks.AML_LAN_MODE_TIMEOUT_SAFETY;
                        module._session.setTimerInterval(interval);
                    } else {
                        saveToLog("%s, %s, %s:%s, %s", "E", "AylaLanModeConfig", "AylaLanMode.device", "null", "getLanConfig_handler_OK");
                        module.lanModeSessionFailed();    // device is not lanEnabled, notify app, v1.67
                        return; //v2.30
                    }

                    saveToLog("%s, %s, %s:%s, %s", "I", "AylaLanModeConfig", "lanModeConfig", jsonResults, "getLanConfig_handler_OK");

                    Boolean haveDataToSend = false; // don't follow key exchange with a get commands
                    module.startLanModeSession(AylaRestService.POST_LOCAL_REGISTRATION, haveDataToSend); // begin a new secure session with this device.
                    return;
                } catch (Exception e) {
                	e.printStackTrace();
                    saveToLog("%s, %s, %s:%s, %s", "E", "AylaLanModeConfig", e.getCause(), jsonResults, "getLanConfig_handler_OK_e");

                }
            } else {
                String severity = "E";
                if (msg.arg1 == 404) {    // 404 == lan mode not enabled for this device "Not Found"
                    try {
                        severity = "I";
                        _device.lanModeConfig = new AylaLanModeConfig();
                        _device.lanModeConfig.status = jsonResults;
                        jsonResults = AylaSystemUtils.gson.toJson(_device.lanModeConfig, AylaLanModeConfig.class);
                        saveToLog("%s, %s, %s:%s, %s", "I", "AylaLanModeConfig", "lanModeConfig", jsonResults, "getLanConfig_handler_404");

                        AylaCache.save(AylaCache.AML_CACHE_LAN_CONFIG, _device.dsn, jsonResults);
                        module.lanModeSessionFailed();    // v2.11a
                        return;
                    } catch (Exception e) {
                    	e.printStackTrace();
                        saveToLog("%s, %s, %s:%s, %s", "E", "AylaLanModeConfig", "lanModeConfig", e.getCause(), "getLanConfig_handler_404");

                    }
                }
                saveToLog("%s, %s, %s:%d, %s", severity, "AylaLanModeConfig", "msg", msg.arg1, "getLanConfig_handler_err");

                if (_device != null) {
                    _device.lanModeConfig = null;
                }
            }
            module.lanModeSessionFailed();    // device is not lanEnabled, notify app, v1.67_ENG
        }
    }

    static public class AylaLanSession {
        private WeakReference<AylaLanModule> _module;
        private AylaTimer _sessionTimer;
        private AylaLanMode.lanModeSession _lanModeSession;
        private Error _lastError;
        private SessionType _sessionType;
        private Deque<AylaLanCommandEntity> commandsSendQueue = new ArrayDeque<>();
        private Hashtable<String, AylaRestService> commandsOutstanding = new Hashtable<String, AylaRestService>();
        int commandsOutstandingCount = 0;
        int nextCommandOutstandingId = 0;
        int oneStatus = 200;
        private int sessionInterval = 30000-AylaNetworks.AML_LAN_MODE_TIMEOUT_SAFETY; // default secure session keep-alive value

        public boolean isNotifyOutstanding() {
            return _notifyOutstanding;
        }

        public void setNotifyOutstanding(boolean notifyOutstanding) {
            _notifyOutstanding = notifyOutstanding;
        }

        private boolean _notifyOutstanding = false;


        AylaLanSession(AylaLanModule module) {
            _module = new WeakReference<AylaLanModule>(module);
        }

        /**
         * Used for session extension/keep-alive with the module
         */

        public void sessionTimer() {
            saveToLog("%s, %s, %s:%s, %s", "I", tag, "interval", sessionInterval, "sessionTimer");
            if ( _sessionTimer != null ) {
                stopSessionTimer();
            }

            _sessionTimer = new AylaTimer(sessionInterval, new Runnable() { // allow for delivery/busy time
                public void run() {
                    if ( AylaLanMode.lanModeState != AylaNetworks.lanMode.RUNNING ) {
                        saveToLog("%s, %s, %s:%s, %s", "I", tag, "!RUNNING", sessionInterval, "sessionTimer");
                        stopSessionTimer();
                        return;
                    }
                    
                    AylaLanModule module = _module.get();
                    if (module == null) {
                    	saveToLog("%s, %s, %s, %s.", "E", tag, "AylaTimer.run", "module is null in an active session");
                    	return;
                    }

                    boolean haveDataToSend = commandsSendQueue.size() > 0;
                    module.startLanModeSession(AylaRestService.PUT_LOCAL_REGISTRATION, haveDataToSend );
                }
            });
            _sessionTimer.start();
        }

        public void stopSessionTimer() {
            if ( _sessionTimer != null ) {
                _sessionTimer.stop();
                _sessionTimer = null;
            }
        }

        public void setTimerInterval(int interval) {
            sessionInterval = interval;
            sessionTimer();
        }

        // Save commandId and restService for handling device async response
        void putCommandsOutstanding(int cmdId, AylaRestService rs) {
            if (cmdId != AylaNetworks.AML_COMMAND_ID_NOT_USED) {
                commandsOutstanding.put(Integer.toString(cmdId), rs);
                commandsOutstandingCount++;
            }
        }


        private AylaRestService getCommandsOutstanding(String cmdIdStr) {
            return commandsOutstanding.get(cmdIdStr);
        }

        private void removeCommandsOutstanding(String cmdId) {
            if (commandsOutstanding.remove(cmdId)!=null
                    && commandsOutstandingCount > 0) {
                commandsOutstandingCount--;
            }
        }

        public synchronized int nextCommandOutstandingId() {
            return nextCommandOutstandingId++;
        }

        public void clearCommandsOutstanding() {
            commandsOutstanding.clear();
            commandsOutstandingCount = 0;
        }

        // FIFO Queue
        // Queue command requests for the device
        void enQueueSend(final AylaLanCommandEntity entity) {
            commandsSendQueue.offer(entity); // queue datapoint
        }

        AylaLanCommandEntity nextInSendQueue() {
            return commandsSendQueue.peek(); // get highest priority item
        }

        public AylaLanCommandEntity deQueueSend() {
            return commandsSendQueue.poll(); // remove highest priority item

        }

        public void clearSendQueue() {
            commandsSendQueue.clear();
        }

        public void sendToLanModeDevice(AylaLanCommandEntity entity, AylaRestService rs) {
            if (AylaLanMode.lanModeState == AylaNetworks.lanMode.RUNNING) {

                AylaLanCommandEntity aQueuedEntity = nextInSendQueue();
                saveToLog("%s, %s, %s, %s:%s, %s:%s.", "D", "AylaLanModule", "sendToLanModeDevice", "entity.jsonStr", entity.jsonStr, "baseType", entity.baseType);

                rs.updateCmdEntity(entity.cmdId+"", entity);
                putCommandsOutstanding(entity.cmdId, rs.lanModeInit()); // hash rs for when device responds
                enQueueSend(entity); // queue up the datapoint to send to the device

                if (aQueuedEntity == null) { // already queued up
                    stopSessionTimer();
                    Boolean haveDataToSend = true;
                    AylaLanModule module = _module.get();
                    if ( module != null ) {
                        module.startLanModeSession(AylaRestService.PUT_LOCAL_REGISTRATION, haveDataToSend); // notify device to get commands
                    }
                    setTimerInterval(sessionInterval); // restart the session interval
                }
            } else {
                saveToLog("%s, %s, %s:%s, %s", "E", "AylaLanModule", "lanModeState", AylaNetworks.lanModeMsg[AylaLanMode.lanModeState.ordinal()], "sendToLanModeDevice");
            }
        }
    }// end of AylaLanSession class      
    
}




