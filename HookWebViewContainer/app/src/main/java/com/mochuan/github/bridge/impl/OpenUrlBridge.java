package com.mochuan.github.bridge.impl;

import android.text.TextUtils;
import android.webkit.WebView;

import com.mochuan.github.bridge.IXBridge;
import com.mochuan.github.bridge.IXBridgeCallback;
import com.mochuan.github.util.RouterManager;

import org.json.JSONObject;

/**
 * @version mochuan.zhb on 2020-11-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 实现一个打开H5页面的Bridge
 */
public class OpenUrlBridge implements IXBridge {
    @Override
    public String getFuncName() {
        return "openUrl";
    }

    @Override
    public void process(WebView mWebView, String param, IXBridgeCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject(param);
            String url = jsonObject.getString("url");
            JSONObject resp = new JSONObject(param);
            if (!TextUtils.isEmpty(url) && url.startsWith("http")) {
                RouterManager.openUrl(url);
                callback.success(resp);
            } else {
                callback.failed(resp);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
