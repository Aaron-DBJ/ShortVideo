package com.arron_dbj.camera2demo.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StringUtil {


    public static String getCurrentTime(String pattern){
        Date currentTime = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(currentTime);
    }

    public static String getCurrentTime_yyyyMMddHHmmss(){
        return getCurrentTime("yyyyMMddHHmmss");
    }

    public static String getCurrentTime_yyyy_MM_dd_HH_mm_ss(){
        return getCurrentTime("yyyy-MM-dd HH:mm:ss");
    }

}
