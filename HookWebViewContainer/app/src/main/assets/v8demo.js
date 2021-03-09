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