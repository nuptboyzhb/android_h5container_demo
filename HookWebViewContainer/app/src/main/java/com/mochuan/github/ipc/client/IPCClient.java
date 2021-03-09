package com.mochuan.github.ipc.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.mochuan.github.ipc.IH5PkgService;
import com.mochuan.github.ipc.server.PkgLoaderService;
import com.mochuan.github.log.XLog;

import java.util.Map;

/**
 * @version mochuan.zhb on 2020-12-30
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 离线包的client端，跨进程调用Service
 */
public class IPCClient {

    public static Map<String, byte[]> offlinePkg = null;
    private static boolean isBinding = false;

    public static void checkIPCIfNeed(Context context) {
        if (offlinePkg == null && !isBinding) {
            bindService(context);
        }
    }

    public static void bindService(Context context) {
        isBinding = true;
        Intent bindIntent = new Intent(context, PkgLoaderService.class);
        ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                XLog.d("onServiceConnected...");
                IH5PkgService h5PkgService = IH5PkgService.Stub.asInterface(service);
                try {
                    offlinePkg = (Map<String, byte[]>) h5PkgService.getOfflinePkg();
                    isBinding = false;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                XLog.d("onServiceDisconnected...");
                isBinding = false;
            }
        };
        context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
}
