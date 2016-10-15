/*
 * Copyright 2011 David Simmons
 * http://cafbit.com/entry/testing_multicast_support_on_android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aylanetworks.aaml.mdns;

import android.net.wifi.WifiManager.MulticastLock;

import com.aylanetworks.aaml.AylaDiscovery;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This thread runs in the background while the user has our
 * program in the foreground, and handles sending mDNS queries
 * and processing incoming mDNS packets.
 * @author simmons
 */
public class NetThread extends Thread {

    public static final String TAG = AylaDiscovery.TAG;
    
    // the standard mDNS multicast address and port number
    private static final byte[] MDNS_ADDR =
        new byte[] {(byte) 224,(byte) 0,(byte) 0,(byte) 251};
    
    private static final int MDNS_PORT = 10276;
    private static final int MDNS_STD_PORT = 5353;

    private static final int BUFFER_SIZE = 4096;

    // It uses Ayla customized port 10276.  
    private MulticastSocket mMulticastAylaSocket;
    private MulticastSocket mMulticastStdSocket;       
    
    private NetworkInterface networkInterface;
    private InetAddress groupAddress;
    private NetUtil netUtil;
    private AylaDiscovery activity;
    
    /**
     * Construct the network thread.
     * @param aylaDiscovery
     */
    public NetThread(AylaDiscovery aylaDiscovery) {
    	
        super("net");
        this.activity = aylaDiscovery;
        netUtil = new NetUtil(AylaNetworks.appContext);
    }
    
    /**
     * Open a multicast socket on the mDNS address and port.
     * 
     * @throws IOException
     */
    private void openAylaSocket() throws IOException {
    	mMulticastAylaSocket = new MulticastSocket(MDNS_PORT+1);
        mMulticastAylaSocket.setTimeToLive(2);
        mMulticastAylaSocket.setNetworkInterface(networkInterface);
        mMulticastAylaSocket.joinGroup(groupAddress);
    }
    
    private void openStandardMDSSocket() throws IOException {
    	mMulticastStdSocket = new MulticastSocket(MDNS_STD_PORT+1);
        mMulticastStdSocket.setTimeToLive(2);
        mMulticastStdSocket.setNetworkInterface(networkInterface);
        mMulticastStdSocket.joinGroup(groupAddress);
    }

    private void closeAylaSocket(){
        if(mMulticastAylaSocket !=null){
            mMulticastAylaSocket.close();
            mMulticastAylaSocket =null;
        }
    }

    private void closemMulticastStdSocket(){
        if(mMulticastStdSocket !=null){
            mMulticastStdSocket.close();
            mMulticastStdSocket =null;
        }

    }
    

    /**
     * The main network loop.  Multicast DNS packets are received,
     * processed, and sent to the UI.
     * 
     * This loop may be interrupted by closing the multicastSocket,
     * at which time any commands in the commandQueue will be
     * processed.
     */
    @Override
    public void run() {
    	
        Set<InetAddress> localAddresses = NetUtil.getLocalAddresses();
        MulticastLock multicastLock = null;
        
        // initialize the network
        try {
            networkInterface = netUtil.getFirstWifiOrEthernetInterface();
            if (networkInterface == null) {
                throw new IOException("Your WiFi is not enabled.");
            }
            groupAddress = InetAddress.getByAddress(MDNS_ADDR); 

            multicastLock = netUtil.getWifiManager().createMulticastLock("unmote");
            multicastLock.acquire();
        } catch (IOException e) {
            activity.ipc.setStatus("Your WiFi is not enabled.");
            activity.ipc.error(e);
            return;
        }
        
        try {
        	openAylaSocket();
        } catch (IOException e) {
        	e.printStackTrace();
            activity.ipc.setStatus("cannot initialize ayla socket.");
            activity.ipc.error(e);
            return;
        }
        
        try {
        	openStandardMDSSocket();
        } catch (IOException e) {
        	e.printStackTrace();
            activity.ipc.setStatus("cannot initialize standard DNS socket.");
            activity.ipc.error(e);
            return;
        }

        // set up the buffer for incoming packets
        byte[] responseBuffer = new byte[BUFFER_SIZE];
        DatagramPacket response = new DatagramPacket(responseBuffer, BUFFER_SIZE);

        byte[] responseBuffer2 = new byte[BUFFER_SIZE];
        DatagramPacket response2 = new DatagramPacket(responseBuffer2, BUFFER_SIZE);
        
        // loop!
        while (true) {
            // zero the incoming buffer for good measure.
            java.util.Arrays.fill(responseBuffer, (byte) 0); // clear buffer
            
            java.util.Arrays.fill(responseBuffer2, (byte) 0); // clear buffer   
            // receive a packet (or process an incoming command)
            try {
            	if (mMulticastAylaSocket != null) {
            		mMulticastAylaSocket.receive(response);
            	}
                
                if (mMulticastStdSocket != null) {
                	mMulticastStdSocket.receive(response2);
                }
            } catch (IOException e) {
                // check for commands to be run
                Command cmd = commandQueue.poll();
                if (cmd == null) {
                    activity.ipc.error(e);
                    return;
                }
                // reopen the socket
                try {
                    //First close the socket in case it is still open
                    closeAylaSocket();
                    openAylaSocket();              
                } catch (IOException e1) {
                    activity.ipc.error(new RuntimeException("socket reopen: "+e1.getMessage()));
                    return;
                }
                
                try {
                    //First close the socket in case it is still open
                    closemMulticastStdSocket();
                    openStandardMDSSocket();          
                } catch (IOException e1) {
                    activity.ipc.error(new RuntimeException("socket reopen: "+e1.getMessage()));
                    return;
                }
                
                // process commands
                if (cmd instanceof QueryCommand) {
                    try {
                        query(((QueryCommand)cmd).host);
                    } catch (IOException e1) {
                        activity.ipc.error(e1);
                    }
                } else if (cmd instanceof QuitCommand) {
                    break;
                }
                continue;
            }
            // Ignore our own packet transmissions.
            if ( !localAddresses.contains(response.getAddress()) ) {
            	// parse the DNS packet
                DNSMessage message;
                try {
                    message = new DNSMessage(response.getData(), response.getOffset(), response.getLength());
                 // send the packet to the UI
                    Packet packet = new Packet(response, mMulticastAylaSocket);
                    packet.description = message.toString().trim();
                    activity.ipc.addPacket(packet);
                } catch (Exception e) {
                    activity.ipc.error(e);
                    continue;
                }
            }// end of localAddresses.response     
            
            
            if ( !localAddresses.contains(response2.getAddress()) ) {
            	// parse the DNS packet
                DNSMessage message;
                try {
                    message = new DNSMessage(response2.getData(), response2.getOffset(), response2.getLength());
                 // send the packet to the UI
                    Packet packet = new Packet(response2, mMulticastStdSocket);
                    packet.description = message.toString().trim();
                    activity.ipc.addPacket(packet);
                } catch (Exception e) {
                    activity.ipc.error(e);
                    continue;
                }
            }// end of localAddresses.response2   
            
        }// end of true loop    
        // release the multicast lock
        multicastLock.release();
        multicastLock = null;
    }// end of run.  
    
    
    
    
    /**
     * Transmit an mDNS query on the local network.
     * @param host
     * @throws IOException
     */
    private void query(String host) throws IOException {
        byte[] requestData = (new DNSMessage(host)).serialize();
        DatagramPacket request =
            new DatagramPacket(requestData, requestData.length, InetAddress.getByAddress(MDNS_ADDR), MDNS_PORT);
        
        DatagramPacket stdRequest =
                new DatagramPacket(requestData, requestData.length, InetAddress.getByAddress(MDNS_ADDR), MDNS_STD_PORT);
        
        String msg = String.format("%s %s %s:%s %s.", "I", TAG, "sending_request", host, "NetThread.query");
    	AylaSystemUtils.saveToLog("%s", msg);
    	if (mMulticastAylaSocket!=null) {
    		mMulticastAylaSocket.send(request);
    	}
        
        if (mMulticastStdSocket!=null) {
        	mMulticastStdSocket.send(stdRequest);
        }
        
    }// end of query           
    

    // inter-process communication
    // poor man's message queue
    private Queue<Command> commandQueue = new ConcurrentLinkedQueue<Command>();
    
    public static abstract class Command {
    }
    
    public static class QuitCommand extends Command {}
    public static class QueryCommand extends Command {
        public QueryCommand(String host) { this.host = host; }
        public String host;
    }
    
    public void submitQuery(String host) {
        commandQueue.offer(new QueryCommand(host));
        if (mMulticastAylaSocket != null) {
        	mMulticastAylaSocket.close();
        	mMulticastAylaSocket = null;
        }
        if (mMulticastStdSocket != null) {
        	mMulticastStdSocket.close();
        	mMulticastStdSocket = null;
        }
    }// end of submitQuery         
    
    public void submitQuit() {
        commandQueue.offer(new QuitCommand());
        if (mMulticastAylaSocket != null) {
            mMulticastAylaSocket.close();
            mMulticastAylaSocket = null;
        }
        if (mMulticastStdSocket != null) {
        	mMulticastStdSocket.close();
        	mMulticastStdSocket = null;
        }
    }// end of submitQuit            
    
    public void clearQueue() {
		commandQueue.clear();
	}
}// end of NetThread class    




class Util {

    public static String hexDump(byte[] bytes) {
        return hexDump(bytes, 0, bytes.length);
    }

    public static String hexDump(byte[] bytes, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<length; i+=16) {
            int rowSize = length - i;
            if (rowSize > 16) { rowSize = 16; }
            byte[] row = new byte[rowSize];
            System.arraycopy(bytes, offset+i, row, 0, rowSize);
            hexDumpRow(sb, row, i);
        }
        return sb.toString();
    }

    private static void hexDumpRow(StringBuilder sb, byte[] bytes, int offset) {
        sb.append(String.format("%04X: ",offset));
        for (int i=0; i<16; i++) {
            if (bytes.length > i) {
                sb.append(String.format("%02X ",bytes[i]));
            } else {
                sb.append("   ");
            }
        }
        for (int i=0; i<16; i++) {
            if (bytes.length > i) {
                char c = '.';
                int v = (int)bytes[i];
                if ((v > 0x20) && (v < 0x7F)) {
                    c = (char)v;
                }
                sb.append(c);
            }
        }
        sb.append('\n');
    }

}
