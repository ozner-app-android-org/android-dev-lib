package com.ozner.cup;

import com.ozner.util.ByteUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 水杯原始饮水记录
 *
 * @author zhiyongxu
 */
public class CupRecord implements Comparable {
    /**
     * 时间
     */
    public Date time;
    /**
     * 饮水量
     */
    public int Vol = 0;
    /**
     * 当前记录位置
     */
    public int Index = 0;
    /**
     * 记录总条数
     */
    public int Count;
    public int id;
    /**
     * 当时饮水温度
     */
    public int Temperature;
    /**
     * TDS
     */
    public int TDS;

    public CupRecord() {
        time = new Date();
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return String.format("Time:%s Vol:%d TDS:%d Temperature:%d", sdf.format(time), Vol, TDS, Temperature);
    }

    @SuppressWarnings("deprecation")
    public void FromBytes(byte[] data) {
        time = new Date(data[0] + 2000 - 1900, data[1] - 1, data[2], data[3],
                data[4], data[5]);
        // time.set(data[5],data[4],data[3], data[2],data[1], data[0]+2000);
        Vol = ByteUtil.getShort(data, 8);
        Index = ByteUtil.getShort(data, 10);
        Count = ByteUtil.getShort(data, 12);
        Temperature = ByteUtil.getShort(data, 14);
        TDS = ByteUtil.getShort(data, 16);
    }

    @Override
    public int hashCode() {
        return time.hashCode();
    }

    @Override
    public int compareTo(Object o) {
        CupRecord cup = (CupRecord)o;
        return time.compareTo(cup.time);
    }
}
