package com.mochuan.github.v8;

import com.mochuan.github.bridge.BridgeManager;
import com.mochuan.github.bridge.IXBridgeCallback;
import com.mochuan.github.log.XLog;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @version mochuan.zhb on 2020-12-30
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description V8BridgeImpl 主要做消息协议的解析和封装
 */
public class V8BridgeImpl {

    XV8Manager mXV8Manager;

    public V8BridgeImpl(XV8Manager manager) {
        mXV8Manager = manager;
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
        BridgeManager.getInstance().process(null, func, param, new IXBridgeCallback() {
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
        mXV8Manager.sendToV8Runtime(jsonObject.toString());
    }
}
