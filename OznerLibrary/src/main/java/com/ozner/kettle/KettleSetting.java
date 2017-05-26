package com.ozner.kettle;

import com.ozner.device.DeviceSetting;

/**
 * Created by zhiyongxu on 16/3/28.
 */
public class KettleSetting extends DeviceSetting {
    int reservationTime = 0;

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
        return PreservationMode.valueOf(get("preservationMode", 0).toString());
    }

    /**
     * 设置保温模式
     *
     * @param mode
     */
    public void preservationMode(PreservationMode mode) {
        put("preservationTemperature", mode.toString());
    }

    /**
     * 保温温度
     *
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

    public void reservationTime(int time) {
        reservationTime = time;
    }

    public int reservationTime() {
        return reservationTime;
    }
}
