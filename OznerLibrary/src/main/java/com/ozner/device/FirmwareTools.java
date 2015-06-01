package com.ozner.device;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.ozner.util.ByteUtil;
import com.ozner.util.dbg;

public class FirmwareTools {
    public class FirmwareExcpetion extends Exception {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public FirmwareExcpetion(String message) {
            super(message);
        }
    }

    public String Platform;
    public long Firmware;
    public int Size;
    public byte[] bytes;
    public int Cheksum;
    public FirmwareTools(String path, String Address) throws FirmwareExcpetion, IOException {
        File file = new File(path);
        byte[] key = {0x23, 0x23, 0x24, 0x24, 0x40, 0x40, 0x2a, 0x2a, 0x43, 0x75, 0x70, 0x00};
        Size = (int) file.length();
        if ((Size % 256) != 0) {
            Size = (Size / 256) * 256 + 256;
        }

        if (Size > 127 * 1024) throw new FirmwareExcpetion("文件太大");

        FileInputStream fs = new FileInputStream(path);
        try {
            bytes = new byte[Size];
            fs.read(bytes,0,(int)file.length());
            int v_pos = 0;
            int myLoc1 = 0;
            int myLoc2 = 0;

            boolean ver = false;

            for (int i = 0; i < bytes.length - key.length; i++) {
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
            for (int i = 0; i < bytes.length - key.length; i++) {
                if ((bytes[i] == 0x12) && (bytes[i + 1] == 0x34) && (bytes[i + 2] == 0x56)
                        && (bytes[i + 3] == 0x65) && (bytes[i + 4] == 0x43) && (bytes[i + 5] == 0x21)) {
                    if (myLoc1 == 0)
                        myLoc1 = i;
                    else
                        myLoc2 = i;
                }
            }

            if (!ver) {
                throw new FirmwareExcpetion("错误的文件");
            } else {
                String temp = new String(bytes, v_pos + 16, 18, Charset.forName("US-ASCII"));
                if (temp == "") return;
                try {
                    Platform = temp.substring(0, 2);
                    String mon = temp.substring(3, 6);
                    String day = temp.substring(6, 8);
                    String year = temp.substring(8, 12);
                    String hour = temp.substring(12, 14);
                    String min = temp.substring(14, 16);
                    String sec = temp.substring(16, 18);

                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss", Locale.US);
                    Date date = df.parse(year + "-" + mon + "-" + day + " " + hour + ":" + min + ":" + sec);
                    Firmware = date.getTime();

                } catch (Exception e) {
                    dbg.e(e.toString());

                }
            }
            if (myLoc1 != 0)
            {
                bytes[myLoc1]=(byte)Integer.parseInt(Address.substring(0,2),16);
                bytes[myLoc1+1]=(byte)Integer.parseInt(Address.substring(3,5),16);
                bytes[myLoc1+2]=(byte)Integer.parseInt(Address.substring(6,8),16);
                bytes[myLoc1+3]=(byte)Integer.parseInt(Address.substring(9,11),16);
                bytes[myLoc1+4]=(byte)Integer.parseInt(Address.substring(12,14),16);
                bytes[myLoc1+5]=(byte)Integer.parseInt(Address.substring(15,17),16);
            }
            if (myLoc2!=0)
            {
                bytes[myLoc2]= bytes[myLoc1];
                bytes[myLoc2+1]= bytes[myLoc1+1];
                bytes[myLoc2+2]= bytes[myLoc1+2];
                bytes[myLoc2+3]= bytes[myLoc1+3];
                bytes[myLoc2+4]= bytes[myLoc1+4];
                bytes[myLoc2+5]= bytes[myLoc1+5];
            }

            long temp=0;

            Cheksum=0;
            int len=Size/4;
            for (int i=0;i<len;i++)
            {
                temp+=ByteUtil.getUInt(bytes,i*4);
            }
            long TempMask=0x1FFFFFFFFL;
            TempMask -= 0x100000000L;
            Cheksum =(int) (temp & TempMask);
        } finally {
            fs.close();
        }

    }
}
