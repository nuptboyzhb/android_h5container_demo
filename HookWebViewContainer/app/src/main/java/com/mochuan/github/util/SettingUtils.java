package com.mochuan.github.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.mochuan.github.XApplication;

/**
 * @version mochuan.zhb on 2020-12-30
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 设置工具类
 */
public class SettingUtils {

    public static final String KEY_OK_HTTP = "okhttp_cache";
    public static final String KEY_GLIDE = "glide_cache";
    public static final String KEY_MSG_C = "msg_channel";

    public static boolean isEnabledOkhttpCache() {
        return getSettingInCache(KEY_OK_HTTP);
    }

    public static boolean isEnabledGlideCache() {
        return getSettingInCache(KEY_GLIDE);
    }

    public static boolean isEnabledMsgChannel() {
        return getSettingInCache(KEY_MSG_C);
    }

    public static void saveSettingInCache(String key, boolean value) {
        SharedPreferences sharedPreferences = XApplication.getApplication().getSharedPreferences("setting", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static boolean getSettingInCache(String key) {
        SharedPreferences sharedPreferences = XApplication.getApplication().getSharedPreferences("setting", Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(key, true);
    }

}
