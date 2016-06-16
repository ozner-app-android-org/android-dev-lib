package com.ozner.cup;

import com.ozner.util.ByteUtil;

/**
 * @author zhiyongxu
 *         智能杯传感器对象
 * @category 智能杯
 */
public class CupSensor {
    public static final int CUP_SENSOR_ERROR = 0xffff;
    public int Battery = CUP_SENSOR_ERROR;
    /**
     * 电池电压
     */
    public int BatteryFix = CUP_SENSOR_ERROR;
    public int Temperature = CUP_SENSOR_ERROR;
    /**
     * 温度
     */
    public int TemperatureFix = CUP_SENSOR_ERROR;
    public int Weigh = CUP_SENSOR_ERROR;
    /**
     * 重量
     */
    public int WeighFix = CUP_SENSOR_ERROR;
    public int TDS = CUP_SENSOR_ERROR;
    /**
     * TDS
     */
    public int TDSFix = CUP_SENSOR_ERROR;

    public CupSensor() {
    }

    /**
     * 获取电池电量
     *
     * @return 0-100%
     */
    public float getPower() {
        if (BatteryFix >= 3000) {
            float ret = (BatteryFix - 3000f) / (4200f - 3000f);
            if (ret > 100)
                ret = 100;
            return ret;
        } else
            return 0;
    }

    public void reset() {
        Battery = CUP_SENSOR_ERROR;
        BatteryFix = CUP_SENSOR_ERROR;
        Temperature = CUP_SENSOR_ERROR;
        TemperatureFix = CUP_SENSOR_ERROR;
        Weigh = CUP_SENSOR_ERROR;
        WeighFix = CUP_SENSOR_ERROR;
        TDS = CUP_SENSOR_ERROR;
        TDSFix = CUP_SENSOR_ERROR;
    }

    private static String getValue(int value) {
        if (value == CUP_SENSOR_ERROR) return "-";
        else return String.valueOf(value);
    }

    @Override
    public String toString() {
        return String.format("Battery:%s/%s Temp:%s/%s Weigh:%s/%s TDS:%s/%s",
                getValue(Battery), getValue(BatteryFix), getValue(Temperature),
                getValue(TemperatureFix),
                getValue(Weigh), getValue(WeighFix),
                getValue(TDS), getValue(TDSFix));
    }

    public void FromBytes(byte[] data, int startIndex) {
        Battery = ByteUtil.getShort(data, startIndex + 0);
        BatteryFix = ByteUtil.getShort(data, startIndex + 2);
        Temperature = ByteUtil.getShort(data, startIndex + 4);
        TemperatureFix = ByteUtil.getShort(data, startIndex + 6);
        Weigh = ByteUtil.getShort(data, startIndex + 8);
        WeighFix = ByteUtil.getShort(data, startIndex + 10);
        TDS = ByteUtil.getShort(data, startIndex + 12);
        TDSFix = ByteUtil.getShort(data, startIndex + 14);

    }
}
