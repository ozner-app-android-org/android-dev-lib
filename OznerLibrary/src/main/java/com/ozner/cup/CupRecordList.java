package com.ozner.cup;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;

import com.ozner.util.SQLiteDB;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 饮水量数据库
 *
 * @author xuzhiyong
 */
@SuppressLint({"NewApi", "SimpleDateFormat", "DefaultLocale"})
public class CupRecordList {
    public Date time;
    private String Address;
    private SQLiteDB db;
    public enum QueryInterval {Raw,Hour,Day,Week,Month}
    public CupRecordList(Context context, String Address) {
        super();
        db = new SQLiteDB(context);
        this.Address = Address;
        db.execSQLNonQuery(
                "CREATE TABLE IF NOT EXISTS CupRecordTable (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, Address VARCHAR NOT NULL, Time INTEGER NOT NULL," +
                        "TDS INTEGER NOT NULL,VOLUME INTEGER NOT NULL,TEMPERATURE INTEGER NOT NULL, updated BOOLEAN NOT NULL)",
                new String[]{});
    }


    /**
     * 清除设备数据并往数据库里面插入一组数据,
     *
     * @param Address 设备地址
     * @param cupRecords 设备记录列表
     */
    public void LoadDay(String Address, CupRecord[] cupRecords) {
        synchronized (this) {
            db.execSQLNonQuery("delete from CupRecordTable where Address=?", new String[]{Address});
            for (CupRecord cupRecord : cupRecords) {
                ContentValues values = new ContentValues();
                values.put("address", Address);
                values.put("time", cupRecord.start.getTime());
                values.put("tds", cupRecord.TDS);
                values.put("volume", cupRecord.Volume);
                values.put("temperature", cupRecord.Temperature);
                values.put("updated", true);
                db.insert("CupRecordTable", values);
            }
        }
    }

    private DBRecord getRecord(String[] dbResult)
    {
        DBRecord record=new DBRecord();
        record.time=new Date(Long.parseLong(dbResult[0]));
        record.tds=Integer.parseInt(dbResult[1]);
        record.volume=Integer.parseInt(dbResult[2]);
        record.temperature=Integer.parseInt(dbResult[3]);
        record.updated=Boolean.parseBoolean(dbResult[4]);
        return record;
    }

    /**
     * 获取最后一天的饮水数据
     * @return 如果没有返回 Null
     */
    public CupRecord getLastDay() {
        try {
            synchronized (this) {
                List<String[]> valuesListDay = db
                        .ExecSQL("select time from CupRecordTable where address=? order by time desc limit 1 ;",
                                new String[]{Address});
                if (valuesListDay.size() > 0) {
                    //取整日时间
                    int time = Integer.parseInt(valuesListDay.get(0)[0]) / 86400000 * 86400000;
                    List<String[]> values = db
                            .ExecSQL("select time,tds,volume,Temperature,updated from CupRecordTable where address=? and time>=?;",
                                    new String[]{Address, String.valueOf(time)});
                    if (values.size() > 0) {
                        CupRecord ret = new CupRecord();
                        for (String[] value : values) {
                            DBRecord record = getRecord(value);
                            ret.calcRecord(record);
                        }
                        return ret;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        }catch (Exception e)
        {
            return null;
        }
    }

    private CupRecord getLastHour() {

        try {
            synchronized (this) {
                List<String[]> valuesListDay = db
                        .ExecSQL("select time from CupRecordTable where address=? order by time desc limit 1 ;",
                                new String[]{Address});
                if (valuesListDay.size() > 0) {
                    //取最后条数据的整小时时间
                    int time = Integer.parseInt(valuesListDay.get(0)[0]) / 3600000 * 3600000;
                    List<String[]> values = db
                            .ExecSQL("select time,tds,volume,Temperature,updated from CupRecordTable where address=? and time>=?;",
                                    new String[]{Address, String.valueOf(time)});
                    if (values.size() > 0) {
                        CupRecord ret = new CupRecord();
                        for (String[] value : values) {
                            DBRecord record = getRecord(value);
                            ret.calcRecord(record);
                        }
                        return ret;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        }catch (Exception e)
        {
            return null;
        }

    }

    /**
     * 将分钟的饮水记录统计写入小时数据和日数据表
     *
     * @param items 饮水记录数组
     */
    public void addRecord(RawRecord[] items) {
        synchronized (this) {
            if (items.length <= 0) {
                return;
            }
            for (RawRecord item : items) {
                ContentValues values = new ContentValues();
                values.put("address", Address);
                values.put("time", item.time.getTime());
                values.put("tds", item.TDS);
                values.put("volume", item.Vol);
                values.put("temperature", item.Temperature);
                values.put("updated", false);
                db.insert("CupRecordTable", values);
            }
        }
    }

    /**
     * 获取指定时间开始的所有统计数据
     *
     * @param time 要获取的时间
     */
    public CupRecord getRecordByDate(Date time) {
        synchronized (this) {
            List<String[]> valuesList = db
                    .ExecSQL(
                            "select time,tds,volume,Temperature,updated from CupRecordTable where address=? and time>=?;",
                            new String[]{Address, String.valueOf(time.getTime())});
            if (valuesList.size() <= 0) {
                return null;
            } else {
                CupRecord ret=new CupRecord();
                for (String[] val : valuesList) {
                    DBRecord record=getRecord(val);
                    ret.calcRecord(record);
                }
                return ret;
            }
        }
    }

    /**
     * 获取指定时间开始,指定周期的统计数据
     *
     * @param time 要获取的时间
     * @param interval 统计走起间隔
     */
    public CupRecord[] getRecordByDate(Date time,QueryInterval interval) {
        synchronized (this) {
            ArrayList<CupRecord> arrays=new ArrayList<>();

            List<String[]> valuesList = db
                    .ExecSQL(
                            "select time,tds,volume,Temperature,updated from CupRecordTable where address=? and time>=?;",
                            new String[]{Address, String.valueOf(time.getTime())});
            if (valuesList.size() <= 0) {
                return arrays.toArray(new CupRecord[arrays.size()]);
            } else {
                long lastTime=0;
                long t=0;
                CupRecord ret=null;
                for (String[] val : valuesList) {
                    DBRecord record=getRecord(val);
                    switch (interval)
                    {
                        case Raw:{
                            t=record.time.getTime();
                            break;
                        }
                        case Hour:
                        {
                            t=record.time.getTime()/ 3600000 * 3600000;
                            break;
                        }

                        case Day:
                        {
                            t=record.time.getTime()/ 86400000 * 86400000;
                            break;
                        }
                        case Month:
                        {
                            Calendar calendar=Calendar.getInstance();
                            calendar.setTime(record.time);
                            t=calendar.get(Calendar.MONTH);
                            break;
                        }
                        case Week:
                        {
                            Calendar calendar=Calendar.getInstance();
                            calendar.setTime(record.time);
                            t=calendar.get(Calendar.WEEK_OF_MONTH);
                            break;
                        }
                    }
                    if (t!=lastTime)
                    {
                        if (ret!=null)
                        {
                            arrays.add(ret);
                        }
                        ret=new CupRecord();
                    }
                    lastTime=t;
                    ret.calcRecord(record);
                }
                arrays.add(ret);
            }
            return arrays.toArray(new CupRecord[arrays.size()]);
        }
    }


}
