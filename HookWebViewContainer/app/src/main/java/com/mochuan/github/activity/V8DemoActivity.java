package com.mochuan.github.activity;

import android.os.Bundle;

import com.mochuan.github.util.XFileUtils;
import com.mochuan.github.v8.XV8Manager;

/**
 * @version mochuan.zhb on 2020-12-30
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description v8demo
 */
public class V8DemoActivity extends XWebViewActivity {

    private XV8Manager mXV8Manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mXV8Manager = new XV8Manager();
        mXV8Manager.init();
        String v8demo = XFileUtils.readFromAssets(getApplicationContext(), "v8demo.js");
        mXV8Manager.executeJSContent(v8demo);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mXV8Manager.release();
    }

    public XV8Manager getV8Manager() {
        return mXV8Manager;
    }
}
