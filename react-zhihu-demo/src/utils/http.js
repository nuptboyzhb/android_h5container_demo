import mockdata from "../mockdata";

export default class MyHttp {
  getLatestMsg(callback) {
    if (window.XJSBridge) {
      console.log("window.XJSBridge success...");
      window.XJSBridge.callNative(
        "http",
        { url: "https://news-at.zhihu.com/api/4/news/latest", data: {} },
        (resp) => {
          if (resp) {
            callback(resp);
          }
        }
      );
    } else {
      console.log("window.XJSBridge failed....");
      return callback(mockdata);
    }
  }

  getTheDateMsg(pageIndex, callback) {
    if (window.XJSBridge) {
      console.log("window.XJSBridge success...");
      const date = this.getParamDate(pageIndex);
      const getUrl = `http://news-at.zhihu.com/api/4/news/before/${date}`;
      console.log("start req:" + getUrl);
      window.XJSBridge.callNative("http", { url: getUrl, data: {} }, (resp) => {
        if (resp) {
          callback(resp);
        }
      });
    } else {
      console.log("window.XJSBridge failed in getTheDateMsg....");
      return callback(mockdata);
    }
  }

  getParamDate(pageIndex) {
    var nowDate = new Date();
    var paramTime = nowDate.getTime() - 1000 * 60 * 60 * 24 * pageIndex;
    var paramDate = new Date(paramTime);
    var mm = paramDate.getMonth() + 1; // getMonth() is zero-based
    var dd = paramDate.getDate();
    return [
      paramDate.getFullYear(),
      (mm > 9 ? "" : "0") + mm,
      (dd > 9 ? "" : "0") + dd,
    ].join("");
  }
}
