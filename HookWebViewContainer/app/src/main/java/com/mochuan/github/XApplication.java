package com.mochuan.github;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.mochuan.github.cache.OkHttpCacheManager;
import com.mochuan.github.h5pkg.OfflinePkgManager;
import com.mochuan.github.log.XLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @version mochuan.zhb on 2020-12-22
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description application
 */
public class XApplication extends Application {

    private final static ExecutorService mThreadPool = Executors.newFixedThreadPool(5);
    public static boolean isMainProcess = true;
    private static Application application = null;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
                XLog.e("App Crash:");
                throwable.printStackTrace();
            }
        });
        isMainProcess = getApplicationContext().getPackageName().equals
                (getCurrentProcessName());
        if (isMainProcess) {
//            Intent intent = new Intent(this, PkgLoaderService.class);
//            startService(intent);
        } else {

        }

        getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                OfflinePkgManager.getInstance().loadAssetsPkg(getApplicationContext());
            }
        });
        //url维度的缓存
        List<String> urls = new ArrayList<>();
        urls.add("https://as.alipayobjects.com/g/component/fastclick/1.0.6/fastclick.js");
        urls.add("https://as.alipayobjects.com/g/component/es6-promise/3.2.2/es6-promise.min.js");
        urls.add("https://www.baidu.com/index.html");
        OkHttpCacheManager.getIntance().preLoadResource(urls);
    }

    /**
     * 获取当前进程名
     */
    private String getCurrentProcessName() {
        int pid = android.os.Process.myPid();
        String processName = "";
        ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService
                (Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo process : manager.getRunningAppProcesses()) {
            if (process.pid == pid) {
                processName = process.processName;
            }
        }
        return processName;
    }

    public static ExecutorService getThreadPool() {
        return mThreadPool;
    }

    public static Application getApplication() {
        return application;
    }
}
