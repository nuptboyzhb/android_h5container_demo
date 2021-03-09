package com.mochuan.github.webview;

import android.webkit.JavascriptInterface;

import com.mochuan.github.log.XLog;

/**
 * @version mochuan.zhb on 2020-11-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 供js调用的方法类
 */
public class NativeLog {

    @JavascriptInterface
    public void print(String log) {
        XLog.d("nativeLog:" + log);
    }
}
