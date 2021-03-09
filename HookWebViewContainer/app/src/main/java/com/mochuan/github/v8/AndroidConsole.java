package com.mochuan.github.v8;

import com.alibaba.fastjson.JSONObject;
import com.eclipsesource.v8.V8Object;
import com.mochuan.github.log.XLog;

/**
 * @version mochuan.zhb on 2020-12-30
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 桥接日志，注册到
 */
public class AndroidConsole {

    private static final String TAG = ">>>AndroidConsole<<<";

    /**
     * 通过反射注册Java方法
     *
     * @param msg
     */
    public void log(String msg) {
        XLog.d(TAG, msg);
    }

    public void logObj(V8Object msg) {
        try {
            JSONObject jsonObject = XV8Utils.toJSONObject(msg);
            XLog.d(TAG, jsonObject.toJSONString());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * 通过反射注册Java方法
     *
     * @param msg
     */
    public void error(String msg) {
        XLog.e(TAG, msg);
    }
}
