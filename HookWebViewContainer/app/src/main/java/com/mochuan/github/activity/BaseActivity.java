package com.mochuan.github.activity;

import android.app.Activity;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * @version mochuan.zhb on 2020-11-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 基础Activity
 */
public class BaseActivity extends FragmentActivity {

    private static List<Activity> currentActivity = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentActivity.add(this);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        currentActivity.remove(this);
    }

    public static Activity getCurrentActivity() {
        if (currentActivity != null && currentActivity.size() > 0) {
            return currentActivity.get(currentActivity.size() - 1);
        }
        return null;
    }
}
