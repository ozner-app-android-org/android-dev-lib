package com.ozner.util;

public class ByteUtil {

	public static void putShort(byte b[], short s, int index) {
		b[index] = (byte) (s >> 8);
		b[index + 1] = (byte) (s & 0xFF);
	}

	public static short getShort(byte[] b, int index) {
		return (short) (((b[index + 1] & 0xFF) << 8) + (b[index] & 0xFF));
	}

	public static void putInt(byte[] bb, int x, int index) {
		bb[index] = (byte) (x & 0x000000FF >> 0);
		bb[index + 1] = (byte) ((x & 0x0000FF00) >> 8);
		bb[index + 2] = (byte) ((x & 0x00FF0000) >> 16);
		bb[index + 3] = (byte) ((x & 0xFF000000) >> 24);
	}

	public static int getInt(byte[] bb, int index) {
		return (int) ((((bb[index + 3] & 0xff) << 24)
				| ((bb[index + 2] & 0xff) << 16)
				| ((bb[index + 1] & 0xff) << 8) | ((bb[index] & 0xff) << 0)));
	}

}