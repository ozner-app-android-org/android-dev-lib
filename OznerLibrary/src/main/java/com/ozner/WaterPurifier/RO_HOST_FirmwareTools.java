package com.ozner.WaterPurifier;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.FirmwareTools;
import com.ozner.util.ByteUtil;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;

public class RO_HOST_FirmwareTools extends FirmwareTools {
    final static byte[] findKey = {0x23, 0x23, 0x24, 0x24, 0x40, 0x40, 0x2A, 0x2A};

    public RO_HOST_FirmwareTools() {
        super();
    }


    int CheckFirmwareSize(byte[] bytes) {
        int ret = 0;
        int size = bytes.length;
        for (int i = size - 1; i == 1; i--) {
            if (bytes[i] != 0) {
                return i + 2;
            }
        }
        return size;
    }

    @Override
    protected void loadFile(String path) throws Exception {
        File file = new File(path);
        FileInputStream fs = new FileInputStream(path);
        byte[] firmware = new byte[fs.available()];
        try {
            fs.read(firmware, 0, fs.available());
        } finally {
            fs.close();
        }

        Size = CheckFirmwareSize(firmware);
        if (Size > 200 * 1024) throw new FirmwareException("文件太大");
        if ((Size % 256) != 0) {
            Size = (Size / 256) * 256 + 256;
        }


        int myLoc1 = 0;
        int myLoc2 = 0;

        for (int i = 0; i < bytes.length - 6; i++) {
            if ((bytes[i] == 0x12) && (bytes[i + 1] == 0x34) && (bytes[i + 2] == 0x56)
                    && (bytes[i + 3] == 0x65) && (bytes[i + 4] == 0x43) && (bytes[i + 5] == 0x21)) {
                if (myLoc1 == 0)
                    myLoc1 = i;
                else
                    myLoc2 = i;
            }
        }
        boolean ver = false;
        for (int i = 0; i < bytes.length - findKey.length; i++) {
            ver = false;
            for (int x = 0; x < findKey.length; x++) {
                if (findKey[x] == bytes[i + x]) {
                    ver = true;
                } else {
                    ver = false;
                    break;
                }
            }
            if (ver) {
                String check = new String(bytes, i + 8, 8, Charset.forName("US-ASCII"));
                if (!"OznerROH".equals(check)) {
                    throw new FirmwareException("错误的文件");
                }
                break;
            }
        }

        String Address = getAddress();
        if (myLoc1 != 0) {
            bytes[myLoc1 + 5] = (byte) Integer.parseInt(Address.substring(0, 2), 16);
            bytes[myLoc1 + 4] = (byte) Integer.parseInt(Address.substring(3, 5), 16);
            bytes[myLoc1 + 3] = (byte) Integer.parseInt(Address.substring(6, 8), 16);
            bytes[myLoc1 + 2] = (byte) Integer.parseInt(Address.substring(9, 11), 16);
            bytes[myLoc1 + 1] = (byte) Integer.parseInt(Address.substring(12, 14), 16);
            bytes[myLoc1] = (byte) Integer.parseInt(Address.substring(15, 17), 16);
        }
        if (myLoc2 != 0) {
            bytes[myLoc2 + 5] = bytes[myLoc1];
            bytes[myLoc2 + 4] = bytes[myLoc1 + 1];
            bytes[myLoc2 + 3] = bytes[myLoc1 + 2];
            bytes[myLoc2 + 2] = bytes[myLoc1 + 3];
            bytes[myLoc2 + 1] = bytes[myLoc1 + 4];
            bytes[myLoc2] = bytes[myLoc1 + 5];
        }
        long temp = 0;

        Checksum = 0;
        int len = Size / 4;
        for (int i = 0; i < len; i++) {
            temp += ByteUtil.getUInt(bytes, i * 4);
        }
        long TempMask = 0x1FFFFFFFFL;
        TempMask -= 0x100000000L;
        Checksum = (int) (temp & TempMask);
    }

    private boolean eraseBlock(int block) throws InterruptedException {
        if (deviceIO.send(BluetoothIO.makePacket((byte) 0xc0, new byte[]{(byte) block}))) {
            Thread.sleep(1000);
            return true;
        } else
            return false;
    }

    private boolean eraseMCU() throws InterruptedException {
        if (!eraseBlock(0))
            return false;
        Thread.sleep(1000);
        if (!eraseBlock(1))
            return false;
        Thread.sleep(1000);
        if (!eraseBlock(2))
            return false;
        Thread.sleep(1000);
        if (!eraseBlock(3))
            return false;
        Thread.sleep(1000);
        return true;
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
            } else {
                onFirmwareFail();
                return false;
            }
            Thread.sleep(1000);
            byte[] data = new byte[19];
            ByteUtil.putInt(data, Size, 0);
            data[4] = 'H';
            data[5] = 'O';
            data[6] = 'S';
            ByteUtil.putInt(data, Checksum, 7);
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
