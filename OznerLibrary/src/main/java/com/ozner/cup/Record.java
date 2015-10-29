package com.ozner.cup;

import android.annotation.SuppressLint;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author zhiyongxu
 *         饮水记录
 */
public class Record {
    public long id;
    /**
     * 饮水时间
     */
    public Date time;
    /**
     * 饮水量
     */
    public int Volume;
    /**
     * 小于25度的次数
     */
    public int Temperature_25;
    /**
     * 大于65度的饮水次数
     */
    public int Temperature_65;
    /**
     * 25-65度的次数
     */
    public int Temperature_25_65;
    /**
     * TDS小于50的次数
     */
    public int TDS_50;
    /**
     * TDS大于200的次数
     */
    public int TDS_200;
    /**
     * TDS 50-200的次数
     */
    public int TDS_50_200;

    /**
     * TDS 最高值
     */
    public int TDS_High = 0;

    /**
     * 温度最高值
     */
    public int Temperature_High = 0;
    /**
     * 饮水次数
     */
    public int Count = 0;


    public void incTemp(int Temp) {
        if (Temp < 20)
            Temperature_25++;
        else if (Temp > 50)
            Temperature_65++;
        else
            Temperature_25_65++;

        if (Temp > Temperature_High)
            Temperature_High = Temp;
        Count++;
    }

    public void incTDS(int TDS) {
        if (TDS < 50)
            TDS_50++;
        else if (TDS > 200)
            TDS_200++;
        else
            TDS_50_200++;
        if (TDS > TDS_High)
            TDS_High = TDS;
    }

    @Override
    public String toString() {
        return String.format("time:%s vol:%d t25:%d t25-65:%d t64:%d s50:%d s200:%d s50-200:%d tds_h:%d temp_h:%d count:%d",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time), Volume,
                Temperature_25, Temperature_25_65, Temperature_65, TDS_50, TDS_200, TDS_50_200,
                TDS_High, Temperature_High, Count);
    }

    @SuppressLint("NewApi")
    public String toJSON() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Volume", Volume);
            jsonObject.put("Temperature_25", Temperature_25);
            jsonObject.put("Temperature_65", Temperature_65);
            jsonObject.put("Temperature_25_65", Temperature_25_65);
            jsonObject.put("TDS_50", TDS_50);
            jsonObject.put("TDS_200", TDS_200);
            jsonObject.put("TDS_50_200", TDS_50_200);
            jsonObject.put("Temperature_High", Temperature_High);
            jsonObject.put("TDS_High", TDS_High);
            jsonObject.put("Count", Count);
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
            if (object.has("Volume")) {
                Volume = object.getInt("Volume");
            }

            if (object.has("Temperature_25")) {
                Temperature_25 = object.getInt("Temperature_25");
            }
            if (object.has("Temperature_25_65")) {
                Temperature_25_65 = object.getInt("Temperature_25_65");
            }
            if (object.has("Temperature_65")) {
                Temperature_65 = object.getInt("Temperature_65");
            }

            if (object.has("TDS_50")) {
                TDS_50 = object.getInt("TDS_50");
            }
            if (object.has("TDS_200")) {
                TDS_200 = object.getInt("TDS_200");
            }
            if (object.has("TDS_50_200")) {
                TDS_50_200 = object.getInt("TDS_50_200");
            }
            if (object.has("Count")) {
                Count = object.getInt("Count");
            }
            if (object.has("TDS_High")) {
                TDS_High = object.getInt("TDS_High");
            }
            if (object.has("Temperature_High")) {
                Temperature_High = object.getInt("Temperature_High");
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
