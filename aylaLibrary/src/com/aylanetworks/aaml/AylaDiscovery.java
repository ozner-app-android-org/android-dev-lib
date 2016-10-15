//
//  AylaDiscovery.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 01/25/2013.
//  Copyright (c) 2013 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import com.aylanetworks.aaml.mdns.NetThread;
import com.aylanetworks.aaml.mdns.Packet;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import java.lang.ref.WeakReference;

public class AylaDiscovery
{
	public static final String TAG = "AylaDiscovery";

	private Boolean needInit = true;
	boolean waitForDiscovery = true;
	private String saveIpv4Addr = "";
	Boolean timesUp;
	
	private NetThread netThread = null;
	String hostName;

	// Members used for discovery (moved from AylaLanMode)
	String discoveredLanIp;

	public IPCHandler ipc = new IPCHandler(this);

	private WeakReference<AylaDevice>_device;

	public AylaDiscovery(AylaDevice device) {
		_device = new WeakReference<>(device);
	}

	String queryDeviceIpAddress(String host) {
		String deviceIpAddress = null;
		
		hostName = host;
		initialize();
		submitQuery(host);
		//TODO: Not sure how deviceIpAddress is going to be changed, I do not think this will work. 
		return deviceIpAddress;
	}
	
	// need to initialize when the network changes
	// set needInit to true in AylaLanMode.resume() & AylaConnecivityLister.onReceive()
	public void initialize() {
		
		String ipv4Addr = AylaLanMode.getLocalIpv4Address();
		if (ipv4Addr != null && !ipv4Addr.equals(saveIpv4Addr) || netThread == null) {
			needInit = true;
		}
		
        if (needInit) {
        	needInit = false;
         	
        	String msg = String.format("%s %s %s:%s %s", "I", "AylaDiscovery", "status", "OK", "initialize");
        	AylaSystemUtils.saveToLog("%s", msg);
        	saveIpv4Addr = ipv4Addr;

			// Make sure our previous thread can exit if necessary
			exit();

        	netThread = new NetThread(this);
            netThread.start();
        	AylaSystemUtils.sleep(300); // allow time for thread to start
        }
	}

	public void exit() {
        if (netThread != null) {
         	String msg = String.format("%s %s %s:%s %s", "I", "AylaDiscovery", "status", "OK", "exit");
        	AylaSystemUtils.saveToLog("%s", msg);
        	netThread.submitQuit();
        	netThread = null;
        }
    }
	
	/**
     * Handle submitting an mDNS query.
     */
	void submitQuery(String host) {
		netThread.clearQueue();
		try {
			timesUp = true; // assume time out
			int times = 0;
			countForMilliSecs(AylaNetworks.AML_DEVICE_REACHABLE_TIMEOUT); // kick off the time-out timer
			do {
				netThread.submitQuery(host); // send three multicast queries
				waitForMilliSecs (100); // space them out to avoid network congestion
				times++;
			} while (times < 3);

		} catch (Exception e) {
			String msg = String.format("%s %s %s:%s %s", "E", "AylaDiscovery", "Exception", e.getMessage(), "submitQuery");
        	AylaSystemUtils.saveToLog("%s", msg);
		}
	}
	
	
	private void countForMilliSecs (final int n) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				long t0, t1;
				t0 =  System.currentTimeMillis();
				do {
					try { // v2.30 fatal-spin-on-suspend-stuck-on-threadid
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					t1 = System.currentTimeMillis();
				} while ((t1 - t0) < n);
				
				if (timesUp) { // if not set to false by mdns IPC Handler
					continueDiscovery(true);
				}
			}
		});
		thread.start();
	}

	protected void continueDiscovery(Boolean timedOut) {

		if (waitForDiscovery) { // waiting for discovery to return. Only need one value
			waitForDiscovery = false;
			AylaDevice device = _device.get();
			if ( device == null ) {
				return;
			}

			if (discoveredLanIp != null && timedOut == false) { // we have a discovery
				if (device.lanIp != null) {
					AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaDevice", "AylaLanMode.device.LanIp", device.lanIp, "lanModeEnablContinued");
				}
				if (!TextUtils.equals(discoveredLanIp, device.lanIp)) {
					device.updateDevicesCacheLanIp(discoveredLanIp);
					device.lanIp = discoveredLanIp;
					discoveredLanIp = null;
				}
			}

			AylaReachability.determineServiceReachability(true); // determine connectivity, reachablity, and notify registered handlers,  v3.00a
			if ( device.getLanModule() == null ) {
				new AylaLanModule(device);
			}

			//TODO: why?
//			device.getLanModule().getLanModeConfig();
		}
	}

	private void waitForMilliSecs (int n) {
		long t0, t1;

		t0 =  System.currentTimeMillis();
		do {
			try { // v2.30 fatal-spin-on-suspend-stuck-on-threadid
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			t1 = System.currentTimeMillis();
		} while ((t1 - t0) < n);
	}

	/**
	 * Allow the network thread to send us messages
	 * via this IPC mechanism.
	 * @author simmons
	 */
	
	public static class IPCHandler extends Handler {

		private static final int MSG_SET_STATUS = 1;
		private static final int MSG_ADD_PACKET = 2;
		private static final int MSG_ERROR = 3;

		private WeakReference<AylaDiscovery>_discovery;
		public IPCHandler(AylaDiscovery discovery) {
			super(AylaNetworks.appContext.getMainLooper());
			_discovery = new WeakReference<AylaDiscovery>(discovery);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			AylaDiscovery discovery = _discovery.get();
			if ( discovery == null ) {
				return;
			}

			// don't process incoming IPC if we are paused.
            if (discovery.netThread == null) {
                String errMsg = String.format("%s %s %s:%s %s", "W", "AylaDiscovery", "dropping incoming message", msg.obj, "IPCHandler");
            	AylaSystemUtils.saveToLog("%s", errMsg);
                return;
            }
            
            switch (msg.what) {
            case MSG_SET_STATUS:
                break;
            case MSG_ADD_PACKET:
            	// AC000W00000####.local A /###.###.###.###
            	Packet thisPacket = (Packet)msg.obj;
            	Packet newPacket = new Packet();
            	if (thisPacket != null) {
            		newPacket = thisPacket;
            		if (newPacket.description.contains(discovery.hostName)) { // description is upper/lower case matching on the query's case
	            			int forwardSlash = thisPacket.description.indexOf('/');
	            			String ipAddress = thisPacket.description.substring(++forwardSlash);
	            			String errMsg = String.format("%s %s %s:%s %s", "I", "AylaDiscovery", "IP Address", ipAddress, "IPCHandler");
	                    	AylaSystemUtils.saveToLog("%s", msg.toString());
	                    	AylaSystemUtils.consoleMsg(errMsg, AylaSystemUtils.loggingLevel);
	            			discovery.discoveredLanIp = ipAddress; // set lanIP address
	            			discovery.timesUp = false;
	            			discovery.continueDiscovery(false);
            		} else {
            			String errMsg = String.format("%s %s %s:%s %s", "W", "AylaDiscovery", "newPacket", newPacket.toString(), "IPCHandler");
                    	AylaSystemUtils.saveToLog("%s", errMsg);
            		}
            	}
                break;
            case MSG_ERROR:
                Packet packet = new Packet();
                packet.description = ((Throwable)msg.obj).getMessage();
                break;
            default:
                String errMsg = String.format("%s %s %s:%d %s", "W", "AylaDiscovery", "Unknown activity message code", msg.what, "IPCHandler");
            	AylaSystemUtils.saveToLog("%s", errMsg);
                break;
            }
        }

        public void setStatus(String status) {
            sendMessage(Message.obtain(this, MSG_SET_STATUS, status));
        }

        public void addPacket(Packet packet) {
            sendMessage(Message.obtain(this, MSG_ADD_PACKET, packet));
        }

        public void error(Throwable throwable) {
            sendMessage(Message.obtain(this, MSG_ERROR, throwable));
        }
    };
}




