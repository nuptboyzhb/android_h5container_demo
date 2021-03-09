package com.mochuan.github.webview;


import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebMessage;
import android.webkit.WebMessagePort;
import android.webkit.WebView;

import com.mochuan.github.log.XLog;

/**
 * @version mochuan.zhb on 2021-01-08
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description WebView的MessageChannel
 */
public class XMessageChannel {

    private static final String TAG = "XMessageChannel";

    private WebMessagePort nativePort = null;
    private WebMessagePort h5Port = null;

    @TargetApi(Build.VERSION_CODES.M)
    public void init(WebView webView) {
        try {
            final WebMessagePort[] channel = webView.createWebMessageChannel();

            //供native使用的port
            nativePort = channel[0];
            //供h5使用的port
            h5Port = channel[1];
            //监听从h5Port中发送过来的消息
            nativePort.setWebMessageCallback(new WebMessagePort.WebMessageCallback() {
                @Override
                public void onMessage(WebMessagePort port, WebMessage message) {
                    XLog.d(TAG, ":onMessage:" + message.getData());
                    postMessageToH5("hello from native:" + this.getClass().getName());
                }
            });
            //发送webmessage，把h5Port发送给H5页面
            XLog.d(TAG, "start postWebMessage to transfer port");
            WebMessage webMessage = new WebMessage("__init_port__", new WebMessagePort[]{h5Port});
            webView.postWebMessage(webMessage, Uri.EMPTY);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    /**
     * 通过port，向H5发送webMessage
     *
     * @param msg
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void postMessageToH5(String msg) {
        if (nativePort != null) {
            nativePort.postMessage(new WebMessage(msg));
        }
    }

    public void close() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (nativePort != null) {
                nativePort.close();
            }
        }
    }
}
