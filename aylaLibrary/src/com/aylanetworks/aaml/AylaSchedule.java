//
//  AylaSchedule.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 5/15/13.
//  Copyright (c) 2013 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml; 

import java.util.Map;

import org.json.JSONObject;

import com.google.gson.annotations.Expose;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;


class AylaScheduleContainer {
	@Expose
	public AylaSchedule schedule = null;
}

public class AylaSchedule extends AylaSystemUtils {

	//public AylaScheduleItems schedule; // schedule properties for a schedule object
	@Expose
	Number key; // owner device id, required except for create
	@Expose
	public String direction; //input:to_device, output:from_device, required
	@Expose
	public String name; // name of the schedule, required
	@Expose
	public String displayName; // user facing name of the schedule, optional
	@Expose
	public boolean active; // true/active by default, optional
	@Expose
	public boolean utc; // true/utc tz by default, optional
	@Expose
	public String startDate; // yyyy-mm-dd inclusive, optional
	@Expose
	public String endDate; // yyyy-mm-dd inclusive, optional
	@Expose
	public String startTimeEachDay; // HH:mm:ss inclusive
	@Expose
	public String endTimeEachDay; // HH:mm:ss inclusive, optional
	@Expose
	public int[] daysOfWeek; // 1-7 inclusive, 1 == Sunday, optional
	@Expose
	public int[] daysOfMonth; // 1-32 inclusive, 32 == last day of the month: 28, 29, 30, or 31, optional
	@Expose
	public int[] monthsOfYear; // 1-12 inclusive, 1 == January, optional
	@Expose
	public int[] dayOccurOfMonth; // 1-7 inclusive, 7 == last occurrence of the day in the month, optional
	@Expose
	public int duration; // seconds, default == 0, optional
	@Expose
	public int interval; // seconds, default == 0, optional

	@Expose
	public AylaScheduleAction[] scheduleActions;
	@Expose
	public AylaScheduleAction scheduleAction; 

	public AylaSchedule () {
		scheduleAction = new AylaScheduleAction(); 
	}
	private int numActionsToAttach = 0;
	private int numActionsToDelete = 0;

	// -------------------------- Change Schedule -----------------------
	/**
	 * Same as {@link AylaSchedule#update(Handler, AylaDevice, AylaScheduleAction[])} with no handler to return results.
	 * **/
	AylaRestService update(AylaDevice device, AylaScheduleAction[] actions) {
		return update(null, device, actions);
	}

	/**
	 * This updateSchedule method is used to update/change schedule object and associated Schedule Action properties. When using the Full Template Schedule
	 * Model,(schedules and Actions are pre-created in the OEM template), this method will PUT the data to existing schedule and action instances passed in
	 * as parameters. When using the Dynamic Template Schedule Model, (schedules are precreated in the OEM template, Schedule Actions are dynamically created
	 * and deleted), this method will create and delete the Actions as required if newly allocated scheduleAction object(s) are passed in as parameters.
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results.
	 * @param actions is the array of new actions updated to current schedule.
	 * @return AylaRestService object.
	 */
	AylaRestService update(Handler mHandle, AylaDevice device, AylaScheduleAction[] actions) {

		// https://ads-dev.aylanetworks.com/apiv1/devices/<dev_key>/schedules/<sched_key>.json
		int count = 0;
		String aName = "none";
		int errCode = AML_USER_INVALID_PARAMETERS;
		Number devKey = device.getKey().intValue(); // Handle gson LazilyParsedNumber
		Number schedKey = this.key.intValue();
		String url = String.format("%s%s%d%s%d%s", deviceServiceBaseURL(), "devices/", devKey, "/schedules/", schedKey, ".json");
		AylaRestService rs = null;

		JSONObject errors = new JSONObject();

		try {
			// test validity of required parameters
			if (this.name == null) {
				errors.put("name", "can't be blank"); 
			}

			if (this.startTimeEachDay == null) {
				errors.put("startTimeEachDay", "can't be blank");
			}

			if (this.direction == null) {
				errors.put("direction", "can't be blank"); 
			}

			if (actions != null && actions.length > DEFAULT_MAX_SCHEDULE_ACTIONS) {
				errors.put("allocation", "too many actions");
				errCode =  AML_ALLOCATION_FAILURE;
			}

			// return if errors in required fields
			if(errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.UPDATE_SCHEDULE);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaSchedule", ERR_URL, errors.toString(), "update");
				returnToMainActivity(rs, errors.toString(), errCode, 0, false);
				return rs;
			}

			// All good, update the schedule

			rs = new AylaRestService(mHandle, url, AylaRestService.UPDATE_SCHEDULE);
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaSchedule", "sched.name", this.name, "update");

			// no actions
			if (actions == null) {
				updateContinue(mHandle, rs);
				return rs;
			}
			
			// copy actions
			this.scheduleActions = new AylaScheduleAction[actions.length];
			for (AylaScheduleAction action : actions) {
				this.scheduleActions[count] = action;					// copy the action values
				count++;
			}

			if (actions[0].key != null) { // Full Template model, actions preExist
				updateContinue(mHandle, rs);
			} else { // Dynamic Actions model, create actions first
				numActionsToAttach = actions.length;
				saveOrigRS = null;
				createActions(mHandle, rs, this);
			}
			
			if (actions[0].name != null) {
				aName = actions[0].name;
			}
			saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaSchedule", "sched.name", this.name, "action.name", aName, "update");

		} catch (Exception e) {
			rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.UPDATE_SCHEDULE);
			saveToLog("%s, %s, %s:%s, %s", "E", "AylaSchedule", "exception", e.getCause(), "update");
			returnToMainActivity(rs, errors.toString(), AML_GENERAL_EXCEPTION, AML_UPDATE_SCHEDULE , false);
		}

		return rs;
	}

	/**
	 * Create schedule actions, attach to this schedule
	 * Iterate until all are created
	 * @param mHandle handler to return results.
	 * @param OrigRS
	 * @return
	 */
	private static AylaRestService saveOrigRS;
	private static AylaSchedule saveSchedule;
	private AylaRestService createActions(Handler mHandle, AylaRestService OrigRS, AylaSchedule schedule) {
		if (saveOrigRS == null) {
			saveOrigRS = OrigRS;
			saveSchedule = schedule;
		}
		AylaRestService rs = saveOrigRS;
		try {
			if (numActionsToAttach-- > 0) {
				this.scheduleActions[numActionsToAttach].create(createActionsHandler, this, false); // create actions, ignore delay
				saveToLog("%s, %s, %s:%s, %s", "I", "AylaSchedule", "sched.name", this.name, "createActions");
			} else {
				updateContinue(mHandle, rs); // actions created, continue with update
			}
		} catch (Exception e) {
			rs = new AylaRestService(saveOrigRS.mHandler, ERR_URL, AylaRestService.CREATE_SCHEDULE_ACTIONS);
			saveToLog("%s, %s, %s:%s, %s", "E", "AylaSchedule", "exception", e.getCause(), "createActions");
			returnToMainActivity(rs, e.getCause().toString(), AML_GENERAL_EXCEPTION, AML_CREATE_SCHEDULE_ACTIONS, false);
		}

		return rs;
	}

	private static final Handler createActionsHandler = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			doCreateActionsHandler(msg);
		}
	};
	
	private static void doCreateActionsHandler(Message msg) {
		String jsonResults = (String)msg.obj;
		if (msg.what == AylaNetworks.AML_ERROR_OK) {
			saveSchedule.createActions(createActionsHandler, null, null);
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaSchedule", "Action Created", "OK", "createActions_handler");
		} else {
			AylaRestService rs = new AylaRestService(saveOrigRS.mHandler, ERR_URL, AylaRestService.CREATE_SCHEDULE_ACTIONS);
			saveToLog("%s, %s, %s:%s, %s", "E", "AylaSchedule", "exception", jsonResults, "createActions_handler");
			returnToMainActivity(rs, jsonResults, msg.arg1, AML_CREATE_SCHEDULE_ACTIONS, false);
		}
	}

	private AylaRestService updateContinue(Handler mHandle, AylaRestService rs) {
		try {	
			// create the schedule object
			AylaScheduleContainer scheduleContainer = new AylaScheduleContainer();
			scheduleContainer.schedule = this;
			if (scheduleContainer.schedule.scheduleActions[0].key == null) { 	// Dynamic actions model
				scheduleContainer.schedule.scheduleActions = null;				// actions were just created and don't need updating
			}
			scheduleContainer.schedule.scheduleAction = null;					// remove schedule action v3.08_ENG
			String jsonScheduleContainer = AylaSystemUtils.gson.toJson(scheduleContainer, AylaScheduleContainer.class);

			rs.setEntity(jsonScheduleContainer);
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaSchedule", "sched.name", this.name, "updateContinue");

			if (mHandle != null) {
				rs.execute();
			}

		} catch (Exception e) {
			rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.UPDATE_SCHEDULE);
			saveToLog("%s, %s, %s:%s, %s", "E", "AylaSchedule", "exception", e.getCause(), "updateContinue");
			returnToMainActivity(rs, e.getCause().toString(), AML_GENERAL_EXCEPTION, AML_SCHEDULE_UPDATE_CONTINUE, false);
		}

		return rs;
	}
	
	// --------------------- Get By Name -------------------------------
	/**
	 * Same as {@link AylaSchedule#getByName(Handler, AylaDevice, Map, boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	protected static AylaRestService getByName(AylaDevice device, Map<String, String> callParams) {
		return getByName(null, device, callParams, true);
	}

	/**
	 * Same as {@link AylaSchedule#getByName(Handler, AylaDevice, Map, boolean)} with no handler to return results.
	 * **/
	protected static AylaRestService getByName(Handler mHandle, AylaDevice device, Map<String, String> callParams) {
		return getByName(mHandle, device, callParams, false);
	}

	/**
	 * The method results in the schedule matching the given name being returned to the handler. The AylaSchedule instance includes the schedule properties
	 * and the associated Schedule Actions. This method is typically used to provide complete schedule information for a top-level schedule selected from a
	 * list populated by the getAllSchedules method.
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results.
	 * @param callParams must contain required parameter(s) by this method. Please read the mobile library document for details.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object.
	 */
	protected static AylaRestService getByName(Handler mHandle, AylaDevice device, Map<String, String> callParams, boolean delayExecution)
	{
		String name = "";
		if (callParams != null) { // TBD
			Object obj = callParams.get("name"); //HashMap hm = (HashMap) obj; System.out.println(hm);
			name = "?name=" + (String)obj;
		}

		//devices/122/schedules/find_by_name.json?name=sched1
		Number key = device.getKey().intValue(); // Handle gson LazilyParsedNumber
		String url = String.format("%s%s%d%s%s", deviceServiceBaseURL(), "devices/", key, "/schedules/find_by_name.json", name);
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.GET_SCHEDULE, device.dsn);

		saveToLog("%s, %s, %s:%s, %s", "I", "AylaSchedule", "url", url, "get");
		if (delayExecution == false) {
			rs.execute(); //GET schedule with name, stripContainer to deserialize
		}
		return rs;
	}
	
	// Strip the class container
	protected static String stripContainer(String jsonScheduleContainer, String dsn) throws Exception {
		String jsonSchedule = "";
		try {
			AylaSchedule aylaSchedule = new AylaSchedule();
			AylaDevice device = AylaDeviceManager.sharedManager().deviceWithDSN(dsn);
			AylaScheduleContainer scheduleContainer = AylaSystemUtils.gson.fromJson(jsonScheduleContainer,AylaScheduleContainer.class);
			if (scheduleContainer != null) {
				aylaSchedule = scheduleContainer.schedule; 

				jsonSchedule = AylaSystemUtils.gson.toJson(aylaSchedule, AylaSchedule.class);
				AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "AylaSchedule", "schedule", aylaSchedule.toString(), "stripContainer");

				aylaSchedule.lanModeEnable(device);  // maintain model relationship

				return jsonSchedule;
			} else {
				return null;
			}
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", "AylaSchedule", "jsonScheduleContainer", jsonScheduleContainer, "stripContainer");
			e.printStackTrace();
			throw e;
		}
	}

	// --------------------- Get All Schedules -------------------------------
	/**
	 * Same as {@link AylaSchedule#getAll(Handler, AylaDevice, Map, boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	protected static AylaRestService getAll(AylaDevice device, Map<String, String> callParams) {
		return getAll(null, device, callParams, true);
	}

	/**
	 * Same as {@link AylaSchedule#getAll(Handler, AylaDevice, Map, boolean)} with no option to setup the call to execute later.
	 * **/
	protected static AylaRestService getAll(Handler mHandle, AylaDevice device, Map<String, String> callParams) {
		return getAll(mHandle, device, callParams, false);
	}

	/**
	 * This method results in all schedules for a given device object being return to successBlock. Each AylaSchedule array member instance includes only
	 * the schedule properties and not the associated Schedule Actions. This method is typically used to provide a top-level listing of available schedules
	 * from which the end user selects.
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param callParams must contain required parameter(s) by this method. Please read the mobile library document for details.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event
	 * @return AylaRestService object
	 */
	protected static AylaRestService getAll(Handler mHandle, AylaDevice device, Map<String, String> callParams, boolean delayExecution)
	{
		String name = "";
		if (callParams != null) { // TBD
			Object obj = callParams.get("name"); 
			name = "?name=" + (String)obj;
		}

		//devices/122/schedules.json?name=sched1
		Number key = device.getKey().intValue(); // Handle gson LazilyParsedNumber
		String url = String.format("%s%s%d%s%s", deviceServiceBaseURL(), "devices/", key, "/schedules.json", name);
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.GET_SCHEDULES, device.dsn);

		saveToLog("%s, %s, %s:%s, %s", "I", "AylaSchedule", "url", url, "get");
		if (delayExecution == false) {
			rs.execute(); //GET schedules, stripContainers to deserialize
		}
		return rs;
	}
	protected static String stripContainers(String jsonScheduleContainers) throws Exception {
		int count = 0;
		String jsonSchedules = "[]";
		try {
			AylaScheduleContainer[] scheduleContainers = AylaSystemUtils.gson.fromJson(jsonScheduleContainers,AylaScheduleContainer[].class);
			if (scheduleContainers != null) {
				AylaSchedule[] schedules = new AylaSchedule[scheduleContainers.length];
				for (AylaScheduleContainer scheduleContainer : scheduleContainers) {
					schedules[count++]= scheduleContainer.schedule;   			
				}
				jsonSchedules = AylaSystemUtils.gson.toJson(schedules,AylaSchedule[].class);
			}
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", "AylaSchedules", "count", count, "stripScheduleContaniers");
			return jsonSchedules;
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s:%s %s", "E", "AylaSchedule", "count", count, "jsonScheduleContainers", jsonScheduleContainers, "stripScheduleContainers");
			e.printStackTrace();
			throw e;
		}
	}

	// -------------------------- Clear Schedule -------------------------
	/**
	 * Same as {@link AylaSchedule#clear(Handler, boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService clear() {
		return clear(null, true);
	}

	/**
	 * Same as {@link AylaSchedule#clear(Handler, boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService clear(Handler mHandle) {
		return clear(mHandle, false);
	}

	/**
	 * The clear method will delete the Schedule Actions associated with the Schedule instance and also set the schedule.active value to false. Consider the clear() method a
	 * virtual delete method for the Dynamic Action Schedule Model. DO NOT use clear when implementing the Full Template model as it will delete the Actions. Instead, simply
	 * set schedule.active (and optionally the associated scheduleAction[].active values) to false using the schedule.update() method.
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService clear(Handler mHandle, boolean delayExecution)
	{
		AylaRestService rs;
		if (this == null || this.key == null) {
			rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.CLEAR_SCHEDULE);
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaSchedule", "this", "null", "clear");
			returnToMainActivity(rs, "{}", 200, 0, false);
			return rs;
		}

		// PUT /apiv1/schedules/<schedule_id>/clear.json
		Number key = this.key.intValue(); // Handle gson LazilyParsedNumber
		String url = String.format("%s%s%d%s", deviceServiceBaseURL(), "schedules/", key, "/clear.json");
		rs = new AylaRestService(mHandle, url, AylaRestService.CLEAR_SCHEDULE);

		this.lanModeDisable(); // maintain model relationship

		saveToLog("%s, %s, %s:%s, %s", "I", "AylaSchedule", "path", url, "clearSchedule");
		if (delayExecution == false) {
			rs.execute();
		}
		return rs;
	}

	/**
	 * Same as {@link AylaSchedule#deleteAllActions(Handler, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService deleteAllActions() {
		return deleteAllActions(null, true);
	}

	/**
	 * Same as {@link AylaSchedule#deleteAllActions(Handler, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService deleteAllActions(Handler mHandle) {
		return deleteAllActions(mHandle, false);
	}

	/**
	 * Deletes all actions associated with this schedule
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService deleteAllActions(Handler mHandle, Boolean delayExecution) {
		// By default Schedule delete action request should not be sent to the Cloud for now. 
		AylaRestService rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.DELETE_SCHEDULE_ACTIONS);
		if (this == null || this.key == null || this.scheduleActions == null) {
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaSchedule", "this", "null", "deleteAllActions");
			returnToMainActivity(rs, "{}", 200, 0, false);
			return rs;
		}
		try {
			numActionsToDelete = this.scheduleActions.length;
			saveOrigRS = null;
			deleteActions(mHandle, rs, delayExecution);

			saveToLog("%s, %s, %s:%s, %s", "I", "AylaSchedule", "sched.name", this.name, "deleteAllActions");
		} catch (Exception e) {
			rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.DELETE_SCHEDULE_ACTIONS);
			saveToLog("%s, %s, %s:%s, %s", "E", "AylaSchedule", "exception", e.getCause(), "deleteAllActions");
			returnToMainActivity(rs, e.getCause().toString(), AML_GENERAL_EXCEPTION, 0, false);
		}

		return rs;
	}

	/**
	 * Delete schedule actions, attached to this schedule.
	 * 
	 * @param mHandle is where result would be returned.
	 * @param OrigRS
	 * @return AylaRestService object
	 */
	private AylaRestService deleteActions(Handler mHandle, AylaRestService OrigRS, boolean delayExecution) {
		if (saveOrigRS == null) {
			saveOrigRS = OrigRS;
			saveSchedule = this;
		}
		AylaRestService rs = saveOrigRS;
		try {
			if (numActionsToDelete-- > 0) {
				scheduleActions[numActionsToDelete].delete(deleteActionsHandler, this, false);
				saveToLog("%s, %s, %s:%s, %s", "I", "AylaSchedule", "sched.name", this.name, "deleteActions");
			} else {
				// all actions deleted
				AylaScheduleContainer scheduleContainer = new AylaScheduleContainer();
				scheduleContainer.schedule = this;
				scheduleContainer.schedule.scheduleActions = null;
				String jsonScheduleContainer = AylaSystemUtils.gson.toJson(scheduleContainer, AylaScheduleContainer.class);

				rs.setEntity(jsonScheduleContainer);

				saveToLog("%s, %s, %s:%s, %s", "I", "AylaSchedule", "sched.name", this.name, "deleteActions");
				if (delayExecution == false) {
					rs.execute();
				}
			}
		} catch (Exception e) {
			rs = new AylaRestService(saveOrigRS.mHandler, ERR_URL, AylaRestService.DELETE_SCHEDULE_ACTIONS);
			saveToLog("%s, %s, %s:%s, %s", "E", "AylaSchedule", "exception", e.getCause(), "deleteActions");
			returnToMainActivity(rs, e.getCause().toString(), AML_GENERAL_EXCEPTION, AML_DELETE_SCHEDULE_ACTIONS, false);
		}
		return rs;
	}

	private static final Handler deleteActionsHandler = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			String jsonResults = (String)msg.obj;
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
				// In case saveOrigRS is null.  
				AylaRestService rs = new AylaRestService(this, ERR_URL, AylaRestService.DELETE_SCHEDULE_ACTIONS);
				saveSchedule.deleteActions(this, rs, false); // delete next scheduleAction
				saveToLog("%s, %s, %s:%s, %s", "I", "AylaSchedule", "Action Deleted", "OK", "deleteActions_handler");
			} else {
				AylaRestService rs = new AylaRestService(saveOrigRS.mHandler, ERR_URL, AylaRestService.DELETE_SCHEDULE_ACTIONS);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaSchedule", "exception", jsonResults, "createActions_handler");
				returnToMainActivity(rs, jsonResults, msg.arg1, AML_DELETE_SCHEDULE_ACTIONS, false);
			}
		}
	};

	// --------------------------- Pass Through Methods -------------------------------------
	/**
	 * Same as {@link AylaSchedule#createAction(Handler, AylaScheduleAction, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService createAction(AylaScheduleAction action) {
		return action.create(null, this, true);
	}

	/**
	 * Same as {@link AylaSchedule#createAction(Handler, AylaScheduleAction, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService createAction(Handler mHandle, AylaScheduleAction action) {
		return action.create(mHandle, this, false);
	}

	/**
	 * The createAction method will create a Schedule Action that is associated with the Schedule object. Consider letting the device.updateSchedule method
	 * create Schedule Actions by passing in newly allocated scheduleAction object(s), or using the Full Template Scheduling model where actions are pre-defined.
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param action is the Shedule Action to be created.
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService createAction(Handler mHandle, AylaScheduleAction action, Boolean delayExecution) {
		return action.create(mHandle, this, delayExecution);
	}

	/**
	 * Same as {@link AylaSchedule#updateAction(Handler, AylaScheduleAction, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService updateAction(AylaScheduleAction action) {
		return action.update(null, this, true);
	}

	/**
	 * Same as {@link AylaSchedule#updateAction(Handler, AylaScheduleAction, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService updateAction(Handler mHandle, AylaScheduleAction action) {
		return action.update(mHandle, this, false);
	}

	/**
	 * The updateAction method will update a Schedule Action that is associated with the Schedule object. Consider letting the device.updateSchedule method
	 * update Schedule Actions by passing in existing scheduleAction object(s), or using the Full Template Scheduling model.
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param action is the Shedule Action set to desired values .
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService updateAction(Handler mHandle, AylaScheduleAction action, Boolean delayExecution) {
		return action.update(mHandle, this, delayExecution);
	}

	/**
	 * Same as {@link AylaSchedule#getAllActions(Handler, Map, boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService getAllActions(Map<String, String> callParams) {
		return AylaScheduleAction.getAll(null, this, callParams, true); 
	}

	/**
	 * Same as {@link AylaSchedule#getAllActions(Handler, Map, boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getAllActions(Handler mHandle, Map<String, String> callParams) {
		return AylaScheduleAction.getAll(mHandle, this, callParams, false); 
	}

	/**
	 * The getAllActions method will return all Schedule Actions associated with a Schedule object. Consider using the device.getScheduleByName() method retrieve
	 * both the Schedule and associated Schedule Actions.
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results.
	 * @param callParams is not required (TBD).
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService getAllActions(Handler mHandle, Map<String, String> callParams, boolean delayExecution) {
		return AylaScheduleAction.getAll(mHandle, this, callParams, delayExecution); 
	}

	/**
	 * Same as {@link AylaSchedule#getActionsByName(Handler, Map, boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService getActionsByName(Map<String, String> callParams) {
		return this.scheduleAction.getByName(null, this, callParams, true);
	}

	/**
	 * Same as {@link AylaSchedule#getActionsByName(Handler, Map, boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService getActionsByName(Handler mHandle, Map<String, String> callParams) {
		return this.scheduleAction.getByName(mHandle, this, callParams, false);
	}

	/**
	 * The getActionsByName method will retrieve all Schedule Actions with a given name that are associated with the Schedule object.
	 *
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param callParams might contain the name of requested actions
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService getActionsByName(Handler mHandle, Map<String, String> callParams, boolean delayExecution) {
		return this.scheduleAction.getByName(mHandle, this, callParams, delayExecution);
	}

	/**
	 * Same as {@link AylaSchedule#deleteAction(Handler, AylaScheduleAction, Boolean)} with no handler to return results and no option to setup the call to execute later.
	 * **/
	public AylaRestService deleteAction(AylaScheduleAction action) {
		return deleteAction(null, action, true);
	}

	/**
	 * Same as {@link AylaSchedule#deleteAction(Handler, AylaScheduleAction, Boolean)} with no option to setup the call to execute later.
	 * **/
	public AylaRestService deleteAction(Handler mHandle, AylaScheduleAction action) {
		return deleteAction(mHandle, action, false);
	}

	/**
	 * The deleteAction method will destroy the Schedule Action associated with the Schedule object. Consider letting the device.updateSchedule() method delete Schedule
	 * Actions, or using the Full Template Scheduling model where action. Active is set to false instead of being deleted. To delete all Actions associated with a given
	 * schedule use the schedule.clear() method which also sets the value of schedule.active to false.
	 * @param mHandle is where async results are returned. If null, restServiceObject.execute() provides synchronous results
	 * @param action is the action to be deleted
	 * @param delayExecution could be set to true if you want setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService deleteAction(Handler mHandle, AylaScheduleAction action, Boolean delayExecution) {
		return action.delete(mHandle, this, delayExecution);
	}
	
	// ---------------------------------- Helper methods ------------------------------------
	
   // Return a schedule action matching the property name
	public AylaScheduleAction findAction(String actionName) {
		if (this.scheduleAction != null) {
			for (AylaScheduleAction scheduleAction: this.scheduleActions) {
				if (TextUtils.equals(scheduleAction.name, actionName)) {
					return scheduleAction;
				}
			}
		}
		return null;
	}

	// attach schedule to parent and add to list of schedules
	private void lanModeEnable(AylaDevice device) {

		if (lanModeState != lanMode.DISABLED) {
			if (device != null ) {
				device.schedule = this;

				if (device.schedules != null) {
					int len = device.schedules.length;
					if (len > DEFAULT_MAX_SCHEDULES) {
						saveToLog("%s, %s, %s:%d, %s", "E", "AylaSchedule", "length", len, "lanModeEnable");
						return;
					}

					if (len > 0) {
						int i, j;
						AylaSchedule scheds[] = new AylaSchedule[len+1];
						for (i = j = 0;i < len; i++) {
							if (device.schedules[i] != null) {
								scheds[i] = device.schedules[i];
								j++;
							}
							scheds[j] = this;
							device.schedules = scheds;
						}
					}
				}
			} else {
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaSchedule", "AylaLanMode.device", "null", "lanModeEnable");
			}
		}
	}

	// remove schedule from parent and list of schedules
	void lanModeDisable() {
		// TODO: Do we need to do anything here? Why are we disabling LAN mode on a schedule?
	}

	//TODO: move to a common utils class
	static private void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId, Boolean fromDevice) {
		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		
		rs.execute(); // return in main activity
	}
}









