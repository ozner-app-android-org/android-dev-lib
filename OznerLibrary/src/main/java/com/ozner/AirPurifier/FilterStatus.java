package com.ozner.AirPurifier;

import com.ozner.util.ByteUtil;

import java.util.Date;

/**
 * Created by zhiyongxu on 15/11/11.
 */
public class FilterStatus {
    public Date lastTime=new Date();
    public Date stopTime=new Date();
    public int WorkTime=0;
    public int MaxWorkTime=10000;
    public byte[] toBytes()
    {
        byte[] bytes=new byte[16];
        ByteUtil.putInt(bytes,(int)(lastTime.getTime()/1000),0);
        ByteUtil.putInt(bytes,WorkTime, 4);
        ByteUtil.putInt(bytes,(int)(stopTime.getTime()/1000),8);
        ByteUtil.putInt(bytes,MaxWorkTime,12);
        return bytes;
    }
    public void fromBytes(byte[] bytes)
    {
        if ((bytes!=null) && (bytes.length==16))
        {
            lastTime=new Date(ByteUtil.getInt(bytes,0)*1000L);
            WorkTime=ByteUtil.getInt(bytes,4);
            stopTime=new Date(ByteUtil.getInt(bytes,8)*1000L);
            MaxWorkTime=ByteUtil.getInt(bytes,12);


        }
    }

}
