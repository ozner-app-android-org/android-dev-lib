package com.ozner.cup;

import android.annotation.SuppressLint;
import com.ozner.util.ByteUtil;

@SuppressLint("DefaultLocale")
/**
 * 水杯姿态信息
 * @author zhiyongxu
 */
class CupGravity {
	public float x = 0;
	public float y = 0;
	public float z = 0;

	public CupGravity() {

	}
	/**
	 * 判断是否倒立
	 * @return TRUE倒立
	 */
	public boolean IsHandstand() {
		return (z >= 150 && z <= 190);
	}
	@Override
	public String toString() {
		return String.format("x:%f y:%f x:%f", x,y,z);
	}
	
	/**
	 * 通过数组反序列化
	 * @param data
	 * @param Index
	 */
	public void FromBytes(byte[] data, int Index) {
		int tx = ByteUtil.getShort(data, Index);
		if (tx > 16384)
			tx = 16384;
		if (tx < -16384)
			tx = -16384;

		int ty = ByteUtil.getShort(data, Index + 2);
		if (ty > 16384)
			ty = 16384;
		if (ty < -16384)
			ty = -16384;

		int tz = ByteUtil.getShort(data, Index + 4);
		if (tz > 16384)
			tz = 16384;
		if (tz < -16384)
			tz = -16384;

		x = (float) (Math.asin(tx / 16384f) * 180 / Math.PI);
		y = (float) (Math.asin(ty / 16384f) * 180 / Math.PI);
		z = (float) (Math.acos(tz / 16384f) * 180 / Math.PI);
	}

	public void FromBytes(byte[] data) {
		FromBytes(data, 0);
	}
}
