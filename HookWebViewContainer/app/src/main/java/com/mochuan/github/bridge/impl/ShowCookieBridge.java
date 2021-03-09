package com.mochuan.github.bridge.impl;

import android.webkit.CookieManager;
import android.webkit.WebView;

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
 * @Description 展示cookie的jsbridge
 */
public class ShowCookieBridge implements IXBridge {

    @Override
    public String getFuncName() {
        return "showCookie";
    }

    @Override
    public void process(WebView mWebView, String param, IXBridgeCallback callback) {
        final String url = mWebView.getUrl();
        XLog.d("start get cookie...");
        String cookieResult = CookieManager.getInstance().getCookie(url);
        XLog.d("url = " + url + ",cookieResult=" + cookieResult);
        JSONObject mockRes = new JSONObject();
        try {
            mockRes.put("bridge", XJSBridgeImpl.class.getName());
            mockRes.put("data", "cookieResult = " + cookieResult);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (callback != null) {
            callback.success(mockRes);
        }
    }
}
