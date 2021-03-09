(function () {
  var callbackArr = {};
  window.XJSBridge = {
    callNative: function (func, param, callback) {
      var seqId = "__bridge_id_" + Math.random() + "" + new Date().getTime();
      callbackArr[seqId] = callback;
      var msgObj = {
        func: func,
        param: param,
        msgType: "callNative",
        seqId: seqId,
      };
      console.log("____xbridge____:" + JSON.stringify(msgObj));
    },
    nativeInvokeJS: function (res) {
      res = JSON.parse(res);
      if (res && res.msgType === "jsCallback") {
        var func = callbackArr[res.seqId];
        if ("function" === typeof func) {
          setTimeout(function () {
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
