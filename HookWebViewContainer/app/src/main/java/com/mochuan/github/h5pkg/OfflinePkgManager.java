package com.mochuan.github.h5pkg;

import android.content.Context;
import android.content.res.AssetManager;

import com.alibaba.fastjson.JSON;
import com.mochuan.github.log.XLog;
import com.mochuan.github.util.XFileUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * @version mochuan.zhb on 2020-12-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 离线包的管理类
 */
public class OfflinePkgManager {

    private static final String PKG_DESC_JSON = "manifest.json";
    private static final String DEFAULT_INDEX_URL = "/index.html";
    public static CountDownLatch zipCountDownLatch = new CountDownLatch(1);

    private static OfflinePkgManager mOfflinePkgManager = null;
    //保存离线包的内容：key为url的路径，byte为对应的缓存内容
    private Map<String, byte[]> offlinePkg = new ConcurrentHashMap<>();

    //保存离线包的地址以及启动参数等信息
    private Map<String, PkgDescModel> vHostUrlInfoMap = new ConcurrentHashMap<>();

    private OfflinePkgManager() {

    }

    public Map<String, PkgDescModel> getvHostUrlInfoMap() {
        return this.vHostUrlInfoMap;
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
        XLog.d("start synchronized zipCountDownLatch...");
        try {
            AssetManager assetManager = context.getAssets();
            //获取assets的所有资源名称
            String[] paths = assetManager.list("");
            List<String> zipPkg = new ArrayList<>();
            if (paths != null && paths.length > 0) {
                for (String item : paths) {
                    //过滤zip压缩包
                    if (item != null && item.endsWith(".zip")) {
                        zipPkg.add(item);
                    }
                }
            }
            if (zipPkg.size() > 0) {
                //对zip压缩包进行解析
                for (String zipName : zipPkg) {
                    InputStream inputStream = context.getAssets().open(zipName);
                    Map<String, byte[]> relativePathByteMap = new HashMap<>();
                    XFileUtils.loadZipFile(inputStream, relativePathByteMap);
                    addPackageInfo(relativePathByteMap);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        zipCountDownLatch.countDown();
        XLog.d("zipCountDownLatch countDown");
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

    public Map<String, byte[]> getOfflinePkg() {
        return offlinePkg;
    }
}