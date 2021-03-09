package com.mochuan.github.v8;

import android.os.Handler;
import android.os.Looper;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import com.mochuan.github.XApplication;
import com.mochuan.github.util.XFileUtils;
import com.mochuan.github.v8.timeout.ClearTimeoutCallBack;
import com.mochuan.github.v8.timeout.SetTimeoutCallBack;

/**
 * @version mochuan.zhb on 2020-12-30
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description v8 manager
 */
public class XV8Manager {

    private V8 v8runtime = null;
    private V8Object mXJSBridge;
    private V8Function mNativeInvokeJS;
    private Handler mUIHandler;

    public void init() {
        //创建uiHandler
        mUIHandler = new Handler(Looper.getMainLooper());
        //创建V8示例
        v8runtime = V8.createV8Runtime();
        //桥接日志
        registerLogMethod();
        //向v8中注入对象Java方法
        v8runtime.registerJavaMethod(new XV8JsBridgeCallback(this), "__xBridge_js_func__");
        registerMethods();
        //从assets从获取JSBridge的内容
        String bridge4v8 = getV8BridgeContent();
        //执行自定义的JSBridge代码
        v8runtime.executeVoidScript(bridge4v8);
        //获取JSBridge对象
        mXJSBridge = v8runtime.getObject("XJSBridge");
        //获取调用JS的方法
        mNativeInvokeJS = (V8Function) mXJSBridge.getObject("nativeInvokeJS");
        testTimeout();
    }

    /**
     * 桥接v8中的console.log
     */
    private void registerLogMethod() {
        AndroidConsole androidConsole = new AndroidConsole();
        V8Object v8Object = new V8Object(v8runtime);
        v8runtime.add("console", v8Object);
        //params1：对象
        //params2：java方法名
        //params3：js里面写的方法名
        //params4：方法的参数类型 个数
        v8Object.registerJavaMethod(androidConsole, "log", "log", new Class<?>[]{String.class});
        v8Object.registerJavaMethod(androidConsole, "logObj", "logObj", new Class<?>[]{V8Object.class});
        v8Object.registerJavaMethod(androidConsole, "error", "error", new Class<?>[]{String.class});
        //在js中调用 `console.log('test')`
        v8runtime.executeScript("console.log('test');");
        v8Object.close();
    }

    /**
     * 添加对setTimeout、clearTimeout的java实现
     */
    private void registerMethods() {
        v8runtime.registerJavaMethod(new SetTimeoutCallBack(this), "setTimeout");
        v8runtime.registerJavaMethod(new ClearTimeoutCallBack(this), "clearTimeout");
    }

    private void testTimeout(){
        v8runtime.executeIntegerScript("setTimeout(()=>{console.log(\"from timeout\");},1000);");
    }

    public void executeJSContent(String jsContent) {
        v8runtime.executeVoidScript(jsContent);
    }

    private String getV8BridgeContent() {
        return XFileUtils.readFromAssets(XApplication.getApplication(), "xbridge4v8.js");
    }

    /**
     * 将消息传给v8
     *
     * @param msg
     */
    public void sendToV8Runtime(final String msg) {
        //XLog.d("sendToV8Runtime,msg:" + msg);
        if (isUiThread()) {
            sendToV8RuntimeInUiThread(msg);
        } else {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    sendToV8RuntimeInUiThread(msg);
                }
            });
        }
    }

    /**
     * 必须在主进程进行操作
     *
     * @param msg
     */
    private void sendToV8RuntimeInUiThread(String msg) {
        V8Array args = new V8Array(v8runtime);
        args.push(msg);
        mNativeInvokeJS.call(mXJSBridge, args);
        args.close();
    }

    public void release() {
        if (mNativeInvokeJS != null && !mNativeInvokeJS.isReleased()) {
            mNativeInvokeJS.close();
        }
        if (mXJSBridge != null && !mNativeInvokeJS.isReleased()) {
            mNativeInvokeJS.close();
        }
        if (v8runtime != null) {
            v8runtime.release(true);
        }
    }

    public Handler getHandler() {
        return mUIHandler;
    }

    public static boolean isUiThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }
}
