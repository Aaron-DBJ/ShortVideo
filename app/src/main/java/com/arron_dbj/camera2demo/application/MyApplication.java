package com.arron_dbj.camera2demo.application;

import android.app.Application;
import android.content.Context;

import com.arron_dbj.camera2demo.crash.CrashHandler;
import com.squareup.leakcanary.LeakCanary;

public class MyApplication extends Application {
    private Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
        mContext = getApplicationContext();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(mContext);
    }
}
