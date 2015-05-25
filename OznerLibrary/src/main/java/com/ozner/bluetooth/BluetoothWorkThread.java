package com.ozner.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.HashMap;

/**
 * Created by zhiyongxu on 15/5/22.
 */
public class BluetoothWorkThread extends BluetoothGattCallback implements Runnable  {
    private Context mContext;
    private final Object mLock = new Object();
    private Looper mLooper;
    private Thread mThread;
    private MessageHandler mHandler;
    private HashMap<String,BluetoothGatt> mDevices=new HashMap<>();
    private HashMap<String,BluetoothGatt> mConnected=new HashMap<>();
    class MessageHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
        public MessageHandler()
        {
            super(mLooper);
        }
    }
    private void connect(BluetoothDevice device)
    {
        String address=device.getAddress();
        synchronized (mLock)
        {
            if (mDevices.containsKey(address))
            {
                mDevices.get(address).disconnect();
                mDevices.get(address).close();
                mDevices.remove(address);
            }
            BluetoothGatt gatt=device.connectGatt(mContext,false,this);
            if (gatt!=null)
            {
                gatt.connect();
                waitLock();
            }
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        mLock.notify();
        if (newState==BluetoothGatt.STATE_CONNECTED)
        {
            mDevices.put(gatt.getDevice().getAddress(),gatt);
        }

    }

    private void waitLock()
    {
        try {
            mLock.wait();
        } catch (InterruptedException ex) {
        }
    }

    public BluetoothWorkThread(Context context)
    {
        mContext=context;
        mThread=new Thread(this);
        mThread.start();
        synchronized (mLock)
        {
            while(mLooper==null) {
                waitLock();
            }
        }
        mHandler=new MessageHandler();
    }

    @Override
    public void run() {
        synchronized (mLock) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Looper.prepare();
            mLooper=Looper.myLooper();
            mLock.notify();
        }
        Looper.loop();
    }


    public void quit()
    {
        mLooper.quit();
    }

}
