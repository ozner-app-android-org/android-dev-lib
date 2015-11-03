package com.ozner.wifi.mxchip.WaterPurifier;

import android.content.Context;
import android.util.Log;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.OperateCallback;
import com.ozner.device.OperateCallbackProxy;
import com.ozner.device.OznerDevice;
import com.ozner.util.ByteUtil;
import com.ozner.wifi.mxchip.CRC8;
import com.ozner.wifi.mxchip.MXChipIO;
import com.ozner.wifi.mxchip.easylink.helper.Helper;

import org.fusesource.mqtt.client.Callback;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by xzyxd on 2015/11/2.
 */
public class WaterPurifier extends OznerDevice {
    private static String SecureCode="16a21bd6";

    private static final byte GroupCode_DeviceToApp=(byte)0xFB;
    private static final byte GroupCode_AppToDevice=(byte)0xFA;
    private static final byte GroupCode_DevceToServer=(byte)0xFC;

    private static final byte Opcode_RequestStatus=(byte)0x01;
    private static final byte Opcode_RespondStatus=(byte)0x01;
    private static final byte Opcode_ChangeStatus=(byte)0x02;
    private static final byte Opcode_DeviceInfo=(byte)0x01;
    final WaterPurifierStatus status=new WaterPurifierStatus();
    boolean statusInited=false;
    final WaterPurifierImp waterPurifierImp=new WaterPurifierImp();
    public WaterPurifier(Context context, String Address, String Model, String Setting) {
        super(context, Address, Model, Setting);
    }

    @Override
    public Class<?> getIOType() {
        return MXChipIO.class;
    }

    @Override
    protected void doSetDeviceIO(BaseDeviceIO oldIO, BaseDeviceIO newIO) {
        if (oldIO!=null)
        {
            oldIO.setOnTransmissionsCallback(null);
            oldIO.unRegisterStatusCallback(waterPurifierImp);
            oldIO.setOnInitCallback(null);
        }
        if (newIO!=null)
        {
            MXChipIO io=(MXChipIO)newIO;
            io.setSecureCode(SecureCode);
            io.setOnTransmissionsCallback(waterPurifierImp);
            io.registerStatusCallback(waterPurifierImp);
            io.setOnInitCallback(waterPurifierImp);
        }
    }

    @Override
    protected void doChangeRunningMode() {
        if (getRunningMode()==RunningMode.Foreground)
        {
            updateStatus(null, null);
        }
        super.doChangeRunningMode();
    }

    public static byte[] MakeWoodyBytes(byte Group,byte OpCode,String Address,byte[] payload)
    {
        int len=10+(payload==null?3:payload.length+3);
        byte[] bytes=new byte[len];
        bytes[0]=Group;
        ByteUtil.putShort(bytes,(short)len,1);
        bytes[3]=OpCode;

        byte[] macs = MXChipIO.HexString2Bytes(Address.replace(":", ""));
        System.arraycopy(macs, 0, bytes, 4, 6);

        bytes[10]=0;//保留数据
        bytes[11]=0;//保留数据
        if (payload!=null)
            System.arraycopy(payload, 0, bytes, 12, payload.length);

        bytes[len-1]=CRC8.calcCrc8(bytes, 0, bytes.length-1);
        return bytes;
    }

    @Override
    public boolean connected() {
        return IO() != null && IO().isReady();
    }

    public boolean Power()
    {
        return status.Power;
    }
    /**
     * 打开电源
     * @param Power 开关
     * @param cb 状态回调
     */
    public void setPower(boolean Power,OperateCallback<Void> cb)
    {
        if (IO()==null)
        {
            cb.onFailure(null);
        }
        status.Power=Power;
        setStatus(cb);
    }


    public boolean Cool()
    {
        return status.Cool;
    }
    /**
     * 打开制冷
     * @param Cool 开关
     * @param cb 状态回调
     */
    public void setCool(boolean Cool,OperateCallback<Void> cb)
    {
        if (IO()==null)
        {
            cb.onFailure(null);
        }
        status.Cool=Cool;
        setStatus(cb);
    }


    public boolean Hot()
    {
        return status.Power;
    }
    /**
     * 打开加热
     * @param Hot 开关
     * @param cb 状态回调
     */
    public void setHot(boolean Hot,OperateCallback<Void> cb)
    {
        if (IO()==null)
        {
            cb.onFailure(null);
        }
        status.Hot=Hot;
        setStatus(cb);
    }

    public boolean Sterilization()
    {
        return status.Sterilization;
    }

    /**
     * 打开杀菌
     * @param Sterilization 开关
     * @param cb 状态回调
     */
    public void setSterilization(boolean Sterilization,OperateCallback<Void> cb)
    {
        if (IO()==null)
        {
            cb.onFailure(null);
        }
        status.Sterilization=Sterilization;
        setStatus(cb);
    }


    private void setStatus(OperateCallback<Void> cb)
    {
        if ((IO()==null) || (!IO().isReady()))
        {
            if (cb!=null)
                cb.onFailure(null);
            return;
        }

        IO().send(MakeWoodyBytes(GroupCode_AppToDevice, Opcode_ChangeStatus, Address(),
                        status.toBytes()),
                new OperateCallbackProxy<Void>(cb) {
                    @Override
                    public void onFailure(Throwable var1) { //失败时重新更新状态
                        updateStatus(null, null);
                        super.onFailure(var1);
                    }

                    @Override
                    public void onSuccess(Void var1) {

                        super.onSuccess(var1);
                    }
                });

    }


    private void updateStatus(BaseDeviceIO.DataSendProxy sendHandle,OperateCallback<Void> cb)
    {
        if (sendHandle==null)
        {
            if (IO()==null)
            {
                if (cb!=null)
                    cb.onFailure(null);
            }else
            {
                IO().send(MakeWoodyBytes(GroupCode_AppToDevice, Opcode_RequestStatus, Address(), null), cb);
            }
        }else
        {
            sendHandle.send(MakeWoodyBytes(GroupCode_AppToDevice, Opcode_RequestStatus, Address(), null), cb);
        }

    }


    class WaterPurifierImp implements
            BaseDeviceIO.OnTransmissionsCallback,
            BaseDeviceIO.StatusCallback,
            BaseDeviceIO.OnInitCallback
    {

        @Override
        public void onConnected(BaseDeviceIO io) {

        }

        @Override
        public void onDisconnected(BaseDeviceIO io) {

        }

        @Override
        public void onReady(BaseDeviceIO io) {
            if (io!=null)
            {

            }
        }

        @Override
        public void onIOSend(byte[] bytes) {

        }


        @Override
        public void onIORecv(byte[] bytes) {
            if ((bytes!=null) && (bytes.length>10)) {
                byte group = bytes[0];
                byte opCode = bytes[3];
                switch (group)
                {
                    case GroupCode_DeviceToApp:
                        if (opCode==Opcode_RespondStatus)
                        {
                            status.fromBytes(bytes);
                            statusInited=true;

                        }
                        break;
                    case GroupCode_DevceToServer:
                        if (opCode==Opcode_DeviceInfo)
                        {

                        }
                        break;
                }

            }
        }

        @Override
        public boolean onIOInit(BaseDeviceIO.DataSendProxy sendHandle) {
            try {
                statusInited=false;
                updateStatus(sendHandle, new OperateCallback<Void>() {
                    @Override
                    public void onSuccess(Void var1) {
                    }

                    @Override
                    public void onFailure(Throwable var1) {
                        setObject();
                    }
                });
              //  return true;
                waitObject(5000); //等待5秒接受状态更新
                if (statusInited)
                {
                    return true;
                }else
                    return false;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }



}
