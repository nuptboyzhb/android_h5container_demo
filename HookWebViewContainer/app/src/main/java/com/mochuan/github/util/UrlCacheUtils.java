package com.mochuan.github.util;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;

import com.mochuan.github.XApplication;
import com.mochuan.github.log.XLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 简单的本地缓存
 */
public class UrlCacheUtils {

    private static File cacheFileDir = XApplication.getApplication().getDir("urls", Context.MODE_PRIVATE);
    private static String URL_FILE_NAME = "collection.txt";

    public static class Data implements Serializable {
        public Set<String> urls = new HashSet<>();

        public Data(Set<String> urls) {
            this.urls = urls;
        }
    }

    public static boolean saveUrls(String url) {
        Set<String> urls = getUrls();
        urls.add(url);
        Data data = new Data(urls);
        try {
            if (!cacheFileDir.exists()) {
                cacheFileDir.mkdirs();
            }
            File file = new File(cacheFileDir, URL_FILE_NAME);
            FileOutputStream f = new FileOutputStream(file);
            ObjectOutputStream o = new ObjectOutputStream(f);
            // Write objects to file
            o.writeObject(data);
            o.close();
            f.close();
            XLog.d("save url success...");
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Set<String> getUrls() {
        try {
            if (!cacheFileDir.exists()) {
                cacheFileDir.mkdirs();
            }
            FileInputStream fi = new FileInputStream(new File(cacheFileDir, URL_FILE_NAME));
            ObjectInputStream oi = new ObjectInputStream(fi);

            // Read objects
            Data data = (Data) oi.readObject();

            oi.close();
            fi.close();
            if (data != null && data.urls != null && data.urls.size() > 0) {
                return data.urls;
            } else {
                XLog.d("cache file is empty...");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return new HashSet<>();
    }

    public static boolean remove(String url) {
        try {
            if (!cacheFileDir.exists()) {
                cacheFileDir.mkdirs();
            }
            Set<String> urls = getUrls();
            urls.remove(url);
            Data data = new Data(urls);
            File file = new File(cacheFileDir, URL_FILE_NAME);
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream f = new FileOutputStream(file);
            ObjectOutputStream o = new ObjectOutputStream(f);
            // Write objects to file
            o.writeObject(data);
            o.close();
            f.close();
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void copy(String content) {
        if (!TextUtils.isEmpty(content)) {
            // 得到剪贴板管理器
            ClipboardManager cmb = (ClipboardManager) XApplication.getApplication().getSystemService(Context.CLIPBOARD_SERVICE);
            cmb.setText(content.trim());
            // 创建一个剪贴数据集，包含一个普通文本数据条目（需要复制的数据）
            ClipData clipData = ClipData.newPlainText(null, content);
            // 把数据集设置（复制）到剪贴板
            cmb.setPrimaryClip(clipData);
        }
    }

}
