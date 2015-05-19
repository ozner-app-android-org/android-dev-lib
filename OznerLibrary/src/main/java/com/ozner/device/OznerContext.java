package com.ozner.device;

import com.ozner.util.SQLiteDB;

import android.content.Context;

public class OznerContext {
	private Context mApplication;
	private SQLiteDB mDB;
	public SQLiteDB getDB() {
		return mDB;
	}
	public Context getApplication()
	{
		return mApplication;
	}
	
	public OznerContext (Context context)
	{
		mApplication=context;
		mDB=new SQLiteDB(context);
	}
}
