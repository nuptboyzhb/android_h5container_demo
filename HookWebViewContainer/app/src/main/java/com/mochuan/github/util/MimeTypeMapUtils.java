package com.mochuan.github.util;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.mochuan.github.log.XLog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version mochuan.zhb on 2021-01-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description
 */
public class MimeTypeMapUtils {

    public static String getMimeType(String url) {
        try {
            String mimeType = null;
            String ext = MimeTypeMap.getFileExtensionFromUrl(url);
            if ("js".equalsIgnoreCase(ext)) {
                mimeType = "application/javascript";
            } else {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            }
            if (!TextUtils.isEmpty(mimeType)) {
                XLog.d("mimeType = " + mimeType);
                return mimeType;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "text/html";
    }

    public static String getFileExtensionFromUrl(String url) {
        url = url.toLowerCase();
        if (!TextUtils.isEmpty(url)) {
            int fragment = url.lastIndexOf('#');
            if (fragment > 0) {
                url = url.substring(0, fragment);
            }

            int query = url.lastIndexOf('?');
            if (query > 0) {
                url = url.substring(0, query);
            }

            int filenamePos = url.lastIndexOf('/');
            String filename =
                    0 <= filenamePos ? url.substring(filenamePos + 1) : url;

            // if the filename contains special characters, we don't
            // consider it valid for our matching purposes:
            if (!filename.isEmpty()) {
                int dotPos = filename.lastIndexOf('.');
                if (0 <= dotPos) {
                    return filename.substring(dotPos + 1);
                }
            }
        }

        return "";
    }
    public static String getMimeTypeFromUrl(String url) {
        return  MimeTypeMap.getSingleton().getMimeTypeFromExtension(getFileExtensionFromUrl(url));
    }

    public static Map<String, String> multimapToSingle(Map<String, List<String>> maps) {

        StringBuilder sb = new StringBuilder();
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : maps.entrySet()) {
            List<String> values = entry.getValue();
            sb.delete(0, sb.length());
            if (values != null && values.size() > 0) {
                for (String v : values) {
                    sb.append(v);
                    sb.append(";");
                }
            }
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
            map.put(entry.getKey(), sb.toString());
        }
        return map;
    }
}
