package com.ozner.MusicCap;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author zhiyongxu
 *         饮水记录
 */
public class SportRecord {
    /**
     * 数据周期起始时间
     */
    public Date start;

    /**
     * 数据周期结束时间
     */
    public Date end;


    /**
     * 饮水量
     */
    public int sportCount = 0;

    /**
     * 最后一次运动值
     */
    public int lastSport = 0;



    /**
     * 总纪录数
     */
    public int Count = 0;

    protected void calcRecord(DBRecord record)
    {
        if (start==null)
            start=record.time;
        end=record.time;
        sportCount +=record.sportCount;
        lastSport=record.sportCount;
        Count++;
    }


    @Override
    public String toString() {
        return String.format("time:%s\nsport:%d last:%d count:%d\n",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(start),
                sportCount,lastSport,Count);
    }

//    @SuppressLint("NewApi")
//    public String toJSON() {
//        try {
//            JSONObject jsonObject = new JSONObject();
//
//            jsonObject.put("Volume", Volume);
//            jsonObject.put("Temperature", Temperature);
//            jsonObject.put("TDS", TDS);
//
//            jsonObject.put("Temperature_High", Temperature_High);
//            jsonObject.put("Temperature_Mid", Temperature_Mid);
//            jsonObject.put("Temperature_Low", Temperature_Low);
//
//            jsonObject.put("TDS_Good", TDS_Good);
//            jsonObject.put("TDS_Mid", TDS_Mid);
//            jsonObject.put("TDS_Bad", TDS_Bad);
//
//            jsonObject.put("Temperature_MAX", Temperature_MAX);
//            jsonObject.put("TDS_High", TDS_High);
//            jsonObject.put("Count", Count);
//            return jsonObject.toString();
//        } catch (JSONException e) {
//            e.printStackTrace();
//            return "";
//        }
//    }
//
//    @SuppressLint("NewApi")
//    public void FromJSON(String json) {
//        if (json == null)
//            return;
//        if (json.isEmpty())
//            return;
//        try {
//            JSONObject object = new JSONObject(json);
//            if (object.has("Volume")) {
//                Volume = object.getInt("Volume");
//            }
//
//            if (object.has("Temperature_High")) {
//                Temperature_High = object.getInt("Temperature_High");
//            }
//            if (object.has("Temperature_Mid")) {
//                Temperature_Mid = object.getInt("Temperature_Mid");
//            }
//            if (object.has("Temperature_Low")) {
//                Temperature_Low = object.getInt("Temperature_Low");
//            }
//
//            if (object.has("TDS_Good")) {
//                TDS_Good = object.getInt("TDS_Good");
//            }
//            if (object.has("TDS_Mid")) {
//                TDS_Mid = object.getInt("TDS_Mid");
//            }
//            if (object.has("TDS_Bad")) {
//                TDS_Bad = object.getInt("TDS_Bad");
//            }
//            if (object.has("Count")) {
//                Count = object.getInt("Count");
//            }
//            if (object.has("TDS_High")) {
//                TDS_High = object.getInt("TDS_High");
//            }
//            if (object.has("Temperature_MAX")) {
//                Temperature_MAX = object.getInt("Temperature_MAX");
//            }
//        } catch (JSONException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }
}
