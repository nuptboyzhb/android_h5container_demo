package com.mochuan.github.util;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.mochuan.github.activity.XWebViewActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @version mochuan.zhb on 2020-12-30
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description H5的路由控制
 */
public class RouterManager {

    private static Activity mMainActivity;
    private static int index = 0;

    private static List<String> availableActivityList = new ArrayList<>();

    private static final String[] H5_ACTIVITY_ARR = new String[]{
            XWebViewActivity.H5Activity1.class.getName(),
            XWebViewActivity.H5Activity2.class.getName(),
            XWebViewActivity.H5Activity3.class.getName(),
            XWebViewActivity.H5Activity4.class.getName(),
            XWebViewActivity.H5Activity5.class.getName(),
    };

    public static void init(Activity activity) {
        index = 0;
        mMainActivity = activity;
        availableActivityList.addAll(Arrays.asList(H5_ACTIVITY_ARR));
    }

    public static void openH5Activity(String url) {
        String activityClazz = availableActivityList.get(index % (availableActivityList.size() - 1));
        try {
            Class<?> c = Class.forName(activityClazz);
            Intent intent = new Intent(mMainActivity, c);
            intent.putExtra("url", url.trim());
            mMainActivity.startActivity(intent);
            index++;
        } catch (Exception ignored) {
        }
    }

    public static void openUrl(String url) {
        try {
            Intent intent = new Intent(mMainActivity, XWebViewActivity.class);
            intent.putExtra("url", url.trim());
            mMainActivity.startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    public static void openActivity(String activityClazz, String url) {
        try {
            Class<?> c = Class.forName(activityClazz);
            Intent intent = new Intent(mMainActivity, c);
            intent.putExtra("url", url.trim());
            mMainActivity.startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    public static boolean openScheme(String url) {
        try {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mMainActivity.startActivity(intent);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return false;
        }
        return true;
    }


    public static BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

}
