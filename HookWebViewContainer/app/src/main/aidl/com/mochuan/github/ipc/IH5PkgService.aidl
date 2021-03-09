// IH5PkgService.aidl
package com.mochuan.github.ipc;
import java.util.Map;
/**
 * @version mochuan.zhb on 2020-12-30
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 离线包的AIDL
 */
interface IH5PkgService {
    //获取离线包内容
    Map getOfflinePkg();
}
