package com.mochuan.github.v8.timeout;


import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import com.mochuan.github.log.XLog;
import com.mochuan.github.v8.XV8Manager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @version mochuan.zhb on 2020-12-30
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description JS的setTimeout桥接实现
 */
public class SetTimeoutCallBack implements JavaCallback {

    private XV8Manager mXV8Manager;

    public SetTimeoutCallBack(XV8Manager xv8Manager) {
        this.mXV8Manager = xv8Manager;
    }

    @Override
    public Object invoke(final V8Object receiver, V8Array parameters) {
        XLog.d("SetTimeoutCallBack invoke start ...");
        if (parameters.length() == 2) {
            final Object timeout = parameters.get(1);
            final Object callback = parameters.get(0);
            if (!(timeout instanceof Integer) || !(callback instanceof V8Function)) {
                XLog.e("SetTimeoutCallBack parameters error...");
                return null;
            }
            final Integer timeoutValue = (Integer) timeout;
            final V8Function callbackFunc = ((V8Function) callback);
            mXV8Manager.getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        V8Function v8Function = callbackFunc.twin();
                        v8Function.call(receiver, null);
                        v8Function.close();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }

                }
            },timeoutValue);
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        Thread.sleep(timeoutValue);
//                    } catch (Throwable t) {
//                        t.printStackTrace();
//                    }
//                    XLog.d("native invoke setTimeout js function callback in thread.");
//                    //需要在主线程调用
//                    mXV8Manager.getHandler().post(new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//                                callbackFunc.twin();
//                                callbackFunc.call(receiver, null);
//                                callbackFunc.close();
//                            } catch (Throwable t) {
//                                t.printStackTrace();
//                            }
//
//                        }
//                    });
//                }
//            }).start();
//            ScheduledFuture scheduledFuture = Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
//                @Override
//                public void run() {
//                    //调用JS回调
//                    XLog.d("native invoke setTimeout js function callback ...");
//                    callbackFunc.call(receiver, null);
//
//                }
//            }, timeoutValue, TimeUnit.MILLISECONDS);
//            //保存ScheduledFuture，并将生成的taskId返回给前端，与clearTimeOut
//            int taskId = ScheduledFutureManager.addTimeoutTaskInfo(scheduledFuture);
//            return taskId;
        }
        return -1;
    }
}
