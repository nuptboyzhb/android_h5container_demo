package com.mochuan.github.webview;

import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.mochuan.github.log.XLog;


public class XWebChromeClient extends WebChromeClient {

    private WebViewPage mWebViewPage;

    public XWebChromeClient setWebViewPage(WebViewPage mWebViewPage) {
        this.mWebViewPage = mWebViewPage;
        return this;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {

        if (ConsoleMessage.MessageLevel.LOG.equals(consoleMessage.messageLevel())) {
            XLog.d("onConsoleMessage:" + consoleMessage.message()
                    + ",messageLevel = " + consoleMessage.messageLevel()
                    + ",line = " + consoleMessage.lineNumber()
                    + ",sourceId = " + consoleMessage.sourceId()
            );
            //log级别的日志
            if (mWebViewPage.handleMsgFromJS(consoleMessage.message())) {
                return true;
            }
        } else if (ConsoleMessage.MessageLevel.WARNING.equals(consoleMessage.messageLevel())) {
            XLog.w("onConsoleMessage:" + consoleMessage.message()
                    + ",messageLevel = " + consoleMessage.messageLevel()
                    + ",line = " + consoleMessage.lineNumber()
                    + ",sourceId = " + consoleMessage.sourceId()
            );
        } else if (ConsoleMessage.MessageLevel.ERROR.equals(consoleMessage.messageLevel())) {
            //ERROR级别的日志
            XLog.e("onConsoleMessage:" + consoleMessage.message()
                    + ",messageLevel = " + consoleMessage.messageLevel()
                    + ",line = " + consoleMessage.lineNumber()
                    + ",sourceId = " + consoleMessage.sourceId()
            );
        } else {
            XLog.i("onConsoleMessage:" + consoleMessage.message()
                    + ",messageLevel = " + consoleMessage.messageLevel()
                    + ",line = " + consoleMessage.lineNumber()
                    + ",sourceId = " + consoleMessage.sourceId()
            );
        }
        return super.onConsoleMessage(consoleMessage);
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        super.onReceivedTitle(view, title);
        //接收H5中设置title
        XLog.d("onReceivedTitle:" + title);
    }
}
