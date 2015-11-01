package com.ozner.tap;

import android.annotation.SuppressLint;

import com.ozner.util.ByteUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 水探头原始记录数据
 *
 * @author zhiyongxu
 * @category 水探头
 */
@SuppressLint("SimpleDateFormat")
class TapRecord implements Comparable{
    public int id;
    public Date time;
    public int Index = 0;
    public int Count;
    public int TDS;

    public TapRecord() {
        time = new Date();
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return String.format("Time:%s TDS:%d ", sdf.format(time), TDS);
    }

    @SuppressWarnings("deprecation")
    public void FromBytes(byte[] data) {
        time = new Date(data[0] + 2000 - 1900, data[1] - 1, data[2], data[3],
                data[4], data[5]);
        TDS = ByteUtil.getShort(data, 6);
        Index = ByteUtil.getShort(data, 8);
        Count = ByteUtil.getShort(data, 10);
    }

    @SuppressLint("NewApi")
    public String toJSON() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("TDS", TDS);
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    @SuppressLint("NewApi")
    public void FromJSON(String json) {
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
            e.printStackTrace();
        }
    }

    @Override
    public int compareTo(Object o) {
        TapRecord tap = (TapRecord)o;
        return time.compareTo(tap.time);
    }

}
