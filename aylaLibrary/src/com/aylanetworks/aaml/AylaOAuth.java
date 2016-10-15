//
//  AylaOAuth.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 3/23/14.
//  Copyright (c) 2014 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import java.lang.ref.WeakReference;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * 
 * This class is used to authenticate login credentials via a 3rd party OAuth provider
 *
 */
public class AylaOAuth extends AylaSystemUtils {

	 private static final String TAG = "OAUTH";
	
	// constants for OAuth
	public final static String aylaOAuthAccountTypeGoogle = "google_provider";
	public final static String aylaOAuthAccountTypeFacebook = "facebook_provider";
	private final static String aylaOAuthRedirectUriRemote = "http%3A%2F%2Fmobile.aylanetworks.com%2F";
	private final static String aylaOAuthRedirectUriLocal = "http%3A%2F%2Flocalhost:9000%2F";
	private final static String aylaOAuthCodeParser = "code=";

	// values saved for use in compound oauth login call
	private static AylaRestService saveRS = null;
	private static String saveAccountType = null;
	private static String saveAppId = null;
	private static WebView saveWebView = null;

	private WebViewClient oAuthWebViewClient;	// used to monitor WebView session with OAUTH provider
	
	/**
	 * Compound call to login the user via an authenticated OAUTH provider
	 *   1. Retrieve the Oauth Provider URL used for authentication from Ayla User Service
	 *   2. Retrieve Oauth provider authCode via WebView after user enters credentials
	 *   3. Pass authCode to Ayla User Service for login account validation
	 *   4. Return validated AylaUser credentials back to the application
	 *   
	 *   This method may only be called in asynchronous mode
	 * 
	 * @param mHandle		final callback handler to return the validated AylaUser credentials
	 * @param accountType	OAuth provider. Either aylaOAuthAccountTypeGoogle or aylaOAuthAccountTypeFacebook
	 * @param webView		WebView instance pass in from calling app activity
	 * @param appId			Ayla developer app id from calling app activity
	 * @param appSecret		Ayla developer app secret from calling app activity
	 * @return				AylaRestService instance for this compound method call
	 */
	public AylaRestService loginThroughOAuth(Handler mHandle, String accountType, WebView webView, String appId, String appSecret) {
		// check for required parameters
		if (accountType == null || webView == null || appId == null || appSecret == null) {
			saveToLog("%s, %s, %s:%s, %s", "E", "AylaOauth", "invalid user parameters", "true", "loginThroughOauth");
			AylaRestService rs = new AylaRestService(mHandle, "AylaOauth.loginThroughOauth", AylaRestService.LOGIN_THROUGH_OAUTH);
			returnToMainActivity(rs, "Invalid user parameters", AML_USER_INVALID_PARAMETERS, AML_POST_OAUTH_LOGIN);
			return rs;
		}
		
		// check reachability to the cloud service
		boolean waitForResults = true;
		int serviceReachability = AylaReachability.getConnectivity();				// get current service reachability status
		if (serviceReachability == AML_REACHABILITY_UNKNOWN) {						// if reachabiltiy is has not been determined
			AylaReachability.determineReachability(waitForResults);					// can take a few seconds to determine			
			serviceReachability = AylaReachability.getConnectivity();				// get the results
			if ( serviceReachability != AylaNetworks.AML_REACHABILITY_REACHABLE ) {	// if the service is not reachable
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaOauth", "cloud service is not reachable", "true", "loginThroughOauth");
				AylaRestService rs = new AylaRestService(mHandle, "AylaOauth.loginThroughOauth", AylaRestService.LOGIN_THROUGH_OAUTH);
				returnToMainActivity(rs, "Cloud service is not reachable", AML_ERROR_UNREACHABLE, AML_POST_OAUTH_LOGIN);												// reset re-entrancy flag
				return rs;															// no cloud service connectivity
			}
		}
		
		// good params & user service is reachable

		// Clear existing user info, if any v3.20_ENG
		AylaUser.user.setauthHeaderValue ("none");
		AylaUser.user.accessTokenExpiresAt = 0;
		
		// save values for later method calls
		saveRS = new AylaRestService(mHandle, "AylaOauth.loginThroughOauth", AylaRestService.LOGIN_THROUGH_OAUTH);
		saveAppId = appId;
		saveAccountType = accountType;
		saveWebView = webView;
		
		// Get the oauth provider authentication URL
		retrieveOauthUrl(accountType, appId, appSecret);
		
		return saveRS;
	}
	
	/**
	 * 1. Retrieve the Oauth Provider URL used for authentication from Ayla User Service
	 * 
	 * @param accountType	OAuth provider. Either aylaOAuthAccountTypeGoogle or aylaOAuthAccountTypeFacebook
	 * @param appId			Ayla developer app id from calling app activity
	 * @param appSecret		Ayla developer app secret from calling app activity
	 */
	private void retrieveOauthUrl(String accountType, String appId, String appSecret) {
		
		AylaUser.user.setauthHeaderValue ("none");
		AylaUser.user.accessTokenExpiresAt = 0;
		
		String url = String.format("%s%s", userServiceBaseURL(), "users/sign_in.json");
		AylaRestService rs = new AylaRestService(oAuthProviderUrl, url, AylaRestService.POST_USER_OAUTH_LOGIN);

		// {"user":{'auth_method':'google_provider', "application":{"app_id":"debwebserver_id","app_secret":"debwebserver_secret"}}}
		String userParam = 		"{\"user\":{";
		userParam = userParam + "\"auth_method\":" + "\"" + accountType + "\"" + ",";
		userParam = userParam + "\"application\":{";
		userParam = userParam + "\"app_id\":" + "\"" + appId + "\"" + ",";
		userParam = userParam + "\"app_secret\":" + "\"" + appSecret + "\"";
		userParam = userParam + "}}}";
		rs.setEntity(userParam);
		saveToLog("%s, %s, %s:%s, %s", "I", "AylaOauth", "url", url, "retrieveOauthUrl");
		
		rs.execute();	// return results to oAuthProviderUrl handler
	}
	
	private static class OAuthHandler extends Handler
	{
		private WeakReference<AylaOAuth> aylaOAuthRef;
		
		public OAuthHandler(AylaOAuth aylaOAuth)
		{
			super();
			aylaOAuthRef = new WeakReference<AylaOAuth>(aylaOAuth);
		}
		
		@SuppressLint("SetJavaScriptEnabled")		// required by OAUTH providers
		public void handleMessage(Message msg) {
			String jsonResults = (String)msg.obj;
			
			AylaOAuth aylaOAuth = aylaOAuthRef.get();
			
			if (aylaOAuth != null) {
				if (msg.what == AylaNetworks.AML_ERROR_OK) {
					AylaUser aUser =  AylaSystemUtils.gson.fromJson(jsonResults,AylaUser.class);
					AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaOauth", "url", aUser.url, "oAuthProvierUrl_handler");
					
					// create auth provider url with redirect
					String redirectRemoteUrlStr = aUser.url.contains("google") ? aylaOAuthRedirectUriLocal : aylaOAuthRedirectUriRemote;
					String toWebViewURL = String.format("%s&redirect_uri=%s", aUser.url, redirectRemoteUrlStr);
	      
					// setup a WebView session so user can authenticate with OAUTH provider
					// monitor user/oauth provider exchange in oAuthWebViewClient.shouldOverrideUrlLoading()
					saveWebView.getSettings().setJavaScriptEnabled(true);
					aylaOAuth.oAuthWebViewClient = new OAuthWebViewClient(aylaOAuth);
					saveWebView.setWebViewClient(aylaOAuth.oAuthWebViewClient);
					
					saveWebView.loadUrl(toWebViewURL);	// display web page to user
				} else {
					// handle error
					saveToLog("%s, %s, %s:%s, %s", "E", "AylaOauth", "exception", jsonResults, "oAuthProviderUrl_handler");
					aylaOAuth.returnToMainActivity(saveRS, jsonResults, msg.arg1, AML_POST_OAUTH_PROVIDER_URL);
				}
			}
		}		
	}
	
	/**
	 *   2. Retrieve Oauth provider authCode via WebView after user enters credentials
	 *   
	 *      Use the OAUTH provider URL in a WebView session
	 */
	private final Handler oAuthProviderUrl = new OAuthHandler(this);	
	
	/**
	 * 	 2. Retrieve Oauth provider authCode via WebView after user enters credentials
	 *      Monitor WebView exchange between user and 3rd Party OAUTH provider
	 *      On success, pass authentication code to authenticateToService()
	 *
	 */
	private static class OAuthWebViewClient extends WebViewClient {
		
		private WeakReference<AylaOAuth> aylaOAuthRef;
		private String authCode = null;		// authorization credentials from the 3rd party OAUTH provider
		
		public OAuthWebViewClient(AylaOAuth aylaOAuth) {
			super();
			aylaOAuthRef = new WeakReference<AylaOAuth>(aylaOAuth);
		}
		
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "amca.OAuth", "url", url, "shouldOverrideUrlLoading");
	    	
	    	String host = Uri.parse(url).getHost();
	    	 if ( !(host.equals("localhost") || host.equals("mobile.aylanetworks.com")) ) {
	             Log.i(TAG, "display auth provider sign-in page");	// display the page via webview
	            return false;
	        }
	    	 
	        Log.i(TAG, "get the authorization code");
	        int aylaOAuthCodeParserBeginNdx = url.indexOf(AylaOAuth.aylaOAuthCodeParser) + AylaOAuth.aylaOAuthCodeParser.length();
		    if (aylaOAuthCodeParserBeginNdx != -1) {
		    	if (host.equals("localhost")) {
		    		authCode = url.substring(aylaOAuthCodeParserBeginNdx);
		    	} else {
		    		int aylaOAuthCodeParserEndNdx = url.indexOf('&', aylaOAuthCodeParserBeginNdx);
				    authCode = url.substring(aylaOAuthCodeParserBeginNdx, aylaOAuthCodeParserEndNdx);
		    	}
		    }
		    
		    AylaOAuth aylaOAuth = aylaOAuthRef.get();
		    if (aylaOAuth != null) {
		    	aylaOAuth.authenticateToService(authCode);	// Continue oauth login process
		    }
	       return true;
	    }
	}
	
	
	/**
	 *   3. Pass authCode to Ayla User Service for login account validation
	 *   
	 * @param authCode		Authorization code from 3rd Party OAUTH provider, returned from WebView session
	 * @return				AylaRestService instance
	 */
	private AylaRestService authenticateToService(String authCode) {
    	JSONObject params = new JSONObject();
 
    	try {
    		// prepare call parms in json
    		params.put("code", authCode);
    		params.put("app_id", saveAppId);
			params.put("provider", saveAccountType);
			params.put("redirect_url", saveAccountType.equals(aylaOAuthAccountTypeGoogle) ? aylaOAuthRedirectUriLocal : aylaOAuthRedirectUriRemote);
			String paramsStr = params.toString();
	    	
	    	// create auth provider url with redirect
	    	String url = String.format("%s%s", userServiceBaseURL(), "users/provider_auth.json");
			AylaRestService rs = new AylaRestService(oAuthAuthenticateToService, url, AylaRestService.POST_USER_OAUTH_AUTHENTICATE_TO_SERVICE);
			
	    	saveToLog("%s, %s, %s:%s, %s", "I", "AylaOAuth", "params", "OK", "authenticateToServer");
			rs.setEntity(paramsStr);
			rs.execute();
			
			return rs;
    	} catch (Exception e) {
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaOAuth", "exception", e.getCause(), "authenticateToServer");
			returnToMainActivity(saveRS, e.getLocalizedMessage(), AML_GENERAL_EXCEPTION, AML_POST_OAUTH_AUTHENTICATE_TO_SERVICE);
			return saveRS;
    	}
	}
	
	/**
	 *   4. Return validated AylaUser credentials back to the application
	 */
	
	private static class OAuthAuthenticateToServiceHandler extends Handler {
		private WeakReference<AylaOAuth> aylaOAuthRef;
		
		public OAuthAuthenticateToServiceHandler(AylaOAuth aylaOAuth) {
			super();
			aylaOAuthRef = new WeakReference<AylaOAuth>(aylaOAuth);
		}
		
		public void handleMessage(Message msg) {
			String jsonResults = (String)msg.obj;
			
			AylaOAuth aylaOAuth = aylaOAuthRef.get();
			
			if (aylaOAuth != null) {
				if (msg.what == AylaNetworks.AML_ERROR_OK) {
					// {"access_token":"dafe6fb1c8efab055461afb684934538","refresh_token":"cdee360047bfa872e8c1281fd9cc7326","expires_in":1800}
					try {
						// Success - return to app
						aylaOAuth.returnToMainActivity(saveRS, jsonResults, msg.arg1, 0);
					} catch (Exception e) {
						// Failed - handle error
						saveToLog("%s, %s, %s:%s, %s", "E", "Exception", e.getLocalizedMessage(), "null", "oAuthAuthenticateToService_handler");
						aylaOAuth.returnToMainActivity(saveRS, jsonResults, msg.arg1, AML_POST_OAUTH_AUTHENTICATE_TO_SERVICE);
					}
				} else {
					// Failed - handle error
					saveToLog("%s, %s, %s:%s, %s", "E", "AylaOauth", "authCode", "null", "oAuthAuthenticateToService_handler");
					aylaOAuth.returnToMainActivity(saveRS, jsonResults, msg.arg1, AML_POST_OAUTH_AUTHENTICATE_TO_SERVICE);
				}
			}
		}
	}
	
	private final Handler oAuthAuthenticateToService = new OAuthAuthenticateToServiceHandler(this);
	
	/**
	 * Return all results back to caller
	 * 
	 * @param rs					AylaRestService instance
	 * @param thisJsonResults		JSON string
	 * @param thisResponseCode		return code
	 * @param thisSubTaskId			sub task id of a failed compound method
	 */ // TODO: put it in a common utils class
	private void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId) {
		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		
		rs.execute();
	}

}
