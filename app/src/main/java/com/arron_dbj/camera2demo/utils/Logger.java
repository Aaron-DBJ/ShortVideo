package com.arron_dbj.camera2demo.utils;

import android.util.Log;

public class Logger {
    private final int DEBUG = 1;
    private final int INFO = 2;
    private final int ERROE = 3;
    private final int NORMAL = 0;

    private int level = DEBUG;

    private volatile static Logger logger;

    private Logger(){}

    public static Logger getInstance(){
        if (logger == null){
            synchronized (Logger.class){
                if (logger == null){
                    logger = new Logger();
                }
            }
        }
        return logger;
    }

    public void debug(String tag, String log){
        if (level >= DEBUG) {
            Log.d(tag, log);
        }
    }

    public void info(String tag, String log){
        if (level >= INFO) {
            Log.d(tag, log);
        }
    }

    public void error(String tag, String log){
        if (level >= ERROE) {
            Log.d(tag, log);
        }
    }

    public void noraml(String tag, String log){
        if (level >= NORMAL) {
            Log.d(tag, log);
        }
    }

}
