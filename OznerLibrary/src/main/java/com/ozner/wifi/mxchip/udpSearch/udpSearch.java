package com.ozner.wifi.mxchip.udpSearch;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class udpSearch {

    boolean bFindGateway = false;
    // private int iReceivePort = 0; // ���յ���Ϣ�Ķ˿ں�
    // private static final int TIMEOUT = 1000; // ���ó�ʱΪ100ms
    private String udpresult = null; // ���������ص���Ϣ
    // private String ip;
    private DatagramSocket socketSend, socketReceive = null;

    /**
     * Send UDP broadcast
     *
     * @param BroadcastData
     * @param BroadcastPort
     * @param BroadcastIP
     * @param listener
     */
    public void doUdpFind(final String BroadcastData, final int BroadcastPort,
                          final String BroadcastIP, final udpSearch_Listener listener) {
        try {
            socketSend = socketReceive = new DatagramSocket();
            socketSend.setReuseAddress(true);
            socketReceive.setReuseAddress(true);
        } catch (SocketException e1) {
            e1.printStackTrace();
        }

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    // ��������Ϊ�㲥
                    socketSend.setBroadcast(true);
                    // ���÷��͵���Ϣ
                    String str = BroadcastData;
                    // ���ͺͽ��յ����ݰ�
                    byte[] dataSend = str.getBytes();

                    // ���͵�ַ
                    InetAddress address = InetAddress.getByName(BroadcastIP);
                    // �����������͵����ݱ�
                    DatagramPacket sendPacket = new DatagramPacket(dataSend,
                            dataSend.length, address, BroadcastPort);
                    int i = 0;
                    // �Ƿ���յ�����

                    while (!bFindGateway && i < 5) {
                        i++;
                        // ͨ���׽��ַ�������
                        socketSend.send(sendPacket);
                        Thread.sleep(1000l);
                    }
                    if (null == udpresult)
                        listener.onDeviceFound(null);
                    bFindGateway = true;
                    // �ر��׽���
                    socketSend.close();
                    socketReceive.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {

            @Override
            public void run() {
                byte[] dataReceive = new byte[1024];
                DatagramPacket packetReceive = new DatagramPacket(dataReceive,
                        dataReceive.length);

                while (!bFindGateway) {
                    dataReceive = new byte[1024];
                    packetReceive = new DatagramPacket(dataReceive,
                            dataReceive.length);
                    // ͨ���׽��ֽ�������
                    try {
                        socketReceive.receive(packetReceive);

                        // System.out.println("receive message is ok.");
                        // ��÷���ip��ַ
                        // ip = packetReceive.getAddress().getHostAddress();
                        // ���������ص�����
                        udpresult = new String(packetReceive.getData(),
                                packetReceive.getOffset(),
                                packetReceive.getLength());
                        // Log.i("", "data:" + udpresult);
                        // Log.i("", "ip:" + ip);
                        listener.onDeviceFound(udpresult.trim());
                    } catch (IOException e) {
                        // e.printStackTrace();
                    }
                }

            }
        }).start();
    }

    public void stopUdpFind() {
        bFindGateway = true;
    }
}
