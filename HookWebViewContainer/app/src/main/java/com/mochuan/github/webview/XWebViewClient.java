package com.mochuan.github.webview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.mochuan.github.XApplication;
import com.mochuan.github.activity.TestActivity;
import com.mochuan.github.cache.GlideImgCacheManager;
import com.mochuan.github.cache.OkHttpCacheManager;
import com.mochuan.github.h5pkg.OfflinePkgManager;
import com.mochuan.github.log.XLog;
import com.mochuan.github.util.MimeTypeMapUtils;
import com.mochuan.github.util.RouterManager;
import com.mochuan.github.util.SettingUtils;
import com.mochuan.github.util.XFileUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @version mochuan.zhb on 2020-11-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description extends 系统的WebViewClient，重写关键方法
 */
public class XWebViewClient extends WebViewClient {

    private boolean loadTimeout;
    private long startLoadTime = 0L;

    WebViewPage webViewPage;

    private Handler mHandler;

    public XWebViewClient() {
        super();
        mHandler = new Handler(Looper.getMainLooper());
    }

    public XWebViewClient(WebViewPage webViewPage) {
        this.webViewPage = webViewPage;
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        startLoadTime = System.currentTimeMillis();
        webViewPage.hideErrorPage();
        XLog.d("onPageStarted:" + url);
        new Thread(new Runnable() {
            @Override
            public void run() {
                loadTimeout = true;
                try {
                    Thread.sleep(10000);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                if (loadTimeout) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            webViewPage.showErrorPage("页面加载超时(10s)");
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        //html+css+js加载完成即回调，不包括图片的加载时间以及其他网络请求的时间
        long endTime = System.currentTimeMillis();
        XLog.d(String.format("onPageFinished(%s):%s", (endTime - startLoadTime), url));
        loadTimeout = false;
        super.onPageFinished(view, url);
        String jsCode = ";(function(win){if(!win.performance||!win.performance.timing){return{}}var time=win.performance.timing;var timingResult={};timingResult[\"重定向时间\"]=(time.redirectEnd-time.redirectStart)/1000;timingResult[\"DNS解析时间\"]=(time.domainLookupEnd-time.domainLookupStart)/1000;timingResult[\"TCP完成握手时间\"]=(time.connectEnd-time.connectStart)/1000;timingResult[\"HTTP请求响应完成时间\"]=(time.responseEnd-time.requestStart)/1000;timingResult[\"DOM开始加载前所花费时间\"]=(time.responseEnd-time.navigationStart)/1000;timingResult[\"DOM加载完成时间\"]=(time.domComplete-time.domLoading)/1000;timingResult[\"DOM结构解析完成时间\"]=(time.domInteractive-time.domLoading)/1000;timingResult[\"脚本加载时间\"]=(time.domContentLoadedEventEnd-time.domContentLoadedEventStart)/1000;timingResult[\"onload事件时间\"]=(time.loadEventEnd-time.loadEventStart)/1000;timingResult[\"页面完全加载时间\"]=timingResult[\"重定向时间\"]+timingResult[\"DNS解析时间\"]+timingResult[\"TCP完成握手时间\"]+timingResult[\"HTTP请求响应完成时间\"]+timingResult[\"DOM结构解析完成时间\"]+timingResult[\"DOM加载完成时间\"];return{result:timingResult}})(this);";
        view.evaluateJavascript(jsCode, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {
                XLog.d("onPageFinished:JSResult = " + s);
            }
        });

        //using WebMessagePort is not nearly so nice. In particular,
        // you cannot attempt to initialize the communications until the page is loaded
        // (e.g., onPageFinished() on a WebViewClient).
        //webViewPage.getMessageChannel().initMessageChannel(view);
        if (SettingUtils.isEnabledMsgChannel()) {
            webViewPage.getMessageChannel().init(view);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        if (shouldOverrideUrlLoadingInternal(view, request.getUrl().toString())) {
            return true;
        }
        return super.shouldOverrideUrlLoading(view, request);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (shouldOverrideUrlLoadingInternal(view, url)) {
            return true;
        }
        return super.shouldOverrideUrlLoading(view, url);
    }

    private boolean shouldOverrideUrlLoadingInternal(WebView view, String url) {
        XLog.d("shouldOverrideUrlLoading:" + url);
        if (webViewPage != null) {
            webViewPage.refreshTitle(url);
        }
        if (url != null && url.startsWith("http")) {
            RouterManager.openUrl(url);
            return true;
        }
        if (url != null && url.startsWith("xscheme://native_page")) {
            //根据前端的href进行scheme拦截，跳转到native的页面
            view.getContext().startActivity(new Intent(view.getContext(), TestActivity.class));
            return true;
        }
        //非http的scheme，唤起对应scheme的app
        if (url != null && !url.startsWith("http")) {
            try {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(uri);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                view.getContext().startActivity(intent);
            } catch (Throwable t) {
                t.printStackTrace();
                Toast.makeText(XApplication.getApplication(), url + "唤起失败", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        //下载apk的scheme，我们调用系统浏览器下载
        if (url != null && url.endsWith(".apk")) {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            view.getContext().startActivity(intent);
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        WebResourceResponse webResourceResponse = interceptRequestInternal(view, url);
        if (webResourceResponse != null) {
            return webResourceResponse;
        }
        webResourceResponse = GlideImgCacheManager.getInstance().interceptRequest(view, url);
        if (webResourceResponse != null) {
            return webResourceResponse;
        }
        webResourceResponse = OkHttpCacheManager.getIntance().interceptRequest(url, null);
        if (webResourceResponse != null) {
            return webResourceResponse;
        }
        return super.shouldInterceptRequest(view, url);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        WebResourceResponse webResourceResponse = interceptRequestInternal(view, request.getUrl().toString());
        if (webResourceResponse != null) {
            return webResourceResponse;
        }
        webResourceResponse = GlideImgCacheManager.getInstance().interceptRequest(view, request.getUrl().toString());
        if (webResourceResponse != null) {
            return webResourceResponse;
        }
        webResourceResponse = OkHttpCacheManager.getIntance().interceptRequest(request.getUrl().toString(), request.getRequestHeaders());
        if (webResourceResponse != null) {
            return webResourceResponse;
        }
        return super.shouldInterceptRequest(view, request);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private WebResourceResponse interceptRequestInternal(WebView view, String url) {
        XLog.d("shouldInterceptRequest:" + url);
        if (url != null && url.equals("https://www.baidu.com/xbridge.js")) {
            XLog.d("start inject js bridge...");
            WebResourceResponse webResourceResponse = null;
            try {
                InputStream inputStream = view.getContext().getAssets().open("xbridge.js");
                webResourceResponse = new WebResourceResponse(MimeTypeMapUtils.getMimeType(url), "UTF-8", inputStream);
                Map<String, String> header = new HashMap<>();
                header.put("Access-Control-Allow-Origin", url);
                webResourceResponse.setResponseHeaders(header);
            } catch (Exception e) {
                e.printStackTrace();
                XLog.e(e.getMessage());
            }
            return webResourceResponse;
        }
        if (url != null && !url.startsWith("http")) {
            boolean success = RouterManager.openScheme(url);
            String page = null;
            if (success) {
                XLog.d("goto scheme :" + url);
                page = "success.html";
            } else {
                page = "fail.html";
            }
            try {
                InputStream inputStream = XApplication.getApplication().getAssets().open(page);
                return new WebResourceResponse("text/html", "UTF-8", inputStream);
            } catch (Throwable t) {
            }
        }
        if (url != null) {
            WebResourceResponse webResourceResponse = null;
            try {
                byte[] contentByte = OfflinePkgManager.getInstance().getOfflineContent(url);
                if (contentByte != null && contentByte.length > 0) {
                    InputStream inputStream = new ByteArrayInputStream(contentByte);
                    webResourceResponse = new WebResourceResponse(MimeTypeMapUtils.getMimeType(url), "UTF-8", inputStream);
                    Map<String, String> header = new HashMap<>();
                    header.put("Access-Control-Allow-Origin", url);
                    webResourceResponse.setResponseHeaders(header);
                }
            } catch (Exception e) {
                e.printStackTrace();
                XLog.e(e.getMessage());
            }
            return webResourceResponse;
        }
        return null;
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        super.onLoadResource(view, url);
        XLog.d("onLoadResource,url = " + url);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        //比如错误的url：https://m.h5.taobao.com/
        try {
            String errorMsg = "onReceivedError";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                errorMsg += ":\n" + error.getDescription().toString() + "\nerrorCode:" + error.getErrorCode();
            }
            XLog.d(errorMsg);
            webViewPage.showErrorPage(errorMsg);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        super.onReceivedHttpError(view, request, errorResponse);
        String errorMsg = "onReceivedHttpError";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            errorMsg += ":\nstatusCode=" + errorResponse.getStatusCode();
        }
        XLog.d(errorMsg);
        webViewPage.showErrorPage(errorMsg);
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        super.onReceivedSslError(view, handler, error);
        String errorMsg = "onReceivedSslError:" + error.getPrimaryError();
        XLog.d(errorMsg);
        webViewPage.showErrorPage(errorMsg);
    }
}
