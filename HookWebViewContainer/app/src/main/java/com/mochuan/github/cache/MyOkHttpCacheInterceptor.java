package com.mochuan.github.cache;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * @version mochuan.zhb on 2021-01-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description MyOkHttpCacheInterceptor
 */
public class MyOkHttpCacheInterceptor implements Interceptor {

    private int maxAga = 365;//default
    private TimeUnit timeUnit = TimeUnit.DAYS;

    public void setMaxAge(int maxAga, TimeUnit timeUnit) {
        this.maxAga = maxAga;
        this.timeUnit = timeUnit;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());

        CacheControl cacheControl = new CacheControl.Builder()
                .maxAge(maxAga, timeUnit)
                .build();

        return response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", cacheControl.toString())
                .build();
    }
}
