package com.mochuan.github.h5pkg;

import java.io.Serializable;

/**
 * @version mochuan.zhb on 2020-12-22
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 离线包的描述文件
 */
public class PkgDescModel implements Serializable {

    /**
     * {
     *   "launchParams": {
     *     "indexUrl": "/index.html",
     *     "transparentTitle": "true"
     *   },
     *   "vHost": "https://www.taobao.com"
     * }
     */

    //离线包的虚拟host
    private String vHost;

    //启动参数
    private LaunchParamsModel launchParams;

    /**
     * 启动参数的对象
     */
    public static class LaunchParamsModel implements Serializable {

        private String indexUrl;

        private String transparentTitle;

        public String getIndexUrl() {
            return indexUrl;
        }

        public void setIndexUrl(String indexUrl) {
            this.indexUrl = indexUrl;
        }

        public String getTransparentTitle() {
            return transparentTitle;
        }

        public void setTransparentTitle(String transparentTitle) {
            this.transparentTitle = transparentTitle;
        }
    }

    public String getvHost() {
        return vHost;
    }

    public void setvHost(String vHost) {
        this.vHost = vHost;
    }

    public LaunchParamsModel getLaunchParams() {
        return launchParams;
    }

    public void setLaunchParams(LaunchParamsModel launchParams) {
        this.launchParams = launchParams;
    }
}
