package com.mochuan.github.bridge.impl;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.ArrayMap;
import android.webkit.WebView;

import com.mochuan.github.XApplication;
import com.mochuan.github.activity.BaseActivity;
import com.mochuan.github.activity.XWebViewActivity;
import com.mochuan.github.bridge.IXBridge;
import com.mochuan.github.bridge.IXBridgeCallback;
import com.mochuan.github.log.XLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version mochuan.zhb on 2021-01-15
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 实现一个向WebView发送msg的Bridge
 */
public class PostMsgToWebViewBridge implements IXBridge {

    @Override
    public String getFuncName() {
        return "postMsgToWebView";
    }

    @Override
    public void process(WebView mWebView, final String param, IXBridgeCallback callback) {
        //TODO 获取最上层的Activity，然后获取webview的MessageChannel，将消息发送出去
        XLog.d("PostMsgToWebViewBridge...");
//        Activity topActivity = getForegroundActivity(XApplication.getApplication());

        Activity topActivity = BaseActivity.getCurrentActivity();
        if (topActivity instanceof XWebViewActivity) {
            final XWebViewActivity currentActivity = (XWebViewActivity) topActivity;
            final Runnable task = new Runnable() {
                @Override
                public void run() {
                    XLog.d("start to post...");
                    currentActivity.getWebViewPage().getMessageChannel().postMessageToH5(param);
                }
            };
            XApplication.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    XLog.d("wait to post...");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    currentActivity.runOnUiThread(task);
                }
            });
        }
    }


    public static Activity getForegroundActivity(Context context) {
        List<Activity> list = getActivities(context, true);
        return list.isEmpty() ? null : (Activity) list.get(0);
    }

    public static List<Activity> getActivities(Context context,
                                               boolean foregroundOnly) {
        List<Activity> list = new ArrayList<>();
        try {
            Class activityThreadClass = Class
                    .forName("android.app.ActivityThread");
            Object activityThread = getActivityThread(context,
                    activityThreadClass);
            Field activitiesField = activityThreadClass
                    .getDeclaredField("mActivities");
            activitiesField.setAccessible(true);

            Object collection = activitiesField.get(activityThread);
            Collection c;
            if ((collection instanceof HashMap)) {
                Map activities = (HashMap) collection;
                c = activities.values();
            } else {
                if ((Build.VERSION.SDK_INT >= 19)
                        && ((collection instanceof ArrayMap))) {
                    ArrayMap activities = (ArrayMap) collection;
                    ArrayList aList = new ArrayList();
                    for (Map.Entry<String, Object> entry : ((ArrayMap<String, Object>) activities).entrySet()) {
                        aList.add(entry.getValue());
                    }
                    c = aList;
                } else {
                    return list;
                }
            }
            for (Object activityRecord : c) {
                Class activityRecordClass = activityRecord.getClass();
                if (foregroundOnly) {
                    Field pausedField = activityRecordClass
                            .getDeclaredField("paused");
                    pausedField.setAccessible(true);
                    if (pausedField.getBoolean(activityRecord)) {
                    }
                } else {
                    Field activityField = activityRecordClass
                            .getDeclaredField("activity");
                    activityField.setAccessible(true);
                    Activity activity = (Activity) activityField
                            .get(activityRecord);
                    if (activity != null) {
                        list.add(activity);
                    }
                }
            }
        } catch (Throwable ignore) {
            ignore.printStackTrace();
        }
        return list;
    }

    /**
     * 获取当前的ActivityThread对象
     *
     * @param context
     * @param activityThread
     * @return
     */
    public static Object getActivityThread(Context context,
                                           Class<?> activityThread) {
        try {
            if (activityThread == null) {
                activityThread = Class.forName("android.app.ActivityThread");
            }
            Method m = activityThread.getMethod("currentActivityThread",
                    new Class[0]);
            m.setAccessible(true);
            Object currentActivityThread = m.invoke(activityThread,
                    new Object[0]);
            Object apk = null;
            Field mActivityThreadField = null;
            if ((currentActivityThread == null) && (context != null)) {
                Field mLoadedApk = context.getClass().getField("mLoadedApk");
                mLoadedApk.setAccessible(true);
                apk = mLoadedApk.get(context);
                mActivityThreadField = apk.getClass().getDeclaredField(
                        "mActivityThread");
                mActivityThreadField.setAccessible(true);
            }
            return mActivityThreadField.get(apk);
        } catch (Throwable ignore) {
            ignore.printStackTrace();
        }
        return null;
    }


}
