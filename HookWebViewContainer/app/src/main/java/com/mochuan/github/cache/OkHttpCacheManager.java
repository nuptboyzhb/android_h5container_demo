package com.mochuan.github.cache;

import android.os.Build;
import android.text.TextUtils;
import android.util.LruCache;
import android.webkit.WebResourceResponse;

import com.mochuan.github.XApplication;
import com.mochuan.github.log.XLog;
import com.mochuan.github.util.MimeTypeMapUtils;
import com.mochuan.github.util.SettingUtils;
import com.mochuan.github.util.XFileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @version mochuan.zhb on 2021-01-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 使用OkHttp做资源的缓存:借助Okhttp的缓存策略，对url做缓存
 */
public class OkHttpCacheManager {

    private static final String TAG = "CACHE";

    private static OkHttpCacheManager sOkHttpCacheManager;

    private int allCount = 0;
    private int cacheCount = 0;

    //内存级别的预加载缓存
    private static LruCache<String, byte[]> preLoadCache = new LruCache<>(100);

    //只缓存白名单中的资源
    private static final HashSet CACHE_MIME_TYPE = new HashSet() {
        {
            add("html");
            add("htm");
            add("js");
            add("ico");
            add("css");
            add("png");
            add("jpg");
            add("jpeg");
            add("gif");
            add("bmp");
            add("ttf");
            add("woff");
            add("woff2");
            add("otf");
            add("eot");
            add("svg");
            add("xml");
            add("swf");
            add("txt");
            add("text");
            add("conf");
            add("webp");
        }
    };

    public synchronized static OkHttpCacheManager getIntance() {
        if (sOkHttpCacheManager == null) {
            sOkHttpCacheManager = new OkHttpCacheManager();
        }
        return sOkHttpCacheManager;
    }

    private OkHttpClient mHttpClient;

    private OkHttpCacheManager() {
        //设置缓存的目录文件
        File httpCacheDirectory = new File(XApplication.getApplication().getExternalCacheDir(), "x-webview-http-cache");
        //仅作为日志使用
        if (httpCacheDirectory.exists()) {
            List<File> result = XFileUtils.listFiles(httpCacheDirectory);
            for (File file : result) {
                XLog.d(TAG, "file = " + file.getAbsolutePath());
            }
        }
        //缓存的大小，OkHttp会使用DiskLruCache缓存
        int cacheSize = 20 * 1024 * 1024; // 20 MiB
        Cache cache = new Cache(httpCacheDirectory, cacheSize);
        //设置缓存
        mHttpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(new MyOkHttpCacheInterceptor())
                .cache(cache)
                .build();
    }


    /**
     * 预加载资源
     *
     * @param urls
     */
    public void preLoadResource(final List<String> urls) {
        XApplication.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                for (String url : urls) {
                    try {
                        XLog.e(TAG, "start load res:" + url);
                        Request.Builder reqBuilder = new Request.Builder()
                                .url(url);
                        Request request = reqBuilder.get().build();
                        Response response = mHttpClient.newCall(request).execute();
                        if (response.code() == 200) {
                            XLog.e(TAG, "res preload success..." + url);
                            //保存下载的资源
                            preLoadCache.put(url, response.body().bytes());
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * 针对url级别的缓存，包括主文档，图片，js，css等
     *
     * @param url
     * @param headers
     * @return
     */
    public WebResourceResponse interceptRequest(String url, Map<String, String> headers) {
        try {
            if (!SettingUtils.isEnabledOkhttpCache()) {
                return null;
            }
            String extension = MimeTypeMapUtils.getFileExtensionFromUrl(url);
            if (TextUtils.isEmpty(extension) || !CACHE_MIME_TYPE.contains(extension.toLowerCase())) {
                //不在支持的缓存范围内
                XLog.w(TAG + "+" + url + " 's extension is " + extension + "!!not support...");
                return null;
            }
            //预加载
            if (preLoadCache.get(url) != null) {
                byte[] contentByte = preLoadCache.get(url);
                InputStream inputStream = new ByteArrayInputStream(contentByte);
                WebResourceResponse webResourceResponse = new WebResourceResponse(MimeTypeMapUtils.getMimeType(url), "UTF-8", inputStream);
                XLog.e(TAG, "hit preload cache.url = " + url);
                return webResourceResponse;
            }
            long startTime = System.currentTimeMillis();
            Request.Builder reqBuilder = new Request.Builder()
                    .url(url);
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    XLog.d(TAG, String.format("header:(%s=%s)", entry.getKey(), entry.getValue()));
                    reqBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }

            Request request = reqBuilder.get().build();
            Response response = mHttpClient.newCall(request).execute();
            if (response.code() != 200) {
                XLog.e(TAG, "response code = " + response.code() + ",extension = " + extension);
                return null;
            }
            String mimeType = MimeTypeMapUtils.getMimeTypeFromUrl(url);
            XLog.d(TAG, "mimeType = " + mimeType + ",extension = " + extension + ",url = " + url);
            WebResourceResponse okHttpWebResourceResponse = new WebResourceResponse(mimeType, "", response.body().byteStream());
            Response cacheRes = response.cacheResponse();
            long endTime = System.currentTimeMillis();
            long costTime = endTime - startTime;
            allCount++;
            if (cacheRes != null) {
                cacheCount++;
                XLog.e(TAG, String.format("count rate = (%s),costTime = (%s);from cache: %s", (1.0f * cacheCount / allCount), costTime, url));
            } else {
                XLog.e(TAG, String.format("costTime = (%s);from server: %s", costTime, url));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String message = response.message();
                if (TextUtils.isEmpty(message)) {
                    message = "OK";
                }
                try {
                    okHttpWebResourceResponse.setStatusCodeAndReasonPhrase(response.code(), message);
                } catch (Exception e) {
                    return null;
                }
                Map<String, String> header = MimeTypeMapUtils.multimapToSingle(response.headers().toMultimap());
                okHttpWebResourceResponse.setResponseHeaders(header);
            }
            return okHttpWebResourceResponse;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }


}
