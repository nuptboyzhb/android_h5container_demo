export default class Utils {
  constructor() {
    this.isAndroidOrIOS.bind(this);
  }

  isAndroid() {
    return this.isAndroidOrIOS() === "android";
  }

  isIOS() {
    return this.isAndroidOrIOS() === "ios";
  }

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
}
