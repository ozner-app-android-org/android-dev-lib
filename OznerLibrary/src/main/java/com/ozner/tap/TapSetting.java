package com.ozner.tap;

import com.ozner.device.DeviceSetting;
/**
 * 水探头设置信息
 * @category 水探头
 * @author zhiyongxu
 *
 */
public class TapSetting extends DeviceSetting {
	/**
	 * 是否开启监测时间1
	 * @return TRUE=是
	 */
	public boolean isDetectTime1()
	{
		return (Boolean)get("isDetectTime1",false);
	}
	/**
	 * 设置是否开启监测时间1
	 * @param value TRUE=是
	 */
	public void isDetectTime1(boolean value)
	{
		put("isDetectTime1",value);
	}
	/**
	 * 是否开启监测时间2
	 * @return TRUE=是
	 */
	public boolean isDetectTime2()
	{
		return (Boolean)get("isDetectTime2",false);
	}
	/**
	 * 设置是否开启监测时间2
	 * @param value TRUE=是
	 */
	public void isDetectTime2(boolean value)
	{
		put("isDetectTime2",value);
	}	
	/**
	 * 是否开启监测时间3
	 * @return TRUE=是
	 */
	public boolean isDetectTime3()
	{
		return (Boolean)get("isDetectTime3",false);
	}
	/**
	 * 设置是否开启监测时间3
	 * @param value TRUE=是
	 */
	public void isDetectTime3(boolean value)
	{
		put("isDetectTime3",value);
	}
	/**
	 * 是否开启监测时间4
	 * @return TRUE=是
	 */
	public boolean isDetectTime4()
	{
		return (Boolean)get("isDetectTime4",false);
	}
	/**
	 * 设置是否开启监测时间4
	 * @param value TRUE=是
	 */
	public void isDetectTime4(boolean value)
	{
		put("isDetectTime4",value);
	}
	/**
	 * 获取监测时间1
	 * @return 当0点开始的秒单位时间
	 */
	public int DetectTime1()
	{
		return (Integer)get("detectTime1",0);
	}
	/**
	 * 设置监测时间1
	 * @param time 当0点开始的秒单位时间
	 */
	public void DetectTime1(int time)
	{
		put("detectTime1",time);
	}
	/**
	 * 获取监测时间2
	 * @return 当0点开始的秒单位时间
	 */
	public int DetectTime2()
	{
		return (Integer)get("detectTime2",0);
	}
	/**
	 * 设置监测时间2
	 * @param time 当0点开始的秒单位时间
	 */
	public void DetectTime2(int time)
	{
		put("detectTime2",time);
	}
	/**
	 * 获取监测时间3
	 * @return 当0点开始的秒单位时间
	 */
	public int DetectTime3()
	{
		return (Integer)get("detectTime3",0);
	}
	/**
	 * 设置监测时间3
	 * @param time 当0点开始的秒单位时间
	 */
	public void DetectTime3(int time)
	{
		put("detectTime3",time);
	}
	
	/**
	 * 获取监测时间4
	 * @return 当0点开始的秒单位时间
	 */
	public int DetectTime4()
	{
		return (Integer)get("detectTime4",0);
	}
	/**
	 * 设置监测时间4
	 * @param time 当0点开始的秒单位时间
	 */
	public void DetectTime4(int time)
	{
		put("detectTime4",time);
	}
}
