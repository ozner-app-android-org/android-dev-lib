package com.ozner.kettle;

import com.ozner.util.ByteUtil;

/**
 * Created by zhiyongxu on 2017/5/23.
 */

public class KettleStatus {
    /**
     * 数据加载标记，TRUE已经加载数据
     */
    public boolean isLoaded = false;

    public void load(byte[] bytes) {
        if (bytes.length < 18) return;
        if (bytes[0] != 0x21) return;

        if (bytes[1] == 1)
            heatMode = HeatMode.heating;
        else if (bytes[1] == 2)
            heatMode = HeatMode.Preservation;
        else
            heatMode = HeatMode.Idle;
        temperature = bytes[2];
        TDS = ByteUtil.getShort(bytes, 3);
        reservation = bytes[5] != 0;
        reservationTime = ByteUtil.getShort(bytes, 6);
        boilingTemperature = bytes[8];
        if (bytes[9] == 0) {
            preservationMode = PreservationMode.boiling;
        } else
            preservationMode = PreservationMode.heating;
        preservationTemperature = bytes[10];
        preservationTime = ByteUtil.getShort(bytes, 11);
        preservationRunTime = ByteUtil.getShort(bytes, 13);
        heatingTime = ByteUtil.getShort(bytes, 15);
        error_code = bytes[17];
        isLoaded = true;
    }

    public void reset() {
        isLoaded = false;
    }

    public enum HeatMode {
        /**
         * 加热中
         */
        heating,
        /**
         * 保温
         */
        Preservation,
        /**
         * 待机
         */
        Idle
    }


    /**
     * 加热模式
     */
    public HeatMode heatMode;
    /**
     * 温度
     */
    public int temperature;
    /**
     * tds
     */
    public int TDS;

    /**
     * 预约开关
     */
    public boolean reservation = false;
    /**
     * 预约时间
     */
    public int reservationTime = 0;
    /**
     * 煮沸温度
     */
    public int boilingTemperature;

    /**
     * 保温模式
     */
    public PreservationMode preservationMode;

    /**
     * 保温温度
     */
    public int preservationTemperature;

    /**
     * 设置的保温时间（分钟）
     */
    public int preservationTime;

    /**
     * 当前保温时间（分钟）
     */
    public int preservationRunTime;

    /**
     * 当前加热时间（秒单位）
     */
    public int heatingTime;


    /**
     * 错误码
     */
    public int error_code;

    @Override
    public String toString() {
        if (isLoaded) {
            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append(String.format("模式:%s Temp:%d TDS:%d \n", heatMode.toString(), temperature, TDS));
            stringBuilder.append(String.format("预约:%s 预约时间:%d \n", reservation ? "开" : "关", reservationTime));
            stringBuilder.append(String.format("煮沸模式:%s 保温温度:%d\n", preservationMode.toString(), preservationTemperature));
            stringBuilder.append(String.format("保温时间:%d 剩余保温时间:%d\n", preservationTime, preservationTime));
            stringBuilder.append(String.format("当前加热时间:%d\n", this.heatingTime));

            return stringBuilder.toString();
        } else {
            return "";
        }

    }
}
