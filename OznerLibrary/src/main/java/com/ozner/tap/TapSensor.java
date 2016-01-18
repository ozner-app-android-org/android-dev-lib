package com.ozner.tap;

import com.ozner.util.ByteUtil;

/**
 * @author zhiyongxu
 *         智能杯传感器对象
 * @category 智能杯
 */
public class TapSensor {
    public static final int TAP_SENSOR_ERROR = 0xffff;
    public int Battery = 0;
    /**
     * 电池电压
     */
    public int BatteryFix = 0;


//    /**
//     * 温度
//     */
//    public int TemperatureFix = 0;
//    public int Temperature = 0;
//
//    /**
//     * 重量
//     */
//    public int WeighFix = 0;
//    public int Weigh = 0;
    /**
     * TDS
     */
    public int TDSFix = TAP_SENSOR_ERROR;
    public int TDS = TAP_SENSOR_ERROR;

    public TapSensor() {
    }

    /**
     * 获取电池电量
     *
     * @return 0-100%
     */
    public float getPower() {
        if (BatteryFix == TAP_SENSOR_ERROR) return TAP_SENSOR_ERROR;
        if (BatteryFix > 3000) return 1;
        if (BatteryFix >= 2900) return 0.9f;
        if (BatteryFix >= 2800) return 0.7f;
        if (BatteryFix >= 2700) return 0.5f;
        if (BatteryFix >= 2600) return 0.3f;
        if (BatteryFix >= 2500) return 0.17f;
        if (BatteryFix >= 2400) return 0.16f;
        if (BatteryFix >= 2300) return 0.15f;
        if (BatteryFix >= 2200) return 0.07f;
        if (BatteryFix >= 2100) return 0.03f;
        if (BatteryFix == 0) return 0;
        return 0f;
    }

    private static String getValue(int value) {
        if (value == TAP_SENSOR_ERROR) return "-";
        else return String.valueOf(value);
    }

    public void reset() {
        Battery = TAP_SENSOR_ERROR;
        BatteryFix = TAP_SENSOR_ERROR;
        TDS = TAP_SENSOR_ERROR;
        TDSFix = TAP_SENSOR_ERROR;
    }

    @Override
    public String toString() {
        return String.format("Battery:%s/%s TDS:%s/%s", getValue(Battery), getValue(BatteryFix), getValue(TDS), getValue(TDSFix));
    }

    public void FromBytes(byte[] data, int startIndex) {
        Battery = ByteUtil.getShort(data, startIndex + 0);
        BatteryFix = ByteUtil.getShort(data, startIndex + 2);
//        Temperature = ByteUtil.getShort(data, startIndex + 4);
//        TemperatureFix = ByteUtil.getShort(data, startIndex + 6);
//        Weigh = ByteUtil.getShort(data, startIndex + 8);
//        WeighFix = ByteUtil.getShort(data, startIndex + 10);
        TDS = ByteUtil.getShort(data, startIndex + 12);
        TDSFix = ByteUtil.getShort(data, startIndex + 14);

    }
}
