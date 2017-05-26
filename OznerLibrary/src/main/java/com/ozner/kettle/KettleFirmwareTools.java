package com.ozner.kettle;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.FirmwareTools;
import com.ozner.util.ByteUtil;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

public class KettleFirmwareTools extends FirmwareTools {

    public KettleFirmwareTools() {
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
        Arrays.fill(bytes,(byte)0xff);

        FileInputStream fs = new FileInputStream(path);
        try {

            fs.read(bytes, 0, (int) file.length());

//            short crc=(short)0xffff;
//
//            for (int i=0;i<Size;i++)
//            {
//                crc= (short) ((byte)(crc>>8) | (crc<<8));
//                crc^=bytes[i];
//                crc^=(crc & 0xff) >> 4;
//                crc ^= (crc << 8) << 4;
//                crc ^= ((crc & 0xff) << 4) << 1;
//            }
//            Checksum=crc;

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
    static  final  byte packetSize=16;
    @Override
    protected boolean startFirmwareUpdate() throws InterruptedException {
        try {
            onFirmwareUpdateStart();
            if (Firmware == deviceIO.getFirmware()) {
                onFirmwareFail();
                return false;
            }


            if (eraseMCU()) {
                int po=0;

                while(po<Size) {
                    int t=Size-po;
                    byte len=(byte)(t>packetSize?packetSize:t);
                    byte[] data = new byte[20];
                    data[0] = (byte) 0xc1;
                    short p = (short) (po / packetSize);
                    ByteUtil.putShort(data, p, 1);
                    data[3]=len;
                    System.arraycopy(bytes, po, data, 4, 16);
                    if (!deviceIO.send(data)) {
                        onFirmwareFail();
                        return false;
                    } else {
                        onFirmwarePosition(po, Size);
                    }
                    po+=len;
                }
            }else
            {
                onFirmwareFail();
                return false;
            }
            Thread.sleep(1000);
            byte[] data = new byte[9];
            data[0]=(byte)0xc3;
            ByteUtil.putInt(data, Size, 1);
            ByteUtil.putInt(data, Checksum, 5);

            if (deviceIO.send( data)) {
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
