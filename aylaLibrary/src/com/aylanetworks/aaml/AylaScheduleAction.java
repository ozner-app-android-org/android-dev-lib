//
//  AylaScheduleAction.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 5/27/2013.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import java.text.DecimalFormat;
import java.util.Map;

import org.json.JSONObject;

import com.google.gson.annotations.Expose;

import android.os.Handler;
import android.text.TextUtils;

//----------------------------------------------- Schedule Actions -------------------------------
class AylaScheduleActionContainer {
	@Expose
	public AylaScheduleAction scheduleAction = null;
}

public class AylaScheduleAction extends AylaSystemUtils 
{
	//Properties for Schedule Actions
	@Expose
	public String name; // associated property name, required
	@Expose
	public String type; // "SchedulePropertyAction", required
	@Expose
	public boolean inRange; // true == fire action if time is in the range specified, false == fire on start/end date/time only, optional
	@Expose
	public boolean atStart; // true == fire action if time is at the start of the range specified by the schedule, optional
	@Expose
	public boolean atEnd; // true == fire action if time is at the end of the range specified by the schedule, optional
	@Expose
	public boolean active; // true if this action is used in determining a firing action, required
	@Expose
	public String baseType; // string, integer, boolean, decimal, required
	@Expose
	public String value; // value to set when fired, required
	
	@Expose
	Number key; // required, except for create
	
	public AylaScheduleAction () {
	}

	/**
	 * Same as {@link AylaScheduleAction#create(Handler, AylaSchedule, Boolean)} with no handler to return results.
	 * **/
	public AylaRestService create(AylaSchedule schedule) {
		return create(null, schedule, true);
	}


	/**
	 * The createAction method will create a Schedule Action that is associated with the Schedule object. Consider letting the device.updateSchedule method
	 * create Schedule Actions by passing in newly allocated scheduleAction object(s), or using the Full Template Scheduling model where actions are pre-defined.
	 *
	 * @param mHandle is where result would be returned.
	 * @param schedule is the schedule this action binds to .
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService create(Handler mHandle, AylaSchedule schedule, Boolean delayExecution) 
	{
		int errCode = AML_USER_INVALID_PARAMETERS;
		// https://ads-dev.aylanetworks.com/apiv1/schedules/<sched_key>/schedule_actions.json
		Number key = schedule.key.intValue(); // Handle gson LazilyParsedNumber
		String url = String.format("%s%s%d%s", deviceServiceBaseURL(), "schedules/", key, "/schedule_actions.json");
		AylaRestService rs = null;

		JSONObject errors = new JSONObject();

		try {
			// test validity of required parameters
			if (this.name == null) {
				errors.put("name", "can't be blank"); 
			}

			if (this.type == null) {
				errors.put("type", "can't be blank");
			}

			if (this.value == null) {
				errors.put("value", "can't be blank"); 
			}

			if (this.baseType == null) {
				errors.put("baseType", "can't be blank"); 
			}

			// return if errors in required fields
			if(errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.CREATE_SCHEDULE_ACTION);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaScheduleAction", ERR_URL, errors.toString(), "create");
				returnToMainActivity(rs, errors.toString(), errCode, 0, false);
				return rs;
			}
			saveToLog("%s, %s, %s:%s, %s", "I", "ScheduleAction", "path", url, "create");
			
			// create the scheduleAction object
			AylaScheduleActionContainer scheduleActionContainer = new AylaScheduleActionContainer();
			scheduleActionContainer.scheduleAction = this;
			String jsonScheduleActionContainer = AylaSystemUtils.gson.toJson(scheduleActionContainer, AylaScheduleActionContainer.class);

			rs = new AylaRestService(mHandle, url, AylaRestService.CREATE_SCHEDULE_ACTION);
			rs.setEntity(jsonScheduleActionContainer);
			
			if (delayExecution == false) {
				rs.execute();
			}
		} catch (Exception e) {
			rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.CREATE_SCHEDULE_ACTION);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaSchedule", "exception", e.getCause(), "create");
			returnToMainActivity(rs, errors.toString(), AML_GENERAL_EXCEPTION, 0, false);
    	}
    	
		return rs;
	}

	// -------------------------- Change Schedule Action -----------------------

	/**
	 * Same as {@link AylaScheduleAction#update(Handler, AylaSchedule, Boolean)} with no handler to return results.
	 * **/
	public AylaRestService update(AylaSchedule schedule) {
		return update(null, schedule, true);
	}

	/**
	 * The updateAction method will update a Schedule Action that is associated with the Schedule object. Consider letting the device.updateSchedule method
	 * update Schedule Actions by passing in existing scheduleAction object(s), or using the Full Template Scheduling model.
	 *
	 * @param mHandle is where result would be returned.
	 * @param schedule is the schedule this action binds to .
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService update(Handler mHandle, AylaSchedule schedule, Boolean delayExecution) 
	{
		// https://ads-dev.aylanetworks.com/apiv1/schedule_actions/1.json
		int errCode = AML_USER_INVALID_PARAMETERS;
		Number key = this.key.intValue(); // Handle gson LazilyParsedNumber
		String url = String.format("%s%s%d%s", deviceServiceBaseURL(), "schedule_actions/", key, ".json");
		AylaRestService rs = null;

		JSONObject errors = new JSONObject();
    	
    	try {
    		// test validity of required parameters
			if (this.name == null) {
				errors.put("name", "can't be blank"); 
			}
			
			if (this.value == null) {
				errors.put("value", "can't be blank"); 
			}
			
			if (this.baseType == null) {
				errors.put("baseType", "can't be blank"); 
			}
			
			// return if errors in required fields
			if(errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.UPDATE_SCHEDULE_ACTION);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaScheduleAction", ERR_URL, errors.toString(), "update");
				returnToMainActivity(rs, errors.toString(), errCode, 0, false);
				return rs;
			}
			 
			// All good, update the action
			rs = new AylaRestService(mHandle, url, AylaRestService.UPDATE_SCHEDULE_ACTION);
			
			// create the scheduleAction object
	    	AylaScheduleActionContainer scheduleActionContainer = new AylaScheduleActionContainer();
	    	scheduleActionContainer.scheduleAction = this;
	    	String jsonScheduleActionContainer = AylaSystemUtils.gson.toJson(scheduleActionContainer, AylaScheduleActionContainer.class);
    	
			rs.setEntity(jsonScheduleActionContainer);
			saveToLog("%s, %s, %s:%s, %s", "I", "ScheduleAction", "path", url, "create");
			if (delayExecution == false) {
				rs.execute();
			}

    	} catch (Exception e) {
    		rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.UPDATE_SCHEDULE_ACTION);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaSchedule", "exception", e.getCause(), "update");
			returnToMainActivity(rs, errors.toString(), AML_GENERAL_EXCEPTION, 0, false);
    	}

		return rs;
	}
	protected static String stripContainer(String jsonScheduleActionContainer) throws Exception {
		String jsonScheduleAction = "";
		try {
			AylaScheduleActionContainer scheduleActionContainer = AylaSystemUtils.gson.fromJson(jsonScheduleActionContainer,AylaScheduleActionContainer.class);
			AylaScheduleAction scheduleAction = scheduleActionContainer.scheduleAction;
			
			jsonScheduleAction = AylaSystemUtils.gson.toJson(scheduleAction,AylaScheduleAction.class);
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "ScheduleAction", "scheduleAction", scheduleAction.toString(), "stripContainer");
			
			return jsonScheduleAction;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", "ScheduleAction", "jsonScheduleActionContainer", jsonScheduleActionContainer, "stripContainer");
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Same as {@link AylaScheduleAction#getAll(Handler, AylaSchedule, Map, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public static AylaRestService getAll(AylaSchedule schedule, Map<String, String> callParams) {
		return getAll(null, schedule, callParams, true);
	}

	/**
	 * Same as {@link AylaScheduleAction#getAll(Handler, AylaSchedule, Map, Boolean)} with no option to setup the call to execute later.
	 * **/
	public static AylaRestService getAll(Handler mHandle, AylaSchedule schedule, Map<String, String> callParams) {
		return getAll(mHandle, schedule, callParams, false);
	}

	/**
	 * The getAllActions method will return all Schedule Actions associated with a Schedule object. Consider using the device.getScheduleByName() method retrieve
	 * both the Schedule and associated Schedule Actions.
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param callParams is not required (TBD).
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public static AylaRestService getAll(Handler mHandle, AylaSchedule schedule, Map<String, String> callParams, Boolean delayExecution)
	{
		// https://ads-dev.aylanetworks.com/apiv1/schedules/<sched_key>/schedules_actions.json
		Number key = schedule.key.intValue(); // Handle gson LazilyParsedNumber
		String url = String.format("%s%s%d%s", deviceServiceBaseURL(), "schedules/", key, "/schedule_actions.json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.GET_SCHEDULE_ACTIONS); 

		saveToLog("%s, %s, %s:%s, %s", "I", "ScheduleAction", "url", url, "get");
		if (delayExecution == false) {
			rs.execute(); //GET schedule actions, stripContainers to deserialize
		}
		return rs;
	}
	
	// --------------------- Get By Name -------------------------------

	/**
	 * Same as {@link AylaScheduleAction#getByName(Handler, AylaSchedule, Map, boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService getByName(AylaSchedule schedule, Map<String, String> callParams) {
		return getByName(null, schedule, callParams, true);
	}

	/**
	 * Same as {@link AylaScheduleAction#getByName(Handler, AylaSchedule, Map, boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getByName(Handler mHandle, AylaSchedule schedule, Map<String, String> callParams) {
		return getByName(mHandle, schedule, callParams, false);
	}

	/**
	 * The getActionsByName method will retrieve all Schedule Actions with a given name that are associated with the Schedule object.
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param callParams might contain the name of requested actions
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService getByName(Handler mHandle, AylaSchedule schedule, Map<String, String> callParams, boolean delayExecution)
	{
		String name = "";
		if (callParams != null) { // TBD
			Object obj = callParams.get("name"); 
			name = "?name=" + (String)obj;
		}

		//schedules/122/schedules_actions/find_by_name.json?name=action1
		Number key = schedule.key.intValue(); // Handle gson LazilyParsedNumber
		String url = String.format("%s%s%d%s%s", deviceServiceBaseURL(), "schedules/", key, "/schedule_actions/find_by_name.json", name);
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.GET_SCHEDULE_ACTIONS_BY_NAME); 

		saveToLog("%s, %s, %s:%s, %s", "I", "AylaScheduleAction", "url", url, "getByName");
		if (delayExecution == false) {
			rs.execute(); //GET schedule actions with name, stripContainers to deserialize
		}
		return rs;
	}
	
	protected static String stripContainers(String jsonScheduleActionContainers) throws Exception {
		int count = 0;
		String jsonScheduleActions = "";
		try {
			AylaScheduleActionContainer[] scheduleActionContainers = AylaSystemUtils.gson.fromJson(jsonScheduleActionContainers,AylaScheduleActionContainer[].class);
			AylaScheduleAction[] scheduleActions = new AylaScheduleAction[scheduleActionContainers.length];
			for (AylaScheduleActionContainer scheduleActionContainer : scheduleActionContainers) {
				scheduleActions[count++]= scheduleActionContainer.scheduleAction;   			
			}
			jsonScheduleActions = AylaSystemUtils.gson.toJson(scheduleActions,AylaScheduleAction[].class);
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "ScheduleAction", "count", count, "stripContainers");
			return jsonScheduleActions;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s:%s %s", "E", "ScheduleAction", "count", count, "jsonPropertyContainers", jsonScheduleActionContainers, "stripContainers");
			e.printStackTrace();
			throw e;
		}
	}
	
	// -------------------------- Delete Schedule Action -------------------------

	/**
	 * Same as {@link AylaScheduleAction#delete(Handler, AylaSchedule, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/

	public AylaRestService delete(AylaSchedule schedule) {
		return delete(null, schedule, true);
	}

	/**
	 * The deleteAction method will destroy the Schedule Action associated with the Schedule object. Consider letting the device.updateSchedule() method delete Schedule
	 * Actions, or using the Full Template Scheduling model where action. Active is set to false instead of being deleted. To delete all Actions associated with a given
	 * schedule use the schedule.clear() method which also sets the value of schedule.active to false.
	 *
	 * @param mHandle is where result would be returned.
	 * @param schedule is the schedule this action binds to.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService delete(Handler mHandle, AylaSchedule schedule, Boolean delayExecution)
	{
		AylaRestService rs;
		if (this == null || this.key == null) {
			rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.DELETE_SCHEDULE_ACTION);
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaScheduleAction", "this", "null", "delete");
			returnToMainActivity(rs, "{}", 200, 0, false);
			return rs;
		}
		
		// https://ads-dev.aylanetworks.com/apiv1/schedule_actions/<key>.json
		Number key = this.key.intValue(); // Handle gson LazilyParsedNumber
		String url = String.format("%s%s%d%s", deviceServiceBaseURL(), "schedules/", key, ".json");
		rs = new AylaRestService(mHandle, url, AylaRestService.DELETE_SCHEDULE_ACTION);
		
		saveToLog("%s, %s, %s:%s, %s", "I", "AylaSchedule", "path", url, "delete");
		if (delayExecution == false) {
			rs.execute();
		}
		return rs;
	}
	
	
	// ---------------------------------- Helper methods ------------------------------------
	public void convertValueToType(String sValue, Number nValue) {
		// convert sValue and nValue to value
		if (TextUtils.equals(this.baseType, "integer")) {
			this.value = nValue.toString();
		} else if (TextUtils.equals(this.baseType, "string")) {
			this.value = sValue;
		} else if (TextUtils.equals(this.baseType, "boolean")) {	// checks TBD
			this.value = nValue.toString();
		} else if (TextUtils.equals(this.baseType, "decimal")) {
			DecimalFormat myFormatter = new DecimalFormat("##0.00");
			String decString = myFormatter.format(nValue.doubleValue());
			this.value = decString;
		} else if (TextUtils.equals(this.baseType, "float")) {
			this.value = nValue.toString();
		} else {
			saveToLog("%s, %s, %s:%s, %s", "E", "ScheduleAction", "baseType", baseType, "convertValueToType:unsupported base type");
		}
	}

	// attach scheduleAction to parent and add to list of scheduleActions
	private void lanModeEnable(AylaSchedule schedule) {
		if (lanModeState != lanMode.DISABLED) {
			if (schedule != null && schedule.scheduleActions != null) {

				int len = schedule.scheduleActions.length;
				if (len > DEFAULT_MAX_SCHEDULE_ACTIONS) {
					saveToLog("%s, %s, %s:%d, %s", "E", "AylaScheduleAction", "Too many actions", len, "lanModeEnable");
					return;
				}

				if (len > 0) {
					int i, j;
					AylaScheduleAction schedActions[] = new AylaScheduleAction[len+1];
					for (i = j = 0;i < len; i++) {
						if (schedule.scheduleActions[i] != null) {
							schedActions[i] = schedule.scheduleActions[i];
							j++;
						}
						schedActions[j] = this;
						schedule.scheduleActions = schedActions;
					}
				}
			} else {
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaScheduleAction", "AylaLanMode.device.schedule.scheduleActions", "null", "lanModeEnable");
			}
		}
	}

	private static  void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId, Boolean fromDevice) {
		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		
		rs.execute(); // return in main activity
	}
}    






