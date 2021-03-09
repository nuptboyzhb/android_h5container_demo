package com.mochuan.github.cache;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.mochuan.github.log.XLog;
import com.mochuan.github.util.MimeTypeMapUtils;
import com.mochuan.github.util.SettingUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashSet;

/**
 * @version mochuan.zhb on 2021-01-29
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 使用Glide对WebView的图片进行缓存
 */
public class GlideImgCacheManager {

    private static final String TAG = "GlideCache";

    private static GlideImgCacheManager sGlideImgCacheManager = null;

    //只缓存白名单中的资源
    private static final HashSet CACHE_IMG_TYPE = new HashSet() {
        {
            add("png");
            add("jpg");
            add("jpeg");
            add("bmp");
            add("webp");
        }
    };

    public synchronized static GlideImgCacheManager getInstance() {
        if (sGlideImgCacheManager == null) {
            sGlideImgCacheManager = new GlideImgCacheManager();
        }
        return sGlideImgCacheManager;
    }

    /**
     * 拦截资源
     *
     * @param url
     * @return
     */
    public WebResourceResponse interceptRequest(WebView webView, String url) {
        try {
            if (!SettingUtils.isEnabledGlideCache()) {
                return null;
            }
            String extension = MimeTypeMapUtils.getFileExtensionFromUrl(url);
            if (TextUtils.isEmpty(extension) || !CACHE_IMG_TYPE.contains(extension.toLowerCase())) {
                //不在支持的缓存范围内
                return null;
            }
            XLog.d(TAG, String.format("start glide cache img (%s),url:%s", extension, url));
            long startTime = System.currentTimeMillis();
            //String mimeType = MimeTypeMapUtils.getMimeTypeFromUrl(url);
            InputStream inputStream = null;
            Bitmap bitmap = Glide.with(webView).asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL).load(url).submit().get();
            inputStream = getBitmapInputStream(bitmap, Bitmap.CompressFormat.JPEG);
            long costTime = System.currentTimeMillis() - startTime;
            if (inputStream != null) {
                XLog.d(TAG, String.format("glide cache img(%s ms): %s", costTime, url));
                WebResourceResponse webResourceResponse = new WebResourceResponse("image/jpg", "UTF-8", inputStream);
                return webResourceResponse;
            } else {
                XLog.e(TAG, String.format("glide cache error.(%s ms): %s", costTime, url));
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    /**
     * 将bitmap进行压缩转换成InputStream
     *
     * @param bitmap
     * @param compressFormat
     * @return
     */
    private InputStream getBitmapInputStream(Bitmap bitmap, Bitmap.CompressFormat compressFormat) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(compressFormat, 80, byteArrayOutputStream);
            byte[] data = byteArrayOutputStream.toByteArray();
            return new ByteArrayInputStream(data);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }
}
