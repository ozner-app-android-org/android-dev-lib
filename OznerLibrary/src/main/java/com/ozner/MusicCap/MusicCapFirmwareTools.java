package com.ozner.MusicCap;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.FirmwareTools;
import com.ozner.util.ByteUtil;

import java.io.File;
import java.io.FileInputStream;

public class MusicCapFirmwareTools extends FirmwareTools {

    public MusicCapFirmwareTools() {
        super();
    }

    @Override
    protected void loadFile(String path) throws Exception {
        File file = new File(path);

        Size = (int) file.length();
        if (Size > 127 * 1024) throw new FirmwareException("文件太大");

        if ((Size % 256) != 0) {
            Size = (Size / 256) * 256 + 256;
        }
        bytes = new byte[Size];

        FileInputStream fs = new FileInputStream(path);
        try {

            fs.read(bytes, 0, (int) file.length());
            long temp = 0;
            Checksum = 0;
            int len = Size / 4;
            for (int i = 0; i < len; i++) {
                temp += ByteUtil.getUInt(bytes, i * 4);
            }
            long TempMask = 0x1FFFFFFFFL;
            TempMask -= 0x100000000L;
            Checksum = (int) (temp & TempMask);
        } finally {
            fs.close();
        }
    }


    private boolean eraseMCU() throws InterruptedException {
        if (deviceIO.send(BluetoothIO.makePacket((byte) 0xc2, null))) {
            Thread.sleep(1000);
            return true;
        } else
            return false;
    }

    @Override
    protected boolean startFirmwareUpdate() throws InterruptedException {
        try {
            onFirmwareUpdateStart();
            if (Firmware == deviceIO.getFirmware()) {
                onFirmwareFail();
                return false;
            }
            if (eraseMCU()) {

                for (int i = 0; i < Size; i += 16) {
                    byte[] data = new byte[20];
                    data[0] = (byte) 0xc1;
                    short p = (short) (i / 16);
                    ByteUtil.putShort(data, p, 1);
                    System.arraycopy(bytes, i, data, 3, 16);
                    if (!deviceIO.send(data)) {
                        onFirmwareFail();
                        return false;
                    } else {
                        onFirmwarePosition(i, Size);
                    }
                }
            }
            Thread.sleep(1000);
            byte[] data = new byte[19];
            ByteUtil.putInt(data, Size, 1);
            ByteUtil.putInt(data, Checksum, 5);
            if (deviceIO.send(BluetoothIO.makePacket((byte) 0xc3, data))) {
                onFirmwareComplete();
                Thread.sleep(5000);
                return true;
            } else {
                onFirmwareFail();
                return false;
            }

        } catch (Exception e) {
            onFirmwareFail();
            return false;
        }
    }


}
