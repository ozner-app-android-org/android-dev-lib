/*
 * AylaLogManager.java
 * Ayla Mobile Library
 *
 * Created by Raji Pillay on 7/26/15.
 * Copyright (c) 2015 Ayla Networks. All Rights Reserved.
 * */
package com.aylanetworks.aaml;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Class used to create and initialize loggers needed for the library.
 *
 */
public class AylaLogManager {

    //single instance of the log manager
    private static AylaLogManager logManagerInstance;

    public static enum LOG_LEVEL {verbose, info, debug, warning, error};
    public static LOG_LEVEL loggingLevel = LOG_LEVEL.error;
    public static String logFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + AylaNetworks.DEFAULT_LOGFILE_NAME;

    //list to store log messages
    private static LinkedList<LogMessage> logMessageList = new LinkedList();

    //private list of file and cloud loggers for use within the library. Not exposed to app
    private static List<AylaLogger> loggerList = new ArrayList<>();
    private static List<AylaLogger> appLoggerList = new ArrayList<>();

    private static CloudLogger cloudLogger = new CloudLogger();
    private static FileLogger fileLogger = new FileLogger();
    private static ConsoleLogger consoleLogger = new ConsoleLogger();

    public static List<AylaLogger> getLoggerList() {
        return loggerList;
    }

    protected static void setLoggerList(ArrayList<AylaLogger> loggerList) {
        AylaLogManager.loggerList = loggerList;
    }



    //singleton instance
    public static AylaLogManager getInstance(){
        if(logManagerInstance == null){
            logManagerInstance = new AylaLogManager();

        }
        if(loggerList.size() == 0){
            loggerList.add(fileLogger);
            loggerList.add(consoleLogger);
        }
        return logManagerInstance;
    }

    // file and console loggers are enabled by default
    public void init(){
        //by default add file and console
        if(loggerList.isEmpty()){
            loggerList.add(fileLogger);
            loggerList.add(consoleLogger);
        }


    }


    /*
    *add loggers for applications
     */

    public AylaLogger addLogger(String regex){

       if(regex != null){
           AylaLogger logger = new ConsoleLogger();
           logger.setLoggerIdRegex(regex);
           appLoggerList.add(logger);
           return logger;
       }
        return null;
    }


    /*
    * Method called from AylaSystemUtils to send messages to existing loggers
    * Logs are queued up and sent asynchronously
     */
    protected void log(String tag, LOG_LEVEL level, String format, Object...args) {


        //check logging level from settings
        if(AylaLogManager.getLogLevel(level) <=  AylaSystemUtils.loggingLevel){
            LogMessage logMsg = new LogMessage(tag,level, 0, format, args);
            enqueueLogMessage(logMsg);
            sendQueuedLogs();
        }

    }


    public void removeLogger(String type){
        if(type.equals("file")){
            loggerList.remove(fileLogger);
        }
        else if(type.equals("console")){
            loggerList.remove(consoleLogger);
        }
        else if(type.equals("cloud")){
            loggerList.remove(cloudLogger);
        }
        else{
            //check if multiple apploggers are supported
            appLoggerList.remove(0);
        }

    }


    private static synchronized void enqueueLogMessage(LogMessage logMessage){
        logMessageList.add(logMessage);
    }

    private static synchronized LogMessage dequeueLogMessage(){
        if(logMessageList == null){
            return null;
        }
        try{
            if(!logMessageList.isEmpty()){
                return logMessageList.removeFirst();
            }
        }catch(NoSuchElementException e){
            e.printStackTrace();
            return null;
        }

        return null;
    }

    private static void sendQueuedLogs(){

        LogMessage logFromQueue = dequeueLogMessage(); //this is fully formatted message
        if(logFromQueue != null && AylaNetworks.appContext != null){

            for(AylaLogger logger: loggerList){
                logger.saveLogs(logsHandler, logFromQueue);
            }

            for(AylaLogger logger: appLoggerList){
                logger.saveLogs(logsHandler, logFromQueue);
            }
        }

    }


    /*
    set log level for the app
    params: loglevels in AylaNetworks.AML_LOGGING_LEVEL*
     */


    public void setLogLevel(int logLevel) {
        switch (logLevel) {
            case AylaNetworks.AML_LOGGING_LEVEL_ALL:
                loggingLevel = LOG_LEVEL.verbose;
                break;
            case AylaNetworks.AML_LOGGING_LEVEL_ERROR:
                loggingLevel = LOG_LEVEL.error;
                break;
            case AylaNetworks.AML_LOGGING_LEVEL_DEBUG:
                loggingLevel = LOG_LEVEL.debug;
                break;
            case AylaNetworks.AML_LOGGING_LEVEL_WARNING:
                loggingLevel = LOG_LEVEL.warning;
                break;
            case AylaNetworks.AML_LOGGING_LEVEL_INFO:
                loggingLevel = LOG_LEVEL.info;
                break;
            default:
                loggingLevel = LOG_LEVEL.info;


        }

    }


    public static int getLogLevel(LOG_LEVEL level) {
        switch (level) {
            case verbose:
                return AylaNetworks.AML_LOGGING_LEVEL_ALL;

            case info:
                return AylaNetworks.AML_LOGGING_LEVEL_INFO;

            case warning:
                return AylaNetworks.AML_LOGGING_LEVEL_WARNING;

            case error:
                return AylaNetworks.AML_LOGGING_LEVEL_ERROR;

            case debug:
                return AylaNetworks.AML_LOGGING_LEVEL_DEBUG;

            default:
                return AylaNetworks.AML_LOGGING_LEVEL_INFO;
        }
    }

    public static String getLogFilePath() {
        return fileLogger.getCurrentFilePath();
    }
    /*
    Log to cloud service. Not implemented

     */
    private static class CloudLogger extends AylaLogger{


        @Override
        public String logFormatter(LogMessage logMsg) {

            return "Cloud Logger test";
        }

        @Override
        public void saveLogs(Handler handler,  LogMessage logMsg) {
            Log.d(logMsg.tag, logFormatter(logMsg));
            handler.sendEmptyMessage(0);

        }
    }

/*
*Send logs to file aml_log(*)
 */
    private static class FileLogger extends AylaLogger{
    public static String logFileName = AylaNetworks.DEFAULT_LOGFILE_NAME;
    private static final int NUM_OF_LOG_FILES = 3; //files will be replaced after max number is reached
    private static final int FILE_MEMORY_LIMIT = 200000;
    public static int currentFile = 1;



    @Override
    public String logFormatter(LogMessage logMsg) {
        String msg = null;
        List<Object> arrayOfArgs = new ArrayList<Object>();
        String spaceChar = ",  ";
        StringBuilder strBuilder = new StringBuilder();
        String date = null;

        //add date
        strBuilder.append("\n");
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
        if(logMsg.time != 0){
            Calendar logGenCal = Calendar.getInstance();
            logGenCal.setTimeInMillis(logMsg.time);
            Date logGenDate = logGenCal.getTime();
            date = dateFormat.format(logGenDate);
        }
        else {
            Date currentDate = Calendar.getInstance().getTime();
            date = dateFormat.format(currentDate);
        }
        strBuilder.append(date);
        strBuilder.append(spaceChar);


        for (Object arg : logMsg.args) {
            arrayOfArgs.add(arg);
        }
        if(logMsg.format != null && arrayOfArgs.size()!= 0){
            strBuilder.append( String.format(logMsg.format, arrayOfArgs.toArray()));
        }
        else{
            strBuilder.append( "Error in file logger format");
        }

        return strBuilder.toString();
    }

    @Override
        public void saveLogs(Handler handler,  LogMessage logMsg) {
        String msg = logFormatter(logMsg);
       // handler.sendEmptyMessage(0);
        writeMessageToFile(handler, msg);

        }

    public String getCurrentFilePath(){
        String currentFilePath = Environment.getExternalStorageDirectory().getAbsolutePath()+
                "/"+logFileName+ "("+String.valueOf(currentFile)+ ")";
        return currentFilePath;
    }

    public static void writeMessageToFile(Handler mHandle, String message){

       // Log.d("FILE", " currentFile "+currentFile);
        //Check if file is more than 200kb
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        File file = new File(path+ "/"+logFileName+ "("+String.valueOf(currentFile)+ ")");

        try {

            if(!file.exists()){
                //create new one
               // Log.d("FILE", " file doesn't exist. creating new file at "+file.getAbsolutePath());
                file.createNewFile();
            }
            else{
                if(file.length() < FILE_MEMORY_LIMIT && currentFile <= NUM_OF_LOG_FILES){
                    //Log.d("FILE", " file exists. writing to Current file "+file.getAbsolutePath());
                    //write to this
                    file.setWritable(true);

                    FileOutputStream fileOutputStream = new FileOutputStream(file,true);
                    fileOutputStream.write(message.getBytes());
                    fileOutputStream.flush();
                    fileOutputStream.close();

                }
                else if(currentFile < NUM_OF_LOG_FILES){
                    currentFile++;
                    file = new File(path+ "/"+logFileName+ "("+String.valueOf(currentFile)+ ")");
                    //write to this
                    //Log.d("FILE", " new file after limit exceeded. writing to file "+file.getAbsolutePath());
                    file.setWritable(true);
                    FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                    fileOutputStream.write(message.getBytes());
                    fileOutputStream.flush();
                    fileOutputStream.close();

                }
                else{
                    //else delete 1st file,

                    //Log.d("FILE", " delete first file and rename rest ");
                    File file1 =  new File(path+ "/"+logFileName+ "(1)");
                    file1.delete();

                    // rename all files and create a 3rd
                    for(int i=1; i<= NUM_OF_LOG_FILES; i++){
                        File thisFile = new File(path+ "/"+logFileName+ "("+String.valueOf(i)+ ")");
                        File renameFile = new File(path+ "/"+logFileName+ "("+String.valueOf(i-1)+ ")");
                        thisFile.renameTo(renameFile);
                    }

                    currentFile = NUM_OF_LOG_FILES;
                    file = new File(path+ "/"+logFileName+ "("+String.valueOf(currentFile)+ ")");
                    //write to this
                    file.setWritable(true);
                    FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                    fileOutputStream.write(message.getBytes());
                    fileOutputStream.flush();
                    fileOutputStream.close();

                }
            }
            mHandle.sendEmptyMessage(0);

        } catch (IOException e) {
            e.printStackTrace();
            mHandle.sendEmptyMessage(1);
        }

    }

}

/*
 Log to console

 */
    private static class ConsoleLogger extends AylaLogger{

        @Override
        public String logFormatter(LogMessage logMsg) {

            String msg = null;
            List<Object> arrayOfArgs = new ArrayList<Object>();
            String spaceChar = ",  ";
            StringBuilder strBuilder = new StringBuilder();
            for (Object arg : logMsg.args) {
                arrayOfArgs.add(arg);
            }
            if(logMsg.format != null && arrayOfArgs.size()!= 0){
               strBuilder.append(String.format(logMsg.format, arrayOfArgs.toArray()));
            }
            else{
                strBuilder.append("Error in console logger format");
            }

            return strBuilder.toString();
        }

        @Override
        public void saveLogs(Handler handler, LogMessage logMsg) {
            switch(logMsg.level){
                case "verbose":
                    Log.v(logMsg.tag, logFormatter(logMsg));
                    break;
                case "debug":
                    Log.d(logMsg.tag, logFormatter(logMsg));
                    break;
                case "error":
                    Log.e(logMsg.tag, logFormatter(logMsg));
                    break;
                case "warning":
                    Log.w(logMsg.tag, logFormatter(logMsg));
                    break;
                default:
                    Log.i(logMsg.tag, logFormatter(logMsg));
            }


            handler.sendEmptyMessage(0);
        }
    }


    private static final Handler logsHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (!logMessageList.isEmpty()) {
                sendQueuedLogs();
            }
        }
    };


}

class LogMessage {
    long time;
    String message; 	// The data to be logged , required
    String level; 	// the severity level for this log, required
    String format;
    int flag; // for future use. developers can set this flag
    String tag;
    Object args[];

    public LogMessage(String tag, AylaLogManager.LOG_LEVEL logLevel, int flag, String format, Object...args){
        time = System.currentTimeMillis();
        this.message = message;
        level = logLevel.name();
        this.tag = tag;
        this.args = args;
        this.format = format;

    }
}