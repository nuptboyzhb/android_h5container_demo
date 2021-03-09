package com.mochuan.github.webview;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.RequiresApi;

/**
 * @version mochuan.zhb on 2020-11-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description extends 系统的WebView，重写部分方法
 */
public class XWebView extends WebView {
    public XWebView(Context context) {
        super(context);
    }

    public XWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public XWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public XWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public XWebView(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
        super(context, attrs, defStyleAttr, privateBrowsing);
    }

    @Override
    public WebSettings getSettings() {

        WebSettings webSettings = super.getSettings();

        String ua = webSettings.getUserAgentString();

        try {
            PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            String version = pInfo.versionName;
            String appName = getContext().getString(pInfo.applicationInfo.labelRes);
            String newUaWithVersion = ua + " AndroidApp_" + appName + "/" + version;
            webSettings.setUserAgentString(newUaWithVersion);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        webSettings.setCacheMode(WebSettings.LOAD_NORMAL);

        return webSettings;
    }
}
