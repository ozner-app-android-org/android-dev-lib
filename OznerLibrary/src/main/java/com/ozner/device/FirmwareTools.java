package com.ozner.device;

import android.os.Handler;
import android.os.Message;

import com.ozner.bluetooth.BluetoothIO;

/**
 * Created by zhiyongxu on 15/10/28.
 */
public abstract class FirmwareTools implements BluetoothIO.BluetoothRunnable {
    private static final int MSG_Start = 1;
    private static final int MSG_Position = 2;
    private static final int MSG_Fail = 3;
    private static final int MSG_Complete = 4;
    protected BluetoothIO deviceIO = null;
    protected String Platform;
    protected long Firmware;
    protected int Size;
    protected byte[] bytes;
    protected int Checksum;
    protected String firmwareFile = "";
    protected FirmwareUpateInterface firmwareUpateInterface = null;
    String filePath;
    UpdateHandler updateHandler = new UpdateHandler();

    public FirmwareTools() {

    }

    public void bind(BluetoothIO deviceIO) {
        this.deviceIO = deviceIO;
    }

    protected abstract void loadFile(String path) throws Exception;

    @Override
    public void run(BaseDeviceIO.DataSendProxy sendHandle) {
        try {
            startFirmwareUpdate(sendHandle);
        } catch (Exception e) {
            Message message = new Message();
            message.obj = e;
            message.what = MSG_Fail;
            updateHandler.sendMessage(message);
        }
    }

    protected abstract boolean startFirmwareUpdate(BaseDeviceIO.DataSendProxy sendHandle) throws InterruptedException;

    protected void onFirmwareUpdateStart() {
        updateHandler.sendEmptyMessage(MSG_Start);
    }

    protected void onFirmwarePosition(int postion, int size) {
        Message m = new Message();
        m.what = MSG_Position;
        m.arg1 = postion;
        m.arg2 = size;
        updateHandler.sendMessage(m);
    }

    protected void onFirmwareFail() {
        updateHandler.sendEmptyMessage(MSG_Fail);
    }

    protected void onFirmwareComplete() {
        updateHandler.sendEmptyMessage(MSG_Complete);
    }

    protected String getAddress() {
        return deviceIO != null ? deviceIO.getAddress() : null;
    }

    public void setFirmwareUpateInterface(FirmwareUpateInterface firmwareUpateInterface) {
        this.firmwareUpateInterface = firmwareUpateInterface;
    }

    public void udateFirmware(String file) {
        firmwareFile = file;
        this.filePath = file;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (deviceIO == null) {
                        throw new DeviceNotReadlyException();
                    }

                    loadFile(filePath);
                    if (!deviceIO.post(FirmwareTools.this))
                        throw new DeviceNotReadlyException();
                } catch (Exception e) {
                    Message message = new Message();
                    message.obj = e;
                    message.what = MSG_Fail;
                    updateHandler.sendMessage(message);
                }
            }
        }).start();
    }

    public interface FirmwareUpateInterface {
        void onFirmwareUpdateStart(String Address);

        void onFirmwarePosition(String Address, int Position, int size);

        void onFirmwareComplete(String Address);

        void onFirmwareFail(String Address);
    }

    public class FirmwareException extends Exception {
        private String message;

        public FirmwareException(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    class UpdateHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (firmwareUpateInterface == null) return;
            switch (msg.what) {
                case MSG_Start:
                    firmwareUpateInterface.onFirmwareUpdateStart(getAddress());
                    break;
                case MSG_Position:
                    firmwareUpateInterface.onFirmwarePosition(getAddress(), msg.arg1, msg.arg2);
                    break;
                case MSG_Fail:
                    firmwareUpateInterface.onFirmwareFail(getAddress());
                    break;
                case MSG_Complete:
                    firmwareUpateInterface.onFirmwareComplete(getAddress());
                    break;

            }
            super.handleMessage(msg);
        }
    }
}
