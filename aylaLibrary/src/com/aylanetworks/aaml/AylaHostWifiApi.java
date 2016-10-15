//
//  AylaHostWifiApi.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 10/08/12
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.GroupCipher;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.PairwiseCipher;
import android.net.wifi.WifiConfiguration.Protocol;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.util.List;

class AylaHostWifiApi
{
	public static final String ACTION_CURRENT_CONNECTION="currentConnection";
	public static final String ACTION_REMOVE_CONFIGURED_CONNECTION="removeConfiguredConnection";
	public static final String ACTION_REMOVE_CONFIGURED_CONNECTIONS="removeConfiguredConnections";
	public static final String ACTION_CONFIGURED_CONNECTIONS="configuredConnections";
	public static final String ACTION_SCAN_RESULTS="scanResults";
	public static final String ACTION_WIFI_ENABLE="wifiEnable";
	public static final String ACTION_WIFI_STATE="wifiState";
	public static final String ACTION_NETWORK_CONNECT="networkConnect";
	public static final String ACTION_REMOVE_NETWORK="removeNetwork";
	public static final String ACTION_DNS_CHECK="dnsCheck";
	

	// Constants for security types: mobile
	public static final String WPA2 = "WPA2";
	public static final String WPA = "WPA";
	public static final String WEP = "WEP";
	public static final String OPEN = "OPEN";
	// EAP Enterprise fields
	public static final String WPA_EAP = "WPA_EAP";
	public static final String IEEE8021X = "IEEE8021X";
	public static final String[] EAP_METHOD = { "PEAP", "TLS", "TTLS" };
	public static final int WEP_PASSWORD_AUTO = 0;
	public static final int WEP_PASSWORD_ASCII = 1;
	public static final int WEP_PASSWORD_HEX = 2;

	// Constants for security types: module
	public static final String WPA_AES = "WPA2 Personal AES"; // WPA2
	public static final String WPA_MIX = "WPA2 Personal Mixed"; //WPA
	// public static final String WEP = "WEP";  // WEP
	public static final String NONE = "None"; // OPEN
	public static final String UNKNOWN = "Unknown"; // WPS?

	private static final String TAG = "HostWifiApi";
	private static Context context;
	private List<ScanResult> wScanResults;
	private Boolean discovering;
	WifiManager wWifiManager;
	  
	WifiApiResult execute(String action, JSONObject args,  Context appContext) {
		Log.d(TAG, "execute() called");
		WifiApiResult result = null;
		Boolean rc = false;

		context = appContext;
		wWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

		// Get connection info on the established network
		// Args: None
		// Returns: JSON object with attributes of the network currently connected (null if not connected)
		if (ACTION_CURRENT_CONNECTION.equals(action)) {
			try {
				Log.d(TAG, "Entering " + ACTION_CURRENT_CONNECTION);

				WifiInfo info = wWifiManager.getConnectionInfo();
				Log.d(TAG, "ACTION_CURRENT_CONNECTION getConnectionInfo(): " + info.toString());

				String ssid = info.getSSID();
				if ( TextUtils.isEmpty(ssid)) {
					ssid = "\"\"";	// no current connection
				} else {
					ssid = convertToQuotedString(info.getSSID());
				}

				String currentResult = "{"
						+ "\"ssid\":" +  Uri.encode(ssid) + ","
						+ "\"net_id\":" + "\"" + info.getNetworkId() + "\"" + ","
						+ "\"hidden\":" + "\"" + info.getHiddenSSID() + "\"" +  ","
						+ "\"rssi\":" + "\"" + info.getRssi() + "\"" +
						"}";

				result = new WifiApiResult(WifiApiResult.Status.OK, currentResult);
			} catch (Exception Ex) {
				Log.d(TAG +" - " + ACTION_CURRENT_CONNECTION, "Exception: "+ Ex.getMessage());
				result = new WifiApiResult(WifiApiResult.Status.ERROR, Ex.getMessage());
			}
		}



		// Remove a configured connection from the list
		//Args: netID of the network to remove in the configured connections list
		//returns: true on success, false on failure
		else if (ACTION_REMOVE_CONFIGURED_CONNECTION.equals(action)) {
			try {
				Log.d(TAG, "Entering " + ACTION_REMOVE_CONFIGURED_CONNECTION);
				String arg = args.getString("net_id");
				int netID = Integer.parseInt(arg);
				Boolean rcBool;

				rcBool = wWifiManager.removeNetwork (netID); // remove the wifi profile

				Log.d(TAG, "ACTION_REMOVE_CONFIGURED_CONNECTION removeNetwork(netID), netID: " + netID + " success: " + rcBool);

				String jsonSuccess = "{"
						+ "\"success\":" + "\"" + rcBool + "\"" +
						"}";
				result = new WifiApiResult(WifiApiResult.Status.OK, jsonSuccess);
			} catch (Exception Ex) {
				Log.d(TAG +" - " + ACTION_REMOVE_CONFIGURED_CONNECTION, "Exception: "+ Ex.getMessage());
				result = new WifiApiResult(WifiApiResult.Status.ERROR, Ex.getMessage());
			}
		}




		// Remove a configured connection from the list
		//Args: netID of the network to remove in the configured connections list
		//returns: true on success, false on failure
		else if (ACTION_REMOVE_CONFIGURED_CONNECTIONS.equals(action)) {
			try {
				Log.d(TAG, "Entering " + ACTION_REMOVE_CONFIGURED_CONNECTIONS);
				String ssidRegEx = args.getString("ssidRegEx");

				Boolean removed = false;
				String thisSSID = null;
				int profilesRemoved = 0;
				List<WifiConfiguration> configs = wWifiManager.getConfiguredNetworks();
				for (WifiConfiguration config : configs) {
					if (config != null) {
						thisSSID = config.SSID.replace("\"",""); // strip beginning and ending quotes
						if (thisSSID.matches(ssidRegEx)) {
							removed = wWifiManager.removeNetwork (config.networkId); // remove the wifi profile
							if (removed) {
								Log.d(TAG, "\nACTION_REMOVE_CONFIGURED_CONNECTIONS: " + "netId:" + config.networkId + ", ssid:" + thisSSID +  ", success");
								profilesRemoved++;

								//clear process binding made to Ayla device during setup process.
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
									ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
									connManager.bindProcessToNetwork(null);
								}

							} else {
								Log.d(TAG, "\nACTION_REMOVE_CONFIGURED_CONNECTIONS: " + config.toString() + "failed");
							}
						}
					}
				}

				Log.d(TAG, "ACTION_REMOVE_CONFIGURED_CONNECTIONS, profilesRemoved: " + profilesRemoved);
				String jsonSuccess = "{"
						+ "\"removed\":" + "\"" + profilesRemoved + "\"" +
						"}";
				result = new WifiApiResult(WifiApiResult.Status.OK, jsonSuccess);
			} catch (Exception Ex) {
				String errMsg = (Ex.getMessage() == null) ? "general error" : Ex.getMessage();
				Log.d(TAG +" - " + ACTION_REMOVE_CONFIGURED_CONNECTIONS, "Exception: "+ errMsg);
				result = new WifiApiResult(WifiApiResult.Status.ERROR, errMsg);
			}
		}
		
		
		
		
		// Get the list of known networks from flash
		// Args: none
		// returns: JSON Array of networks in the configured connections list
		else if (ACTION_CONFIGURED_CONNECTIONS.equals(action)) {
			try {
				Log.d(TAG, "Entering " + ACTION_CONFIGURED_CONNECTIONS);

				List<WifiConfiguration> configs = wWifiManager.getConfiguredNetworks();
				Boolean firstTime = true;
				String securityType = "";
				String configuredResult = "{\"configured\":[";
				for (WifiConfiguration config : configs) {
					if (config != null) {
						if (!firstTime) {
							configuredResult = configuredResult + ",";
						}
						firstTime = false;
						securityType = DetermineSecurityType(config);

						configuredResult = configuredResult + "{";
						configuredResult = configuredResult + "\"net_id\":" + "\"" + config.networkId + "\"" + ",";
						configuredResult = configuredResult + "\"ssid\":" + convertToQuotedString(Uri.encode(config.SSID)) + ",";
						configuredResult = configuredResult + "\"key_mgmt\":" + "\"" + securityType + "\"" + ",";
						configuredResult = configuredResult + "\"hidden\":" + "\"" + config.hiddenSSID + "\"";
						configuredResult = configuredResult + "}";
						Log.d(TAG, "\nACTION_CONFIGURED_CONNECTIONS: " + config.toString());
					}
				}
				configuredResult = configuredResult + "]}";

				result = new WifiApiResult(WifiApiResult.Status.OK, configuredResult);
			} catch (Exception Ex) {
				Log.d(TAG +" - " + ACTION_CONFIGURED_CONNECTIONS, "Exception: "+ Ex.getMessage());
				result = new WifiApiResult(WifiApiResult.Status.ERROR, Ex.getMessage());
			}
		}




		// Scan for wifi networks in proximity
		// Args: none
		// returns: Array of JSON objects for each network discovered
		else if (ACTION_SCAN_RESULTS.equals(action)) {
			try {
				Log.d(TAG, "Entering " + ACTION_SCAN_RESULTS);

				result = getWiFiState(wWifiManager);
				if (!result.getMessage().equals("WIFI_STATE_ENABLED")) {
					Log.d(TAG, "Leaving " + ACTION_SCAN_RESULTS);
					result = new WifiApiResult(WifiApiResult.Status.WIFI_STATE_ERROR, result.getMessage());
					return result;
				}

				// register to receive broadcast messages in onReceive()
				Log.d(TAG, "registering for SCAN_RESULTS_AVAILABLE_ACTION broadcasts");
				IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
				context.registerReceiver(wReceiver, filter);

				rc = wWifiManager.startScan();  // will call onCreate(), results in onRecieve()

				String scanResults = "";
				if (rc) {
					Log.d(TAG, "success launching startScan()");

					discovering = true;
					while (discovering){}
					//wait();
					Log.d(TAG, "success returning from onReceive()");

					Boolean firstTime = true;
					ScanResult bestSignal = null;
					scanResults = "{\"scan_results\":[";
					for (ScanResult wScanResult : wScanResults) {
						if (bestSignal == null || WifiManager.compareSignalLevel(bestSignal.level, wScanResult.level) < 0) {
							bestSignal = wScanResult;
						}
						if (wScanResult != null) {
							Log.d(TAG, "SCAN_RESULTS: SSID: " + wScanResult.SSID + ", Capabilities: " + wScanResult.capabilities);
							if (!firstTime) {
								scanResults = scanResults + ",";
							}
							firstTime = false;

							scanResults = scanResults + "{";
							scanResults = scanResults + "\"ssid\":" + convertToQuotedString(Uri.encode(wScanResult.SSID)) + ",";
							scanResults = scanResults + "\"key_mgmt\":" + "\"" + ScanCapabilitiesSecurity(wScanResult.capabilities) + "\"" + ",";
							scanResults = scanResults + "\"rssi\":" + "\"" + wScanResult.level + "\"";
							scanResults = scanResults + "}";
						}
					}
					scanResults = scanResults + "]}";
					Log.d(TAG, "SCAN_RESULTS Complete");

					if ( (wScanResults != null) && (bestSignal != null)) {
						String message = String.format("%s networks found. %s is the strongest.",
								wScanResults.size(), bestSignal.SSID);
						Log.d(TAG, "SCAN_RESULTS summary: " + message);
					} else {
						Log.d(TAG, "SCAN_RESULTS summary: no networks found.");
					}
				} else {
					Log.d(TAG, "Failed launching startScan()");
				}

				if (!discovering){ // check all pending
					context.unregisterReceiver(wReceiver); // stop scans
				}

				Log.d(TAG, "Leaving " + ACTION_SCAN_RESULTS);
				result = new WifiApiResult(WifiApiResult.Status.OK, scanResults);

			} catch (Exception Ex) {
				Log.d(TAG + " - " + ACTION_SCAN_RESULTS, "Exception: " + Ex.getMessage());
				result = new WifiApiResult(WifiApiResult.Status.ERROR, Ex.getMessage());
			}
		}



		// Enable or disable the Wifi service
		// Args: "true" == enable, "false" == disable
		// returns: Boolean true or false depending on the operation success or failure
		else if (ACTION_WIFI_ENABLE.equals(action)) {
			try {
				Log.d(TAG, "Entering " + ACTION_WIFI_ENABLE);

				String operStr = args.toString();
				String supMsg, netMsg, returnMsg = ""; // overall success/failure message
				Boolean rcBool = false; // overall success/failure
				SupplicantState rcSupplicantState = SupplicantState.UNINITIALIZED;
				NetworkInfo.DetailedState rcDetailedState = NetworkInfo.DetailedState.DISCONNECTED;

				Boolean operBool;
				if ("false".equals(operStr)) { // Only false disables wifi, else enable wifi
					operBool = false;
					Log.d(TAG, ACTION_WIFI_ENABLE + "Operation: Disable");
				} else {
					operBool = true;
					Log.d(TAG, ACTION_WIFI_ENABLE + "Operation: Enable");
				}

				Boolean enableRC = wWifiManager.setWifiEnabled(operBool); // enable on true, disable on false
				Log.d(TAG, ACTION_WIFI_ENABLE + "setWifiEnabled() rc: " + enableRC);

				if (operBool) { // enable wifi
					// Ensure wifi associates, authenticates, and complets the connection
					Log.d(TAG, ACTION_WIFI_ENABLE + "Determine suplicant state");
					if (enableRC) {
						for (int i = 0; (i <= 10 && SupplicantState.COMPLETED != rcSupplicantState); i++) { // add to config
							rcSupplicantState = ConnectionState();
							Thread.sleep(1000);
						}
					}
					supMsg = "Supplicant State: " + rcSupplicantState;

					if (SupplicantState.COMPLETED == rcSupplicantState) {
						// Ensure network optains IP_ADDR and connects
						Log.d(TAG, ACTION_WIFI_ENABLE + "Determine network state");
						for (int i = 0; (i <= 10 && NetworkInfo.DetailedState.CONNECTED != rcDetailedState); i++) {
							rcDetailedState = NetworkState(context);
							Thread.sleep(1000);
						}
						if (rcDetailedState == NetworkInfo.DetailedState.CONNECTED) { 
							rcBool = true;   // set overall return code
						}
					}
					netMsg = "Network State: " + rcDetailedState;
				} else { // disable wifi
					if (enableRC) {
						rcBool = true; // set overall return code
						rcSupplicantState = SupplicantState.DISCONNECTED;
						rcDetailedState = NetworkInfo.DetailedState.DISCONNECTED;
					} else {
						rcSupplicantState = ConnectionState();  // don't expect this to happen, state unknown
						rcDetailedState = NetworkState(context); // ditto
					}
					supMsg = "Supplicant State: " + rcSupplicantState;
					netMsg = "NetworkState: " + rcDetailedState;
				}

				// Build status msg
				if (rcBool) { // overall return code
					if (operBool){ // enable wifi
						returnMsg = "Wifi enable: SUCCESS";
					} else {
						returnMsg = "Wifi disable: SUCCESS";
					}
				} else { // overall operation failed
					if (operBool){ // enable wifi
						returnMsg = "Wifi enable: FAIL";
					} else {
						returnMsg = "Wifi disable: FAIL";
					}
				}
				returnMsg = returnMsg + ", " + supMsg + ", " + netMsg;
				Log.d(TAG, ACTION_WIFI_ENABLE + returnMsg);

				Log.d(TAG, "Leaving " + ACTION_WIFI_ENABLE);
				result = new WifiApiResult(WifiApiResult.Status.OK, rcBool);

			} catch (Exception Ex) {
				Log.d(TAG + " - " + ACTION_WIFI_ENABLE, "Exception: " + Ex.getMessage());
				result = new WifiApiResult(WifiApiResult.Status.ERROR, Ex.getMessage());
			}
		}


		// Get the top level connection state
		// Args: none
		// return one of: WIFI_STATE_DISABLED, WIFI_STATE_DISABLING, WIFI_STATE_ENABLED, WIFI_STATE_ENABLING, WIFI_STATE_UNKNOWN
		else if (ACTION_WIFI_STATE.equals(action)) {
			try {
				Log.d(TAG, "Entering " + ACTION_WIFI_STATE);

				result = getWiFiState(wWifiManager);
				JSONObject jsonState = new JSONObject();
				jsonState.put("state", result.getMessage());
				if (!result.getMessage().equals("WIFI_STATE_ENABLED")) {
					Log.d(TAG, "Leaving " + ACTION_WIFI_STATE);
					result = new WifiApiResult(WifiApiResult.Status.WIFI_STATE_ERROR, jsonState.toString());
				} else {
					result = new WifiApiResult(WifiApiResult.Status.OK, jsonState.toString());
				}
			} catch (Exception Ex) {
				Log.d(TAG + " - " + ACTION_WIFI_STATE, "Exception: " + Ex.getMessage());
				result = new WifiApiResult(WifiApiResult.Status.ERROR, Ex.getMessage());
			}
		}


		
		

		// Connect to a network
		// Args: netID, SSID, Security Type, Password
		// netID == -1 equals add to configured list, else update existing configured network
		// returns: -1 if failed, else netID of the new/updated network in the configured list
		else if (ACTION_NETWORK_CONNECT.equals(action)) {
			try {
				Log.d(TAG, "Entering " + ACTION_NETWORK_CONNECT);

				Boolean connectedBool = false; // overall return code
				Boolean isUpdate = false;
				String addUpdateMsg, enableMsg, netMsg, dhcpMsg, returnMsg;
				int netID =-1;
				String ssidArg = Uri.decode(args.getString("ssid"));

				int netIdArg = args.getInt("net_id");	// netIdArg == -1 => update existing config, else add
				String securityArg = args.getString("security_type");
				String passwordArg = "";
				if (!securityArg.equals("OPEN")) {
					passwordArg = Uri.decode(args.getString("password"));
				}

				int retriesArg =args.getInt("retries");
				Log.d(TAG, ACTION_NETWORK_CONNECT + " netId: " + netIdArg + " ssid: " + ssidArg + " security: " + securityArg + " password: " + "password" + " retries: " + retriesArg);

				// parms set from args
				WifiConfiguration wc = new WifiConfiguration();
                wc.SSID = convertToQuotedString(ssidArg);
				if (netIdArg != -1) {
					isUpdate = true;
					wc.networkId = netIdArg;	
				} else
//				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
				{
                    // Look for an existing network with the same SSID. We cannot add a network
                    // if one already exists, so we need to find it and consider it an update.
                    List<WifiConfiguration> configuredNetworks =
                            wWifiManager.getConfiguredNetworks();
                    for ( WifiConfiguration config : configuredNetworks ) {
                        if ( TextUtils.equals(wc.SSID, config.SSID) ) {
                            // We found an existing configuration. Use that.
                            wc = config;
							netIdArg = wc.networkId;
                            isUpdate = true;
                            break;
                        }
                    }
                }
				wc.priority = Integer.MAX_VALUE;
				wc.status = WifiConfiguration.Status.ENABLED;
				setupSecurity(wc, securityArg, passwordArg);

				// add/update to the configured list
				if (isUpdate) {
//					netID = wc.networkId;
					netID = netIdArg;
					addUpdateMsg = "\nupdateNetwork() netID: " + netID;
				} else {
					netID = wWifiManager.addNetwork(wc);
					addUpdateMsg = "\naddNetwork() netID: " + netID;
				}
                Log.d(TAG, ACTION_NETWORK_CONNECT + addUpdateMsg );

				// get current DHCP info
				DhcpInfo dhcpInfoCurrent = wWifiManager.getDhcpInfo();

				Boolean enableRC = false;
				Boolean DhcpInfoNew = false;
				DhcpInfo dhcpInfoNew = null;
				NetworkInfo.DetailedState rcDetailedState = NetworkInfo.DetailedState.DISCONNECTED;
				
				// check for already connected to the network v1.32
				String testSsid;
				WifiInfo info = wWifiManager.getConnectionInfo();
				if ( (info.getSSID() != null) && (!info.getSSID().equals(""))) {
					testSsid = convertToQuotedString(info.getSSID());
					if (testSsid.equals(wc.SSID)){
						Log.d(TAG, "ACTION_NETWORK_CONNECT Already connected to: " + testSsid);
						connectedBool = true; // already connected
						rcDetailedState = NetworkInfo.DetailedState.CONNECTED;
						dhcpInfoNew = wWifiManager.getDhcpInfo();
					}
				}		

				if ( (netID != -1) && (connectedBool == false)) { // v1.32
                    enableRC = wWifiManager.enableNetwork(netID, true);

					if (enableRC) {
						// Ensure network obtains IP_ADDR and connects
						Log.d(TAG, ACTION_NETWORK_CONNECT + "Determine network state1");
						for (int i = 0; (i <= retriesArg && NetworkInfo.DetailedState.CONNECTED != rcDetailedState); i++) {
							rcDetailedState = NetworkState(context);
							Thread.sleep(1500);
						}
						if (rcDetailedState == NetworkInfo.DetailedState.CONNECTED) {
							for (int i = 0; (i <= retriesArg && DhcpInfoNew != true); i++) {
								dhcpInfoNew = wWifiManager.getDhcpInfo();
								if (dhcpInfoNew != null) {
									if ( 	
											(dhcpInfoNew.gateway != dhcpInfoCurrent.gateway && dhcpInfoNew.gateway != 0) ||
											(dhcpInfoNew.ipAddress != dhcpInfoCurrent.ipAddress && dhcpInfoNew.ipAddress != 0) || // v3.03
											(dhcpInfoNew.netmask != dhcpInfoCurrent.netmask && dhcpInfoNew.netmask != 0)
										)
									{
										DhcpInfoNew = true;
										connectedBool = true;   // set overall return code
									}
									
									Log.d(TAG, ACTION_NETWORK_CONNECT + " dhcpInfoNew1:" + dhcpInfoNew.toString() );
								}
								Thread.sleep(1000);
							}

							for (int i = 0; (i <= retriesArg && DhcpInfoNew != true); i++) {
								dhcpInfoNew = wWifiManager.getDhcpInfo();
								if (dhcpInfoNew != null) {
									if ( 
											(dhcpInfoNew.gateway != dhcpInfoCurrent.gateway && dhcpInfoNew.gateway != 0) ||
											(dhcpInfoNew.ipAddress != dhcpInfoCurrent.ipAddress && dhcpInfoNew.ipAddress != 0) || // v3.03
											(dhcpInfoNew.netmask != dhcpInfoCurrent.netmask && dhcpInfoNew.netmask != 0)
										)
									{
										DhcpInfoNew = true;
										connectedBool = true;   // set overall return code
									}
									
									Log.d(TAG, ACTION_NETWORK_CONNECT + " dhcpInfoNew: " + dhcpInfoNew.toString() );
								}
								Thread.sleep(1000);
							}
							//}
						} else {
							connectedBool = false;   // failed, try and clean up
							if (ssidArg.matches(AylaNetworks.deviceSsidRegex)) { // if new device
								wWifiManager.removeNetwork(netID); // remove the wifi profile
								wWifiManager.disableNetwork(netID); // disable
							}
						}
					} else {
						Log.d(TAG, ACTION_NETWORK_CONNECT + "enableNetwork failed" );	
					}
				}
				enableMsg = "\nenableNetwork() rc:" + enableRC;
				netMsg = "\nNetwork State:" + rcDetailedState;
				if (dhcpInfoNew != null) {
					dhcpMsg = "\ndhcpInfo:" + dhcpInfoNew.toString();
				} else {
					dhcpMsg = "\ndhcpInfo:null";
				}

				if(connectedBool) {
					returnMsg = "\nWifi Netwok Connect:SUCCESS\n";
				} else {
					if (!DhcpInfoNew) {
						netID = -2;
					} else {
						netID = -1;
					}
					returnMsg = "\nWifi Netwok Connect:FAIL\n";
				}

				/*
				For Android M bindProcessToNetwork() has to be called to ensure that data is sent to
				Ayla device in AP mode
				 */
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					final ConnectivityManager connectivityManager =
							(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
					boolean found = false;
					for (Network n : connectivityManager.getAllNetworks()) {
						NetworkInfo ni = connectivityManager.getNetworkInfo(n);
						String extraInfo = ni.getExtraInfo();
						if(extraInfo != null){
							if (extraInfo.contains(ssidArg)) {
								found = connectivityManager.bindProcessToNetwork(n);
								break;
							}
						}

					}
					if (!found) {
						connectivityManager.bindProcessToNetwork(null);
					}
				}

				returnMsg = returnMsg + addUpdateMsg + enableMsg + netMsg + dhcpMsg;
				Log.d(TAG, ACTION_NETWORK_CONNECT + returnMsg);

				Log.d(TAG, "Leaving " + ACTION_NETWORK_CONNECT);
				JSONObject jsonNetID = new JSONObject();
				jsonNetID.put("net_id", netID);
				result = new WifiApiResult(WifiApiResult.Status.OK, jsonNetID);

			} catch (Exception Ex) {
				Log.d(TAG, " - " + ACTION_NETWORK_CONNECT + "Exception:" + Ex.getMessage());
				result = new WifiApiResult(WifiApiResult.Status.ERROR, Ex.getMessage());
			}
		}



		// Remove an existing network from the configured networks list
		else if (ACTION_REMOVE_NETWORK.equals(action)) {
			try {
				Log.d(TAG, "Entering " + ACTION_REMOVE_NETWORK);

				String returnMsg, argStr;
				Boolean rcBool;
				int netId;

				// get the args
				argStr = args.toString();
				netId = Integer.parseInt(argStr);
				Log.d(TAG, ACTION_REMOVE_NETWORK + " netIdArg: " + netId);

				// parms set from args
				rcBool = wWifiManager.removeNetwork(netId); // remove the wifi profile

				if (rcBool) {
					returnMsg = "ACTION_REMOVE_NETWORK success";
				} else {
					returnMsg = "ACTION_REMOVE_NETWORK failed rc: " + rcBool;
				}
				Log.d(TAG, ACTION_REMOVE_NETWORK + returnMsg);

				Log.d(TAG, "Leaving " + ACTION_REMOVE_NETWORK);
				result = new WifiApiResult(WifiApiResult.Status.OK, rcBool);

			} catch (Exception Ex) {
				Log.d(TAG, " - " + ACTION_REMOVE_NETWORK + "Exception: " + Ex.getMessage());
				result = new WifiApiResult(WifiApiResult.Status.ERROR, Ex.getMessage());
			}
		}
		// Unknown action!
		else {
			result = new WifiApiResult(WifiApiResult.Status.INVALID_ACTION);
			Log.d(TAG, "Invalid action arg: " + action );
		}

		return result;
	}

	//--------------------------------------- End execute() -----------------------------  
	private static WifiApiResult getWiFiState(WifiManager mWifiManager) {
		WifiApiResult result;
		try {
			Log.d(TAG, "Entering getWiFiState");

			int stateRC = mWifiManager.getWifiState();
			Log.d(TAG, "getWifiState() rc " + stateRC);

			String state;
			switch(stateRC) {
			case WifiManager.WIFI_STATE_DISABLING:
				state = "WIFI_STATE_DISABLING";
				break;
			case WifiManager.WIFI_STATE_DISABLED:
				state = "WIFI_STATE_DISABLED";
				break;
			case WifiManager.WIFI_STATE_ENABLING:
				state = "WIFI_STATE_ENABLING";
				break;
			case WifiManager.WIFI_STATE_ENABLED:
				state = "WIFI_STATE_ENABLED";
				break;
			case WifiManager.WIFI_STATE_UNKNOWN:
				state = "WIFI_STATE_UNKNOWN";
				break;
			default:
				state = "UNDEFINED";
			}

			Log.d(TAG, "getWifiState(): " + state);

			Log.d(TAG, "Leaving " + ACTION_WIFI_STATE);
			result = new WifiApiResult(WifiApiResult.Status.OK, state);

		} catch (Exception Ex) {
			Log.d(TAG + " - " + ACTION_WIFI_STATE, "Exception: " + Ex.getMessage());
			result = new WifiApiResult(WifiApiResult.Status.ERROR);
		}
		return result;
	}



	private final BroadcastReceiver wReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "Entering onReceive()");

			final String action = intent.getAction();
			Log.d(TAG, "onReceive() action: " + action);

			if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
				Log.d(TAG, "onReceive() retrieving scan results ");
				wScanResults = wWifiManager.getScanResults();
				discovering=false;
			}
			// else if: AUTHENTICATING, BLOCKED, CONNECTED, CONNECTING, DISCONNECTED, 
			// DISCONNECTING, FAILED, IDLE, OBTAINING_IPADDR, SCANNING, SUSPENDED
			Log.d(TAG, "Leaving onReceive()");
		}
	};

	private final SupplicantState ConnectionState() {
		SupplicantState supState; 
		WifiInfo wifiInfo = wWifiManager.getConnectionInfo();
		supState = wifiInfo.getSupplicantState();
		Log.d(TAG, "ConnectionState() supplicantState: " + supState);

		return supState;
	}

	protected static NetworkInfo.DetailedState NetworkState(Context context) {

		NetworkInfo.DetailedState rcDS = null;

		ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo network = cm.getActiveNetworkInfo();

		// determin wifi or mobile data connection
		State wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();

		if (network == null || wifi != NetworkInfo.State.CONNECTED) { // || wifi != NetworkInfo.State.CONNECTING
			rcDS = NetworkInfo.DetailedState.FAILED ;
		} else {
			rcDS = network.getDetailedState();
		}

		Log.d(TAG, "NetworkState() detailedState: " + rcDS);
		return rcDS;
	}

	void onDestroy() {
		Log.d(TAG, ".onDestroy() called");
		context.unregisterReceiver(wReceiver);
	}

	private static String DetermineSecurityType(WifiConfiguration wifiConfig) {
		Log.d(TAG, "Entering DetermineSecurityType()");

		if (wifiConfig.allowedKeyManagement.get(KeyMgmt.NONE)) {
			// wpa_supplicant uses all group ciphers by default
			// No group ciphers set/required for OPEN mode
			// WEP: WEP40 and WEP104 set/required, so no CCMP and TKIP
			if (!wifiConfig.allowedGroupCiphers.get(GroupCipher.CCMP)
					&& (wifiConfig.allowedGroupCiphers.get(GroupCipher.WEP40)
							|| wifiConfig.allowedGroupCiphers.get(GroupCipher.WEP104))) {
				return WEP;
			} else {
				return OPEN;
			}
		} else if (wifiConfig.allowedProtocols.get(Protocol.RSN)) {
			return WPA2;
		} else if (wifiConfig.allowedKeyManagement.get(KeyMgmt.WPA_EAP)) {
			return WPA_EAP;
		} else if (wifiConfig.allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
			return IEEE8021X;
		} else if (wifiConfig.allowedProtocols.get(Protocol.WPA)) {
			return WPA;
		} else {
			Log.d(TAG, "Unknown security type from WifiConfiguration, falling back on open.");
			return OPEN;
		}
	}

	private static char quoteChar = '"';
	private static String convertToQuotedString(String string) {
		if (TextUtils.isEmpty(string)) {
			return "\"\"";
		}

		final int lastPos = string.length() - 1;
		if ( (lastPos < 0) || ( (string.charAt(0) == quoteChar) && (string.charAt(lastPos) == quoteChar)) ) {
			return string;
		}

		return "\"" + string + "\"";
	}

	// Determine the security type based on network capabilities
	// Args: network capabilities string
	// returns: security type
	static final String[] SECURITY_MODES = { WEP, WPA, WPA2, WPA_EAP, IEEE8021X };
	private static String ScanCapabilitiesSecurity(String cap) {
		for (int i = SECURITY_MODES.length - 1; i >= 0; i--) {
			if (cap.contains(SECURITY_MODES[i])) {
				return SECURITY_MODES[i];
			}
		}

		return OPEN;
	}

	// configure wificonfig object for connecting
	// Args: wifiConfig, security type, password
	// returns: updated wifiConfig
	static private void setupSecurity(WifiConfiguration config, String security, final String password) {
		config.allowedAuthAlgorithms.clear();
		config.allowedGroupCiphers.clear();
		config.allowedKeyManagement.clear();
		config.allowedPairwiseCiphers.clear();
		config.allowedProtocols.clear();

		if (TextUtils.isEmpty(security)) {
			security = OPEN;
			Log.w(TAG, "Empty security, assuming open");
		}

		// OPEN
		if (security.equals(WEP)) {
			int wepPasswordType = WEP_PASSWORD_AUTO;
			// Leave empty password alone

			if (!TextUtils.isEmpty(password)) {
				if (wepPasswordType == WEP_PASSWORD_AUTO) {
					if (isHexWepKey(password)) {
						config.wepKeys[0] = password;
					} else {
						config.wepKeys[0] = convertToQuotedString(password);
					}
				} else {
					config.wepKeys[0] = wepPasswordType == WEP_PASSWORD_ASCII
							? convertToQuotedString(password)
									: password;
				}
			}
			config.wepTxKeyIndex = 0;
			config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
			config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
			config.allowedKeyManagement.set(KeyMgmt.NONE);
			config.allowedGroupCiphers.set(GroupCipher.WEP40);
			config.allowedGroupCiphers.set(GroupCipher.WEP104);

			// WPA or WPA2
		} else if (security.equals(WPA) || security.equals(WPA2)){
			config.allowedGroupCiphers.set(GroupCipher.TKIP);
			config.allowedGroupCiphers.set(GroupCipher.CCMP);
			config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
			config.allowedPairwiseCiphers.set(PairwiseCipher.CCMP);
			config.allowedPairwiseCiphers.set(PairwiseCipher.TKIP);
			config.allowedProtocols.set(security.equals(WPA2) ? Protocol.RSN : Protocol.WPA);

			// Leave empty password alone
			if (!TextUtils.isEmpty(password)) {
				if (password.length() == 64 && isHex(password)) {
					config.preSharedKey = password; // unquoted hex
				} else {
					config.preSharedKey = convertToQuotedString(password); // quoted text
				}
			}

			// OPEN
		} else if (security.equals(OPEN)) {
			config.allowedKeyManagement.set(KeyMgmt.NONE);

			// WPA_EAP
		} else if (security.equals(WPA_EAP) || security.equals(IEEE8021X)) {
			config.allowedGroupCiphers.set(GroupCipher.TKIP);
			config.allowedGroupCiphers.set(GroupCipher.CCMP);
			if (security.equals(WPA_EAP)) {
				config.allowedKeyManagement.set(KeyMgmt.WPA_EAP);
			} else {
				config.allowedKeyManagement.set(KeyMgmt.IEEE8021X);
			}
			if (!TextUtils.isEmpty(password)) {
				config.preSharedKey = convertToQuotedString(password);
			}
		}
	}

	private static Boolean isHexWepKey(String wepKey) {
		final int len = wepKey.length();

		// WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
		if (len != 10 && len != 26 && len != 58) {
			return false;
		}
		return isHex(wepKey);
	}

	private static Boolean isHex(String key) {
		for (int i = key.length() - 1; i >= 0; i--) {
			final char c = key.charAt(i);
			if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')) {
				return false;
			}
		}
		return true;
	}
	
}
