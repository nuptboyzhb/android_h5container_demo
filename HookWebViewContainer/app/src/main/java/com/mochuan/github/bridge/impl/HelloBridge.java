package com.mochuan.github.bridge.impl;

import android.webkit.WebView;
import android.widget.Toast;

import com.mochuan.github.bridge.IXBridge;
import com.mochuan.github.bridge.IXBridgeCallback;
import com.mochuan.github.log.XLog;
import com.mochuan.github.webview.XJSBridgeImpl;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @version mochuan.zhb on 2020-11-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 一个简单的JSBridge实现
 */
public class HelloBridge implements IXBridge {
    @Override
    public String getFuncName() {
        return "nativeMethod";
    }

    @Override
    public void process(WebView mWebView, String param, final IXBridgeCallback callback) {
        Toast.makeText(mWebView.getContext(), "Hello XJSBridge!I am in native.", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                XLog.d("hello, I am native method...");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                JSONObject mockRes = new JSONObject();
                try {
                    mockRes.put("bridge", XJSBridgeImpl.class.getName());
                    mockRes.put("data", "I am from native");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (callback != null) {
                    callback.success(mockRes);
                }
            }
        }).start();
    }
}
