package com.mochuan.github.bridge;

import android.webkit.WebView;

/**
 * @version mochuan.zhb on 2020-11-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description JSBridge的接口规范
 */
public interface IXBridge {

    String getFuncName();
    void process(WebView mWebView,String param, IXBridgeCallback callback);
}
