package com.ozner.bluetooth;

import java.util.Arrays;
import java.util.Date;
import android.annotation.SuppressLint;
import com.ozner.util.ByteUtil;

public class BluetoothScanRep {
	/**
	 * 型号
	 */
	public String Model="";
	/**
	 * @deprecated
	 */
	public int ServiceId;
	/**
	 * 固件版本
	 */
	public Date Firmware;
	/**
	 * 硬解平台
	 */
	public String Platform;
	/**
	 * 自定义数据类型
	 */
	public int CustomDataType;
	/**
	 * 自定义数据长度
	 */
	public int CustomDataLength;
	/**
	 * 自定义数据 最大8个字节
	 */
	public byte[] CustomData;
	public boolean Available;
	
	public BluetoothScanRep() {
	}
	private String byteToStr(byte t)
	{
		return String.format("%1$,02d",t);
	}
	@SuppressLint("NewApi") 
	public void FromBytes(byte[] data) {
		ServiceId=ByteUtil.getShort(data, 0);
		Platform=new String(data,2,3);
		Model = new String(data, 5,6).trim();
		Firmware = new Date(data[11] + 2000 - 1900, data[12] - 1, data[13], data[14],
				data[15], data[16]);
		CustomDataType=data[17];
		CustomDataLength=data[18];
		CustomData=Arrays.copyOfRange(data, 19, 19+CustomDataLength);
		Available=data[27]!=0;
	}
}
