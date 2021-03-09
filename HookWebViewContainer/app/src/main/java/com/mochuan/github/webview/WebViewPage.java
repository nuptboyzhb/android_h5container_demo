package com.mochuan.github.webview;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mochuan.github.R;
import com.mochuan.github.XApplication;
import com.mochuan.github.activity.BaseActivity;
import com.mochuan.github.log.XLog;
import com.mochuan.github.util.UrlCacheUtils;

/**
 * @version mochuan.zhb on 2020-11-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description webview页面逻辑的抽象封装
 */
public class WebViewPage {

    private Context mContext;

    private TextView titleBar;

    private WebView mWebView;

    private XJSBridgeImpl mXJSBridgeImpl;

    private FrameLayout mContainer;

    private View mErrorView;
    private TextView mErrorText;
    private XMessageChannel messageChannel;

    private View mPopMenuView;

    public WebViewPage(Context context) {
        this.mContext = context;
        initWebView();
    }

    /**
     * 加载url
     *
     * @param url
     */
    public void loadUrl(String url) {
        mWebView.loadUrl(url);
    }


    private void initWebView() {
        mWebView = new XWebView(mContext);
        mWebView.setWebChromeClient(new XWebChromeClient().setWebViewPage(this));
        mWebView.setWebViewClient(new XWebViewClient(this));
        mWebView.getSettings().setJavaScriptEnabled(true);
        mXJSBridgeImpl = new XJSBridgeImpl(mWebView);
        messageChannel = new XMessageChannel();
        testCookie();
    }

    public XMessageChannel getMessageChannel() {
        return messageChannel;
    }

    public void setContainer(FrameLayout mContainer) {
        this.mContainer = mContainer;
    }

    private void testCookie() {
        mWebView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mWebView.getUrl() != null && mWebView.getUrl().startsWith("file://")) {
                    try {
                        CookieManager.getInstance().setCookie(mWebView.getUrl(), "H_PS_PSSID=32818_32617_1464_32790_7545_32705_32231_7516_32116_32718_22158; path=/; domain=30.55.179.246:8000");
                        CookieManager.getInstance().setCookie("test_native_cookie", "H_PS_PSSID=32818; path=/; domain=30.55.179.246:8000");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            CookieManager.getInstance().flush();
                        }
                        XLog.d("write cookie success");
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        }, 1000);
    }


    public WebView getWebView() {
        return mWebView;
    }


    /**
     * 处理js log
     *
     * @param message
     * @return
     */
    public boolean handleMsgFromJS(String message) {
        if (mXJSBridgeImpl.handleLogFromJS(message)) {
            return true;
        }
        return false;
    }

    public void changeH5Background() {
        mXJSBridgeImpl.changeH5Background();
    }

    public void addObjectForJS() {
        mXJSBridgeImpl.addObjectForJS();
    }

    public void refreshPage() {
        mWebView.reload();
    }

    public void refreshTitle(String url) {
        if (titleBar != null) {
            titleBar.setText(url);
        }
    }

    public void setTitleView(TextView titleView) {
        this.titleBar = titleView;
    }

    /**
     * 展示报错信息
     *
     * @param errorMsg
     */
    public void showErrorPage(String errorMsg) {
        if (mErrorView == null) {
            mErrorView = LayoutInflater.from(mContainer.getContext()).inflate(R.layout.webview_error_view, mContainer, false);
            mErrorText = (TextView) mErrorView.findViewById(R.id.error_content);
            mErrorText.setText(errorMsg);
            mErrorView.findViewById(R.id.refresh_page).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    refreshPage();
                }
            });
            mErrorView.findViewById(R.id.close_error_page).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mErrorView.setVisibility(View.GONE);
                }
            });
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            mContainer.addView(mErrorView, layoutParams);
        } else {
            mErrorText.setText(errorMsg);
            mErrorView.setVisibility(View.VISIBLE);
        }
    }

    public void hideErrorPage() {
        if (mErrorView != null) {
            mErrorView.setVisibility(View.GONE);
        }
    }

    public void closePage() {
        if (messageChannel != null) {
            messageChannel.close();
        }
        mWebView.destroy();
    }

    public void showPopMenuView() {
        if (mPopMenuView == null) {
            mPopMenuView = LayoutInflater.from(mContainer.getContext()).inflate(R.layout.webview_pop_menu_view, mContainer, false);
            mPopMenuView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mPopMenuView.setVisibility(View.GONE);
                }
            });
            mPopMenuView.findViewById(R.id.collection).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final String url = mWebView.getUrl();
                    if (!TextUtils.isEmpty(url)) {
                        XApplication.getThreadPool().execute(new Runnable() {
                            @Override
                            public void run() {
                                UrlCacheUtils.saveUrls(url);
                            }
                        });
                        mPopMenuView.setVisibility(View.GONE);
                        Toast.makeText(view.getContext(), "收藏成功", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            mPopMenuView.findViewById(R.id.refresh).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    refreshPage();
                    mPopMenuView.setVisibility(View.GONE);
                }
            });

            mPopMenuView.findViewById(R.id.copy_url).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    UrlCacheUtils.copy(mWebView.getUrl());
                    Toast.makeText(view.getContext(), "复制成功", Toast.LENGTH_SHORT).show();
                    mPopMenuView.setVisibility(View.GONE);
                }
            });

            mPopMenuView.findViewById(R.id.close_pop_menu).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mPopMenuView.setVisibility(View.GONE);
                }
            });

            mPopMenuView.findViewById(R.id.close_page).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    BaseActivity.getCurrentActivity().finish();
                }
            });

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            mContainer.addView(mPopMenuView, layoutParams);
        } else {
            mPopMenuView.setVisibility(View.VISIBLE);
        }
    }
}
