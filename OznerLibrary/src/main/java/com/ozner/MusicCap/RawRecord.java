package com.ozner.MusicCap;

/**
 * Created by zhiyongxu on 16/2/29.
 */

import com.ozner.util.ByteUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 水杯原始饮水记录
 *
 * @author zhiyongxu
 */
class RawRecord implements Comparable {
    /**
     * 时间1
     */
    public Date time;
//
//    /**
//     * 时间1
//     */
//    public Date time2;

    /**
     * 运动次数
     */
    public int sportCount = 0;
    /**
     * 当前记录位置
     */
    public int Index = 0;
    /**
     * 记录总条数
     */
    public int Count;


    public RawRecord() {
        time = new Date();
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return String.format("Time:%s Sport:%d ", sdf.format(time), sportCount);
    }

    @SuppressWarnings("deprecation")
    public void FromBytes(byte[] data) {
        int tmp=ByteUtil.getInt(data,1);
        time = new Date(tmp*1000L);
//        time2 = new Date(ByteUtil.getInt(data,4)*1000L);
        sportCount =ByteUtil.getShort(data,9);

        Index = ByteUtil.getShort(data, 13);
        Count = ByteUtil.getShort(data, 15);
    }

    @Override
    public int hashCode() {
        return time.hashCode();
    }

    @Override
    public int compareTo(Object o) {
        RawRecord r = (RawRecord) o;
        return time.compareTo(r.time);
    }
}
