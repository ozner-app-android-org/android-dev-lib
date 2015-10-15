package com.ozner.util;

import android.content.Context;
import android.util.Log;

public class dbg {

	public static void i(String msg) {
		Log.i("ozner", msg);
		if (mMessageListener!=null)
		{
			mMessageListener.OnMessage(msg);
		}
	}

	public static void i(String msg, Object... args)
	{
		String m=String.format(msg, args);
		Log.i("ozner", m);
		if (mMessageListener!=null)
		{
			mMessageListener.OnMessage(m);
		}
	}

	public static void e(String msg) {
		Log.e("ozner", msg);
		if (mMessageListener!=null)
		{
			mMessageListener.OnMessage(msg);
		}
	}

	public static void e(String msg, Object... args) {
		String m=String.format(msg, args);
		Log.e("ozner", m);
		if (mMessageListener!=null)
		{
			mMessageListener.OnMessage(m);
		}
	}

	public static void d(String msg) {
		Log.d("ozner", msg);
		if (mMessageListener!=null)
		{
			mMessageListener.OnMessage(msg);
		}
	}
	public interface IDbgMessage
	{
		void OnMessage(String message);
	}
	static IDbgMessage mMessageListener=null;

	public static void setMessageListener(IDbgMessage messageListener)
	{
		mMessageListener=messageListener;
	}

	public static void d(String msg, Object... args) {
		String m=String.format(msg, args);

		Log.d("ozner", String.format(msg, args));

		if (mMessageListener!=null)
		{
			mMessageListener.OnMessage(m);
		}
	}
	public static void w(String msg) {
		Log.w("ozner", msg);
	}

	public static void w(String msg, Object... args) {
		Log.w("ozner", String.format(msg, args));
	}

}
