/*
 * AylaBlob.java
 * Ayla Mobile Library
 * 
 * Created by Daniel Myers on 02/27/2014
 * Copyright (c) 2014 Ayla Networks. All rights reserved.
 * */

package com.aylanetworks.aaml;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.util.Locale;
import java.util.Map;


class AylaBlobContainer {
	@Expose
	public AylaBlob datapoint = null;
}


@SuppressLint("HandlerLeak")
public class AylaBlob extends AylaDatapoint {

	private static final String tag = AylaBlob.class.getSimpleName();

	public static final String kAylaBlobEcho = "echo";
	public static final String kAylaBlobClosed = "closed";
	public static final String kAylaBlobFile = "file";
	
	/* Declare a local dir where stream files will locate. 
	 * By default, it would be root of primary external storage.*/
	public static final String kAylaBlobFileLocalPath = "stream_dir";
	
	/* Declare the suffix name of the stream file. 
	 * Format would be Blob_${suffix_name}, Blob_Stream by default.*/
	public static final String kAylaBlobFileSuffixName = "stream_file_suffix";
	
	public static final String kAylaBlobFetched = "fetched";
	public static final String kAylaBlobAtomic = "atomic";
	
	@Expose
	protected String file;
	
	@Expose
	protected boolean echo;
	
	@Expose 
	protected boolean closed;
	
	// getBlob/createblob, getBlobSaveToFile/createBlobPostToFile, markFinished all in one, by default true
	private boolean mIsAtomic; 
	
	// Need to do markFetched automatically at the end, by default false.
	private boolean mMarkAsFetched;  
	private AylaProperty mOwner;
	private Map<String, String> mParams;
	private Handler mFinishHandler;
	
	/**
	 * Atomic operation means getBlob/createblob, getBlobSaveToFile/createBlobPostToFile, markFinished \n
	 * all done in one API call. By default true.
	 * @return true if the API is atomic.
	 * */
	public boolean isAtomic() {
		return mIsAtomic;
	}
	
	public boolean mIsFetched() {
		return mMarkAsFetched;
	}
	
	/**
	 * @return the file URL where the blob file will be fetched.
	 * */
	public String file() {
		return file;
	}
	
	/**
	 * Initialize the file URL.
	 * @param f URL we wish to download the blob file.
	 * */
	public void file(final String f) {
		file = f;
	}
	
	public boolean echo() {
		return echo;
	}
	
	public void echo(final boolean e) {
		echo = e;
	}
	
	public boolean closed() {
		return closed;
	}
	
	public void closed(final boolean c) {
		closed = c;
	}
	// ----------------------------- Blob Support -------------------------------

	private void init(Handler mHandler, AylaProperty property, Map<String, String> params) {
		mIsAtomic = true;
		mMarkAsFetched = false;
		mOwner = property;
		
		if (params != null) {
			if (params.get(kAylaBlobAtomic)!=null) {
				mIsAtomic = TextUtils.equals("true", params.get(kAylaBlobAtomic)) ?true:false;
			}
			if (params.get(kAylaBlobFetched)!=null) {
				mMarkAsFetched = TextUtils.equals("true", params.get(kAylaBlobFetched)) ?true:false;
			}
		}
		if (mIsAtomic) {
			mFinishHandler = mHandler;
		}
		if (mIsAtomic) {
			mParams = params;
		}
	}
	
	
	/**
	 * Same as {@link AylaBlob#createBlob(Handler, AylaProperty, Map)} with no option to setup the call to execute on an external event.
	 *   
	 * @param mHandle is intent handler where the final results are returned
	 * @param property is the stream baseType property to retrieve
	 * @param params is the call parameters
	 * @return AylaRestService object
	 */
	public AylaRestService createBlob(Handler mHandle, AylaProperty property, Map<String, String> params) {
		return createBlob(mHandle, property, params, false);
	}

	/**
	 * createBlob is a compound method that pushes a stream baseType also known as a long property from the cloud service.
	 * The entire process to push a BLOB:
	 * Call the compound method createBlob with Blob meta-data in params
	 * Get the URL of the destination and Blob bytes using createDatapoint(), return in kAylaBlobCreateBlobHandler
	 * Pass the BLOB URL and Bytes to createBlobPostToFile which post the BLOB.
	 * Mark the stream/long property as finished on the service via markFinished().
	 *
	 * @param mHandle is intent handler where the final results are returned
	 * @param property is the stream baseType property to retrieve
	 * @param params is the call parameters
	 * @param delayExecution could be set to true if you want to setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService createBlob(Handler mHandle, AylaProperty property, Map<String, String> params, boolean delayExecution) {
		Number propKey = property.getKey().intValue();
		AylaRestService rs = null;
		String jsonBlob = "";
		this.convertValueToType(property);
		
		init(mHandle, property, params);
		if (mIsAtomic) {
			mHandle = kAylaBlobCreateBlobHandler;
		}
		
		if (AylaReachability.isCloudServiceAvailable()) {
			final JsonObject blobObject = new JsonObject();
			blobObject.addProperty(kAylaBlobEcho, this.echo);
			blobObject.addProperty(kAylaBlobClosed, this.closed);
			blobObject.addProperty(kAylaDataPointValue, this.value);
			blobObject.addProperty(kAylaBlobFile, this.file);
			
			jsonBlob = jsonBlob + "{\"datapoint\":" + blobObject.toString() + "}";
			String url = String.format(Locale.getDefault(), "%s%s%d%s", deviceServiceBaseURL(), "properties/", propKey, "/datapoints.json");
			rs = new AylaRestService(mHandle, url, AylaRestService.CREATE_DATAPOINT_BLOB);
			rs.setEntity(jsonBlob);
			saveToLog("%s, %s, %s:%s, %s%s, %s", "I", tag, "url", url, "JsonBlob", jsonBlob,
					"createBlob");
			if (!delayExecution) {
				rs.execute();
			}
		} else {
			saveToLog("%s, %s, %s:%s, %s", "E", tag, "Reachability", "Cloud is somehow not " +
					"reachable.", "createBlob");
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.CREATE_DATAPOINT_BLOB);
			jsonBlob = AylaSystemUtils.gson.toJson(property.datapoint, AylaBlob.class); // return original value
			returnToMainActivity(rs, jsonBlob, AML_ERROR_UNREACHABLE, 0);
		}
		return rs;
	}
	
	private final Handler kAylaBlobCreateBlobHandler = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			String jsonResults = (String)msg.obj;
			saveToLog("%s, %s, %s, %s, %s.", "D", tag, "kAylaBlobCreateBlobHandler"
					, "msg.arg1:"+msg.arg1, "msg.obj:" + msg.obj);
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
				AylaBlob blob = AylaSystemUtils.gson.fromJson(jsonResults, AylaBlob.class);   
				AylaBlob.this.value(blob.value());
				AylaBlob.this.file(blob.file());
				saveToLog("%s, %s, %s.", "I", tag, "createBlob success");
				if (mIsAtomic) {
					AylaBlob.this.createBlobPostToFile(kAylaBlobCreateBlobPostToFileHandler, mOwner, mParams);
				}
			} else {
				saveToLog("%s, %s, %s.", "E", tag, "createBlob failed.");
			}
		}// end of handleMessage 
	};
	
	private AylaRestService createBlobPostToFile(Handler mHandle, AylaProperty property, Map<String, String> callParams) {
		return createBlobPostToFile(mHandle, property, callParams, false);
	}
	private AylaRestService createBlobPostToFile(Handler mHandle, AylaProperty property, Map<String, String> params, boolean delayExecution) {
		AylaRestService rs = null;
		String url = this.file;
		if (params != null && params.get(kAylaBlobFile)!=null) {
			url = params.get(kAylaBlobFile);
		} 
		
		if (!kAylaBlobCreateBlobPostToFileHandler.equals(mHandle)) {
			mFinishHandler = mHandle;
			mHandle = kAylaBlobCreateBlobPostToFileHandler;
		}
		
		if (TextUtils.isEmpty(url)) { // file url is not setup properly
			saveToLog("%s, %s, %s:%s, %s", "E", tag, "InvalidParam", "FileURL not setup properly",
					"createBlobPostToFile");
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.CREATE_DATAPOINT_BLOB_POST_TO_FILE);
			returnToMainActivity(rs, null, AML_USER_INVALID_PARAMETERS, AML_CREATE_DATAPOINT_BLOBS_FILES);
			return rs;
		}
		
		byte[] streamBytes = AylaBlob.getBlobFromFile(params);
		if ( streamBytes == null ) { // note that "" means empty file.
			saveToLog("%s, %s, %s:%s, %s", "E", tag, "InvalidParam", "Local stream file not setup" +
					" " +
					"properly.", "createBlobPostToFile");
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.CREATE_DATAPOINT_BLOB_POST_TO_FILE);
			returnToMainActivity(rs, null, AML_USER_INVALID_PARAMETERS, AML_CREATE_DATAPOINT_BLOBS_FILES);
			return rs;
		}
		
		if (AylaReachability.isCloudServiceAvailable()) { // Assume cloud and S3 share the same reachability.
			rs = new AylaRestService(mHandle, url, AylaRestService.CREATE_DATAPOINT_BLOB_POST_TO_FILE);
			saveToLog("%s, %s, %s:%s, %s.", "I", tag, "url", url, "createBlobPostToFile");
			rs.setEntity(streamBytes);
			if (!delayExecution) {
				rs.execute();
			}
		} else {
			saveToLog("%s, %s, %s:%s, %s", "E", tag, "Reachability", "Cloud is somehow not " +
					"reachable", "createBlobPostToFile");
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.CREATE_DATAPOINT_BLOB_POST_TO_FILE);
			returnToMainActivity(rs, null, AML_ERROR_UNREACHABLE, 0);
		}
		return rs;
	}
	
	private final Handler kAylaBlobCreateBlobPostToFileHandler = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
				
				saveToLog("%s, %s, %s.", "I", tag, "createBlobPostToFile success, will call " +
						"markFinished()");
				AylaBlob.this.markFinished(mFinishHandler, null, false);
			} else if (msg.what == AylaNetworks.AML_ERROR_TOKEN_EXPIRE) {
				saveToLog("%s, %s, %s.", "W", tag, "createBlobPostToFile failed due to signature " +
						"expires, reqeust S3 signature again.");
				AylaBlob.this.createBlob(kAylaBlobCreateBlobHandler, mOwner, mParams);
			} else {
				saveToLog("%s, %s, %s.", "E", tag, "createBlobPostToFile failed");
				// Get back to app.
				AylaRestService rs = new AylaRestService(mFinishHandler, AylaSystemUtils.ERR_URL
						, AylaRestService.CREATE_DATAPOINT_BLOB_POST_TO_FILE);
				returnToMainActivity(rs, (String) msg.obj, msg.arg1, AylaRestService
						.CREATE_DATAPOINT_BLOB_POST_TO_FILE);
			}
		}// end of handleMessage     
	};
	

	/**
	 * Same as {@link AylaBlob#getBlobs(Handler, AylaProperty, Map)} with no option to setup this call to execute on an external event.
	 */
	public AylaRestService getBlobs(Handler mHandle, AylaProperty property, Map<String, String> params) {
		return getBlobs(mHandle, property, params, false);
	}

	/**
	 *  getBlobs is a compound method that retrieves a stream baseType also known as a long property from the cloud service.
	 *  The entire process to retrieve a BLOB:
	 *  Call the compound method getBlobs with callParams of count:1 (TBD count > 1)
	 *  Get the URL of the stream using getDatapoints() with a count of 1, return in getBlobURLsHandler
	 *  Pass the BLOB URL to getBlobSaveToFile() which downloads the BLOB to <default ext dir>/AylaBlob (TBD named dir & fn)
	 *  Mark the stream/long property as fetched on the service via markFetched()
	 * @param mHandle is intent handler where the final results are returned
	 * @param property is the stream baseType property to retrieve
	 * @param params is the call parameters.
	 * @param delayExecution could be set to true if you want to setup this call but have it execute on an external event.
	 * @return AylaRestService object
	 */
	public AylaRestService getBlobs(Handler mHandle, AylaProperty property, Map<String, String> params, boolean delayExecution) {
		Number propKey = property.getKey().intValue();
		AylaRestService rs = null;
		
		init(mHandle, property, params);
		if (mIsAtomic) {
			mHandle = kGetBlobHandler;
		}
		// must be a stream property
		if (!(TextUtils.equals("stream", property.baseType) || TextUtils.equals("file", property.baseType))) {
			saveToLog("%s, %s, %s:%s, %s", "E", tag, "baseType", " not \"stream\" or \"file\".",
					"getBlobs");
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.GET_DATAPOINT_BLOB);
			returnToMainActivity(rs, null, AML_USER_INVALID_PARAMETERS, AML_GET_DATAPOINT_BLOBS);
			return rs;
		}
		
		int count = 1; 
		if (params != null && params.get(kAylaDataPointCount)!=null) {
			count = Integer.parseInt(params.get(kAylaDataPointCount));
		}
		
		if (count > 1) { 
			saveToLog("%s, %s, %s:%s, %s", "E", tag, "baseType", "Blob array not supported for " +
					"now" +
					".", "getBlobs");
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.GET_DATAPOINT_BLOB);
			returnToMainActivity(rs, null, AML_USER_INVALID_PARAMETERS, AML_GET_DATAPOINT_BLOBS);
			return rs;
		}
		
		if (AylaReachability.isCloudServiceAvailable()) { // Assume S3 and cloud shares the same reachability.
			String url = String.format(Locale.getDefault(), "%s%s%d%s", deviceServiceBaseURL(), "properties/", propKey, "/datapoints.json");     
			rs = new AylaRestService(mHandle, url, AylaRestService.GET_DATAPOINT_BLOB);
			saveToLog("%s, %s, %s:%s, %s.", "I", "Datapoints", "url", url, "getBlobs");
			if (!delayExecution) {
				rs.execute();
			}
		} else {
			saveToLog("%s, %s, %s:%s, %s", "E", tag, "Reachablility", "Cloud is somehow not " +
					"reachable.", "getBlobs");
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.GET_DATAPOINT_BLOB);
			returnToMainActivity(rs, null, AML_ERROR_UNREACHABLE, 0);
			return rs;
		}
		return rs;
	}

	
	private final Handler kGetBlobHandler = new Handler(AylaNetworks.appContext.getMainLooper()){
		public void handleMessage(Message msg) {
			String jsonResults = (String)msg.obj;
			saveToLog("%s, %s, %s, %s, %s.", "D", tag, "GetBlob"
					, "msg.arg1:" + msg.arg1
					, "msg.obj:" + msg.obj);
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
				AylaBlob[] blobs = AylaSystemUtils.gson.fromJson(jsonResults, AylaBlob[].class);
				if (blobs == null) {
					saveToLog("%s, %s, %s.", "E", tag, "");
					return;
				}
				
				AylaBlob.this.value(blobs[0].value());
				AylaBlob.this.file(blobs[0].file());
				saveToLog("%s, %s, %s.", "I", tag, "GetBlobHandler passes");
		    	if (mIsAtomic) {
		    		AylaBlob.this.getBlobSaveToFile(kAylaBlobGetBlobSaveToFileHandler, null, false);
		    	}
			} else {
				saveToLog("%s, %s, %s", "E", tag, "GetBlob fails.");
				return;
			}
		}// end of handleMessage 
	};
	

	/**
	 * Same as {@link AylaBlob#getBlobSaveToFile(Handler, Map, Boolean)} with no option to setup the call to execute later
	 */
	private AylaRestService getBlobSaveToFile(Handler mHandle, Map<String, String> params) {
		return getBlobSaveToFile(mHandle, params, false);
	}

	/**
	 * Retrieves blob/stream property from service
	 * Saves it to a file in AylaExecuteRequest.saveBlobToFile()
	 *
	 * @param mHandle   - return results to intent handler
	 * @param params is the call parameters
	 * @param delayExecution - execute now or later
	 * @return AylaResteService object
	 */
	private AylaRestService getBlobSaveToFile(Handler mHandle, Map<String, String> params, Boolean delayExecution) {
		AylaRestService rs = null;
		String url = this.file;
		
		if (params != null && params.get(kAylaBlobFile)!=null) {
			url = params.get(kAylaBlobFile);
		} 
		
		if (!mMarkAsFetched) {
			mHandle = mFinishHandler;
		}
		
		if (TextUtils.isEmpty(url)) { // file url is not setup properly
			saveToLog("%s, %s, %s:%s, %s", "E", tag, "InvalidParam", "FileURL not setup properly.",
					"getBlobSaveToFile");
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.GET_DATAPOINT_BLOB_SAVE_TO_FILE);
			returnToMainActivity(rs, null, AML_USER_INVALID_PARAMETERS, AML_GET_DATAPOINT_BLOBS_FILES);
			return rs;
		}
		
		if (AylaReachability.isCloudServiceAvailable()) { // Assume cloud and S3 share the same reachability.
			rs = new AylaRestService(mHandle, url, AylaRestService.GET_DATAPOINT_BLOB_SAVE_TO_FILE);
			saveToLog("%s, %s, %s:%s, %s.", "I", tag, "url", url, "getBlobSaveToFile");
			if (!delayExecution) {
				rs.execute();
			}
		} else {
			saveToLog("%s, %s, %s:%s, %s", "E", tag, "Reachability", "Cloud is somehow not " +
					"reachable", "getBlobSaveToFile");
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, AylaRestService.GET_DATAPOINT_BLOB_SAVE_TO_FILE);
			returnToMainActivity(rs, null, AML_ERROR_UNREACHABLE, AML_GET_DATAPOINT_BLOBS_FILES);
		}
		return rs;
	}

	
	private final Handler kAylaBlobGetBlobSaveToFileHandler = new Handler(AylaNetworks.appContext.getMainLooper()) {
		public void handleMessage(Message msg) {
			saveToLog("%s, %s, %s, %s, %s.", "D", tag
					, "kAylaBlobGetBlobSaveToFileHandler", "msg.arg1:" + msg.arg1
					, "msg.obj:" + msg.obj);
			if (msg.what == AylaNetworks.AML_ERROR_OK) {
				saveToLog("%s, %s, %s", "I", tag, "GetBlobSaveToFile passes, will " +
						(mMarkAsFetched?"":"not") + " call markFetched()");
				if (mMarkAsFetched) {
					AylaBlob.this.markFetched(mFinishHandler, null, false);
				}         
			} else {
				saveToLog("%s, %s, %s", "E", tag, "GetBlobSaveToFile fails.");
			}
		}// end of handleMessage
	};
	
	
	/**
	 *   Mark a stream datapoint as fetched on the device service.
	 *   Mark as fetched to retrieve the next stream value.
	 *   
	 * @param mHandle   - return results to intent handler.
	 * @param params - call parameters.
	 * @param delayExecution	- execute now or later.
	 * @return AylaRestService object
	 */
	public AylaRestService markFetched(Handler mHandle, Map<String, String> params, boolean delayExecution) {

		return mark(mHandle, params, delayExecution, true);
	}
	
	/**
	 * Mark a stream datapoint operation is finished on the device service.
	 * Mark as finished to retrieve the same stream value next time.
	 * 
	 * @param mHandle	- return results to intent handler.
	 * @param params	- call parameters.
	 * @param delayExecution	- execute now or later.
	 * @return AylaRestService object
	 * */
	private AylaRestService markFinished(Handler mHandle, Map<String, String> params, boolean delayExecution) {
		
		return mark(mHandle, params, delayExecution, false);
	}
	
	private AylaRestService mark(Handler mHandle, Map<String, String> params, boolean delayExecution, boolean isFetched) {
		AylaRestService rs = null;
		String url = this.value;
		int requestID = AylaRestService.BLOB_MARK_FINISHED;
		int subTaskId = AML_MARK_DATAPOINT_BLOB_FINISHED;
		
		if (isFetched) {
			requestID = AylaRestService.BLOB_MARK_FETCHED;
			subTaskId = AML_MARK_DATAPOINT_BLOB_FETCHED;
		}
		
		if (params != null && params.get(kAylaDataPointValue)!=null) {
			url = params.get(kAylaDataPointValue);
		} 
		
		if (TextUtils.isEmpty(url)) { // file url is not setup properly
			saveToLog("%s, %s, %s:%s, %s", "E", tag, "InvalidParam", "URL not setup properly",
					"markFinished");
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, requestID);
			returnToMainActivity(rs, null, AML_USER_INVALID_PARAMETERS, subTaskId);
			return rs;
		}
		
		if (AylaReachability.isCloudServiceAvailable()) { // Assume cloud and S3 share the same reachability.
			rs = new AylaRestService(mHandle, url, requestID);
			saveToLog("%s, %s, %s:%s, %s.", "I", tag, "url", url, "markFinished");
			if (isFetched) {
				//rs.addParam(kAylaBlobFetched, "true");

				JsonObject entityVal = new JsonObject();
				entityVal.addProperty(kAylaBlobFetched, "true");
				rs.setEntity(entityVal.toString());
			}
			
			if (!delayExecution) {
				rs.execute();
			}
		} else {
			saveToLog("%s, %s, %s:%s, %s", "E", tag, "Reachability", "Cloud is somehow not " +
					"reachable", "markFinished");
			rs = new AylaRestService(mHandle, AylaSystemUtils.ERR_URL, requestID);
			returnToMainActivity(rs, null, AML_ERROR_UNREACHABLE, subTaskId);
		}
		return rs;
	}// end of markFinish   
	
	
	protected static String stripContainer(String jsonBlobContainer, int requestId) throws Exception {
		/* Interact with S3, initialResponse will return empty response.*/    
		if (TextUtils.isEmpty(jsonBlobContainer)) {
			return "";
		}
		String jsonBlobs = null;
		try {
			if (requestId == AylaRestService.GET_DATAPOINT_BLOB) { // array
				AylaBlobContainer[] blobContainers = AylaSystemUtils.gson.fromJson(jsonBlobContainer, AylaBlobContainer[].class);
				int count = 0;
				if (blobContainers != null) {
					AylaBlob[] blobs = new AylaBlob[blobContainers.length];     
					for (AylaBlobContainer blobContainer : blobContainers) {
						blobs[count++] = blobContainer.datapoint;
					}
					jsonBlobs = AylaSystemUtils.gson.toJson(blobs, AylaBlob[].class);
				}
				AylaSystemUtils.saveToLog("%s %s %s:%d %s", "I", tag, "count", count,
						"stripContainer");
			} else { // requestId == GET_DATAPOINT_BLOB, single datapoint
				AylaBlobContainer blobContainer = AylaSystemUtils.gson.fromJson(jsonBlobContainer, AylaBlobContainer.class);
				AylaBlob blob = blobContainer.datapoint;
				jsonBlobs = AylaSystemUtils.gson.toJson(blob, AylaBlob.class); 
				AylaSystemUtils.saveToLog("%s %s %s:%s %s", "I", tag, "value", blob.value,
						"stripContainer");
			}
		} catch (Exception e) {
			AylaSystemUtils.saveToLog("%s %s %s:%s %s", "E", tag, "jsonBlobContainer",
					jsonBlobContainer, "stripContainer");
			e.printStackTrace();
			throw e;
		}
		return jsonBlobs;
	}
	
	/**
	 *  Return status and results/errors to the handler passed in getBlobs()
	 */   //TODO: Move to a common utils class. 
	public static void returnToMainActivity(AylaRestService rs, String thisJsonResults, int thisResponseCode, int thisSubTaskId) {
		rs.jsonResults = thisJsonResults;
		rs.responseCode = thisResponseCode;
		rs.subTaskFailed = thisSubTaskId;
		
		rs.execute();
	}	
	
	
	/**
	 * saveBlobToFile saves a blob object in byte stream in a local file.
	 * 
	 * @param params includes file local path by AylaBlob.kAylaBlobFileLocalPath, file name by AylaBlob.kAylaBlobFileSuffixName
	 * @param entity is the blob stream in HttpEntity.
	 * @return json array indicating the destination path/file_name on successful commit, "Error saving file." otherwise.
	 * */  //TODO: need to move this to 
	public static String saveBlobToFile(Map<String, String> params, HttpEntity entity) {
		
		StringBuilder jsonResult = new StringBuilder();
		try {
			byte[] blob = EntityUtils.toByteArray(entity);
			String path = "";
			String suffix = "Stream";
			if (params != null) {
				if (params.get(kAylaBlobFileLocalPath)!=null) {
					path = File.separator + params.get(kAylaBlobFileLocalPath);
				}
				
				if (params.get(kAylaBlobFileSuffixName)!=null) {
					suffix = params.get(kAylaBlobFileSuffixName);
				}
			}
			String fileName = "Blob_" + suffix;
			// TODO: Use Environment.getExternalStorageState() to verify.
			path = Environment.getExternalStorageDirectory().toString() + path;
			boolean isIOSuccess = AylaSystemUtils.writeToFile(blob, path, fileName);
			if (isIOSuccess) {
				jsonResult.append("[{\"file_name\":\"")
					.append(path)
					.append(File.separator)
					.append(fileName)
					.append("\"}]");
				AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", tag, "File Name", jsonResult,
						"AylaBlob.saveBlobToFile");
			} else {
				jsonResult.append("Error saving file.");
				AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "E", tag, "File Name", jsonResult,
						"AylaBlob.saveBlobToFile");
			}
		} catch (Exception e) {
			// TODO: What if not enough memory.
			String eMsg = (e.getLocalizedMessage() == null)? e.toString():e.getLocalizedMessage();
			AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%s", "E", tag, "Error", AylaNetworks
					.AML_GENERAL_EXCEPTION, "eMsg", eMsg, "AylaBlob.saveBlobToFile");
			return eMsg;
		}
		return jsonResult.toString();
	}
	
	/**
	 * Get blob object in string from local file.
	 * 
	 * @param params includes file local path by AylaBlob.kAylaBlobFileLocalPath, file name by AylaBlob.kAylaBlobFileSuffixName
	 * @return file content, which is the blob object in string.
	 * */
	public static byte[] getBlobFromFile(Map<String, String> params) {
		
		byte[] result = null;
		try {
			String path = "";
			String suffix = "Stream";
			if (params != null) {
				if (params.get(kAylaBlobFileLocalPath)!=null) {
					path = File.separator + params.get(kAylaBlobFileLocalPath);
				}
				
				if (params.get(kAylaBlobFileSuffixName)!=null) {
					suffix = params.get(kAylaBlobFileSuffixName);
				}
			}
			String fileName = "Blob_" + suffix;
			// TODO: Use Environment.getExternalStorageState() to verify.
			path = Environment.getExternalStorageDirectory().toString() + path;
			result = AylaSystemUtils.readFromFile(path +File.separator + fileName);
		} catch (Exception e) {
			String eMsg = (e.getLocalizedMessage() == null)? e.toString():e.getLocalizedMessage();
			AylaSystemUtils.saveToLog("%s, %s, %s:%d, %s:%s", "E", tag, "Error", AylaNetworks
					.AML_GENERAL_EXCEPTION, "eMsg", eMsg, "AylaBlob.getBlobFromFile");
			result = null;
		}
		return result;
	}
}// end of AylaBlob class              




