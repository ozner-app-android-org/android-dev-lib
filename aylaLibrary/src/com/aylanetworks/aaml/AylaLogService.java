//
//  AylaLogService.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 6/19/2013.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import java.util.LinkedList;
import java.util.Map;

import org.json.JSONObject;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

//----------------------------------------------- Log Service -------------------------------

public class AylaLogService extends AylaSystemUtils {
	
	class Log {
		long time; 		// Time in seconds since 1970-01-01
		String text; 	// The data to be logged by the service, required
		String level; 	// the severity level for this log, required
		String mod;		// The module/area this log pertains to, required
	}
	
	private static LinkedList<Map<String, String>> listOfLogParams = new LinkedList<Map<String, String>>();
	static boolean sendingLogMessage = false;
	
	/**
	 * Send a log message to the service.
	 * Queues log message, best effort, no callback, non-persistent
	 * 
	 * @param callParams 		// log parameters queued for the service, if null, nothing is queued
	 * @param delayExecution	// if true, send the next log in the queue, if false, just queue the log parameters
	 **/
	protected static void sendLogServiceMessage(Map<String, String> callParams, boolean delayExecution) {
		if (callParams != null) {
			enQueueLogs(callParams); // queue up the service log message
		}
		
		if (delayExecution == false) {
			sendNextInLogsQueue();	// send any queued log messages to the service
		}
	}

	/**
	 * Send queued message to the log service
	 * @param mHandle		// Callback handler. Typically sendLogServiceHandler, which is used if null, required
	 * @param callParams
	 * 		String time; 	// Time in seconds since 1970-01-01. Current date & time are used if omitted, optional
	 *		String text; 	// The data to be logged by the service, required
	 *		String level; 	// the severity level for this log. One of info, warning, debug, error, pass, fail, required
	 *		String mod;		// The module/area this log pertains to, required
	 * @return AylaRestService object
	 **/

	private static AylaRestService send(Handler mHandle, Map<String, String>callParams)
	{
		int errCode = AML_USER_INVALID_PARAMETERS;
		AylaRestService rs = null;
		
		JSONObject logObj = new JSONObject();
		JSONObject logValues = new JSONObject();
		JSONObject errors = new JSONObject();
		String paramKey, paramValue;
		
		sendingLogMessage = true;
		if (mHandle == null) {
			mHandle = sendLogServiceHandler; // default handler for best effort service
		}

		try {
			// test validity of required parameters
			paramKey = "dsn";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue == null) {
				errors.put(paramKey, "can't be blank");
			}
			logObj.put(paramKey, paramValue);

			paramKey = "level";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue == null) {
				errors.put(paramKey, "can't be blank");
			}
			logValues.put(paramKey, paramValue);

			paramKey = "mod";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue == null) {
				errors.put(paramKey, "can't be blank");
			}
			logValues.put(paramKey, paramValue);
			
			// return if errors in required fields
			if (errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.CREATE_LOG_IN_SERVICE);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaLogService", ERR_URL, errors.toString(), "send");
				returnToMainActivity(rs, errors.toString(), errCode, 0, false);
				return rs;
			}

			// add optional values
			paramKey = "text";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue == null) {
				paramValue = "{}";
			}
			logValues.put(paramKey, paramValue);

			// use current time if not specified
			long lTime = 0L;
			String sTime;
			paramValue = (String)callParams.get("time");
			if (paramValue == null) {
				lTime = System.currentTimeMillis()/1000;
			} else {
				lTime = Long.parseLong(paramValue, 10);
			}
			sTime = String.valueOf(lTime);
			logValues.put("time", sTime);

			// add log values to log service object
			logObj.put("logs", logValues);

			// wrap it all up in a user object
			String jsonLogService = logObj.toString();

			// POST https://log.aylanetworks.com/api/v1/app/logs.json
			String url = String.format("%s%s", logServiceBaseURL(), "app/logs.json");
			rs = new AylaRestService(mHandle, url, AylaRestService.CREATE_LOG_IN_SERVICE);
			rs.setEntity(jsonLogService);

			if (AylaReachability.isCloudServiceAvailable()) { // ensure service reachability
				rs.execute();
			} else {
				saveToLog("%s, %s, %s:%s, %s", "W", "AylaLogService", "LogService", "not_reachable", "send");
			}
		} catch (Exception e) {
			rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.CREATE_LOG_IN_SERVICE);
			saveToLog("%s, %s, %s:%s, %s", "E", "AylaLogService", "exception", e.getCause(), "create");
			returnToMainActivity(rs, errors.toString(), AML_GENERAL_EXCEPTION, 0, false);
		}

		return rs;
	}
	
	// log wifi status history to the service
	private final static Handler sendLogServiceHandler = new Handler(Looper.getMainLooper()) {
		public void handleMessage(Message msg) {
			sendingLogMessage = false;
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
				AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "I", "AylaLogService", "rc", msg.arg1, "sendLogService_handler");
				
				deQueueLogs(); 			// remove the message just sent from queue
				sendNextInLogsQueue();	// send the next one, if any
			} else {
				// message did not log to service, leave it queued
				AylaSystemUtils.saveToLog("%s, %s, %s:%d %s", "E", "AylaLogService", "rc", msg.arg1, "sendLogService_handler");

				if (msg.arg1 != 401) {		// if authentication failure
					deQueueLogs(); 			// remove the bad message just sent from queue
					sendNextInLogsQueue();	// send the next one, if any
				}
			}
		}		
	};
	
	// FIFO Queue Helper calls
	// Queue service log messages for best effort delivery
	private static void enQueueLogs(Map<String, String> logParams) {
		listOfLogParams.add(logParams);
	}

	private static Map<String, String> nextInLogsQueue() {
		return listOfLogParams.peek(); // get highest priority item
	}

	private static void deQueueLogs() {
		listOfLogParams.poll(); // remove highest priority item
	}
	
	static void clearLogsQueue() {
		listOfLogParams.clear();
	}

	/**
	 * get the next log message in the queue. Send it to the service.
	 */
	private static void sendNextInLogsQueue() {
		Map<String, String> dequeuedLogParams = nextInLogsQueue();
		if (dequeuedLogParams != null)  {
			if (sendingLogMessage != true) {
				send(sendLogServiceHandler, dequeuedLogParams);
			} else {
				saveToLog("%s, %s, %s:%s, %s", "I", "AylaLogService", "sendingLogMessage", "true", "sendNextInLogsQueue");
			}
		}
	}
	
	// TODO: Move to a common utils class.
	static private void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId, Boolean fromDevice) {
		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		
		rs.execute(); // return in main activity
		return;
	}

}



