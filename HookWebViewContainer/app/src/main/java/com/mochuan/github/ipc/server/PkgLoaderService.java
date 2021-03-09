package com.mochuan.github.ipc.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.mochuan.github.XApplication;
import com.mochuan.github.h5pkg.OfflinePkgManager;
import com.mochuan.github.ipc.IH5PkgService;
import com.mochuan.github.log.XLog;

import java.util.Map;

/**
 * @version mochuan.zhb on 2020-12-30
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 离线包的Service
 */
public class PkgLoaderService extends Service {

    private static final String TAG = "PkgLoaderService";

    @Override
    public void onCreate() {
        super.onCreate();
        XLog.d("onCreate");
        int pid = android.os.Process.myPid();
        XLog.d(TAG, "pid = " + pid);
        //在主进程加载离线包
        XApplication.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                OfflinePkgManager.getInstance().loadAssetsPkg(getApplicationContext());
            }
        });
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        XLog.d(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);

    }

    private IH5PkgService.Stub binder = new IH5PkgService.Stub() {

        @Override
        public Map getOfflinePkg() throws RemoteException {
            XLog.d("getOfflinePkg in service");
            return OfflinePkgManager.getInstance().getOfflinePkg();
        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        int pid = android.os.Process.myPid();
        Log.d("TAG", "onBind hashCode:" + getApplication().hashCode() + ",pid = " + pid);
        return binder;
    }
}
