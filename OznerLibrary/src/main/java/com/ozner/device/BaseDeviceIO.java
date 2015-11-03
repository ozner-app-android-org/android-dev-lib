package com.ozner.device;

import android.content.Context;
import android.content.Intent;

import com.ozner.XObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhiyongxu on 15/10/28.
 * IO基类
 */
public abstract class BaseDeviceIO extends XObject {

    String Model = "";
    boolean isReady = false;


    /**
     * 开始连接设备事件
     */
    public final static String ACTION_DEVICE_CONNECTING = "com.ozner.device.connecting";

    /**
     * 设备连接并初始化完成事件
     */
    public final static String ACTION_DEVICE_CONNECTED = "com.ozner.device.connected";


    /**
     * 设备连接断开事件
     */
    public final static String ACTION_DEVICE_DISCONNECTED = "com.ozner.device.disconnected";
    public final static String Extra_Address="Address";

    OnInitCallback onInitCallback = null;
    CheckTransmissionsCompleteCallback checkTransmissionsCompleteCallback =null;
    OnTransmissionsCallback onTransmissionsCallback=null;
    final ArrayList<StatusCallback> statusCallback = new ArrayList<>();

    public BaseDeviceIO(Context context,String Model)
    {
        super(context);
        this.Model = Model;
    }

    public String getModel() {
        return this.Model;
    }

    public boolean isReady() {
        return isReady;
    }

    /**
     * 异步方法
     * @param bytes 发送的数据
     * @return 返回TRUE进入队列,FALSE加入操作队列失败
     */
    public abstract boolean send(byte[] bytes);

    /**
     * 异步方法,带回调返回
     * @param bytes 发送的数据
     * @param callback 在发送成功以后调用onSuccess,失败或者异常时调用onFailure
     * @return 返回TRUE进入队列,FALSE加入操作队列失败
     */
    public abstract boolean send(byte[] bytes,OperateCallback<Void> callback);

    public abstract void close();

    public abstract void open() throws DeviceNotReadyException;

    public abstract boolean connected();


    /**
     * 在后台模式调用完成doReady以后会调用doComplete来检查当前传输是否完成,没有继续等待,完成以后关闭连接
     */
    public void setCheckTransmissionsCompleteCallback(CheckTransmissionsCompleteCallback cb)
    {
        checkTransmissionsCompleteCallback=cb;
    }

    /**
     * 设置数据传输监听回调
     * @param cb
     */
    public void setOnTransmissionsCallback(OnTransmissionsCallback cb)
    {
        onTransmissionsCallback=cb;
    }

    /**
     * 在后台模式调用完成doReady以后会调用doComplete来检查当前传输是否完成,没有继续等待,完成以后关闭连接
     */
    protected boolean doCheckTransmissionsComplete()
    {
        if (checkTransmissionsCompleteCallback!=null)
        {
            return checkTransmissionsCompleteCallback.CheckTransmissionsComplete(this);
        }else
            return true;
    }

    /**
     * 调用数据监听回调
     */
    protected void doSend(byte[] bytes)
    {
        if (onTransmissionsCallback!=null)
        {
            onTransmissionsCallback.onIOSend(bytes);
        }
    }

    /**
     * 调用数据监听回调
     */
    protected void doRecv(byte[] bytes)
    {
        if (onTransmissionsCallback!=null)
        {
            onTransmissionsCallback.onIORecv(bytes);
        }
    }



    public void registerStatusCallback(StatusCallback callback) {
        synchronized (statusCallback) {
            if (!statusCallback.contains(callback))
                statusCallback.add(callback);
        }
    }

    public void unRegisterStatusCallback(StatusCallback callback) {
        synchronized (statusCallback) {
            statusCallback.remove(callback);
        }
    }

    protected void doConnecting()
    {
        Intent intent=new Intent(ACTION_DEVICE_CONNECTING);
        intent.putExtra(Extra_Address,getAddress());
        context().sendBroadcast(intent);
    }
    protected void doConnected() {
        isReady = false;
        synchronized (statusCallback) {
            for (StatusCallback cb : statusCallback)
                cb.onConnected(this);
        }


    }

    protected void doDisconnected() {
        isReady = false;
        synchronized (statusCallback) {
            List<StatusCallback> list=new ArrayList<>(statusCallback);
            for (StatusCallback cb : list)
                cb.onDisconnected(this);
        }
        Intent intent=new Intent(ACTION_DEVICE_DISCONNECTED);
        intent.putExtra(Extra_Address,getAddress());
        context().sendBroadcast(intent);
    }

    protected void doReady() {
        isReady = true;
        synchronized (statusCallback) {
            for (StatusCallback cb : statusCallback)
                cb.onReady(this);
        }
        Intent intent=new Intent(ACTION_DEVICE_CONNECTED);
        intent.putExtra(Extra_Address,getAddress());
        context().sendBroadcast(intent);
    }


    /**
     * 蓝牙连接初始化完成时的回调
     */
    public void setOnInitCallback(OnInitCallback onInitCallback) {
        this.onInitCallback = onInitCallback;
    }




    protected boolean doInit(DataSendProxy handle) {
        return onInitCallback == null || onInitCallback.onIOInit(handle);
    }



    public abstract String getName();

    public abstract String getAddress();


    public interface StatusCallback {
        void onConnected(BaseDeviceIO io);
        void onDisconnected(BaseDeviceIO io);
        void onReady(BaseDeviceIO io);
    }

    /**
     * 传送检查回调
     */
    public interface CheckTransmissionsCompleteCallback
    {
        /**
         * 在后台模式调用完成doReady以后会调用doComplete来检查当前传输是否完成,没有继续等待,完成以后关闭连接
         * @return true 传送完成,可以断开连接
         */
        boolean CheckTransmissionsComplete(BaseDeviceIO io);
    }

    /**
     * 数据传输监听回调
     */
    public interface OnTransmissionsCallback
    {
        /**
         * 接口发送数据时调用该回调
         * @param bytes
         */
        void onIOSend(byte[] bytes);

        /**
         * 接口收到数据时调用
         * @param bytes
         */
        void onIORecv(byte[] bytes);
    }


    public interface OnInitCallback {
        /**
         * 连接完成以后,通过回调来继续初始化操作
         *
         * @param sendHandle 发送接口代理
         * @return 返回FALSE初始化失败
         */
        boolean onIOInit(DataSendProxy sendHandle);
    }



    public abstract class DataSendProxy {
        public abstract boolean send(byte[] data);
        public abstract boolean send(byte[] data,OperateCallback<Void> callback);

    }


}
