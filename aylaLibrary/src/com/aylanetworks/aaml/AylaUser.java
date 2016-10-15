//
//  AylaUser.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 8/15/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;


import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.webkit.WebView;

import com.google.gson.annotations.Expose;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;


public class AylaUser extends AylaSystemUtils
{

	private final static String tag = AylaUser.class.getSimpleName();


	/* Model class for parsing */
	public static class AylaUserRole {
		@Expose
		public boolean canAddRoleUser;

		@Expose
		public int group;

		@Expose
		public int id;

		@Expose
		public String name;

		@Expose
		public int oemId;
	}

	// Response from OAuth:
	//{"access_token":"XXXXXXXXX","expires_in":3600,
	// "refresh_token":"XXXXXXXXXX","role":{"can_add_role_user":false,"group":null,"id":13,
	// "name":"EndUser","oem_id":2}}

	// Response from regular login:
	// {"access_token":"XXXXXXXXX","refresh_token":"XXXXXXXXXX",
	// "expires_in":86400,"role":"EndUser","role_tags":[]}
	@Expose
	String accessToken;
	@Expose
	String refreshToken;
	@Expose
	int expiresIn;

	AylaUserRole OAuthRole;

	@Expose
	String role;
	
	// Sign-out
	@Expose
	private String logout;
	
	// Sign-up
	@Expose
	public String email; 			// required
	@Expose
	public String password; 		// required
	@Expose
	public String firstname;		// required
	@Expose
	public String lastname; 		// required
	@Expose
	public String country; 			// required
	@Expose
	public String street;
	@Expose
	public String city;
	@Expose
	public String state;
	@Expose
	public String zip;
	@Expose
	public String phoneCountryCode;
	@Expose
	public String phone;
	@Expose
	public String aylaDevKitNum;				// evaluation board (EVB)
	
	@Expose
	public AylaDatum datum;						// recent metadata associated with the registered user
	@Expose
	public AylaDatum datums[];					// all metadata associated with the registered user
	
	@Expose
	public AylaShare share;
	@Expose
	public AylaShare shares[];

	// derived
	@Expose
	protected long accessTokenExpiresAt = 0L;	// calculated seconds to expiration
	@Expose
	String url;									// OAUTH provider URL returned from Ayla User Service
	
	private static final String mailRegex = "^[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}$"; // email char check
	
	public static AylaUser user; // = new AylaUser(); // save current user info v3.20_ENG
	
	// value is set in AylaExecuteRequest.commit()
	protected long updatedAt = 0L; // timestamp in milliSeconds of last user login or token refresh.
	protected String authHeader = null;
	
	// User for Google and Facebook OAUTH
	private static AylaOAuth oAuth = null;	// used for pass through to AylaOAuth
	
	// getters and setters
	public String getAccessToken() {
		return accessToken;
	}
	
	public void setAccessToken(String newAccessToken) {
		accessToken = newAccessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}
	public void setRefreshToken(String newRefreshToken) {
		refreshToken = newRefreshToken;
	}
	
	public int getExpiresIn() {
		return expiresIn;
	}
	public void setExpiresIn(int newExpiresIn) {
		expiresIn = newExpiresIn;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public AylaUserRole getOAuthRole() {
		return OAuthRole;
	}

	public void setOAuthRole(AylaUserRole OAuthRole) {
		this.OAuthRole = OAuthRole;
	}

	public String getLogout() {
		return logout;
	}
	
	public static AylaUser setCurrent(AylaUser newUser) {
		user = newUser;
		user.setauthHeaderValue(newUser.accessToken);
		user.accessTokenExpiresAt = user.updatedAt + (user.expiresIn * 1000); // milliseconds token expires

		return user;
	}
	
	public static AylaUser getCurrent() {
		return user;
	}
	
   	@Override
	public String toString() {
	    return AylaSystemUtils.gson.toJson(this);
	}
   	
	// -------------------------------- Authentication Methods ----------------------------------------
	public String getauthHeaderValue() {
		return user.authHeader;
	}
	
	public void setauthHeaderValue (String authToken) {
		if (authToken == null) {
			authToken = "none";
		}
		
		if (TextUtils.equals(authToken, "none")) {
			user.updatedAt = 0L; // reset timestamp used to calc access token expiry
		} else {
			user.updatedAt = System.currentTimeMillis();
		}
		user.authHeader = "auth_token " + authToken;
	}

	public int accessTokenSecondsToExpiry() {
		int delta = 0;
		int secondsLeft = (int) ((accessTokenExpiresAt - System.currentTimeMillis())*.001);
		if (secondsLeft > 0 ) {
			delta = secondsLeft;
		}

		return delta;
	}
	
	/**
	 * Refresh the access token if it it's time to live is less than the threshold
	 * Uses sharedPreferences "currentUser" for persistence. Called from AylaLanMode.resume().
	 * 
	 * @param thresholdInSeconds default is DEFAULT_ACCESS_TOKEN_REFRESH_THRRESHOLD
	 */
	public static void refreshAccessTokenOnExpiry(int thresholdInSeconds) {
		String jsonUser = AylaSystemUtils.loadSavedSetting("currentUser", "");
		if (AylaReachability.isCloudServiceAvailable()) { 			// service is reachable
			// Best effort attempt to update the access token using the refresh token
			if (!TextUtils.isEmpty(jsonUser)) {
				AylaUser aylaUser = AylaSystemUtils.gson.fromJson(jsonUser, AylaUser.class);
				AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "D", tag, "refreshAccessTokenOnExpiry"
						, "AylaUser:" + aylaUser);
				final String refreshToken = aylaUser.getRefreshToken();
				int secondsToExpiry = aylaUser.accessTokenSecondsToExpiry();
				// secondsToExpiry = 0;										// TESTING ONLY - force new access token
				if (secondsToExpiry < thresholdInSeconds) { 								// compare access token time to live
					if (!TextUtils.isEmpty(refreshToken)) { 
						Thread thread = new Thread(new Runnable() {
							public void run() {
								AylaUser aylaUser;
								AylaRestService rs = AylaUser.refreshAccessToken(refreshToken);
								Message msg = rs.execute();											// get the new access token
								String jsonResults = (String)msg.obj;
								AylaSystemUtils.saveToLog("%s, %s, %s, %s.", "D", tag,
										"refreshAccessTokenOnExpiry", "msg:" + msg.toString());
								if (msg.what == AylaNetworks.AML_ERROR_OK) {

									boolean isOAuth = true;
									String role = null;
									try {
										JSONObject object = new JSONObject(jsonResults);
										role = object.getString("role");
										isOAuth = false;
									} catch (JSONException e) {
										e.printStackTrace();
										isOAuth = true;
									}

									if ( !TextUtils.isEmpty(role) ) {
										try {
											new JSONObject(role);
											isOAuth = true;
										} catch (JSONException e) {
											e.printStackTrace();
											isOAuth = false;
										}
									}

									// TODO: extract this as a util method, as it is reused
									// in app level, in AMAP.sessionManager.
									// public static AylaUser parseUser(String json, boolean auth)
									if (!isOAuth) {
										aylaUser = AylaSystemUtils.gson.fromJson(jsonResults, AylaUser.class);
									} else {
										aylaUser = new AylaUser();
										try {
											JSONObject object = new JSONObject(jsonResults);
											// Inside AylaUser so no need to change setter/getter
											// Change this if moving to app space.
											aylaUser.accessToken = object.getString("access_token");
											aylaUser.refreshToken = object.getString("refresh_token");
											aylaUser.expiresIn = object.getInt("expires_in");

											JSONObject roleObject = object.getJSONObject("role");
											AylaUser.AylaUserRole oAuthRoleObject =
													AylaSystemUtils.gson.fromJson(roleObject
															.toString(), AylaUser.AylaUserRole
															.class);
											aylaUser.OAuthRole = oAuthRoleObject;
											aylaUser.role = oAuthRoleObject.name;
											// TODO: oAuthRoleObject has id/oem_id/group/can_add_role_user, map and init
											// roleTags.
										} catch (Exception e) {
											e.printStackTrace();
											//TODO: Set to "EndUser" if missing, SVC-2195
											aylaUser.role = "EndUser";
										}
									}


									aylaUser = AylaUser.setCurrent(aylaUser);
									
									// persist authentication info
									jsonResults = AylaSystemUtils.gson.toJson(aylaUser, AylaUser.class);
									AylaSystemUtils.saveSetting("currentUser", jsonResults); 		// save new refreshToken to storage
									
									AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "I", "AylaUser", "refreshedAccessTokenStatus", msg.arg1, "refreshAccessTokenOnExpiry");
								} else {
									AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%s, %s", "E", "AylaUser", "rc", msg.arg1, "error", jsonResults, "refreshAccessTokenOnExpiry");
								}
							}
						
						});
						thread.start();
						try {
							thread.join();		// Wait for thread to complete
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} else {
					AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "I", "AylaUser", "AccessTokenSecsToExpiry", secondsToExpiry, "refreshAccessTokenOnExpiry");
				}
			}
		}
	}
	
	/**
	 * Refresh the access token if it it's time to live is less than the threshold.
	 * Does not use sharedPreferences so persistence is left to the caller/application.
	 * 
	 * @param thresholdInSeconds
	 * @return new aylaUser if successful, else null
	 */
	public AylaUser refreshAccessTokenOnExpiry2(int thresholdInSeconds) {
		if (AylaReachability.isCloudServiceAvailable()) { 		// service is reachable
			// Best effort attempt to update the access token using the refresh token
			final String refreshToken = getRefreshToken();
			int secondsToExpiry = accessTokenSecondsToExpiry();
			// secondsToExpiry = 0;														// TESTING ONLY - force new access token
			if (secondsToExpiry < thresholdInSeconds) { 								// compare access token time to live
				Message msg = new Message();
				Thread thread = new Thread(new Runnable() {
					public void run() {
						AylaUser aylaUser= null;;
						AylaRestService rs = AylaUser.refreshAccessToken(refreshToken);
						Message msg = rs.execute();										// get the new access token
						String jsonResults = (String)msg.obj;
						if (msg.what == AylaNetworks.AML_ERROR_OK) {

							// save auth info to current user
							aylaUser = AylaSystemUtils.gson.fromJson(jsonResults, AylaUser.class);
							aylaUser = AylaUser.setCurrent(aylaUser);

							// persist authentication info
							jsonResults = AylaSystemUtils.gson.toJson(aylaUser, AylaUser.class);

							AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "I", "AylaUser", "refreshedAccessTokenStatus", msg.arg1, "refreshAccessTokenOnExpiry2");
						} else {
							AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%s, %s", "E", "AylaUser", "rc", msg.arg1, "error", jsonResults, "refreshAccessTokenOnExpiry2");
						}
					}
				});
				thread.start();
				try {
					thread.join();		// Wait for thread to complete
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (msg.arg1 == 200) {
					return getCurrent();	// return new access & refresh token
				} else {
					AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "E", "AylaUser", "rc", msg.arg1, "refreshAccessTokenOnExpiry2");
					return null;			// leave current user info intact
				}

			} else {
				AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s", "I", "AylaUser", "AccessTokenSecsToExpiry", secondsToExpiry, "refreshAccessTokenOnExpiry2");
			}
		}
		return getCurrent();	// continue to use current access token
	}

	/**
	 * Same as {@link AylaUser#login(Handler, String, String, String, String)} with no handler to return results.
	 * **/
   	public static AylaRestService login(String userName, String password, String appId, String appSecret) {
   		return login(null, userName, password, appId, appSecret);
   	}

    /** Returns the AylaUser saved in a previous call to {@link #setCachedUser} */
    public static AylaUser getCachedUser() {
        String json = AylaSystemUtils.loadSavedSetting(SETTING_SAVED_USER, "");
        if ( TextUtils.isEmpty(json) ) {
            return null;
        }

        return AylaSystemUtils.gson.fromJson(json, AylaUser.class);
    }

    /** Saves an AylaUser to the cache. Can be retreived via {@link #getCachedUser}. */
    public static void setCachedUser(AylaUser user) {
            String json = AylaSystemUtils.gson.toJson(user);
            AylaSystemUtils.saveSetting(SETTING_SAVED_USER, json);
        }

    private static class LoginTask extends AsyncTask<Object, Void, AylaRestService> {

        @Override
        protected AylaRestService doInBackground(Object... params) {
            // Parameters are: username, password, app ID, app secret, handler
            String  userName = (String)params[0];
            String password = (String)params[1];
            String appId = (String)params[2];
            String appSecret = (String)params[3];
            Handler handler = (Handler)params[4];

            // Get the cached user, if present.
            AylaUser cachedUser = AylaUser.getCachedUser();

            // check reachability to the cloud service
            boolean waitForResults = true;

            int serviceReachability = AylaReachability.getConnectivity();				// get current service reachability status

            if (serviceReachability != AylaNetworks.AML_REACHABILITY_REACHABLE) {	// if we don't know we're reachable
                saveToLog("Calling determineReachability()...");
                AylaReachability.determineReachability(waitForResults);			// can take a few seconds to determine
                saveToLog("determineReachability() done!");

                serviceReachability = AylaReachability.getConnectivity();				// get the results
            }

            if ( serviceReachability != AylaNetworks.AML_REACHABILITY_REACHABLE ) {
                // The service is not reachable. If the user is trying to log in
                // with the same username / password as the last successful login, then we will
                // allow the login to continue in LAN mode (cached results only).
                // Otherwise we must consider this a failure.
                if ( cachedUser == null ||
                        !TextUtils.equals(cachedUser.email, userName) ||
                        !TextUtils.equals(cachedUser.password, password)) {
                    // We can't verify that this is the same username / password that was
                    // previously used successfully.
                    AylaRestService rs = new AylaRestService(handler, ERR_URL, AylaRestService.POST_USER_LOGIN);
                    saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", "cloud service not reachable", "true", "login");
                    returnToMainActivity(rs, "Cloud service not reachable", AML_ERROR_UNREACHABLE, 0, false);
                    return rs;
                }

                // The service is not reachable, but the username / password supplied is the same
                // as the username / password we previously saved on a successful login. We can
                // just continue as if we had logged in and return cached data.
                AylaRestService rs = new AylaRestService(handler, ERR_URL, AylaRestService.POST_USER_LOGIN);

                // Return the cached user with a response code of "cached".
                rs.jsonResults = AylaSystemUtils.loadSavedSetting(SETTING_SAVED_USER, "");
                rs.responseCode = AML_ERROR_ASYNC_OK_CACHED;
                returnToMainActivity(rs, rs.jsonResults, AML_ERROR_ASYNC_OK_CACHED, 0, false);
                return rs;
            }

            // Clear existing user info, if any // v3.20_ENG
            AylaUser.user.setauthHeaderValue ("none");
            AylaUser.user.accessTokenExpiresAt = 0;

            String url = String.format("%s%s", userServiceBaseURL(), "users/sign_in.json");
            AylaRestService rs = new AylaRestService(handler, url, AylaRestService.POST_USER_LOGIN);

            //{"user":{"email":"user@aylanetworks.com","password":"password","application":{"app_id":"debwebserver_id","app_secret":"debwebserver_secret"}}}
            String userParam = 		"{\"user\":{";
            userParam = userParam + "\"email\":" + "\"" + userName + "\"" + ",";
            userParam = userParam + "\"password\":" + "\"" + password + "\"" + ",";
            userParam = userParam + "\"application\":{";
            userParam = userParam + "\"app_id\":" + "\"" + appId + "\"" + ",";
            userParam = userParam + "\"app_secret\":" + "\"" + appSecret + "\"";
            userParam = userParam + "}}}";
            rs.setEntity(userParam);
            saveToLog("%s, %s, %s:%s, %s", "I", "User", "url", url, "login");

            return rs;
        }

        @Override
        protected void onPostExecute(AylaRestService aylaRestService) {
            saveToLog("I, User, Login: onPostExecute called");
            if ( aylaRestService != null && aylaRestService.mHandler != null ) {
                aylaRestService.execute();
            }
        }
    }

	/**
	 * Use this function to provide user access to the devices registered with their Ayla account. It handles all user authentication
	 * and credentialing. Therefore, after loadSavedSettings, this is the first method that must be called prior to accessing any Ayla Cloud
	 * Service.
	 *
	 * @param mHandle is where result would be returned.
	 * @param userName is user account email address.
	 * @param password is user account password.
	 * @param appId is Ayla supplied application identity.
	 * @param appSecret is Ayla supplied application secret.
	 * @return AylaRestService object
	 */
	public static AylaRestService login(Handler mHandle, String userName, String password, String appId, String appSecret) {
		final LoginTask task = new LoginTask();


        // Create final copies of our parameters so we don't need to change the interface of this
        // method
        final Handler h = mHandle;
        final String u = userName;
        final String p = password;
        final String a = appId;
        final String s = appSecret;

        if ( mHandle != null ) {
            // Run in the background and return null. Make sure we call execute() on the UI thread.
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    task.execute(u, p, a, s, h);
                }
            });
            return null;
        } else {
            // Run right now synchronously and return the result
            return task.doInBackground(userName, password, appId, appSecret, null);
        }
	}


	private static class SSOLoginTask extends AsyncTask<Object, Void, AylaRestService> {

		@Override
		protected AylaRestService doInBackground(Object... params) {
			// Parameters are: username, password, app ID, app secret, handler
			String  userName = (String)params[0];
			String password = (String)params[1];
			String token = (String)params[2];
			String appId = (String)params[3];
			String appSecret = (String)params[4];
			Handler handler = (Handler)params[5];

			// Get the cached user, if present.
			AylaUser cachedUser = AylaUser.getCachedUser();

			// check reachability to the cloud service
			boolean waitForResults = true;

			int serviceReachability = AylaReachability.getConnectivity();				// get current service reachability status

			if (serviceReachability != AylaNetworks.AML_REACHABILITY_REACHABLE) {	// if we don't know we're reachable
				AylaReachability.determineReachability(waitForResults);			// can take a few seconds to determine
				serviceReachability = AylaReachability.getConnectivity();				// get the results
			}

			if ( serviceReachability != AylaNetworks.AML_REACHABILITY_REACHABLE ) {
				// The service is not reachable. If the user is trying to log in
				// with the same username / password as the last successful login, then we will
				// allow the login to continue in LAN mode (cached results only).
				// Otherwise we must consider this a failure.
				if ( cachedUser == null ||
						!TextUtils.equals(cachedUser.email, userName) ||
						!TextUtils.equals(cachedUser.password, password)) {
					// We can't verify that this is the same username / password that was
					// previously used successfully.
					AylaRestService rs = new AylaRestService(handler, ERR_URL, AylaRestService.POST_USER_LOGIN);
					String errString = "Cloud service not reachable";
					saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", errString, "true", "sso login");
					try {
						JSONObject errors = new JSONObject();
						errors.put("error",errString);
						returnToMainActivity(rs, errors.toString(), AML_ERROR_UNREACHABLE, 0, false);
					}catch(Exception ex){
						returnToMainActivity(rs, errString, AML_ERROR_UNREACHABLE, 0, false);
					}

					return rs;
				}

				// The service is not reachable, but the username / password supplied is the same
				// as the username / password we previously saved on a successful login. We can
				// just continue as if we had logged in and return cached data.
				AylaRestService rs = new AylaRestService(handler, ERR_URL, AylaRestService.POST_USER_LOGIN);

				// Return the cached user with a response code of "cached".
				rs.jsonResults = AylaSystemUtils.loadSavedSetting(SETTING_SAVED_USER, "");
				rs.responseCode = AML_ERROR_ASYNC_OK_CACHED;
				returnToMainActivity(rs, rs.jsonResults, AML_ERROR_ASYNC_OK_CACHED, 0, false);
				return rs;
			}

			// Clear existing user info, if any // v3.20_ENG
			AylaUser.user.setauthHeaderValue ("none");
			AylaUser.user.accessTokenExpiresAt = 0;

			String url = String.format("%s%s", userServiceBaseURL(), "api/v1/token_sign_in.json");
			AylaRestService rs = new AylaRestService(handler, url, AylaRestService.POST_USER_LOGIN);

			//{"user":{"email":"user@aylanetworks.com","password":"password","application":{"app_id":"debwebserver_id","app_secret":"debwebserver_secret"}}}
			String userParam = 		"{";
			userParam = userParam + "\"token\":" + "\"" + token + "\"" + ",";
			userParam = userParam + "\"app_id\":" + "\"" + appId + "\"" + ",";
			userParam = userParam + "\"app_secret\":" + "\"" + appSecret + "\"";
			userParam = userParam + "}";
			rs.setEntity(userParam);
			saveToLog("%s, %s, %s:%s, %s", "I", "User", "url", url, "sso login");
			saveToLog("%s, %s, %s: %s", "I", "User", "userparams", userParam);

			return rs;
		}

		@Override
		protected void onPostExecute(AylaRestService aylaRestService) {
			saveToLog("I, User, Login: onPostExecute called");
			if ( aylaRestService != null && aylaRestService.mHandler != null ) {
				aylaRestService.execute();
			}
		}
	}


	/**
	 * Use this function to login to Ayla service using access token provided by an external identity provider (SSO).
	 * Username and password passed to this method are the username and password used to login to the external identity provider.
	 * These parameters are not required by the Ayla service for SSO login, and are used to check for cached users.
	 *
	 * @param mHandle is where result would be returned.
	 * @param userName is user account email address in the external identity provider service.
	 * @param password is user account password in the external identity provider service.
	 * @param token is the access token provided by external identity provider
	 * @param appId is Ayla supplied application identity.
	 * @param appSecret is Ayla supplied application secret.
	 * @return AylaRestService object
	 */
	public static AylaRestService ssoLogin(Handler mHandle, String userName, String password, String token, String appId, String appSecret) {
		final SSOLoginTask task = new SSOLoginTask();
		final Handler h = mHandle;
		final String u = userName;
		final String p = password;
		final String t = token;
		final String a = appId;
		final String s = appSecret;

		if ( mHandle != null ) {
			// Run in the background and return null. Make sure we call execute() on the UI thread.
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					task.execute(u, p, t, a, s, h);
				}
			});
			return null;
		} else {
			// Run right now synchronously and return the result
			return task.doInBackground(userName, password, token, appId, appSecret, null);
		}
	}


	/**
	 * curl -X POST -d "{"user" => {"refresh_token" => "3ccd999effb335c50775d739ece32ab8"}}"
	 *  -H "Content-Type:application/json" http://staging-user.aylanetworks.com/users/refresh_token.json
	 *  return
	 *  {"authorization": {"access_token":"dafe6fb1c8efab055461afb684934538","refresh_token":"3ccd999effb335c50775d739ece32ab8","expires_in":"300"}}

	 * @param refreshToken
	 * @return AylaRestService object
	 */
   	public static AylaRestService refreshAccessToken(String refreshToken) {
   		return refreshAccessToken(null, refreshToken);
   	}
	public static AylaRestService refreshAccessToken(Handler mHandle, String refreshToken) {
		String url = String.format("%s%s", userServiceBaseURL(), "users/refresh_token.json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.POST_USER_REFRESH_ACCESS_TOKEN);

		//{"user":{"refresh_token":"3ccd999effb335c50775d739ece32ab8"}}
		try {
			JSONObject userValues = new JSONObject().put("refresh_token", refreshToken);
			String userParam = new JSONObject().put("user", userValues).toString();
			
			rs.setEntity(userParam);
			saveToLog("%s, %s, %s:%s, %s", "I", "User", "url", url, "login");
			
			if (mHandle != null) {
				rs.execute();
			}
	    
		} catch (Exception e) {
			rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.POST_USER_REFRESH_ACCESS_TOKEN);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", "exception", e.getCause(), "refreshAccessToken");
			returnToMainActivity(rs, e.getLocalizedMessage(), AML_GENERAL_EXCEPTION, 0, false);
			return rs;
		}
	    
		return rs;
	}

	/**
	 * Same as {@link AylaUser#logout(Handler, Map)} with no handler to return results.
	 * **/
	public static AylaRestService logout(Map<String, String> callParams) {
		return logout(null, callParams);
	}

	/**
	 * This method will log the user off of the Ayla cloud service and remove security credentials preventing subsequent network transactions.
	 * @param mHandle is where result would be returned.
	 * @param callParams must contain required parameter(s) by this method. Please read the mobile library document for details.
	 * @return AylaRestService object
	 */
	public static AylaRestService logout(Handler mHandle, Map<String, String> callParams) {
		String url = String.format("%s%s", userServiceBaseURL(), "users/sign_out.json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.POST_USER_LOGOUT);

		if(callParams != null){
			Object obj = callParams.get("access_token");
			String access_token = (String) obj;

			// '{"user": {"access_token": "e0b062246574a6de2980687857f28240"}}'
			String userParam = 		"{\"user\":{";
			userParam = userParam + "\"access_token\":" + "\"" + access_token + "\"";
			userParam = userParam + "}}";
			rs.setEntity(userParam);
		}

		saveToLog("%s, %s, %s:%s, %s", "I", "User", "url", url, "logout");

		// shuts down LanMode (If there is any) upon user logout, for either secure setup session or normal lan mode session.
		AylaSetup.exitSecureSetupSession();

		if (mHandle != null) {
			rs.execute();
		}

		return rs;
	}

	public static AylaRestService logout(Handler mHandle) {
		return logout(mHandle, null);
	}

  
/*
    "user": {
        "email": "user@myusers.com",
        "password": "myPassword",
        "firstname": "fn",
        "lastname": "ln",
        "country": "a country",
        "zip": "95134",
        "phoneCountryCode": "1"
        "phone": "4089991212",
        "ayla_dev_kit_num": "322",
        "application": {
            "app_id": "devwebserver_id",
            "app_secret": "devwebserver_secret"
        }
    }
 */
	
	/**
	 * This API takes the user"s confirmation token as received in her email to confirm account ownership.
	 * Input:
	 *     Mandatory: confirmation_token
	 * Output:
	 *     HTTP response 201, if confirmation_token is valid and unconfirmed
	 *     HTTP response 422 if user is already confirmed or token is invalid
	 * curl -ki -X PUT -H"Content-Type: application/xml" -H"Authorization: auth_token a662d708d9be4a168430b19fba88ffea" -d"{}"  
	 * https://staging-user.aylanetworks.com/users/confirmation.json?confirmation_token=12345678
	 * 
	 * Response:
	 *     Success: AylaUser object
	 *     Errors: {"errors":{"confirmation_token":["is invalid"]}}
	 */
	public static AylaRestService signUpConfirmation(Map<String, String> callParams) {
		return signUpConfirmation(null, callParams);
	}
	public static AylaRestService signUpConfirmation(Handler mHandle, Map<String, String> callParams) {
		//String url = "http://ads-dev.aylanetworks.com/users/confirmation.json?confirmation_token=abcdefg";
		String url = String.format("%s%s", userServiceBaseURL(), "users/confirmation.json");
		AylaRestService rs = null;

		JSONObject userValues = new JSONObject();
		JSONObject errors = new JSONObject();
    	String paramKey, paramValue;
    	
		try {
    		// test validity of mandatory objects
	    	paramKey = "confirmation_token";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue == null) {
				errors.put(paramKey, "can't be blank");
			} else {
				if (paramValue.length() < 8) {
					errors.put(paramKey, "must be at least 8 characters long");
				}
			}
			userValues.put(paramKey, paramValue);
		
			// return if errors in required fields
			if(errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.PUT_USER_SIGNUP_CONFIRMATION);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", ERR_URL, errors.toString(), "signUpConfirmation");
				returnToMainActivity(rs, errors.toString(), AML_USER_INVALID_PARAMETERS, 0, false);
				return rs;
			}
			
			
			// All good, confirm the user
			rs = new AylaRestService(mHandle, url, AylaRestService.PUT_USER_SIGNUP_CONFIRMATION);
			
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaUser", "url", url, "signUpConfirmation");
			
			String userStr = userValues.toString();
			rs.setEntity(userStr);
			
			if (mHandle != null) {
				rs.execute();
			}
			
			return rs;
			
    	} catch (Exception e) {
    		rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.PUT_USER_SIGNUP_CONFIRMATION);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", "exception", e.getCause(), "signUpConfirmation");
			returnToMainActivity(rs, errors.toString(), AML_GENERAL_EXCEPTION, 0, false);
			return rs;
    	}
	}

	/**
	 * Same as {@link AylaUser#signUp(Handler, Map, String, String)} with no handler to return results.
	 * **/
	public static AylaRestService signUp( Map<String, String> callParams, String appId, String appSecret) {
		return signUp(null, callParams, appId, appSecret);
	}

	/**
	 * Use this method to create a new user account on the Ayla Cloud Service.
	 * @param mHandle is where result would be returned.
	 * @param callParams must contain required parameter(s) by this method. Please read the mobile library document for details.
	 * @param appId is Ayla supplied application identity.
	 * @param appSecret is Ayla supplied application secret.
	 * @return AylaRestService object
	 */
	public static AylaRestService signUp(Handler mHandle, Map<String, String> callParams, String appId, String appSecret) {
	
		// String url = "http://user.aylanetworks.com/users.json";
		String url = String.format("%s%s", userServiceBaseURL(), "users.json");
		AylaRestService rs = null;
		
		JSONObject userValues = new JSONObject();
		JSONObject errors = new JSONObject();
    	String paramKey, paramValue;
    	
    	try {
    		// test validity of mandatory objects
	    	paramKey = "email";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue == null) {
				errors.put(paramKey, "can't be blank");
			} else {
				if (paramValue.matches(mailRegex) == false) {
					errors.put(paramKey, "is not a valid email");
				}
			}
			userValues.put(paramKey, paramValue);
			
			paramKey = "password";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue == null) {
				errors.put(paramKey, "can't be blank");
			} else {
				if (paramValue.length() < 6) {
					errors.put(paramKey, "must be at least 6 characters long");
				}
			}
			userValues.put(paramKey, paramValue);
			
			paramKey = "firstname";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue == null) {
				errors.put(paramKey, "can't be blank");
			} else {
				if (paramValue.length() < 2) {
					errors.put(paramKey, "is to short");
				}
			}
			userValues.put(paramKey, paramValue);
			
			paramKey = "lastname";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue == null) {
				errors.put(paramKey, "can't be blank");
			} else {
				if (paramValue.length() < 2) {
					errors.put(paramKey, "is to short");
				}
			}
			userValues.put(paramKey, paramValue);

	    	paramKey = "country";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue == null) {
				errors.put(paramKey, "can't be blank");
			} else {
				if (paramValue.length() < 2) {
					errors.put(paramKey, "is to short");
				}
			}
			userValues.put(paramKey, paramValue);
			
			// return if errors in required fields
			if(errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.POST_USER_SIGNUP);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", ERR_URL, errors.toString(), "signUp");
				returnToMainActivity(rs, errors.toString(), AML_USER_INVALID_PARAMETERS, 0, false);
				return rs;
			}
			
			// All good, sign up the user
			rs = new AylaRestService(mHandle, url, AylaRestService.POST_USER_SIGNUP);
	    	
	    	// add optional parameters
	    	userValues.put("zip", (String)callParams.get("zip"));
	    	userValues.put("phone_country_code", (String)callParams.get("phone_country_code"));
	    	userValues.put("phone", (String)callParams.get("phone"));
	    	userValues.put("ayla_dev_kit_num", (String)callParams.get("aylaDevKitNum"));
	    	userValues.put("company", (String)callParams.get("company"));
	    	userValues.put("street", (String)callParams.get("street"));
	    	userValues.put("city", (String)callParams.get("city"));
	    	userValues.put("state", (String)callParams.get("state"));

	    	// add application object
			JSONObject appVals = new JSONObject();
	    	appVals.put("app_id", appId);
	    	appVals.put("app_secret", appSecret);
	    	userValues.put("application", appVals);
	    	
	    	
	    	// wrap it all up in a user object
	    	JSONObject user = new JSONObject();
	    	user.put("email_template_id", (String)callParams.get(AML_EMAIL_TEMPLATE_ID));
	    	user.put("email_subject", (String)callParams.get(AML_EMAIL_SUBJECT));
	    	user.put("email_body_html", (String)callParams.get(AML_EMAIL_BODY_HTML));
	    	user.put("user", userValues).toString();
	    	String userStr = user.toString();
			rs.setEntity(userStr);
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaUser", "userValues", "passed", "signUp");
			
			if (mHandle != null) {
				rs.execute();
			}
			
			return rs;
    	} catch (Exception e) {
    		rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.POST_USER_SIGNUP);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", "exception", e.getCause(), "signUp");
			returnToMainActivity(rs, errors.toString(), AML_GENERAL_EXCEPTION, 0, false);
			return rs;
    	}
	}

	/**
	 * Same as {@link AylaUser#getInfo(Handler)} with no handler to return results.
	 * **/
    public static AylaRestService getInfo() {
        return getInfo((Handler)null);
    }

    @Deprecated // callParams no longer used
	public static AylaRestService getInfo(Map<String, String> callParams) {
		return getInfo((Handler)null);
	}

	/**
	 * Use this method to retrieve existing user account information from Ayla Cloud Services. The user must be authenticated, via login, before calling
	 * this method.
	 * @param mHandle is where result would be returned.
	 * @param callParams must contain required parameter(s) by this method. Please read the mobile library document for details.
	 * @return AylaRestService object
	 */
    @Deprecated // callParams no longer used
    public static AylaRestService getInfo(Handler mHandle, Map<String, String> callParams) {
        return getInfo(mHandle);
    }

	/**
	 * Use this method to retrieve existing user account information from Ayla Cloud Services. The user must be authenticated, via login, before calling
	 * this method.
	 * @param mHandle is where result would be returned.
	 * @return AylaRestService object
	 */
    public static AylaRestService getInfo(Handler mHandle) {
		String url = String.format("%s%s", userServiceBaseURL(), "users/get_user_profile.json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.GET_USER_INFO);

		saveToLog("%s, %s, %s:%s, %s", "I", "User", "url", url, "getInfo");
		if (mHandle != null) {
			rs.execute();
		}
		return rs;		
	}

	/**
	 * Same as {@link AylaUser#updateInfo(Handler, Map, String, String)} with no handler to return results.
	 * **/
	public static AylaRestService updateInfo(Map<String, String> callParams, String appId, String appSecret) {
		return updateInfo(null, callParams,  appId, appSecret);
	}

	/**
	 * Use this method to modify existing account information from Ayla Cloud Services. The user must be authenticated, via login, before calling this method.
	 * @param mHandle is where result would be returned.
	 * @param callParams must contain required parameter(s) by this method. Please read the mobile library document for details.
	 * @param appId is Ayla supplied application identity.
	 * @param appSecret is Ayla supplied application secret.
	 * @return AylaRestService object
	 */
	public static AylaRestService updateInfo(Handler mHandle, Map<String, String> callParams, String appId, String appSecret) {
		
		// String url = "http://user.aylanetworks.com/users.json";
		String url = String.format("%s%s", userServiceBaseURL(), "users.json");
		AylaRestService rs = null;
		
		JSONObject userValues = new JSONObject();
		JSONObject errors = new JSONObject();
    	String paramKey, paramValue;
    	
    	try {
    		// test validity of objects
	    	paramKey = "email";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue != null) {
				errors.put(paramKey, "can't be modified"); // use delete() & signUp()
			}
			userValues.put(paramKey, paramValue);
			
			paramKey = "password";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue != null) {
				errors.put(paramKey, "can't be modified"); // use changePassword()
			}
			userValues.put(paramKey, paramValue);
			
			paramKey = "firstname";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue == null) {
				errors.put(paramKey, "can't be blank");
			} else {
				if (paramValue.length() < 2) {
					errors.put(paramKey, "is to short");
				}
			}
			userValues.put(paramKey, paramValue);
			
			paramKey = "lastname";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue == null) {
				errors.put(paramKey, "can't be blank");
			} else {
				if (paramValue.length() < 2) {
					errors.put(paramKey, "is to short");
				}
			}
			userValues.put(paramKey, paramValue);

	    	paramKey = "country";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue == null) {
				errors.put(paramKey, "can't be blank");
			} else {
				if (paramValue.length() < 2) {
					errors.put(paramKey, "is to short");
				}
			}
			userValues.put(paramKey, paramValue);
			
			// return if errors in required fields
			if(errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.PUT_USER_CHANGE_INFO);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", ERR_URL, errors.toString(), "changeInfo");
				returnToMainActivity(rs, errors.toString(), AML_USER_INVALID_PARAMETERS, 0, false);
				return rs;
			}
			
			// All good, sign up the user
			rs = new AylaRestService(mHandle, url, AylaRestService.PUT_USER_CHANGE_INFO);
	    	
	    	// Add all the parameters
	    	userValues.put("zip", (String)callParams.get("zip"));
	    	userValues.put("phone_country_code", (String)callParams.get("phone_country_code"));
	    	userValues.put("phone", (String)callParams.get("phone"));
	    	userValues.put("ayla_dev_kit_num", (String)callParams.get("aylaDevKitNum"));
	    	userValues.put("company", (String)callParams.get("company"));
	    	userValues.put("street", (String)callParams.get("street"));
	    	userValues.put("city", (String)callParams.get("city"));
	    	userValues.put("state", (String)callParams.get("state"));
	    	
	    	// add application object
			JSONObject appVals = new JSONObject();
	    	appVals.put("app_id", appId);
	    	appVals.put("app_secret", appSecret);
	    	userValues.put("application", appVals);
	    	
	    	// wrap it all up in a user object
			String userStr = new JSONObject().put("user", userValues).toString();
			rs.setEntity(userStr);

			saveToLog("%s, %s, %s:%s, %s", "I", "AylaUser", "userValues", "passed", "changeInfo");
			if (mHandle != null) {
				rs.execute();
			}
			
			return rs;
    	} catch (Exception e) {
    		rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.PUT_USER_CHANGE_INFO);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", "exception", e.getCause(), "changeInfo");
			returnToMainActivity(rs, errors.toString(), AML_GENERAL_EXCEPTION, 0, false);
			return rs;
    	}
	}


	/* appId and appSecret are required*/
	@Deprecated 
	public static AylaRestService resendConfirmation(String email, Map<String, String> callParams) {	
		return resendConfirmation(null, email, null, null, callParams);
	}
	/* appId and appSecret are required*/
	@Deprecated
	public static AylaRestService resendConfirmation(Handler mHandle, String email, Map<String, String> callParams) {
		return resendConfirmation(mHandle, email, null, null, callParams);
	}

	/**
	 * Same as {@link AylaUser#resendConfirmation(Handler, String, String, String, Map)} with no handler to return results.
	 * **/
	public static AylaRestService resendConfirmation(String email, String appId, String appSecret, Map<String, String> callParams) {	
		return resendConfirmation(null, email, appId, appSecret, callParams);
	}

	/**
	 * This method will send confirmation token to user email address again.
	 * @param mHandle is where result would be returned.
	 * @param email is user registered mail address. Required
	 * @param appId Ayla provided application identity Required
	 * @param appSecret Ayla provided application secret Required
	 * @param callParams call parameters
	 * @return AylaRestService object
	 */
	public static AylaRestService resendConfirmation(Handler mHandle, String email, String appId, String appSecret, Map<String, String> callParams) {
		// String url = "https://user.aylanetworks.com/user/confiramation.json";
		String url = String.format("%s%s", userServiceBaseURL(), "users/confirmation.json");
		AylaRestService rs = null;
		
		JSONObject user = new JSONObject();
		JSONObject userValues = new JSONObject();
		JSONObject appValues = new JSONObject();
		JSONObject errors = new JSONObject();
		String paramKey = "";
		String paramValue = "";

		try {
			// {\"user\":{\"email\":\"email@yahoo.com\", "application": {"app_id":"device_service_id", "app_secret":"device_service_secret" }}}
			
			// test validity of mandatory objects
			paramKey = "email";
			paramValue = email;
			if (TextUtils.isEmpty(paramValue)) {
				errors.put(paramKey, "is invalid");
			}
			userValues.put(paramKey, paramValue); 					// add user parameter: {"email":"thisUser@bing.com"}
			
			paramKey = "appId";
			paramValue = appId;
			if (TextUtils.isEmpty(paramValue)) {
				// errors.put(paramKey, " is required");	// optional for now v3.31_ENG
				saveToLog("%s, %s, %s:%s, %s", "W", "AylaUser", paramKey, "required parameter is missing", "resendConfirmation");
			} else {
				appValues.put(paramKey,  paramValue);			// add user parameter: "application":{"app_id":"device_service_id"}
			}
			
			paramKey = "appSecret";
			paramValue = appSecret;
			if (TextUtils.isEmpty(paramValue)) {
				// errors.put(paramKey, " is required");	// optional for now v3.31_ENG
				saveToLog("%s, %s, %s:%s, %s", "W", "AylaUser", paramKey, "required parameter is missing", "resendConfirmation");
			} else {
				appValues.put(paramKey,  paramValue);			// add user parameter: "application": {"app_secret":"device_service_secret" }
			}
			
			// return if errors in required fields
			if (errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.POST_USER_RESEND_CONFIRMATION);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", ERR_URL, errors.toString(), "resendConfirmation");
				returnToMainActivity(rs, errors.toString(), AML_USER_INVALID_PARAMETERS, 0, false);
				return rs;
			}
			
			// good call
			
			// optional parameters
			if (callParams != null) {
				// get custom email params
				user.put("email_template_id", (String)callParams.get(AML_EMAIL_TEMPLATE_ID));
		    	user.put("email_subject", (String)callParams.get(AML_EMAIL_SUBJECT));
		    	user.put("email_body_html", (String)callParams.get(AML_EMAIL_BODY_HTML));
			} 
			
			// optional appId & appSecret for now v3.31_ENG
			if (appValues.length() > 0) {	
				user.put("application", appValues);// wrap it in an application object: "application": {"app_id":"device_service_id", "app_secret":"device_service_secret" }
			}
			
			user.put("user", userValues);// wrap it in a user object: {"user":{"email":"thisUser@bing.com"},"email_template_id":"template1"}
			
			String userStr = user.toString(); 
			
			rs = new AylaRestService(mHandle, url, AylaRestService.POST_USER_RESEND_CONFIRMATION);
					
			rs.setEntity(userStr);
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaUser", "user", userStr, "resendConfirmation");

			if (mHandle != null) {
				rs.execute();
			}
			return rs;
			
		} catch (Exception e) {
			rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.POST_USER_RESEND_CONFIRMATION);
			saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", "exception", e.getCause(), "resendConfirmation");
			returnToMainActivity(rs, errors.toString(), AML_GENERAL_EXCEPTION, 0, false);
			
			return rs;
    	}
	}
	

	/* appId and appSecret are required*/
	@Deprecated 
	public static AylaRestService resetPassword(String email, Map<String, String> callParams) {
		return resetPassword(null, email, null, null, callParams);
	}
	/* appId and appSecret are required*/
	@Deprecated  
	public static AylaRestService resetPassword(Handler mHandle, String email, Map<String, String> callParams) {
		return resetPassword(mHandle, email, null, null, callParams);
	}

	/**
	 * Same as {@link AylaUser#resetPassword(Handler, String, String, String, Map)} with no handler to return results.
	 * **/
	public static AylaRestService resetPassword(String email, String appId, String appSecret, Map<String, String> callParams) {
		return resetPassword(null, email, appId, appSecret, callParams);
	}

	/**
	 * This method will delete the users existing password and send a token to their registered email address.
	 * The token and new password can then be used to login.
	 * @param mHandle is where result would be returned.
	 * @param email is user registered mail address.
	 * @param  appId Ayla provided application identity Required
	 * @param  appSecret Ayla provided application secret Required
	 * @param callParams call parameters
	 * @return AylaRestService object
	 */
	public static AylaRestService resetPassword(Handler mHandle, String email, String appId, String appSecret, Map<String, String> callParams) {	
		
		// String url = "https://user.aylanetworks.com/user/password.json";
		String url = String.format("%s%s", userServiceBaseURL(), "users/password.json");
		AylaRestService rs = null;

		JSONObject user = new JSONObject();
		JSONObject userValues = new JSONObject();
		JSONObject appValues = new JSONObject();
		JSONObject errors = new JSONObject();
		String paramKey = "";
		String paramValue = "";

    	try {
			// test validity of mandatory objects
			paramKey = "email";
			paramValue = email;
			if (TextUtils.isEmpty(paramValue)) {
				errors.put(paramKey, "is invalid");
			}
			userValues.put(paramKey, paramValue); 					// add user parameter: {"email":"thisUser@bing.com"}
			
			paramKey = "appId";
			paramValue = appId;
			if (TextUtils.isEmpty(paramValue)) {
				// errors.put(paramKey, " is required");	// optional for now v3.31_ENG
				saveToLog("%s, %s, %s:%s, %s", "W", "AylaUser", paramKey, "required parameter is missing", "resetPassword");
			} else {
				appValues.put(paramKey,  paramValue);			// add user parameter: "application":{"app_id":"device_service_id"}
			}
			
			paramKey = "appSecret";
			paramValue = appSecret;
			if (TextUtils.isEmpty(paramValue)) {
				// errors.put(paramKey, " is required");	// optional for now v3.31_ENG
				saveToLog("%s, %s, %s:%s, %s", "W", "AylaUser", paramKey, "required parameter is missing", "resetPassword");
			} else {
				appValues.put(paramKey,  paramValue);			// add user parameter: "application": {"app_secret":"device_service_secret" }
			}
			
			// return if errors in required fields
			if (errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.POST_USER_RESET_PASSWORD);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", ERR_URL, errors.toString(), "resetPassword");
				returnToMainActivity(rs, errors.toString(), AML_USER_INVALID_PARAMETERS, 0, false);
				return rs;
			}
			// good call
			
			// optional parameters
			if (callParams != null) {
				// get custom email params
				user.put("email_template_id", (String)callParams.get(AML_EMAIL_TEMPLATE_ID));
		    	user.put("email_subject", (String)callParams.get(AML_EMAIL_SUBJECT));
		    	user.put("email_body_html", (String)callParams.get(AML_EMAIL_BODY_HTML));
			} 
			
			// optional appId & appSecret for now v3.31_ENG
			if (appValues.length() > 0) {	
				user.put("application", appValues);// wrap it in an application object: "application": {"app_id":"device_service_id", "app_secret":"device_service_secret" }
			}
			user.put("user", userValues);
			String userStr = user.toString();

			
			rs = new AylaRestService(mHandle, url, AylaRestService.POST_USER_RESET_PASSWORD);
			rs.setEntity(userStr);
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaUser", "email", email, "resetPassword");
			
			if (mHandle != null) {
				rs.execute();
			}
			return rs;
    	} catch (Exception e) {
    		rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.POST_USER_RESET_PASSWORD);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", "exception", e.getCause(), "resetPassword");
			returnToMainActivity(rs, errors.toString(), AML_GENERAL_EXCEPTION, 0, false);
			return rs;
    	}
	}
	
	// Reset Password With Token
	public static AylaRestService resetPasswordWithToken(Handler mHandle, Map<String, String> callParams) {
		// String url = "https://user.aylanetworks.com/users/password.json";
		String url = String.format("%s%s", userServiceBaseURL(), "users/password.json");
		AylaRestService rs = null;
		
		JSONObject userValues = new JSONObject();
		JSONObject errors = new JSONObject();
    	String paramKey, paramValue;
    	
    	try {
    		// test validity of objects
    		paramKey = "reset_password_token";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue == null) {
				errors.put(paramKey, "can't be blank");
			} else {
				if (paramValue.length() < 8) {
					errors.put(paramKey, "must be at least 8 characters long");
				}
			}
			userValues.put(paramKey, paramValue);
			
			paramKey = "password";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue == null) {
				errors.put(paramKey, "can't be blank");
			} else {
				if (paramValue.length() < 6) {
					errors.put(paramKey, "must be at least 6 characters long");
				}
			}
			userValues.put(paramKey, paramValue);
			
			paramKey = "password_confirmation";
			paramValue = (String)callParams.get(paramKey);
			if (paramValue == null) {
				errors.put(paramKey, "can't be blank");
			} else {
				if (paramValue.length() < 6) {
					errors.put(paramKey, "is to short");
				}
			}
			userValues.put(paramKey, paramValue);
			
			// return if errors in required fields
			if(errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.PUT_RESET_PASSWORD_WITH_TOKEN);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", ERR_URL, errors.toString(), "resetPasswordWithToken");
				returnToMainActivity(rs, errors.toString(), AML_USER_INVALID_PARAMETERS, 0, false);
				return rs;
			}
			
			// All good, sign up the user
			rs = new AylaRestService(mHandle, url, AylaRestService.PUT_RESET_PASSWORD_WITH_TOKEN);
	    	
	    	// wrap it all up in a user object
			String userStr = new JSONObject().put("user", userValues).toString();
			rs.setEntity(userStr);

			saveToLog("%s, %s, %s:%s, %s", "I", "AylaUser", "userValues", "passed", "resetPasswordWithToken");
			if (mHandle != null) {
				rs.execute();
			}
			
			return rs;
    	} catch (Exception e) {
    		rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.PUT_RESET_PASSWORD_WITH_TOKEN);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", "exception", e.getCause(), "resetPasswordWithToken");
			returnToMainActivity(rs, errors.toString(), AML_GENERAL_EXCEPTION, 0, false);
			return rs;
    	}
	}

	/**
	 * Same as {@link AylaUser#changePassword(Handler, String, String)} with no handler to return results.
	 * **/
	public static AylaRestService changePassword(String currentPassword, String newPassword) {	
		return changePassword(null, currentPassword, newPassword);
	}

	/**
	 * Use this method to change the user"s password. This method may be called only after the user has successfully completed login.
	 * @param mHandle is where result would be returned.
	 * @param currentPassword is user's current password.
	 * @param newPassword is the new password.
	 * @return AylaRestService object
	 */
	public static AylaRestService changePassword(Handler mHandle, String currentPassword, String newPassword) {	
		// String url = "https://ads-dev.aylanetworks.com/user.json";
		String url = String.format("%s%s", userServiceBaseURL(), "users.json");
		AylaRestService rs = null;
		
		JSONObject userValues = new JSONObject();
		JSONObject errors = new JSONObject();
    	String paramKey, paramValue;
    	
    	try {
    		// at current password
	    	paramKey = "current_password";
	    	paramValue = currentPassword;
			userValues.put(paramKey, paramValue);

			// test validity of the new password
			paramKey = "password";
			paramValue = newPassword;
			if (paramValue == null) {
				errors.put("password", "can't be blank");
			} else {
				if (paramValue.length() < 6) {
					errors.put("password", "must be at least 6 characters long");
				}
			}
			userValues.put(paramKey, paramValue);
			
			// return if errors in required fields
			if (errors.length() != 0) {
				rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.PUT_USER_CHANGE_PASSWORD);
				saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", ERR_URL, errors.toString(), "changePassword");
				returnToMainActivity(rs, errors.toString(), AML_USER_INVALID_PARAMETERS, 0, false);
				return rs;
			}
			
			String userStr = new JSONObject().put("user", userValues).toString(); // {"user":{"current_password":"abcdef", "password":"ghijkl"}}
			
			rs = new AylaRestService(mHandle, url, AylaRestService.PUT_USER_CHANGE_PASSWORD);
			rs.setEntity(userStr);
			saveToLog("%s, %s, %s:%s, %s", "I", "AylaUser", "user", "userStr", "changePassword");
			
			if (mHandle != null) {
				rs.execute();
			}
			return rs;
    	} catch (Exception e) {
    		rs = new AylaRestService(mHandle, ERR_URL, AylaRestService.PUT_USER_CHANGE_PASSWORD);
    		saveToLog("%s, %s, %s:%s, %s", "E", "AylaUser", "exception", e.getCause(), "changePassword");
			returnToMainActivity(rs, errors.toString(), AML_GENERAL_EXCEPTION, 0, false);
			return rs;
    	}
	}

	/**
	 * Same as {@link AylaUser#delete(Handler)} with no handler to return results.
	 * **/
	public static AylaRestService delete() {
		return delete(null);
	}

	/**
	 * Use this method to remove an existing account from Ayla Cloud Services.
	 * @param mHandle is where result would be returned.
	 * @return AylaRestService object
	 */
	public static AylaRestService delete(Handler mHandle) {
		
		String url = String.format("%s%s", userServiceBaseURL(), "users.json");
		AylaRestService rs = new AylaRestService(mHandle, url, AylaRestService.DELETE_USER);
		saveToLog("%s, %s, %s:%s, %s", "I", "User", "url", url, "delete");
		
		if (mHandle != null) {
			rs.execute();
		}	
		return rs;		
	}
	
	// ---------------------- User Datum pass-through methods ------------------------
		
		public AylaRestService createDatum(AylaDatum userDatum) {
			return userDatum.create(this);
		}
		public AylaRestService createDatum(Handler mHandle, AylaDatum userDatum) {
			return userDatum.create(mHandle, this);
		}
		public AylaRestService updateDatum(AylaDatum userDatum) {
			return userDatum.update(this);
		}
		public AylaRestService updateDatum(Handler mHandle, AylaDatum userDatum) {
			return userDatum.update(mHandle, this);
		}
		public AylaRestService getDatumWithKey(String key) {
			return AylaDatum.getWithKey(this, key);
		}
		public AylaRestService getDatumWithKey(Handler mHandle, String key) {
			return AylaDatum.getWithKey(mHandle, this, key);
		}
		public AylaRestService getDatum(Map<String, ArrayList<String>> callParams) {
			return AylaDatum.get(this, callParams);
		}
		public AylaRestService getDatum(Handler mHandle, Map<String, ArrayList<String>> callParams) {
			return AylaDatum.get(mHandle, this, callParams);
		}
		public AylaRestService deleteDatum(AylaDatum userDatum) {
			return userDatum.delete(this);
		}
		public AylaRestService deleteDatum(Handler mHandle, AylaDatum userDatum) {
			return userDatum.delete(mHandle, this);
		}
		
// ----------------------------- OAuth pass-through methods -------------------------------
		public static AylaRestService loginThroughOAuth(Handler mHandle, String accountType, WebView webView, String appId, String appSecret) {
			if (oAuth == null) {
				oAuth = new AylaOAuth();
			}
			return oAuth.loginThroughOAuth(mHandle, accountType, webView, appId, appSecret);
		}

// ---------------------- User Share pass-through methods ------------------------
		// create an owned resource share
		public AylaRestService createShare(AylaShare userShare) {
			return userShare.create(this);
		}
		public AylaRestService createShare(Handler mHandle, AylaShare userShare) {
			return userShare.create(mHandle, this);
		}
		// update an owned resource share
		public AylaRestService updateShare(AylaShare userShare) {
			return userShare.update();
		}
		public AylaRestService updateShare(Handler mHandle, AylaShare userShare) {
			return userShare.update(mHandle);
		}
		// get a owned or received resource share for a given id
		public AylaRestService getShare(String id) {
			return AylaShare.getWithId(null, id);
		}
		public AylaRestService getShare(Handler mHandle, String id) {
			return AylaShare.getWithId(mHandle, id);
		}
		// get owned resource shares for a given class type/resourceName and/or resourceId (dynamic)
		public AylaRestService getShares(Map<String, String> callParams) {
			return AylaShare.get(null, this, callParams);
		}
		public AylaRestService getShares(Handler mHandle, Map<String, String> callParams) {
			return AylaShare.get(mHandle, this, callParams);
		}
		// get owned resource shares for a given class type/resourceName and/or resourceId (static)
		static public AylaRestService getAllShares(Map<String, String> callParams) {
			AylaUser userObj = new AylaUser();
			return AylaShare.get(null, userObj, callParams);
		}
		static public AylaRestService getAllShares(Handler mHandle, Map<String, String> callParams) {
			AylaUser userObj = new AylaUser();
			return AylaShare.get(mHandle, userObj, callParams);
		}
		// delete an owned or received share
		public AylaRestService deleteShare(AylaShare userShare) {
			return userShare.delete(null);
		}
		public AylaRestService deleteShare(Handler mHandle, AylaShare userShare) {
			return userShare.delete(mHandle);
		}
		
		// ---------------------- Received Share pass-through methods ------------------------
		// get all received shares for a given class type/resourceName and/or resourceId
		static public AylaRestService getAllReceivedShares(Map<String, String> callParams) {
			AylaUser userObj = new AylaUser();
			return AylaShare.getReceives(null, userObj, callParams);
		}
		static public AylaRestService getAllReceivedShares(Handler mHandle, Map<String, String> callParams) {
			AylaUser userObj = new AylaUser();
			return AylaShare.getReceives(mHandle, userObj, callParams);
		}

	//TODO: Move to a common utils class.
	private static void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId, Boolean fromDevice) {
		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		
		rs.execute(); // return in main activity

		return;
	}
}




