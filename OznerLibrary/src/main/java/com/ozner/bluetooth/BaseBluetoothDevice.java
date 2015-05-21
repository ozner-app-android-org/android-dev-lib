package com.ozner.bluetooth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import com.ozner.device.FirmwareTools;
import com.ozner.util.ByteUtil;
import com.ozner.util.dbg;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.Time;

@SuppressLint({ "NewApi", "HandlerLeak" })
public abstract class BaseBluetoothDevice extends BluetoothGattCallback {
	BluetoothDevice mDevice = null;
	BluetoothGatt mGatt = null;
	Context mContext;
	protected Context getContext() {
		return mContext;
	}
	String Serial = "";
	String Model = "";
	String Platform="";
	long Firmware = 0;
	
	/**
	 * 设备初始化完成标记,TRUE成功
	 */
	boolean mReadly=false;
	/**
	 * 蓝牙设备关闭回调
	 * 
	 * @author xzy
	 *
	 */
	public interface BluetoothCloseCallback {
		void OnOznerBluetoothClose(BaseBluetoothDevice device);
	}

	int mStatus = STATE_DISCONNECTED;
	/**
	 * 设备连接成功广播
	 */
	public final static String ACTION_BLUETOOTH_CONNECTED = "com.ozner.bluetooth.connected";
	public final static String ACTION_BLUETOOTH_ERROR = "com.ozner.bluetooth.error";

	/**
	 * 设备就绪广播
	 */
	public final static String ACTION_BLUETOOTH_READLY = "com.ozner.bluetooth.readly";
	/**
	 * 设备连接断开广播
	 */
	public final static String ACTION_BLUETOOTH_DISCONNECTED = "com.ozner.bluetooth.disconnected";
	/**
	 * 获取到设备信息广播
	 */
	public final static String ACTION_BLUETOOTH_DEVICE = "com.ozner.bluetooth.device";
	
	/**
	 * 连接中
	 */
	public final static int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
	/**
	 * 已连接
	 */
	public final static int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;
	/**
	 * 连接断开
	 */
	public final static int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
	/**
	 * 关闭中
	 */
	public final static int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;
	
	static final int Msg_Readly = 0x1000;
	static final int Msg_Data = 0x1001;
	static final int Msg_InitService = 0x1002;
	static final int Msg_Connect=0x2000;
	static final int Msg_Close=0x2001;
	


	static final byte opCode_DeviceInfo = (byte)0x15;
	static final byte opCode_DeviceInfoRet = (byte) 0xA5;
	static final byte opCode_UpdateTime = (byte) 0xF0;
	static final byte opCode_SetName = (byte) 0x80;
	static final byte opCode_SetBackgroundMode=(byte)0x21;
	static final byte opCode_GetFirmware=(byte)0x82;
	static final byte opCode_GetFirmwareRet=(byte)-126;
	
	BluetoothGattCharacteristic mInput = null;
	BluetoothGattCharacteristic mOutput = null;
	BluetoothGattService mService = null;
	BluetoothCloseCallback mCloseCallback = null;
	RunHandler mHandler=new RunHandler();
	private static final int ServiceId = 0xFFF0;
	final UUID Characteristic_Input = GetUUID(0xFFF2);
	final UUID Characteristic_Output = GetUUID(0xFFF1);
	final UUID GATT_CLIENT_CHAR_CFG_UUID = GetUUID(0x2902);
	
	/**
	 * 获取当前设备连接状态
	 * 
	 * @return
	 */
	public int getStatus() {
		return mStatus;
	}
	/**
	 * 获取设备序列号
	 * @return
	 */
	public String getSerial() {
		return Serial;
	}
	/**
	 * 获取设备硬解平台
	 * @return
	 */
	public String getPlatform()
	{
		return Platform;
	}
	/**
	 * 获取设备固件版本日期
	 * @return 
	 */
	public long getFirmware() {
		return Firmware;
	}
	/**
	 * 获取设备地址
	 * @return
	 */
	public String getAddress()
	{
		return mDevice.getAddress();
	}
	/**
	 * 获取设备型号
	 * @return
	 */
	public String getModel() {
		return Model;
	}
	/**
	 * 获取底层蓝牙通讯设备实例
	 * @return
	 */
	public BluetoothDevice getDevice()
	{
		return mDevice;
	}
	
	static int getShotUUID(UUID id) {
		return (int) (id.getMostSignificantBits() >> 32);
	}

	static UUID GetUUID(int id) {
		return UUID.fromString(String.format(
				"%1$08x-0000-1000-8000-00805f9b34fb", id));
	}
	class RunHandler extends Handler
	{
		public RunHandler() {
			super(Looper.getMainLooper());
		}
		@Override
		public void dispatchMessage(Message msg) {
			switch(msg.what)
			{
			case Msg_Connect:
			{
				
			}
			case Msg_Readly:
				onReadly();
				break;
			case Msg_InitService:
				initService();
				break;
			case Msg_Data:
			{
				byte[] src =(byte[])msg.obj;
				byte opCode = src[0];
				int len = src.length - 1;
				byte[] data = null;
				if (len > 0) {
					data = new byte[len];
					System.arraycopy(src, 1, data, 0, len);
				}
				try
				{
					onData(opCode, data);
				}catch(Exception e)
				{
					dbg.e("Data is Null");
					dbg.e(e.toString());
				}
				break;
			}
			}
			super.dispatchMessage(msg);
		}
	}
	public BaseBluetoothDevice(Context context, BluetoothDevice device,
			BluetoothCloseCallback callback) {
		super();
		mDevice=device;
		mCloseCallback = callback;
		mContext = context;
	}
	Handler checkHandler=new Handler();
	/**
	 * 连接蓝牙设备
	 */
	public void connect()
	{
		if (mStatus!=STATE_DISCONNECTED) return;		
		dbg.d("device:%s connect",getAddress());
		
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (mStatus!=STATE_DISCONNECTED) return;
				mStatus=STATE_CONNECTING;
				dbg.i("开始连接");
				
				mGatt = mDevice.connectGatt(mContext, false, BaseBluetoothDevice.this);
				if (!mGatt.connect())
				{
					dbg.e("连接错误");
					close();
				}else
				{
					dbg.i("开始连接计时");
					//连接完以后2秒时间去完成设置通知状态,和列举服务等初始化工作,如果3秒还未完成初始化动作,判断设备失败,关闭连接
					checkHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							if (mGatt==null) return;
							if (!mReadly)
							{
								dbg.e("%s ConnectTimeout",getAddress());
								close(); 
							}
						}
					}, 60000);
				}
			}
		});
	}
	
	/**
	 * 关闭连接
	 */
	public void close() {

		if (mStatus==STATE_DISCONNECTED) return;
		dbg.d("Close");
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (mCloseCallback != null) {
					mCloseCallback.OnOznerBluetoothClose(BaseBluetoothDevice.this);
				}
				if (mGatt != null) {
					mGatt.disconnect();
					mGatt.close();
					mGatt = null;
					dbg.i("set mGatt=null");
				}
			}
		});
	}
	
	private boolean sendTime() {
		Time time = new Time();
		time.setToNow();
		byte[] data = new byte[6];
		data[0] = (byte) (time.year - 2000);
		data[1] = (byte) (time.month + 1);
		data[2] = (byte) time.monthDay;
		data[3] = (byte) time.hour;
		data[4] = (byte) time.minute;
		data[5] = (byte) time.second;
		dbg.i(getAddress() + " 同步时间", mContext);
		return send(opCode_UpdateTime, data);
	}
	/**
	 * 设置设备名称
	 * @param name 设备名称
	 * @return TRUE成功，FALSE失败
	 */
	public boolean setName(String name) {
		byte[] data = new byte[18];
		for (int i = 0; i < 18; i++)
			data[i] = 0;
		byte[] buff = name.getBytes();
		System.arraycopy(buff, 0, data, 0, buff.length < 18 ? buff.length : 18);
		return send(opCode_SetName, data);
	}
	
	protected void onPause() {
		
	}
	static final byte opCode_ReadSensor = 0x12;
	protected void onReadly() {
		sendTime();
		sleep();
		updateBackgroundMode();
		sleep();
		sendOpCode(opCode_GetFirmware);
		sleep();
		sendBroadcastReadly();
	}

	protected void sendBroadcastReadly() {
		Intent intent = new Intent();
		intent.setAction(ACTION_BLUETOOTH_READLY);
		intent.putExtra("Address", mDevice.getAddress());
		mContext.sendBroadcast(intent);
	}
	protected void sleep() {
		try {
			Thread.sleep(400);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	protected boolean send(byte opCode, byte[] data) {
		if (mGatt==null) return false;
		dbg.i("send:%x len:%d", opCode, data.length);
		if (mInput != null) {
			ByteArrayOutputStream out = new ByteArrayOutputStream(20);
			try {
				try {
					out.write(opCode);
					if (data != null)
						out.write(data);
					mInput.setValue(out.toByteArray());
				} finally {
					out.close();
				}
			} catch (IOException e) {
				return false;
			}
			return mGatt.writeCharacteristic(mInput);
		}
		return false;
	}

	protected boolean sendOpCode(int opCode) {
		if (mGatt==null) return false;
		dbg.i("sendOpCode:%x", opCode);
		mInput.setValue(opCode, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		return mGatt.writeCharacteristic(mInput);
	}


	protected void onData(byte opCode, byte[] Data) {
		switch (opCode) {
		case opCode_DeviceInfoRet:
			Serial = new String(Data, 0, 10);
			Model = new String(Data, 11, 6);
			Firmware = ByteUtil.getShort(Data, 16);
			sendBroadcastDeviceInfo();
			dbg.d("收到数据:Serial:%s Model:%s Firmware:%s",Serial,Model,Firmware);
			break;
		case opCode_GetFirmwareRet:
			if (Data.length<14) return;
			String temp=new String(Data, Charset.forName("US-ASCII"));
			try
			{
				Platform=temp.substring(0, 2);
				String mon=temp.substring(3,6);
				String day=temp.substring(6,8);
				String year=temp.substring(8,12);
				String hour=temp.substring(12,14);
				String min=temp.substring(14,16);
				String sec=temp.substring(16,18);
				SimpleDateFormat df=new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss",Locale.US);
				Date date= df.parse(year+"-"+mon+"-"+day+" "+hour+":"+min+":"+sec);
				Firmware=date.getTime();
			}catch (Exception e) {
				dbg.e(e.toString());
			}
			break;
		}
	}

	protected void sendBroadcastDeviceInfo() {
		Intent intent = new Intent(ACTION_BLUETOOTH_DEVICE);
		intent.putExtra("Address", mDevice.getAddress());
		intent.putExtra("Model", Model);
		intent.putExtra("Firmware", Firmware);
		mContext.sendBroadcast(intent);
	}

	/**
	 * 获取设备名称
	 * @return
	 */
	public String getName()
	{
		return mDevice.getName();
	}

	@Override
	public void onCharacteristicChanged(BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic) {
		dbg.d("onCharacteristicChanged:%d", characteristic.getValue().length);
		if (characteristic.getUuid().equals(Characteristic_Output)) {			
				Message msg=new Message();
				msg.what=Msg_Data;
				msg.obj=characteristic.getValue();;
				mHandler.sendMessage(msg);
		}
		super.onCharacteristicChanged(gatt, characteristic);
	}

	/**
	 * 设置广播附加数据
	 * @param CustomType
	 * @param data
	 */
	public void updateCustomData(int CustomType,byte[] data)
	{
		
	}

	/**
	 * 获取广播附加数据
	 * @return
	 */
	public Object getCustomObject()
	{
		return null;
	}
	/**
	 * 获取设备是否在配对模式
	 * @return TRUE配对模式，FALSE正常模式
	 */
	public boolean isBindMode()
	{
		return false;
	}
	private void initService() {
		dbg.i("initService");
		if (mGatt==null)
		{
			dbg.i("mGatt is Null");
		}
		mService = mGatt.getService(GetUUID(ServiceId));
		if (mService != null) {
			mInput = mService.getCharacteristic(Characteristic_Input);
			if (mInput==null) return;
			mInput.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
			mOutput = mService.getCharacteristic(Characteristic_Output);
			if (mOutput != null) {
				BluetoothGattDescriptor desc = mOutput
						.getDescriptor(GATT_CLIENT_CHAR_CFG_UUID);
				if (desc != null) {
					desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
					mGatt.setCharacteristicNotification(mOutput, true);
					mGatt.writeDescriptor(desc);
					dbg.i("writeDescriptor");
				} else {
					close();
				}
			} else {
				close();
			}
		}else
		{
			dbg.i("start discoverServices");
			if (!mGatt.discoverServices())
			{
				sendroadcastError("discoverServices error");
				dbg.e("discoverServices error");
			}
		}
	}
	
	@Override
	public void onConnectionStateChange(BluetoothGatt gatt, int status,
			int newState) {
		
		if (status==BluetoothGatt.GATT_FAILURE)
		{
			dbg.e("%s onConnectionStateFailure:%d", getAddress(), newState);
			sendroadcastError(String.format("onConnectionStateFailure:%d", newState));

			return;
		}
		
		super.onConnectionStateChange(gatt, status, newState);
		
		mStatus = newState;
		dbg.i("%s onConnectionStateChange:%d",getAddress(), newState);
		switch (newState) {
		case BluetoothProfile.STATE_CONNECTED: {
			mReadly=false;
			sendBroadcastConnected();
			mHandler.sendEmptyMessage(Msg_InitService);
			/*if (mService == null) {
				gatt.discoverServices();
			} else
				
				initService();*/
			break;
		}
		
		case BluetoothProfile.STATE_DISCONNECTING:
		{
			onPause();
			break;
		}
		case BluetoothProfile.STATE_DISCONNECTED: {
			dbg.e("%s onDisconnected:%d",getAddress(), status);
			onPause();
			sendroadcastDisconnected();
			break;
		}
		}
		
	}
	protected void sendroadcastError(String Message) {
		Intent intent = new Intent();
		intent.setAction(ACTION_BLUETOOTH_ERROR);
		intent.putExtra("Address", mDevice.getAddress());
		intent.putExtra("Message", Message);

		mContext.sendBroadcast(intent);
	}

	protected void sendroadcastDisconnected() {
		Intent intent = new Intent();
		intent.setAction(ACTION_BLUETOOTH_DISCONNECTED);
		intent.putExtra("Address", mDevice.getAddress());
		mContext.sendBroadcast(intent);
	}

	protected void sendBroadcastConnected() {
		Intent intent = new Intent();
		intent.setAction(ACTION_BLUETOOTH_CONNECTED);
		intent.putExtra("Address", mDevice.getAddress());
		mContext.sendBroadcast(intent);
	}

	@Override
	public void onServicesDiscovered(BluetoothGatt gatt, int status) {
		super.onServicesDiscovered(gatt, status);
		if (status == BluetoothGatt.GATT_SUCCESS)
			mHandler.sendEmptyMessage(Msg_InitService);
		else
		{

			sendroadcastError(String.format("onServicesDiscovered:%d", status));
			close();
		}
	}

	@Override
	public void onCharacteristicRead(BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic, int status) {
		super.onCharacteristicRead(gatt, characteristic, status);
		if (status!=BluetoothGatt.GATT_SUCCESS)
		{
			sendroadcastError(String.format("onCharacteristicRead:%d", status));
		}
	}

	@Override
	public void onCharacteristicWrite(BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic, int status) {
		super.onCharacteristicWrite(gatt, characteristic, status);
		if (status!=BluetoothGatt.GATT_SUCCESS)
		{
			sendroadcastError(String.format("onCharacteristicWrite:%d",status));
			close();
		}
	}
	
	@Override
	public void onDescriptorWrite(BluetoothGatt gatt,
			BluetoothGattDescriptor descriptor, int status) {
		super.onDescriptorWrite(gatt, descriptor, status);

		if (status == BluetoothGatt.GATT_SUCCESS) {
			mReadly=true;
			mHandler.sendEmptyMessage(Msg_Readly);
		} else
		{

			sendroadcastError(String.format("onDescriptorWrite:%d", status));

			close();
		}
			
		
	}
	boolean misBackground=false;
	/**设置设备前后台模式
	 * 
	 * @param isBackground
	 */
	public void setBackgroundMode(boolean isBackground)
	{
		if (misBackground==isBackground) return;
		this.misBackground=isBackground;

		updateBackgroundMode();

		//如果在后台模式,判断是否处于忙碌状态,如果是等待40秒关闭连接
		if (misBackground)
		{
			if (isBusy())
			{
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (misBackground)
							close();

					}
				}, 40000);
			}else
			{
				close();
			}
		}
	}
	private void updateBackgroundMode() {
		if (mReadly)
		{
			if (!misBackground)
			{
				dbg.i("发送0X21");
				sendOpCode(opCode_SetBackgroundMode);
			}
		}
	}
	protected boolean isBackground() {
		return misBackground;
	}
	/**
	 * 设备是否忙碌
	 * @return TRUE忙碌
	 */
	public abstract boolean isBusy();
	/**
	 * 设备是否就绪
	 * @return TRUE=就绪可操作
	 */
	public boolean isReadly()
	{
		return mReadly;
	}
	public void updateInfo(String Model,String Platform,long Firmware)
	{
		this.Model=Model;
		this.Platform=Platform;
		this.Firmware=Firmware;
	}
	/**
	 * 获取传感器
	 * @return
	 */
	public Object getSensor()
	{
		return null;
	}

	protected boolean isUpdateFirmware=false;
	public interface FirmwareUpateInterface {
		void onFirmwareUpdateStart(String Address);

		void onFirmwarePosition(String Address, int Position);

		void onFirmwareComplete(String Address);

		void onFirmwareFail(String Address);
	}
	private void eraseRom()
	{
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				send((byte) 0x0c, new byte[]{0});
			}
		});
		sleep();
		sleep();

		mHandler.post(new Runnable() {
			@Override
			public void run() {
				send((byte) 0x0c, new byte[]{0});
			}
		});
		sleep();
		sleep();
	}


	public void udateFirmware(String file,FirmwareUpateInterface OnUpdateInterface) throws IOException, FirmwareTools.FirmwareExcpetion {
		FirmwareTools tools=new FirmwareTools(file);
		final FirmwareUpateInterface callback=OnUpdateInterface;
		isUpdateFirmware=true;
		Thread thread=new Thread(new Runnable() {
			@Override
			public void run() {
				callback.onFirmwareUpdateStart(getAddress());
				eraseRom();
				
				callback.onFirmwareComplete(getAddress());

			}
		});
		thread.start();
	}
}
