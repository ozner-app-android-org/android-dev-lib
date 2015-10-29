package com.ozner.cup;

import android.graphics.Color;

import com.ozner.device.DeviceSetting;

/**
 * @author zhiyongxu
 *         智能杯设置
 * @category 智能杯
 */
public class CupSetting extends DeviceSetting {
    /**
     * 提醒时点亮光环
     */
    public final static int Halo_Remind = 0x1;
    /**
     * 光环根据温度提示
     */
    public final static int Halo_Temperature = 0x2;
    /**
     * 光环根据TDS提示
     */
    public final static int Halo_TDS = 0x3;
    /**
     * 快速闪烁
     */
    public final static int Halo_Fast = 0xA0;
    /**
     * 慢速闪烁
     */
    public final static int Halo_Slow = 0x90;
    /**
     * 呼吸闪烁
     */
    public final static int Halo_Breathe = 0x80;
    /**
     * 不闪烁
     */
    public final static int Halo_None = 0x00;
    /**
     * 蜂鸣器不响
     */
    public final static int Beep_Nono = 0x00;
    /**
     * 响一声
     */
    public final static int Beep_Once = 0x80;
    /**
     * 两声
     */
    public final static int Beep_Dobule = 0x90;

	/*public String name = "Ozner Cup";

	// 杯子提醒设置
	public boolean toRemind = true;
	
	public int remindStart = 7 * 60 * 60;
	// 结束提醒时间,秒单位
	public int remindEnd = 17 * 60 * 60;
	// 提醒间隔枚举
	public int remindInterval = 15;
	// 目标饮水量
	public int tagetVol = 2000;
	public boolean autoTaget = false;
	// 提醒光环的颜色
	public int haloColor = Color.GREEN;
	public int haloMode = Halo_Temperature;
	public int haloSpeed = Halo_Breathe;
	public int haloConter = 15;
	public int beepMode = Beep_Once;
	// 使用者是不是自己
	public boolean isMe = true;*/

    /**
     * 设置是否提醒
     *
     * @param value
     */
    public void RemindEnable(boolean value) {
        put("isRemind", value);
    }

    /**
     * 是否提醒
     */
    public boolean RemindEnable() {
        return (Boolean) get("isRemind", true);
    }

    /**
     * 开始提醒时间,秒单位
     */
    public int remindStart() {
        return (Integer) get("remindStart", 7 * 60 * 60);
    }

    /**
     * 设置开始提醒时间,0点开始起的秒单位
     */
    public void remindStart(int value) {
        put("remindStart", value);
    }


    /**
     * 结束提醒时间,0点开始起的秒单位
     */
    public int remindEnd() {
        return (Integer) get("remindEnd", 17 * 60 * 60);
    }

    /**
     * 设置结束提醒时间,0点开始起的秒单位
     */
    public void remindEnd(int value) {
        put("remindEnd", value);
    }


    /**
     * 提醒间隔，分钟单位
     */
    public int remindInterval() {
        return (Integer) get("remindInterval", 15);
    }

    /**
     * 设置提醒间隔,分钟单位
     */
    public void remindInterval(int value) {
        put("remindInterval", value);
    }


    /**
     * 光环颜色
     *
     * @return
     */
    public int haloColor() {
        return (Integer) get("haloColor", Color.GREEN);
    }

    /**
     * 设置光环颜色
     *
     * @param value
     */
    public void haloColor(int value) {
        put("haloColor", value);
    }

    /**
     * 光环闪烁模式
     */
    public int haloMode() {
        return (Integer) get("haloMode", Halo_TDS);
    }

    /**
     * 设置光环闪烁模式
     */
    public void haloMode(int value) {
        put("haloMode", value);
    }

    /**
     * 光环闪烁速度
     */
    public int haloSpeed() {
        return (Integer) get("haloSpeed", Halo_Breathe);
    }

    /**
     * 设置光环闪烁速度
     */
    public void haloSpeed(int value) {
        put("haloMode", value);
    }

    /**
     * 光环闪烁次数
     *
     * @return
     */
    public int haloConter() {
        return (Integer) get("haloConter", 2);
    }

    /**
     * 设置光环闪烁次数
     *
     * @param value
     */
    public void haloConter(int value) {
        put("haloConter", value);
    }

    /**
     * 目标饮水量
     *
     * @return
     */
    public int tagetVol() {
        return (Integer) get("tagetVol", 2000);
    }

    /**
     * 设置目标饮水量
     *
     * @param value
     */
    public void tagetVol(int value) {
        put("tagetVol", value);
    }

    /**
     * 响铃模式
     *
     * @return
     */
    public int beepMode() {
        return (Integer) get("beepMode", Beep_Once);
    }

    /**
     * 设置响铃模式
     *
     * @param value
     */
    public void beepMode(int value) {
        put("beepMode", value);
    }

    /**
     * 是否自动推荐饮水量
     */
    public boolean autoTaget() {
        return (Boolean) get("autoTaget", true);
    }

    /**
     * 设置是否打开推荐饮水量
     *
     * @param value
     */
    public void autoTaget(boolean value) {
        put("autoTaget", value);
    }

    /**
     * 是否属于自己的水杯
     */
    public boolean isMe() {
        return (Boolean) get("isMe", true);
    }

    /**
     * 设置水杯是否属于自己
     *
     * @param value
     */
    public void isMe(boolean value) {
        put("isMe", value);
    }

	/*
	// 构造方法通过sn来设置,并加载配置
	public CupSetting() {
		super();
	}
	
	public void setIsRemind(boolean )
	
	@Override
	public String toString() {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("name", name);
			jsonObject.put("isRemind", isRemind);
			jsonObject.put("remindStart", remindStart);
			jsonObject.put("remindEnd", remindEnd);
			jsonObject.put("remindInterval", remindInterval);
			jsonObject.put("remindColor", haloColor);
			jsonObject.put("haloMode", haloMode);
			jsonObject.put("haloSpeed", haloSpeed);
			jsonObject.put("haloConter", haloConter);
			jsonObject.put("beepMode", beepMode);
			jsonObject.put("isMe", isMe);
			jsonObject.put("tagetVol", tagetVol);
			jsonObject.put("autoTaget", autoTaget);
			return jsonObject.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return "";
		}
	}

	public void Load(String json) {
		if (json == null)
			return;
		if (json.isEmpty())
			return;

		try {
			JSONObject object = new JSONObject(json);
			if (object.has("name")) {
				name = object.getString("name");
			}
			if (object.has("isRemind")) {
				isRemind = object.getBoolean("isRemind");
			}
			if (object.has("remindStart")) {
				remindStart = object.getInt("remindStart");
			}
			if (object.has("remindEnd")) {
				remindEnd = object.getInt("remindEnd");
			}
			if (object.has("remindInterval")) {
				remindInterval = object.getInt("remindInterval");
			}
			if (object.has("remindColor")) {
				haloColor = object.getInt("remindColor");
			}
			if (object.has("isMe")) {
				isMe = object.getBoolean("isMe");
			}
			if (object.has("haloMode")) {
				haloMode = object.getInt("haloMode");
			}
			if (object.has("haloSpeed")) {
				haloSpeed = object.getInt("haloSpeed");
			}

			if (object.has("haloConter")) {
				haloConter = object.getInt("haloConter");
			}

			if (object.has("beepMode")) {
				beepMode = object.getInt("beepMode");
			}

			if (object.has("tagetVol")) {
				tagetVol = object.getInt("tagetVol");
			}

			if (object.has("autoTaget")) {
				autoTaget = object.getBoolean("autoTaget");
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}*/

}
