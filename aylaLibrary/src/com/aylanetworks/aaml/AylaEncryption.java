//
//  AylaEncryption.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 11/25/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;


class AylaEncryption {
    public static String keyAylaEncryptionType = "type";
    public static String keyAylaEncryptionData = "data";

    public static String valueAylaEncryptionTypeWifiSetupRSA = "wifi_setup_rsa";

    public int version;
    public int proto_1 = -1;
    public int key_id_1 = -1;
    public String sRnd_1;
    public String sRnd_2;
    public Number nTime_1 = -1;
    public Number nTime_2 = -1L;
    public String sTime_1 = null;
    public String sTime_2 = null;

    public String createdAt = null;
    public String isValid = null;
    public int sessionId = -1;

    public String sLanKey;
    public byte[] bLanKey = null;

    public String sec = null;

    public byte[] lastByte = new byte[1];
    public byte[] appSignKey = null;
    public byte[] appCryptoKey = null;
    public byte[] appIvSeed = null;
    public byte[] devSignKey = null;
    public byte[] devCryptoKey = null;
    public byte[] devIvSeed = null;

    public Cipher eCipher = null;
    public Cipher dCipher = null;
    public java.security.Key eSkey = null;
    public java.security.Key dSkey = null;

    private static int nextSessionID = 1;

    private WeakReference<AylaDevice>_device;
    public AylaEncryption(AylaDevice device) {
        _device = new WeakReference<AylaDevice>(device);
    }

//    private AylaEncryption(){}

    public int generateSessionKeys(Map<String, String> callParams) {
        // (NSDictionary *)param sRnd1:(NSString*)_sRnd1 nTime1:(NSNumber *)_nTime1 sRnd2:(NSString*)_sRnd2 nTime2:(NSNumber*)_nTime2;
        //int generateSessionKeys(, String sRnd_1, Number nTime_1, String sRnd_2, Number nTime_2) {
        byte[] bRnd_1 = null;
        byte[] bRnd_2 = null;
        byte[] bTime_1 = null;
        byte[] bTime_2 = null;
        //String sTestKey = "key", sTestData = "The quick brown fox jumps over the lazy dog";


        AylaSystemUtils.saveToLog("%s, %s, %s, %s", "I", "AylaEncryption", "entry", "generateSessionKeys");
        createdAt = AylaSystemUtils.gmtFmt.format(new Date());
        sessionId = AylaEncryption.nextSessionID++;

        AylaDevice device = _device.get();
        if ( device == null ) {
            Log.e("BSK", "No device");
            return AylaNetworks.AML_ERROR_FAIL;
        }

        try {
            bRnd_1 = sRnd_1.getBytes("UTF-8");
            bRnd_2 = sRnd_2.getBytes("UTF-8");

            if (callParams != null) {
                String type = callParams.get("type");
                if (TextUtils.equals(type, "wifi_setup_rsa")) {
                    String key64 = callParams.get("data");
                    bLanKey = decode(key64);
                } else {
                    return AylaNetworks.AML_ERROR_FAIL; // Unsupported key generation type
                }
            } else if ( device.lanModeConfig != null && device.lanModeConfig.lanipKey != null ){
                sLanKey = device.lanModeConfig.lanipKey;
                bLanKey = sLanKey.getBytes("UTF-8");
            } else {
                Log.e("BSK", "null params, not lan mode active");
                return AylaNetworks.AML_ERROR_FAIL;
            }
        } catch (UnsupportedEncodingException e) {
            AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s:%s, %s", "E", "AylaEncryption", "sRnd_1", sRnd_1, "sRnd_2", sRnd_2, "generateSessionKeys");
            e.printStackTrace();
            return AylaNetworks.AML_ERROR_FAIL;
        }

        sTime_1 = nTime_1.toString();
        sTime_2 = nTime_2.toString();
        try {
            bTime_1 = sTime_1.getBytes("UTF-8");
            bTime_2 = sTime_2.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s:%s, %s", "E", "AylaEncryption", "nTime_1", nTime_1, "sRnd_2", sRnd_2, "generateSessionKeys");
            e.printStackTrace();
            Log.e("BSK", "Unsupported encoding");
            return AylaNetworks.AML_ERROR_FAIL;
        }

        // generate session keys
        byte[] bTempKey = null;

        // App Signing key:    <random_1> + <random_2> + <time_1> + <time_2> + 0
        // App Encrypting key: <random_1> + <random_2> + <time_1> + <time_2> + 1
        // App IV CBC seed:    <random_1> + <random_2> + <time_1> + <time_2> + 2
        lastByte[0] = 48;
        bTempKey = concat(bRnd_1, bRnd_2, bTime_1, bTime_2, lastByte);
        appSignKey = hmacForKeyAndData(bLanKey, concat(hmacForKeyAndData(bLanKey, bTempKey), bTempKey));

        lastByte[0] = 49;
        bTempKey = concat(bRnd_1, bRnd_2, bTime_1, bTime_2, lastByte);
        appCryptoKey = hmacForKeyAndData(bLanKey, concat(hmacForKeyAndData(bLanKey, bTempKey), bTempKey));

        lastByte[0] = 50;
        bTempKey = concat(bRnd_1, bRnd_2, bTime_1, bTime_2, lastByte);
        byte[] seed = hmacForKeyAndData(bLanKey, concat(hmacForKeyAndData(bLanKey, bTempKey), bTempKey));
        appIvSeed = Arrays.copyOfRange(seed, 0, 16);

        // Debug Only!
        AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaEncryption", "devLanIpKey", sLanKey, "generateSessionKeys");

        //AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaEncryption", "appSignKeyHex", bytesToHex(appSignKey), "generateSessionKeys");
        //AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaEncryption", "appCryptoKeyHex", bytesToHex(appCryptoKey), "generateSessionKeys");
        //AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaEncryption", "appIvSeedHex", bytesToHex(appIvSeed), "generateSessionKeys");

        // Device Signing key:    <random_2> + <random_1> + <time_2> + <time_1> + 0
        // Device Encrypting key: <random_2> + <random_1> + <time_2> + <time_1> + 1
        // Device IV CBC seed:    <random_2> + <random_1> + <time_2> + <time_1> + 2

        lastByte[0] = 48;
        bTempKey = concat(bRnd_2, bRnd_1, bTime_2, bTime_1, lastByte);
        devSignKey = hmacForKeyAndData(bLanKey, concat(hmacForKeyAndData(bLanKey, bTempKey), bTempKey));

        lastByte[0] = 49;
        bTempKey = concat(bRnd_2, bRnd_1, bTime_2, bTime_1, lastByte);
        devCryptoKey = hmacForKeyAndData(bLanKey, concat(hmacForKeyAndData(bLanKey, bTempKey), bTempKey));

        lastByte[0] = 50;
        bTempKey = concat(bRnd_2, bRnd_1, bTime_2, bTime_1, lastByte);
        seed = hmacForKeyAndData(bLanKey, concat(hmacForKeyAndData(bLanKey, bTempKey), bTempKey));
        devIvSeed = Arrays.copyOfRange(seed, 0, 16);

        // Debug Only!
        //AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaEncryption", "devSignKeyHex", bytesToHex(devSignKey), "generateSessionKeys");
        //AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaEncryption", "devCryptoKeyHex", bytesToHex(devCryptoKey), "generateSessionKeys");
        //AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaEncryption", "devIvSeedHex", bytesToHex(devIvSeed), "generateSessionKeys");

        // instantiate cipher objects
        try {
            // encrypt
            eCipher = Cipher.getInstance("AES/CBC/NoPadding");
            eSkey = new javax.crypto.spec.SecretKeySpec(appCryptoKey, "AES");
            eCipher.init(Cipher.ENCRYPT_MODE, eSkey, new IvParameterSpec(appIvSeed));

            // decrypt
            dCipher = Cipher.getInstance("AES/CBC/NoPadding");
            dSkey = new javax.crypto.spec.SecretKeySpec(devCryptoKey, "AES");
            dCipher.init(Cipher.DECRYPT_MODE, dSkey, new IvParameterSpec(devIvSeed));
        } catch (Exception e) {
            AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "E", "AylaEncryption", "init", "failed", "generateSessionKeys.cipher");
            System.out.println(e.toString());
            Log.e("BSK", "Exception enc / dec");
        }

        return AylaNetworks.AML_ERROR_OK;
    }

    public String encryptEncapsulateSign(int seq_no, String jsonProperty, byte[] sign) {
        String jsonBase64;
        String jsonText = "";

        String jsonText0 = "{\"enc\":" + "\"";

        String jsonText1 = "";
        jsonText1 = jsonText1 + "{\"seq_no\":" + seq_no;
        jsonText1 = jsonText1 + ",\"data\":";
        if (jsonProperty != null) {

            jsonText1 = jsonText1 + jsonProperty;
        }

        jsonText1 = jsonText1 + "}";

        byte[] bJsonText1 = null;
        String errMsg = String.format("%s, %s, %s:%s, %s", "E", "AylaLanMode", "jsonPropertyText", jsonText1, "encryptEncapsulateSign");
        bJsonText1 = AylaSystemUtils.stringToBytes(jsonText1, errMsg);

        // signature
        String jsonText2 = "";

        byte[] thisSign = AylaEncryption.hmacForKeyAndData(sign, bJsonText1);

        jsonBase64 = Base64.encodeToString(thisSign, Base64.NO_WRAP);

        jsonText2 = jsonText2 + "\"sign\":" + "\"" + jsonBase64 + "\""; // add signature
        jsonText2 = jsonText2 + "}";

        // create a padded buffer for CBC cipher
        int len = jsonText1.length() + 1; // add one for nul termination
        int pad = len % 16; // 128 bit AES buffer
        pad = (pad > 0) ? (16 - pad) : pad;
        byte[] paddedBuffer = new byte[len + pad];
        paddedBuffer = Arrays.copyOfRange(bJsonText1, 0, len + pad); // assumes paddedBuffer is null filled

        // Encrypt the message using key and initialization vector derived during key generation, then base64 encode
        try {
            byte[] encrypted;

            encrypted = eCipher.update(paddedBuffer); // encrypt

            jsonBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP); // encode

            jsonText1 = jsonBase64 + "\",";
        } catch (Exception e) {
            AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "E", "AylaEncryption", "jsonText1Bytes:str", jsonText1, "encryptEncapsulateSign.cipher");
            System.out.println(e);
        }

        jsonText = String.format("%s%s%s", jsonText0, jsonText1, jsonText2);
        return jsonText;
    }

    static byte[] decode(String encoded) {
        byte[] decoded = null;

        decoded = Base64.decode(encoded, Base64.NO_WRAP); // base64 decode

        return decoded;
    }

    // Base64 decode, then decrypt the message using key and initialization vector derived during key generation.
    public String unencodeDecrypt(String encodedEncrypted) {
        byte[] decoded = null;
        String unEncrypted = null;
        byte[] unEncrypt = null;
        if (encodedEncrypted != null) {
            try {
                decoded = Base64.decode(encodedEncrypted, Base64.NO_WRAP); // base64 decode

                try {
                    unEncrypt = dCipher.update(decoded); // decrypt

                    // strip buffer nuls if any, convert to a string
                    int i = unEncrypt.length - 1;
                    byte[] unEncryptNulTerm = null;
                    while ((unEncrypt[i] == 0) && (i >= 0)) i--;
                    unEncryptNulTerm = Arrays.copyOfRange(unEncrypt, 0, ++i);
                    unEncrypted = new String(unEncryptNulTerm, 0, i, "UTF-8");
                    AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "AylaEncryption", "clear_text:str", unEncrypted, "unencodeDecrypt.cipher");
                } catch (Exception e) {
                    AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "E", "AylaEncryption", "unencrypt:str", unEncrypt, "unencodeDecrypt.cipher");
                    System.out.println(e);
                }
            } catch (Exception e) {
                AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "E", "AylaEncryption", "encodedEncrypted", encodedEncrypted, "unencodeDecrypt.Base64.decode");
                e.printStackTrace();
            }
        } else {
            AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "E", "AylaEncryption", "encodedEncrypted", "null", "unencodeDecrypt");
        }
        return unEncrypted;
    }

    static byte[] hmacForKeyAndData(byte[] key, byte[] data) {
        javax.crypto.Mac mac = null;
        try {
            mac = javax.crypto.Mac.getInstance("HmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        javax.crypto.spec.SecretKeySpec secret = new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256");
        try {
            mac.init(secret);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
        byte[] digest = mac.doFinal(data);
        return digest;
    }

    static String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String convertHexToString(String hex) {

        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        //49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for (int i = 0; i < hex.length() - 1; i += 2) {

            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char) decimal);

            temp.append(decimal);
        }
        System.out.println("Decimal : " + temp.toString());

        return sb.toString();
    }

    static byte[] concat(byte[]... arrays) {
        // Determine the length of the result array
        int totalLength = 0;
        for (int i = 0; i < arrays.length; i++) {
            totalLength += arrays[i].length;
        }

        // create the result array
        byte[] result = new byte[totalLength];

        // copy the source arrays into the result array
        int currentIndex = 0;
        for (int i = 0; i < arrays.length; i++) {
            System.arraycopy(arrays[i], 0, result, currentIndex, arrays[i].length);
            currentIndex += arrays[i].length;
        }

        return result;
    }

    // generate an alphanumeric random number
    static String randomToken(int length) {
        String token = "";
        char c;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < length; i++) {
            c = chars.charAt((int) (Math.random() * chars.length()));
            token += c;
        }

        return token;
    }

    // generate a random number
    static String randomNumber(int length) {
        String token = "";
        char c;
        String chars = "0123456789";
        for (int i = 0; i < length; i++) {
            c = chars.charAt((int) (Math.random() * chars.length()));
            token += c;
        }

        return token;
    }
}






