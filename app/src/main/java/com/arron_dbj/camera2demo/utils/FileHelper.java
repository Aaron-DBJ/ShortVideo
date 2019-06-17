package com.arron_dbj.camera2demo.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public class FileHelper {
    private static volatile FileHelper fileHelper;
    private Context mContext;

    private FileHelper(Context context){
        mContext = context;
    }
    public static FileHelper getInstance(Context context){
        if (fileHelper == null){
            synchronized (FileHelper.class){
                if (fileHelper == null){
                    fileHelper = new FileHelper(context);
                }
            }
        }
        return fileHelper;
    }

    public File getDiskCacheDir(Context context, String dirName){
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
        || !Environment.isExternalStorageRemovable()){
            //getExternalCacheDir获取的路径是sdcard/android/data/<package name>/cache
            cachePath = context.getExternalCacheDir().getPath();
        }else {
            //getCacheDir获取的路径是data/data/<package name>/cache
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + dirName);
    }

    public String getFileName(String filePath){
        int index = filePath.lastIndexOf("/");
        return filePath.substring(index+1);
    }
}
