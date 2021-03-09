package com.mochuan.github.activity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.mochuan.github.R;
import com.mochuan.github.XApplication;
import com.mochuan.github.util.SettingUtils;
import com.mochuan.github.util.XFileUtils;

/**
 * @version mochuan.zhb on 2020-11-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 设置Activity
 */
public class SettingActivity extends BaseActivity {

    private Handler mHandler;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        mHandler = new Handler(Looper.getMainLooper());
        final CheckBox glideCache = findViewById(R.id.cb_glide_cache);
        final CheckBox okHttpCache = findViewById(R.id.cb_okhttp_url_cache);
        final CheckBox msgChannel = findViewById(R.id.cb_msg_channel);

        glideCache.setChecked(SettingUtils.getSettingInCache(SettingUtils.KEY_GLIDE));
        okHttpCache.setChecked(SettingUtils.getSettingInCache(SettingUtils.KEY_OK_HTTP));
        msgChannel.setChecked(SettingUtils.getSettingInCache(SettingUtils.KEY_MSG_C));

        glideCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingUtils.saveSettingInCache(SettingUtils.KEY_GLIDE, glideCache.isChecked());
            }
        });

        okHttpCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingUtils.saveSettingInCache(SettingUtils.KEY_OK_HTTP, okHttpCache.isChecked());
            }
        });

        msgChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingUtils.saveSettingInCache(SettingUtils.KEY_MSG_C, msgChannel.isChecked());
            }
        });

        findViewById(R.id.tv_delete_cache).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteCache();
            }
        });

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private boolean isDeleting = false;

    private void deleteCache() {
        if (isDeleting) {
            return;
        }
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.show();
        XApplication.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                count += XFileUtils.deleteFiles(XApplication.getApplication().getCacheDir());
                count += XFileUtils.deleteFiles(XApplication.getApplication().getExternalCacheDir());
                Glide.get(XApplication.getApplication()).clearDiskCache();
                final int deleteCount = count;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.dismiss();
                        mProgressDialog.cancel();
                        mProgressDialog = null;
                        Toast.makeText(XApplication.getApplication(), "清空完毕(" + deleteCount + ")个文件", Toast.LENGTH_SHORT).show();
                    }
                });
                isDeleting = false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
