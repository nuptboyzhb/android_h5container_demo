package com.mochuan.github.bridge.impl;

import android.webkit.WebView;

import com.mochuan.github.XApplication;
import com.mochuan.github.bridge.IXBridge;
import com.mochuan.github.bridge.IXBridgeCallback;
import com.mochuan.github.cache.MyOkHttpCacheInterceptor;
import com.mochuan.github.log.XLog;
import com.mochuan.github.util.XFileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @version mochuan.zhb on 2020-11-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description http的bridge实现：HttpUrlConnection和OkHttp
 */
public class HttpBridge implements IXBridge {

    private OkHttpClient client;

    public HttpBridge() {
        //设置缓存的目录文件
        File httpCacheDirectory = new File(XApplication.getApplication().getCacheDir(), "x-okhttp-req");
        //仅作为日志使用
        if (httpCacheDirectory.exists()) {
            List<File> result = XFileUtils.listFiles(httpCacheDirectory);
            for (File file : result) {
                XLog.d("file = " + file.getAbsolutePath());
            }
        }
        //缓存的大小，OkHttp会使用DiskLruCache缓存
        int cacheSize = 2 * 1024 * 1024; // 2 MiB
        Cache cache = new Cache(httpCacheDirectory, cacheSize);
        //设置缓存时间
        MyOkHttpCacheInterceptor myOkHttpCacheInterceptor = new MyOkHttpCacheInterceptor();
        //缓存2分钟
        myOkHttpCacheInterceptor.setMaxAge(24 * 60, TimeUnit.MINUTES);
        client = new OkHttpClient.Builder()
                .addNetworkInterceptor(myOkHttpCacheInterceptor)
                .cache(cache)
                .build();
    }

    @Override
    public String getFuncName() {
        return "http";
    }

    /**
     * http请求
     *
     * @param mWebView
     * @param param:{url:'https://xxxx',data:{},method:''}
     * @param callback
     */
    @Override
    public void process(WebView mWebView, final String param, final IXBridgeCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject = new JSONObject(param);
                    String url = jsonObject.getString("url");
                    okHttpGet(url, callback);
                    //androidHttpGet(url, callback);
                } catch (Exception e) {
                    if (callback != null) {
                        callback.failed(getFailMsg(e.getMessage()));
                    }
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 创建失败消息
     *
     * @param msg
     * @return
     */
    private JSONObject getFailMsg(String msg) {
        JSONObject failed = new JSONObject();
        try {
            failed.put("msg", msg);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
        return failed;
    }

    /**
     * @param url
     * @param callback
     */
    public void okHttpGet(String url, final IXBridgeCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response result = null;
        try {
            result = client.newCall(request).execute();
            XLog.d("start http with okhttp...");
            if (result.cacheResponse() != null) {
                XLog.e("start http with okhttp...hit cache..." + url);
            }
            if (result.isSuccessful()) {
                XLog.d("request is successful ...");
                String str = result.body().string();
                if (callback != null) {
                    JSONObject resp = new JSONObject(str);
                    callback.success(resp);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            if (callback != null) {
                XLog.d("request is failed ...");
                callback.failed(getFailMsg(e.getMessage()));
            }
        }
    }

    /**
     * Android 原生的的Get请求
     *
     * @param urlString
     * @param callback
     */
    public void androidHttpGet(String urlString, final IXBridgeCallback callback) {
        URL url = null;
        HttpURLConnection urlConnection = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            url = new URL(urlString);

            urlConnection = (HttpURLConnection) url
                    .openConnection();

            InputStream in = urlConnection.getInputStream();

            InputStreamReader isw = new InputStreamReader(in);

            int data = isw.read();
            while (data != -1) {
                char current = (char) data;
                data = isw.read();
                stringBuilder.append(current);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        String respStr = stringBuilder.toString();
        XLog.d("respStr = " + respStr);
        if (respStr.length() > 0) {
            JSONObject resp = null;
            try {
                resp = new JSONObject(respStr);
                if (callback != null) {
                    callback.success(resp);
                    return;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (callback != null) {
            callback.failed(getFailMsg("unknown"));
        }
    }
}
