package com.ozner.cup;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author zhiyongxu
 *         饮水记录
 */
public class CupRecord {

    public static final int TDS_Good_Value=50;
    public static final int TDS_Bad_Value=200;
    public static final int Temperature_Low_Value=25;
    public static final int Temperature_High_Value=50;

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
    public int Volume = 0;

    /**
     * 最后一次TDS 值
     */
    public int TDS = 0;

    /**
     * 最后一次温度
     */
    public int Temperature = 0;

    /**
     * 水温低的次数
     */
    public int Temperature_Low;
    /**
     * 水温中的次数
     */
    public int Temperature_Mid;
    /**
     * 水温高的次数
     */
    public int Temperature_High;
    /**
     * TDS好的次数
     */
    public int TDS_Good;
    /**
     * TDS中的次数
     */
    public int TDS_Mid;
    /**
     * TDS差的次数
     */
    public int TDS_Bad;

    /**
     * TDS 最高值
     */
    public int TDS_High = 0;

    /**
     * 温度最高值
     */
    public int Temperature_MAX = 0;
    /**
     * 饮水次数
     */
    public int Count = 0;

    protected void calcRecord(DBRecord record)
    {
        if (start==null)
            start=record.time;
        end=record.time;
        Volume+=record.volume;
        TDS=record.tds;
        Temperature=record.temperature;
        TDS_High=Math.max(TDS_High,record.tds);
        Temperature_MAX=Math.max(Temperature_MAX,record.temperature);
        Count++;

        if (record.tds < TDS_Good_Value)
            TDS_Good++;
        else if (record.tds > TDS_Bad_Value)
            TDS_Bad++;
        else
            TDS_Mid++;

        if (record.temperature < Temperature_Low_Value)
            Temperature_Low++;
        else if (record.temperature > Temperature_High_Value)
            Temperature_High++;
        else
            Temperature_Mid++;

    }


    @Override
    public String toString() {
        if (start.equals(end))
        {
            return String.format("time:%s\nvol:%d tds:%d temp:%d count:%d\n" +
                            "温度高:%d 温度中:%d 温度低:%d\n" +
                            "TDS好:%d TDS中:%d TDS差:%d",
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(start),
                    Volume,TDS,Temperature,Count,
                    Temperature_High, Temperature_Mid, Temperature_Low,
                    TDS_Good, TDS_Mid, TDS_Bad);
        }else
            return String.format("start:%s\nend:%s\nvol:%d tds:%d temp:%d count:%d\n" +
                            "温度高:%d 温度中:%d 温度低:%d\n" +
                            "TDS好:%d TDS中:%d TDS差:%d",
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(start),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(end),
                    Volume,TDS,Temperature,Count,
                    Temperature_High, Temperature_Mid, Temperature_Low,
                    TDS_Good, TDS_Mid, TDS_Bad);
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
