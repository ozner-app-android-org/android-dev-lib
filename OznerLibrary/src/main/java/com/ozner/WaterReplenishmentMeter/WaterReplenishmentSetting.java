package com.ozner.WaterReplenishmentMeter;

import com.ozner.device.DeviceSetting;

/**
 * Created by zhiyongxu on 16/3/28.
 */
public class WaterReplenishmentSetting extends DeviceSetting {

    /**
     * 喷雾时间
     *
     * @return 秒单位时间
     */
    public int atomization() {
        return (Integer) get("atomization", 0);
    }

    /**
     * 设置喷雾时间
     *
     * @param time 秒单位时间
     */
    public void atomization(int time) {
        put("atomization", time);
    }

    /**
     * 喷雾时间
     *
     * @return 秒单位时间
     */
    public int massage() {
        return (Integer) get("atomization", 0);
    }

    /**
     * 设置按摩时间
     *
     * @param time 秒单位时间
     */
    public void massage(int time) {
        put("atomization", time);
    }




}
