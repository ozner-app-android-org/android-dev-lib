package com.ozner.cup;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;

import com.ozner.util.SQLiteDB;
import com.ozner.util.dbg;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 饮水量数据库
 *
 * @author xuzhiyong
 */
@SuppressLint({"NewApi", "SimpleDateFormat", "DefaultLocale"})
public class CupVolume {
    public Date time;
    private String Address;
    private SQLiteDB db;

    public CupVolume(Context context, String Address) {
        super();
        db = new SQLiteDB(context);
        this.Address = Address;
        db.execSQLNonQuery(
                "CREATE TABLE IF NOT EXISTS DayTable (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, SN VARCHAR NOT NULL, Time INTEGER NOT NULL,JSON TEXT NOT NULL, UpdateFlag BOOLEAN NOT NULL)",
                new String[]{});
        db.execSQLNonQuery(
                "CREATE TABLE IF NOT EXISTS HourTable2 (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, SN VARCHAR NOT NULL, Time INTEGER NOT NULL,JSON TEXT NOT NULL, UpdateFlag BOOLEAN NOT NULL)",
                new String[]{});
        Date time = new Date();
        Long t = time.getTime() / 86400000 * 86400000;
        db.execSQLNonQuery("delete from HourTable2 where Time<?",
                new String[]{t.toString()});
    }

    /**
     * 清除设备数据并往数据库里面插入一组数据,
     *
     * @param Address 设备地址
     * @param Records 设备记录列表
     */
    public void LoadDay(String Address, Record[] Records) {
        db.execSQLNonQuery("delete from DayTable where sn=?", new String[]{Address});
        db.execSQLNonQuery("delete from HourTable2 where sn=?", new String[]{Address});

        for (Record record : Records) {
            db.execSQLNonQuery(
                    "insert into DayTable (sn,time,json,updateflag) values (?,?,?,1);",
                    new Object[]{Address,
                            record.time.getTime(),
                            record.toJSON()});
        }
    }

    private Record getLastDay() {
        List<String[]> valuesListDay = db
                .ExecSQL("select id,time,json from DayTable where sn=? order by time desc limit 1 ;",
                        new String[]{Address});
        if (valuesListDay.size() > 0) {
            Record lastDay = new Record();

            String[] valDay = valuesListDay.get(0);

            lastDay.id = Integer.parseInt(valDay[0]);
            lastDay.time = new Date(Long.parseLong(valDay[1]));
            dbg.d("getLastDay Time:%s", valDay[1]);
            lastDay.FromJSON(valDay[2]);
            dbg.i("lastDay.time:" + lastDay.time);

            return lastDay;
        } else {
            return null;
        }

    }

    private Record getLastHour() {
        List<String[]> valuesList = db
                .ExecSQL(
                        "select id,time,json from HourTable2 where sn=? order by time desc limit 1 ;",
                        new String[]{Address});
        if (valuesList.size() > 0) {
            Record item = new Record();
            item.id = Integer.parseInt(valuesList.get(0)[0]);
            item.time = new Date(Long.parseLong(valuesList.get(0)[1]));
            item.FromJSON(valuesList.get(0)[2]);
            return item;
        } else {
            return null;
        }

    }

    /**
     * 将分钟的饮水记录统计写入小时数据和日数据表
     *
     * @param items 饮水记录数组
     */
    public void addRecord(CupRecord[] items) {
        synchronized (this) {
            if (items.length <= 0) {
                return;
            }

            Record lastHour = getLastHour();
            if (lastHour == null) {
                lastHour = new Record();
                lastHour.time = new Date(0);
            }
            Record lastDay = getLastDay();
            if (lastDay == null) {
                lastDay = new Record();
                lastDay.time = new Date(0);
            }

            long hour = (lastHour.time.getTime() / 3600000);
            long day = (lastDay.time.getTime() / 86400000);

            boolean hourChange = false;
            boolean dayChange = false;

            for (CupRecord item : items) {
                long inv = item.time.getTime();
                // inv/86400000==lastHour.time.getTime()/3600000
                if (inv / 3600000 == hour) {
                    lastHour.Volume += item.Vol;
                    lastHour.incTDS(item.TDS);
                    lastHour.incTemp(item.Temperature);
                    hourChange = true;
                } else {
                    if (hourChange) {
                        db.execSQLNonQuery(
                                "update HourTable2 set json=?,updateflag=0 where id=? ;",
                                new Object[]{lastHour.toJSON(),
                                        lastHour.id});// /
                    }
                    lastHour = new Record();

                    hour = inv / 3600000;

                    lastHour.time = new Date(hour * 3600000);
                    lastHour.Volume += item.Vol;
                    lastHour.incTDS(item.TDS);
                    lastHour.incTemp(item.Temperature);
                    ContentValues values = new ContentValues();
                    values.put("sn", Address);
                    values.put("time", lastHour.time.getTime());
                    values.put("json", lastHour.toJSON());
                    values.put("updateflag", 0);
                    lastHour.id = db.insert("HourTable2", values);

                    //db.execSQLNonQuery("insert into HourTable (sn,time,json) values(?,?,?);", new String[] { Address,
                    //		String.valueOf(lastHour.time.getTime()),
                    //		lastHour.toJSON()});
                    //lastHour.id = Integer.parseInt(db.ExecSQLOneRet("select last_insert_rowid();",new String[] {}));

                    //lastHour.id = Integer.parseInt(db.ExecSQLOneRet(
                    //		"select last_insert_rowid();", new String[] {}));
                    hourChange = true;
                }
                if (inv / 86400000 == day) {
                    lastDay.Volume += item.Vol;
                    lastDay.incTDS(item.TDS);
                    lastDay.incTemp(item.Temperature);
                    dayChange = true;
                } else {
                    if (dayChange) {
                        db.execSQLNonQuery(
                                "update DayTable set json=?,updateflag = 0 where id=? ;",
                                new Object[]{lastDay.toJSON(),
                                        lastDay.id});
                    }
                    lastDay = new Record();
                    day = inv / 86400000;
                    lastDay.time = new Date(day * 86400000);
                    lastDay.Volume += item.Vol;
                    lastDay.incTDS(item.TDS);
                    lastDay.incTemp(item.Temperature);
                    dbg.d("Time:%d", lastDay.time.getTime());

                    ContentValues values = new ContentValues();
                    values.put("sn", Address);
                    values.put("time", lastDay.time.getTime());
                    values.put("json", lastDay.toJSON());
                    values.put("updateflag", 0);

                    lastHour.id = db.insert("DayTable", values);

//					db.execSQLNonQuery(
//							"insert into DayTable (sn,time,json,updateflag)values(?,?,?,0);",
//							new String[] { Address,
//									String.valueOf(lastDay.time.getTime()),
//									lastDay.toJSON()});
//					
//					lastDay.id = Integer.parseInt(db.ExecSQLOneRet(
//							"select last_insert_rowid();", new String[] {}));
                    dayChange = true;
                }
            }
            // 小时变更标记
            if (hourChange) {
                db.execSQLNonQuery(
                        "update HourTable2 set json=?, updateflag=0 where id=? ;",
                        new Object[]{lastHour.toJSON(), lastHour.id});
            }
            if (dayChange) {
                db.execSQLNonQuery(
                        "update DayTable set json=?, updateflag = 0 where id=? ;",
                        new Object[]{lastDay.toJSON(), lastDay.id});
            }
            dbg.i("lastHour:%s", lastHour.toString());
            dbg.i("lastDay:%s", lastDay.toString());

        }
    }

	/*
     * public CupRecord getItemByDay(Date day) { synchronized (this) { Long pt =
	 * new Date(day.getTime() / 86400000*86400000).getTime();
	 * 
	 * List<String[]> valuesList = db .ExecSQL(
	 * "select id, time,vol from DayTable where sn=? and time=? ;", new
	 * String[] { Address, pt.toString() });
	 * 
	 * if (valuesList.size() <= 0) { return null; } else { for (String[] val :
	 * valuesList) { CupRecord item = new CupRecord(); item.id =
	 * Integer.parseInt(val[0]); item.time = new Date(Long.parseLong(val[1]));//
	 * valuesList.get(0)[1] item.Vol = Integer.parseInt(val[2]); return item; }
	 * }} return null; }
	 */

    /**
     * 获取指定日期的饮水记录
     *
     * @param time 要获取的时间
     */
    public Record getRecordByDate(Date time) {
        synchronized (this) {

            Long pt = new Date(time.getTime() / 86400000 * 86400000).getTime();
            List<String[]> valuesList = db
                    .ExecSQL(
                            "select id,time,json from DayTable where sn=? and time=?;",
                            new String[]{Address, pt.toString()});
            if (valuesList.size() <= 0) {
                return null;
            } else {
                for (String[] val : valuesList) {
                    Record item = new Record();
                    item.id = Integer.parseInt(val[0]);
                    item.time = new Date(Long.parseLong(val[1]));// valuesList.get(0)[1]
                    item.FromJSON(val[2]);
                    return item;
                }
                return null;
            }
        }

    }

    /**
     * 获取日饮水记录
     *
     * @param time 要获取的起始时间
     */
    public Record[] getRecordsByDate(Date time) {
        synchronized (this) {

            Long pt = new Date(time.getTime() / 86400000 * 86400000).getTime();

            List<String[]> valuesList = db
                    .ExecSQL(
                            "select id,time,json from DayTable where sn=? and time>=?;",
                            new String[]{Address, pt.toString()});

            ArrayList<Record> list = new ArrayList<Record>();
            if (valuesList.size() <= 0) {
                return list.toArray(new Record[list.size()]);
            } else {
                for (String[] val : valuesList) {
                    Record item = new Record();
                    item.id = Integer.parseInt(val[0]);
                    item.time = new Date(Long.parseLong(val[1]));// valuesList.get(0)[1]
                    item.FromJSON(val[2]);
                    list.add(item);
                }
                return list.toArray(new Record[list.size()]);
            }
        }

    }

    /**
     * 获取当前小时的饮水数据
     */
    public int getCurrHourVol() {
        synchronized (this) {
            Record lastHour = getLastHour();
            Date dt = new Date();
            if (lastHour != null) {
                if ((lastHour.time.getTime() / 3600000) == (dt.getTime() / 3600000)) {
                    return lastHour.Volume;
                } else
                    return 0;
            } else {

                return 0;
            }
        }
    }

    public Record getTodayItem() {
        return getRecordByDate(new Date());
    }

    public void clearToday() {
        synchronized (this) {
            Long pt = new Date(new Date().getTime() / 86400000 * 86400000).getTime();
            db.execSQLNonQuery("delete from DayTable where sn=? and time=?;", new Object[]{Address, pt.toString()});
        }
    }

    /**
     * 获取今日饮水小时数据
     */
    public Record[] getToday() {
        synchronized (this) {
            Date now = new Date();

            long startDate = new Date(now.getTime() / 86400000 * 86400000)
                    .getTime();

            long endDate = new Date(
                    (now.getTime() + 86400000) / 86400000 * 86400000).getTime();

            String sql = "select id, time,json from HourTable2 where sn=? and time>=? and time<=?";
            List<String[]> valuesList = db.ExecSQL(sql,
                    new String[]{Address, String.valueOf(startDate), String.valueOf(endDate)});
            ArrayList<Record> list = new ArrayList<>();
            if (valuesList.size() <= 0) {
                return list.toArray(new Record[list.size()]);
            } else {
                for (String[] val : valuesList) {
                    Record item = new Record();
                    item.id = Integer.parseInt(val[0]);
                    item.time = new Date(Long.parseLong(val[1]));
                    item.FromJSON(val[2]);
                    list.add(item);
                }
            }

            return list.toArray(new Record[list.size()]);
        }
    }

    /**
     * 获取未同步的数据
     *
     * @param time 起始时间
     */
    public Record[] getNoSyncItemHour(Date time) {
        synchronized (this) {

            Long pt = new Date(time.getTime() / 86400000).getTime();
            List<String[]> valuesList = db
                    .ExecSQL(
                            "select id, time,json from HourTable2 where updateflag=0 and sn=? and time>=?;",
                            new String[]{Address, pt.toString()});
            ArrayList<Record> list = new ArrayList<>();
            for (String[] val : valuesList) {
                Record item = new Record();
                item.id = Integer.parseInt(val[0]);
                item.time = new Date(Long.parseLong(val[1]));// valuesList.get(0)[1]
                item.FromJSON(val[2]);
                list.add(item);
            }
            return list.toArray(new Record[list.size()]);
        }
    }

    /**
     * 将同步过的数据打上已更新标记
     *
     * @param time 更新的时间
     */
    public void setHourSyncTime(Date time) {
        synchronized (this) {
            db.execSQLNonQuery(
                    "update HourTable2 set updateflag = 1 where sn = ? and time <=?",
                    new String[]{Address, time.toString()});
        }
    }

    /**
     * 获取未同步的数据
     *
     * @param time 起始时间
     */
    public Record[] getNoSyncItemDay(Date time) {
        synchronized (this) {

            Long pt = new Date(time.getTime() / 86400000).getTime();
            List<String[]> valuesList = db
                    .ExecSQL(
                            "select id, time,json from DayTable where updateflag=0 and sn=? and time>=?;",
                            new String[]{Address, pt.toString()});
            ArrayList<Record> list = new ArrayList<>();
            for (String[] val : valuesList) {
                Record item = new Record();
                item.id = Integer.parseInt(val[0]);
                item.time = new Date(Long.parseLong(val[1]));// valuesList.get(0)[1]
                item.FromJSON(val[2]);
                list.add(item);
            }
            return list.toArray(new Record[list.size()]);
        }
    }

    /**
     * 将同步过的数据打上已更新标记
     *
     * @param time 更新的时间
     */
    public void setSyncTime(Date time) {
        synchronized (this) {
            db.execSQLNonQuery(
                    "update DayTable set updateflag = 1 where sn = ? and time <=?",
                    new String[]{Address, time.toString()});
        }
    }

}
