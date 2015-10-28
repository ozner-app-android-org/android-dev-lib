package com.ozner.device;

import android.os.Handler;
import android.os.Message;

import com.ozner.bluetooth.BluetoothIO;

/**
 * Created by zhiyongxu on 15/10/28.
 */
public abstract class FirmwareTools implements BluetoothIO.BluetoothRunnable {
    protected BluetoothIO bluetoothIO;

    private static final int MSG_Start = 1;
    private static final int MSG_Postion = 2;
    private static final int MSG_Fail = 3;
    private static final int MSG_Complete = 4;
    String filePath;

    public class FirmwareInvalidFormatExcpetion extends Exception {
    }

    ;

    public class FirmwareException extends Exception {
        private String message;

        public FirmwareException(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    ;


    protected String Platform;
    protected long Firmware;
    protected int Size;
    protected byte[] bytes;
    protected int Cheksum;


    public FirmwareTools(BluetoothIO bluetoothIO) {
        this.bluetoothIO = bluetoothIO;
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


    public interface FirmwareUpateInterface {
        void onFirmwareUpdateStart(String Address);

        void onFirmwarePosition(String Address, int Position, int size);

        void onFirmwareComplete(String Address);

        void onFirmwareFail(String Address);
    }

    protected String firmwareFile = "";
    protected FirmwareUpateInterface firmwareUpateInterface = null;


    protected void onFirmwareUpdateStart() {
        updateHandler.sendEmptyMessage(1);
    }

    protected void onFirmwarePosition(int postion, int size) {
        Message m = new Message();
        m.what = 2;
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

    UpdateHandler updateHandler = new UpdateHandler();

    class UpdateHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (firmwareUpateInterface == null) return;
            switch (msg.what) {
                case 1:
                    firmwareUpateInterface.onFirmwareUpdateStart(getAddress());
                    break;
                case 2:
                    firmwareUpateInterface.onFirmwarePosition(getAddress(), msg.arg1, msg.arg2);
                    break;
                case 3:
                    firmwareUpateInterface.onFirmwareFail(getAddress());
                    break;
                case 4:
                    firmwareUpateInterface.onFirmwareComplete(getAddress());
                    break;

            }
            super.handleMessage(msg);
        }
    }

    private String getAddress() {
        return bluetoothIO.getAddress();
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
                    if (bluetoothIO == null) {
                        throw new DeviceNotReadlyException();
                    }
                    loadFile(filePath);
                    if (!bluetoothIO.post(FirmwareTools.this))
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
}
