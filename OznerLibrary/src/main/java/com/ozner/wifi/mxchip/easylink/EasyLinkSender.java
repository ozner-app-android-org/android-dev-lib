/**
 *
 */
package com.ozner.wifi.mxchip.easylink;

import com.ozner.wifi.mxchip.easylink.helper.Helper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * @author Perry
 * @date 2014-9-13
 */
public class EasyLinkSender {
    private static int START_FLAG1 = 0x5AA;
    private static int START_FLAG2 = 0x5AB;
    private static int START_FLAG3 = 0x5AC;
    private static int UDP_START_PORT = 50000;
    private static byte send_data[] = new byte[128];
    private static byte buffer[] = new byte[1500];
    private static DatagramSocket udpSocket;
    private static int len;
    private InetAddress address = null;
    private DatagramPacket send_packet = null;
    private int port;
    private boolean small_mtu;

    public EasyLinkSender() {
        try {
            udpSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        udpSocket.close();
    }

    public void setSettings(String SSID, String passwrod, int ipAddress) {
        try {

            byte key[] = passwrod.getBytes("UTF-8");
            ;
            byte ssid[] = SSID.getBytes("UTF-8");
            ;
            byte user_info[] = new byte[5];

            user_info[0] = 0x23;
            String strIP = String.format("%08x", ipAddress);
            System.arraycopy(Helper.hexStringToBytes(strIP), 0, user_info, 1, 4);

            int broadcatIp = 0xFE000000 | ipAddress;
            String ipString = ((broadcatIp & 0xff) + "." + (broadcatIp >> 8 & 0xff) + "."
                    + (broadcatIp >> 16 & 0xff) + "." + (broadcatIp >> 24 & 0xff));
            this.address = InetAddress.getByName(ipString);

            short checksum = 0;
            udpSocket = new DatagramSocket();
            udpSocket.setBroadcast(true);
            int i = 1;
            send_data[0] = (byte) (3 + ssid.length + key.length + user_info.length + 2);
            send_data[i++] = (byte) ssid.length;
            send_data[i++] = (byte) key.length;

            int j;
            for (j = 0; j < ssid.length; ++i) {
                send_data[i] = ssid[j];
                ++j;
            }

            for (j = 0; j < key.length; ++i) {
                send_data[i] = key[j];
                ++j;
            }

            for (j = 0; j < user_info.length; ++i) {
                send_data[i] = user_info[j];
                ++j;
            }

            for (j = 0; j < i; ++j) {
                checksum = (short) (checksum + (send_data[j] & 255));
            }

            send_data[i++] = (byte) ((checksum & 0xffff) >> 8);
            send_data[i++] = (byte) (checksum & 255);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean send() {
        try {
            this.port = UDP_START_PORT;
            int k = 0;
            this.UDP_SEND(START_FLAG1);
            this.UDP_SEND(START_FLAG2);
            this.UDP_SEND(START_FLAG3);
            int i = 0;

            for (int j = 1; i < send_data[0]; ++i) {
                len = j * 256 + (send_data[i] & 255);
                this.UDP_SEND(len);
                if (i % 4 == 3) {
                    ++k;
                    len = 1280 + k;
                    this.UDP_SEND(len);
                }

                ++j;
                if (j == 5) {
                    j = 1;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public void SetSmallMTU(boolean onoff) {
        small_mtu = onoff;
    }

    private void UDP_SEND(int length) {
        try {
            if (small_mtu) {
                if (length > 0x500)
                    length -= 0x500;
                if (length < 0x40)
                    length += 0xB0;
            }
            send_packet = new DatagramPacket(buffer, length, address, port);
            udpSocket.send(send_packet);
            port++;
            Thread.sleep(10);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
