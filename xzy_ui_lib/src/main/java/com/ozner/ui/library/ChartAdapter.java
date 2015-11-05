package com.ozner.ui.library;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by zhiyongxu on 15/9/17.
 */
public abstract class ChartAdapter {
    private Date viewDate = new Date();

    private AdapterListener adapterListener;

    public void setAdapterListener(AdapterListener adapterListener) {
        this.adapterListener = adapterListener;

    }

    public String getPostionText(int postion) {
        switch (getViewMode()) {
            case Day:
                return String.valueOf(postion);
            case Week:
                return String.valueOf(postion + 1);
            case Month:
                return String.valueOf(postion + 1);
        }
        return "";
    }

    /**
     * 开始跟新
     */
    public void update() {
        if (adapterListener != null) {
            adapterListener.onUpdate(this);
        }
    }

    /**
     * 获取当前模式下最大数据量，日=24，周=7，月=当月天数
     *
     * @return
     */
    public int getMaxCount() {
        switch (getViewMode()) {
            case Day:
                return 24;
            case Week:
                return 7;
            case Month: {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(viewDate);
                return calendar.getActualMaximum(Calendar.DATE);
            }
        }
        return 1;
    }

    /**
     * 数据长度
     *
     * @return
     */
    public abstract int count();

    public abstract int getValue(int Index);

    public abstract int getMax();

    public int getMin() {
        return 0;
    }

    /**
     * 获取当前查看的时间
     *
     * @return
     */
    public Date getViewDate() {
        return viewDate;
    }

    /**
     * 设置当前查看时间
     *
     * @param viewDate
     */
    public void setViewDate(Date viewDate) {
        this.viewDate = viewDate;
    }

    public abstract ViewMode getViewMode();

    public enum ViewMode {Day, Week, Month}

    public interface AdapterListener {
        void onUpdate(ChartAdapter adapter);
    }


}
