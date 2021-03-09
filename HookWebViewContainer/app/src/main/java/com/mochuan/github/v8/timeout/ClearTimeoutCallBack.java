package com.mochuan.github.v8.timeout;

import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.mochuan.github.log.XLog;
import com.mochuan.github.v8.XV8Manager;

import java.util.concurrent.ScheduledFuture;

/**
 * @version mochuan.zhb on 2020-12-30
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description JS的clearTimeout桥接实现
 */
public class ClearTimeoutCallBack implements JavaVoidCallback {

    private XV8Manager mXV8Manager;

    public ClearTimeoutCallBack(XV8Manager xv8Manager) {
        this.mXV8Manager = xv8Manager;
    }

    @Override
    public void invoke(V8Object receiver, V8Array parameters) {
        XLog.d("start clear setTimeout");
        if (parameters.length() == 2) {
            final Object taskId = parameters.get(0);
            if (!(taskId instanceof Integer)) {
                return;
            }
            Integer taskIdValue = (Integer) taskId;
            ScheduledFuture scheduledFuture = ScheduledFutureManager.removeTimeoutTaskInfo(taskIdValue);
            if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
                XLog.d("clear setTimeout to cancel task ...");
                scheduledFuture.cancel(true);
            }
        }
    }
}
