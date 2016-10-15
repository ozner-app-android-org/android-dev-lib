/*
 * AylaencryptionHelper.java
 * Ayla Mobile Library
 * 
 * Created by Di Wang on 05/20/2015
 * Copyright (c) 2015 Ayla Networks. All Rights Reserved.
 * */

package com.aylanetworks.aaml;

import java.math.BigInteger;
import java.security.SecureRandom;

import android.util.Base64;
import android.util.Log;

import com.aylanetworks.aaml.spongycastle.ASN1EncodableVector;
import com.aylanetworks.aaml.spongycastle.ASN1Integer;
import com.aylanetworks.aaml.spongycastle.ASN1Sequence;
import com.aylanetworks.aaml.spongycastle.AsymmetricBlockCipher;
import com.aylanetworks.aaml.spongycastle.AsymmetricCipherKeyPair;
import com.aylanetworks.aaml.spongycastle.AsymmetricKeyParameter;
import com.aylanetworks.aaml.spongycastle.DERSequence;
import com.aylanetworks.aaml.spongycastle.PKCS1Encoding;
import com.aylanetworks.aaml.spongycastle.RSAEngine;
import com.aylanetworks.aaml.spongycastle.RSAKeyGenerationParameters;
import com.aylanetworks.aaml.spongycastle.RSAKeyPairGenerator;
import com.aylanetworks.aaml.spongycastle.RSAKeyParameters;



import com.aylanetworks.aaml.enums.IAML_SECURITY_KEY_SIZE;


/**
 * All cryptography related stuff 
 * */    
class AylaEncryptionHelper {
	
	private static final String tag = AylaEncryptionHelper.class.getSimpleName();
	
	// Better to be singleton, one instance maps to one mKeyPair
	private static AylaEncryptionHelper mInstance = null;
	
	public static synchronized AylaEncryptionHelper getInstance() {
		if (mInstance == null) {
			mInstance = new AylaEncryptionHelper();
		}
		return mInstance;
	}// end of getInstance.
	
	
	private AylaEncryptionHelper() {
		mKeyChoice = 1024;// default
		generateKeyPair();
	}
	
	/* A printing utility. */
//	public static void printHexByteLogs(final byte[] src) {
//		if (src == null || src.length == 0) {
//			return;
//		}
//		
//		StringBuilder sb = new StringBuilder();
//		for (int i=0; i<src.length; i++) {
//			sb.append(Integer.toHexString(0x0100 + (src[i] & 0x00ff)).substring(1));
//		}
//		Log.d(tag, "Byte[]:" + sb.toString());
//	}// end of printHexByteLogs           
	
	/*
	 * For Base64 flag, the value could be Base64.DEFAULT, Base64.NO_CLOSE
	 * , Base64.NO_PADDING, Base64.NO_WRAP, Base64.URL_SAFE
	 * */
	static String encode(byte[] src) {
		try {
			return Base64.encodeToString(src, Base64.NO_WRAP);        
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}// end of encode   
	static byte[] decode(byte[] src) {
		try {
			return Base64.decode(src, Base64.NO_WRAP);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}// end of decode          
	
  
	private AsymmetricCipherKeyPair mPair = null;
	private int mKeyChoice = 1024; // By Default 1024
	
	private BigInteger mPublicMod = null;
	private BigInteger mPublicExp = null;
	
	private BigInteger mPrivateMod = null;
	private BigInteger mPrivateExp = null;
	
	public void init(final IAML_SECURITY_KEY_SIZE keySize) {
		int oldVal = mKeyChoice;                   
		mKeyChoice = keySize.getValue();
		Log.d(tag, "key size:" + mKeyChoice);
		if (mPair == null || oldVal != mKeyChoice) {
			generateKeyPair();
		}
	}// end of init
	
	
	byte[] getPublicKeyPKCS1V21Encoded() {
		if (mPair == null) {
			generateKeyPair();
		}
		try {
			
			ASN1EncodableVector v = new ASN1EncodableVector();
			v.add(new ASN1Integer(mPublicMod));    
			v.add(new ASN1Integer(mPublicExp));
			
			ASN1Sequence sequence = new DERSequence(v);
			return sequence.getEncoded();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}// get PKCS1 V21 encoded bytes.
	
	
	
	byte[] getPrivateKeyPKCS1V21Encoded() {
		if (mPair == null) {
			generateKeyPair();
		}
		try {
			
			ASN1EncodableVector  v = new ASN1EncodableVector();
			v.add(new ASN1Integer(mPrivateMod));
			v.add(new ASN1Integer(mPrivateExp));
			ASN1Sequence sequence = new DERSequence(v);
			
			return sequence.getEncoded();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}// end of getPrivateKeyPKCS1V21encoded
	
	
	
	public void refreshKeyPair() {
		generateKeyPair();
	}// end of refreshKeyPair                
	
	
	
	private synchronized void generateKeyPair() {
		try {
			RSAKeyPairGenerator generator = new RSAKeyPairGenerator();   
			
			generator.init(new RSAKeyGenerationParameters(
					new BigInteger("65537") // public exponent.
					, SecureRandom.getInstance("SHA1PRNG")
					, mKeyChoice
					, 5) // The bigger this param, the better odds for successful key generation, the slower the process.
			);
			
			mPair = generator.generateKeyPair();
			
			initRSAKeyParam();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}// end of generateKeyPair  
	
	
	private void initRSAKeyParam() throws Exception 
	{
		if ( mPair == null ) {
			generateKeyPair();
		}
		
		AsymmetricKeyParameter key1 = mPair.getPublic();
		if  (!(key1 instanceof RSAKeyParameters)) {
			throw new Exception("Public key is not RSA.");
		}
		RSAKeyParameters rsaPublicKey = (RSAKeyParameters)key1;
		AsymmetricKeyParameter key2 = mPair.getPrivate();
		if (!(key2 instanceof RSAKeyParameters)) {
			throw new Exception("Private key is not RSA.");
		}
		RSAKeyParameters rsaPrivateKey = (RSAKeyParameters)key2;
		
		mPublicMod = rsaPublicKey.getModulus();
		mPublicExp = rsaPublicKey.getExponent();
		
		mPrivateMod = rsaPrivateKey.getModulus();
		mPrivateExp = rsaPrivateKey.getExponent();
	}// end of initRSAKeyParam
	
	
	byte[] encrypt(final byte[] src) {
		if (src == null || src.length ==0) {
			return null;
		}
		
		if ( mPair == null) {
			generateKeyPair();
		}
		
		try {
			AsymmetricBlockCipher e = new RSAEngine();        
			e = new PKCS1Encoding(e);
			e.init(true, mPair.getPublic());
			return e.processBlock(src, 0, src.length);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}// end of encrypt            

	
	byte[] decrypt(final byte[] src) {
		if (src == null || src.length ==0) {
			return null;
		}
		if ( mPair == null ) {
			generateKeyPair();
		}
		
		try {
			AsymmetricBlockCipher e = new RSAEngine();
			e = new PKCS1Encoding(e);
			e.init(false, mPair.getPrivate());
			return e.processBlock(src, 0, src.length);
		} catch ( Exception e ) {
			e.printStackTrace();
			return null;
		}
	}// end of decrypt                
	
}// end of AylaSecurity class             











