package com.ozner.tap;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
/**
 * @category 水探头
 * @author zhiyongxu
 * 水探头TDS自动监测记录
 */
public class Record {
	public int id;
	/**
	 * 监测时间
	 */
	public Date time;
	/**
	 * TDS
	 */
	public int TDS;
	@SuppressLint("NewApi") 
	public String toJSON()
	{
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("TDS", TDS);
			return jsonObject.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return "";
		}
	}
	@Override
	public String toString() {
		return String.format("time:%s TDS:%d", 
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time),TDS);
	}
	@SuppressLint("NewApi") 
	public void FromJSON(String json)
	{
		if (json == null)
			return;
		if (json.isEmpty())
			return;
		try {
			JSONObject object = new JSONObject(json);
			if (object.has("TDS")) {
				TDS = object.getInt("TDS");
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
