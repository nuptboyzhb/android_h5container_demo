package com.mochuan.github.log;

import android.util.Log;

/**
 * @version mochuan.zhb on 2020-11-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 日志类
 */
public class XLog {

    private static String TAG = "XLog";

    public static void d(String log) {
        Log.d(TAG, log);
    }

    public static void d(String tag, String log) {
        Log.d(TAG, tag + ":::" + log);
    }

    public static void e(String e) {
        Log.e(TAG, e);
    }

    public static void e(String tag, String e) {
        Log.e(TAG, tag + ":::" + e);
    }

    public static void e(Throwable e) {
        Log.e(TAG, "", e);
    }

    public static void w(String s) {
        Log.w(TAG, s);
    }

    public static void i(String s) {
        Log.i(TAG, s);
    }
}
