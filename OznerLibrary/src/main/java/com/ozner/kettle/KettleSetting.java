package com.ozner.kettle;

import com.ozner.device.DeviceSetting;

/**
 * Created by zhiyongxu on 16/3/28.
 */
public class KettleSetting extends DeviceSetting {
    boolean reservationEnable = false;

    /**
     * 保温时间
     *
     * @return 秒单位时间
     */
    public int preservationTime() {
        return (Integer) get("preservationTime", 0);
    }

    /**
     * 保温时间
     *
     * @param time 分单位时间
     */
    public void preservationTime(int time) {
        put("preservationTime", time);
    }

    /**
     * 保温模式
     *
     * @return
     */
    public PreservationMode preservationMode() {
        return PreservationMode.valueOf(get("preservationMode", PreservationMode.boiling.toString()).toString());
    }

    /**
     * 设置保温模式
     *
     * @param mode
     */
    public void preservationMode(PreservationMode mode) {
        put("preservationMode", mode.toString());
    }

    /**
     * 保温温度
     * @return
     */
    public int preservationTemperature() {
        return (Integer) get("preservationTemperature", 0);
    }

    /**
     * 设置保温温度
     *
     * @param temperature
     */
    public void preservationTemperature(int temperature) {
        put("preservationTemperature", temperature);
    }

    /**
     * 煮沸温度
     *
     * @return
     */
    public int boilingTemperature() {
        return (Integer) get("boilingTemperature", 0);
    }

    /**
     * 设置煮沸温度
     *
     * @param temperature
     */
    public void boilingTemperature(int temperature) {
        put("boilingTemperature", temperature);
    }

    /**
     * 预约时间
     * @param time 分钟单位
     */
    public void reservationTime(int time) {
        put("reservationTime", time);
    }

    /**
     * 获取预约时间
     * @return 分钟单位
     */
    public int reservationTime() {
        return (Integer) get("reservationTime", 0);
    }

    /**
     * 预约使能
     * @return
     */
    public boolean reservationEnable()
    {
        return reservationEnable;
    }

    /**
     * 预约使能
     * @param enable
     */
    public void reservationEnable(boolean enable)
    {
        reservationEnable=enable;
    }
}
