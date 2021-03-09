package com.mochuan.github.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mochuan.github.R;
import com.mochuan.github.XApplication;
import com.mochuan.github.adapter.XRecyclerViewAdapter;
import com.mochuan.github.h5pkg.OfflinePkgManager;
import com.mochuan.github.h5pkg.PkgDescModel;
import com.mochuan.github.util.RouterManager;
import com.mochuan.github.util.UrlCacheUtils;

import java.util.Map;
import java.util.Set;


/**
 * @version mochuan.zhb on 2020-11-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 主Activity
 */
public class MainActivity extends BaseActivity {

    private EditText mUrlEditText;
    private RecyclerView mRecyclerView;
    private XRecyclerViewAdapter mXRecyclerViewAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private Handler mHandler = null;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RouterManager.init(this);
        setContentView(R.layout.activity_main);

        mHandler = new Handler(Looper.getMainLooper());
        mUrlEditText = findViewById(R.id.url_edit);

        findViewById(R.id.open_url_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = mUrlEditText.getEditableText().toString();
                saveUrlInSpCache(url);
                openUrl(url);
                mXRecyclerViewAdapter.addUrl(url);
            }
        });

        findViewById(R.id.settings_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RouterManager.openActivity(SettingActivity.class.getName(), "settings");
            }
        });


        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mXRecyclerViewAdapter = new XRecyclerViewAdapter();
        mRecyclerView.setAdapter(mXRecyclerViewAdapter);

        mXRecyclerViewAdapter.addUrl("file:///android_asset/xbridge_demo.html");

        mXRecyclerViewAdapter.addUrl("https://www.baidu.com");
        mXRecyclerViewAdapter.addUrl("https://www.youtube.com");
        mXRecyclerViewAdapter.addUrl("https://www.zhihu.com");

        String cacheUrl = getUrlCacheFromSp();
        if (!TextUtils.isEmpty(cacheUrl)) {
            mUrlEditText.setText(cacheUrl);
        }


        XApplication.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    OfflinePkgManager.zipCountDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Map<String, PkgDescModel> offlineUrls = OfflinePkgManager.getInstance().getvHostUrlInfoMap();
                        mXRecyclerViewAdapter.addUrl(0, offlineUrls.keySet());
                    }
                });
            }
        });

        findViewById(R.id.open_v8_demo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = "file:///android_asset/v8_demo.html";
                RouterManager.openActivity(V8DemoActivity.class.getName(), url);
            }
        });

    }

    private void saveUrlInSpCache(String url) {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("_open_url", url);
        editor.apply();
    }

    private String getUrlCacheFromSp() {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("data", Context.MODE_PRIVATE);
        String url = sharedPreferences.getString("_open_url", "");
        return url;
    }

    private void openUrl(String url) {
//        if (!TextUtils.isEmpty(url)) {
//            Intent intent = new Intent(MainActivity.this, XWebViewActivity.class);
//            intent.putExtra("url", url.trim());
//            startActivity(intent);
//        } else {
//            Toast.makeText(getApplicationContext(), "url为null", Toast.LENGTH_SHORT).show();
//        }
        RouterManager.openH5Activity(url);
    }

    @Override
    protected void onResume() {
        super.onResume();
        XApplication.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                final Set<String> urls = UrlCacheUtils.getUrls();
                if (urls != null && urls.size() > 0) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mXRecyclerViewAdapter.addUrl(urls);
                        }
                    });
                }
            }
        });
    }
}
