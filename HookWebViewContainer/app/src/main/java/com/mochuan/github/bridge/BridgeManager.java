package com.mochuan.github.bridge;

import android.webkit.WebView;

import com.mochuan.github.bridge.impl.HelloBridge;
import com.mochuan.github.bridge.impl.HttpBridge;
import com.mochuan.github.bridge.impl.OpenUrlBridge;
import com.mochuan.github.bridge.impl.PostMsgToWebViewBridge;
import com.mochuan.github.bridge.impl.ShowCookieBridge;
import com.mochuan.github.log.XLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @version mochuan.zhb on 2020-11-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description bridge的统一管理类，负责JSBridge的保持和分发
 */
public class BridgeManager {

    private static BridgeManager mBridgeManager = null;

    private static Map<String, IXBridge> sXBridgeMap = new HashMap<>();

    private BridgeManager() {
        this.addBridge(new HttpBridge());
        this.addBridge(new HelloBridge());
        this.addBridge(new ShowCookieBridge());
        this.addBridge(new OpenUrlBridge());
        this.addBridge(new PostMsgToWebViewBridge());
    }

    public static BridgeManager getInstance() {
        if (mBridgeManager == null) {
            mBridgeManager = new BridgeManager();
        }
        return mBridgeManager;
    }

    public void addBridge(IXBridge bridge) {
        if (bridge != null) {
            sXBridgeMap.put(bridge.getFuncName(), bridge);
        }
    }

    /**
     * 处理JSBridge的消息
     *
     * @param func
     * @param param
     * @param callback
     */
    public void process(WebView mWebview, String func, String param, IXBridgeCallback callback) {
        IXBridge bridge = sXBridgeMap.get(func);
        if (bridge != null) {
            bridge.process(mWebview, param, callback);
            return;
        }
        XLog.d("no bridge found with name = " + func);
        //无法处理的消息
        if (callback != null) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("msg", "no bridge found with name = " + func);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            callback.failed(jsonObject);
        }
    }


}
