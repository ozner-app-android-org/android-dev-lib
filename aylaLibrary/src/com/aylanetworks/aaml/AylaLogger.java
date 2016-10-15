/*
 * AylaLogger.java
 * Ayla Mobile Library
 *
 * Created by Raji Pillay on 7/26/15.
 * Copyright (c) 2015 Ayla Networks. All Rights Reserved.
 * */

package com.aylanetworks.aaml;

import android.os.Handler;

import java.util.LinkedList;

/**
 * Abstract class for creating Loggers
 * */
public abstract class AylaLogger {

    private static LinkedList<String> logMessageList = new LinkedList();


    private static String loggerIdRegex = null;


    /*
    * override to format logs as required by the logger
     */
    public abstract String logFormatter(LogMessage logMsg);

    /*
  * override to save logs as required by the logger
   */
    public abstract void saveLogs(Handler handler, LogMessage logMsg);

    public void setLoggerIdRegex(String regex){
        loggerIdRegex = regex;
    }

    public String getLoggerIdRegex(){
        return loggerIdRegex;
    }

}
