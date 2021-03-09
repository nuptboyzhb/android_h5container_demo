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
        console.log("nativeInvokeJS parse end...");
        if (resObj && resObj.msgType === "jsCallback") {
          var func = callbackArr[resObj.seqId];
          if ("function" === typeof func) {
            func(resObj.param);
          }
        } else {
          console.log("parse error...");
        }
        return true;
      } catch (error) {
        console.error(error);
        return false;
      }
    },
  };
})();
