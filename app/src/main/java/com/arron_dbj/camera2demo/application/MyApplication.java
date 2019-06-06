package com.arron_dbj.camera2demo.application;

import android.app.Application;
import android.content.Context;

import com.arron_dbj.camera2demo.crash.CrashHandler;

public class MyApplication extends Application {
    private Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(mContext);
    }
}
