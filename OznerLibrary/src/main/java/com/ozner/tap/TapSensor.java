package com.ozner.tap;

import com.ozner.util.ByteUtil;

/**
 * @author zhiyongxu
 *         智能杯传感器对象
 * @category 智能杯
 */
public class TapSensor {
    public int Battery = 0;
    /**
     * 电池电压
     */
    public int BatteryFix = 0;
    public int Temperature = 0;
    /**
     * 温度
     */
    public int TemperatureFix = 0;
    public int Weigh = 0;
    /**
     * 重量
     */
    public int WeighFix = 0;
    public int TDS = 0;
    /**
     * TDS
     */
    public int TDSFix = 0;

    public TapSensor() {
    }

    /**
     * 获取电池电量
     *
     * @return 0-100%
     */
    public float getPower() {
        if (BatteryFix >= 3000) {
            float ret = BatteryFix - 3000f / (4200f - 3000f);
            if (ret > 100)
                ret = 100;
            return ret;
        } else
            return 0;
    }

    @Override
    public String toString() {
        return String.format("Battery:%d/%d Temp:%d/%d Weigh:%d/%d TDS:%d/%d", Battery, BatteryFix, Temperature, TemperatureFix,
                Weigh, WeighFix, TDS, TDSFix);
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
