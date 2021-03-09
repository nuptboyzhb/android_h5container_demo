package com.mochuan.github.webview;

import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.mochuan.github.bridge.BridgeManager;
import com.mochuan.github.bridge.IXBridgeCallback;
import com.mochuan.github.log.XLog;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @version mochuan.zhb on 2020-11-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description JSBridge的封装协议部分，和assets中的js实现对应
 */
public class XJSBridgeImpl {

    private WebView mWebView;

    public XJSBridgeImpl(WebView view) {
        mWebView = view;
    }

    private static String XJSBRIDGE_HEADER = "____xbridge____:";

    public boolean handleLogFromJS(String log) {
        if (log != null && log.startsWith(XJSBRIDGE_HEADER)) {
            String msg = log.substring(XJSBRIDGE_HEADER.length());
            XLog.d("msg:" + msg);
            return handleMsgFromJS(msg);
        }
        return false;
    }

    public boolean handleMsgFromJS(String message) {
        try {
            JSONObject jsonObject = new JSONObject(message);
            String func = jsonObject.getString("func");
            String seqId = jsonObject.getString("seqId");
            String param = jsonObject.getString("param");
            dispatch(func, seqId, param);
            return true;
        } catch (JSONException e) {
            XLog.e(e);
        }
        return false;
    }

    private boolean dispatch(String func, final String seqId, String param) {
        //调用JSBridge进行分发处理
        BridgeManager.getInstance().process(mWebView, func, param, new IXBridgeCallback() {
            @Override
            public void success(JSONObject resp) {
                if (resp == null) {
                    resp = new JSONObject();
                }
                try {
                    resp.put("success", true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                invokeJS(seqId, resp);
            }

            @Override
            public void failed(JSONObject fail) {
                if (fail == null) {
                    fail = new JSONObject();
                }
                try {
                    fail.put("success", false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                invokeJS(seqId, fail);
            }
        });
        return false;
    }


    private void invokeJS(String seqId, JSONObject res) {
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("seqId", seqId);
            jsonObject.put("msgType", "jsCallback");
            jsonObject.put("param", res);
        } catch (JSONException e) {
            XLog.e(e);
        }
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                //需要在主线程调用
                mWebView.evaluateJavascript(String.format("javascript: window.XJSBridge.nativeInvokeJS('%s')", jsonObject.toString()), new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        XLog.d("onReceiveValue:s = " + s);
                    }
                });
            }
        });
    }

    public void changeH5Background() {
        mWebView.evaluateJavascript(String.format("javascript: changeColor('%s')", "#f00"), new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {
                XLog.d("changeH5Background:onReceiveValue:s = " + s);
            }
        });
    }

    public void addObjectForJS() {
        mWebView.addJavascriptInterface(new NativeLog(), "nativeLog");
    }
}
