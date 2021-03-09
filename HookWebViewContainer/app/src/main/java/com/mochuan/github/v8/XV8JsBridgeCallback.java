package com.mochuan.github.v8;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.mochuan.github.log.XLog;

/**
 * @version mochuan.zhb on 2020-12-30
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description XV8JsBridgeCallback
 */
public class XV8JsBridgeCallback implements JavaVoidCallback {

    XV8Manager mXV8Manager;

    public XV8JsBridgeCallback(XV8Manager manager) {
        this.mXV8Manager = manager;
    }

    @Override
    public void invoke(V8Object receiver, V8Array params) {
        //获取JS中传递过来的参数
        JSONArray jsonArray = XV8Utils.toJSONArray(params);
        if (jsonArray != null) {
            XLog.d("V8ArrayCallBack:" + jsonArray.toJSONString());
        }
        String string = (String)jsonArray.get(0);
        //获取消息，然后处理消息
        new V8BridgeImpl(mXV8Manager).handleMsgFromJS(string);
        params.close();
        receiver.close();
    }
}
