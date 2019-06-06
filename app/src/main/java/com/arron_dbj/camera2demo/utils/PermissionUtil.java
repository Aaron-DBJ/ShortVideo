package com.arron_dbj.camera2demo.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class PermissionUtil {

    private static final String tag = "PermissionUtils";


    public static void verifyPermissions(Context context, String[] permissions, int requestCode){
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions((Activity) context, permissions, requestCode);
            Logger.getInstance().debug(tag, "申请权限成功");
        }else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
        PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions((Activity) context, permissions, requestCode);
            Logger.getInstance().debug(tag, "申请权限成功");
        }
    }




}
