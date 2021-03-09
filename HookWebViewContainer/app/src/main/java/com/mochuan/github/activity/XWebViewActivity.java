package com.mochuan.github.activity;

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.mochuan.github.R;
import com.mochuan.github.log.XLog;
import com.mochuan.github.webview.WebViewPage;

import static android.view.KeyEvent.KEYCODE_BACK;

/**
 * @version mochuan.zhb on 2020-11-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description webview抽象封装页面
 */
public class XWebViewActivity extends BaseActivity {

    private WebViewPage mWebViewPage = null;
    private FrameLayout frameLayout = null;
    private String currentUrl = null;
    private TextView titleView = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Process.setThreadPriority(-20);
        setContentView(R.layout.activity_webview);


        currentUrl = parseUrl(getIntent());
        XLog.d("currentUrl = " + currentUrl);
        frameLayout = (FrameLayout) findViewById(R.id.container);
        mWebViewPage = new WebViewPage(XWebViewActivity.this);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        frameLayout.addView(mWebViewPage.getWebView(), layoutParams);
        mWebViewPage.setContainer(frameLayout);
        if (currentUrl == null) {
            showErrorView();
        } else {
            mWebViewPage.loadUrl(currentUrl);
        }
        titleView = findViewById(R.id.title);
        titleView.setText(currentUrl);
        mWebViewPage.setTitleView(titleView);

        mWebViewPage.addObjectForJS();
        CookieManager.getInstance().acceptCookie();
        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean aBoolean) {
                    XLog.d("removeAllCookies = " + aBoolean);
                }
            });
        }

        findViewById(R.id.menu_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mWebViewPage.showPopMenuView();
            }
        });

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mWebViewPage.getWebView().canGoBack()) {
                    mWebViewPage.getWebView().goBack();
                } else {
                    finish();
                }
            }
        });

        setTaskDesc();
    }


    private void setTaskDesc() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setTaskDescription(new ActivityManager.TaskDescription("H5进程", R.drawable.h5_icon));
        } else {
            Bitmap iconBmp = BitmapFactory.decodeResource(getResources(), R.drawable.h5_icon); // 这里应该是小程序图标的bitmap
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setTaskDescription(new ActivityManager.TaskDescription("H5进程", iconBmp));
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mWebViewPage != null) {
            mWebViewPage.closePage();
        }
        super.onDestroy();
    }

    public WebViewPage getWebViewPage() {
        return mWebViewPage;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String url = parseUrl(intent);
        XLog.d("onNewIntent.url = " + url);
        if (!TextUtils.isEmpty(url) && !currentUrl.equals(url)) {
            mWebViewPage.loadUrl(url);
            this.currentUrl = url;
        }
    }

    private String parseUrl(Intent intent) {
        String scheme = intent.getScheme();
        XLog.d("scheme = " + scheme);
        if (!TextUtils.isEmpty(scheme)) {
            return scheme;
        } else {
            String url = intent.getStringExtra("url");
            XLog.d("url = " + url);
            return url;
        }
    }

    public void showErrorView() {
        mWebViewPage.showErrorPage("url为空");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KEYCODE_BACK) && mWebViewPage.getWebView().canGoBack()) {
            mWebViewPage.getWebView().goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public static class H5Activity1 extends XWebViewActivity {

    }

    public static class H5Activity2 extends XWebViewActivity {

    }

    public static class H5Activity3 extends XWebViewActivity {

    }

    public static class H5Activity4 extends XWebViewActivity {

    }

    public static class H5Activity5 extends XWebViewActivity {

    }

}
