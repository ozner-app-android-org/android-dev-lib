package com.ozner.WaterPurifier;

import com.ozner.device.FirmwareTools;
import com.ozner.util.ByteUtil;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;

public class RO_BLE_FirmwareTools extends FirmwareTools {
    final static byte[] findKey = {0x23, 0x23, 0x24, 0x24, 0x40, 0x40, 0x2A, 0x2A};

    public RO_BLE_FirmwareTools() {
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
        byte firmware[] = new byte[(int) file.length()];
        try {
            fs.read(firmware, 0, (int) file.length());
        } finally {
            fs.close();
        }

        Size = CheckFirmwareSize(firmware);
        if (Size > 127 * 1024) throw new FirmwareException("文件太大");
        if ((Size % 256) != 0) {
            Size = (Size / 256) * 256 + 256;
        }
        bytes = new byte[Size];
        System.arraycopy(firmware, 0, bytes, 0, firmware.length);



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
                if (!"OznerROB".equals(check)) {
                    throw new FirmwareException("错误的文件");
                }
                break;
            }
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
        byte[] data = new byte[20];
        data[0] = (byte) 0xc0;
        data[1] = (byte) block;
        data[2] = (byte) ((int)(data[0] & 0x0ff) + (int)(data[1] & 0x0ff) & 0xff);
        if (deviceIO.send(data)) {
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
                    int checksum = 0;
                    for (int x = 0; x < 19; x++) {
                        checksum += data[x] & 0x0ff;
                    }
                    data[19] = (byte) (checksum & 0xff);
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
            data[0]=(byte)0xc3;
            ByteUtil.putInt(data, Size, 1);
            data[5] = 'B';
            data[6] = 'L';
            data[7] = 'E';
            ByteUtil.putInt(data, Checksum, 8);
           /* int checksum = 0;
            for (int i = 0; i < 12; i++) {
                checksum += data[i] & 0x0ff;
            }
            data[12] = (byte) (checksum & 0xff);*/
            if (deviceIO.send(data)) {
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
