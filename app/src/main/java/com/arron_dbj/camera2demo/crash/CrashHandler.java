package com.arron_dbj.camera2demo.crash;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.util.Log;

import com.arron_dbj.camera2demo.utils.StringUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String tag = "CrashHandler";
    private Context mContext;
    private static volatile CrashHandler crashHandler;
    private Thread.UncaughtExceptionHandler mDefaultUncaughtHandler;
    private CrashHandler(){
    }

    public static CrashHandler getInstance(){
        if (crashHandler == null){
            synchronized (CrashHandler.class){
                if (crashHandler == null){
                    crashHandler = new CrashHandler();
                }
            }
        }
        return crashHandler;
    }

    public void init(Context context){
        mDefaultUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        mContext = context;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        dumpToSDCard(e);
        //TODO 上传到服务器

        if (mDefaultUncaughtHandler != null){
            mDefaultUncaughtHandler.uncaughtException(t, e);
        }else {
            Process.killProcess(Process.myPid());
        }
    }

    private void dumpToSDCard(Throwable e){
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            Log.d(tag, "no sdcard, skip dump");
            return;
        }

        File dir = getDir(mContext, "Crash");
        if (!dir.exists())
            dir.mkdirs();

        File file = new File(dir, StringUtil.getCurrentTime_yyyyMMddHHmmss()+".log");
        String time = StringUtil.getCurrentTime_yyyy_MM_dd_HH_mm_ss();
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            writer.println(time);
            dumpPhoneInfo(writer, e);
            writer.println();
            writer.flush();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (PackageManager.NameNotFoundException e1) {
            e1.printStackTrace();
        }finally {
            if (writer != null){
                writer.close();
            }
        }
    }

    private void dumpPhoneInfo(PrintWriter writer, Throwable e) throws PackageManager.NameNotFoundException {
        PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(),
                PackageManager.GET_ACTIVITIES);

        writer.print("APP Version：");
        writer.print(packageInfo.versionName + "_");
        writer.println(packageInfo.versionCode);

        writer.print("OS Version: ");
        writer.print(Build.VERSION.RELEASE);
        writer.print("_");
        writer.println(Build.VERSION.SDK_INT);

        writer.print("Vendor: ");
        writer.println(Build.MANUFACTURER);

        writer.print("Model: ");
        writer.println(Build.MODEL);

        writer.print("CPU_ABI: ");
        writer.println(Build.CPU_ABI);
        writer.println("Details:");
        e.printStackTrace(writer);
    }

    private File getDir(Context context, String dirName){
        String path;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ||
        !Environment.isExternalStorageRemovable()){
            path = context.getExternalCacheDir().getPath();
        }else {
            path = context.getCacheDir().getPath();
        }

        return new File(path + File.separator + dirName);
    }
}
