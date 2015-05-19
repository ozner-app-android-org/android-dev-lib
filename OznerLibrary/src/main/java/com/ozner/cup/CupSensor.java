package com.ozner.cup;

import android.annotation.SuppressLint;
import com.ozner.util.ByteUtil;

@SuppressLint("DefaultLocale")
/**
 * @category 智能杯
 * @author zhiyongxu
 * 智能杯传感器对象
 */
public class CupSensor {
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
	public int Weigth = 0;
	/**
	 * 重量
	 */
	public int WeigthFix = 0;
	public int TDS = 0;
	/**
	 * TDS
	 */
	public int TDSFix = 0;

	public CupSensor() {
	}
	/**
	 * 获取电池电量
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
		return String.format("Battery:%d/%d Temp:%d/%d Weigth:%d/%d TDS:%d/%d", Battery, BatteryFix, Temperature, TemperatureFix,
				Weigth,WeigthFix,TDS,TDSFix);
	}
	public void FromBytes(byte[] data) {
		Battery = ByteUtil.getShort(data, 0);
		BatteryFix = ByteUtil.getShort(data, 2);
		Temperature = ByteUtil.getShort(data, 4);
		TemperatureFix = ByteUtil.getShort(data, 6);
		Weigth = ByteUtil.getShort(data, 8);
		WeigthFix = ByteUtil.getShort(data, 10);
		TDS = ByteUtil.getShort(data, 12);
		TDSFix = ByteUtil.getShort(data, 14);

	}
}
