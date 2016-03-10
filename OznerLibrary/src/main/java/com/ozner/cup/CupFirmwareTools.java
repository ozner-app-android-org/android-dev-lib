package com.ozner.cup;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.FirmwareTools;
import com.ozner.util.ByteUtil;
import com.ozner.util.dbg;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CupFirmwareTools extends FirmwareTools {

    public CupFirmwareTools() {
        super();
    }

    @Override
    protected void loadFile(String path) throws Exception {
        File file = new File(path);
        byte[] key = {0x23, 0x23, 0x24, 0x24, 0x40, 0x40, 0x2a, 0x2a, 0x43, 0x75, 0x70, 0x00};
        Size = (int) file.length();
        if (Size > 127 * 1024) throw new FirmwareException("文件太大");

        if ((Size % 256) != 0) {
            Size = (Size / 256) * 256 + 256;
        }


        FileInputStream fs = new FileInputStream(path);
        try {
            bytes = new byte[Size];
            fs.read(bytes, 0, (int) file.length());
            int v_pos = 0;
            int myLoc1 = 0;
            int myLoc2 = 0;

            boolean ver = false;

            for (int i = 0; i < bytes.length - key.length; i++) {
                ver = false;
                for (int x = 0; x < key.length; x++) {
                    if (key[x] == bytes[i + x]) {
                        ver = true;
                    } else {
                        ver = false;
                        break;
                    }
                }
                if (ver) {
                    v_pos = i;
                    break;
                }
            }
            for (int i = 0; i < bytes.length - 6; i++) {
                if ((bytes[i] == 0x12) && (bytes[i + 1] == 0x34) && (bytes[i + 2] == 0x56)
                        && (bytes[i + 3] == 0x65) && (bytes[i + 4] == 0x43) && (bytes[i + 5] == 0x21)) {
                    if (myLoc1 == 0)
                        myLoc1 = i;
                    else
                        myLoc2 = i;
                }
            }

            if (!ver) {
                throw new FirmwareException("错误的文件");
            } else {
                int ver_pos = ByteUtil.getInt(bytes, v_pos + 16);
                if ((ver_pos < 0) || (ver_pos > bytes.length)) throw new FirmwareException("错误的文件");

                int day_pos = ByteUtil.getInt(bytes, v_pos + 20);
                if ((day_pos < 0) || (day_pos > bytes.length)) throw new FirmwareException("错误的文件");

                int time_pos = ByteUtil.getInt(bytes, v_pos + 24);
                if ((time_pos < 0) || (time_pos > bytes.length))
                    throw new FirmwareException("错误的文件");

                String verS = new String(bytes, ver_pos, 3, Charset.forName("US-ASCII"));
                String dayS = new String(bytes, day_pos, 11, Charset.forName("US-ASCII"));
                String timeS = new String(bytes, time_pos, 8, Charset.forName("US-ASCII"));

                if (verS == "") throw new FirmwareException("错误的文件");
                if (dayS == "") throw new FirmwareException("错误的文件");
                if (timeS == "") throw new FirmwareException("错误的文件");

                try {
                    Platform = verS.substring(0, 3);
                    SimpleDateFormat df = new SimpleDateFormat("MMM dd yyyy HH:mm:ss", Locale.US);
                    Date date = df.parse(dayS + " " + timeS);
                    Firmware = date.getTime();
                    if (!(Platform.equals("C01") || Platform.equals("C02") || (Platform.equals("C03")))) {
                        throw new FirmwareException("错误的文件");
                    }
                } catch (Exception e) {
                    dbg.e(e.toString());

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
        } finally {
            fs.close();
        }
    }


    private boolean eraseMCU() throws InterruptedException {
        if (deviceIO.send(BluetoothIO.makePacket((byte) 0x0c, new byte[]{0}))) {
            Thread.sleep(1000);
            if (deviceIO.send(BluetoothIO.makePacket((byte) 0x0c, new byte[]{1}))) {
                Thread.sleep(1000);
                return true;
            } else
                return false;
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
            }else
            {
                onFirmwareFail();
                return false;
            }
            Thread.sleep(1000);
            byte[] data = new byte[19];
            ByteUtil.putInt(data, Size, 0);
            data[4] = 'S';
            data[5] = 'U';
            data[6] = 'M';
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
