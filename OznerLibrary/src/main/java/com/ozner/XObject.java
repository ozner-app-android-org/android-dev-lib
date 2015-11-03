package com.ozner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.ozner.util.dbg;

/**
 * Created by xzyxd on 2015/10/30.
 * 基本对象
 */
public abstract class XObject {
    private static final String ACTION_RUNNING_MODE_CHANGE="com.ozner.XObject.RunningModeChange";
    public enum RunningMode {Background, Foreground}
    private static RunningMode runningMode=RunningMode.Foreground;
    private Context context;

    private final StatusMonitor statusMonitor=new StatusMonitor();
    private class StatusMonitor extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_RUNNING_MODE_CHANGE))
            {
                doChangeRunningMode();
            }
        }
    }

    public final Context context()
    {
        return context;
    }

    public XObject(Context context)
    {
        this.context=context;
        context.registerReceiver(statusMonitor,new IntentFilter(ACTION_RUNNING_MODE_CHANGE));
    }


    @Override
    protected void finalize() throws Throwable {
        context.unregisterReceiver(statusMonitor);
        super.finalize();
    }




    /**
     * 设置运行模式
     */
    public static void setRunningMode(Context context, RunningMode runningMode)
    {
        if (XObject.runningMode!=runningMode)
        {
            XObject.runningMode=runningMode;
            context.sendBroadcast(new Intent(ACTION_RUNNING_MODE_CHANGE));
            dbg.i("设置运行模式:" + runningMode.toString());
        }

    }

    public static RunningMode getRunningMode() {
        return XObject.runningMode;
    }

    protected void doChangeRunningMode()
    {

    }
    private final Object waitObject = new Object();
    protected void waitObject(int time) throws InterruptedException {
        synchronized (waitObject) {
            waitObject.wait(time);
        }
    }

    protected void setObject() {
        synchronized (waitObject) {
            waitObject.notify();
        }
    }


}
