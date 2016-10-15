//
//  AylaLanMode.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 11/13/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//
package com.aylanetworks.aaml;

import android.os.Handler;

import java.io.IOException;
import java.util.Map;


public class AylaLanMode extends AylaSystemUtils {

	private static final String tag = AylaLanMode.class.getSimpleName();
	
    static String serverIpAddress = null;
    private static AylaHttpServer httpServer = null;


    // State of the session with device
    public enum lanModeSession {
        DOWN, LOCAL_REGISTRATION, KEY_EXCHANGE, UP, UNKNOWN
    }

    // Secure Setup methods / variables
    private static AylaDevice _secureSetupDevice;

    public static AylaDevice getSecureSetupDevice() {
        return _secureSetupDevice;
    }

    public static void setSecureSetupDevice(AylaDevice device) {
        if ( device != null ) {
            // Constructor automatically sets the module on the device
            AylaLanModule module = new AylaLanModule(device);
        }
        _secureSetupDevice = device;
    }

    public static void closeSecureSetup(){
        if(_secureSetupDevice != null){
            if(_secureSetupDevice.getLanModule() != null){
                if(_secureSetupDevice.getLanModule().getDevice().lanIp == GBL_MODULE_DEFAULT_WIFI_IPADDR){
                    _secureSetupDevice.setLanModule(null);

                }
            }
        }
        _secureSetupDevice = null;
    }

    public static boolean hasSecureSetupDevice() {
        return _secureSetupDevice != null;
    }

    /**
     * 
     * @param entity the cmd strucutre to send.
     * @param rs the AylaRestService object including the callback handler, caller`s responsibility for the validation. 
     * */ 
    static void sendToSecureSetupDevice(AylaLanCommandEntity entity, AylaRestService rs) {
        AylaDevice setupDevice = getSecureSetupDevice();
        saveToLog("%s, %s, %s, %s:%s, %s:%s, %s:%s.", "D", "AylaLanMode", "sendToSecureSetupDevice"
        		, "entity.jsonStr", entity.jsonStr
        		, "baseType", entity.baseType
        		, "setupDevice", setupDevice.toString());
        
        if (lanModeState == lanMode.RUNNING && setupDevice != null) {
            AylaLanModule.AylaLanSession session = setupDevice.getLanModule().getSession();

            AylaLanCommandEntity aQueuedEntity = session.nextInSendQueue();

            session.enQueueSend(entity); // queue up the datapoint to send to the device
            
            session.putCommandsOutstanding(entity.cmdId, rs);

            if (aQueuedEntity == null) { // already queued up
                session.stopSessionTimer();
                setupDevice.getLanModule().startLanModeSession(AylaRestService.PUT_LOCAL_REGISTRATION, true); // notify device to get commands
            }
        } else {
            saveToLog("%s, %s, %s:%s, %s", "E", "AylaLanMode", "lanModeState", lanModeMsg[lanModeState.ordinal()], "sendToLanModeDevice");
        }
    }
    /**
     * @return the current LAN Mode Session State
     */
    // Each device can have its session state queried now
    @Deprecated
    public static lanModeSession getSessionState() {
        return lanModeSession.DOWN;
    }

    private static class aylaHTTPD extends AylaHttpServer {
        // start the HTTP server
        aylaHTTPD() throws IOException {
            super(serverPortNumber);
            saveToLog("%s, %s, %s:%s, %s:%d, %s", "I", "AylaLanMode", "serverIpAddress", serverIpAddress,
                    "serverPortNumber", serverPortNumber, "aylaHTTPD()");
        }

        //-------------------------- Begin Main HTTPD response method ----------------------
        @Override
        public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parms, Map<String, String> files) {
            AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s:%s, %s:%s, %s:%s, %s", "I", "AylaLanMode", "uri", uri,
                    "method", method.toString(), "header", header, "parms", parms, "Response");

            // Find the module to handle this request
            String ipAddr = header.get("http-client-ip");
            if (ipAddr == null) {
                saveToLog("%s, %s, %s, %s", "E", tag, "serve", "No http-client-ip in header!");
                return new AylaHttpServer.Response(AylaHttpServer.Response.Status.NOT_FOUND, MIME_HTML, "");  
            }

            AylaDevice targetDevice = AylaDeviceManager.sharedManager().deviceWithLanIP(ipAddr);
            if (targetDevice == null) {
                saveToLog("%s, %s, %s, %s.", "E", tag, "serve", "No device found with IP " + ipAddr);
                return new AylaHttpServer.Response(AylaHttpServer.Response.Status.NOT_FOUND, MIME_HTML, ""); 
            }

            // Does this device have a LAN module yet?
            AylaLanModule targetModule = targetDevice.getLanModule();
            if (targetModule == null) {
                targetModule = new AylaLanModule(targetDevice);
            }

            return targetModule.handleRequest(uri, method, header, parms, files);
        }
    }
    //-------------------------- End Main HTTPD response method ----------------------

    // called from AylaDevice


    // --------------------------------------- BEGIN LAN MODE ENABLEMENT & DISABLEMENT ------------------------

    /**
     * Enables the application (activity) to use LAN Mode secure communication. In addition to enabling the application, the current device must
     * also be LAN Mode enabled
     *
     * @return can be AML_ERROR_OK or AML_ERROR_FAIL
     */
    public static int enable(Handler notifierHandler, Handler reachabilityHandler) {
        lanModeState = lanMode.ENABLED; // must ENABLE for LAN Mode operation, DISABLE is the default
        saveToLog("%s, %s, %s:%s, %s", "I", "AylaLanMode", "lanModeState", lanModeMsg[lanModeState.ordinal()], "enable");

        // Initialize queues
        AylaDeviceManager.sharedManager().clearQueues();

        // save handlers for callback to main activity
        AylaNotify.register(notifierHandler);
        AylaReachability.register(reachabilityHandler);

        String sIpAddr = getLocalIpv4Address();
        serverIpAddress = (sIpAddr == null) ? serverIpAddress : sIpAddr;

        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    if (httpServer == null) {
                        httpServer = new aylaHTTPD();
                        httpServer.start();
                    }

                    if (httpServer != null) {

                        lanModeState = lanMode.RUNNING;
                    } else {
                        lanModeState = lanMode.FAILED;
                    }
                } catch (IOException e) {
                    if (e.getMessage().contains("Address already in use")) {
                        lanModeState = lanMode.RUNNING;
                    } else {
                        lanModeState = lanMode.FAILED;
                        e.printStackTrace();
                    }
                }
            }
        });

        lanModeState = lanMode.STOPPED;
        thread.start();
        while (lanModeState == lanMode.STOPPED) { // wait for server to start, v2.21_ENG
            //JVM friendly NOP
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            continue;
        }

        return AML_ERROR_OK; // OK, but should only call once to enable lan mode
    }

    /**
     * General resume method. Should always be called when the app is coming to the foreground.
     * Will refresh the access token if needed.
     * Will start the HTTP server if LAN Mode is enabled
     * Resume LAN mode communication and reboot http server. Typically called after LAN mode communication is paused.
     *
     * @return can be AML_ERROR_OK or AML_ERROR_FAIL
     */
    public static int resume() {
        if (lanModeState == lanMode.DISABLED) {
            saveToLog("%s, %s, %s:%s, %s", "I", "AylaLanMode", "lanModeState", lanModeMsg[lanModeState.ordinal()], "resume");
            Boolean waitForResults = true;
            AylaReachability.determineReachability(waitForResults); // determine connectivity, reachablity, and notify registered handlers

            AylaUser.refreshAccessTokenOnExpiry(DEFAULT_ACCESS_TOKEN_REFRESH_THRRESHOLD);    // auto refresh access token if seconds to expiry is less than threshold
            return AML_ERROR_OK; // call enable for resume/disable to work
        }

        if (AylaNetworks.appContext == null) {
            // app unloaded by OS
            return AML_ERROR_FAIL;
        }

        String sIpAddr = getLocalIpv4Address();
        serverIpAddress = (sIpAddr == null) ? serverIpAddress : sIpAddr;

        lanModeState = lanMode.STOPPED;
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    if (httpServer == null) {
                        httpServer = new aylaHTTPD();
                        httpServer.start();
                    }

                    if (httpServer != null) {
                        lanModeState = lanMode.RUNNING;
                    } else {
                        lanModeState = lanMode.FAILED;
                    }
                } catch (IOException e) {
                    if (e.getMessage().contains("Address already in use")) {
                        lanModeState = lanMode.RUNNING;
                    } else {
                        lanModeState = lanMode.FAILED;
                        e.printStackTrace();
                    }
                }

                if (lanModeState == lanMode.RUNNING) {
                    AylaReachability.determineReachability(true); // determine connectivity, reachablity, and notify registered handlers
                    saveToLog("%s, %s, %s:%s, %s", "I", "AylaLanMode", "lanModeState", lanModeMsg[lanModeState.ordinal()], "resume");
                } else {
                    saveToLog("%s, %s, %s:%s, %s", "E", "AylaLanMode", "lanModeState", lanModeMsg[lanModeState.ordinal()], "resume");
                }
            }
        });
        thread.start();

        while (lanModeState == lanMode.STOPPED) {    // wait for server to start
            //JVM friendly NOP
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        AylaDeviceManager.sharedManager().resume();
        AylaUser.refreshAccessTokenOnExpiry(DEFAULT_ACCESS_TOKEN_REFRESH_THRRESHOLD);    // auto refresh access token if seconds to expiry is less than threshold

        return AML_ERROR_OK;
    }

    /**
     * Pause LAN mode communication. This method will also stop current connection to the LME device and stop http server.
     *
     * @return can be AML_ERROR_OK or AML_ERROR_FAIL
     * <p/>
     * pausedByUser indicates user changed activities, so the app is still running
     */
    public static int pause(Boolean pausedByUser) {

        if (!pausedByUser) {  // going into background vs next device
            AylaSystemUtils.serviceReachableTimeout = AylaNetworks.AML_SERVICE_REACHABLE_TIMEOUT; // always test reachability when coming out of the background
        }

        // We no longer know the reachability state of the devices.
        AylaReachability.setDeviceReachability(null, AylaNetworks.AML_REACHABILITY_UNKNOWN);

        if (lanModeState != lanMode.DISABLED) {
            if (!pausedByUser) {  // going into background vs next device, intent, fragment, etc
                AylaDeviceManager.sharedManager().pause();
            }

            // stop HTTPD
            if (httpServer != null) {
                httpServer.stop();
                httpServer = null;
                lanModeState = lanMode.STOPPED;
                AylaDeviceManager.sharedManager().stopLANModeOnAllDevices();
            } else {
                lanModeState = lanMode.FAILED;
            }
        }

        saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "AylaLanMode", "lanModeState", lanModeMsg[lanModeState.ordinal()],
                "serviceReachabilityTimeout", AylaSystemUtils.serviceReachableTimeout, "pause");

        return AML_ERROR_OK;
    }

    /**
     * This method will stop communication with all LME devices, stop the HTTP server, timers, etc. Buffered information of last connected LME device will be cleared.
     *
     * @return can be AML_ERROR_OK or AML_ERROR_FAIL
     */
    public static int disable() {
        if (lanModeState == lanMode.RUNNING) {
            pause(false); // stop httpd & timer
        }

        lanModeState = lanMode.DISABLED;
        AylaDeviceManager.sharedManager().pause();

        saveToLog("%s, %s, %s:%s, %s", "I", "AylaLanMode", "lanModeState", lanModeMsg[lanModeState.ordinal()], "disable");
        return AML_ERROR_OK;
    }


    public static boolean isLanModeEnabled() {
        return lanModeState == lanMode.ENABLED;
    }

    public static boolean isLanModeRunning() {
        return lanModeState == lanMode.RUNNING;
    }

    public static lanMode getLanModeState(){
        return lanModeState;
    }
}






