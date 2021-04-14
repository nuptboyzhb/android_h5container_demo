# Android H5容器整理

### 1.如何实现和设计一套JSBridge？


前端JS调用native的方式有很多种，或者说android有很多种方式可以拦截或者获取到JS的行为。如下使用onConsoleMessage的方式，来设计一个简单的JSBridge：


- 前端代码片段


```js
(function () {
  var callbackArr = {};
  window.XJSBridge = {
    //JS调动native
    callNative: function (func, param, callback) {
      //生成调用的序列号Id
      var seqId = "__bridge_id_" + Math.random() + "" + new Date().getTime();
      //保存回调
      callbackArr[seqId] = callback;
      //生成约定的JSBridge消息
      var msgObj = {
        func: func,
        param: param,
        msgType: "callNative",
        seqId: seqId,
      };
      //打印约定的日志
      console.log("____xbridge____:" + JSON.stringify(msgObj));
    },
    //native调用JS的方法
    nativeInvokeJS: function (res) {
      //解析native的消息
      res = JSON.parse(res);
      //判断是否是callback消息
      if (res && res.msgType === "jsCallback") {
        //获取之前保持的回调方法
        var func = callbackArr[res.seqId];
        //delete callbackArr[res.seqId];
        //调用JS的回调
        if ("function" === typeof func) {
          setTimeout(function () {
            //调用js的回调
            func(res.param);
          }, 1);
        }
      }
      return true;
    },
  };
  document.dispatchEvent(new Event("xbridge inject success..."), null);
  console.log("xbridge inject success...");
})();

```


如上代码就是在window对象上挂载一个XJSBridge对象，可以通过callNative函数来调用android的native方法，其原理就是JS调用console.log打印一行约定的日志(Bridge协议)，android端通过WebChromeClient的onConsoleMessage方法，获取到打印的日志，然后针对协议解析出对应的协议。


- 其中WebChromeClient的代码如下



```java

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
                    + ",line = " + consoleMessage.lineNumber()
                    + ",sourceId = " + consoleMessage.sourceId()
                    + ",messageLevel = " + consoleMessage.messageLevel());
            //log级别的日志
            if (mWebViewPage.handleMsgFromJS(consoleMessage.message())) {
                return true;
            }
        } else if (ConsoleMessage.MessageLevel.ERROR.equals(consoleMessage.messageLevel())) {
            //ERROR级别的日志
            XLog.e("onConsoleMessage:" + consoleMessage.message()
                    + ",line = " + consoleMessage.lineNumber()
                    + ",sourceId = " + consoleMessage.sourceId()
                    + ",messageLevel = " + consoleMessage.messageLevel());
        } else {
            XLog.w("onConsoleMessage:" + consoleMessage.message()
                    + ",line = " + consoleMessage.lineNumber()
                    + ",sourceId = " + consoleMessage.sourceId()
                    + ",messageLevel = " + consoleMessage.messageLevel());
        }
        return super.onConsoleMessage(consoleMessage);
    }
}
```


这里的mWebViewPage会调用到XJSBridgeImpl的handleLogFromJS方法，在这里针对JSBridge进行解析，比如：


```java

public class XJSBridgeImpl {

    private WebView mWebView;

    public XJSBridgeImpl(WebView view) {
        mWebView = view;
    }

    private static String XJSBRIDGE_HEADER = "____xbridge____:";

    public boolean handleLogFromJS(String log) {
        if (log != null && log.startsWith(XJSBRIDGE_HEADER)) {
            String msg = log.substring(XJSBRIDGE_HEADER.length());
            XLog.d("msg:" + msg);
            return handleMsgFromJS(msg);
        }
        return false;
    }

    private boolean dispatch(String func, final String seqId, String param) {

        if (func.equals("nativeMethod")) {
            //模拟js bridge的native实现
            Toast.makeText(mWebView.getContext(), "Hello XJSBridge!I am in native.", Toast.LENGTH_SHORT).show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    XLog.d("hello, I am native method...");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    JSONObject mockRes = new JSONObject();
                    try {
                        mockRes.put("bridge", XJSBridgeImpl.class.getName());
                        mockRes.put("data", "I am from native");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    invokeJS(seqId, mockRes);
                }
            }).start();
        }
        return false;
    }


    private void invokeJS(String seqId, JSONObject res) {
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("seqId", seqId);
            jsonObject.put("msgType", "jsCallback");
            jsonObject.put("param", res);
        } catch (JSONException e) {
            XLog.e(e);
        }
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                //需要在主线程调用
                mWebView.evaluateJavascript(String.format("javascript: window.XJSBridge.nativeInvokeJS('%s')", jsonObject.toString()), new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        XLog.d("onReceiveValue:s = " + s);
                    }
                });
            }
        });
    }


    private boolean handleMsgFromJS(String message) {
        try {
            JSONObject jsonObject = new JSONObject(message);
            String func = jsonObject.getString("func");
            String seqId = jsonObject.getString("seqId");
            String param = jsonObject.getString("param");
            dispatch(func, seqId, param);
            return true;
        } catch (JSONException e) {
            XLog.e(e);
        }
        return false;
    }

    public void changeH5Background() {
        mWebView.evaluateJavascript(String.format("javascript: changeColor('%s')", "#f00"), new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {
                XLog.d("changeH5Background:onReceiveValue:s = " + s);
            }
        });
    }

    public void addObjectForJS(){
        mWebView.addJavascriptInterface(new NativeLog(),"nativeLog");
    }
}
```


- native如何回调给前端？



android通过mWebView.evaluateJavascript或者mWebView.loadUrl的方式调用JS，在android 4.4以上推荐使用evaluateJavascript，loadUrl会导致页面刷新，键盘收回等问题。


- native如何向前端注入对象？



通过addJavascriptInterface添加对象，比如：


```java

mWebView.addJavascriptInterface(new NativeLog(),"nativeLog");
```


其中NativeLog的实现如下：


```java

public class NativeLog {

    @JavascriptInterface
    public void print(String log) {
        XLog.d("nativeLog:" + log);
    }
}
```


这里面的方法print，需要添加注解@JavascriptInterface，JS才能访问到。


添加之后，前端可以通过如下方式调用：


```javascript

document.getElementById('a2').addEventListener('click', function () {
    console.log('clicked....');
    if (nativeLog) {
        nativeLog.print('content from js');
    }
});
```


### 2.如果实现url的拦截和重定向？


WebView的WebViewClient中，可以通过重写shouldOverrideUrlLoading实现，如果需要拦截，可以return true;比如：


```java

public class XWebViewClient extends WebViewClient {

    public XWebViewClient() {
        super();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        XLog.d("shouldOverrideUrlLoading2...");
        if (shouldOverrideUrlLoadingInternal(view, request.getUrl().toString())) {
            return true;
        }
        return super.shouldOverrideUrlLoading(view, request);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        XLog.d("shouldOverrideUrlLoading1...");
        if (shouldOverrideUrlLoadingInternal(view, url)) {
            return true;
        }
        return super.shouldOverrideUrlLoading(view, url);
    }

    private boolean shouldOverrideUrlLoadingInternal(WebView view, String url) {
        if (url != null && url.startsWith("https://www.baidu.com")) {
            view.loadUrl("file:///android_asset/xbridge_demo.html");
            return true;
        }
        if (url != null && url.startsWith("xscheme://native_page")) {
            //根据前端的href进行scheme拦截，跳转到native的页面
            view.getContext().startActivity(new Intent(view.getContext(), TestActivity.class));
            return true;
        }
        if(url != null && url.startsWith("")){

        }
        return false;
    }
	...
}
```


- 拦截url
这里可以实现很多功能，比如页面跳转到外部链接[https://www.baidu.com](https://www.baidu.com)时，可以拦截掉，然后让WebView加载本地的某个页面，或者是错误页面。
- 扩展JSBridge

也可以通过shouldOverrideUrlLoading实现JSBridge，比如拦截到url是`xscheme://native_page`时，容器跳转到某一个native页面，达到启动native页面的目的，也可以扩展其他功能。前端可以通过如下方式：


```js

<a href="xscheme://native_page?name=test_activity" class="btn read" id="a3">通过href的scheme调用native</a><br><br><br>
```


- 处理指定的某些url

比如这里可以判断url如果是以.apk结尾的时候，启动系统的浏览器进行下载；拦截到某些scheme时，唤起对应的app等等。


### 3.如何实现资源的拦截？


可以对WebViewClient的shouldInterceptRequest方法进行Override，然后根据资源请求进行拦截。


```

public class XWebViewClient extends WebViewClient {

	 ...

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        XLog.d("shouldInterceptRequest1:" + url + "");
        WebResourceResponse webResourceResponse = interceptRequestInternal(view, url);
        if (webResourceResponse != null) {
            return webResourceResponse;
        }
        return super.shouldInterceptRequest(view, url);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        XLog.d("shouldInterceptRequest2:" + request.getUrl() + "");
        WebResourceResponse webResourceResponse = interceptRequestInternal(view, request.getUrl().toString());
        if (webResourceResponse != null) {
            return webResourceResponse;
        }
        return super.shouldInterceptRequest(view, request);
    }

    private WebResourceResponse interceptRequestInternal(WebView view, String url) {
        if (url != null &&
                (url.endsWith("png")
                        || url.endsWith("jpg")
                        || url.endsWith("gif")
                        || url.endsWith("JPEG"))) {
            WebResourceResponse webResourceResponse = null;
            try {
                webResourceResponse = new WebResourceResponse("image/jpeg", "UTF-8", getStream());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                XLog.e(e.getMessage());
            }
            return webResourceResponse;
        }
        return null;
    }

    private InputStream getStream() throws FileNotFoundException {

        FileInputStream fileInputStream = new FileInputStream(new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/xxx.jpg"));

        return fileInputStream;
    }

    ...
}
```


比如上述代码，是对H5页面的图片资源进行拦截，把H5页面的图片资源换成本地的图片。如上代码并没有实际意义，只是举例说明对资源的拦截。


### 4.如何实现资源的离线？


上述步骤中根据拦截shouldInterceptRequest方法，构建对应的WebResourceResponse，其实H5资源离线的原理正是如此。这里需要注意的是CORS问题，拦截之后构建的资源URI可能和当前H5页面的域名不同，因此需要添加Access-Control-Allow-Origin；


```
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private WebResourceResponse interceptRequestInternal(WebView view, String url) {
        if (url != null &&
                (url.endsWith("png")
                        || url.endsWith("jpg")
                        || url.endsWith("gif")
                        || url.endsWith("JPEG"))) {
            WebResourceResponse webResourceResponse = null;
            try {
                webResourceResponse = new WebResourceResponse("image/jpeg", "UTF-8", getStream());
                Map<String, String> header = new HashMap<>();
                header.put("Access-Control-Allow-Origin", "**");
                webResourceResponse.setResponseHeaders(header);
            } catch (Exception e) {
                e.printStackTrace();
                XLog.e(e.getMessage());
            }
            return webResourceResponse;
        }
        return null;
    }
```


资源离线的原理就是在H5页面需要访问的在线资源，在H5页面打开之前，提前下载好或者内置到apk中，等到H5页面加载时，通过shouldInterceptRequest拦截资源的加载，然后将已经离线的资源封装成WebResourceResponse对象，提高H5页面的加载速度。


### 5.如何获取JS打印的日志？


之前已经讲过，可以通过WebChromeClient.onConsoleMessage方法获取。但是，如果部分机型没有回调，可以通过addJavascriptInterface的方式，给H5注入一个Console对象，并实现Console对象的log等方法即可。


### 6.如何实现接口的预取？


对H5页面的网络请求提前预取，可以提升H5页面的性能。其原理是在启动H5页面的Activity时，就时开始进行网络请求，把请求的结果进行缓存。当页面加载时，拦截页面的网络请求，然后将缓存的结果返回给H5，完成接口预期，加速页面的加载。


### 7.WebView如何和native的Cookie同步？

很多app的登录页面是native实现的，登录成功之后，希望H5页面能够在加载时，共用native的Cookie，此时如何做同步呢？Android中可以使用CookieManager


```java
    CookieManager cookieManager = CookieManager.getInstance();
    cookieManager.setAcceptCookie(true);

    List<Cookie> cookies = getCookiesFromLogin();//从业务中获取

    cookieManager.removeAllCookie();

    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if (cookie.getName().contains("session")){
                String cookieString = cookie.getName() + "=" + cookie.getValue() + "; Domain=" + cookie.getDomain();
                cookieManager.setCookie(cookie.getDomain(), cookieString);
            }
        }
    }
    ...
    //加载H5页面
    webView.loadUrl(url);
```


当然，android也可以从CookieManager获取WebView保存的Cookie，比如H5页面HTTP回应的头信息里面，放置的Set-Cookie信息，WebView会保存在CookieManager中.

### 8.如何注入JSBridge？

为了方便的管控JSBridge的内容，方便后续统一升级，我们希望JSBridge的js通过拦截注入的方式加载，而不是直接写到前端代码中，那我们如何做呢？

- 1.将JSBridge的内容抽离成一个xbridge.js文件，存放到assets目录中
- 2.做一个虚拟域名，比如demo中的`https://www.baidu.com/xbridge.js`
- 3.在H5的入口html中，添加script标签，比如：

```html
<script src="https://www.baidu.com/xbridge.js"></script>
```

- 4.在webview的WebViewClient中拦截虚拟url，然后将本地assets目录中的jsbridge注入进去即可。

```java
...

	private WebResourceResponse interceptRequestInternal(WebView view, String url) {
        if (url != null && url.equals("https://www.baidu.com/xbridge.js")) {
            XLog.d("start inject js bridge...");
            WebResourceResponse webResourceResponse = null;
            try {
                InputStream inputStream = view.getContext().getAssets().open("xbridge.js");
                webResourceResponse = new WebResourceResponse("text/html", "UTF-8", inputStream);
                Map<String, String> header = new HashMap<>();
                header.put("Access-Control-Allow-Origin", url);
                webResourceResponse.setResponseHeaders(header);
            } catch (Exception e) {
                e.printStackTrace();
                XLog.e(e.getMessage());
            }
            return webResourceResponse;
        }
        return null;
    }
...
```

### 9.如何唤起其他APP？

大家都知道，每个app有一个scheme，比如口碑的scheme为`alipays://platformapi/startApp?appId=20000001`；具体每个app的scheme是什么，我们可以反编译对应点apk，查看AndroidManifest文件的如下代码片段：

```xml
<activity ....>
	<intent-filter>
		<action android:name="android.intent.action.VIEW" />
			<category android:name="android.intent.category.DEFAULT" />
			<category android:name="android.intent.category.BROWSABLE" />
			<data android:scheme="demoscheme{这里是scheme的值}" />
	</intent-filter>
</activity>
```

其中`android:scheme`的值即为scheme

那我们如何唤起app呢？

- 1.前端通过`window.location.href`传入scheme，比如：

```html
...
document.getElementById("a9").addEventListener("click", function () {
	const scheme = "koubei://platformapi/startApp?appId=20000001";
	console.log("scheme = " + scheme);
	window.location.href = scheme;
});
...

```
以上是唤起口碑app的demo

- 2.在webview的WebViewClient中通过shouldOverrideUrlLoading拦截scheme，然后唤起app

```java
	@Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        XLog.d("shouldOverrideUrlLoading1...");
        if (shouldOverrideUrlLoadingInternal(view, url)) {
            return true;
        }
        return super.shouldOverrideUrlLoading(view, url);
    }
	...
	private boolean shouldOverrideUrlLoadingInternal(WebView view, String url) {
        //非http的scheme，唤起对应scheme的app
        if (url != null && !url.startsWith("http")) {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            view.getContext().startActivity(intent);
            return true;
        }
        ...
        return false;
    }

```

### 10.前端如何下载apk？

针对android的情况，可以有如下方式：

- 1.通过JSBridge，由native实现一个文件下载和安装的功能，前端只需要调用即可
- 2.通过window.location.href，客户端对其进行拦截，同上；
- 3.使用iframe.src中添加js标本的方式，可以避免打开新的页面；
- 4.使用form表单的action字段，通过submit也可以


### 11.前端根据ua判断iOS/Android以及版本


- 1.前端直接可以通过ua即可判断，比如：

```js

isAndroidOrIOS() {
    var u = navigator.userAgent;
    console.log("ua = " + u);
    var isAndroid = u.indexOf("Android") > -1 || u.indexOf("Adr") > -1; //android终端
    var isiOS = !!u.match(/\(i[^;]+;( U;)? CPU.+Mac OS X/); //ios终端
    if (isAndroid) {
      return "android";
    }
    if (isiOS) {
      return "ios";
    }
    return false;
 }
```

- 2.自定义UA

如果端上想把自己的版本号等信息，也想通过UA传给前端，可以直接设置webview的settings，比如：

```java

public class XWebView extends WebView {

    @Override
    public WebSettings getSettings() {

        WebSettings webSettings = super.getSettings();

        String ua = webSettings.getUserAgentString();

        try {
            PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            String version = pInfo.versionName;
            String appName = getContext().getString(pInfo.applicationInfo.labelRes);
            String newUaWithVersion = ua + " AndroidApp_" + appName + "/" + version;
            webSettings.setUserAgentString(newUaWithVersion);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return webSettings;
    }
}
```

当然，也可以直接调用webview的setUserAgentString进行重置；比如设置后的UA为：

```
Mozilla/5.0 (Linux; Android 6.0.1; MuMu Build/V417IR; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/52.0.2743.100 Mobile Safari/537.36 AndroidApp_XWebViewDemo/1.0
```

### 12.离线包设计

- 离线压缩包设计

以一个`create-react-app`的H5项目为例，执行`npm build`进行编译，我们将生成的`build`目录下的文件进行简单的压缩，压缩成zip包。为了方便后续离线包的管理，我们在离线包中添加一个我们约定的包信息描述文件，比如命名为`pkg-desc.json`，当然，我们也可以复用`manifest.json`这个文件，在json中，添加对离线包的描述，比如：

```json
{
  "launchParams": {
    "indexUrl": "/index.html",
    "transparentTitle": "true"
  },
  "vHost": "https://www.taobao.com"
}
```
这里简单列几个参数，比如vHost，即是我们设计的`虚拟域名`，我们设计的目的，是在webview加载vHost+indexUrl时，自动加载当前离线包目录里的index.html；压缩成离线包之前的目录为：

```
.
├── asset-manifest.json
├── favicon.ico
├── index.html
├── manifest.json
├── robots.txt
└── static
    ├── css
    │   ├── main.5b343dc0.chunk.css
    │   └── main.5b343dc0.chunk.css.map
    └── js
        ├── 2.94eb2e42.chunk.js
        ├── 2.94eb2e42.chunk.js.LICENSE.txt
        ├── 2.94eb2e42.chunk.js.map
        ├── 3.df4689f7.chunk.js
        ├── 3.df4689f7.chunk.js.map
        ├── main.83926041.chunk.js
        ├── main.83926041.chunk.js.map
        ├── runtime-main.ebe92296.js
        └── runtime-main.ebe92296.js.map

```

这些文件均是`create-react-app`编译(npm run build)之后生成的文件，其中`manifest.json`保存了我们自定义的信息。

- 离线包的解压和加载

我们将上述设计的压缩包，压缩成`zip`文件，在app启动的时候，将离线包的内容加载到内存中，并以Map的形式保存。我们以存放到assets目录下的离线包为例，对其进行加载的部分核心代码：

```java

public class OfflinePkgManager {

    private static final String PKG_DESC_JSON = "manifest.json";
    private static final String DEFAULT_INDEX_URL = "/index.html";

    private static OfflinePkgManager mOfflinePkgManager = null;
    //保存离线包的内容：key为url的路径，byte为对应的缓存内容
    private Map<String, byte[]> offlinePkg = new ConcurrentHashMap<>();

    //保存离线包的地址以及启动参数等信息
    private Map<String, PkgDescModel> vHostUrlInfoMap = new ConcurrentHashMap<>();

    private OfflinePkgManager() {

    }

    public synchronized static OfflinePkgManager getInstance() {
        if (mOfflinePkgManager == null) {
            mOfflinePkgManager = new OfflinePkgManager();
        }
        return mOfflinePkgManager;
    }

    /**
     * 加载assets中的离线包
     *
     * @param context
     */
    public void loadAssetsPkg(Context context) {
        try {
            InputStream inputStream = context.getAssets().open("react_zhihu_demo_offline_pkg.zip");
            Map<String, byte[]> relativePathByteMap = new HashMap<>();
            XFileUtils.loadZipFile(inputStream, relativePathByteMap);
            addPackageInfo(relativePathByteMap);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加离线包信息
     *
     * @param relativePathByteMap
     */
    private void addPackageInfo(Map<String, byte[]> relativePathByteMap) {
        //获取离线包的描述信息
        byte[] descByte = relativePathByteMap.get(PKG_DESC_JSON);
        if (descByte != null) {
            String jsonStr = new String(descByte);
            PkgDescModel pkgDesc = JSON.parseObject(jsonStr, PkgDescModel.class);
            String vHost = pkgDesc.getvHost();
            String indexUrl = getIndexUrl(pkgDesc);
            String vHostUrl = vHost + indexUrl;
            //保存离线包信息
            vHostUrlInfoMap.put(vHostUrl, pkgDesc);
            for (Map.Entry<String, byte[]> entry : relativePathByteMap.entrySet()) {
                String fullUrl = vHost + "/" + entry.getKey();
                //保存离线包内容
                offlinePkg.put(fullUrl, entry.getValue());
            }
            XLog.d("add packageInfo success:" + vHostUrl);
        }
    }

    /**
     * 获取入口html的url
     *
     * @param pkgDesc
     * @return
     */
    private String getIndexUrl(PkgDescModel pkgDesc) {
        if (pkgDesc != null
                && pkgDesc.getLaunchParams() != null
                && pkgDesc.getLaunchParams().getIndexUrl() != null) {
            return pkgDesc.getLaunchParams().getIndexUrl();
        }
        //默认为/index.html
        return DEFAULT_INDEX_URL;
    }

    public byte[] getOfflineContent(String url) {
        byte[] offlineContent = offlinePkg.get(url);
        if (offlineContent != null) {
            XLog.d(String.format("url:%s  load from cache", url));
        }
        return offlineContent;
    }

}
```

加载zip文件的方法实现：

```java
//将ZIP文件加载内存中，以路径和byte[]的key/value形式存储
public class XFileUtils {
    /**
     * 将压缩包解压到内存中
     *
     * @param is
     * @param relativePathByteMap
     * @return
     */
    public static boolean loadZipFile(InputStream is, Map<String, byte[]> relativePathByteMap) {
        ZipInputStream zis;
        try {
            String filename;
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry zipEntry;
            int count;
            byte[] buffer = new byte[1024];
            while ((zipEntry = zis.getNextEntry()) != null) {
                filename = zipEntry.getName();
                if (zipEntry.isDirectory() || TextUtils.isEmpty(filename)) {
                    continue;
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                while ((count = zis.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, count);
                }
                byte[] data = byteArrayOutputStream.toByteArray();
                String pureFilename = filename.substring(filename.indexOf('/') + 1);
                //保持相对路径的文件名称以及对应的数据
                relativePathByteMap.put(pureFilename, data);
                byteArrayOutputStream.close();
                XLog.d("unzip filename = " + pureFilename);
                zis.closeEntry();
            }
            zis.close();
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}

```
- 虚拟域名与离线包的匹配

使用webview通过loadUrl加载虚拟域名的时候，webview通过shouldInterceptRequest拦截url，查找对应的``

```java
public class XWebViewClient extends WebViewClient {

    ...
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        WebResourceResponse webResourceResponse = interceptRequestInternal(view, url);
        if (webResourceResponse != null) {
            return webResourceResponse;
        }
        return super.shouldInterceptRequest(view, url);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        WebResourceResponse webResourceResponse = interceptRequestInternal(view, request.getUrl().toString());
        if (webResourceResponse != null) {
            return webResourceResponse;
        }
        return super.shouldInterceptRequest(view, request);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private WebResourceResponse interceptRequestInternal(WebView view, String url) {
        if (url != null) {
            WebResourceResponse webResourceResponse = null;
            try {
                //尝试获取离线内容
                byte[] contentByte = OfflinePkgManager.getInstance().getOfflineContent(url);
                if (contentByte != null && contentByte.length > 0) {
                    InputStream inputStream = new ByteArrayInputStream(contentByte);
                    //构造WebResourceResponse
                    webResourceResponse = new WebResourceResponse(getMimeType(url), "UTF-8", inputStream);
                    Map<String, String> header = new HashMap<>();
                    header.put("Access-Control-Allow-Origin", url);
                    webResourceResponse.setResponseHeaders(header);
                }
            } catch (Exception e) {
                e.printStackTrace();
                XLog.e(e.getMessage());
            }
            return webResourceResponse;
        }
        return null;
    }

    /**
     * 获取mimeType：
     *
     * @param url
     * @return
     */
    private String getMimeType(String url) {
        try {
            String mimeType = null;
            String ext = MimeTypeMap.getFileExtensionFromUrl(url);
            if ("js".equalsIgnoreCase(ext)) {
                mimeType = "application/javascript";
            } else {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            }
            XLog.d("mimeType = " + mimeType);
            return mimeType;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "text/html";
    }

}
```

这一部分就是根据url查找离线包的内容，然后构造WebResourceResponse以及对应的MimeType；

- url参数(魔术参数)

`manifest.json`里面还可以添加一下默认的参数，比如设置titlebar的参数，这些参数可以在webview加载url之前生效。比如titlebar的背景，title等信息，实现原理比较简单，这里不再赘述。

### 13.自定义错误页面

native可以根据WebView的报错信息，自定义错误页面，可通过WebViewClient监听报错回调：

```java
onReceivedError
onReceivedHttpError
onReceivedSslError
```

自定义的错误页面有如下几种实现方式：

- 1.H5实现一个默认的错误页，打包到apk中，直接使用webview加载。
- 2.使用native实现一个layout，然后覆盖在webview布局上方，同时可以自定义一些功能。

### 14.页面加载超时

自定义webview的页面加载超时时间，可以通过`onPageStarted`和`onPageFinished`进行配合

```java
public class XWebViewClient extends WebViewClient {

	 private boolean loadTimeout;
    ...
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        startLoadTime = System.currentTimeMillis();
        XLog.d("onPageStarted:" + url);
        new Thread(new Runnable() {
            @Override
            public void run() {
                loadTimeout = true;
                try {
                    Thread.sleep(10000);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                if (loadTimeout) {
                    webViewPage.showErrorPage("TIMEOUT");
                }
            }
        }).start();
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        loadTimeout = false;
        super.onPageFinished(view, url);
    }

```


### 15.对前端页面的性能统计

为了更加准确和详细的获取H5页面的性能，可以通过前端的`performance`实现：

- 前端代码:

```js
;(function (win) {
  if (!win.performance || !win.performance.timing) return {};
  var time = win.performance.timing;
  var timingResult = {};
  timingResult["重定向时间"] = (time.redirectEnd - time.redirectStart) / 1000;
  timingResult["DNS解析时间"] =
    (time.domainLookupEnd - time.domainLookupStart) / 1000;
  timingResult["TCP完成握手时间"] =
    (time.connectEnd - time.connectStart) / 1000;
  timingResult["HTTP请求响应完成时间"] =
    (time.responseEnd - time.requestStart) / 1000;
  timingResult["DOM开始加载前所花费时间"] =
    (time.responseEnd - time.navigationStart) / 1000;
  timingResult["DOM加载完成时间"] = (time.domComplete - time.domLoading) / 1000;
  timingResult["DOM结构解析完成时间"] =
    (time.domInteractive - time.domLoading) / 1000;
  timingResult["脚本加载时间"] =
    (time.domContentLoadedEventEnd - time.domContentLoadedEventStart) / 1000;
  timingResult["onload事件时间"] =
    (time.loadEventEnd - time.loadEventStart) / 1000;
  timingResult["页面完全加载时间"] =
    timingResult["重定向时间"] +
    timingResult["DNS解析时间"] +
    timingResult["TCP完成握手时间"] +
    timingResult["HTTP请求响应完成时间"] +
    timingResult["DOM结构解析完成时间"] +
    timingResult["DOM加载完成时间"];
  return { result: timingResult };
})(this);

```

- webview在onPageFinised加载完成时执行统计代码

```
public class XWebViewClient extends WebViewClient {

    ...
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        String jsCode = "(function(win){if(!win.performance||!win.performance.timing){return{}}var time=win.performance.timing;var timingResult={};timingResult[\"重定向时间\"]=(time.redirectEnd-time.redirectStart)/1000;timingResult[\"DNS解析时间\"]=(time.domainLookupEnd-time.domainLookupStart)/1000;timingResult[\"TCP完成握手时间\"]=(time.connectEnd-time.connectStart)/1000;timingResult[\"HTTP请求响应完成时间\"]=(time.responseEnd-time.requestStart)/1000;timingResult[\"DOM开始加载前所花费时间\"]=(time.responseEnd-time.navigationStart)/1000;timingResult[\"DOM加载完成时间\"]=(time.domComplete-time.domLoading)/1000;timingResult[\"DOM结构解析完成时间\"]=(time.domInteractive-time.domLoading)/1000;timingResult[\"脚本加载时间\"]=(time.domContentLoadedEventEnd-time.domContentLoadedEventStart)/1000;timingResult[\"onload事件时间\"]=(time.loadEventEnd-time.loadEventStart)/1000;timingResult[\"页面完全加载时间\"]=timingResult[\"重定向时间\"]+timingResult[\"DNS解析时间\"]+timingResult[\"TCP完成握手时间\"]+timingResult[\"HTTP请求响应完成时间\"]+timingResult[\"DOM结构解析完成时间\"]+timingResult[\"DOM加载完成时间\"];return{result:timingResult}})(this);";
        view.evaluateJavascript(jsCode, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {
                XLog.d("onPageFinished:JSResult = " + s);
                //TODO 对性能数据进行上报
            }
        });
    }
    ...
}
```

- 统计结果示例

以https://www.baidu.com为例：

```json

{
    "result":{
        "DNS解析时间":0.013,
        "DOM加载完成时间":1.586,
        "DOM开始加载前所花费时间":0.312,
        "DOM结构解析完成时间":0.142,
        "HTTP请求响应完成时间":0.118,
        "TCP完成握手时间":0.123,
        "onload事件时间":0.001,
        "脚本加载时间":0.001,
        "重定向时间":0,
        "页面完全加载时间":1.982
    }
}

```

### 16.H5页面的任务栈多开

类似微信小程序或者支付宝小程序，我们可以将H5页面在一个独立的进程中加载.

- 多进程的优势：
	- 隔离：与主进程是进程级别的隔离，不影响主进程的Crash
	- 增加app的可用内存

- 多进程的劣势：
	- 数据共享问题
	- 不必要的初始化

对于一个H5页面，我们需要将WebView所在的Activity设置为独立进程。同时，为了支持多个H5页面，我们预注册5个Activity，比如：

```

public class XWebViewActivity extends Activity {

    private WebViewPage mWebViewPage = null;

    ...
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Process.setThreadPriority(-20);
        setContentView(R.layout.activity_webview);
        ...
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
```

可以通过setTaskDescription设置task的名称，比如以小程序的业务名称命名。在AndroidManifest.xml中配置多进程以及task的属性，以如下xml为例：主要是设置`android:process`,`launchMode`,`taskAffinity`三个属性。

- xml的设置

```xml
<activity
    android:name=".activity.XWebViewActivity"
    android:label="@string/app_name"
    android:launchMode="singleTask"
    android:process=":h5container"
    android:taskAffinity=":lite1">
    <intent-filter>
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="xwebview" />
    </intent-filter>
</activity>

<activity
    android:name=".activity.XWebViewActivity$H5Activity1"
    android:launchMode="singleTask"
    android:process=":activity1"
    android:taskAffinity=":activity1" />

<activity
    android:name=".activity.XWebViewActivity$H5Activity2"
    android:launchMode="singleTask"
    android:process=":activity2"
    android:taskAffinity=":activity2" />

<activity
    android:name=".activity.XWebViewActivity$H5Activity3"
    android:launchMode="singleTask"
    android:process=":activity3"
    android:taskAffinity=":activity3" />

<activity
    android:name=".activity.XWebViewActivity$H5Activity4"
    android:launchMode="singleTask"
    android:process=":activity4"
    android:taskAffinity=":activity4" />

<activity
    android:name=".activity.XWebViewActivity$H5Activity5"
    android:launchMode="singleTask"
    android:process=":activity5"
    android:taskAffinity=":activity5" />
```

- 启动H5Activity以及复用

最多打开5个独立进程的H5页面，当打开第6个时，会覆盖掉第1个打开的进程。如下是部分核心代码：

```
public class RouterManager {

    private static Activity mMainActivity;
    private static int index = 0;

    private static List<String> availableActivityList = new ArrayList<>();

    private static final String[] H5_ACTIVITY_ARR = new String[]{
            XWebViewActivity.H5Activity1.class.getName(),
            XWebViewActivity.H5Activity2.class.getName(),
            XWebViewActivity.H5Activity3.class.getName(),
            XWebViewActivity.H5Activity4.class.getName(),
            XWebViewActivity.H5Activity5.class.getName(),
    };

    public static void init(Activity activity) {
        index = 0;
        mMainActivity = activity;
        availableActivityList.addAll(Arrays.asList(H5_ACTIVITY_ARR));
    }

    public static void openH5Activity(String url) {
        String activityClazz = availableActivityList.get(index % (availableActivityList.size() - 1));
        try {
            Class<?> c = Class.forName(activityClazz);
            Intent intent = new Intent(mMainActivity, c);
            intent.putExtra("url", url.trim());
            mMainActivity.startActivity(intent);
            index++;
        } catch (Exception ignored) {
        }
    }
}
```

- 多进程的“后遗症”

多进程下，Application会初始化多次，我们需要排除**非必要的初始化**，以及合理安排APP主进程和H5进行的**分工**，然后通过**进程间通信**(比如AIDL)的方式解决主进程和子进程之间的通信问题。

【注】：部分机型上，多进程引起的WebView的Crash，可以通过`WebView.setDataDirectorySuffix(suffix);`[解决](https://stackoverflow.com/questions/51843546/android-pie-9-0-webview-in-multi-process)；

### 17.使用MessageChannel通信

- [MessageChannel](https://developer.mozilla.org/en-US/docs/Web/API/MessageChannel)

MessageChannel，并不是一个新的概念，它是一个Web API。它允许我们创建一个新的MessageChannel(消息通道)然后通过这个消息通道的两个端口(MessagePort)进行传递数据。比如，前端可以通过MessageChannel的

```js
var channel = new MessageChannel();
var port1 = channel.port1;
var port2 = channel.port2;
port1.onmessage = function (event) {
  console.log("recv msg from port2:" + event.data);
};
port2.onmessage = function (event) {
  console.log("recv msg from port1:" + event.data);
};

port1.postMessage("send to port2");
port2.postMessage("send to port1");

```

运行之后，可以看到日志如下：

```txt
recv msg from port1:send to port2
recv msg from port2:send to port1
```

以上只是前端方面的一个简单Demo

- android webview与H5之间通过MessageChannel通信

我们知道，我们可以通过MessageChannel的两个MessagePort进行通信。那native和H5的通信的思路是，native通过WebMessage把其中一个端口发送给H5，H5保存端口的引用，然后通过端口postMessage另一个port；

android端代码：

```
import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebMessage;
import android.webkit.WebMessagePort;
import android.webkit.WebView;

import com.mochuan.github.log.XLog;

/**
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description WebView的MessageChannel
 */
public class XMessageChannel {

    private static final String TAG = "XMessageChannel";

    private WebMessagePort nativePort = null;
    private WebMessagePort h5Port = null;

    @TargetApi(Build.VERSION_CODES.M)
    public void init(WebView webView) {
        final WebMessagePort[] channel = webView.createWebMessageChannel();

        //供native使用的port
        nativePort = channel[0];
        //供h5使用的port
        h5Port = channel[1];
        //监听从h5Port中发送过来的消息
        nativePort.setWebMessageCallback(new WebMessagePort.WebMessageCallback() {
            @Override
            public void onMessage(WebMessagePort port, WebMessage message) {
                XLog.d(TAG, ":onMessage:" + message.getData());
                postMessageToH5("hello from native:" + this.getClass().getName());
            }
        });
        //发送webmessage，把h5Port发送给H5页面
        XLog.d(TAG, "start postWebMessage to transfer port");
        WebMessage webMessage = new WebMessage("__init_port__", new WebMessagePort[]{h5Port});
        webView.postWebMessage(webMessage, Uri.EMPTY);
    }

    /**
     * 通过port，向H5发送webMessage
     *
     * @param msg
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void postMessageToH5(String msg) {
        nativePort.postMessage(new WebMessage(msg));
    }

}
```

不过，MessageChannel需要在WebView的onPageFinished以后才能创建，时机相对较晚。也即是在WebViewClient的onPageFinished回调中，调用init方法。首先通过webView.createWebMessageChannel创建端口，将其中一个供native使用，并设置消息监听回调，另外一个则通过webView.postWebMessage，将内容为`__init_port__`的WebMessage发送给前端，消息中携带了WebMessagePort端口信息。其中``__init_port__`是我们自定义的协议，供前端判断。

接下来，看前端部分的代码实现，前端通过window.addEventListener，监听所有的message消息。然后判断messageEvent的data部分是否是`__init_port__`，如果是的话，就从messageEvent中获取到native传过来的port对象，将其挂载到window上，比如`window.__my_port__`，并设置onmessage消息监听，用于监听android发送过来的消息。另外，我们也可以通过`window.__my_port__`向native发送消息。

```JS
  document.getElementById("a10").addEventListener("click", function () {
    sendMessageToNative();
  });

  function sendMessageToNative() {
    if (window.__my_port__) {
      window.__my_port__.postMessage("h5 test message");
    }
  }

  window.addEventListener("message", receiverMessage, false);

  function receiverMessage(messageEvent) {
    console.log("onmessage...", JSON.stringify(messageEvent.data));
    if (messageEvent.data === "__init_port__") {
      //在window上挂载port对象，将native发过来的h5Port引用保存起来
      window.__my_port__ = messageEvent.ports[0];
      //设置消息
      window.__my_port__.onmessage = function (f) {
        console.log("recv msg from native...");
        onChannelMessage(f.data);
      };
    }
  }

  function onChannelMessage(msg) {
    const content = "msg from native:" + msg;
    document.getElementById("text").innerHTML = "<span>" + content + "</span>";
  }
```

以上即完成native和H5通过MessageChannel进行通信的过程。基于这个消息通道，我们可以自定义自己的通信协议。MessageChannel的优势就是可以减少序列化和反序列化，提升消息通信的性能。[stackoverflow](https://stackoverflow.com/questions/41753104/how-do-you-use-webmessageport-as-an-alternative-to-addjavascriptinterface)也有关于这个问题的讨论和解答。


### 18.webview的安全问题

- addJavscriptInterface

addJavscriptInterface是Android 4.2之前的安全漏洞，4.2以上，可通过@JavascriptInterface注解来声明JS可以访问的native方法，这里不再赘述。

- file协议的跨域

手动配置setAllowFileAccessFromFileURLs或setAllowUniversalAccessFromFileURLs两个API为false当然省事，但是很多场景需要使用到访问本地资源。此时则需要对uri进行校验或权限控制。其他情况，参见国家信息安全漏洞平台的公告[https://www.cnvd.org.cn/webinfo/show/4365?from=timeline](https://www.cnvd.org.cn/webinfo/show/4365?from=timeline)

- 其他安全漏洞:TODO

### 19.V8引擎(J2V8)


- V8的引入以及初始化

```
implementation 'com.eclipsesource.j2v8:j2v8:6.0.0@aar'
```

- 初始化

```
v8runtime = V8.createV8Runtime();
```

- 桥接console.log

```java
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
```

其中，AndroidConsole的源码为：

```java
public class AndroidConsole {

    private static final String TAG = ">>>AndroidConsole<<<";

    /**
     * 通过反射注册Java方法
     *
     * @param msg
     */
    public void log(String msg) {
        XLog.d(TAG, msg);
    }

    public void logObj(V8Object msg) {
        try {
            JSONObject jsonObject = XV8Utils.toJSONObject(msg);
            XLog.d(TAG, jsonObject.toJSONString());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * 通过反射注册Java方法
     *
     * @param msg
     */
    public void error(String msg) {
        XLog.e(TAG, msg);
    }
}
```

这样，在js中调用`console.log`,将会回调到AndroidConsole的log方法，然后就可以看到v8中的日志信息了。

- JSBridge的整体设计

整体思路是，注册一个Java回调与对应的JS方法做绑定，比如

```java
//向v8中注入对象Java方法
v8runtime.registerJavaMethod(new XV8JsBridgeCallback(this), "__xBridge_js_func__");
```

当JS中调用`__xBridge_js_func__`的时候，会回调到XV8JsBridgeCallback的invoke方法。

```java
public class XV8JsBridgeCallback implements JavaVoidCallback {

    XV8Manager mXV8Manager;

    public XV8JsBridgeCallback(XV8Manager manager) {
        this.mXV8Manager = manager;
    }

    @Override
    public void invoke(V8Object receiver, V8Array params) {
        //获取JS中传递过来的参数
        JSONArray jsonArray = XV8Utils.toJSONArray(params);
        if (jsonArray != null) {
            XLog.d("V8ArrayCallBack:" + jsonArray.toJSONString());
        }
        String string = (String)jsonArray.get(0);
        //获取消息，然后处理消息
        new V8BridgeImpl(mXV8Manager).handleMsgFromJS(string);
        params.close();
        receiver.close();
    }
}
```

然后就是JS层的设计，参考第一部分关于WebView的设计，进行如下改造，比如JSBridge的文件名称为`xbridge4v8.js`，其内容如下：

```js

var XJSBridge;
(function () {
  var callbackArr = {};
  XJSBridge = {
    callNative: function (func, param, callback) {
      var seqId = "__bridge_id_" + Math.random() + "" + new Date().getTime();
      callbackArr[seqId] = callback;
      var msgObj = {
        func: func,
        param: param,
        msgType: "callNative",
        seqId: seqId,
      };
      if (typeof __xBridge_js_func__ === "undefined") {
        console.log("__xBridge_js_func__ not register before...");
      } else {
        __xBridge_js_func__(JSON.stringify(msgObj));
      }
    },
    nativeInvokeJS: function (res) {
      console.log("nativeInvokeJS start...");
      try {
        var resObj = JSON.parse(res);
        if (resObj && resObj.msgType === "jsCallback") {
          var func = callbackArr[resObj.seqId];
          if ("function" === typeof func) {
            func(resObj.param);
          }
        } else {
          console.log("error...");
        }
        return true;
      } catch (error) {
        console.error(error);
        return false;
      }
    },
  };
})();

```

与WebView的JSBridge的协议内容一致，但是最终是通过`__xBridge_js_func__`方法调用到native。


```java
//执行自定义的JSBridge代码
v8runtime.executeVoidScript(bridge4v8);
//获取JSBridge对象
mXJSBridge = v8runtime.getObject("XJSBridge");
//获取调用JS的方法
mNativeInvokeJS = (V8Function) mXJSBridge.getObject("nativeInvokeJS");
```

与webview不同，webview是通过`loadUrl`或者`evaluateJavascript`来调用JS，在v8中，我们是通过v8获取对应的JSFunction的对象，来进行调用的，比如以上代码里，我们保存了`mNativeInvokeJS`对象。当我们JSBridge中处理完之后，就可以通过`mNativeInvokeJS`回调给JS了。

```java
private void sendToV8RuntimeInUiThread(String msg) {
	V8Array args = new V8Array(v8runtime);
	args.push(msg);
	mNativeInvokeJS.call(mXJSBridge, args);
	args.close();
}
```

- V8与WebView的通信(以点击dom触发V8调用http请求为例，大致的流程)
	- 1.用户点击webview中的dom，通过webview的JSBridge，将事件传递给native<br>
	- 2.native对事件进行分发，找到对应的JSBridge,对事件进行进行解析<br>
	- 3.然后通过v8的JSBridge，这里的`mNativeInvokeJS`调用V8中的JS方法<br>
	- 4.V8中的JS触发对应的JS方法(需要通过反射调用)<br>
	- 5.JS方法中，调用v8的JSBridge，比如http的JSBridge，调用到native<br>
	- 6.回调到XV8JsBridgeCallback的invoke方法<br>
	- 7.通过V8BridgeImpl对消息进行分发，然后找到对应的HttpBridege<br>
	- 8.HttpBridege通过Okhttp发送请求<br>
	- 9.将http的结果回调给v8中的JS<br>
	- 10.v8中的JS再调用JSBridge，将http的结果，发送给native
	- 11.native再对消息进行分发，调用PostMsgToWebViewBridge这个JSBridge
	- 12.JSBridge通过loadUrl或者evaluateJavascript，或者MessageChannel，发给WebView
	- 13.WebView接收到native发过来的消息，然后展示到dom中。

以上流程，第4步还未完成，因此，通过v8直接运行的`v8demo.js`,其中`v8demo.js`的代码如下：

```js
(function () {
  console.log("start execute...");
  if (XJSBridge) {
    XJSBridge.callNative(
      "http",
      { url: "https://news-at.zhihu.com/api/4/news/latest", data: {} },
      (resp) => {
        if (resp) {
          console.logObj(resp);
          XJSBridge.callNative("postMsgToWebView", resp, () => {
            console.log("post end.");
          });
        }
      }
    );
  } else {
    console.log("XJSBridge is invalid.");
  }
})();
```

如果通过MessageChannel进行通信，`v8_demo.html`的演示代码如下：

```html
<h1>H5与V8引擎测试页面</h1>

<a href="javascript:void(0)" class="btn read" id="a1">v8测试</a
><br /><br /><br />

<body>
  <div id="text"></div>
  <br /><br /><br />
  <div id="container"></div>
</body>
<!-- 在这里注入JSBridge，由webview拦截这个链接，然后替换成assets目录的xbridge.js文件-->
<script src="https://www.baidu.com/xbridge.js"></script>
<script>
  document.getElementById("a1").addEventListener("click", function () {
    window.XJSBridge.callNative("nativeMethod", { name: "test" }, (res) => {
       //TODO
    });
  });

  window.addEventListener("message", receiverMessage, false);

  function receiverMessage(messageEvent) {
    console.log("onmessage...", JSON.stringify(messageEvent.data));
    if (messageEvent.data === "__init_port__") {
      //在window上挂载port对象，将native发过来的h5Port引用保存起来
      window.__my_port__ = messageEvent.ports[0];
      //设置消息
      window.__my_port__.onmessage = function (f) {
        console.log("recv msg from v8...");
        onChannelMessage(f.data);
      };
    }
  }

  function onChannelMessage(msg) {
    const content = "msg from native:" + msg;
    document.getElementById("text").innerHTML = "<span>" + content + "</span>";
  }
</script>

```

- V8中的setTimeout,setInterval,clearTimeout,clearInterval问题

类似console.log的方式，通过native实现


### 20.借助Glide图片加载框架对webview的图片进行缓存

第12部分讲了基于离线包(packageApp)级别的缓存，将前端的html、js、css等进行了统一的缓存。在APP启动的时，将离线包加载到内存中。当Webview加载H5页面时，根据虚拟域名匹配，加载本地的资源。但是，离线包在进行网络请求之后，会开始渲染服务端json数据，这里面有一些图片资源。我们如何对图片进行缓存呢？native通过图片加载框架(比如Glide)实现图片的多级缓存及复用，WebView则没有这么能力，本小节就是通过Glide实现图片资源的拦截和缓存。

- 接入Glide

```
implementation 'com.github.bumptech.glide:glide:4.11.0'
annotationProcessor 'com.github.bumptech.glide:compiler:4.11.0'
```

- 对图片url进行判断，使用Glide加载图片

完整代码如下，主要是通过Glide将图片加载为bitmap，然后构建WebResourceResponse即可。

```java
public class GlideImgCacheManager {

    private static final String TAG = "GlideCache";

    private static GlideImgCacheManager sGlideImgCacheManager = null;

    //只缓存白名单中的图片资源
    private static final HashSet CACHE_IMG_TYPE = new HashSet() {
        {
            add("png");
            add("jpg");
            add("jpeg");
            add("bmp");
            add("webp");
        }
    };

    public synchronized static GlideImgCacheManager getInstance() {
        if (sGlideImgCacheManager == null) {
            sGlideImgCacheManager = new GlideImgCacheManager();
        }
        return sGlideImgCacheManager;
    }

    /**
     * 拦截资源
     *
     * @param url
     * @return
     */
    public WebResourceResponse interceptRequest(WebView webView, String url) {
        try {
            String extension = MimeTypeMapUtils.getFileExtensionFromUrl(url);
            if (TextUtils.isEmpty(extension) || !CACHE_IMG_TYPE.contains(extension.toLowerCase())) {
                //不在支持的缓存范围内
                return null;
            }
            XLog.d(TAG, String.format("start glide cache img (%s),url:%s", extension, url));
            long startTime = System.currentTimeMillis();
            //String mimeType = MimeTypeMapUtils.getMimeTypeFromUrl(url);
            InputStream inputStream = null;
            Bitmap bitmap = Glide.with(webView).asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL).load(url).submit().get();
            inputStream = getBitmapInputStream(bitmap, Bitmap.CompressFormat.JPEG);
            long costTime = System.currentTimeMillis() - startTime;
            if (inputStream != null) {
                XLog.d(TAG, String.format("glide cache img(%s ms): %s", costTime, url));
                WebResourceResponse webResourceResponse = new WebResourceResponse("image/jpg", "UTF-8", inputStream);
                return webResourceResponse;
            } else {
                XLog.e(TAG, String.format("glide cache error.(%s ms): %s", costTime, url));
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    /**
     * 将bitmap进行压缩转换成InputStream
     *
     * @param bitmap
     * @param compressFormat
     * @return
     */
    private InputStream getBitmapInputStream(Bitmap bitmap, Bitmap.CompressFormat compressFormat) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(compressFormat, 80, byteArrayOutputStream);
            byte[] data = byteArrayOutputStream.toByteArray();
            return new ByteArrayInputStream(data);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }
}
```

- 在webView的WebViewClient中拦截

```java
	...
	@Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        WebResourceResponse webResourceResponse = GlideImgCacheManager.getInstance().interceptRequest(view, request.getUrl().toString());
        if (webResourceResponse != null) {
            return webResourceResponse;
        }
        return super.shouldInterceptRequest(view, request);
    }
```

### 21.借助OkHttp的缓存策略做url级别的缓存&预加载

第12部分讲了基于离线包(packageApp)级别的缓存，20讲了基于Glide对图片资源的缓存。那么，如果不是离线包，是在线页面，如何对在线页面的文档、js、css以及其他文件进行缓存呢？我们可以通过OkHttp替代WebView的网络请求，然后使用OkHttp的缓存策略，来缓存WebView中需要加载的url资源。

- 创建OkHttp的拦截器Interceptor

```java
public class MyOkHttpCacheInterceptor implements Interceptor {

    private int maxAga = 365;//default
    private TimeUnit timeUnit = TimeUnit.DAYS;

    public void setMaxAge(int maxAga, TimeUnit timeUnit) {
        this.maxAga = maxAga;
        this.timeUnit = timeUnit;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());

        CacheControl cacheControl = new CacheControl.Builder()
                .maxAge(maxAga, timeUnit)
                .build();

        return response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", cacheControl.toString())
                .build();
    }
}
```

- 对url资源进行拦截，然后使用OkHttp进行加载和缓存

```
public class OkHttpCacheManager {

    private static final String TAG = "CACHE";

    private static OkHttpCacheManager sOkHttpCacheManager;

    private int allCount = 0;
    private int cacheCount = 0;

    //只缓存白名单中的资源
    private static final HashSet CACHE_MIME_TYPE = new HashSet() {
        {
            add("html");
            add("htm");
            add("js");
            add("ico");
            add("css");
            add("png");
            add("jpg");
            add("jpeg");
            add("gif");
            add("bmp");
            add("ttf");
            add("woff");
            add("woff2");
            add("otf");
            add("eot");
            add("svg");
            add("xml");
            add("swf");
            add("txt");
            add("text");
            add("conf");
            add("webp");
        }
    };

    public synchronized static OkHttpCacheManager getIntance() {
        if (sOkHttpCacheManager == null) {
            sOkHttpCacheManager = new OkHttpCacheManager();
        }
        return sOkHttpCacheManager;
    }

    private OkHttpClient mHttpClient;

    private OkHttpCacheManager() {
        //设置缓存的目录文件
        File httpCacheDirectory = new File(XApplication.getApplication().getExternalCacheDir(), "x-webview-http-cache");
        //仅作为日志使用
        if (httpCacheDirectory.exists()) {
            List<File> result = XFileUtils.listFiles(httpCacheDirectory);
            for (File file : result) {
                XLog.d(TAG, "file = " + file.getAbsolutePath());
            }
        }
        //缓存的大小，OkHttp会使用DiskLruCache缓存
        int cacheSize = 20 * 1024 * 1024; // 20 MiB
        Cache cache = new Cache(httpCacheDirectory, cacheSize);
        //设置缓存
        mHttpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(new MyOkHttpCacheInterceptor())
                .cache(cache)
                .build();
    }

    /**
     * 针对url级别的缓存，包括主文档，图片，js，css等
     *
     * @param url
     * @param headers
     * @return
     */
    public WebResourceResponse interceptRequest(String url, Map<String, String> headers) {
        try {
            String extension = MimeTypeMapUtils.getFileExtensionFromUrl(url);
            if (TextUtils.isEmpty(extension) || !CACHE_MIME_TYPE.contains(extension.toLowerCase())) {
                //不在支持的缓存范围内
                XLog.w(TAG + "+" + url + " 's extension is " + extension + "!!not support...");
                return null;
            }
            long startTime = System.currentTimeMillis();
            Request.Builder reqBuilder = new Request.Builder()
                    .url(url);
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    XLog.d(TAG, String.format("header:(%s=%s)", entry.getKey(), entry.getValue()));
                    reqBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }

            Request request = reqBuilder.get().build();
            Response response = mHttpClient.newCall(request).execute();
            if (response.code() != 200) {
                XLog.e(TAG, "response code = " + response.code() + ",extension = " + extension);
                return null;
            }
            String mimeType = MimeTypeMapUtils.getMimeTypeFromUrl(url);
            XLog.d(TAG, "mimeType = " + mimeType + ",extension = " + extension + ",url = " + url);
            WebResourceResponse okHttpWebResourceResponse = new WebResourceResponse(mimeType, "", response.body().byteStream());
            Response cacheRes = response.cacheResponse();
            long endTime = System.currentTimeMillis();
            long costTime = endTime - startTime;
            allCount++;
            if (cacheRes != null) {
                cacheCount++;
                XLog.e(TAG, String.format("count rate = (%s),costTime = (%s);from cache: %s", (1.0f * cacheCount / allCount), costTime, url));
            } else {
                XLog.e(TAG, String.format("costTime = (%s);from server: %s", costTime, url));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String message = response.message();
                if (TextUtils.isEmpty(message)) {
                    message = "OK";
                }
                try {
                    okHttpWebResourceResponse.setStatusCodeAndReasonPhrase(response.code(), message);
                } catch (Exception e) {
                    return null;
                }
                Map<String, String> header = MimeTypeMapUtils.multimapToSingle(response.headers().toMultimap());
                okHttpWebResourceResponse.setResponseHeaders(header);
            }
            return okHttpWebResourceResponse;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

}
```

- 在webView的WebViewClient中设置

```java
    ...
    @Override
	public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        WebResourceResponse webResourceResponse = OkHttpCacheManager.getIntance().interceptRequest(request.getUrl().toString(), request.getRequestHeaders());
        if (webResourceResponse != null) {
            return webResourceResponse;
        }
        return super.shouldInterceptRequest(view, request);
    }
```

- 预加载缓存

上述的逻辑是实时缓存，也就是下次请求相同url才能使用。如果想做预加载，在首次请求就可以使用缓存，则可以提前对资源的url进行加载，然后再拦截到url时，从内存中获取。比如：

```java
    ...
 	 //内存级别的预加载缓存
    private static LruCache<String, byte[]> preLoadCache = new LruCache<>(100);
    ...
	/**
     * 预加载资源
     *
     * @param urls
     */
    public void preLoadResource(final List<String> urls) {
        XApplication.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                for (String url : urls) {
                    try {
                        XLog.e(TAG, "start load res:" + url);
                        Request.Builder reqBuilder = new Request.Builder()
                                .url(url);
                        Request request = reqBuilder.get().build();
                        Response response = mHttpClient.newCall(request).execute();
                        if (response.code() == 200) {
                            XLog.e(TAG, "res preload success..." + url);
                            //保存下载的资源
                            preLoadCache.put(url, response.body().bytes());
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        });
    }
    ...

    public WebResourceResponse interceptRequest(String url, Map<String, String> headers) {
        try {
            ...
            //预加载
            if (preLoadCache.get(url) != null) {
                byte[] contentByte = preLoadCache.get(url);
                InputStream inputStream = new ByteArrayInputStream(contentByte);
                WebResourceResponse webResourceResponse = new WebResourceResponse(MimeTypeMapUtils.getMimeType(url), "UTF-8", inputStream);
                XLog.e(TAG, "hit preload cache.url = " + url);
                return webResourceResponse;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
	  }
```

可以在APP启动或者页面启动时，预加载所需要的资源：

```java
List<String> urls = new ArrayList<>();
urls.add("https://as.alipayobjects.com/g/component/fastclick/1.0.6/fastclick.js");
urls.add("https://as.alipayobjects.com/g/component/es6-promise/3.2.2/es6-promise.min.js");
urls.add("https://www.baidu.com/index.html");
OkHttpCacheManager.getIntance().preLoadResource(urls);
```