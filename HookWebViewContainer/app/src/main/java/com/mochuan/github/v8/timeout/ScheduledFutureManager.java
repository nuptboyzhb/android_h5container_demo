package com.mochuan.github.v8.timeout;


import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;

/**
 * @version mochuan.zhb on 2020-12-30
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description ScheduledFuture的管理
 */
public class ScheduledFutureManager {

    public static Map<Integer, ScheduledFuture> timeoutScheduledFutureMap = new HashMap<>();
    public static Map<Integer, ScheduledFuture> intervalScheduledFutureMap = new HashMap<>();


    /**
     * 添加ScheduledFuture
     *
     * @param scheduledFuture
     * @return
     */
    public static int addTimeoutTaskInfo(ScheduledFuture scheduledFuture) {
        int taskId = new Random().nextInt(9999);
        timeoutScheduledFutureMap.put(taskId, scheduledFuture);
        return taskId;
    }

    /**
     * 删除ScheduledFuture
     *
     * @param taskId
     * @return
     */
    public static ScheduledFuture removeTimeoutTaskInfo(int taskId) {
        return timeoutScheduledFutureMap.remove(taskId);
    }

    /**
     * 添加ScheduledFuture
     *
     * @param scheduledFuture
     * @return
     */
    public static int addIntervalTaskInfo(ScheduledFuture scheduledFuture) {
        int taskId = new Random().nextInt(9999);
        intervalScheduledFutureMap.put(taskId, scheduledFuture);
        return taskId;
    }

    /**
     * 删除ScheduledFuture
     *
     * @param taskId
     * @return
     */
    public static ScheduledFuture removeIntervalTaskInfo(int taskId) {
        return intervalScheduledFutureMap.remove(taskId);
    }
}
