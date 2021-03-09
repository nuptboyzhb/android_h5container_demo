package com.mochuan.github.bridge;

import org.json.JSONObject;

/**
 * @version mochuan.zhb on 2020-11-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description JSBridge的回调接口
 */
public interface IXBridgeCallback {

    void success(JSONObject resp);

    void failed(JSONObject fail);
}
