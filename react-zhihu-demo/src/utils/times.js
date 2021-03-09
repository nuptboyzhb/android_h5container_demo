(function (win) {
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
