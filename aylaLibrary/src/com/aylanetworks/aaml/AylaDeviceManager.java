package com.aylanetworks.aaml;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * AylaDeviceManager.java
 * aAML Library
 *
 * Created by Brian King on 6/6/15
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class AylaDeviceManager {
	
	
	private static final String tag = AylaDeviceManager.class.getSimpleName();                
	

    private static AylaDeviceManager __sharedManager;

    public static AylaDeviceManager sharedManager() {
        return __sharedManager;
    }

    static {
        __sharedManager = new AylaDeviceManager();
        __sharedManager.setAllFromCache();
    }

    // TODO: change this as a Map<dsn, device> 
    private List<AylaDevice> _devices;
    private Map<String, String> _lanDsnIpMap;

    // Private default constructor - use sharedManager() instead
    private AylaDeviceManager() {
        _devices = new ArrayList<>();
        _lanDsnIpMap = new HashMap<>();
    }

    
    /**
     * 
     * precondition:  devices are initiated in flat structure, while nodes are retrieved in getDevices 
     * each node should have a copy in first level.
     * 
     * If the device is what we are setting up, return it directly;
     * If it is EVB or gateway, return it directly;
     * If it is node, find it in the first level, and find the right gateway, return the node object in gateway.nodes[].      
     * 
     * @param dsn device dsn
     * @return device object in the right position in the hierarchy, null if missing or invalid dsn.
     * */ 
    public AylaDevice deviceWithDSN(final String dsn) {
        return deviceWithDSN(dsn, false);
    }
    // TODO: make sure no app uses this APi, and deprecate recursiveCheck. 
    public AylaDevice deviceWithDSN(final String dsn, final boolean recursiveCheck) {
    	return deviceWithDSN(dsn, recursiveCheck, false);
    }
    /*
    * TODO: registration candidates should not be added, or change AylaDeviceManager internal state
    * then we do not need isFlat param either, and the corresponding logic to validate node.gatewayDSN
    * in updateDevices should be removed/simplified. As reserving it does not hurt, would like to do it later.
    * */
    @Deprecated
    AylaDevice deviceWithDSN(final String dsn
    		, final boolean recrusiveCheck
    		, final boolean isFlat
    ){

    	if ( TextUtils.isEmpty(dsn)) {
    		return null;
    	}
    	
        // Check for the secure setup device first
        AylaDevice secureDevice = AylaLanMode.getSecureSetupDevice();
        if ( secureDevice != null && TextUtils.equals(dsn, secureDevice.dsn)) {
            return secureDevice;
        }

        AylaDevice device = null;
        synchronized (this) {
            
            for (AylaDevice d : _devices) {
            	if (TextUtils.equals(dsn, d.dsn)) {
            		device = d;
            		break;
            	}
            }
            
            if (device == null) {
            	AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "W", tag, "deviceWithDSN", "Cannot find device " + dsn + " in AylaDeviceManager first level");
            	return device;
            }
            
            if (isFlat) {
            	return device;
            }
            
            if (device.isNode()) {
            	AylaDeviceNode node = (AylaDeviceNode)device;
            	device = null;
            	AylaDeviceGateway gw = null;   
            	for (AylaDevice d : _devices) {
            		if ( !d.isGateway() ) {
            			continue;
            		}
            		
            		// d is gateway
            		gw = (AylaDeviceGateway)d;
            		if ( !TextUtils.equals(gw.dsn, node.gatewayDsn) ) {
        				continue;
        			}
            		if (gw.nodes == null) {
        				AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "W", tag, "deviceWithDSN", "gateway.nodes not initialized inside DeviceManager");
        				return null;
        			}
            		
            		for (AylaDeviceNode adn : gw.nodes) {
            			if (TextUtils.equals(node.dsn, adn.dsn)) {
            				return adn;
            			}
            		}// end of adn looop
            		
            		AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "W", tag, "deviceWithDSN"
        					, "Gateway " + node.gatewayDsn + " does not have node " + node.dsn + " in nodes array");
        			return null;
            	}// end of d loop
            	
            	AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "W", tag, "deviceWithDSN", "Cannot find gateway " + node.gatewayDsn + " for node " + node.dsn);
            }
        }// end of synchronized. 
        return device;
    }// end of deviceWithDSN           
    
    
    /**
     * @deprecated Because the logic is implemented in deviceWithDSN, 
     * and sometimes we just do not know if it is endpoint. 
     * Suggest use @link devcieWithDSN instead.
     * */  
    @Deprecated
    public AylaDevice endpointForDSN(String dsn) {
        AylaDevice device = deviceWithDSN(dsn);
        if ( device != null ) {
            return device;
        }

        // Look for the gateway nodes
        synchronized (this) {
            for ( AylaDevice aylaDevice : _devices ) {
                if ( aylaDevice.isGateway() ) {
                    AylaDeviceGateway gateway = (AylaDeviceGateway)aylaDevice;
                    if ( gateway.nodes == null ) {
                        continue;
                    }
                    for ( AylaDeviceNode node : gateway.nodes ) {
                        if ( TextUtils.equals(node.dsn, dsn) ) {
                            return node;
                        }
                    }
                }
            }
        }
        return null;
    }// end of endpointForDSN       
    
    /**
     * 
     * 
     * @param lanIP The lanIP of a device
     * @return The device object that actually holds the lan session, null if missing or invalid lanIP. 
     * */  
    // this is not related to nodes, keep as it is. 
    public AylaDevice deviceWithLanIP(String lanIP) {
        synchronized (this) {
            // Check for the secure setup device first
            AylaDevice secureDevice = AylaLanMode.getSecureSetupDevice();
            if ( secureDevice != null && lanIP.equals(secureDevice.lanIp)) {
                return secureDevice;
            }

            String dsn = _lanDsnIpMap.get(lanIP);
            if ( dsn != null ) {
                return deviceWithDSN(dsn);
            }
        }
        return null;
    }// end of deviceWithLanIP         

    
    public void pause() {
        synchronized (this) {
            for (AylaDevice device : _devices) {
                AylaLanModule module = device.getLanModule();
                if ( module != null ) {
                    module.lanModeDisable();
                }
            }
        }
    }

    public void resume() {
        synchronized (this) {
            for (AylaDevice device : _devices) {
                if ( device.lanEnabled ) {
                    AylaLanModule module = device.getLanModule();
                    if (module == null) {
                        module = new AylaLanModule(device);
                        device.setLanModule(module);
                    }
                    module.lanModeEnable();
                }
            }
        }
    }

    
    /**
     * Add device to the DeviceManager collection, update the internal hierarchy accordingly when necessary.
     * 
     * @param device Should be a valid AylaDevice object, now we do not do validation for this function.
     * @param skipUpdate   update the internal hierarchy when true, otherwise false.
     * @return true if added successfully, otherwise false. 
     * */  
    //TODO: Implement the spec, guess we do not need the skipUpdate, as the internal hierarchy should always be updated accordingly.
    public boolean addDevice(AylaDevice device, boolean skipUpdate) {
        // If we already have this device, skip it
        AylaDevice currentDevice = deviceWithDSN(device.dsn);
        if ( currentDevice != null ) {
            return false;
        }

        synchronized (this) {
            boolean added = _devices.add(device);
            if ( !TextUtils.isEmpty(device.lanIp) ) {
                _lanDsnIpMap.put(device.lanIp, device.dsn);
            }
            if ( !skipUpdate ) {
                updateDevice(device);
            }
            return added;
        }
    }// end of addDevice    
    

    /**
     * Update internal hierarchy structure. 
     * For gateway, update nodes array assuming all updated nodes have been persisted in local cache
     * For node, find the right gateway, assuming it is already in the first level in memory, update by merging with nodes array. 
     * 
     * @param device to be updated.
     * */
    //TODO: implement the spec.      This does not impact the getNodeProperties returns stale thing problem, ok for now, put it as next step.  
    public void updateDevice(AylaDevice device) {
        if ( device.properties == null ) {
            device.setPropertiesFromCache();
        }

        if ( !TextUtils.isEmpty(device.lanIp) ) {
            synchronized (this) {
                _lanDsnIpMap.put(device.lanIp, device.dsn);
            }
        }

        if (device.isGateway()) {
            // Get the nodes for the gateway from the cache
        	AylaDeviceGateway gateway = (AylaDeviceGateway) device;
        	gateway.nodes = getCachedGatewayNodes(device.dsn);
        }
    }// end of updateDevice       

    /**
     * 
     * Update device collection with the right hierarchy, and persist the structure into cache. 
     * This API deals with level 1 only, level 2 would be setup in AylaDeviceGateway.updateNodes().
     * The structure is like this:      
     * 
     * EVB1-Gateway1-EVB2-Node1-Node2-Gateway2-Node3....
     *          |                         |
     *          Node1`-Node2`             Node3`
     *          
     * Node1 in the first level can be different object from the one in gateway1.nodes array. 
     * Caller is responsible for input validation.
     * 
     * @param devices
     * */ 
    public void updateDevices(AylaDevice[] devices) {
    	if (devices == null || devices.length <1 ) {
    		AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "W", tag, "updateDevices", "devices array invalid");
    		return;
    	}
    	
        synchronized (this) {
            List<AylaDevice> newList = new ArrayList<>();
            for ( int i = 0; i < devices.length; i++ ) {
                AylaDevice newDevice = devices[i];
                if ( newDevice == null || TextUtils.isEmpty(newDevice.dsn) ) {
                    continue;
                }
                AylaDevice myDevice = deviceWithDSN(newDevice.dsn, false, true);
                if ( myDevice == null ) {
                    myDevice = newDevice;
                } else { 
                	/*
                	 * NOTE: init node.gatewayDSN for nodes in the first level. 
                	 * nodes fetched via getRegistrationCandidates does not have gatewayDSN.
                	 * */
                	AylaDeviceNode node = null;
                	AylaDeviceNode newNode = null;
                	
                	if (myDevice.isNode()) {
                		node = (AylaDeviceNode)myDevice;
                	}
                	if (newDevice.isNode()) {
                		newNode = (AylaDeviceNode)newDevice;
                	}
                	
                	if (node != null && newNode!=null && TextUtils.isEmpty(node.gatewayDsn)) {
                		node.gatewayDsn = newNode.gatewayDsn;
                	}

                    // TODO: Improve this: We need to make sure we keep our devices updated with
                    // values that come from the cloud.
                    if ( !TextUtils.equals(myDevice.lanIp, newDevice.lanIp) ) {
                        myDevice.lanIp = newDevice.lanIp;
                    }
                    if ( !TextUtils.equals(myDevice.connectionStatus, newDevice.connectionStatus) ) {
                        myDevice.connectionStatus = newDevice.connectionStatus;
                    }
                    if ( myDevice.lanEnabled != newDevice.lanEnabled) {
                        myDevice.lanEnabled = newDevice.lanEnabled;
                    }
                    if (myDevice.getKey() == null){
                        myDevice.setKey(newDevice.getKey());
                    }
                }
                // get the node in gateway.nodes[] inside AylaDeviceManager.

                if (myDevice != null) {
                	newList.add(myDevice);
                    updateDevice(myDevice);
                } else {
                    newList.add(newDevice);
                    updateDevice(newDevice);
                }
            }

            // Create an array of device containers to cache
            AylaDeviceContainer[] devicesToCache = new AylaDeviceContainer[newList.size()];
            int i = 0;
            for ( AylaDevice d : newList ) {
                devicesToCache[i] = new AylaDeviceContainer();
                devicesToCache[i].device = d;
                i++;
            }

            String json = AylaSystemUtils.gson.toJson(devicesToCache);
            AylaCache.save(AylaCache.AML_CACHE_DEVICE, json);
            _devices = newList;
        }
    }

    public void removeDevices(List<AylaDevice> devices) {
        synchronized (this) {
            _devices.removeAll(devices);
        }
    }

    public void clearQueues() {
        synchronized (this) {
            for (AylaDevice device : _devices ) {
                AylaLanModule module = device.getLanModule();
                if ( module != null ) {
                    module.getSession().clearSendQueue();
                    module.getSession().clearCommandsOutstanding();
                }
            }
        }
    }

    public void stopLANModeOnAllDevices() {
        synchronized (this) {
            for (AylaDevice device : _devices ) {
                AylaLanModule module = device.getLanModule();
                if ( module != null ) {
                    module.getSession().stopSessionTimer();
                    device.setLanModule(null);
                }
            }
        }
    }

    // Helper methods

    private static List<AylaDevice> getCachedDeviceList() {
        List<AylaDevice> results = new ArrayList<>();

        String containerJSON = AylaCache.get(AylaCache.AML_CACHE_DEVICE);
        if ( !TextUtils.isEmpty(containerJSON) ) {
            try {
                String strippedJSON = AylaDevice.stripContainers(containerJSON, AylaRestService.GET_DEVICES);
                if (!TextUtils.isEmpty(strippedJSON)) {
                    AylaDevice[] devices = AylaSystemUtils.gson.fromJson(strippedJSON, AylaDevice[].class);
                    if (devices != null) {
                        results = new ArrayList<>(Arrays.asList(devices));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return results;
    }

    /**
     * @param dsn gateway dsn  caller`s responsible for ensuring dsn is valid.
     * @return AylaDeviceNode array, null if no nodes cached for this gateway
     * */
    private static AylaDeviceNode[] getCachedGatewayNodes(String dsn) {
    	AylaDeviceNode[] nodes = null;
    	try {
            String containerJSON = AylaCache.get(AylaNetworks.AML_CACHE_NODE, dsn);
    		if (TextUtils.isEmpty(containerJSON)) {
    			return nodes;
    		}
    		
    		String strippedJSON = AylaDeviceNode.stripContainers(containerJSON, null);
    		if (TextUtils.isEmpty(strippedJSON)) {
    			return nodes;
    		}
    		
//    		nodes = AylaSystemUtils.gson.fromJson(strippedJSON, AylaDeviceNode[].class);
    		AylaDevice[] ds = AylaSystemUtils.gson.fromJson(strippedJSON, AylaDevice[].class);
    		if (ds == null) {
    			return nodes;
    		}
    		nodes = new AylaDeviceNode[ds.length];
    		for (int i=0; i<ds.length; i++) {
    			nodes[i] = (AylaDeviceNode)ds[i];
    			nodes[i].initPropertiesOwner(); 
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return nodes;
    }// end of getCachedGatewayNodes    
    
    
    /*
     * Based on the gateway dsn, get the node array. 
     * */
    static AylaDeviceNode[] getCachedGatewayNodeArray(final String dsn) {
    	AylaDeviceNode[] nodes = null;
    	
    	if ( TextUtils.isEmpty(dsn) ) {
    		return nodes;
    	}
    	
    	String containerJSON = AylaCache.get(AylaNetworks.AML_CACHE_NODE, dsn);
    	if ( !TextUtils.isEmpty(containerJSON) ) {
    		try {
    			String strippedJSON = AylaDeviceNode.stripContainers(containerJSON, null);
    			if ( !TextUtils.isEmpty(strippedJSON) ) {
    				nodes = AylaSystemUtils.gson.fromJson(strippedJSON, AylaDeviceNode[].class);
    			}
    		} catch (Exception e) {
                e.printStackTrace();
            }
    	}
    	return nodes;
    }// end of getCachedGatewayNodes  
    

    // Private section
    private void setAllFromCache() {
        synchronized (this) {
            _devices = getCachedDeviceList();
            for (AylaDevice device : _devices) {
                updateDevice(device);
            }
        }
    }// end of setAllFromCache    
    
    
    
}// end of AylaDeviceManager class       



