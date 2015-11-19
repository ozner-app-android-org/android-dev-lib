package com.ozner.tap;

import android.content.Context;

import com.ozner.util.SQLiteDB;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 水探头TDS数据集合
 *
 * @author zhiyongxu
 */
public class TapRecordList {
    public Date time;
    private String Address;
    private SQLiteDB db;

    public TapRecordList(Context context, String Address) {
        super();
        this.db = new SQLiteDB(context);
        this.Address = Address;
        db.execSQLNonQuery(
                "CREATE TABLE IF NOT EXISTS TapTable (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, SN VARCHAR NOT NULL, Time INTEGER NOT NULL,JSON TEXT NOT NULL, UpdateFlag BOOLEAN NOT NULL)",
                new String[]{});
    }

    /**
     * 清除设备数据并往数据库里面插入一组数据,
     *
     * @param Address 设备地址
     * @param tapRecords 设备记录列表
     */
    public void LoadRecords(String Address, TapRecord[] tapRecords) {
        db.execSQLNonQuery("delete from DayTable where sn=?", new String[]{Address});
        for (TapRecord tapRecord : tapRecords) {
            db.execSQLNonQuery(
                    "insert into DayTable (sn,time,json,updateflag) values (?,?,?,0);",
                    new Object[]{Address,
                            tapRecord.time.getTime(),
                            tapRecord.toJSON()});
        }
    }

    /**
     * 添加一笔记录
     *
     * @param items TDS记录
     */
    public void addRecord(RawRecord[] items) {
        if (items == null) return;
        if (items.length <= 0) return;
        synchronized (this) {
            for (RawRecord r : items) {
                RawRecord record = new RawRecord();
                record.time = r.time;
                record.TDS = r.TDS;
                db.execSQLNonQuery(
                        "insert into TapTable (sn,time,json,updateflag)values(?,?,?,0);",
                        new Object[]{Address,
                                record.time.getTime(),
                                record.toJSON()});
            }
        }
    }

    /**
     * 获取记录
     *
     * @param time 要获取的起始时间
     * @return
     */
    public TapRecord[] getRecordsByDate(Date time) {
        synchronized (this) {
            Long pt = new Date(time.getTime() / 86400000 * 86400000).getTime();
            List<String[]> valuesList = db
                    .ExecSQL(
                            "select id,time,json from TapTable where sn=? and time>=?;",
                            new String[]{Address, pt.toString()});

            ArrayList<TapRecord> list = new ArrayList<TapRecord>();
            if (valuesList.size() <= 0) {
                return list.toArray(new TapRecord[0]);
            } else {
                for (String[] val : valuesList) {
                    TapRecord item = new TapRecord();
                    item.id = Integer.parseInt(val[0]);
                    item.time = new Date(Long.parseLong(val[1]));// valuesList.get(0)[1]
                    item.FromJSON(val[2]);
                    list.add(item);
                }
                return list.toArray(new TapRecord[list.size()]);
            }
        }
    }

    /**
     * 获取未同步的数据
     *
     * @param time 起始时间
     * @return
     */
    public TapRecord[] getNoSyncItemDay(Date time) {
        synchronized (this) {

            Long pt = new Date(time.getTime() / 86400000 * 86400000).getTime();
            List<String[]> valuesList = db
                    .ExecSQL(
                            "select id, time,json from TapTable where updateflag=0 and sn=? and time>=?;",
                            new String[]{Address, pt.toString()});
            ArrayList<TapRecord> list = new ArrayList<TapRecord>();
            for (String[] val : valuesList) {
                TapRecord item = new TapRecord();
                item.id = Integer.parseInt(val[0]);
                item.time = new Date(Long.parseLong(val[1]));// valuesList.get(0)[1]
                item.FromJSON(val[2]);
                list.add(item);
            }
            return list.toArray(new TapRecord[list.size()]);
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
                    "update TapTable set updateflag = 1 where sn = ? and time <=?",
                    new String[]{Address, time.toString()});
        }
    }

}
